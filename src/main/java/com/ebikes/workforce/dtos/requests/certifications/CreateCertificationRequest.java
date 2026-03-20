package com.ebikes.workforce.dtos.requests.certifications;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import com.ebikes.workforce.enums.CertificationType;

public record CreateCertificationRequest(
    @NotNull(message = "Certification type is required") CertificationType certificationType,
    LocalDate expiresAt,
    @NotNull(message = "Issue date is required") @PastOrPresent(message = "Issue date must not be in the future") LocalDate issuedAt,
    @NotBlank(message = "Issuing authority is required") @Size(max = 255, message = "Issuing authority must not exceed 255 characters") String issuedBy,
    @Size(max = 1000, message = "Notes must not exceed 1000 characters") String notes,
    @Size(max = 100, message = "Reference number must not exceed 100 characters") String referenceNumber) {}
