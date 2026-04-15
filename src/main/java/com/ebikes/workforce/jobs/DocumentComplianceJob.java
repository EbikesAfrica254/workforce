package com.ebikes.workforce.jobs;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.DocumentRepository;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.enums.DocumentType;

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
  private final DocumentRepository documentRepository;

  @Scheduled(cron = "${workforce.jobs.document-compliance.cron}")
  @Transactional
  public void checkExpiredDocuments() {
    LocalDate today = LocalDate.now();
    log.info("Running document compliance check: asOf={}", today);

    int expired = documentRepository.bulkExpireDocuments(today, REQUIRED_EXPIRABLE_DOCUMENT_TYPES);

    if (expired == 0) {
      log.info("Document compliance check complete: no expired documents found");
      return;
    }

    log.info("Expired documents marked: count={}", expired);

    Set<UUID> affectedAgentIds =
        documentRepository.findAgentIdsWithExpiredRequiredDocuments(
            today, REQUIRED_EXPIRABLE_DOCUMENT_TYPES);

    int updated = agentRepository.bulkMarkUnavailable(affectedAgentIds, TRANSITIONABLE_STATUSES);

    log.info(
        "Document compliance check complete: agentsMarkedUnavailable={}, asOf={}", updated, today);
  }
}
