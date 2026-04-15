package com.ebikes.workforce.services.agents.document;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.workforce.configurations.properties.AwsProperties;
import com.ebikes.workforce.database.entities.Document;
import com.ebikes.workforce.dtos.internal.UploadUrlData;
import com.ebikes.workforce.dtos.requests.documents.ConfirmUploadRequest;
import com.ebikes.workforce.dtos.requests.documents.InitiateUploadRequest;
import com.ebikes.workforce.dtos.responses.documents.DocumentResponse;
import com.ebikes.workforce.dtos.responses.documents.UploadInitiationResponse;
import com.ebikes.workforce.mappers.DocumentMapper;
import com.ebikes.workforce.services.agents.AccessPolicy;
import com.ebikes.workforce.services.storage.StorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class IOService {

  private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;

  private final AccessPolicy accessPolicy;
  private final AwsProperties awsProperties;
  private final DocumentMapper documentMapper;
  private final DocumentService documentService;
  private final StorageService storageService;

  @Transactional
  public DocumentResponse confirmReplacement(UUID documentId, ConfirmUploadRequest request) {
    log.info("Confirming document replacement upload: documentId={}", documentId);

    Document document = documentService.requireById(documentId);
    accessPolicy.owns(document.getAgent());
    documentService.validateIsReplacementDocument(document);
    document.markUploaded(request.fileSizeBytes(), request.mimeType(), request.expiryDate());

    Document saved = documentService.save(document);

    documentService.publishReplacementPendingApproval(saved);

    log.info("Document replacement upload confirmed, pending approval: documentId={}", documentId);

    return documentMapper.toResponse(saved);
  }

  @Transactional
  public DocumentResponse confirmUpload(UUID documentId, ConfirmUploadRequest request) {
    log.info("Confirming document upload: documentId={}", documentId);

    Document document = documentService.requireById(documentId);
    accessPolicy.owns(document.getAgent());
    document.markUploaded(request.fileSizeBytes(), request.mimeType(), request.expiryDate());

    return documentMapper.toResponse(documentService.save(document));
  }

  @Transactional(readOnly = true)
  public String download(UUID documentId) {
    log.info("Generating download URL: documentId={}", documentId);

    Document document = documentService.requireById(documentId);
    accessPolicy.owns(document.getAgent());

    return storageService.generateDownloadUrl(document.getFileStorageUrl(), document.getFileName());
  }

  @Transactional
  public UploadInitiationResponse initiateUpload(InitiateUploadRequest request) {
    log.info(
        "Initiating document upload: documentType={}, fileName={}",
        request.documentType(),
        request.fileName());

    documentService.validateContentType(request.documentType(), request.contentType());

    Document document =
        Document.create(request.documentType(), request.fileName(), request.contentType());
    document = documentService.save(document);

    String key =
        storageService.generateKey(request.documentType(), document.getId(), request.fileName());
    document.assignStorageKey(key);
    Document saved = documentService.save(document);

    long maxBytes = awsProperties.getS3().getMaxFileSizeMb() * BYTES_PER_MEGABYTE;

    UploadUrlData uploadData =
        storageService.generateUploadUrl(
            key,
            request.contentType(),
            maxBytes,
            Map.of(
                "documentId", saved.getId().toString(),
                "documentType", request.documentType().name()));

    log.info("Upload initiated: documentId={}, key={}", saved.getId(), key);

    return documentMapper.toUploadInitiationResponse(saved.getId(), key, uploadData);
  }

  @Transactional
  public UploadInitiationResponse replaceDocument(
      UUID documentId, String fileName, String contentType) {
    log.info("Initiating document replacement: documentId={}, fileName={}", documentId, fileName);

    Document oldDocument = documentService.requireById(documentId);
    accessPolicy.owns(oldDocument.getAgent());
    documentService.validateContentType(oldDocument.getDocumentType(), contentType);

    Document newDocument = Document.create(oldDocument.getDocumentType(), fileName, contentType);
    newDocument = documentService.save(newDocument);

    String key =
        storageService.generateKey(oldDocument.getDocumentType(), newDocument.getId(), fileName);
    newDocument.assignStorageKey(key);
    newDocument.designateAsReplacementFor(oldDocument);
    documentService.save(newDocument);

    long maxBytes = awsProperties.getS3().getMaxFileSizeMb() * BYTES_PER_MEGABYTE;

    UploadUrlData uploadData =
        storageService.generateUploadUrl(
            key,
            contentType,
            maxBytes,
            Map.of(
                "agentId", oldDocument.getAgent().getId().toString(),
                "documentId", newDocument.getId().toString(),
                "documentType", oldDocument.getDocumentType().name(),
                "replacesDocumentId", oldDocument.getId().toString()));

    log.info(
        "Replacement initiated: oldDocumentId={}, newDocumentId={}, key={}",
        documentId,
        newDocument.getId(),
        key);

    return documentMapper.toUploadInitiationResponse(newDocument.getId(), key, uploadData);
  }
}
