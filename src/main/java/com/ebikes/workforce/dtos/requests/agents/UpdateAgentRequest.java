package com.ebikes.workforce.dtos.requests.agents;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.ebikes.workforce.enums.CapabilityClass;

public record UpdateAgentRequest(
    @Pattern(
            regexp = "^\\+[1-9]\\d{6,14}$",
            message = "Alternate phone number must be in E.164 format")
        @Size(max = 20, message = "Alternate phone number must not exceed 20 characters") String alternatePhoneNumber,
    CapabilityClass capabilityClass,
    @Email(message = "Email must be valid") @Size(max = 255, message = "Email must not exceed 255 characters") String email,
    @Positive(message = "Max concurrent orders must be positive") Short maxConcurrentOrders) {}
