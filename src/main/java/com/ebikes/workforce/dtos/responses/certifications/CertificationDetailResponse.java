package com.ebikes.workforce.dtos.responses.certifications;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.workforce.enums.CertificationType;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CertificationDetailResponse(
    UUID agentId,
    CertificationType certificationType,
    OffsetDateTime createdAt,
    String createdBy,
    LocalDate expiresAt,
    UUID id,
    boolean isExpired,
    LocalDate issuedAt,
    String issuedBy,
    String notes,
    String referenceNumber) {}
