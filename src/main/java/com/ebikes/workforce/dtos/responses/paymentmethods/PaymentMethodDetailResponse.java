package com.ebikes.workforce.dtos.responses.paymentmethods;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.workforce.enums.PaymentMethodType;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentMethodDetailResponse(
    String accountReference,
    String accountName,
    UUID agentId,
    OffsetDateTime createdAt,
    String createdBy,
    UUID id,
    boolean isPrimary,
    String paybillNumber,
    PaymentMethodType paymentMethodType,
    String phoneNumber,
    String tillNumber,
    OffsetDateTime updatedAt,
    String updatedBy,
    Long version) {}
