package com.ebikes.workforce.controllers;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ebikes.workforce.dtos.requests.agents.LiftSuspensionRequest;
import com.ebikes.workforce.dtos.requests.agents.SuspendAgentRequest;
import com.ebikes.workforce.dtos.requests.filters.SuspensionFilter;
import com.ebikes.workforce.dtos.responses.api.PaginatedResponse;
import com.ebikes.workforce.dtos.responses.api.SuccessResponse;
import com.ebikes.workforce.dtos.responses.suspensions.SuspensionDetailResponse;
import com.ebikes.workforce.dtos.responses.suspensions.SuspensionSummaryResponse;
import com.ebikes.workforce.services.agents.suspension.SuspensionService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/agents")
@RestController
public class SuspensionController {

  private final SuspensionService suspensionService;

  @PreAuthorize(
      "hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_FLEET_MANAGER',"
          + " 'ORGANIZATION_FLEET_SUPPORT', 'BRANCH_ADMIN', 'BRANCH_FLEET_MANAGER',"
          + " 'BRANCH_FLEET_SUPPORT', 'SYSTEM_ADMIN')")
  @GetMapping("/suspensions/{suspensionId}")
  public ResponseEntity<SuccessResponse<SuspensionDetailResponse>> getById(
      @PathVariable UUID suspensionId) {
    SuspensionDetailResponse response = suspensionService.getById(suspensionId);
    return ResponseEntity.ok(SuccessResponse.of(response));
  }

  @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'BRANCH_ADMIN', 'SYSTEM_ADMIN')")
  @PatchMapping("/{agentId}/suspensions/active/lift")
  public ResponseEntity<SuccessResponse<SuspensionDetailResponse>> liftSuspension(
      @PathVariable UUID agentId, @Valid @RequestBody LiftSuspensionRequest request) {
    SuspensionDetailResponse response = suspensionService.liftSuspension(agentId, request);
    return ResponseEntity.ok(SuccessResponse.of(response, "Suspension lifted successfully."));
  }

  @PreAuthorize(
      "hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_FLEET_MANAGER',"
          + " 'ORGANIZATION_FLEET_SUPPORT', 'BRANCH_ADMIN', 'BRANCH_FLEET_MANAGER',"
          + " 'BRANCH_FLEET_SUPPORT', 'SYSTEM_ADMIN')")
  @GetMapping("/{agentId}/suspensions")
  public ResponseEntity<PaginatedResponse<SuspensionSummaryResponse>> search(
      @PathVariable UUID agentId, SuspensionFilter filter) {
    filter.setAgentId(agentId);
    return ResponseEntity.ok(suspensionService.search(filter));
  }

  @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'BRANCH_ADMIN', 'SYSTEM_ADMIN')")
  @PostMapping("/{agentId}/suspensions")
  public ResponseEntity<SuccessResponse<SuspensionDetailResponse>> suspend(
      @PathVariable UUID agentId, @Valid @RequestBody SuspendAgentRequest request) {
    SuspensionDetailResponse response = suspensionService.suspend(agentId, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(SuccessResponse.of(response, "Agent suspended successfully."));
  }
}
