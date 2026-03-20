package com.ebikes.workforce.database.entities;

import java.io.Serial;
import java.time.LocalDate;
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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "certifications",
    schema = "workforce",
    indexes = {
      @Index(name = "idx_certifications_agent", columnList = "agent_id"),
      @Index(name = "idx_certifications_agent_type", columnList = "agent_id, certification_type"),
      @Index(name = "idx_certifications_expires_at", columnList = "expires_at"),
      @Index(name = "idx_certifications_type", columnList = "certification_type")
    })
public class Certification extends BaseEntity {

  @Serial private static final long serialVersionUID = 1L;

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

  private Certification(Builder builder) {
    this.agentId = builder.agentId;
    this.certificationType = builder.certificationType;
    this.createdBy = builder.createdBy;
    this.expiresAt = builder.expiresAt;
    this.issuedAt = builder.issuedAt;
    this.issuedBy = builder.issuedBy;
    this.referenceNumber = builder.referenceNumber;
    this.notes = builder.notes;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private UUID agentId;
    private CertificationType certificationType;
    private String createdBy;
    private LocalDate expiresAt;
    private LocalDate issuedAt;
    private String issuedBy;
    private String notes;
    private String referenceNumber;

    private Builder() {}

    public Builder agentId(UUID agentId) {
      this.agentId = agentId;
      return this;
    }

    public Builder certificationType(CertificationType certificationType) {
      this.certificationType = certificationType;
      return this;
    }

    public Builder createdBy(String createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder expiresAt(LocalDate expiresAt) {
      this.expiresAt = expiresAt;
      return this;
    }

    public Builder issuedAt(LocalDate issuedAt) {
      this.issuedAt = issuedAt;
      return this;
    }

    public Builder issuedBy(String issuedBy) {
      this.issuedBy = issuedBy;
      return this;
    }

    public Builder notes(String notes) {
      this.notes = notes;
      return this;
    }

    public Builder referenceNumber(String referenceNumber) {
      this.referenceNumber = referenceNumber;
      return this;
    }

    public Certification build() {
      return new Certification(this);
    }
  }
}
