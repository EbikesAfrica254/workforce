package com.ebikes.workforce.dtos.events.incoming;

import java.math.BigDecimal;
import java.util.UUID;

import com.ebikes.workforce.enums.VehicleClass;

public record OrderPendingAssignmentEvent(
    String branchId,
    UUID committedQuoteId,
    UUID orderId,
    String organizationId,
    BigDecimal pickupLatitude,
    BigDecimal pickupLongitude,
    String serviceReference,
    VehicleClass vehicleClass) {}
