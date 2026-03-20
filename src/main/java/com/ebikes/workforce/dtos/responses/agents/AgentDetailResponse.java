package com.ebikes.workforce.dtos.responses.agents;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.enums.LocationSource;
import com.ebikes.workforce.enums.NationalIdType;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentDetailResponse(
    String alternatePhoneNumber,
    AvailabilityStatus availabilityStatus,
    CapabilityClass capabilityClass,
    Integer completedDeliveryCount,
    OffsetDateTime createdAt,
    String createdBy,
    String currentH3Index,
    BigDecimal currentLatitude,
    BigDecimal currentLongitude,
    String email,
    String firstName,
    UUID id,
    OffsetDateTime lastLocationUpdatedAt,
    String lastName,
    LocationSource locationSource,
    Short maxConcurrentOrders,
    String nationalIdNumber,
    NationalIdType nationalIdType,
    String phoneNumber,
    BigDecimal reliabilityScore,
    OffsetDateTime updatedAt,
    String updatedBy,
    String userId,
    Long version) {}
