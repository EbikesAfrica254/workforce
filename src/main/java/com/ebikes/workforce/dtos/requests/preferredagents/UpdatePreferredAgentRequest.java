package com.ebikes.workforce.dtos.requests.preferredagents;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdatePreferredAgentRequest(
    @NotNull(message = "Organization ID is required") @Size(max = 36, message = "Organization ID must not exceed 36 characters") String organizationId,
    @Positive(message = "Priority must be a positive integer") Integer priority,
    @Size(max = 1000, message = "Notes must not exceed 1000 characters") String notes) {}
