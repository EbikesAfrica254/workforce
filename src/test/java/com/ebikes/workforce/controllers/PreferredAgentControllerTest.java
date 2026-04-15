package com.ebikes.workforce.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ebikes.workforce.dtos.requests.preferredagents.CreatePreferredAgentRequest;
import com.ebikes.workforce.dtos.requests.preferredagents.UpdatePreferredAgentRequest;
import com.ebikes.workforce.dtos.responses.api.PaginatedResponse;
import com.ebikes.workforce.enums.UserRole;
import com.ebikes.workforce.services.preferences.PreferredAgentService;
import com.ebikes.workforce.support.fixtures.SecurityFixtures;
import com.ebikes.workforce.support.infrastructure.AbstractControllerTest;

@DisplayName("PreferredAgentController")
@WebMvcTest(PreferredAgentController.class)
class PreferredAgentControllerTest extends AbstractControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PreferredAgentService preferredAgentService;

  private static CreatePreferredAgentRequest createRequest() {
    return new CreatePreferredAgentRequest(
        UUID.randomUUID(), null, null, SecurityFixtures.TEST_ORGANIZATION_ID, 1);
  }

  private static UpdatePreferredAgentRequest updateRequest() {
    return new UpdatePreferredAgentRequest(null, 2);
  }

  @Nested
  @DisplayName("POST /preferred-agents")
  class Create {

    @Test
    @DisplayName("should return 201 when preferred agent created")
    void shouldReturn201WhenCreated() throws Exception {
      when(preferredAgentService.create(any())).thenReturn(null);

      mockMvc
          .perform(
              post("/preferred-agents")
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(createRequest())))
          .andExpect(status().isCreated());

      verify(preferredAgentService).create(any());
    }

    @Test
    @DisplayName("should return 400 when request body is invalid")
    void shouldReturn400WhenInvalid() throws Exception {
      mockMvc
          .perform(
              post("/preferred-agents")
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 403 when caller is AGENT")
    void shouldReturn403WhenAgent() throws Exception {
      mockMvc
          .perform(
              post("/preferred-agents")
                  .with(authenticatedJwt(UserRole.AGENT))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(createRequest())))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              post("/preferred-agents")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(createRequest())))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("DELETE /preferred-agents/{preferredAgentId}")
  class Delete {

    @Test
    @DisplayName("should return 200 when preferred agent deleted")
    void shouldReturn200WhenDeleted() throws Exception {
      mockMvc
          .perform(
              delete("/preferred-agents/{preferredAgentId}", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN)))
          .andExpect(status().isOk());

      verify(preferredAgentService).delete(any());
    }

    @Test
    @DisplayName("should return 403 when caller is AGENT")
    void shouldReturn403WhenAgent() throws Exception {
      mockMvc
          .perform(
              delete("/preferred-agents/{preferredAgentId}", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT)))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              delete("/preferred-agents/{preferredAgentId}", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /preferred-agents/{preferredAgentId}")
  class GetById {

    @Test
    @DisplayName("should return 200 when preferred agent found")
    void shouldReturn200WhenFound() throws Exception {
      when(preferredAgentService.getById(any())).thenReturn(null);

      mockMvc
          .perform(
              get("/preferred-agents/{preferredAgentId}", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN)))
          .andExpect(status().isOk());

      verify(preferredAgentService).getById(any());
    }

    @Test
    @DisplayName("should return 403 when caller is AGENT")
    void shouldReturn403WhenAgent() throws Exception {
      mockMvc
          .perform(
              get("/preferred-agents/{preferredAgentId}", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT)))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get("/preferred-agents/{preferredAgentId}", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /preferred-agents")
  class Search {

    @Test
    @DisplayName("should return 200 with paginated results")
    void shouldReturn200WithPaginatedResults() throws Exception {
      when(preferredAgentService.search(any()))
          .thenReturn(
              PaginatedResponse.from("Preferred agents successfully retrieved.", Page.empty()));

      mockMvc
          .perform(get("/preferred-agents").with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN)))
          .andExpect(status().isOk());

      verify(preferredAgentService).search(any());
    }

    @Test
    @DisplayName("should return 403 when caller is AGENT")
    void shouldReturn403WhenAgent() throws Exception {
      mockMvc
          .perform(get("/preferred-agents").with(authenticatedJwt(UserRole.AGENT)))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get("/preferred-agents").with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("PATCH /preferred-agents/{preferredAgentId}")
  class Update {

    @Test
    @DisplayName("should return 200 when preferred agent updated")
    void shouldReturn200WhenUpdated() throws Exception {
      when(preferredAgentService.update(any(), any())).thenReturn(null);

      mockMvc
          .perform(
              patch("/preferred-agents/{preferredAgentId}", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(updateRequest())))
          .andExpect(status().isOk());

      verify(preferredAgentService).update(any(), any());
    }

    @Test
    @DisplayName("should return 403 when caller is AGENT")
    void shouldReturn403WhenAgent() throws Exception {
      mockMvc
          .perform(
              patch("/preferred-agents/{preferredAgentId}", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(updateRequest())))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              patch("/preferred-agents/{preferredAgentId}", UUID.randomUUID())
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(updateRequest())))
          .andExpect(status().isUnauthorized());
    }
  }
}
