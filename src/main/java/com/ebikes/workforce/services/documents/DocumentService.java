package com.ebikes.workforce.services.documents;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.workforce.configurations.properties.AwsProperties;
import com.ebikes.workforce.constants.EventConstants.EventSource;
import com.ebikes.workforce.constants.EventConstants.EventTypes;
import com.ebikes.workforce.constants.EventConstants.RoutingKeys;
import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.Document;
import com.ebikes.workforce.database.repositories.DocumentRepository;
import com.ebikes.workforce.dtos.events.incoming.MakerCheckerDecision;
import com.ebikes.workforce.dtos.events.outgoing.MakerCheckerRequest;
import com.ebikes.workforce.dtos.internal.UploadUrlData;
import com.ebikes.workforce.dtos.requests.documents.ConfirmUploadRequest;
import com.ebikes.workforce.dtos.requests.documents.InitiateUploadRequest;
import com.ebikes.workforce.dtos.responses.documents.DocumentPreviewResponse;
import com.ebikes.workforce.dtos.responses.documents.DocumentResponse;
import com.ebikes.workforce.dtos.responses.documents.DocumentSummaryResponse;
import com.ebikes.workforce.dtos.responses.documents.UploadInitiationResponse;
import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.enums.CheckerOutcome;
import com.ebikes.workforce.enums.DocumentStatus;
import com.ebikes.workforce.enums.DocumentType;
import com.ebikes.workforce.enums.ResponseCode;
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.exceptions.ResourceNotFoundException;
import com.ebikes.workforce.exceptions.ValidationException;
import com.ebikes.workforce.mappers.DocumentMapper;
import com.ebikes.workforce.publishers.AuditEventPublisher;
import com.ebikes.workforce.publishers.MakerCheckerPublisher;
import com.ebikes.workforce.services.storage.StorageService;
import com.ebikes.workforce.support.changes.MakerCheckerRequestBuilder;
import com.ebikes.workforce.support.context.ExecutionContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class DocumentService {

  private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;

  private final AuditEventPublisher auditEventPublisher;
  private final AwsProperties awsProperties;
  private final DocumentMapper documentMapper;
  private final DocumentRepository documentRepository;
  private final MakerCheckerPublisher makerCheckerPublisher;
  private final StorageService storageService;

  @Transactional
  public void activateDocuments(UUID agentId) {
    log.info("Activating documents for agent: agentId={}", agentId);

    List<Document> uploadedDocuments =
        documentRepository.findByAgentIdAndStatus(agentId, DocumentStatus.UPLOADED);

    if (uploadedDocuments.isEmpty()) {
      log.debug("No uploaded documents to activate for agent: {}", agentId);
      return;
    }

    uploadedDocuments.forEach(Document::activate);
    documentRepository.saveAll(uploadedDocuments);

    log.info("Activated {} documents for agent: {}", uploadedDocuments.size(), agentId);
  }

  @Transactional
  public void associateWithAgent(List<String> storageKeys, Agent agent) {
    log.info(
        "Associating documents with agent: agentId={}, documentCount={}",
        agent.getId(),
        storageKeys.size());

    if (storageKeys.isEmpty()) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_ARGUMENTS, "At least one document is required for association");
    }

    Set<String> uniqueStorageKeys = Set.copyOf(storageKeys);
    if (uniqueStorageKeys.size() != storageKeys.size()) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_ARGUMENTS, "Duplicate document storage keys provided");
    }

    List<Document> documents = documentRepository.findAllByFileStorageUrlIn(storageKeys);

    if (documents.size() != storageKeys.size()) {
      throw new BusinessRuleException(
          ResponseCode.RESOURCE_NOT_FOUND,
          "One or more documents could not be found for association");
    }

    documents.forEach(
        document -> {
          validateDocumentStatus(
              document, List.of(DocumentStatus.PENDING, DocumentStatus.UPLOADED), "associate");
          document.associate(agent);
        });

    List<Document> savedDocuments = documentRepository.saveAll(documents);

    log.info(
        "Associated {} documents with agent: agentId={}", savedDocuments.size(), agent.getId());
  }

  @Transactional
  public DocumentResponse confirmReplacement(UUID documentId, ConfirmUploadRequest request) {
    log.info("Confirming document replacement upload: documentId={}", documentId);

    Document newDocument = requireById(documentId);
    validateIsReplacementDocument(newDocument);
    validateDocumentStatus(newDocument, List.of(DocumentStatus.PENDING), "confirm replacement");
    newDocument.markUploaded(request.fileSizeBytes(), request.mimeType(), request.expiryDate());

    Document saved = documentRepository.save(newDocument);

    MakerCheckerRequest makerCheckerRequest =
        MakerCheckerRequestBuilder.forDocumentReplacement(
            saved, saved.getReplacesDocument(), ExecutionContext.getUserId());

    makerCheckerPublisher.publish(
        makerCheckerRequest, RoutingKeys.makerCheckerRequest(EventSource.HOST, "document"));

    log.info("Document replacement upload confirmed, pending approval: documentId={}", documentId);

    return documentMapper.toResponse(saved);
  }

  @Transactional
  public DocumentResponse confirmUpload(UUID documentId, ConfirmUploadRequest request) {
    log.info("Confirming document upload: documentId={}", documentId);

    Document document = requireById(documentId);
    validateDocumentStatus(document, List.of(DocumentStatus.PENDING), "confirm upload");
    document.markUploaded(request.fileSizeBytes(), request.mimeType(), request.expiryDate());
    Document saved = documentRepository.save(document);

    return documentMapper.toResponse(saved);
  }

  @Transactional(readOnly = true)
  public String download(UUID documentId) {
    log.info("Generating download URL: documentId={}", documentId);

    Document document = requireById(documentId);
    Duration ttl = Duration.ofMinutes(awsProperties.getS3().getPresignedUrlExpiryMinutes());

    return storageService.generateDownloadUrl(
        document.getFileStorageUrl(), document.getFileName(), ttl);
  }

  @Transactional(readOnly = true)
  public List<DocumentSummaryResponse> findByAgent(UUID agentId, boolean includeInactive) {
    log.info("Finding documents: agentId={}, includeInactive={}", agentId, includeInactive);

    List<Document> documents =
        includeInactive
            ? documentRepository.findByAgentId(agentId)
            : documentRepository.findByAgentIdAndStatusIn(
                agentId, List.of(DocumentStatus.ACTIVE, DocumentStatus.UPLOADED));

    log.debug("Found {} documents for agent: {}", documents.size(), agentId);

    return documents.stream().map(documentMapper::toSummaryResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<DocumentPreviewResponse> findByAgentWithPreviews(
      UUID agentId, boolean includeInactive) {
    log.info(
        "Finding documents with preview URLs: agentId={}, includeInactive={}",
        agentId,
        includeInactive);

    List<Document> documents =
        includeInactive
            ? documentRepository.findByAgentId(agentId)
            : documentRepository.findByAgentIdAndStatusIn(
                agentId, List.of(DocumentStatus.ACTIVE, DocumentStatus.UPLOADED));

    Duration previewTtl = Duration.ofMinutes(awsProperties.getS3().getPreviewUrlExpiryMinutes());
    Instant expiresAt = Instant.now().plus(previewTtl);

    List<DocumentPreviewResponse> previews =
        documents.stream()
            .map(
                doc -> {
                  String previewUrl =
                      storageService.generatePreviewUrl(doc.getFileStorageUrl(), previewTtl);
                  return documentMapper.toPreviewResponse(doc, previewUrl, expiresAt);
                })
            .toList();

    log.debug(
        "Generated {} preview URLs for agent: {}, expiresAt={}",
        previews.size(),
        agentId,
        expiresAt);

    return previews;
  }

  @Transactional
  public void handleReplacementDecision(MakerCheckerDecision decision) {
    log.info(
        "Processing document replacement decision: entityId={}, outcome={}",
        decision.entityId(),
        decision.outcome());

    Document newDocument = requireById(decision.entityId());
    validateIsReplacementDocument(newDocument);

    if (Objects.requireNonNull(decision.outcome()) == CheckerOutcome.APPROVED) {
      recordReplacementApproval(newDocument);
    } else if (decision.outcome() == CheckerOutcome.REJECTED) {
      recordReplacementRejection(newDocument, decision.reason());
    }
  }

  @Transactional
  public UploadInitiationResponse initiateUpload(InitiateUploadRequest request) {
    log.info(
        "Initiating document upload: documentType={}, fileName={}",
        request.documentType(),
        request.fileName());

    validateContentType(request.documentType(), request.contentType());

    long maxFileSizeBytes = awsProperties.getS3().getMaxFileSizeMb() * BYTES_PER_MEGABYTE;
    String sanitizedFileName = storageService.sanitizeFilename(request.fileName());

    Document document =
        new Document(request.documentType(), request.fileName(), request.contentType());

    document = documentRepository.save(document);

    String key = generateDocumentKey(request.documentType(), document.getId(), sanitizedFileName);
    document.assignStorageKey(key);
    Document saved = documentRepository.save(document);

    UploadUrlData uploadData =
        storageService.generateUploadUrl(
            key,
            request.contentType(),
            maxFileSizeBytes,
            Map.of(
                "documentId", saved.getId().toString(),
                "documentType", request.documentType().name()));

    log.info("Upload initiated: documentId={}, key={}", saved.getId(), key);

    return documentMapper.toUploadInitiationResponse(document.getId(), key, uploadData);
  }

  @Transactional
  public UploadInitiationResponse replaceDocument(
      UUID documentId, String fileName, String contentType) {
    log.info("Initiating document replacement: documentId={}, fileName={}", documentId, fileName);

    Document oldDocument = requireById(documentId);
    validateDocumentStatus(
        oldDocument, List.of(DocumentStatus.ACTIVE, DocumentStatus.EXPIRED), "replace");
    validateContentType(oldDocument.getDocumentType(), contentType);

    long maxFileSizeBytes = awsProperties.getS3().getMaxFileSizeMb() * BYTES_PER_MEGABYTE;
    String sanitizedFileName = storageService.sanitizeFilename(fileName);

    Document newDocument = new Document(oldDocument.getDocumentType(), fileName, contentType);

    newDocument = documentRepository.save(newDocument);

    String key =
        generateDocumentKey(oldDocument.getDocumentType(), newDocument.getId(), sanitizedFileName);

    newDocument.assignStorageKey(key);
    newDocument.designateAsReplacementFor(oldDocument);

    documentRepository.save(newDocument);

    UploadUrlData uploadData =
        storageService.generateUploadUrl(
            key,
            contentType,
            maxFileSizeBytes,
            Map.of(
                "documentId", newDocument.getId().toString(),
                "documentType", oldDocument.getDocumentType().name(),
                "agentId", oldDocument.getAgent().getId().toString(),
                "replacesDocumentId", oldDocument.getId().toString()));

    log.info(
        "Replacement initiated: oldDocumentId={}, newDocumentId={}, key={}",
        documentId,
        newDocument.getId(),
        key);

    return new UploadInitiationResponse(
        newDocument.getId(), uploadData.expiryTime(), key, uploadData.headers(), uploadData.url());
  }

  @Transactional(readOnly = true)
  public void validateRequiredDocumentsUploaded(UUID agentId, CapabilityClass capabilityClass) {
    log.info(
        "Validating required documents: agentId={}, capabilityClass={}", agentId, capabilityClass);

    Set<DocumentType> required = capabilityClass.getRequiredDocuments();

    Set<DocumentType> present =
        documentRepository
            .findByAgentIdAndStatusIn(
                agentId, List.of(DocumentStatus.ACTIVE, DocumentStatus.UPLOADED))
            .stream()
            .map(Document::getDocumentType)
            .collect(Collectors.toSet());

    Set<DocumentType> missing = new HashSet<>(required);
    missing.removeAll(present);

    if (!missing.isEmpty()) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          String.format("Missing required documents for agent %s: %s", agentId, missing));
    }

    log.debug("All required documents validated for agent: {}", agentId);
  }

  Document requireById(UUID documentId) {
    return documentRepository
        .findById(documentId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND, "Document not found with ID: " + documentId));
  }

  private String generateDocumentKey(DocumentType documentType, UUID documentId, String fileName) {
    return String.format(
        "documents/%s/%s-%s-%s", documentType, documentId, System.currentTimeMillis(), fileName);
  }

  private void recordReplacementApproval(Document newDocument) {
    Document oldDocument = newDocument.getReplacesDocument();
    oldDocument.markReplaced();
    newDocument.activate();
    documentRepository.save(oldDocument);
    documentRepository.save(newDocument);

    auditEventPublisher.publishSuccess(
        newDocument.getId(),
        "DOCUMENT",
        EventTypes.Documents.REPLACED,
        null,
        RoutingKeys.WORKFORCE_DOCUMENT_AUDIT);

    log.info(
        "Document replacement approved: newDocumentId={}, oldDocumentId={}",
        newDocument.getId(),
        oldDocument.getId());
  }

  private void recordReplacementRejection(Document newDocument, String reason) {
    newDocument.reject();
    documentRepository.save(newDocument);

    log.info(
        "Document replacement rejected: newDocumentId={}, reason={}", newDocument.getId(), reason);
  }

  private void validateContentType(DocumentType documentType, String contentType) {
    boolean isAllowed =
        documentType.getSupportedContentTypes().stream()
            .anyMatch(allowed -> allowed.equalsIgnoreCase(contentType));

    if (!isAllowed) {
      throw new ValidationException(
          ResponseCode.INVALID_ARGUMENTS,
          String.format(
              "Content type %s not allowed for %s. Allowed types: %s",
              contentType, documentType, documentType.getSupportedContentTypes()),
          "contentType",
          contentType);
    }
  }

  private void validateDocumentStatus(
      Document document, List<DocumentStatus> expectedStatuses, String operation) {
    if (!expectedStatuses.contains(document.getStatus())) {
      throw new ValidationException(
          ResponseCode.INVALID_STATE,
          String.format(
              "Document is in invalid state for %s: expected one of %s, found %s",
              operation, expectedStatuses, document.getStatus()),
          "status",
          document.getStatus());
    }
  }

  private void validateIsReplacementDocument(Document document) {
    if (document.getReplacesDocument() == null) {
      throw new ValidationException(
          ResponseCode.INVALID_STATE,
          "Document is not a replacement document",
          "replacesDocument",
          null);
    }
  }
}
