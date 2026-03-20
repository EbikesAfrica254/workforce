package com.ebikes.workforce.dtos.responses.documents;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.workforce.enums.DocumentStatus;
import com.ebikes.workforce.enums.DocumentType;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentPreviewResponse(
    DocumentType documentType,
    LocalDate expiryDate,
    String fileName,
    UUID id,
    String mimeType,
    String previewUrl,
    Instant previewUrlExpiresAt,
    DocumentStatus status,
    OffsetDateTime uploadedAt) {}
