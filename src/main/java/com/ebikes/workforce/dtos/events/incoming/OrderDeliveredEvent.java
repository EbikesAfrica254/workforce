package com.ebikes.workforce.dtos.events.incoming;

import java.util.UUID;

public record OrderDeliveredEvent(
    String agentId, boolean onTime, UUID orderId, String serviceReference) {}
