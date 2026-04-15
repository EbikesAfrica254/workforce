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

import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.CertificationRepository;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.enums.CertificationType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CertificationComplianceJob {

  private static final List<CertificationType> REQUIRED_CERTIFICATION_TYPES =
      Arrays.stream(CapabilityClass.values())
          .flatMap(c -> c.getRequiredCertifications().stream())
          .distinct()
          .toList();

  private static final Set<AvailabilityStatus> TRANSITIONABLE_STATUSES =
      Set.of(AvailabilityStatus.AVAILABLE, AvailabilityStatus.BUSY, AvailabilityStatus.OFFLINE);

  private final AgentRepository agentRepository;
  private final CertificationRepository certificationRepository;

  @Scheduled(cron = "${workforce.jobs.certification-compliance.cron}")
  @Transactional
  public void checkExpiredCertifications() {
    LocalDate today = LocalDate.now();
    log.info("Running certification compliance check: asOf={}", today);

    List<com.ebikes.workforce.database.entities.Certification> expiredCertifications =
        certificationRepository.findExpiredRequiredCertifications(
            today, REQUIRED_CERTIFICATION_TYPES);

    if (expiredCertifications.isEmpty()) {
      log.info("Certification compliance check complete: no expired certifications found");
      return;
    }

    Set<UUID> affectedAgentIds =
        expiredCertifications.stream()
            .map(com.ebikes.workforce.database.entities.Certification::getAgentId)
            .collect(Collectors.toSet());

    log.info(
        "Expired certifications found: count={}, affectedAgents={}",
        expiredCertifications.size(),
        affectedAgentIds.size());

    int updated = agentRepository.bulkMarkUnavailable(affectedAgentIds, TRANSITIONABLE_STATUSES);

    log.info(
        "Certification compliance check complete: agentsMarkedUnavailable={}, asOf={}",
        updated,
        today);
  }
}
