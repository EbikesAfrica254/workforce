package com.ebikes.workforce.dtos.events.incoming;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderCancelledEvent(
    UUID agentId, String cancelledFromStatus, UUID orderId, String serviceReference) {}
