package com.ebikes.workforce.services.agents.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.Document;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.DocumentRepository;
import com.ebikes.workforce.dtos.responses.documents.DocumentSummaryResponse;
import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.enums.DocumentStatus;
import com.ebikes.workforce.enums.DocumentType;
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.exceptions.ResourceNotFoundException;
import com.ebikes.workforce.exceptions.ValidationException;
import com.ebikes.workforce.mappers.DocumentMapper;
import com.ebikes.workforce.services.agents.AccessPolicy;
import com.ebikes.workforce.services.storage.StorageService;
import com.ebikes.workforce.support.fixtures.AgentFixtures;
import com.ebikes.workforce.support.fixtures.DocumentFixtures;
import com.ebikes.workforce.support.infrastructure.WithExecutionContext;
import com.ebikes.workforce.support.makerchecker.MakerCheckerTemplate;

@DisplayName("DocumentService")
@ExtendWith({MockitoExtension.class, WithExecutionContext.class})
class DocumentServiceTest {

  @Mock private AccessPolicy accessPolicy;
  @Mock private AgentRepository agentRepository;
  @Mock private DocumentMapper documentMapper;
  @Mock private DocumentRepository documentRepository;
  @Mock private MakerCheckerTemplate makerCheckerTemplate;
  @Mock private StorageService storageService;

  @InjectMocks private DocumentService documentService;

  @Nested
  @DisplayName("activateDocuments")
  class ActivateDocuments {

    @Test
    @DisplayName("should do nothing when no uploaded documents found")
    void shouldDoNothingWhenNoUploadedDocumentsFound() {
      when(documentRepository.findByAgentIdAndStatus(
              AgentFixtures.AGENT_ID, DocumentStatus.UPLOADED))
          .thenReturn(List.of());

      documentService.activateDocuments(AgentFixtures.AGENT_ID);

      verify(documentRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("should activate and save all uploaded documents")
    void shouldActivateAndSaveAllUploadedDocuments() {
      Agent agent = AgentFixtures.available();
      Document document = DocumentFixtures.uploaded(agent, DocumentType.NATIONAL_ID_FRONT);
      when(documentRepository.findByAgentIdAndStatus(
              AgentFixtures.AGENT_ID, DocumentStatus.UPLOADED))
          .thenReturn(List.of(document));

      documentService.activateDocuments(AgentFixtures.AGENT_ID);

      assertThat(document.getStatus()).isEqualTo(DocumentStatus.ACTIVE);
      verify(documentRepository).saveAll(List.of(document));
    }
  }

  @Nested
  @DisplayName("associateWithAgent")
  class AssociateWithAgent {

    @Test
    @DisplayName("should throw when storage keys list is empty")
    void shouldThrowWhenStorageKeysListIsEmpty() {
      Agent agent = AgentFixtures.available();

      assertThatThrownBy(() -> documentService.associateWithAgent(List.of(), agent))
          .isInstanceOf(BusinessRuleException.class);
      verify(documentRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("should throw when duplicate storage keys provided")
    void shouldThrowWhenDuplicateStorageKeysProvided() {
      Agent agent = AgentFixtures.available();
      List<String> keys = List.of("key-1", "key-1");

      assertThatThrownBy(() -> documentService.associateWithAgent(keys, agent))
          .isInstanceOf(BusinessRuleException.class);
      verify(documentRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("should throw when documents not found for provided keys")
    void shouldThrowWhenDocumentsNotFoundForKeys() {
      Agent agent = AgentFixtures.available();
      List<String> keys = List.of("key-1", "key-2");
      when(documentRepository.findAllByFileStorageUrlIn(keys)).thenReturn(List.of());

      assertThatThrownBy(() -> documentService.associateWithAgent(keys, agent))
          .isInstanceOf(BusinessRuleException.class);
      verify(documentRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("should associate documents with agent and save")
    void shouldAssociateDocumentsWithAgent() {
      Agent agent = AgentFixtures.available();
      Document doc1 = DocumentFixtures.uploaded(agent, DocumentType.NATIONAL_ID_FRONT);
      Document doc2 = DocumentFixtures.uploaded(agent, DocumentType.NATIONAL_ID_BACK);
      List<String> keys = List.of("key-1", "key-2");
      when(documentRepository.findAllByFileStorageUrlIn(keys)).thenReturn(List.of(doc1, doc2));

      documentService.associateWithAgent(keys, agent);

      verify(documentRepository).saveAll(List.of(doc1, doc2));
    }
  }

  @Nested
  @DisplayName("findByAgent")
  class FindByAgent {

    @Test
    @DisplayName("should throw ResourceNotFoundException when agent not found")
    void shouldThrowWhenAgentNotFound() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> documentService.findByAgent(AgentFixtures.AGENT_ID, false))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should return document summaries for agent")
    void shouldReturnDocumentSummaries() {
      Agent agent = AgentFixtures.available();
      Document document = DocumentFixtures.activeValid(agent, DocumentType.NATIONAL_ID_FRONT);
      DocumentSummaryResponse summary =
          new DocumentSummaryResponse(
              DocumentType.NATIONAL_ID_FRONT,
              null,
              "test-file.pdf",
              DocumentFixtures.DOCUMENT_ID,
              DocumentStatus.ACTIVE,
              null);
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(documentRepository.findByAgentIdAndStatusIn(
              AgentFixtures.AGENT_ID, List.of(DocumentStatus.ACTIVE, DocumentStatus.UPLOADED)))
          .thenReturn(List.of(document));
      when(documentMapper.toSummaryResponse(document)).thenReturn(summary);

      List<DocumentSummaryResponse> result =
          documentService.findByAgent(AgentFixtures.AGENT_ID, false);

      assertThat(result).hasSize(1);
    }
  }

  @Nested
  @DisplayName("validateRequiredDocumentsUploaded")
  class ValidateRequiredDocumentsUploaded {

    @Test
    @DisplayName("should throw BusinessRuleException when required documents are missing")
    void shouldThrowWhenRequiredDocumentsMissing() {
      when(documentRepository.findByAgentIdAndStatusIn(
              AgentFixtures.AGENT_ID, List.of(DocumentStatus.ACTIVE, DocumentStatus.UPLOADED)))
          .thenReturn(List.of());

      assertThatThrownBy(
              () ->
                  documentService.validateRequiredDocumentsUploaded(
                      AgentFixtures.AGENT_ID, CapabilityClass.BICYCLE_RIDER))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should pass when all required documents are present")
    void shouldPassWhenAllRequiredDocumentsPresent() {
      Agent agent = AgentFixtures.available();
      Document front = DocumentFixtures.activeValid(agent, DocumentType.NATIONAL_ID_FRONT);
      Document back = DocumentFixtures.activeValid(agent, DocumentType.NATIONAL_ID_BACK);
      when(documentRepository.findByAgentIdAndStatusIn(
              AgentFixtures.AGENT_ID, List.of(DocumentStatus.ACTIVE, DocumentStatus.UPLOADED)))
          .thenReturn(List.of(front, back));

      documentService.validateRequiredDocumentsUploaded(
          AgentFixtures.AGENT_ID, CapabilityClass.BICYCLE_RIDER);

      verify(documentRepository).findByAgentIdAndStatusIn(any(), any());
    }
  }

  @Nested
  @DisplayName("requireById")
  class RequireById {

    @Test
    @DisplayName("should throw ResourceNotFoundException when document not found")
    void shouldThrowWhenDocumentNotFound() {
      UUID documentId = UUID.randomUUID();
      when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> documentService.requireById(documentId))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should return document when found")
    void shouldReturnDocumentWhenFound() {
      Agent agent = AgentFixtures.available();
      Document document = DocumentFixtures.activeValid(agent, DocumentType.NATIONAL_ID_FRONT);
      UUID documentId = UUID.randomUUID();
      when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

      assertThat(documentService.requireById(documentId)).isEqualTo(document);
    }
  }

  @Nested
  @DisplayName("validateIsReplacementDocument")
  class ValidateIsReplacementDocument {

    @Test
    @DisplayName("should throw BusinessRuleException when replacesDocument is null")
    void shouldThrowWhenReplacesDocumentIsNull() {
      Agent agent = AgentFixtures.available();
      Document document = DocumentFixtures.uploaded(agent, DocumentType.NATIONAL_ID_FRONT);

      assertThatThrownBy(() -> documentService.validateIsReplacementDocument(document))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should pass when replacesDocument is set")
    void shouldPassWhenReplacesDocumentIsSet() {
      Agent agent = AgentFixtures.available();
      Document oldDocument = DocumentFixtures.activeValid(agent, DocumentType.NATIONAL_ID_FRONT);
      Document newDocument = DocumentFixtures.uploaded(agent, DocumentType.NATIONAL_ID_FRONT);
      newDocument.designateAsReplacementFor(oldDocument);

      documentService.validateIsReplacementDocument(newDocument);
    }
  }

  @Nested
  @DisplayName("validateContentType")
  class ValidateContentType {

    @Test
    @DisplayName("should throw ValidationException when content type is not allowed")
    void shouldThrowWhenContentTypeNotAllowed() {
      assertThatThrownBy(
              () ->
                  documentService.validateContentType(
                      DocumentType.NATIONAL_ID_FRONT, "application/exe"))
          .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("should pass when content type is allowed")
    void shouldPassWhenContentTypeAllowed() {
      documentService.validateContentType(DocumentType.NATIONAL_ID_FRONT, "application/pdf");
    }
  }
}
