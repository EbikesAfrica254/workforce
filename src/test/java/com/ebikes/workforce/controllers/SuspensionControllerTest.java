package com.ebikes.workforce.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

import com.ebikes.workforce.dtos.responses.api.PaginatedResponse;
import com.ebikes.workforce.enums.UserRole;
import com.ebikes.workforce.services.agents.suspension.SuspensionService;
import com.ebikes.workforce.support.fixtures.AgentDtoFixtures;
import com.ebikes.workforce.support.infrastructure.AbstractControllerTest;

@DisplayName("SuspensionController")
@WebMvcTest(SuspensionController.class)
class SuspensionControllerTest extends AbstractControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SuspensionService suspensionService;

  @Nested
  @DisplayName("POST /agents/{agentId}/suspensions")
  class Suspend {

    @Test
    @DisplayName("should return 201 when agent suspended")
    void shouldReturn201WhenSuspended() throws Exception {
      when(suspensionService.suspend(any(), any())).thenReturn(null);

      mockMvc
          .perform(
              post("/agents/{agentId}/suspensions", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(AgentDtoFixtures.suspendRequest())))
          .andExpect(status().isCreated());

      verify(suspensionService).suspend(any(), any());
    }

    @Test
    @DisplayName("should return 400 when reason is missing")
    void shouldReturn400WhenReasonMissing() throws Exception {
      mockMvc
          .perform(
              post("/agents/{agentId}/suspensions", UUID.randomUUID())
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
              post("/agents/{agentId}/suspensions", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(AgentDtoFixtures.suspendRequest())))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              post("/agents/{agentId}/suspensions", UUID.randomUUID())
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(AgentDtoFixtures.suspendRequest())))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("PATCH /agents/{agentId}/suspensions/active/lift")
  class LiftSuspension {

    @Test
    @DisplayName("should return 200 when suspension lifted")
    void shouldReturn200WhenLifted() throws Exception {
      when(suspensionService.liftSuspension(any(), any())).thenReturn(null);

      mockMvc
          .perform(
              patch("/agents/{agentId}/suspensions/active/lift", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(AgentDtoFixtures.liftSuspensionRequest())))
          .andExpect(status().isOk());

      verify(suspensionService).liftSuspension(any(), any());
    }

    @Test
    @DisplayName("should return 403 when caller is AGENT")
    void shouldReturn403WhenAgent() throws Exception {
      mockMvc
          .perform(
              patch("/agents/{agentId}/suspensions/active/lift", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(AgentDtoFixtures.liftSuspensionRequest())))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              patch("/agents/{agentId}/suspensions/active/lift", UUID.randomUUID())
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(AgentDtoFixtures.liftSuspensionRequest())))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /agents/suspensions/{suspensionId}")
  class GetById {

    @Test
    @DisplayName("should return 200 when suspension found")
    void shouldReturn200WhenFound() throws Exception {
      when(suspensionService.getById(any())).thenReturn(null);

      mockMvc
          .perform(
              get("/agents/suspensions/{suspensionId}", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN)))
          .andExpect(status().isOk());

      verify(suspensionService).getById(any());
    }

    @Test
    @DisplayName("should return 403 when caller is AGENT")
    void shouldReturn403WhenAgent() throws Exception {
      mockMvc
          .perform(
              get("/agents/suspensions/{suspensionId}", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT)))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get("/agents/suspensions/{suspensionId}", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /agents/{agentId}/suspensions")
  class Search {

    @Test
    @DisplayName("should return 200 with paginated results")
    void shouldReturn200WithPaginatedResults() throws Exception {
      when(suspensionService.search(any()))
          .thenReturn(PaginatedResponse.from("Suspensions retrieved.", Page.empty()));

      mockMvc
          .perform(
              get("/agents/{agentId}/suspensions", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN)))
          .andExpect(status().isOk());

      verify(suspensionService).search(any());
    }

    @Test
    @DisplayName("should return 403 when caller is AGENT")
    void shouldReturn403WhenAgent() throws Exception {
      mockMvc
          .perform(
              get("/agents/{agentId}/suspensions", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT)))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get("/agents/{agentId}/suspensions", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }
}
