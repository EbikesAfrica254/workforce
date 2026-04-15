package com.ebikes.workforce.services.agents;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.workforce.constants.EventConstants.DomainEvents;
import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.dtos.events.incoming.MakerCheckerDecision;
import com.ebikes.workforce.dtos.internal.FieldChange;
import com.ebikes.workforce.enums.CheckerOutcome;
import com.ebikes.workforce.services.agents.document.DocumentService;
import com.ebikes.workforce.support.audit.AuditTemplate;
import com.ebikes.workforce.support.changes.ChangeApplier;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AgentApprovalHandler extends AgentScopedService {

  private static final String OPERATION_CREATE = "CREATE";
  private static final String OPERATION_UPDATE = "UPDATE";

  private final AuditTemplate auditTemplate;
  private final ChangeApplier changeApplier;
  private final DocumentService documentService;

  public AgentApprovalHandler(
      AgentRepository agentRepository,
      AuditTemplate auditTemplate,
      ChangeApplier changeApplier,
      DocumentService documentService) {
    super(agentRepository);
    this.auditTemplate = auditTemplate;
    this.changeApplier = changeApplier;
    this.documentService = documentService;
  }

  @Transactional
  public void handleDecision(MakerCheckerDecision decision) {
    log.info(
        "Processing agent maker-checker decision: entityId={}, operation={}, outcome={}",
        decision.entityId(),
        decision.operation(),
        decision.outcome());

    Agent agent = requireAgentById(decision.entityId());
    boolean approved = decision.outcome() == CheckerOutcome.APPROVED;

    switch (decision.operation()) {
      case OPERATION_CREATE -> {
        if (approved) {
          approveCreation(agent);
        } else {
          reject(agent, decision.operation(), decision.reason());
        }
      }
      case OPERATION_UPDATE -> {
        if (approved) {
          approveUpdate(agent, decision.originalChanges());
        } else {
          reject(agent, decision.operation(), decision.reason());
        }
      }
      default ->
          throw new IllegalArgumentException(
              "Unrecognised operation '"
                  + decision.operation()
                  + "' for entityId="
                  + decision.entityId());
    }
  }

  private void approveCreation(Agent agent) {
    documentService.validateRequiredDocumentsUploaded(agent.getId(), agent.getCapabilityClass());
    documentService.activateDocuments(agent.getId());

    auditTemplate.execute(
        agent,
        null,
        DomainEvents.Agent.APPROVED,
        () -> {
          agent.approve();
          agentRepository.save(agent);
        });

    log.info("Agent creation approved: agentId={}", agent.getId());
  }

  private void approveUpdate(Agent agent, List<FieldChange> changes) {
    auditTemplate.execute(
        agent,
        null,
        DomainEvents.Agent.UPDATE_APPROVED,
        () -> {
          changeApplier.applyChanges(agent, changes);
          agentRepository.save(agent);
        });

    log.info(
        "Agent update approved and applied: agentId={}, changesCount={}",
        agent.getId(),
        changes.size());
  }

  private void reject(Agent agent, String operation, String reason) {
    auditTemplate.execute(
        agent,
        null,
        DomainEvents.Agent.REJECTED,
        () -> {
          agent.reject(reason);
          agentRepository.save(agent);
        });

    log.info("Agent {} rejected: agentId={}", operation, agent.getId());
  }
}
