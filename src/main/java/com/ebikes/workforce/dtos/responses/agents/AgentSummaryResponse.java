package com.ebikes.workforce.dtos.responses.agents;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.CapabilityClass;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentSummaryResponse(
    AvailabilityStatus availabilityStatus,
    CapabilityClass capabilityClass,
    OffsetDateTime createdAt,
    String currentH3Index,
    String firstName,
    UUID id,
    String lastName,
    String phoneNumber,
    BigDecimal reliabilityScore) {}
