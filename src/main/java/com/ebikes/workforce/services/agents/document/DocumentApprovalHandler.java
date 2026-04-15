package com.ebikes.workforce.services.agents.document;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.workforce.constants.EventConstants.DomainEvents;
import com.ebikes.workforce.database.entities.Document;
import com.ebikes.workforce.database.repositories.DocumentRepository;
import com.ebikes.workforce.dtos.events.incoming.MakerCheckerDecision;
import com.ebikes.workforce.enums.CheckerOutcome;
import com.ebikes.workforce.support.audit.AuditTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class DocumentApprovalHandler {

  private final AuditTemplate auditTemplate;
  private final DocumentRepository documentRepository;
  private final DocumentService documentService;

  @Transactional
  public void handleDecision(MakerCheckerDecision decision) {
    log.info(
        "Processing document replacement decision: entityId={}, outcome={}",
        decision.entityId(),
        decision.outcome());

    Document newDocument = documentService.requireById(decision.entityId());
    documentService.validateIsReplacementDocument(newDocument);

    if (decision.outcome() == CheckerOutcome.APPROVED) {
      approve(newDocument);
    } else if (decision.outcome() == CheckerOutcome.REJECTED) {
      reject(newDocument);
    }
  }

  private void approve(Document newDocument) {
    Document oldDocument = newDocument.getReplacesDocument();

    auditTemplate.execute(
        newDocument,
        null,
        DomainEvents.Documents.REPLACED,
        () -> {
          oldDocument.markReplaced();
          newDocument.activate();
          documentRepository.save(oldDocument);
          documentRepository.save(newDocument);
        });

    log.info(
        "Document replacement approved: newDocumentId={}, oldDocumentId={}",
        newDocument.getId(),
        oldDocument.getId());
  }

  private void reject(Document newDocument) {
    newDocument.reject();
    documentRepository.save(newDocument);

    log.info("Document replacement rejected: newDocumentId={}", newDocument.getId());
  }
}
