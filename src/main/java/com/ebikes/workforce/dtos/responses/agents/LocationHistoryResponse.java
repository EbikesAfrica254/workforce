package com.ebikes.workforce.dtos.responses.agents;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.workforce.enums.LocationSource;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LocationHistoryResponse(
    OffsetDateTime createdAt,
    String h3Index,
    UUID id,
    BigDecimal latitude,
    BigDecimal longitude,
    LocationSource source) {}
