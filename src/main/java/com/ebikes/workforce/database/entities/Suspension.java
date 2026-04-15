package com.ebikes.workforce.database.entities;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ebikes.workforce.database.entities.bases.AuditableEntity;
import com.ebikes.workforce.enums.ResponseCode;
import com.ebikes.workforce.exceptions.BusinessRuleException;
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
    name = "suspensions",
    schema = "workforce",
    indexes = {
      @Index(name = "idx_suspensions_agent", columnList = "agent_id"),
      @Index(name = "idx_suspensions_expires_at", columnList = "expires_at"),
      @Index(name = "idx_suspensions_reason", columnList = "reason")
    })
public class Suspension extends AuditableEntity implements Auditable {

  @Column(name = "agent_id", nullable = false, updatable = false)
  @NotNull private UUID agentId;

  @Column(name = "expires_at", columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime expiresAt;

  @Column(name = "lifted_at", columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime liftedAt;

  @Column(name = "lifted_by", length = 36)
  @Size(max = 36) private String liftedBy;

  @Column(name = "notes")
  private String notes;

  @Column(name = "reason", nullable = false, updatable = false)
  @NotBlank private String reason;

  @Column(nullable = false)
  @Version
  private Long version;

  public boolean isActive() {
    return this.liftedAt == null;
  }

  public void lift(String liftedByUserId) {
    if (!isActive()) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Cannot lift a suspension that is no longer active: " + this.getId());
    }
    this.liftedAt = OffsetDateTime.now(ZoneOffset.UTC);
    this.liftedBy = liftedByUserId;
  }

  @Override
  public Map<String, String> toAuditMetadata() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("agentId", this.getAgentId().toString());
    metadata.put("isActive", String.valueOf(this.isActive()));
    metadata.put("reason", this.getReason());
    metadata.put("suspensionId", this.getId().toString());
    if (this.getExpiresAt() != null) {
      metadata.put("expiresAt", this.getExpiresAt().toString());
    }
    if (this.getLiftedAt() != null) {
      metadata.put("liftedAt", this.getLiftedAt().toString());
    }
    if (this.getLiftedBy() != null) {
      metadata.put("liftedBy", this.getLiftedBy());
    }
    return Collections.unmodifiableMap(metadata);
  }
}
