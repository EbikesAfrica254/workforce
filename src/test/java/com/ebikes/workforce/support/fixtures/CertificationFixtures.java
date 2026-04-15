package com.ebikes.workforce.support.fixtures;

import java.time.LocalDate;
import java.util.UUID;

import com.ebikes.workforce.database.entities.Certification;
import com.ebikes.workforce.enums.CertificationType;

public final class CertificationFixtures {

  public static final UUID CERTIFICATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000020");

  private CertificationFixtures() {}

  public static Certification expired(UUID agentId) {
    return base(agentId).expiresAt(LocalDate.now().minusDays(1)).build();
  }

  public static Certification expiring(UUID agentId) {
    return base(agentId).expiresAt(LocalDate.now().plusDays(14)).build();
  }

  public static Certification valid(UUID agentId) {
    return base(agentId).expiresAt(LocalDate.now().plusYears(1)).build();
  }

  public static Certification withId(UUID agentId, UUID certificationId) {
    return base(agentId).id(certificationId).expiresAt(LocalDate.now().plusYears(1)).build();
  }

  public static Certification withType(UUID agentId, CertificationType type) {
    return base(agentId).certificationType(type).expiresAt(LocalDate.now().plusYears(1)).build();
  }

  private static Certification.CertificationBuilder<?, ?> base(UUID agentId) {
    return Certification.builder()
        .id(CERTIFICATION_ID)
        .agentId(agentId)
        .certificationType(CertificationType.GOOD_CONDUCT_CERTIFICATE)
        .createdBy(SecurityFixtures.TEST_USER_ID)
        .issuedAt(LocalDate.now().minusMonths(1))
        .issuedBy("Kenya National Police Service")
        .referenceNumber("GC-2024-001");
  }
}
