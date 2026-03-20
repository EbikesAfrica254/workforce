package com.ebikes.workforce.dtos.internal;

import java.math.BigDecimal;
import java.util.UUID;

import com.ebikes.workforce.enums.VehicleClass;

public record ShortlistRequest(
    UUID orderId,
    String organizationId,
    String branchId,
    BigDecimal pickupLatitude,
    BigDecimal pickupLongitude,
    VehicleClass vehicleClass) {}
