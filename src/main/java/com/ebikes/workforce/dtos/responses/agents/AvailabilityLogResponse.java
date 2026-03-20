package com.ebikes.workforce.dtos.responses.agents;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.workforce.enums.AvailabilityStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AvailabilityLogResponse(
    OffsetDateTime createdAt,
    AvailabilityStatus fromStatus,
    UUID id,
    String reason,
    AvailabilityStatus toStatus) {}
