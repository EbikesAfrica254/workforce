package com.ebikes.workforce.services.preferences;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.workforce.constants.EventConstants.DomainEvents;
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
import com.ebikes.workforce.enums.UserRole;
import com.ebikes.workforce.exceptions.AuthorizationException;
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.exceptions.DuplicateResourceException;
import com.ebikes.workforce.exceptions.ResourceNotFoundException;
import com.ebikes.workforce.exceptions.ValidationException;
import com.ebikes.workforce.mappers.PreferredAgentMapper;
import com.ebikes.workforce.services.agents.AgentScopedService;
import com.ebikes.workforce.support.audit.AuditTemplate;
import com.ebikes.workforce.support.context.ExecutionContext;
import com.ebikes.workforce.support.database.FilterUtilities;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PreferredAgentService extends AgentScopedService {

  private final AuditTemplate auditTemplate;
  private final PreferredAgentMapper preferredAgentMapper;
  private final PreferredAgentRepository preferredAgentRepository;

  public PreferredAgentService(
      AgentRepository agentRepository,
      AuditTemplate auditTemplate,
      PreferredAgentMapper preferredAgentMapper,
      PreferredAgentRepository preferredAgentRepository) {
    super(agentRepository);
    this.auditTemplate = auditTemplate;
    this.preferredAgentMapper = preferredAgentMapper;
    this.preferredAgentRepository = preferredAgentRepository;
  }

  @Transactional
  public PreferredAgentDetailResponse create(CreatePreferredAgentRequest request) {
    String organizationId = resolveOrganizationId(request.organizationId());

    log.info(
        "Creating preferred agent: organizationId={}, agentId={}",
        organizationId,
        request.agentId());

    requireEligibleAgent(request.agentId());
    validateNoDuplicate(request, organizationId);

    PreferredAgent preferredAgent =
        PreferredAgent.create(
            request.agentId(),
            request.branchId(),
            request.notes(),
            organizationId,
            request.priority());

    PreferredAgent savedPreferredAgent =
        auditTemplate.execute(
            preferredAgent,
            organizationId,
            DomainEvents.PreferredAgent.CREATED,
            () -> preferredAgentRepository.save(preferredAgent));

    log.info(
        "Preferred agent created: preferredAgentId={}, organizationId={}, agentId={}",
        savedPreferredAgent.getId(),
        organizationId,
        request.agentId());

    return preferredAgentMapper.toDetailResponse(savedPreferredAgent);
  }

  @Transactional
  public void delete(UUID preferredAgentId) {
    PreferredAgent preferredAgent = requireAccessibleById(preferredAgentId);

    auditTemplate.execute(
        preferredAgent,
        preferredAgent.getOrganizationId(),
        DomainEvents.PreferredAgent.DELETED,
        () -> preferredAgentRepository.delete(preferredAgent));

    log.info(
        "Preferred agent deleted: preferredAgentId={}, organizationId={}",
        preferredAgentId,
        preferredAgent.getOrganizationId());
  }

  @Transactional(readOnly = true)
  public PreferredAgentDetailResponse getById(UUID preferredAgentId) {
    return preferredAgentMapper.toDetailResponse(requireAccessibleById(preferredAgentId));
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
    log.info("Updating preferred agent: preferredAgentId={}", preferredAgentId);

    validateUpdateRequest(request);

    PreferredAgent preferredAgent = requireAccessibleById(preferredAgentId);

    if (request.notes() != null) {
      preferredAgent.updateNotes(request.notes());
    }

    if (request.priority() != null) {
      preferredAgent.updatePriority(request.priority());
    }

    PreferredAgent updatedPreferredAgent =
        auditTemplate.execute(
            preferredAgent,
            preferredAgent.getOrganizationId(),
            DomainEvents.PreferredAgent.UPDATED,
            () -> preferredAgentRepository.save(preferredAgent));

    log.info(
        "Preferred agent updated: preferredAgentId={}, organizationId={}",
        preferredAgentId,
        updatedPreferredAgent.getOrganizationId());

    return preferredAgentMapper.toDetailResponse(updatedPreferredAgent);
  }

  void deleteByAgentId(UUID agentId) {
    log.info("Deleting preferred agent entries for agent: agentId={}", agentId);
    preferredAgentRepository.deleteByAgentId(agentId);
    log.info("Preferred agent entries deleted: agentId={}", agentId);
  }

  private boolean hasSystemAdminRole(ExecutionContext.UserContext userContext) {
    return userContext.roles().contains(UserRole.SYSTEM_ADMIN.name());
  }

  private PreferredAgent requireAccessibleById(UUID preferredAgentId) {
    return switch (ExecutionContext.get()) {
      case ExecutionContext.SystemContext ignored -> requireById(preferredAgentId);
      case ExecutionContext.UserContext userContext ->
          hasSystemAdminRole(userContext)
              ? requireById(preferredAgentId)
              : requireByIdAndOrganizationId(
                  preferredAgentId, requireActiveOrganization(userContext));
    };
  }

  private String requireActiveOrganization(ExecutionContext.UserContext userContext) {
    String organizationId = userContext.activeOrganization();

    if (organizationId == null || organizationId.isBlank()) {
      throw new AuthorizationException(
          ResponseCode.FORBIDDEN, "No active organization in execution context");
    }

    return organizationId;
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
    PreferredAgent preferredAgent =
        preferredAgentRepository
            .findById(preferredAgentId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        ResponseCode.RESOURCE_NOT_FOUND,
                        "Preferred agent with ID " + preferredAgentId + " not found"));

    if (!preferredAgent.getOrganizationId().equals(organizationId)) {
      throw new AuthorizationException(
          ResponseCode.FORBIDDEN,
          "Preferred agent with ID " + preferredAgentId + " does not belong to your organization");
    }

    return preferredAgent;
  }

  private void requireEligibleAgent(UUID agentId) {
    if (AvailabilityStatus.DEACTIVATED.equals(requireAgentById(agentId).getAvailabilityStatus())) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Deactivated agents cannot be added to a preferred agents list");
    }
  }

  private String requireOrganizationId(String organizationId) {
    if (organizationId == null || organizationId.isBlank()) {
      throw new ValidationException(
          ResponseCode.INVALID_ARGUMENTS,
          "organizationId is required",
          "organizationId",
          organizationId);
    }

    return organizationId;
  }

  private String resolveOrganizationId(String requestedOrganizationId) {
    return switch (ExecutionContext.get()) {
      case ExecutionContext.SystemContext ignored -> requireOrganizationId(requestedOrganizationId);
      case ExecutionContext.UserContext userContext -> {
        if (hasSystemAdminRole(userContext)) {
          yield requireOrganizationId(requestedOrganizationId);
        }

        String activeOrganization = requireActiveOrganization(userContext);

        if (requestedOrganizationId != null
            && !requestedOrganizationId.isBlank()
            && !activeOrganization.equals(requestedOrganizationId)) {
          throw new AuthorizationException(
              ResponseCode.FORBIDDEN, "Requested organization does not match active organization");
        }

        yield activeOrganization;
      }
    };
  }

  private void validateNoDuplicate(CreatePreferredAgentRequest request, String organizationId) {
    boolean exists =
        request.branchId() != null
            ? preferredAgentRepository.existsByOrganizationIdAndBranchIdAndAgentId(
                organizationId, request.branchId(), request.agentId())
            : preferredAgentRepository.existsByOrganizationIdAndAgentId(
                organizationId, request.agentId());

    if (exists) {
      throw new DuplicateResourceException(
          ResponseCode.DUPLICATE_RESOURCE,
          "Agent " + request.agentId() + " is already in the preferred agents list");
    }
  }

  private void validateUpdateRequest(UpdatePreferredAgentRequest request) {
    if (request.notes() == null && request.priority() == null) {
      throw new ValidationException(
          ResponseCode.INVALID_ARGUMENTS,
          "At least one of priority or notes must be provided",
          "[priority/notes]",
          null);
    }
  }
}
