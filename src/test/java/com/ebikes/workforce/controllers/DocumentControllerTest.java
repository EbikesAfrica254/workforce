package com.ebikes.workforce.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ebikes.workforce.dtos.requests.documents.ConfirmUploadRequest;
import com.ebikes.workforce.dtos.requests.documents.InitiateUploadRequest;
import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.enums.DocumentType;
import com.ebikes.workforce.enums.UserRole;
import com.ebikes.workforce.services.agents.document.DocumentService;
import com.ebikes.workforce.services.agents.document.IOService;
import com.ebikes.workforce.support.infrastructure.AbstractControllerTest;

@DisplayName("DocumentController")
@WebMvcTest(DocumentController.class)
class DocumentControllerTest extends AbstractControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private DocumentService documentService;
  @MockitoBean private IOService ioService;

  private static InitiateUploadRequest initiateUploadRequest() {
    return new InitiateUploadRequest("application/pdf", DocumentType.NATIONAL_ID_FRONT, "id.pdf");
  }

  private static ConfirmUploadRequest confirmUploadRequest() {
    return new ConfirmUploadRequest(LocalDate.now().plusYears(1), 1024L, "application/pdf");
  }

  @Nested
  @DisplayName("POST /documents/initiate-upload")
  class InitiateUpload {

    @Test
    @DisplayName("should return 201 when upload initiated")
    void shouldReturn201WhenInitiated() throws Exception {
      when(ioService.initiateUpload(any())).thenReturn(null);

      mockMvc
          .perform(
              post("/documents/initiate-upload")
                  .with(authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(initiateUploadRequest())))
          .andExpect(status().isCreated());

      verify(ioService).initiateUpload(any());
    }

    @Test
    @DisplayName("should return 400 when request body is invalid")
    void shouldReturn400WhenInvalid() throws Exception {
      mockMvc
          .perform(
              post("/documents/initiate-upload")
                  .with(authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              post("/documents/initiate-upload")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(initiateUploadRequest())))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("PUT /documents/{id}/confirm-upload")
  class ConfirmUpload {

    @Test
    @DisplayName("should return 200 when upload confirmed")
    void shouldReturn200WhenConfirmed() throws Exception {
      when(ioService.confirmUpload(any(), any())).thenReturn(null);

      mockMvc
          .perform(
              put("/documents/{id}/confirm-upload", UUID.randomUUID())
                  .with(authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(confirmUploadRequest())))
          .andExpect(status().isOk());

      verify(ioService).confirmUpload(any(), any());
    }

    @Test
    @DisplayName("should return 400 when request body is invalid")
    void shouldReturn400WhenInvalid() throws Exception {
      mockMvc
          .perform(
              put("/documents/{id}/confirm-upload", UUID.randomUUID())
                  .with(authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              put("/documents/{id}/confirm-upload", UUID.randomUUID())
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(confirmUploadRequest())))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("POST /documents/{id}/replace")
  class ReplaceDocument {

    @Test
    @DisplayName("should return 201 when replacement initiated")
    void shouldReturn201WhenInitiated() throws Exception {
      when(ioService.replaceDocument(any(), any(), any())).thenReturn(null);

      mockMvc
          .perform(
              post("/documents/{id}/replace", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(initiateUploadRequest())))
          .andExpect(status().isCreated());

      verify(ioService).replaceDocument(any(), any(), any());
    }

    @Test
    @DisplayName("should return 403 when caller is not AGENT")
    void shouldReturn403WhenNotAgent() throws Exception {
      mockMvc
          .perform(
              post("/documents/{id}/replace", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(initiateUploadRequest())))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              post("/documents/{id}/replace", UUID.randomUUID())
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(initiateUploadRequest())))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("PUT /documents/{id}/confirm-replacement")
  class ConfirmReplacement {

    @Test
    @DisplayName("should return 200 when replacement confirmed")
    void shouldReturn200WhenConfirmed() throws Exception {
      when(ioService.confirmReplacement(any(), any())).thenReturn(null);

      mockMvc
          .perform(
              put("/documents/{id}/confirm-replacement", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(confirmUploadRequest())))
          .andExpect(status().isOk());

      verify(ioService).confirmReplacement(any(), any());
    }

    @Test
    @DisplayName("should return 403 when caller is not AGENT")
    void shouldReturn403WhenNotAgent() throws Exception {
      mockMvc
          .perform(
              put("/documents/{id}/confirm-replacement", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(confirmUploadRequest())))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              put("/documents/{id}/confirm-replacement", UUID.randomUUID())
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(confirmUploadRequest())))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /documents/{id}/download")
  class Download {

    @Test
    @DisplayName("should return 200 with download URL")
    void shouldReturn200WithUrl() throws Exception {
      when(ioService.download(any())).thenReturn("https://s3.example.com/file.pdf");

      mockMvc
          .perform(get("/documents/{id}/download", UUID.randomUUID()).with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(ioService).download(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get("/documents/{id}/download", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /documents/workforce-classes/{workforceClass}/required-documents")
  class GetRequiredDocuments {

    @Test
    @DisplayName("should return 200 with required document types")
    void shouldReturn200WithRequiredDocuments() throws Exception {
      mockMvc
          .perform(
              get(
                      "/documents/workforce-classes/{workforceClass}/required-documents",
                      CapabilityClass.BICYCLE_RIDER)
                  .with(authenticatedJwt()))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              get(
                      "/documents/workforce-classes/{workforceClass}/required-documents",
                      CapabilityClass.BICYCLE_RIDER)
                  .with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }
}
