package com.ebikes.workforce.dtos.events.outgoing;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.ebikes.workforce.enums.AuditOutcome;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditEvent(
    UUID entityId,
    @NotBlank String entityType,
    String eventType,
    String failureReason,
    String ipAddress,
    Map<String, String> metadata,
    String organizationId,
    @NotNull AuditOutcome outcome,
    String serviceReference,
    Instant timestamp,
    String userId)
    implements Serializable {

  public AuditEvent {
    metadata = metadata == null ? null : Map.copyOf(metadata);
  }
}
