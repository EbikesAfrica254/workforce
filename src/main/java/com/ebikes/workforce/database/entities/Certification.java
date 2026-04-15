package com.ebikes.workforce.database.entities;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ebikes.workforce.database.entities.bases.BaseEntity;
import com.ebikes.workforce.enums.CertificationType;
import com.ebikes.workforce.support.audit.Auditable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(
    name = "certifications",
    schema = "workforce",
    indexes = {
      @Index(name = "idx_certifications_agent", columnList = "agent_id"),
      @Index(name = "idx_certifications_agent_type", columnList = "agent_id, certification_type"),
      @Index(name = "idx_certifications_expires_at", columnList = "expires_at"),
      @Index(name = "idx_certifications_type", columnList = "certification_type")
    })
public class Certification extends BaseEntity implements Auditable {

  @Column(name = "agent_id", nullable = false, updatable = false)
  @NotNull private UUID agentId;

  @Column(name = "certification_type", nullable = false, updatable = false, length = 30)
  @Enumerated(EnumType.STRING)
  @NotNull private CertificationType certificationType;

  @Column(name = "created_by", nullable = false, updatable = false, length = 36)
  @NotBlank @Size(max = 36) private String createdBy;

  @Column(name = "expires_at")
  private LocalDate expiresAt;

  @Column(name = "issued_at", nullable = false, updatable = false)
  @NotNull private LocalDate issuedAt;

  @Column(name = "issued_by")
  @Size(max = 255) private String issuedBy;

  @Column(name = "notes")
  private String notes;

  @Column(name = "reference_number", length = 100)
  @Size(max = 100) private String referenceNumber;

  @Override
  public Map<String, String> toAuditMetadata() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("agentId", this.getAgentId().toString());
    metadata.put("certificationId", this.getId().toString());
    metadata.put("certificationType", this.getCertificationType().name());
    metadata.put("issuedAt", this.getIssuedAt().toString());
    if (this.getExpiresAt() != null) {
      metadata.put("expiresAt", this.getExpiresAt().toString());
    }
    if (this.getReferenceNumber() != null) {
      metadata.put("referenceNumber", this.getReferenceNumber());
    }
    return Collections.unmodifiableMap(metadata);
  }
}
