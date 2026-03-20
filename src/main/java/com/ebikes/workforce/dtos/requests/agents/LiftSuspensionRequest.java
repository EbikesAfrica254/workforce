package com.ebikes.workforce.dtos.requests.agents;

import jakarta.validation.constraints.Size;

public record LiftSuspensionRequest(
    @Size(max = 1000, message = "Notes must not exceed 1000 characters") String notes) {}
