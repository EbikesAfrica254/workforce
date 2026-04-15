package com.ebikes.workforce.services.agents.shortlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.workforce.configurations.properties.H3Properties;
import com.ebikes.workforce.configurations.properties.ShortlistProperties;
import com.ebikes.workforce.constants.EventConstants.DomainEvents;
import com.ebikes.workforce.constants.EventConstants.RoutingKeys;
import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.PreferredAgentRepository;
import com.ebikes.workforce.dtos.events.outgoing.AgentShortlistEmptyEvent;
import com.ebikes.workforce.dtos.events.outgoing.AgentShortlistResolvedEvent;
import com.ebikes.workforce.dtos.internal.ShortlistRequest;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.enums.VehicleClass;
import com.ebikes.workforce.services.events.OutboxService;
import com.ebikes.workforce.support.fixtures.AgentFixtures;
import com.ebikes.workforce.support.fixtures.SecurityFixtures;
import com.uber.h3core.H3Core;

@DisplayName("ShortlistService")
@ExtendWith(MockitoExtension.class)
class ShortlistServiceTest {

  private static final UUID ORDER_ID = UUID.randomUUID();
  private static final UUID AGENT_B_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");
  private static final String ORGANIZATION_ID = SecurityFixtures.TEST_ORGANIZATION_ID;
  private static final String BRANCH_ID = "branch-001";
  private static final String PICKUP_H3 = "8763a4e6fffffff";
  private static final BigDecimal LAT = new BigDecimal("-1.286389");
  private static final BigDecimal LNG = new BigDecimal("36.817223");

  @Mock private AgentRepository agentRepository;
  @Mock private H3Core h3Core;
  @Mock private H3Properties h3Properties;
  @Mock private OutboxService outboxService;
  @Mock private PreferredAgentRepository preferredAgentRepository;
  @Mock private ShortlistProperties shortlistProperties;

  @InjectMocks private ShortlistService shortlistService;

  private void wireH3(List<String> searchCells) {
    when(h3Properties.getResolution()).thenReturn(7);
    when(h3Core.latLngToCellAddress(LAT.doubleValue(), LNG.doubleValue(), 7)).thenReturn(PICKUP_H3);
    when(shortlistProperties.getDefaultRingSize()).thenReturn(2);
    when(h3Core.gridDisk(PICKUP_H3, 2)).thenReturn(searchCells);
    when(shortlistProperties.getFreshThresholdMinutes()).thenReturn(10);
  }

  private ShortlistRequest request(String organizationId, String branchId) {
    return new ShortlistRequest(ORDER_ID, organizationId, branchId, LAT, LNG, VehicleClass.BICYCLE);
  }

  @Nested
  @DisplayName("resolve")
  class Resolve {

    @Test
    @DisplayName("should publish EMPTY when no eligible agents found")
    void shouldPublishEmptyWhenNoEligibleAgents() {
      List<String> cells = List.of(PICKUP_H3);
      wireH3(cells);
      when(agentRepository.findEligibleForShortlist(
              eq(AvailabilityStatus.AVAILABLE),
              eq(cells),
              any(),
              eq(Set.of(CapabilityClass.BICYCLE_RIDER))))
          .thenReturn(List.of());

      shortlistService.resolve(request(ORGANIZATION_ID, null));

      verify(outboxService)
          .publish(
              eq(DomainEvents.Shortlist.EMPTY),
              any(AgentShortlistEmptyEvent.class),
              eq(RoutingKeys.WORKFORCE_SHORTLIST_EMPTY));
      verify(outboxService, never()).publish(eq(DomainEvents.Shortlist.RESOLVED), any(), any());
    }

    @Test
    @DisplayName("should publish RESOLVED with candidates when eligible agents found")
    void shouldPublishResolvedWhenEligibleAgentsFound() {
      List<String> cells = List.of(PICKUP_H3);
      wireH3(cells);
      when(shortlistProperties.getExpirySeconds()).thenReturn(120);
      Agent agent = AgentFixtures.available();
      when(agentRepository.findEligibleForShortlist(
              eq(AvailabilityStatus.AVAILABLE),
              eq(cells),
              any(),
              eq(Set.of(CapabilityClass.BICYCLE_RIDER))))
          .thenReturn(List.of(agent));
      when(preferredAgentRepository.findAgentIdsByOrganizationId(ORGANIZATION_ID))
          .thenReturn(List.of());

      shortlistService.resolve(request(ORGANIZATION_ID, null));

      verify(outboxService)
          .publish(
              eq(DomainEvents.Shortlist.RESOLVED),
              any(AgentShortlistResolvedEvent.class),
              eq(RoutingKeys.WORKFORCE_SHORTLIST_RESOLVED));
      verify(outboxService, never()).publish(eq(DomainEvents.Shortlist.EMPTY), any(), any());
    }

    @Test
    @DisplayName("should use findAgentIdsByOrganizationId when branchId is null")
    void shouldQueryByOrganizationIdWhenNoBranchId() {
      List<String> cells = List.of(PICKUP_H3);
      wireH3(cells);
      when(shortlistProperties.getExpirySeconds()).thenReturn(120);
      Agent agent = AgentFixtures.available();
      when(agentRepository.findEligibleForShortlist(any(), any(), any(), any()))
          .thenReturn(List.of(agent));
      when(preferredAgentRepository.findAgentIdsByOrganizationId(ORGANIZATION_ID))
          .thenReturn(List.of());

      shortlistService.resolve(request(ORGANIZATION_ID, null));

      verify(preferredAgentRepository).findAgentIdsByOrganizationId(ORGANIZATION_ID);
      verify(preferredAgentRepository, never())
          .findAgentIdsByOrganizationIdAndBranchId(anyString(), anyString());
    }

    @Test
    @DisplayName("should use findAgentIdsByOrganizationIdAndBranchId when branchId is present")
    void shouldQueryByOrganizationIdAndBranchIdWhenBranchPresent() {
      List<String> cells = List.of(PICKUP_H3);
      wireH3(cells);
      when(shortlistProperties.getExpirySeconds()).thenReturn(120);
      Agent agent = AgentFixtures.available();
      when(agentRepository.findEligibleForShortlist(any(), any(), any(), any()))
          .thenReturn(List.of(agent));
      when(preferredAgentRepository.findAgentIdsByOrganizationIdAndBranchId(
              ORGANIZATION_ID, BRANCH_ID))
          .thenReturn(List.of());

      shortlistService.resolve(request(ORGANIZATION_ID, BRANCH_ID));

      verify(preferredAgentRepository)
          .findAgentIdsByOrganizationIdAndBranchId(ORGANIZATION_ID, BRANCH_ID);
      verify(preferredAgentRepository, never()).findAgentIdsByOrganizationId(anyString());
    }

    @Test
    @DisplayName("should skip preferred agent lookup when organizationId is null")
    void shouldSkipPreferredLookupWhenNoOrganizationId() {
      List<String> cells = List.of(PICKUP_H3);
      wireH3(cells);
      when(shortlistProperties.getExpirySeconds()).thenReturn(120);
      Agent agent = AgentFixtures.available();
      when(agentRepository.findEligibleForShortlist(any(), any(), any(), any()))
          .thenReturn(List.of(agent));

      shortlistService.resolve(request(null, null));

      verify(preferredAgentRepository, never()).findAgentIdsByOrganizationId(anyString());
      verify(preferredAgentRepository, never())
          .findAgentIdsByOrganizationIdAndBranchId(anyString(), anyString());
    }

    @Test
    @DisplayName("should mark agent as preferred when their ID is in the preferred set")
    void shouldMarkAgentAsPreferred() {
      List<String> cells = List.of(PICKUP_H3);
      wireH3(cells);
      when(shortlistProperties.getExpirySeconds()).thenReturn(120);
      Agent preferredAgent = AgentFixtures.available();
      Agent nonPreferredAgent = AgentFixtures.withId(AGENT_B_ID);
      when(agentRepository.findEligibleForShortlist(any(), any(), any(), any()))
          .thenReturn(List.of(preferredAgent, nonPreferredAgent));
      when(preferredAgentRepository.findAgentIdsByOrganizationId(ORGANIZATION_ID))
          .thenReturn(List.of(AgentFixtures.AGENT_ID));

      ArgumentCaptor<AgentShortlistResolvedEvent> captor =
          ArgumentCaptor.forClass(AgentShortlistResolvedEvent.class);

      shortlistService.resolve(request(ORGANIZATION_ID, null));

      verify(outboxService).publish(any(), captor.capture(), any());
      List<AgentShortlistResolvedEvent.Candidate> candidates = captor.getValue().candidates();
      assertThat(candidates).hasSize(2);
      assertThat(candidates.get(0).agentId()).isEqualTo(AgentFixtures.AGENT_ID.toString());
      assertThat(candidates.get(0).isPreferred()).isTrue();
      assertThat(candidates.get(1).agentId()).isEqualTo(AGENT_B_ID.toString());
      assertThat(candidates.get(1).isPreferred()).isFalse();
    }

    @Test
    @DisplayName("should map vehicle class correctly from capability class")
    void shouldMapVehicleClassFromCapabilityClass() {
      List<String> cells = List.of(PICKUP_H3);
      wireH3(cells);
      when(shortlistProperties.getExpirySeconds()).thenReturn(120);
      Agent agent = AgentFixtures.available();
      when(agentRepository.findEligibleForShortlist(any(), any(), any(), any()))
          .thenReturn(List.of(agent));
      when(preferredAgentRepository.findAgentIdsByOrganizationId(ORGANIZATION_ID))
          .thenReturn(List.of());

      ArgumentCaptor<AgentShortlistResolvedEvent> captor =
          ArgumentCaptor.forClass(AgentShortlistResolvedEvent.class);

      shortlistService.resolve(request(ORGANIZATION_ID, null));

      verify(outboxService).publish(any(), captor.capture(), any());
      assertThat(captor.getValue().candidates().getFirst().vehicleClass())
          .isEqualTo(VehicleClass.BICYCLE);
    }
  }
}
