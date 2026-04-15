package com.ebikes.workforce.services.agents.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.ebikes.workforce.configurations.properties.H3Properties;
import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.LocationHistory;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.LocationHistoryRepository;
import com.ebikes.workforce.dtos.requests.agents.UpdateLocationRequest;
import com.ebikes.workforce.dtos.requests.filters.LocationHistoryFilter;
import com.ebikes.workforce.dtos.responses.agents.AgentDetailResponse;
import com.ebikes.workforce.dtos.responses.agents.LocationHistoryResponse;
import com.ebikes.workforce.enums.LocationSource;
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.exceptions.ResourceNotFoundException;
import com.ebikes.workforce.mappers.AgentMapper;
import com.ebikes.workforce.mappers.LocationHistoryMapper;
import com.ebikes.workforce.services.agents.AccessPolicy;
import com.ebikes.workforce.support.audit.AuditTemplate;
import com.ebikes.workforce.support.audit.ThrowingSupplier;
import com.ebikes.workforce.support.fixtures.AgentFixtures;
import com.ebikes.workforce.support.infrastructure.WithExecutionContext;
import com.uber.h3core.H3Core;

@DisplayName("LocationService")
@ExtendWith({MockitoExtension.class, WithExecutionContext.class})
class LocationServiceTest {

  @Mock private AccessPolicy accessPolicy;
  @Mock private AgentMapper agentMapper;
  @Mock private AgentRepository agentRepository;
  @Mock private AuditTemplate auditTemplate;
  @Mock private H3Core h3Core;
  @Mock private H3Properties h3Properties;
  @Mock private LocationHistoryRepository locationHistoryRepository;
  @Mock private LocationHistoryMapper locationHistoryMapper;

  @InjectMocks private LocationService locationService;

  private static final BigDecimal LAT = new BigDecimal("-1.286389");
  private static final BigDecimal LNG = new BigDecimal("36.817223");
  private static final String H3_INDEX = "8763a4e6fffffff";

  @SuppressWarnings("unchecked")
  private void wireAuditTemplate() {
    doAnswer(
            invocation -> {
              ThrowingSupplier<?, ?> operation = invocation.getArgument(3);
              return operation.get();
            })
        .when(auditTemplate)
        .execute(any(), any(), any(), any(ThrowingSupplier.class));
  }

  private LocationHistoryFilter filterFor() {
    LocationHistoryFilter filter = new LocationHistoryFilter();
    filter.setAgentId(AgentFixtures.AGENT_ID);
    return filter;
  }

  private UpdateLocationRequest locationRequest() {
    return new UpdateLocationRequest(LAT, LNG);
  }

  @Nested
  @DisplayName("getLocationHistory")
  class GetLocationHistory {

    @Test
    @DisplayName("should throw ResourceNotFoundException when agent not found")
    void shouldThrowWhenAgentNotFound() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.empty());

      LocationHistoryFilter filter = filterFor();
      Pageable pageable = PageRequest.of(0, 10);
      assertThatThrownBy(() -> locationService.getLocationHistory(filter, pageable))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should return mapped location history page when agent found")
    @SuppressWarnings("unchecked")
    void shouldReturnMappedPage() {
      Pageable pageable = PageRequest.of(0, 10);
      LocationHistoryFilter filter = filterFor();

      when(agentRepository.findById(any())).thenReturn(Optional.of(AgentFixtures.available()));
      when(locationHistoryRepository.findAll(any(Specification.class), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of()));

      Page<LocationHistoryResponse> result = locationService.getLocationHistory(filter, pageable);

      assertThat(result).isNotNull();
    }
  }

  @Nested
  @DisplayName("updateLocation")
  class UpdateLocation {

    @Test
    @DisplayName("should throw ResourceNotFoundException when agent not found")
    void shouldThrowWhenAgentNotFound() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.empty());

      UpdateLocationRequest testLocationRequest = locationRequest();
      assertThatThrownBy(
              () ->
                  locationService.updateLocation(
                      AgentFixtures.AGENT_ID, testLocationRequest, LocationSource.MOBILE_BROWSER))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw BusinessRuleException when accessPolicy.owns rejects the caller")
    void shouldThrowWhenOwnershipCheckFails() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID))
          .thenReturn(Optional.of(AgentFixtures.available()));
      doThrow(BusinessRuleException.class).when(accessPolicy).owns(any(Agent.class));

      UpdateLocationRequest testLocationRequest = locationRequest();
      assertThatThrownBy(
              () ->
                  locationService.updateLocation(
                      AgentFixtures.AGENT_ID, testLocationRequest, LocationSource.MOBILE_BROWSER))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should save agent and location history on successful update")
    void shouldSaveAgentAndHistory() {
      wireAuditTemplate();
      Agent agent = AgentFixtures.available();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(h3Properties.getResolution()).thenReturn(9);
      when(h3Core.latLngToCellAddress(LAT.doubleValue(), LNG.doubleValue(), 9))
          .thenReturn(H3_INDEX);
      when(agentRepository.save(agent)).thenReturn(agent);
      when(agentMapper.toDetailResponse(agent)).thenReturn(mock(AgentDetailResponse.class));

      AgentDetailResponse result =
          locationService.updateLocation(
              AgentFixtures.AGENT_ID, locationRequest(), LocationSource.MOBILE_BROWSER);

      verify(agentRepository).save(agent);
      verify(locationHistoryRepository).save(any(LocationHistory.class));
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("should update agent coordinates and h3Index")
    void shouldUpdateAgentCoordinates() {
      wireAuditTemplate();
      Agent agent = AgentFixtures.available();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(h3Properties.getResolution()).thenReturn(9);
      when(h3Core.latLngToCellAddress(LAT.doubleValue(), LNG.doubleValue(), 9))
          .thenReturn(H3_INDEX);
      when(agentRepository.save(agent)).thenReturn(agent);
      when(agentMapper.toDetailResponse(agent)).thenReturn(mock(AgentDetailResponse.class));

      locationService.updateLocation(
          AgentFixtures.AGENT_ID, locationRequest(), LocationSource.ITRACK_DEVICE);

      assertThat(agent.getCurrentLatitude()).isEqualByComparingTo(LAT);
      assertThat(agent.getCurrentLongitude()).isEqualByComparingTo(LNG);
      assertThat(agent.getCurrentH3Index()).isEqualTo(H3_INDEX);
    }

    @Test
    @DisplayName("should pass correct LocationSource to history record")
    void shouldPassCorrectSourceToHistory() {
      wireAuditTemplate();
      Agent agent = AgentFixtures.available();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(h3Properties.getResolution()).thenReturn(9);
      when(h3Core.latLngToCellAddress(LAT.doubleValue(), LNG.doubleValue(), 9))
          .thenReturn(H3_INDEX);
      when(agentRepository.save(agent)).thenReturn(agent);
      when(agentMapper.toDetailResponse(agent)).thenReturn(mock(AgentDetailResponse.class));

      locationService.updateLocation(
          AgentFixtures.AGENT_ID, locationRequest(), LocationSource.WEB_BROWSER);

      verify(locationHistoryRepository).save(any(LocationHistory.class));
    }
  }
}
