package com.ebikes.workforce.services.agents.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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

import com.ebikes.workforce.configurations.properties.AwsProperties;
import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.Document;
import com.ebikes.workforce.dtos.requests.documents.ConfirmUploadRequest;
import com.ebikes.workforce.dtos.requests.documents.InitiateUploadRequest;
import com.ebikes.workforce.enums.DocumentType;
import com.ebikes.workforce.enums.ResponseCode;
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.exceptions.ResourceNotFoundException;
import com.ebikes.workforce.exceptions.ValidationException;
import com.ebikes.workforce.mappers.DocumentMapper;
import com.ebikes.workforce.services.agents.AccessPolicy;
import com.ebikes.workforce.services.storage.StorageService;
import com.ebikes.workforce.support.fixtures.AgentFixtures;
import com.ebikes.workforce.support.fixtures.DocumentDtoFixtures;
import com.ebikes.workforce.support.fixtures.DocumentFixtures;
import com.ebikes.workforce.support.infrastructure.WithExecutionContext;

@DisplayName("IOService")
@ExtendWith({MockitoExtension.class, WithExecutionContext.class})
class IOServiceTest {

  @Mock private AccessPolicy accessPolicy;
  @Mock private AwsProperties awsProperties;
  @Mock private DocumentMapper documentMapper;
  @Mock private DocumentService documentService;
  @Mock private StorageService storageService;

  @InjectMocks private IOService ioService;

  @Nested
  @DisplayName("confirmReplacement")
  class ConfirmReplacement {

    @Test
    @DisplayName("should throw when document is not a replacement")
    void shouldThrowWhenNotReplacementDocument() {
      UUID documentId = UUID.randomUUID();
      Agent agent = AgentFixtures.available();
      Document document = DocumentFixtures.uploaded(agent, DocumentType.NATIONAL_ID_FRONT);
      when(documentService.requireById(documentId)).thenReturn(document);
      doThrow(new BusinessRuleException(ResponseCode.INVALID_STATE, "Not a replacement"))
          .when(documentService)
          .validateIsReplacementDocument(document);

      ConfirmUploadRequest request = DocumentDtoFixtures.confirmUploadRequest();
      assertThatThrownBy(() -> ioService.confirmReplacement(documentId, request))
          .isInstanceOf(BusinessRuleException.class);
      verify(documentService, never()).save(any());
    }

    @Test
    @DisplayName("should mark uploaded, publish pending approval and return response")
    void shouldMarkUploadedPublishAndReturnResponse() {
      UUID documentId = UUID.randomUUID();
      Agent agent = AgentFixtures.available();
      Document document = DocumentFixtures.pending(agent, DocumentType.NATIONAL_ID_FRONT);
      when(documentService.requireById(documentId)).thenReturn(document);
      when(documentService.save(document)).thenReturn(document);
      when(documentMapper.toResponse(document)).thenReturn(DocumentDtoFixtures.documentResponse());

      var result =
          ioService.confirmReplacement(documentId, DocumentDtoFixtures.confirmUploadRequest());

      assertThat(result).isNotNull();
      verify(documentService).save(document);
      verify(documentService).publishReplacementPendingApproval(document);
    }
  }

  @Nested
  @DisplayName("confirmUpload")
  class ConfirmUpload {

    @Test
    @DisplayName("should throw ResourceNotFoundException when document not found")
    void shouldThrowWhenDocumentNotFound() {
      UUID documentId = UUID.randomUUID();
      doThrow(new ResourceNotFoundException(ResponseCode.RESOURCE_NOT_FOUND, "Not found"))
          .when(documentService)
          .requireById(documentId);

      ConfirmUploadRequest request = DocumentDtoFixtures.confirmUploadRequest();
      assertThatThrownBy(() -> ioService.confirmUpload(documentId, request))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should mark uploaded, save and return response")
    void shouldMarkUploadedSaveAndReturnResponse() {
      UUID documentId = UUID.randomUUID();
      Agent agent = AgentFixtures.available();
      Document document = DocumentFixtures.pending(agent, DocumentType.NATIONAL_ID_FRONT);
      when(documentService.requireById(documentId)).thenReturn(document);
      when(documentService.save(document)).thenReturn(document);
      when(documentMapper.toResponse(document)).thenReturn(DocumentDtoFixtures.documentResponse());

      var result = ioService.confirmUpload(documentId, DocumentDtoFixtures.confirmUploadRequest());

      assertThat(result).isNotNull();
      verify(documentService).save(document);
    }
  }

  @Nested
  @DisplayName("download")
  class Download {

    @Test
    @DisplayName("should throw ResourceNotFoundException when document not found")
    void shouldThrowWhenDocumentNotFound() {
      UUID documentId = UUID.randomUUID();
      doThrow(new ResourceNotFoundException(ResponseCode.RESOURCE_NOT_FOUND, "Not found"))
          .when(documentService)
          .requireById(documentId);

      assertThatThrownBy(() -> ioService.download(documentId))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should return presigned download URL")
    void shouldReturnDownloadUrl() {
      UUID documentId = UUID.randomUUID();
      Agent agent = AgentFixtures.available();
      Document document = DocumentFixtures.activeValid(agent, DocumentType.NATIONAL_ID_FRONT);
      String expectedUrl = "https://s3.example.com/download-url";
      when(documentService.requireById(documentId)).thenReturn(document);
      when(storageService.generateDownloadUrl(document.getFileStorageUrl(), document.getFileName()))
          .thenReturn(expectedUrl);

      assertThat(ioService.download(documentId)).isEqualTo(expectedUrl);
    }
  }

  @Nested
  @DisplayName("initiateUpload")
  class InitiateUpload {

    @Test
    @DisplayName("should throw ValidationException when content type is invalid")
    void shouldThrowWhenContentTypeInvalid() {
      doThrow(
              new ValidationException(
                  ResponseCode.INVALID_ARGUMENTS,
                  "Invalid content type",
                  "contentType",
                  "application/exe"))
          .when(documentService)
          .validateContentType(DocumentType.NATIONAL_ID_FRONT, "application/pdf");

      InitiateUploadRequest request = DocumentDtoFixtures.initiateUploadRequest();
      assertThatThrownBy(() -> ioService.initiateUpload(request))
          .isInstanceOf(ValidationException.class);
      verify(documentService, never()).save(any());
    }

    @Test
    @DisplayName("should create document, assign key and return upload initiation response")
    void shouldCreateDocumentAssignKeyAndReturnResponse() {
      Agent agent = AgentFixtures.available();
      Document document = DocumentFixtures.pending(agent, DocumentType.NATIONAL_ID_FRONT);
      AwsProperties.S3 s3 = mock(AwsProperties.S3.class);
      when(awsProperties.getS3()).thenReturn(s3);
      when(s3.getMaxFileSizeMb()).thenReturn(10);
      when(documentService.save(any())).thenReturn(document);
      when(storageService.generateKey(eq(DocumentType.NATIONAL_ID_FRONT), any(), anyString()))
          .thenReturn("documents/NATIONAL_ID_FRONT/key");
      when(storageService.generateUploadUrl(anyString(), anyString(), anyLong(), anyMap()))
          .thenReturn(mock(com.ebikes.workforce.dtos.internal.UploadUrlData.class));
      when(documentMapper.toUploadInitiationResponse(any(), anyString(), any()))
          .thenReturn(DocumentDtoFixtures.uploadInitiationResponse());

      var result = ioService.initiateUpload(DocumentDtoFixtures.initiateUploadRequest());

      assertThat(result).isNotNull();
      verify(documentService)
          .validateContentType(DocumentType.NATIONAL_ID_FRONT, "application/pdf");
    }
  }

  @Nested
  @DisplayName("replaceDocument")
  class ReplaceDocument {

    @Test
    @DisplayName("should throw ResourceNotFoundException when document not found")
    void shouldThrowWhenDocumentNotFound() {
      UUID documentId = UUID.randomUUID();
      doThrow(new ResourceNotFoundException(ResponseCode.RESOURCE_NOT_FOUND, "Not found"))
          .when(documentService)
          .requireById(documentId);

      assertThatThrownBy(
              () -> ioService.replaceDocument(documentId, "new-file.pdf", "application/pdf"))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName(
        "should create replacement document, assign key and return upload initiation response")
    void shouldInitiateReplacementAndReturnResponse() {
      UUID documentId = UUID.randomUUID();
      Agent agent = AgentFixtures.available();
      Document oldDocument = DocumentFixtures.activeValid(agent, DocumentType.NATIONAL_ID_FRONT);
      Document newDocument = DocumentFixtures.pending(agent, DocumentType.NATIONAL_ID_FRONT);
      AwsProperties.S3 s3 = mock(AwsProperties.S3.class);
      when(awsProperties.getS3()).thenReturn(s3);
      when(s3.getMaxFileSizeMb()).thenReturn(10);
      when(documentService.requireById(documentId)).thenReturn(oldDocument);
      when(documentService.save(any())).thenReturn(newDocument);
      when(storageService.generateKey(
              eq(DocumentType.NATIONAL_ID_FRONT), any(), eq("new-file.pdf")))
          .thenReturn("documents/NATIONAL_ID_FRONT/key");
      when(storageService.generateUploadUrl(anyString(), anyString(), anyLong(), anyMap()))
          .thenReturn(mock(com.ebikes.workforce.dtos.internal.UploadUrlData.class));
      when(documentMapper.toUploadInitiationResponse(any(), anyString(), any()))
          .thenReturn(DocumentDtoFixtures.uploadInitiationResponse());

      var result = ioService.replaceDocument(documentId, "new-file.pdf", "application/pdf");

      assertThat(result).isNotNull();
      verify(accessPolicy).owns(oldDocument.getAgent());
      verify(documentService)
          .validateContentType(DocumentType.NATIONAL_ID_FRONT, "application/pdf");
    }
  }
}
