package com.ebikes.workforce.dtos.requests.paymentmethods;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.ebikes.workforce.enums.PaymentMethodType;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CreatePaymentMethodRequest(
    @Size(max = 100, message = "Account reference must not exceed 100 characters") String accountReference,
    @JsonProperty("isPrimary") boolean isPrimary,
    @NotBlank(message = "Account name is required") @Size(max = 255, message = "Account name must not exceed 255 characters") String accountName,
    @Pattern(regexp = "^\\d{4,8}$", message = "Paybill number must be 4-8 digits") String paybillNumber,
    @NotNull(message = "Payment method type is required") PaymentMethodType paymentMethodType,
    @Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = "Phone number must be in E.164 format") @Size(max = 20, message = "Phone number must not exceed 20 characters") String phoneNumber,
    @Pattern(regexp = "^\\d{4,8}$", message = "Till number must be 4-8 digits") String tillNumber) {}
