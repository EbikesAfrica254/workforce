package com.ebikes.workforce.support.fixtures;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import com.ebikes.workforce.dtos.requests.documents.ConfirmUploadRequest;
import com.ebikes.workforce.dtos.requests.documents.InitiateUploadRequest;
import com.ebikes.workforce.dtos.responses.documents.DocumentResponse;
import com.ebikes.workforce.dtos.responses.documents.UploadInitiationResponse;
import com.ebikes.workforce.enums.DocumentStatus;
import com.ebikes.workforce.enums.DocumentType;

public final class DocumentDtoFixtures {

  private DocumentDtoFixtures() {}

  public static ConfirmUploadRequest confirmUploadRequest() {
    return new ConfirmUploadRequest(
        java.time.LocalDate.now().plusYears(1), 1024L, "application/pdf");
  }

  public static DocumentResponse documentResponse() {
    return new DocumentResponse(
        DocumentFixtures.DOCUMENT_ID,
        OffsetDateTime.now(),
        DocumentType.NATIONAL_ID_FRONT,
        "test-file.pdf",
        1024L,
        "documents/NATIONAL_ID_FRONT/test-file.pdf",
        "application/pdf",
        DocumentStatus.UPLOADED,
        OffsetDateTime.now(),
        0L);
  }

  public static InitiateUploadRequest initiateUploadRequest() {
    return new InitiateUploadRequest(
        "application/pdf", DocumentType.NATIONAL_ID_FRONT, "test-file.pdf");
  }

  public static UploadInitiationResponse uploadInitiationResponse() {
    return new UploadInitiationResponse(
        DocumentFixtures.DOCUMENT_ID,
        Instant.now().plusSeconds(900),
        "documents/NATIONAL_ID_FRONT/test-file.pdf",
        Map.of("Content-Type", List.of("application/pdf")),
        "https://s3.example.com/upload-url");
  }
}
