package com.ebikes.workforce.jobs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.CertificationRepository;

@DisplayName("CertificationComplianceJob")
@ExtendWith(MockitoExtension.class)
class CertificationComplianceJobTest {

  @Mock private AgentRepository agentRepository;
  @Mock private CertificationRepository certificationRepository;

  @InjectMocks private CertificationComplianceJob job;

  @Nested
  @DisplayName("checkExpiredCertifications")
  class CheckExpiredCertifications {

    @Test
    @DisplayName("should do nothing when no expired certifications are found")
    void shouldDoNothingWhenNoExpiredCertificationsFound() {
      when(certificationRepository.findExpiredRequiredCertifications(any(LocalDate.class), any()))
          .thenReturn(java.util.List.of());

      job.checkExpiredCertifications();

      verify(agentRepository, never()).bulkMarkUnavailable(any(), any());
    }

    @Test
    @DisplayName("should bulk mark affected agents unavailable when expired certifications found")
    void shouldBulkMarkAffectedAgentsUnavailableWhenExpiredCertificationsFound() {
      java.util.UUID agentId = java.util.UUID.randomUUID();
      com.ebikes.workforce.database.entities.Certification cert =
          com.ebikes.workforce.support.fixtures.CertificationFixtures.expired(agentId);

      when(certificationRepository.findExpiredRequiredCertifications(any(LocalDate.class), any()))
          .thenReturn(java.util.List.of(cert));
      when(agentRepository.bulkMarkUnavailable(any(), any())).thenReturn(1);

      job.checkExpiredCertifications();

      verify(agentRepository).bulkMarkUnavailable(eq(Set.of(agentId)), any());
    }
  }
}
