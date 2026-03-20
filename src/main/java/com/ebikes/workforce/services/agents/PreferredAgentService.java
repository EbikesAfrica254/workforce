package com.ebikes.workforce.services.agents;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.workforce.constants.EventConstants.EventTypes;
import com.ebikes.workforce.constants.EventConstants.RoutingKeys;
import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.PreferredAgent;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.PreferredAgentRepository;
import com.ebikes.workforce.database.specifications.PreferredAgentSpecifications;
import com.ebikes.workforce.dtos.requests.filters.PreferredAgentFilter;
import com.ebikes.workforce.dtos.requests.preferredagents.CreatePreferredAgentRequest;
import com.ebikes.workforce.dtos.requests.preferredagents.UpdatePreferredAgentRequest;
import com.ebikes.workforce.dtos.responses.api.PaginatedResponse;
import com.ebikes.workforce.dtos.responses.preferredagents.PreferredAgentDetailResponse;
import com.ebikes.workforce.dtos.responses.preferredagents.PreferredAgentSummaryResponse;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.ResponseCode;
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.exceptions.DuplicateResourceException;
import com.ebikes.workforce.exceptions.ResourceNotFoundException;
import com.ebikes.workforce.exceptions.ValidationException;
import com.ebikes.workforce.mappers.PreferredAgentMapper;
import com.ebikes.workforce.publishers.AuditEventPublisher;
import com.ebikes.workforce.support.audit.AuditMetadataBuilder;
import com.ebikes.workforce.support.context.ExecutionContext;
import com.ebikes.workforce.support.database.FilterUtilities;
import com.ebikes.workforce.support.security.RBACUtilities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class PreferredAgentService {

  private final AgentRepository agentRepository;
  private final AuditEventPublisher auditEventPublisher;
  private final PreferredAgentMapper preferredAgentMapper;
  private final PreferredAgentRepository preferredAgentRepository;

  @Transactional
  public PreferredAgentDetailResponse create(CreatePreferredAgentRequest request) {
    log.info(
        "Adding preferred agent: organizationId={}, agentId={}",
        request.organizationId(),
        request.agentId());

    Agent agent = requireAgentById(request.agentId());

    if (AvailabilityStatus.DEACTIVATED.equals(agent.getAvailabilityStatus())) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Deactivated agents cannot be added to a preferred agents list");
    }

    boolean duplicate =
        request.branchId() != null
            ? preferredAgentRepository.existsByOrganizationIdAndBranchIdAndAgentId(
                request.organizationId(), request.branchId(), request.agentId())
            : preferredAgentRepository.existsByOrganizationIdAndAgentId(
                request.organizationId(), request.agentId());

    if (duplicate) {
      throw new DuplicateResourceException(
          ResponseCode.DUPLICATE_RESOURCE,
          "Agent " + request.agentId() + " is already in the preferred agents list");
    }

    PreferredAgent preferredAgent =
        new PreferredAgent(
            request.agentId(),
            request.branchId(),
            request.notes(),
            request.organizationId(),
            request.priority());

    preferredAgent = preferredAgentRepository.save(preferredAgent);

    auditEventPublisher.publishSuccess(
        preferredAgent.getId(),
        preferredAgent.getClass().getSimpleName(),
        EventTypes.Workforce.AGENT_UPDATED,
        AuditMetadataBuilder.forPreferredAgent(preferredAgent),
        request.organizationId(),
        RoutingKeys.WORKFORCE_PREFERRED_AGENT_AUDIT);

    log.info(
        "Preferred agent added: preferredAgentId={}, organizationId={}, agentId={}",
        preferredAgent.getId(),
        request.organizationId(),
        request.agentId());

    return preferredAgentMapper.toDetailResponse(preferredAgent);
  }

  @Transactional
  public void delete(UUID preferredAgentId) {
    String organizationId = ExecutionContext.getActiveOrganization();
    PreferredAgent preferredAgent =
        RBACUtilities.hasSystemAdminRole(RBACUtilities.parseRoles(ExecutionContext.getRoles()))
            ? requireById(preferredAgentId)
            : requireByIdAndOrganizationId(preferredAgentId, organizationId);
    preferredAgentRepository.delete(preferredAgent);

    auditEventPublisher.publishSuccess(
        preferredAgent.getId(),
        preferredAgent.getClass().getSimpleName(),
        EventTypes.Workforce.AGENT_UPDATED,
        AuditMetadataBuilder.forPreferredAgent(preferredAgent),
        organizationId,
        RoutingKeys.WORKFORCE_PREFERRED_AGENT_AUDIT);

    log.info(
        "Preferred agent removed: preferredAgentId={}, organizationId={}",
        preferredAgentId,
        organizationId);
  }

  @Transactional(readOnly = true)
  public PreferredAgentDetailResponse getById(UUID preferredAgentId) {
    String organizationId = ExecutionContext.getActiveOrganization();
    PreferredAgent preferredAgent =
        RBACUtilities.hasSystemAdminRole(RBACUtilities.parseRoles(ExecutionContext.getRoles()))
            ? requireById(preferredAgentId)
            : requireByIdAndOrganizationId(preferredAgentId, organizationId);
    return preferredAgentMapper.toDetailResponse(preferredAgent);
  }

  @Transactional(readOnly = true)
  public PaginatedResponse<PreferredAgentSummaryResponse> search(PreferredAgentFilter filter) {
    Specification<PreferredAgent> spec = PreferredAgentSpecifications.buildSpecification(filter);
    Pageable pageable =
        FilterUtilities.buildPageable(filter, PreferredAgentSpecifications.ALLOWED_SORT_FIELDS);
    Page<PreferredAgentSummaryResponse> page =
        preferredAgentRepository
            .findAll(spec, pageable)
            .map(preferredAgentMapper::toSummaryResponse);
    return PaginatedResponse.from("Preferred agents successfully retrieved.", page);
  }

  @Transactional
  public PreferredAgentDetailResponse update(
      UUID preferredAgentId, UpdatePreferredAgentRequest request) {
    log.info(
        "Updating preferred agent: organizationId={}, preferredAgentId={}",
        request.organizationId(),
        preferredAgentId);

    if (request.priority() == null && request.notes() == null) {
      throw new ValidationException(
          ResponseCode.INVALID_ARGUMENTS,
          "At least one of priority or notes must be provided",
          "[priority/notes]",
          null);
    }

    PreferredAgent preferredAgent =
        requireByIdAndOrganizationId(preferredAgentId, request.organizationId());

    if (request.priority() != null) {
      preferredAgent.updatePriority(request.priority());
    }
    if (request.notes() != null) {
      preferredAgent.updateNotes(request.notes());
    }

    preferredAgent = preferredAgentRepository.save(preferredAgent);

    log.info(
        "Preferred agent updated: preferredAgentId={}, organizationId={}",
        preferredAgentId,
        request.organizationId());

    return preferredAgentMapper.toDetailResponse(preferredAgent);
  }

  void deleteByAgentId(UUID agentId) {
    log.info("Removing all preferred agent entries for deactivated agent: agentId={}", agentId);
    preferredAgentRepository.deleteByAgentId(agentId);
    log.info("Preferred agent entries removed: agentId={}", agentId);
  }

  private Agent requireAgentById(UUID agentId) {
    return agentRepository
        .findById(agentId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND, "Agent with ID " + agentId + " not found"));
  }

  private PreferredAgent requireById(UUID preferredAgentId) {
    return preferredAgentRepository
        .findById(preferredAgentId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND,
                    "Preferred agent with ID " + preferredAgentId + " not found"));
  }

  private PreferredAgent requireByIdAndOrganizationId(
      UUID preferredAgentId, String organizationId) {
    return preferredAgentRepository
        .findById(preferredAgentId)
        .filter(pa -> pa.getOrganizationId().equals(organizationId))
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND,
                    "Preferred agent with ID "
                        + preferredAgentId
                        + " not found for organization "
                        + organizationId));
  }
}
