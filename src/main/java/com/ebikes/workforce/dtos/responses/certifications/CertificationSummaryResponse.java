package com.ebikes.workforce.dtos.responses.certifications;

import java.time.LocalDate;
import java.util.UUID;

import com.ebikes.workforce.enums.CertificationType;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CertificationSummaryResponse(
    UUID agentId,
    CertificationType certificationType,
    LocalDate expiresAt,
    UUID id,
    boolean isExpired,
    LocalDate issuedAt) {}
