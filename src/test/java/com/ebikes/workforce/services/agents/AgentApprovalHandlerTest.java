package com.ebikes.workforce.services.agents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.dtos.events.incoming.MakerCheckerDecision;
import com.ebikes.workforce.dtos.internal.FieldChange;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.CheckerOutcome;
import com.ebikes.workforce.enums.FieldType;
import com.ebikes.workforce.exceptions.ResourceNotFoundException;
import com.ebikes.workforce.services.agents.document.DocumentService;
import com.ebikes.workforce.support.audit.AuditTemplate;
import com.ebikes.workforce.support.audit.ThrowingRunnable;
import com.ebikes.workforce.support.changes.ChangeApplier;
import com.ebikes.workforce.support.fixtures.AgentFixtures;
import com.ebikes.workforce.support.infrastructure.WithExecutionContext;

@DisplayName("AgentApprovalHandler")
@ExtendWith({MockitoExtension.class, WithExecutionContext.class})
class AgentApprovalHandlerTest {

  @Mock private AgentRepository agentRepository;
  @Mock private AuditTemplate auditTemplate;
  @Mock private ChangeApplier changeApplier;
  @Mock private DocumentService documentService;

  @InjectMocks private AgentApprovalHandler agentApprovalHandler;

  @SuppressWarnings("unchecked")
  private void wireAuditTemplate() {
    doAnswer(
            invocation -> {
              ThrowingRunnable<?> operation = invocation.getArgument(3);
              operation.run();
              return null;
            })
        .when(auditTemplate)
        .execute(any(), any(), any(), any(ThrowingRunnable.class));
  }

  private MakerCheckerDecision decision(
      String operation, CheckerOutcome outcome, String reason, List<FieldChange> changes) {
    return new MakerCheckerDecision(
        null,
        Instant.now(),
        AgentFixtures.AGENT_ID,
        "AGENT",
        operation,
        changes,
        outcome,
        reason,
        "ref-001");
  }

  @Nested
  @DisplayName("handleDecision")
  class HandleDecision {

    @Test
    @DisplayName("should throw ResourceNotFoundException when agent not found")
    void shouldThrowWhenAgentNotFound() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  agentApprovalHandler.handleDecision(
                      decision("CREATE", CheckerOutcome.APPROVED, null, List.of())))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should validate documents, activate them and approve agent on CREATE + APPROVED")
    void shouldApproveAgentOnCreateApproved() {
      wireAuditTemplate();
      Agent agent = AgentFixtures.pending();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));

      agentApprovalHandler.handleDecision(
          decision("CREATE", CheckerOutcome.APPROVED, null, List.of()));

      verify(documentService)
          .validateRequiredDocumentsUploaded(AgentFixtures.AGENT_ID, agent.getCapabilityClass());
      verify(documentService).activateDocuments(AgentFixtures.AGENT_ID);
      verify(agentRepository).save(agent);
      assertThat(agent.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.OFFLINE);
    }

    @Test
    @DisplayName("should set rejection reason and skip document calls on CREATE + REJECTED")
    void shouldRejectAgentOnCreateRejected() {
      wireAuditTemplate();
      Agent agent = AgentFixtures.pending();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));

      agentApprovalHandler.handleDecision(
          decision("CREATE", CheckerOutcome.REJECTED, "Missing documents", List.of()));

      verify(documentService, never()).validateRequiredDocumentsUploaded(any(), any());
      verify(documentService, never()).activateDocuments(any());
      verify(agentRepository).save(agent);
      assertThat(agent.getRejectionReason()).isEqualTo("Missing documents");
    }

    @Test
    @DisplayName("should apply changes and save agent on UPDATE + APPROVED")
    void shouldApplyChangesOnUpdateApproved() {
      wireAuditTemplate();
      Agent agent = AgentFixtures.offline();
      List<FieldChange> changes =
          List.of(new FieldChange("firstName", FieldType.STRING, "Jane", "John"));
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));

      agentApprovalHandler.handleDecision(
          decision("UPDATE", CheckerOutcome.APPROVED, null, changes));

      verify(changeApplier).applyChanges(agent, changes);
      verify(agentRepository).save(agent);
    }

    @Test
    @DisplayName("should set rejection reason and skip change applier on UPDATE + REJECTED")
    void shouldRejectAgentOnUpdateRejected() {
      wireAuditTemplate();
      Agent agent = AgentFixtures.pending();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));

      agentApprovalHandler.handleDecision(
          decision("UPDATE", CheckerOutcome.REJECTED, "Invalid changes", List.of()));

      verify(changeApplier, never()).applyChanges(any(), any());
      verify(agentRepository).save(agent);
      assertThat(agent.getRejectionReason()).isEqualTo("Invalid changes");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException on unknown operation")
    void shouldThrowOnUnknownOperation() {
      Agent agent = AgentFixtures.pending();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));

      assertThatThrownBy(
              () ->
                  agentApprovalHandler.handleDecision(
                      decision("UNKNOWN", CheckerOutcome.APPROVED, null, List.of())))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }
}
