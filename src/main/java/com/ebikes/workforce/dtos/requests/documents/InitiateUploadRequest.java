package com.ebikes.workforce.dtos.requests.documents;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.ebikes.workforce.enums.DocumentType;

public record InitiateUploadRequest(
    @NotBlank String contentType, @NotNull DocumentType documentType, @NotBlank String fileName) {}
