package com.ebikes.workforce.dtos.requests.preferredagents;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreatePreferredAgentRequest(
    @NotNull(message = "Agent ID is required") UUID agentId,
    @Size(max = 36, message = "Branch ID must not exceed 36 characters") String branchId,
    @Size(max = 1000, message = "Notes must not exceed 1000 characters") String notes,
    @NotNull(message = "Organization ID is required") @Size(max = 36, message = "Organization ID must not exceed 36 characters") String organizationId,
    @NotNull(message = "Priority is required") @Positive(message = "Priority must be a positive integer") Integer priority) {}
