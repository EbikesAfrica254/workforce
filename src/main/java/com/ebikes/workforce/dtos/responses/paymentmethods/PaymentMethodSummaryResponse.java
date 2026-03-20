package com.ebikes.workforce.dtos.responses.paymentmethods;

import java.util.UUID;

import com.ebikes.workforce.enums.PaymentMethodType;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentMethodSummaryResponse(
    String accountName,
    UUID id,
    boolean isPrimary,
    String maskedIdentifier,
    PaymentMethodType paymentMethodType) {}
