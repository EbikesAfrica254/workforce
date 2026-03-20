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

import com.ebikes.workforce.database.entities.Certification;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.CertificationRepository;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.enums.CertificationType;
import com.ebikes.workforce.services.agents.AgentService;
import com.ebikes.workforce.support.context.ExecutionContext;

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

  private static final Set<CapabilityClass> CERTIFICATION_REQUIRED_CAPABILITY_CLASSES =
      Arrays.stream(CapabilityClass.values())
          .filter(c -> !c.getRequiredCertifications().isEmpty())
          .collect(Collectors.toSet());

  private static final Set<AvailabilityStatus> TRANSITIONABLE_STATUSES =
      Set.of(AvailabilityStatus.AVAILABLE, AvailabilityStatus.BUSY, AvailabilityStatus.OFFLINE);

  private final AgentRepository agentRepository;
  private final AgentService agentService;
  private final CertificationRepository certificationRepository;

  @Scheduled(cron = "${workforce.jobs.certification-compliance.cron}")
  @Transactional
  public void checkExpiredCertifications() {
    LocalDate today = LocalDate.now();
    log.info("Running certification compliance check: asOf={}", today);

    ExecutionContext.setSystem();
    try {
      List<Certification> expiredCertifications =
          certificationRepository.findExpiredRequiredCertifications(
              today, REQUIRED_CERTIFICATION_TYPES);

      if (expiredCertifications.isEmpty()) {
        log.info("Certification compliance check complete: no expired certifications found");
        return;
      }

      Set<UUID> affectedAgentIds =
          expiredCertifications.stream().map(Certification::getAgentId).collect(Collectors.toSet());

      log.info(
          "Expired certifications found: count={}, affectedAgents={}",
          expiredCertifications.size(),
          affectedAgentIds.size());

      agentRepository.findAllById(affectedAgentIds).stream()
          .filter(
              agent ->
                  CERTIFICATION_REQUIRED_CAPABILITY_CLASSES.contains(agent.getCapabilityClass()))
          .filter(agent -> TRANSITIONABLE_STATUSES.contains(agent.getAvailabilityStatus()))
          .forEach(
              agent ->
                  agentService.updateAvailability(
                      agent.getId(),
                      AvailabilityStatus.UNAVAILABLE,
                      "Required certification expired"));

      log.info("Certification compliance check complete: asOf={}", today);
    } finally {
      ExecutionContext.clear();
    }
  }
}
