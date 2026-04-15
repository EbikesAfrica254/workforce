package com.ebikes.workforce.services.agents.document;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.Document;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.DocumentRepository;
import com.ebikes.workforce.dtos.responses.documents.DocumentPreviewResponse;
import com.ebikes.workforce.dtos.responses.documents.DocumentSummaryResponse;
import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.enums.DocumentStatus;
import com.ebikes.workforce.enums.DocumentType;
import com.ebikes.workforce.enums.ResponseCode;
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.exceptions.ResourceNotFoundException;
import com.ebikes.workforce.exceptions.ValidationException;
import com.ebikes.workforce.mappers.DocumentMapper;
import com.ebikes.workforce.services.agents.AccessPolicy;
import com.ebikes.workforce.services.agents.AgentScopedService;
import com.ebikes.workforce.services.storage.StorageService;
import com.ebikes.workforce.support.makerchecker.MakerCheckerTemplate;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DocumentService extends AgentScopedService {

  private static final String OPERATION_REPLACE = "REPLACE";

  private final AccessPolicy accessPolicy;
  private final DocumentMapper documentMapper;
  private final DocumentRepository documentRepository;
  private final MakerCheckerTemplate makerCheckerTemplate;
  private final StorageService storageService;

  public DocumentService(
      AccessPolicy accessPolicy,
      AgentRepository agentRepository,
      DocumentMapper documentMapper,
      DocumentRepository documentRepository,
      MakerCheckerTemplate makerCheckerTemplate,
      StorageService storageService) {
    super(agentRepository);
    this.accessPolicy = accessPolicy;
    this.documentMapper = documentMapper;
    this.documentRepository = documentRepository;
    this.makerCheckerTemplate = makerCheckerTemplate;
    this.storageService = storageService;
  }

  @Transactional
  public void activateDocuments(UUID agentId) {
    log.info("Activating documents for agent: agentId={}", agentId);

    List<Document> uploaded =
        documentRepository.findByAgentIdAndStatus(agentId, DocumentStatus.UPLOADED);

    if (uploaded.isEmpty()) {
      log.debug("No uploaded documents to activate for agent: {}", agentId);
      return;
    }

    uploaded.forEach(Document::activate);
    documentRepository.saveAll(uploaded);

    log.info("Activated {} documents for agent: {}", uploaded.size(), agentId);
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

    if (Set.copyOf(storageKeys).size() != storageKeys.size()) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_ARGUMENTS, "Duplicate document storage keys provided");
    }

    List<Document> documents = documentRepository.findAllByFileStorageUrlIn(storageKeys);

    if (documents.size() != storageKeys.size()) {
      throw new BusinessRuleException(
          ResponseCode.RESOURCE_NOT_FOUND,
          "One or more documents could not be found for association");
    }

    documents.forEach(doc -> doc.associate(agent));
    documentRepository.saveAll(documents);

    log.info("Associated {} documents with agent: agentId={}", documents.size(), agent.getId());
  }

  @Transactional(readOnly = true)
  public List<DocumentSummaryResponse> findByAgent(UUID agentId, boolean includeInactive) {
    log.info("Finding documents: agentId={}, includeInactive={}", agentId, includeInactive);

    Agent agent = requireAgentById(agentId);
    accessPolicy.owns(agent);

    List<Document> documents = resolveDocuments(agentId, includeInactive);

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

    Agent agent = requireAgentById(agentId);
    accessPolicy.owns(agent);

    List<Document> documents = resolveDocuments(agentId, includeInactive);
    Instant expiresAt = storageService.previewUrlExpiresAt();

    List<DocumentPreviewResponse> previews =
        documents.stream()
            .map(
                doc ->
                    documentMapper.toPreviewResponse(
                        doc, storageService.generatePreviewUrl(doc.getFileStorageUrl()), expiresAt))
            .toList();

    log.debug(
        "Generated {} preview URLs for agent: {}, expiresAt={}",
        previews.size(),
        agentId,
        expiresAt);

    return previews;
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

  Document save(Document document) {
    return documentRepository.save(document);
  }

  void publishReplacementPendingApproval(Document document) {
    makerCheckerTemplate.publish(
        document,
        null,
        OPERATION_REPLACE,
        List.of(),
        Map.of("replacesDocumentId", document.getReplacesDocument().getId().toString()));
  }

  void validateContentType(DocumentType documentType, String contentType) {
    boolean isAllowed =
        documentType.getSupportedContentTypes().stream()
            .anyMatch(allowed -> allowed.equalsIgnoreCase(contentType));

    if (!isAllowed) {
      throw new ValidationException(
          ResponseCode.INVALID_ARGUMENTS,
          String.format("Content type %s not allowed for %s.", contentType, documentType),
          "contentType",
          contentType);
    }
  }

  void validateIsReplacementDocument(Document document) {
    if (document.getReplacesDocument() == null) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Document " + document.getId() + " is not a replacement document");
    }
  }

  private List<Document> resolveDocuments(UUID agentId, boolean includeInactive) {
    return includeInactive
        ? documentRepository.findByAgentId(agentId)
        : documentRepository.findByAgentIdAndStatusIn(
            agentId, List.of(DocumentStatus.ACTIVE, DocumentStatus.UPLOADED));
  }
}
