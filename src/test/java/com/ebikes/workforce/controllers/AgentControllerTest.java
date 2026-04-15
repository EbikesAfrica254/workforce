package com.ebikes.workforce.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ebikes.workforce.dtos.responses.api.PaginatedResponse;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.UserRole;
import com.ebikes.workforce.services.agents.AgentsService;
import com.ebikes.workforce.services.agents.availability.AvailabilityService;
import com.ebikes.workforce.services.agents.certification.CertificationService;
import com.ebikes.workforce.services.agents.document.DocumentService;
import com.ebikes.workforce.services.agents.location.LocationService;
import com.ebikes.workforce.services.agents.payment.PaymentMethodService;
import com.ebikes.workforce.support.fixtures.AgentDtoFixtures;
import com.ebikes.workforce.support.fixtures.SecurityFixtures;
import com.ebikes.workforce.support.infrastructure.AbstractControllerTest;

@DisplayName("AgentController")
@WebMvcTest(AgentController.class)
class AgentControllerTest extends AbstractControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AgentsService agentsService;
  @MockitoBean private AvailabilityService availabilityService;
  @MockitoBean private CertificationService certificationService;
  @MockitoBean private DocumentService documentService;
  @MockitoBean private LocationService locationService;
  @MockitoBean private PaymentMethodService paymentMethodService;

  @Nested
  @DisplayName("POST /agents")
  class Create {

    @Test
    @DisplayName("should return 201 when agent created successfully")
    void shouldReturn201WhenCreated() throws Exception {
      when(agentsService.create(any())).thenReturn(AgentDtoFixtures.detailResponse());

      mockMvc
          .perform(
              post("/agents")
                  .with(authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(AgentDtoFixtures.createRequest())))
          .andExpect(status().isCreated());

      verify(agentsService).create(any());
    }

    @Test
    @DisplayName("should return 400 when request body is invalid")
    void shouldReturn400WhenRequestInvalid() throws Exception {
      mockMvc
          .perform(
              post("/agents")
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
              post("/agents")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(AgentDtoFixtures.createRequest())))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("DELETE /agents/{agentId}")
  class Deactivate {

    @Test
    @DisplayName("should return 200 when agent deactivated")
    void shouldReturn200WhenDeactivated() throws Exception {
      when(agentsService.deactivate(any())).thenReturn(AgentDtoFixtures.detailResponse());

      mockMvc
          .perform(
              delete("/agents/{agentId}", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.SYSTEM_ADMIN)))
          .andExpect(status().isOk());

      verify(agentsService).deactivate(any());
    }

    @Test
    @DisplayName("should return 403 when caller is not SYSTEM_ADMIN")
    void shouldReturn403WhenNotSystemAdmin() throws Exception {
      mockMvc
          .perform(delete("/agents/{agentId}", UUID.randomUUID()).with(authenticatedJwt()))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(delete("/agents/{agentId}", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /agents")
  class Search {

    @Test
    @DisplayName("should return 200 with paginated results")
    void shouldReturn200WithPaginatedResults() throws Exception {
      when(agentsService.search(any()))
          .thenReturn(
              PaginatedResponse.from(
                  "Agent successfully retrieved.",
                  new PageImpl<>(List.of(AgentDtoFixtures.summaryResponse()))));

      mockMvc.perform(get("/agents").with(authenticatedJwt())).andExpect(status().isOk());

      verify(agentsService).search(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc.perform(get("/agents").with(anonymous())).andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /agents/{agentId}")
  class GetById {

    @Test
    @DisplayName("should return 200 when agent found")
    void shouldReturn200WhenFound() throws Exception {
      when(agentsService.getById(any())).thenReturn(AgentDtoFixtures.detailResponse());

      mockMvc
          .perform(get("/agents/{agentId}", UUID.randomUUID()).with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(agentsService).getById(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get("/agents/{agentId}", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /agents/user/{userId}")
  class GetByUserId {

    @Test
    @DisplayName("should return 200 when agent found")
    void shouldReturn200WhenFound() throws Exception {
      when(agentsService.getByUserId(any())).thenReturn(AgentDtoFixtures.detailResponse());

      mockMvc
          .perform(
              get("/agents/user/{userId}", SecurityFixtures.TEST_USER_ID)
                  .with(authenticatedJwt(UserRole.AGENT)))
          .andExpect(status().isOk());

      verify(agentsService).getByUserId(any());
    }

    @Test
    @DisplayName("should return 403 when caller is not AGENT")
    void shouldReturn403WhenNotAgent() throws Exception {
      mockMvc
          .perform(
              get("/agents/user/{userId}", SecurityFixtures.TEST_USER_ID)
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN)))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get("/agents/user/{userId}", SecurityFixtures.TEST_USER_ID).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("PATCH /agents/{agentId}")
  class Update {

    @Test
    @DisplayName("should return 200 when agent updated")
    void shouldReturn200WhenUpdated() throws Exception {
      when(agentsService.update(any(), any())).thenReturn(AgentDtoFixtures.detailResponse());

      mockMvc
          .perform(
              patch("/agents/{agentId}", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(AgentDtoFixtures.updateRequest())))
          .andExpect(status().isOk());

      verify(agentsService).update(any(), any());
    }

    @Test
    @DisplayName("should return 403 when caller lacks required role")
    void shouldReturn403WhenUnauthorizedRole() throws Exception {
      mockMvc
          .perform(
              patch("/agents/{agentId}", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(AgentDtoFixtures.updateRequest())))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              patch("/agents/{agentId}", UUID.randomUUID())
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(AgentDtoFixtures.updateRequest())))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("PATCH /agents/{agentId}/availability")
  class UpdateAvailability {

    @Test
    @DisplayName("should return 200 when availability updated")
    void shouldReturn200WhenUpdated() throws Exception {
      when(availabilityService.updateAvailability(any(), any(), any()))
          .thenReturn(AgentDtoFixtures.detailResponse());

      mockMvc
          .perform(
              patch("/agents/{agentId}/availability", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT))
                  .param("status", AvailabilityStatus.AVAILABLE.name()))
          .andExpect(status().isOk());

      verify(availabilityService)
          .updateAvailability(any(), eq(AvailabilityStatus.AVAILABLE), any());
    }

    @Test
    @DisplayName("should return 400 when status param is missing")
    void shouldReturn400WhenStatusMissing() throws Exception {
      mockMvc
          .perform(
              patch("/agents/{agentId}/availability", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              patch("/agents/{agentId}/availability", UUID.randomUUID())
                  .with(anonymous())
                  .param("status", AvailabilityStatus.AVAILABLE.name()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("PATCH /agents/{agentId}/location")
  class UpdateLocation {

    @Test
    @DisplayName("should return 200 when location updated")
    void shouldReturn200WhenUpdated() throws Exception {
      when(locationService.updateLocation(any(), any(), any()))
          .thenReturn(AgentDtoFixtures.detailResponse());

      mockMvc
          .perform(
              patch("/agents/{agentId}/location", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(AgentDtoFixtures.updateLocationRequest())))
          .andExpect(status().isOk());

      verify(locationService).updateLocation(any(), any(), any());
    }

    @Test
    @DisplayName("should return 403 when caller is not AGENT")
    void shouldReturn403WhenNotAgent() throws Exception {
      mockMvc
          .perform(
              patch("/agents/{agentId}/location", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(AgentDtoFixtures.updateLocationRequest())))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              patch("/agents/{agentId}/location", UUID.randomUUID())
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(AgentDtoFixtures.updateLocationRequest())))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("POST /agents/{agentId}/resubmit")
  class Resubmit {

    @Test
    @DisplayName("should return 200 when agent resubmitted")
    void shouldReturn200WhenResubmitted() throws Exception {
      when(agentsService.resubmit(any())).thenReturn(AgentDtoFixtures.detailResponse());

      mockMvc
          .perform(
              post("/agents/{agentId}/resubmit", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT)))
          .andExpect(status().isOk());

      verify(agentsService).resubmit(any());
    }

    @Test
    @DisplayName("should return 403 when caller is not AGENT")
    void shouldReturn403WhenNotAgent() throws Exception {
      mockMvc
          .perform(
              post("/agents/{agentId}/resubmit", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN)))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(post("/agents/{agentId}/resubmit", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /agents/{agentId}/availability-log")
  class GetAvailabilityLog {

    @Test
    @DisplayName("should return 200 with paginated log")
    void shouldReturn200WithLog() throws Exception {
      when(availabilityService.getAvailabilityLog(any(), any())).thenReturn(Page.empty());

      mockMvc
          .perform(
              get("/agents/{agentId}/availability-log", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN)))
          .andExpect(status().isOk());

      verify(availabilityService).getAvailabilityLog(any(), any());
    }

    @Test
    @DisplayName("should return 403 when caller is AGENT")
    void shouldReturn403WhenAgent() throws Exception {
      mockMvc
          .perform(
              get("/agents/{agentId}/availability-log", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT)))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get("/agents/{agentId}/availability-log", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /agents/{agentId}/location-history")
  class GetLocationHistory {

    @Test
    @DisplayName("should return 200 with paginated location history")
    void shouldReturn200WithHistory() throws Exception {
      when(locationService.getLocationHistory(any(), any())).thenReturn(Page.empty());

      mockMvc
          .perform(
              get("/agents/{agentId}/location-history", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN)))
          .andExpect(status().isOk());

      verify(locationService).getLocationHistory(any(), any());
    }

    @Test
    @DisplayName("should return 403 when caller is AGENT")
    void shouldReturn403WhenAgent() throws Exception {
      mockMvc
          .perform(
              get("/agents/{agentId}/location-history", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT)))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get("/agents/{agentId}/location-history", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /agents/{agentId}/documents")
  class GetDocuments {

    @Test
    @DisplayName("should return 200 with document list")
    void shouldReturn200WithDocuments() throws Exception {
      when(documentService.findByAgent(any(), anyBoolean())).thenReturn(List.of());

      mockMvc
          .perform(get("/agents/{agentId}/documents", UUID.randomUUID()).with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(documentService).findByAgent(any(), eq(false));
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get("/agents/{agentId}/documents", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /agents/{agentId}/documents/previews")
  class GetDocumentPreviews {

    @Test
    @DisplayName("should return 200 with preview list")
    void shouldReturn200WithPreviews() throws Exception {
      when(documentService.findByAgentWithPreviews(any(), anyBoolean())).thenReturn(List.of());

      mockMvc
          .perform(
              get("/agents/{agentId}/documents/previews", UUID.randomUUID())
                  .with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(documentService).findByAgentWithPreviews(any(), eq(false));
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get("/agents/{agentId}/documents/previews", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("POST /agents/{agentId}/certifications")
  class CreateCertification {

    @Test
    @DisplayName("should return 201 when certification created")
    void shouldReturn201WhenCreated() throws Exception {
      when(certificationService.create(any(), any())).thenReturn(null);

      mockMvc
          .perform(
              post("/agents/{agentId}/certifications", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(AgentDtoFixtures.createCertificationRequest())))
          .andExpect(status().isCreated());

      verify(certificationService).create(any(), any());
    }

    @Test
    @DisplayName("should return 403 when caller is AGENT")
    void shouldReturn403WhenAgent() throws Exception {
      mockMvc
          .perform(
              post("/agents/{agentId}/certifications", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(AgentDtoFixtures.createCertificationRequest())))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              post("/agents/{agentId}/certifications", UUID.randomUUID())
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(AgentDtoFixtures.createCertificationRequest())))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /agents/{agentId}/certifications")
  class GetCertifications {

    @Test
    @DisplayName("should return 200 with certification list")
    void shouldReturn200WithCertifications() throws Exception {
      when(certificationService.getByAgentId(any())).thenReturn(List.of());

      mockMvc
          .perform(
              get("/agents/{agentId}/certifications", UUID.randomUUID()).with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(certificationService).getByAgentId(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get("/agents/{agentId}/certifications", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /agents/{agentId}/certifications/{certificationId}")
  class GetCertificationById {

    @Test
    @DisplayName("should return 200 when certification found")
    void shouldReturn200WhenFound() throws Exception {
      when(certificationService.getById(any(), any())).thenReturn(null);

      mockMvc
          .perform(
              get(
                      "/agents/{agentId}/certifications/{certificationId}",
                      UUID.randomUUID(),
                      UUID.randomUUID())
                  .with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(certificationService).getById(any(), any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              get(
                      "/agents/{agentId}/certifications/{certificationId}",
                      UUID.randomUUID(),
                      UUID.randomUUID())
                  .with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("POST /agents/{agentId}/payment-methods")
  class CreatePaymentMethod {

    @Test
    @DisplayName("should return 201 when payment method created")
    void shouldReturn201WhenCreated() throws Exception {
      when(paymentMethodService.create(any(), any())).thenReturn(null);

      mockMvc
          .perform(
              post("/agents/{agentId}/payment-methods", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(AgentDtoFixtures.createPaymentMethodRequest())))
          .andExpect(status().isCreated());

      verify(paymentMethodService).create(any(), any());
    }

    @Test
    @DisplayName("should return 403 when caller is not AGENT")
    void shouldReturn403WhenNotAgent() throws Exception {
      mockMvc
          .perform(
              post("/agents/{agentId}/payment-methods", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(AgentDtoFixtures.createPaymentMethodRequest())))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              post("/agents/{agentId}/payment-methods", UUID.randomUUID())
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(AgentDtoFixtures.createPaymentMethodRequest())))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /agents/{agentId}/payment-methods")
  class GetPaymentMethods {

    @Test
    @DisplayName("should return 200 with payment method list")
    void shouldReturn200WithPaymentMethods() throws Exception {
      when(paymentMethodService.findByAgentId(any())).thenReturn(List.of());

      mockMvc
          .perform(
              get("/agents/{agentId}/payment-methods", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT)))
          .andExpect(status().isOk());

      verify(paymentMethodService).findByAgentId(any());
    }

    @Test
    @DisplayName("should return 403 when caller lacks required role")
    void shouldReturn403WhenUnauthorizedRole() throws Exception {
      mockMvc
          .perform(
              get("/agents/{agentId}/payment-methods", UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.ORGANIZATION_FLEET_MANAGER)))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get("/agents/{agentId}/payment-methods", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /agents/{agentId}/payment-methods/{paymentMethodId}")
  class GetPaymentMethodById {

    @Test
    @DisplayName("should return 200 when payment method found")
    void shouldReturn200WhenFound() throws Exception {
      when(paymentMethodService.getById(any(), any())).thenReturn(null);

      mockMvc
          .perform(
              get(
                      "/agents/{agentId}/payment-methods/{paymentMethodId}",
                      UUID.randomUUID(),
                      UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT)))
          .andExpect(status().isOk());

      verify(paymentMethodService).getById(any(), any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              get(
                      "/agents/{agentId}/payment-methods/{paymentMethodId}",
                      UUID.randomUUID(),
                      UUID.randomUUID())
                  .with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("DELETE /agents/{agentId}/payment-methods/{paymentMethodId}")
  class DeletePaymentMethod {

    @Test
    @DisplayName("should return 200 when payment method deleted")
    void shouldReturn200WhenDeleted() throws Exception {
      mockMvc
          .perform(
              delete(
                      "/agents/{agentId}/payment-methods/{paymentMethodId}",
                      UUID.randomUUID(),
                      UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT)))
          .andExpect(status().isOk());

      verify(paymentMethodService).delete(any(), any());
    }

    @Test
    @DisplayName("should return 403 when caller is not AGENT")
    void shouldReturn403WhenNotAgent() throws Exception {
      mockMvc
          .perform(
              delete(
                      "/agents/{agentId}/payment-methods/{paymentMethodId}",
                      UUID.randomUUID(),
                      UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN)))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              delete(
                      "/agents/{agentId}/payment-methods/{paymentMethodId}",
                      UUID.randomUUID(),
                      UUID.randomUUID())
                  .with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("PATCH /agents/{agentId}/payment-methods/{paymentMethodId}/set-primary")
  class SetPrimaryPaymentMethod {

    @Test
    @DisplayName("should return 200 when primary payment method updated")
    void shouldReturn200WhenUpdated() throws Exception {
      when(paymentMethodService.setPrimary(any(), any())).thenReturn(null);

      mockMvc
          .perform(
              patch(
                      "/agents/{agentId}/payment-methods/{paymentMethodId}/set-primary",
                      UUID.randomUUID(),
                      UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT)))
          .andExpect(status().isOk());

      verify(paymentMethodService).setPrimary(any(), any());
    }

    @Test
    @DisplayName("should return 403 when caller is not AGENT")
    void shouldReturn403WhenNotAgent() throws Exception {
      mockMvc
          .perform(
              patch(
                      "/agents/{agentId}/payment-methods/{paymentMethodId}/set-primary",
                      UUID.randomUUID(),
                      UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN)))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              patch(
                      "/agents/{agentId}/payment-methods/{paymentMethodId}/set-primary",
                      UUID.randomUUID(),
                      UUID.randomUUID())
                  .with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("PATCH /agents/{agentId}/payment-methods/{paymentMethodId}")
  class UpdatePaymentMethod {

    @Test
    @DisplayName("should return 200 when payment method updated")
    void shouldReturn200WhenUpdated() throws Exception {
      when(paymentMethodService.update(any(), any(), any())).thenReturn(null);

      mockMvc
          .perform(
              patch(
                      "/agents/{agentId}/payment-methods/{paymentMethodId}",
                      UUID.randomUUID(),
                      UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.AGENT))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(AgentDtoFixtures.updatePaymentMethodRequest())))
          .andExpect(status().isOk());

      verify(paymentMethodService).update(any(), any(), any());
    }

    @Test
    @DisplayName("should return 403 when caller is not AGENT")
    void shouldReturn403WhenNotAgent() throws Exception {
      mockMvc
          .perform(
              patch(
                      "/agents/{agentId}/payment-methods/{paymentMethodId}",
                      UUID.randomUUID(),
                      UUID.randomUUID())
                  .with(authenticatedJwt(UserRole.ORGANIZATION_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(AgentDtoFixtures.updatePaymentMethodRequest())))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              patch(
                      "/agents/{agentId}/payment-methods/{paymentMethodId}",
                      UUID.randomUUID(),
                      UUID.randomUUID())
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(toJson(AgentDtoFixtures.updatePaymentMethodRequest())))
          .andExpect(status().isUnauthorized());
    }
  }
}
