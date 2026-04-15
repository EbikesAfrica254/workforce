package com.ebikes.workforce.services.agents.suspension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.AvailabilityLog;
import com.ebikes.workforce.database.entities.Suspension;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.AvailabilityLogRepository;
import com.ebikes.workforce.database.repositories.SuspensionRepository;
import com.ebikes.workforce.dtos.requests.agents.LiftSuspensionRequest;
import com.ebikes.workforce.dtos.requests.agents.SuspendAgentRequest;
import com.ebikes.workforce.dtos.requests.filters.SuspensionFilter;
import com.ebikes.workforce.dtos.responses.api.PaginatedResponse;
import com.ebikes.workforce.dtos.responses.suspensions.SuspensionDetailResponse;
import com.ebikes.workforce.dtos.responses.suspensions.SuspensionSummaryResponse;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.exceptions.ResourceNotFoundException;
import com.ebikes.workforce.mappers.SuspensionMapper;
import com.ebikes.workforce.support.audit.AuditTemplate;
import com.ebikes.workforce.support.audit.ThrowingSupplier;
import com.ebikes.workforce.support.context.ExecutionContext;
import com.ebikes.workforce.support.fixtures.AgentFixtures;
import com.ebikes.workforce.support.fixtures.SuspensionFixtures;
import com.ebikes.workforce.support.infrastructure.WithExecutionContext;

@DisplayName("SuspensionService")
@ExtendWith({MockitoExtension.class, WithExecutionContext.class})
class SuspensionServiceTest {

  @Mock private AgentRepository agentRepository;
  @Mock private AuditTemplate auditTemplate;
  @Mock private AvailabilityLogRepository availabilityLogRepository;
  @Mock private SuspensionMapper suspensionMapper;
  @Mock private SuspensionRepository suspensionRepository;

  @InjectMocks private SuspensionService suspensionService;

  @SuppressWarnings("unchecked")
  private void wireAuditTemplateSupplier() {
    doAnswer(
            invocation -> {
              ThrowingSupplier<?, ?> operation = invocation.getArgument(3);
              return operation.get();
            })
        .when(auditTemplate)
        .execute(any(), any(), any(), any(ThrowingSupplier.class));
  }

  @Nested
  @DisplayName("getById")
  class GetById {

    @Test
    @DisplayName("should throw ResourceNotFoundException when suspension not found")
    void shouldThrowWhenNotFound() {
      when(suspensionRepository.findById(SuspensionFixtures.SUSPENSION_ID))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> suspensionService.getById(SuspensionFixtures.SUSPENSION_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should return mapped detail response when found")
    void shouldReturnDetailResponse() {
      Suspension suspension = SuspensionFixtures.active(AgentFixtures.AGENT_ID);
      SuspensionDetailResponse response = mock(SuspensionDetailResponse.class);
      when(suspensionRepository.findById(SuspensionFixtures.SUSPENSION_ID))
          .thenReturn(Optional.of(suspension));
      when(suspensionMapper.toDetailResponse(suspension)).thenReturn(response);

      SuspensionDetailResponse result = suspensionService.getById(SuspensionFixtures.SUSPENSION_ID);

      assertThat(result).isEqualTo(response);
    }
  }

  @Nested
  @DisplayName("suspend")
  class Suspend {

    private final SuspendAgentRequest request =
        new SuspendAgentRequest(null, "Pending review", "Policy violation");

    @Test
    @DisplayName("should throw ResourceNotFoundException when agent not found")
    void shouldThrowWhenAgentNotFound() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> suspensionService.suspend(AgentFixtures.AGENT_ID, request))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw BusinessRuleException when agent cannot be suspended")
    void shouldThrowWhenAgentCannotBeSuspended() {
      wireAuditTemplateSupplier();
      Agent agent = AgentFixtures.suspended();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));

      assertThatThrownBy(() -> suspensionService.suspend(AgentFixtures.AGENT_ID, request))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should suspend agent, save suspension and log availability change")
    void shouldSuspendAndSave() {
      wireAuditTemplateSupplier();
      Agent agent = AgentFixtures.available();
      Suspension suspension = SuspensionFixtures.active(AgentFixtures.AGENT_ID);
      SuspensionDetailResponse response = mock(SuspensionDetailResponse.class);
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(suspensionRepository.save(any(Suspension.class))).thenReturn(suspension);
      when(suspensionMapper.toDetailResponse(suspension)).thenReturn(response);

      SuspensionDetailResponse result = suspensionService.suspend(AgentFixtures.AGENT_ID, request);

      assertThat(result).isEqualTo(response);
      assertThat(agent.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.SUSPENDED);
      verify(agentRepository).save(agent);
      verify(suspensionRepository).save(any(Suspension.class));
    }

    @Test
    @DisplayName("should log availability change from previous status to SUSPENDED")
    void shouldLogAvailabilityChangeWithCorrectStatuses() {
      wireAuditTemplateSupplier();
      Agent agent = AgentFixtures.available();
      Suspension suspension = SuspensionFixtures.active(AgentFixtures.AGENT_ID);
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(suspensionRepository.save(any(Suspension.class))).thenReturn(suspension);
      when(suspensionMapper.toDetailResponse(suspension))
          .thenReturn(mock(SuspensionDetailResponse.class));

      suspensionService.suspend(AgentFixtures.AGENT_ID, request);

      ArgumentCaptor<AvailabilityLog> captor = ArgumentCaptor.forClass(AvailabilityLog.class);
      verify(availabilityLogRepository).save(captor.capture());
      assertThat(captor.getValue().getFromStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
      assertThat(captor.getValue().getToStatus()).isEqualTo(AvailabilityStatus.SUSPENDED);
    }
  }

  @Nested
  @DisplayName("liftSuspension")
  class LiftSuspension {

    private final LiftSuspensionRequest request = new LiftSuspensionRequest("Resolved");

    @Test
    @DisplayName("should throw ResourceNotFoundException when agent not found")
    void shouldThrowWhenAgentNotFound() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> suspensionService.liftSuspension(AgentFixtures.AGENT_ID, request))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when no active suspension found")
    void shouldThrowWhenNoActiveSuspension() {
      Agent agent = AgentFixtures.suspended();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(suspensionRepository.findByAgentIdAndLiftedAtIsNull(AgentFixtures.AGENT_ID))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> suspensionService.liftSuspension(AgentFixtures.AGENT_ID, request))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw BusinessRuleException when called from system context")
    void shouldThrowWhenSystemContext() {
      ExecutionContext.setSystem();
      Agent agent = AgentFixtures.suspended();
      Suspension suspension = SuspensionFixtures.active(AgentFixtures.AGENT_ID);
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(suspensionRepository.findByAgentIdAndLiftedAtIsNull(AgentFixtures.AGENT_ID))
          .thenReturn(Optional.of(suspension));
      wireAuditTemplateSupplier();

      assertThatThrownBy(() -> suspensionService.liftSuspension(AgentFixtures.AGENT_ID, request))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should lift suspension, restore agent status and log availability change")
    void shouldLiftAndSave() {
      wireAuditTemplateSupplier();
      Agent agent = AgentFixtures.suspended();
      Suspension suspension = SuspensionFixtures.active(AgentFixtures.AGENT_ID);
      SuspensionDetailResponse response = mock(SuspensionDetailResponse.class);
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(suspensionRepository.findByAgentIdAndLiftedAtIsNull(AgentFixtures.AGENT_ID))
          .thenReturn(Optional.of(suspension));
      when(suspensionRepository.save(suspension)).thenReturn(suspension);
      when(suspensionMapper.toDetailResponse(suspension)).thenReturn(response);

      SuspensionDetailResponse result =
          suspensionService.liftSuspension(AgentFixtures.AGENT_ID, request);

      assertThat(result).isEqualTo(response);
      assertThat(agent.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.OFFLINE);
      verify(suspensionRepository).save(suspension);
      verify(agentRepository).save(agent);
    }

    @Test
    @DisplayName("should log availability change from SUSPENDED to OFFLINE")
    void shouldLogAvailabilityChangeWithCorrectStatuses() {
      wireAuditTemplateSupplier();
      Agent agent = AgentFixtures.suspended();
      Suspension suspension = SuspensionFixtures.active(AgentFixtures.AGENT_ID);
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(suspensionRepository.findByAgentIdAndLiftedAtIsNull(AgentFixtures.AGENT_ID))
          .thenReturn(Optional.of(suspension));
      when(suspensionRepository.save(suspension)).thenReturn(suspension);
      when(suspensionMapper.toDetailResponse(suspension))
          .thenReturn(mock(SuspensionDetailResponse.class));

      suspensionService.liftSuspension(AgentFixtures.AGENT_ID, request);

      ArgumentCaptor<AvailabilityLog> captor = ArgumentCaptor.forClass(AvailabilityLog.class);
      verify(availabilityLogRepository).save(captor.capture());
      assertThat(captor.getValue().getFromStatus()).isEqualTo(AvailabilityStatus.SUSPENDED);
      assertThat(captor.getValue().getToStatus()).isEqualTo(AvailabilityStatus.OFFLINE);
    }
  }

  @Nested
  @DisplayName("search")
  class Search {

    @Test
    @DisplayName("should return paginated suspension summaries")
    @SuppressWarnings("unchecked")
    void shouldReturnPaginatedResults() {
      SuspensionFilter filter = new SuspensionFilter();
      filter.setAgentId(AgentFixtures.AGENT_ID);
      SuspensionSummaryResponse summary = mock(SuspensionSummaryResponse.class);
      Suspension suspension = SuspensionFixtures.active(AgentFixtures.AGENT_ID);
      when(suspensionRepository.findAll(any(Specification.class), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(suspension)));
      when(suspensionMapper.toSummaryResponse(suspension)).thenReturn(summary);

      PaginatedResponse<SuspensionSummaryResponse> result = suspensionService.search(filter);

      assertThat(result.data()).containsExactly(summary);
    }
  }
}
