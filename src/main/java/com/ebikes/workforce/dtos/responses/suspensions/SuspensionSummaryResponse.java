package com.ebikes.workforce.dtos.responses.suspensions;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SuspensionSummaryResponse(
    UUID agentId,
    OffsetDateTime createdAt,
    OffsetDateTime expiresAt,
    UUID id,
    boolean isActive,
    String reason) {}
