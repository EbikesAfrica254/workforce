package com.ebikes.workforce.controllers;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import com.ebikes.workforce.services.documents.DocumentService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/documents")
@RestController
public class DocumentController {

  private final DocumentService documentService;

  @PostMapping("/initiate-upload")
  public ResponseEntity<SuccessResponse<UploadInitiationResponse>> initiateUpload(
      @Valid @RequestBody InitiateUploadRequest request) {
    UploadInitiationResponse response = documentService.initiateUpload(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(SuccessResponse.of(response, "Upload initiated successfully."));
  }

  @GetMapping("/capability-classes/{capabilityClass}/required-documents")
  public ResponseEntity<SuccessResponse<Set<DocumentType>>> getRequiredDocuments(
      @PathVariable CapabilityClass capabilityClass) {
    return ResponseEntity.ok(SuccessResponse.of(capabilityClass.getRequiredDocuments()));
  }

  @PutMapping("/{id}/confirm-upload")
  public ResponseEntity<SuccessResponse<DocumentResponse>> confirmUpload(
      @PathVariable UUID id, @Valid @RequestBody ConfirmUploadRequest request) {
    DocumentResponse response = documentService.confirmUpload(id, request);
    return ResponseEntity.ok(SuccessResponse.of(response, "Document upload confirmed."));
  }

  @GetMapping("/{id}/download")
  public ResponseEntity<SuccessResponse<String>> download(@PathVariable UUID id) {
    String downloadUrl = documentService.download(id);
    return ResponseEntity.ok(SuccessResponse.of(downloadUrl, "Download URL generated"));
  }

  @PostMapping("/{id}/replace")
  public ResponseEntity<SuccessResponse<UploadInitiationResponse>> replaceDocument(
      @PathVariable UUID id, @Valid @RequestBody InitiateUploadRequest request) {
    UploadInitiationResponse response =
        documentService.replaceDocument(id, request.fileName(), request.contentType());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(SuccessResponse.of(response, "Document replacement initiated"));
  }

  @PutMapping("/{id}/confirm-replacement")
  public ResponseEntity<SuccessResponse<DocumentResponse>> confirmReplacement(
      @PathVariable UUID id, @Valid @RequestBody ConfirmUploadRequest request) {
    DocumentResponse response = documentService.confirmReplacement(id, request);
    return ResponseEntity.ok(
        SuccessResponse.of(response, "Document replacement confirmed and submitted for approval"));
  }
}
