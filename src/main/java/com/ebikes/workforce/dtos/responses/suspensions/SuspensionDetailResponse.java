package com.ebikes.workforce.dtos.responses.suspensions;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SuspensionDetailResponse(
    UUID agentId,
    OffsetDateTime createdAt,
    String createdBy,
    OffsetDateTime expiresAt,
    UUID id,
    boolean isActive,
    OffsetDateTime liftedAt,
    String liftedBy,
    String notes,
    String reason,
    OffsetDateTime updatedAt,
    String updatedBy,
    Long version) {}
