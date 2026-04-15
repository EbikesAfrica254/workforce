package com.ebikes.workforce.dtos.responses.documents;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.workforce.enums.DocumentStatus;
import com.ebikes.workforce.enums.DocumentType;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentResponse(
    UUID id,
    OffsetDateTime createdAt,
    DocumentType documentType,
    String fileName,
    Long fileSizeBytes,
    String fileStorageUrl,
    String mimeType,
    DocumentStatus status,
    OffsetDateTime uploadedAt,
    Long version) {}
