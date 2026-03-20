package com.ebikes.workforce.dtos.requests.documents;

import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConfirmUploadRequest(
    @FutureOrPresent LocalDate expiryDate,
    @NotNull @Min(1) Long fileSizeBytes,
    @NotBlank String mimeType) {}
