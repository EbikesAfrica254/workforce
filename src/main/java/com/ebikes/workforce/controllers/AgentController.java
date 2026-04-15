package com.ebikes.workforce.controllers;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ebikes.workforce.dtos.requests.agents.CreateAgentRequest;
import com.ebikes.workforce.dtos.requests.agents.UpdateAgentRequest;
import com.ebikes.workforce.dtos.requests.agents.UpdateLocationRequest;
import com.ebikes.workforce.dtos.requests.certifications.CreateCertificationRequest;
import com.ebikes.workforce.dtos.requests.filters.AgentFilter;
import com.ebikes.workforce.dtos.requests.filters.LocationHistoryFilter;
import com.ebikes.workforce.dtos.requests.paymentmethods.CreatePaymentMethodRequest;
import com.ebikes.workforce.dtos.requests.paymentmethods.UpdatePaymentMethodRequest;
import com.ebikes.workforce.dtos.responses.agents.AgentDetailResponse;
import com.ebikes.workforce.dtos.responses.agents.AgentSummaryResponse;
import com.ebikes.workforce.dtos.responses.agents.AvailabilityLogResponse;
import com.ebikes.workforce.dtos.responses.agents.LocationHistoryResponse;
import com.ebikes.workforce.dtos.responses.api.PaginatedResponse;
import com.ebikes.workforce.dtos.responses.api.SuccessResponse;
import com.ebikes.workforce.dtos.responses.certifications.CertificationDetailResponse;
import com.ebikes.workforce.dtos.responses.certifications.CertificationSummaryResponse;
import com.ebikes.workforce.dtos.responses.documents.DocumentPreviewResponse;
import com.ebikes.workforce.dtos.responses.documents.DocumentSummaryResponse;
import com.ebikes.workforce.dtos.responses.paymentmethods.PaymentMethodDetailResponse;
import com.ebikes.workforce.dtos.responses.paymentmethods.PaymentMethodSummaryResponse;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.LocationSource;
import com.ebikes.workforce.services.agents.AgentsService;
import com.ebikes.workforce.services.agents.availability.AvailabilityService;
import com.ebikes.workforce.services.agents.certification.CertificationService;
import com.ebikes.workforce.services.agents.document.DocumentService;
import com.ebikes.workforce.services.agents.location.LocationService;
import com.ebikes.workforce.services.agents.payment.PaymentMethodService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/agents")
@RestController
public class AgentController {

  private final AgentsService agentsService;
  private final AvailabilityService availabilityService;
  private final CertificationService certificationService;
  private final DocumentService documentService;
  private final LocationService locationService;
  private final PaymentMethodService paymentMethodService;

  @PreAuthorize("isAuthenticated()")
  @PostMapping
  public ResponseEntity<SuccessResponse<AgentDetailResponse>> create(
      @Valid @RequestBody CreateAgentRequest request) {
    AgentDetailResponse response = agentsService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(SuccessResponse.of(response, "Agent registered successfully."));
  }

  @PreAuthorize("hasAnyAuthority('BRANCH_ADMIN', 'ORGANIZATION_ADMIN', 'SYSTEM_ADMIN')")
  @PostMapping("/{agentId}/certifications")
  public ResponseEntity<SuccessResponse<CertificationDetailResponse>> createCertification(
      @PathVariable UUID agentId, @Valid @RequestBody CreateCertificationRequest request) {
    CertificationDetailResponse response = certificationService.create(agentId, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(SuccessResponse.of(response, "Certification recorded successfully."));
  }

  @PreAuthorize("hasAuthority('AGENT')")
  @PostMapping("/{agentId}/payment-methods")
  public ResponseEntity<SuccessResponse<PaymentMethodDetailResponse>> createPaymentMethod(
      @PathVariable UUID agentId, @Valid @RequestBody CreatePaymentMethodRequest request) {
    PaymentMethodDetailResponse response = paymentMethodService.create(agentId, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(SuccessResponse.of(response, "Payment method added successfully."));
  }

  @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
  @DeleteMapping("/{agentId}")
  public ResponseEntity<SuccessResponse<AgentDetailResponse>> deactivate(
      @PathVariable UUID agentId) {
    AgentDetailResponse response = agentsService.deactivate(agentId);
    return ResponseEntity.ok(SuccessResponse.of(response, "Agent deactivated successfully."));
  }

  @PreAuthorize("hasAuthority('AGENT')")
  @DeleteMapping("/{agentId}/payment-methods/{paymentMethodId}")
  public ResponseEntity<SuccessResponse<Void>> deletePaymentMethod(
      @PathVariable UUID agentId, @PathVariable UUID paymentMethodId) {
    paymentMethodService.delete(agentId, paymentMethodId);
    return ResponseEntity.ok(SuccessResponse.of(null, "Payment method removed successfully."));
  }

  @PreAuthorize(
      "hasAnyAuthority('AGENT', 'BRANCH_ADMIN', 'BRANCH_FLEET_MANAGER', 'BRANCH_FLEET_SUPPORT',"
          + " 'ORGANIZATION_ADMIN', 'ORGANIZATION_FLEET_MANAGER', 'ORGANIZATION_FLEET_SUPPORT',"
          + " 'SYSTEM_ADMIN')")
  @GetMapping("/{agentId}/certifications/{certificationId}")
  public ResponseEntity<SuccessResponse<CertificationDetailResponse>> getCertificationById(
      @PathVariable UUID agentId, @PathVariable UUID certificationId) {
    CertificationDetailResponse response = certificationService.getById(agentId, certificationId);
    return ResponseEntity.ok(SuccessResponse.of(response));
  }

  @PreAuthorize(
      "hasAnyAuthority('AGENT', 'BRANCH_ADMIN', 'BRANCH_FLEET_MANAGER', 'BRANCH_FLEET_SUPPORT',"
          + " 'ORGANIZATION_ADMIN', 'ORGANIZATION_FLEET_MANAGER', 'ORGANIZATION_FLEET_SUPPORT',"
          + " 'SYSTEM_ADMIN')")
  @GetMapping("/{agentId}/certifications")
  public ResponseEntity<SuccessResponse<List<CertificationSummaryResponse>>> getCertifications(
      @PathVariable UUID agentId) {
    return ResponseEntity.ok(SuccessResponse.of(certificationService.getByAgentId(agentId)));
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{agentId}/documents")
  public ResponseEntity<SuccessResponse<List<DocumentSummaryResponse>>> getDocuments(
      @PathVariable UUID agentId, @RequestParam(defaultValue = "false") boolean includeInactive) {
    List<DocumentSummaryResponse> documents = documentService.findByAgent(agentId, includeInactive);
    return ResponseEntity.ok(SuccessResponse.of(documents));
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{agentId}/documents/previews")
  public ResponseEntity<SuccessResponse<List<DocumentPreviewResponse>>> getDocumentPreviews(
      @PathVariable UUID agentId, @RequestParam(defaultValue = "false") boolean includeInactive) {
    List<DocumentPreviewResponse> previews =
        documentService.findByAgentWithPreviews(agentId, includeInactive);
    return ResponseEntity.ok(SuccessResponse.of(previews));
  }

  @PreAuthorize(
      "hasAnyAuthority('AGENT', 'ORGANIZATION_ADMIN', 'ORGANIZATION_FLEET_MANAGER',"
          + " 'ORGANIZATION_FLEET_SUPPORT', 'BRANCH_ADMIN', 'BRANCH_FLEET_MANAGER',"
          + " 'BRANCH_FLEET_SUPPORT', 'SYSTEM_ADMIN')")
  @GetMapping("/{agentId}")
  public ResponseEntity<SuccessResponse<AgentDetailResponse>> getById(@PathVariable UUID agentId) {
    AgentDetailResponse response = agentsService.getById(agentId);
    return ResponseEntity.ok(SuccessResponse.of(response));
  }

  @PreAuthorize("hasAuthority('AGENT')")
  @GetMapping("/user/{userId}")
  public ResponseEntity<SuccessResponse<AgentDetailResponse>> getByUserId(
      @PathVariable String userId) {
    AgentDetailResponse response = agentsService.getByUserId(userId);
    return ResponseEntity.ok(SuccessResponse.of(response));
  }

  @PreAuthorize(
      "hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_FLEET_MANAGER',"
          + " 'ORGANIZATION_FLEET_SUPPORT', 'BRANCH_ADMIN', 'BRANCH_FLEET_MANAGER',"
          + " 'BRANCH_FLEET_SUPPORT', 'SYSTEM_ADMIN')")
  @GetMapping("/{agentId}/availability-log")
  public ResponseEntity<SuccessResponse<Page<AvailabilityLogResponse>>> getAvailabilityLog(
      @PathVariable UUID agentId, Pageable pageable) {
    Page<AvailabilityLogResponse> response =
        availabilityService.getAvailabilityLog(agentId, pageable);
    return ResponseEntity.ok(SuccessResponse.of(response));
  }

  @PreAuthorize(
      "hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_FLEET_MANAGER',"
          + " 'ORGANIZATION_FLEET_SUPPORT', 'BRANCH_ADMIN', 'BRANCH_FLEET_MANAGER',"
          + " 'BRANCH_FLEET_SUPPORT', 'SYSTEM_ADMIN')")
  @GetMapping("/{agentId}/location-history")
  public ResponseEntity<SuccessResponse<Page<LocationHistoryResponse>>> getLocationHistory(
      @PathVariable UUID agentId, LocationHistoryFilter filter, Pageable pageable) {
    filter.setAgentId(agentId);
    return ResponseEntity.ok(
        SuccessResponse.of(locationService.getLocationHistory(filter, pageable)));
  }

  @PreAuthorize("hasAnyAuthority('AGENT', 'BRANCH_ADMIN', 'ORGANIZATION_ADMIN', 'SYSTEM_ADMIN')")
  @GetMapping("/{agentId}/payment-methods/{paymentMethodId}")
  public ResponseEntity<SuccessResponse<PaymentMethodDetailResponse>> getPaymentMethodById(
      @PathVariable UUID agentId, @PathVariable UUID paymentMethodId) {
    return ResponseEntity.ok(
        SuccessResponse.of(paymentMethodService.getById(agentId, paymentMethodId)));
  }

  @PreAuthorize("hasAnyAuthority('AGENT', 'BRANCH_ADMIN', 'ORGANIZATION_ADMIN', 'SYSTEM_ADMIN')")
  @GetMapping("/{agentId}/payment-methods")
  public ResponseEntity<SuccessResponse<List<PaymentMethodSummaryResponse>>> getPaymentMethods(
      @PathVariable UUID agentId) {
    return ResponseEntity.ok(SuccessResponse.of(paymentMethodService.findByAgentId(agentId)));
  }

  @PreAuthorize("hasAuthority('AGENT')")
  @PostMapping("/{agentId}/resubmit")
  public ResponseEntity<SuccessResponse<AgentDetailResponse>> resubmit(@PathVariable UUID agentId) {
    AgentDetailResponse response = agentsService.resubmit(agentId);
    return ResponseEntity.ok(SuccessResponse.of(response, "Agent resubmitted successfully."));
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping
  public ResponseEntity<PaginatedResponse<AgentSummaryResponse>> search(
      @ModelAttribute AgentFilter filter) {
    return ResponseEntity.ok((agentsService.search(filter)));
  }

  @PreAuthorize("hasAuthority('AGENT')")
  @PatchMapping("/{agentId}/payment-methods/{paymentMethodId}/set-primary")
  public ResponseEntity<SuccessResponse<PaymentMethodDetailResponse>> setPrimaryPaymentMethod(
      @PathVariable UUID agentId, @PathVariable UUID paymentMethodId) {
    PaymentMethodDetailResponse response =
        paymentMethodService.setPrimary(agentId, paymentMethodId);
    return ResponseEntity.ok(
        SuccessResponse.of(response, "Primary payment method updated successfully."));
  }

  @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'BRANCH_ADMIN', 'SYSTEM_ADMIN')")
  @PatchMapping("/{agentId}")
  public ResponseEntity<SuccessResponse<AgentDetailResponse>> update(
      @PathVariable UUID agentId, @Valid @RequestBody UpdateAgentRequest request) {
    AgentDetailResponse response = agentsService.update(agentId, request);
    return ResponseEntity.ok(SuccessResponse.of(response, "Agent updated successfully."));
  }

  @PreAuthorize(
      "hasAnyAuthority('AGENT', 'ORGANIZATION_ADMIN', 'ORGANIZATION_FLEET_MANAGER', 'BRANCH_ADMIN',"
          + " 'BRANCH_FLEET_MANAGER', 'SYSTEM_ADMIN')")
  @PatchMapping("/{agentId}/availability")
  public ResponseEntity<SuccessResponse<AgentDetailResponse>> updateAvailability(
      @PathVariable UUID agentId,
      @RequestParam AvailabilityStatus status,
      @RequestParam(required = false) String reason) {
    AgentDetailResponse response = availabilityService.updateAvailability(agentId, status, reason);
    return ResponseEntity.ok(SuccessResponse.of(response, "Availability updated successfully."));
  }

  @PreAuthorize("hasAuthority('AGENT')")
  @PatchMapping("/{agentId}/location")
  public ResponseEntity<SuccessResponse<AgentDetailResponse>> updateLocation(
      @PathVariable UUID agentId, @Valid @RequestBody UpdateLocationRequest request) {
    AgentDetailResponse response =
        locationService.updateLocation(agentId, request, LocationSource.WEB_BROWSER);
    return ResponseEntity.ok(SuccessResponse.of(response, "Location updated successfully."));
  }

  @PreAuthorize("hasAuthority('AGENT')")
  @PatchMapping("/{agentId}/payment-methods/{paymentMethodId}")
  public ResponseEntity<SuccessResponse<PaymentMethodDetailResponse>> updatePaymentMethod(
      @PathVariable UUID agentId,
      @PathVariable UUID paymentMethodId,
      @Valid @RequestBody UpdatePaymentMethodRequest request) {
    PaymentMethodDetailResponse response =
        paymentMethodService.update(agentId, paymentMethodId, request);
    return ResponseEntity.ok(SuccessResponse.of(response, "Payment method updated successfully."));
  }
}
