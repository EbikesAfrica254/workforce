package com.ebikes.workforce.database.entities;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import com.ebikes.workforce.database.entities.bases.BaseEntity;
import com.ebikes.workforce.enums.AvailabilityStatus;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(
    name = "availability_log",
    schema = "workforce",
    indexes = {
      @Index(name = "idx_availability_log_agent", columnList = "agent_id"),
      @Index(name = "idx_availability_log_created_at", columnList = "created_at")
    })
public class AvailabilityLog extends BaseEntity {

  @Column(name = "agent_id", nullable = false, updatable = false)
  @NotNull private UUID agentId;

  @Column(name = "from_status", nullable = false, updatable = false, length = 20)
  @Enumerated(EnumType.STRING)
  @NotNull private AvailabilityStatus fromStatus;

  @Column(name = "reason", updatable = false)
  private String reason;

  @Column(name = "to_status", nullable = false, updatable = false, length = 20)
  @Enumerated(EnumType.STRING)
  @NotNull private AvailabilityStatus toStatus;

  public static AvailabilityLog create(
      UUID agentId, AvailabilityStatus fromStatus, AvailabilityStatus toStatus, String reason) {
    return AvailabilityLog.builder()
        .agentId(agentId)
        .fromStatus(fromStatus)
        .toStatus(toStatus)
        .reason(reason)
        .build();
  }
}
