package com.ebikes.workforce.dtos.responses.documents;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.workforce.enums.DocumentStatus;
import com.ebikes.workforce.enums.DocumentType;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentSummaryResponse(
    DocumentType documentType,
    LocalDate expiryDate,
    String fileName,
    UUID id,
    DocumentStatus status,
    OffsetDateTime uploadedAt) {}
