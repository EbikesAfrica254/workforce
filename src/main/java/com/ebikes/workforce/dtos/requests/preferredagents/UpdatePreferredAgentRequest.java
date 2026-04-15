package com.ebikes.workforce.dtos.requests.preferredagents;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdatePreferredAgentRequest(
    @Size(max = 1000, message = "Notes must not exceed 1000 characters") String notes,
    @Positive(message = "Priority must be a positive integer") Integer priority) {}
