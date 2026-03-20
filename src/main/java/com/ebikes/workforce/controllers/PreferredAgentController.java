package com.ebikes.workforce.controllers;

import java.util.UUID;

import jakarta.validation.Valid;

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
import org.springframework.web.bind.annotation.RestController;

import com.ebikes.workforce.dtos.requests.filters.PreferredAgentFilter;
import com.ebikes.workforce.dtos.requests.preferredagents.CreatePreferredAgentRequest;
import com.ebikes.workforce.dtos.requests.preferredagents.UpdatePreferredAgentRequest;
import com.ebikes.workforce.dtos.responses.api.PaginatedResponse;
import com.ebikes.workforce.dtos.responses.api.SuccessResponse;
import com.ebikes.workforce.dtos.responses.preferredagents.PreferredAgentDetailResponse;
import com.ebikes.workforce.dtos.responses.preferredagents.PreferredAgentSummaryResponse;
import com.ebikes.workforce.services.agents.PreferredAgentService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/preferred-agents")
@RestController
public class PreferredAgentController {

  private final PreferredAgentService preferredAgentService;

  @PreAuthorize(
      "hasAnyAuthority('BRANCH_ADMIN', 'BRANCH_MAKER', 'ORGANIZATION_ADMIN', 'ORGANIZATION_MAKER',"
          + " 'SYSTEM_ADMIN')")
  @PostMapping
  public ResponseEntity<SuccessResponse<PreferredAgentDetailResponse>> create(
      @Valid @RequestBody CreatePreferredAgentRequest request) {
    PreferredAgentDetailResponse response = preferredAgentService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(SuccessResponse.of(response, "Preferred agent added successfully."));
  }

  @PreAuthorize(
      "hasAnyAuthority('BRANCH_ADMIN', 'BRANCH_MAKER', 'ORGANIZATION_ADMIN', 'ORGANIZATION_MAKER',"
          + " 'SYSTEM_ADMIN')")
  @DeleteMapping("/{preferredAgentId}")
  public ResponseEntity<SuccessResponse<Void>> delete(@PathVariable UUID preferredAgentId) {
    preferredAgentService.delete(preferredAgentId);
    return ResponseEntity.ok(SuccessResponse.of(null, "Preferred agent removed successfully."));
  }

  @PreAuthorize(
      "hasAnyAuthority('BRANCH_ADMIN', 'BRANCH_FLEET_MANAGER', 'BRANCH_FLEET_SUPPORT',"
          + " 'ORGANIZATION_ADMIN', 'ORGANIZATION_FLEET_MANAGER', 'ORGANIZATION_FLEET_SUPPORT',"
          + " 'SYSTEM_ADMIN')")
  @GetMapping("/{preferredAgentId}")
  public ResponseEntity<SuccessResponse<PreferredAgentDetailResponse>> getById(
      @PathVariable UUID preferredAgentId) {
    PreferredAgentDetailResponse response = preferredAgentService.getById(preferredAgentId);
    return ResponseEntity.ok(SuccessResponse.of(response));
  }

  @PreAuthorize(
      "hasAnyAuthority('BRANCH_ADMIN', 'BRANCH_FLEET_MANAGER', 'BRANCH_FLEET_SUPPORT',"
          + " 'ORGANIZATION_ADMIN', 'ORGANIZATION_FLEET_MANAGER', 'ORGANIZATION_FLEET_SUPPORT',"
          + " 'SYSTEM_ADMIN')")
  @GetMapping
  public ResponseEntity<PaginatedResponse<PreferredAgentSummaryResponse>> search(
      @ModelAttribute PreferredAgentFilter filter) {
    return ResponseEntity.ok(preferredAgentService.search(filter));
  }

  @PreAuthorize(
      "hasAnyAuthority('BRANCH_ADMIN', 'BRANCH_MAKER', 'ORGANIZATION_ADMIN', 'ORGANIZATION_MAKER',"
          + " 'SYSTEM_ADMIN')")
  @PatchMapping("/{preferredAgentId}")
  public ResponseEntity<SuccessResponse<PreferredAgentDetailResponse>> update(
      @PathVariable UUID preferredAgentId,
      @Valid @RequestBody UpdatePreferredAgentRequest request) {
    PreferredAgentDetailResponse response = preferredAgentService.update(preferredAgentId, request);
    return ResponseEntity.ok(SuccessResponse.of(response, "Preferred agent updated successfully."));
  }
}
