package com.ebikes.workforce.dtos.requests.paymentmethods;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePaymentMethodRequest(
    @NotBlank(message = "Account name is required") @Size(max = 255, message = "Account name must not exceed 255 characters") String accountName) {}
