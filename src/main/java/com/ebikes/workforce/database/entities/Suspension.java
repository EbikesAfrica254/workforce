package com.ebikes.workforce.database.entities;

import java.io.Serial;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "suspensions",
    schema = "workforce",
    indexes = {
      @Index(name = "idx_suspensions_agent", columnList = "agent_id"),
      @Index(name = "idx_suspensions_expires_at", columnList = "expires_at"),
      @Index(name = "idx_suspensions_reason", columnList = "reason")
    })
public class Suspension extends AuditableEntity {

  @Serial private static final long serialVersionUID = 1L;

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

  public Suspension(UUID agentId, String reason, OffsetDateTime expiresAt, String notes) {
    this.agentId = agentId;
    this.reason = reason;
    this.expiresAt = expiresAt;
    this.notes = notes;
    this.version = 0L;
  }

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
}
