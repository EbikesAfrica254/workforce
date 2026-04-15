package com.ebikes.workforce.controllers;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ebikes.workforce.dtos.requests.documents.ConfirmUploadRequest;
import com.ebikes.workforce.dtos.requests.documents.InitiateUploadRequest;
import com.ebikes.workforce.dtos.responses.api.SuccessResponse;
import com.ebikes.workforce.dtos.responses.documents.DocumentResponse;
import com.ebikes.workforce.dtos.responses.documents.UploadInitiationResponse;
import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.enums.DocumentType;
import com.ebikes.workforce.services.agents.document.IOService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/documents")
@RestController
public class DocumentController {

  private final IOService ioService;

  @PreAuthorize("hasAuthority('AGENT')")
  @PutMapping("/{id}/confirm-replacement")
  public ResponseEntity<SuccessResponse<DocumentResponse>> confirmReplacement(
      @PathVariable UUID id, @Valid @RequestBody ConfirmUploadRequest request) {
    return ResponseEntity.ok(
        SuccessResponse.of(
            ioService.confirmReplacement(id, request),
            "Document replacement confirmed and submitted for approval."));
  }

  @PreAuthorize("isAuthenticated()")
  @PutMapping("/{id}/confirm-upload")
  public ResponseEntity<SuccessResponse<DocumentResponse>> confirmUpload(
      @PathVariable UUID id, @Valid @RequestBody ConfirmUploadRequest request) {
    return ResponseEntity.ok(
        SuccessResponse.of(ioService.confirmUpload(id, request), "Document upload confirmed."));
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{id}/download")
  public ResponseEntity<SuccessResponse<String>> download(@PathVariable UUID id) {
    return ResponseEntity.ok(SuccessResponse.of(ioService.download(id), "Download URL generated."));
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/workforce-classes/{workforceClass}/required-documents")
  public ResponseEntity<SuccessResponse<Set<DocumentType>>> getRequiredDocuments(
      @PathVariable CapabilityClass workforceClass) {
    return ResponseEntity.ok(SuccessResponse.of(workforceClass.getRequiredDocuments()));
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping("/initiate-upload")
  public ResponseEntity<SuccessResponse<UploadInitiationResponse>> initiateUpload(
      @Valid @RequestBody InitiateUploadRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            SuccessResponse.of(
                ioService.initiateUpload(request), "Upload initiated successfully."));
  }

  @PreAuthorize("hasAuthority('AGENT')")
  @PostMapping("/{id}/replace")
  public ResponseEntity<SuccessResponse<UploadInitiationResponse>> replaceDocument(
      @PathVariable UUID id, @Valid @RequestBody InitiateUploadRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            SuccessResponse.of(
                ioService.replaceDocument(id, request.fileName(), request.contentType()),
                "Document replacement initiated."));
  }
}
