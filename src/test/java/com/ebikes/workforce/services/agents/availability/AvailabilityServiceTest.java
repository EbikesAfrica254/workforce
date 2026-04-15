package com.ebikes.workforce.services.agents.availability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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

import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.AvailabilityLog;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.AvailabilityLogRepository;
import com.ebikes.workforce.dtos.responses.agents.AgentDetailResponse;
import com.ebikes.workforce.dtos.responses.agents.AvailabilityLogResponse;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.UserRole;
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.exceptions.ResourceNotFoundException;
import com.ebikes.workforce.mappers.AgentMapper;
import com.ebikes.workforce.mappers.AvailabilityLogMapper;
import com.ebikes.workforce.support.audit.AuditTemplate;
import com.ebikes.workforce.support.audit.ThrowingSupplier;
import com.ebikes.workforce.support.context.ExecutionContext;
import com.ebikes.workforce.support.fixtures.AgentFixtures;
import com.ebikes.workforce.support.fixtures.SecurityFixtures;
import com.ebikes.workforce.support.infrastructure.WithExecutionContext;

@DisplayName("AvailabilityService")
@ExtendWith({MockitoExtension.class, WithExecutionContext.class})
class AvailabilityServiceTest {

  @Mock private AgentMapper agentMapper;
  @Mock private AgentRepository agentRepository;
  @Mock private AuditTemplate auditTemplate;
  @Mock private AvailabilityLogMapper availabilityLogMapper;
  @Mock private AvailabilityLogRepository availabilityLogRepository;

  @InjectMocks private AvailabilityService availabilityService;

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

  @Nested
  @DisplayName("getAvailabilityLog")
  class GetAvailabilityLog {

    @Test
    @DisplayName("should throw ResourceNotFoundException when agent not found")
    void shouldThrowWhenAgentNotFound() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.empty());

      PageRequest request = PageRequest.of(0, 10);
      assertThatThrownBy(
              () -> availabilityService.getAvailabilityLog(AgentFixtures.AGENT_ID, request))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should return mapped availability log page when agent found")
    @SuppressWarnings("unchecked")
    void shouldReturnMappedPage() {
      Pageable pageable = PageRequest.of(0, 10);
      when(agentRepository.findById(AgentFixtures.AGENT_ID))
          .thenReturn(Optional.of(AgentFixtures.available()));
      when(availabilityLogRepository.findAll(any(Specification.class), eq(pageable)))
          .thenReturn(new PageImpl<>(List.of()));

      Page<AvailabilityLogResponse> result =
          availabilityService.getAvailabilityLog(AgentFixtures.AGENT_ID, pageable);

      assertThat(result).isNotNull();
    }
  }

  @Nested
  @DisplayName("updateAvailability")
  class UpdateAvailability {

    @Test
    @DisplayName("should throw ResourceNotFoundException when agent not found")
    void shouldThrowWhenAgentNotFound() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  availabilityService.updateAvailability(
                      AgentFixtures.AGENT_ID, AvailabilityStatus.AVAILABLE, null))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw BusinessRuleException when AGENT role targets BUSY")
    void shouldThrowWhenAgentRoleTargetsBusy() {
      ExecutionContext.set(
          SecurityFixtures.TEST_USER_ID,
          SecurityFixtures.TEST_ORGANIZATION_ID,
          null,
          SecurityFixtures.TEST_EMAIL,
          Set.of(),
          SecurityFixtures.TEST_PHONE_NUMBER,
          Set.of(UserRole.AGENT.name()));
      when(agentRepository.findById(AgentFixtures.AGENT_ID))
          .thenReturn(Optional.of(AgentFixtures.available()));

      assertThatThrownBy(
              () ->
                  availabilityService.updateAvailability(
                      AgentFixtures.AGENT_ID, AvailabilityStatus.BUSY, null))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should allow AGENT role to target AVAILABLE")
    void shouldAllowAgentRoleToTargetAvailable() {
      wireAuditTemplate();
      ExecutionContext.set(
          SecurityFixtures.TEST_USER_ID,
          SecurityFixtures.TEST_ORGANIZATION_ID,
          null,
          SecurityFixtures.TEST_EMAIL,
          Set.of(),
          SecurityFixtures.TEST_PHONE_NUMBER,
          Set.of(UserRole.AGENT.name()));
      Agent agent = AgentFixtures.offline();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(agentRepository.save(agent)).thenReturn(agent);
      when(agentMapper.toDetailResponse(agent)).thenReturn(mock(AgentDetailResponse.class));

      availabilityService.updateAvailability(
          AgentFixtures.AGENT_ID, AvailabilityStatus.AVAILABLE, null);

      verify(agentRepository).save(agent);
      verify(availabilityLogRepository).save(any(AvailabilityLog.class));
      assertThat(agent.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
    }

    @Test
    @DisplayName("should allow non-agent role to target BUSY")
    void shouldAllowNonAgentRoleToTargetBusy() {
      wireAuditTemplate();
      Agent agent = AgentFixtures.available();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(agentRepository.save(agent)).thenReturn(agent);
      when(agentMapper.toDetailResponse(agent)).thenReturn(mock(AgentDetailResponse.class));

      availabilityService.updateAvailability(AgentFixtures.AGENT_ID, AvailabilityStatus.BUSY, null);

      verify(agentRepository).save(agent);
      assertThat(agent.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.BUSY);
    }

    @Test
    @DisplayName("should transition agent to OFFLINE")
    void shouldTransitionToOffline() {
      wireAuditTemplate();
      Agent agent = AgentFixtures.available();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(agentRepository.save(agent)).thenReturn(agent);
      when(agentMapper.toDetailResponse(agent)).thenReturn(mock(AgentDetailResponse.class));

      availabilityService.updateAvailability(
          AgentFixtures.AGENT_ID, AvailabilityStatus.OFFLINE, null);

      assertThat(agent.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.OFFLINE);
    }

    @Test
    @DisplayName("should transition agent to UNAVAILABLE")
    void shouldTransitionToUnavailable() {
      wireAuditTemplate();
      Agent agent = AgentFixtures.available();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(agentRepository.save(agent)).thenReturn(agent);
      when(agentMapper.toDetailResponse(agent)).thenReturn(mock(AgentDetailResponse.class));

      availabilityService.updateAvailability(
          AgentFixtures.AGENT_ID, AvailabilityStatus.UNAVAILABLE, null);

      assertThat(agent.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.UNAVAILABLE);
    }

    @Test
    @DisplayName("should throw BusinessRuleException when transitioning to invalid status")
    void shouldThrowOnInvalidStatusTransition() {
      assertThatThrownBy(
              () ->
                  availabilityService.updateAvailability(
                      AgentFixtures.AGENT_ID, AvailabilityStatus.PENDING, null))
          .isInstanceOf(BusinessRuleException.class);
    }
  }
}
