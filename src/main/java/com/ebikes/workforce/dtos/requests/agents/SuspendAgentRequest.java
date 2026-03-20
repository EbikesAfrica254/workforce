package com.ebikes.workforce.dtos.requests.agents;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SuspendAgentRequest(
    OffsetDateTime expiresAt,
    @Size(max = 1000, message = "Notes must not exceed 1000 characters") String notes,
    @NotBlank(message = "Suspension reason is required") @Size(max = 255, message = "Reason must not exceed 255 characters") String reason) {}
