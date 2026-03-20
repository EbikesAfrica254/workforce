package com.ebikes.workforce.dtos.internal;

import java.math.BigDecimal;
import java.util.UUID;

import com.ebikes.workforce.enums.VehicleClass;

public record ShortlistCandidate(
    UUID agentId,
    BigDecimal latitude,
    BigDecimal longitude,
    VehicleClass vehicleClass,
    boolean isPreferred) {}
