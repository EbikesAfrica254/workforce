package com.ebikes.workforce.services.agents.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.workforce.database.entities.Document;
import com.ebikes.workforce.database.repositories.DocumentRepository;
import com.ebikes.workforce.dtos.events.incoming.MakerCheckerDecision;
import com.ebikes.workforce.enums.DocumentStatus;
import com.ebikes.workforce.enums.DocumentType;
import com.ebikes.workforce.enums.ResponseCode;
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.exceptions.ResourceNotFoundException;
import com.ebikes.workforce.support.audit.AuditTemplate;
import com.ebikes.workforce.support.audit.ThrowingRunnable;
import com.ebikes.workforce.support.fixtures.AgentFixtures;
import com.ebikes.workforce.support.fixtures.DocumentFixtures;
import com.ebikes.workforce.support.fixtures.MakerCheckerFixtures;
import com.ebikes.workforce.support.infrastructure.WithExecutionContext;

@DisplayName("DocumentApprovalHandler")
@ExtendWith({MockitoExtension.class, WithExecutionContext.class})
class DocumentApprovalHandlerTest {

  @Mock private AuditTemplate auditTemplate;
  @Mock private DocumentRepository documentRepository;
  @Mock private DocumentService documentService;

  @InjectMocks private DocumentApprovalHandler approvalHandler;

  @SuppressWarnings("unchecked")
  private void wireAuditTemplate() {
    doAnswer(
            invocation -> {
              ThrowingRunnable<?> operation = invocation.getArgument(3);
              operation.run();
              return null;
            })
        .when(auditTemplate)
        .execute(any(), any(), any(), any(ThrowingRunnable.class));
  }

  @Nested
  @DisplayName("handleDecision")
  class HandleDecision {

    @Test
    @DisplayName("should throw ResourceNotFoundException when document not found")
    void shouldThrowWhenDocumentNotFound() {
      UUID documentId = UUID.randomUUID();
      doThrow(new ResourceNotFoundException(ResponseCode.RESOURCE_NOT_FOUND, "Document not found"))
          .when(documentService)
          .requireById(documentId);

      MakerCheckerDecision decision =
          MakerCheckerFixtures.approved(documentId, "DOCUMENT", "REPLACE");
      assertThatThrownBy(() -> approvalHandler.handleDecision(decision))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw BusinessRuleException when document is not a replacement")
    void shouldThrowWhenNotAReplacementDocument() {
      UUID documentId = UUID.randomUUID();
      Document document =
          DocumentFixtures.uploaded(AgentFixtures.available(), DocumentType.NATIONAL_ID_FRONT);
      when(documentService.requireById(documentId)).thenReturn(document);
      doThrow(new BusinessRuleException(ResponseCode.INVALID_STATE, "Not a replacement document"))
          .when(documentService)
          .validateIsReplacementDocument(document);

      MakerCheckerDecision decision =
          MakerCheckerFixtures.approved(documentId, "DOCUMENT", "REPLACE");
      assertThatThrownBy(() -> approvalHandler.handleDecision(decision))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should mark old document as replaced and activate new document on APPROVED")
    void shouldApproveReplacement() {
      wireAuditTemplate();
      Document oldDocument =
          DocumentFixtures.activeValid(AgentFixtures.available(), DocumentType.NATIONAL_ID_FRONT);
      Document newDocument =
          DocumentFixtures.uploaded(AgentFixtures.available(), DocumentType.NATIONAL_ID_FRONT);
      newDocument.designateAsReplacementFor(oldDocument);
      UUID documentId = UUID.randomUUID();
      when(documentService.requireById(documentId)).thenReturn(newDocument);

      approvalHandler.handleDecision(
          MakerCheckerFixtures.approved(documentId, "DOCUMENT", "REPLACE"));

      assertThat(oldDocument.getStatus()).isEqualTo(DocumentStatus.REPLACED);
      assertThat(newDocument.getStatus()).isEqualTo(DocumentStatus.ACTIVE);
      verify(documentRepository).save(oldDocument);
      verify(documentRepository).save(newDocument);
    }

    @Test
    @DisplayName("should reject new document and not invoke audit template on REJECTED")
    @SuppressWarnings("unchecked")
    void shouldRejectReplacement() {
      Document oldDocument =
          DocumentFixtures.activeValid(AgentFixtures.available(), DocumentType.NATIONAL_ID_FRONT);
      Document newDocument =
          DocumentFixtures.uploaded(AgentFixtures.available(), DocumentType.NATIONAL_ID_FRONT);
      newDocument.designateAsReplacementFor(oldDocument);
      UUID documentId = UUID.randomUUID();
      when(documentService.requireById(documentId)).thenReturn(newDocument);

      approvalHandler.handleDecision(
          MakerCheckerFixtures.rejected(documentId, "DOCUMENT", "REPLACE", null));

      assertThat(newDocument.getStatus()).isEqualTo(DocumentStatus.REJECTED);
      verify(documentRepository).save(newDocument);
      verify(auditTemplate, never()).execute(any(), any(), any(), any(ThrowingRunnable.class));
    }
  }
}
