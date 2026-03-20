package com.ebikes.workforce.jobs;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.Document;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.DocumentRepository;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.enums.DocumentType;
import com.ebikes.workforce.services.agents.AgentService;
import com.ebikes.workforce.support.context.ExecutionContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentComplianceJob {

  private static final Set<DocumentType> REQUIRED_EXPIRABLE_DOCUMENT_TYPES =
      Arrays.stream(CapabilityClass.values())
          .flatMap(c -> c.getRequiredDocuments().stream())
          .filter(DocumentType::isRequiresExpiryDate)
          .collect(Collectors.toSet());

  private static final Set<AvailabilityStatus> TRANSITIONABLE_STATUSES =
      Set.of(AvailabilityStatus.AVAILABLE, AvailabilityStatus.BUSY, AvailabilityStatus.OFFLINE);

  private final AgentRepository agentRepository;
  private final AgentService agentService;
  private final DocumentRepository documentRepository;

  @Scheduled(cron = "${workforce.jobs.document-compliance.cron}")
  @Transactional
  public void checkExpiredDocuments() {
    LocalDate today = LocalDate.now();
    log.info("Running document compliance check: asOf={}", today);

    ExecutionContext.setSystem();
    try {
      List<Document> expiredDocuments =
          documentRepository.findActiveDocumentsPastExpiryDate(
              today, REQUIRED_EXPIRABLE_DOCUMENT_TYPES);

      if (expiredDocuments.isEmpty()) {
        log.info("Document compliance check complete: no expired documents found");
        return;
      }

      expiredDocuments.forEach(Document::expire);
      documentRepository.saveAll(expiredDocuments);

      Set<UUID> affectedAgentIds =
          expiredDocuments.stream().map(doc -> doc.getAgent().getId()).collect(Collectors.toSet());

      log.info(
          "Expired documents found: count={}, affectedAgents={}",
          expiredDocuments.size(),
          affectedAgentIds.size());

      agentRepository.findAllById(affectedAgentIds).stream()
          .filter(agent -> agentStillMissingRequiredDocument(agent, expiredDocuments))
          .filter(agent -> TRANSITIONABLE_STATUSES.contains(agent.getAvailabilityStatus()))
          .forEach(
              agent ->
                  agentService.updateAvailability(
                      agent.getId(), AvailabilityStatus.UNAVAILABLE, "Required document expired"));

      log.info("Document compliance check complete: asOf={}", today);
    } finally {
      ExecutionContext.clear();
    }
  }

  private boolean agentStillMissingRequiredDocument(Agent agent, List<Document> expiredDocuments) {
    Set<DocumentType> requiredExpirable =
        agent.getCapabilityClass().getRequiredDocuments().stream()
            .filter(DocumentType::isRequiresExpiryDate)
            .collect(Collectors.toSet());

    Set<DocumentType> justExpiredForAgent =
        expiredDocuments.stream()
            .filter(doc -> doc.getAgent().getId().equals(agent.getId()))
            .map(Document::getDocumentType)
            .collect(Collectors.toSet());

    return requiredExpirable.stream().anyMatch(justExpiredForAgent::contains);
  }
}
