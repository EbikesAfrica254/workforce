package com.ebikes.workforce.dtos.events.outgoing;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.workforce.enums.VehicleClass;

public record AgentShortlistEmptyEvent(
    String branchId,
    UUID orderId,
    String organizationId,
    String pickupH3Index,
    OffsetDateTime queriedAt,
    String serviceReference,
    VehicleClass vehicleClass)
    implements Serializable {}
