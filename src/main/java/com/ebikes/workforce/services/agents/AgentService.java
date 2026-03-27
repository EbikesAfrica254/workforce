package com.ebikes.workforce.services.agents;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.workforce.configurations.properties.H3Properties;
import com.ebikes.workforce.constants.EventConstants.EventSource;
import com.ebikes.workforce.constants.EventConstants.EventTypes;
import com.ebikes.workforce.constants.EventConstants.RoutingKeys;
import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.AvailabilityLog;
import com.ebikes.workforce.database.entities.LocationHistory;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.AvailabilityLogRepository;
import com.ebikes.workforce.database.repositories.LocationHistoryRepository;
import com.ebikes.workforce.database.specifications.AgentSpecifications;
import com.ebikes.workforce.database.specifications.LocationHistorySpecifications;
import com.ebikes.workforce.dtos.events.incoming.MakerCheckerDecision;
import com.ebikes.workforce.dtos.internal.FieldChange;
import com.ebikes.workforce.dtos.requests.agents.CreateAgentRequest;
import com.ebikes.workforce.dtos.requests.agents.UpdateAgentRequest;
import com.ebikes.workforce.dtos.requests.agents.UpdateLocationRequest;
import com.ebikes.workforce.dtos.requests.documents.DocumentUploadInfo;
import com.ebikes.workforce.dtos.requests.filters.AgentFilter;
import com.ebikes.workforce.dtos.requests.filters.LocationHistoryFilter;
import com.ebikes.workforce.dtos.responses.agents.AgentDetailResponse;
import com.ebikes.workforce.dtos.responses.agents.AgentSummaryResponse;
import com.ebikes.workforce.dtos.responses.agents.AvailabilityLogResponse;
import com.ebikes.workforce.dtos.responses.agents.LocationHistoryResponse;
import com.ebikes.workforce.dtos.responses.api.PaginatedResponse;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.CheckerOutcome;
import com.ebikes.workforce.enums.LocationSource;
import com.ebikes.workforce.enums.ResponseCode;
import com.ebikes.workforce.enums.UserRole;
import com.ebikes.workforce.exceptions.AuthorizationException;
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.exceptions.DuplicateResourceException;
import com.ebikes.workforce.exceptions.ResourceNotFoundException;
import com.ebikes.workforce.mappers.AgentMapper;
import com.ebikes.workforce.mappers.AvailabilityLogMapper;
import com.ebikes.workforce.mappers.LocationHistoryMapper;
import com.ebikes.workforce.publishers.AuditEventPublisher;
import com.ebikes.workforce.publishers.MakerCheckerPublisher;
import com.ebikes.workforce.services.documents.DocumentService;
import com.ebikes.workforce.services.events.OutboxService;
import com.ebikes.workforce.support.audit.AuditMetadataBuilder;
import com.ebikes.workforce.support.changes.ChangeApplier;
import com.ebikes.workforce.support.changes.MakerCheckerRequestBuilder;
import com.ebikes.workforce.support.changes.SnapshotCreator;
import com.ebikes.workforce.support.context.ExecutionContext;
import com.ebikes.workforce.support.database.FilterUtilities;
import com.uber.h3core.H3Core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class AgentService {

  private final AgentMapper agentMapper;
  private final AgentRepository agentRepository;
  private final AuditEventPublisher auditEventPublisher;
  private final AvailabilityLogMapper availabilityLogMapper;
  private final AvailabilityLogRepository availabilityLogRepository;
  private final ChangeApplier changeApplier;
  private final DocumentService documentService;
  private final H3Core h3Core;
  private final H3Properties h3Properties;
  private final LocationHistoryMapper locationHistoryMapper;
  private final LocationHistoryRepository locationHistoryRepository;
  private final MakerCheckerPublisher makerCheckerPublisher;
  private final OutboxService outboxService;
  private final PreferredAgentService preferredAgentService;
  private final SnapshotCreator snapshotCreator;

  private static final String PREVIOUS_STATUS = "previousStatus";

  @Transactional
  public AgentDetailResponse create(CreateAgentRequest request) {
    log.info("Registering agent: phoneNumber={}", request.phoneNumber());

    if (agentRepository.existsByPhoneNumber(request.phoneNumber())) {
      throw new DuplicateResourceException(
          ResponseCode.DUPLICATE_RESOURCE,
          "Agent with phone number " + request.phoneNumber() + " already exists");
    }

    if (agentRepository.existsByNationalIdNumber(request.nationalIdNumber())) {
      throw new DuplicateResourceException(
          ResponseCode.DUPLICATE_RESOURCE,
          "Agent with national ID " + request.nationalIdNumber() + " already exists");
    }

    if (agentRepository.existsByUserId(request.userId())) {
      throw new DuplicateResourceException(
          ResponseCode.DUPLICATE_RESOURCE, "An agent record already exists for this user");
    }

    validateCreateAccess(request.userId());

    Agent agent = buildAgent(request);

    agent = agentRepository.save(agent);

    documentService.associateWithAgent(
        request.documents().stream().map(DocumentUploadInfo::key).toList(), agent);

    agent = requireById(agent.getId());

    auditEventPublisher.publishSuccess(
        agent.getId(),
        agent.getClass().getSimpleName(),
        EventTypes.Workforce.AGENT_CREATED,
        AuditMetadataBuilder.forAgent(agent),
        RoutingKeys.WORKFORCE_AGENT_AUDIT);

    List<FieldChange> changes = snapshotCreator.extractFields(agent);
    makerCheckerPublisher.publish(
        MakerCheckerRequestBuilder.forAgentCreate(agent, changes, ExecutionContext.getUserId()),
        RoutingKeys.makerCheckerRequest(EventSource.HOST, "agent"));

    log.info("Agent registered: agentId={}, phoneNumber={}", agent.getId(), agent.getPhoneNumber());

    return agentMapper.toDetailResponse(agent);
  }

  private static @NonNull Agent buildAgent(CreateAgentRequest request) {
    short maxConcurrentOrders =
        request.maxConcurrentOrders() != null
            ? request.maxConcurrentOrders()
            : request.capabilityClass().getDefaultMaxConcurrentOrders();

    return Agent.builder()
        .alternatePhoneNumber(request.alternatePhoneNumber())
        .capabilityClass(request.capabilityClass())
        .email(request.email())
        .firstName(request.firstName())
        .lastName(request.lastName())
        .nationalIdNumber(request.nationalIdNumber())
        .nationalIdType(request.nationalIdType())
        .phoneNumber(request.phoneNumber())
        .maxConcurrentOrders(maxConcurrentOrders)
        .userId(request.userId())
        .build();
  }

  @Transactional
  public AgentDetailResponse deactivate(UUID agentId) {
    log.info("Deactivating agent: agentId={}", agentId);

    Agent agent = requireById(agentId);
    AvailabilityStatus previousStatus = agent.getAvailabilityStatus();

    agent.deactivate();
    agentRepository.save(agent);

    availabilityLogRepository.save(
        new AvailabilityLog(
            agentId, previousStatus, AvailabilityStatus.DEACTIVATED, "Agent deactivated"));

    preferredAgentService.deleteByAgentId(agentId);

    auditEventPublisher.publishSuccess(
        agent.getId(),
        agent.getClass().getSimpleName(),
        EventTypes.Workforce.AGENT_DEACTIVATED,
        AuditMetadataBuilder.forAgent(agent, Map.of(PREVIOUS_STATUS, previousStatus.name())),
        RoutingKeys.WORKFORCE_AGENT_AUDIT);

    outboxService.save(
        EventTypes.Workforce.AGENT_AVAILABILITY_CHANGED,
        agent,
        RoutingKeys.WORKFORCE_AGENT_AVAILABILITY_CHANGED);

    log.info("Agent deactivated: agentId={}", agentId);

    return agentMapper.toDetailResponse(agent);
  }

  @Transactional(readOnly = true)
  public AgentDetailResponse getById(UUID agentId) {
    return agentMapper.toDetailResponse(requireById(agentId));
  }

  @Transactional(readOnly = true)
  public Page<AvailabilityLogResponse> getAvailabilityLog(UUID agentId, Pageable pageable) {
    requireById(agentId);
    return availabilityLogRepository
        .findAll((root, query, cb) -> cb.equal(root.get("agentId"), agentId), pageable)
        .map(availabilityLogMapper::toResponse);
  }

  @Transactional(readOnly = true)
  public Page<LocationHistoryResponse> getLocationHistory(
      LocationHistoryFilter filter, Pageable pageable) {
    requireById(filter.getAgentId());
    return locationHistoryRepository
        .findAll(LocationHistorySpecifications.buildSpecification(filter), pageable)
        .map(locationHistoryMapper::toResponse);
  }

  @Transactional
  public void handleApprovalDecision(MakerCheckerDecision decision) {
    log.info(
        "Processing agent maker-checker decision: entityId={}, operation={}, outcome={}",
        decision.entityId(),
        decision.operation(),
        decision.outcome());

    Agent agent = requireById(decision.entityId());
    boolean approved = decision.outcome() == CheckerOutcome.APPROVED;

    switch (decision.operation()) {
      case "CREATE" -> {
        if (approved) {
          recordCreateApproval(agent);
        } else {
          recordRejection(agent, decision.operation(), decision.reason());
        }
      }
      case "UPDATE" -> {
        if (approved) {
          recordUpdateApproval(agent, decision.originalChanges());
        } else {
          recordRejection(agent, decision.operation(), decision.reason());
        }
      }
      default ->
          throw new IllegalArgumentException(
              "Unrecognised operation '"
                  + decision.operation()
                  + "' for entityId="
                  + decision.entityId());
    }
  }

  @Transactional
  public void recordAssigned(String agentId) {
    log.info("Recording assignment: agentId={}", agentId);
    Agent agent = requireById(UUID.fromString(agentId));
    agent.incrementCurrentOrders();
    agentRepository.save(agent);
    log.info("Current orders incremented: agentId={}", agentId);
  }

  @Transactional
  public void recordDeliveryCompleted(UUID agentId, boolean onTime) {
    log.info("Recording delivery completed: agentId={}, onTime={}", agentId, onTime);

    Agent agent = requireById(agentId);
    agent.incrementCompletedDeliveries();

    if (onTime) {
      agent.incrementOnTimeDeliveries();
    }

    if (agent.getCompletedDeliveryCount() % 5 == 0) {
      BigDecimal onTimeRate =
          BigDecimal.valueOf(agent.getOnTimeDeliveryCount())
              .divide(
                  BigDecimal.valueOf(agent.getCompletedDeliveryCount()), 4, RoundingMode.HALF_UP);
      BigDecimal score =
          BigDecimal.valueOf(0.6)
              .add(BigDecimal.valueOf(0.4).multiply(onTimeRate))
              .multiply(BigDecimal.valueOf(100))
              .setScale(2, RoundingMode.HALF_UP);
      agent.updateReliabilityScore(score);
    }
    // TODO[PW]: - Entrypoint for rider split

    agent.decrementCurrentOrders();

    agentRepository.save(agent);

    log.info(
        "Delivery recorded: agentId={}, completedDeliveryCount={}, onTimeDeliveryCount={}",
        agentId,
        agent.getCompletedDeliveryCount(),
        agent.getOnTimeDeliveryCount());
  }

  @Transactional
  public void recordOrderCancelled(String agentId) {
    log.info("Recording order cancellation: agentId={}", agentId);
    Agent agent = requireById(UUID.fromString(agentId));
    agent.decrementCurrentOrders();
    agentRepository.save(agent);
    log.info("Current orders decremented on cancellation: agentId={}", agentId);
  }

  @Transactional
  public AgentDetailResponse resubmit(UUID agentId) {
    log.info("Resubmitting agent after rejection: agentId={}", agentId);

    Agent agent = requireById(agentId);
    enforceOwnershipIfAgent(agent);

    agent.resubmit();
    agent = agentRepository.save(agent);
    agent = requireById(agent.getId());

    auditEventPublisher.publishSuccess(
        agent.getId(),
        agent.getClass().getSimpleName(),
        EventTypes.Workforce.AGENT_RESUBMITTED,
        AuditMetadataBuilder.forAgent(agent),
        RoutingKeys.WORKFORCE_AGENT_AUDIT);

    List<FieldChange> changes = snapshotCreator.extractFields(agent);
    makerCheckerPublisher.publish(
        MakerCheckerRequestBuilder.forAgentCreate(agent, changes, ExecutionContext.getUserId()),
        RoutingKeys.makerCheckerRequest(EventSource.HOST, "agent"));

    log.info("Agent resubmitted: agentId={}", agentId);

    return agentMapper.toDetailResponse(agent);
  }

  @Transactional(readOnly = true)
  public PaginatedResponse<AgentSummaryResponse> search(AgentFilter filter) {
    log.info("Searching agents");
    Specification<Agent> spec = AgentSpecifications.buildSpecification(filter);
    Pageable pageable =
        FilterUtilities.buildPageable(filter, AgentSpecifications.ALLOWED_SORT_FIELDS);
    Page<AgentSummaryResponse> page =
        agentRepository.findAll(spec, pageable).map(agentMapper::toSummaryResponse);
    return PaginatedResponse.from("Agents successfully retrieved.", page);
  }

  @Transactional
  public AgentDetailResponse update(UUID agentId, UpdateAgentRequest request) {
    log.info("Updating agent: agentId={}", agentId);

    Agent agent = requireById(agentId);

    if (request.alternatePhoneNumber() != null) {
      agent.updateAlternatePhoneNumber(request.alternatePhoneNumber());
    }
    if (request.capabilityClass() != null) {
      agent.updateCapabilityClass(request.capabilityClass());
    }
    if (request.email() != null) {
      agent.updateEmail(request.email());
    }
    if (request.maxConcurrentOrders() != null) {
      agent.updateMaxConcurrentOrders(request.maxConcurrentOrders());
    }

    agent = agentRepository.save(agent);

    auditEventPublisher.publishSuccess(
        agent.getId(),
        agent.getClass().getSimpleName(),
        EventTypes.Workforce.AGENT_UPDATED,
        AuditMetadataBuilder.forAgent(agent),
        RoutingKeys.WORKFORCE_AGENT_AUDIT);

    log.info("Agent updated: agentId={}", agentId);

    return agentMapper.toDetailResponse(agent);
  }

  @Transactional
  public AgentDetailResponse updateAvailability(
      UUID agentId, AvailabilityStatus targetStatus, String reason) {
    log.info("Updating availability: agentId={}, targetStatus={}", agentId, targetStatus);

    Agent agent = requireById(agentId);

    Set<String> roles = ExecutionContext.getRoles();
    if (roles.contains(UserRole.AGENT.name())
        && (targetStatus == AvailabilityStatus.BUSY
            || targetStatus == AvailabilityStatus.DEACTIVATED
            || targetStatus == AvailabilityStatus.SUSPENDED)) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE, "Agents cannot self-assign status: " + targetStatus);
    }

    AvailabilityStatus previousStatus = agent.getAvailabilityStatus();

    switch (targetStatus) {
      case AVAILABLE -> agent.markAvailable();
      case BUSY -> agent.markBusy();
      case OFFLINE -> agent.markOffline();
      case UNAVAILABLE -> agent.markUnavailable();
      default ->
          throw new BusinessRuleException(
              ResponseCode.INVALID_STATE, "Cannot transition agent to status: " + targetStatus);
    }

    agentRepository.save(agent);

    availabilityLogRepository.save(
        new AvailabilityLog(agentId, previousStatus, targetStatus, reason));

    Map<String, String> auditMetadata =
        reason != null
            ? Map.of(PREVIOUS_STATUS, previousStatus.name(), "reason", reason)
            : Map.of(PREVIOUS_STATUS, previousStatus.name());

    auditEventPublisher.publishSuccess(
        agent.getId(),
        agent.getClass().getSimpleName(),
        EventTypes.Workforce.AGENT_AVAILABILITY_CHANGED,
        AuditMetadataBuilder.forAgent(agent, auditMetadata),
        RoutingKeys.WORKFORCE_AGENT_AUDIT);

    outboxService.save(
        EventTypes.Workforce.AGENT_AVAILABILITY_CHANGED,
        agent,
        RoutingKeys.WORKFORCE_AGENT_AVAILABILITY_CHANGED);

    log.info(
        "Availability updated: agentId={}, previousStatus={}, newStatus={}",
        agentId,
        previousStatus,
        targetStatus);

    return agentMapper.toDetailResponse(agent);
  }

  @Transactional
  public AgentDetailResponse updateLocation(
      UUID agentId, UpdateLocationRequest request, LocationSource source) {
    log.info("Updating location: agentId={}", agentId);

    Agent agent = requireById(agentId);

    enforceOwnershipIfAgent(agent);

    String h3Index =
        h3Core.latLngToCellAddress(
            request.latitude().doubleValue(),
            request.longitude().doubleValue(),
            h3Properties.getResolution());

    agent.updateLocation(request.latitude(), request.longitude(), h3Index, source);
    agentRepository.save(agent);

    locationHistoryRepository.save(
        new LocationHistory(agentId, request.latitude(), request.longitude(), h3Index, source));

    outboxService.save(
        EventTypes.Workforce.AGENT_LOCATION_UPDATED,
        agent,
        RoutingKeys.WORKFORCE_AGENT_LOCATION_UPDATED);

    log.info("Location updated: agentId={}, h3Index={}", agentId, h3Index);

    return agentMapper.toDetailResponse(agent);
  }

  Agent requireById(UUID agentId) {
    return agentRepository
        .findById(agentId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND, "Agent with ID " + agentId + " not found"));
  }

  private void enforceOwnershipIfAgent(Agent agent) {
    Set<String> roles = ExecutionContext.getRoles();
    if (roles.contains(UserRole.AGENT.name())
        && !agent.getUserId().equals(ExecutionContext.getUserId())) {
      throw new BusinessRuleException(
          ResponseCode.FORBIDDEN, "Access denied: agent does not own this resource");
    }
  }

  private boolean isPrivilegedCreator() {
    Set<String> roles = ExecutionContext.getRoles();

    return roles.contains(UserRole.SYSTEM_ADMIN.name())
        || roles.contains(UserRole.ORGANIZATION_ADMIN.name())
        || roles.contains(UserRole.ORGANIZATION_FLEET_MANAGER.name())
        || roles.contains(UserRole.ORGANIZATION_FLEET_SUPPORT.name())
        || roles.contains(UserRole.BRANCH_ADMIN.name())
        || roles.contains(UserRole.BRANCH_FLEET_MANAGER.name())
        || roles.contains(UserRole.BRANCH_FLEET_SUPPORT.name());
  }

  private void recordCreateApproval(Agent agent) {
    documentService.validateRequiredDocumentsUploaded(agent.getId(), agent.getCapabilityClass());
    documentService.activateDocuments(agent.getId());

    agent.approve();
    agentRepository.save(agent);

    auditEventPublisher.publishSuccess(
        agent.getId(),
        agent.getClass().getSimpleName(),
        EventTypes.Workforce.AGENT_APPROVED,
        AuditMetadataBuilder.forAgent(agent),
        RoutingKeys.WORKFORCE_AGENT_AUDIT);

    outboxService.save(
        EventTypes.Workforce.AGENT_AVAILABILITY_CHANGED,
        agent,
        RoutingKeys.WORKFORCE_AGENT_AVAILABILITY_CHANGED);

    log.info("Agent creation approved: agentId={}", agent.getId());
  }

  private void recordRejection(Agent agent, String operation, String reason) {
    agent.reject(reason);
    agentRepository.save(agent);

    auditEventPublisher.publishSuccess(
        agent.getId(),
        agent.getClass().getSimpleName(),
        EventTypes.Workforce.AGENT_REJECTED,
        AuditMetadataBuilder.forAgent(
            agent, Map.of("operation", operation, "rejectionReason", reason != null ? reason : "")),
        RoutingKeys.WORKFORCE_AGENT_AUDIT);

    log.info("Agent {} rejected: agentId={}", operation, agent.getId());
  }

  private void recordUpdateApproval(Agent agent, List<FieldChange> changes) {
    changeApplier.applyChanges(agent, changes);
    agentRepository.save(agent);

    auditEventPublisher.publishSuccess(
        agent.getId(),
        agent.getClass().getSimpleName(),
        EventTypes.Workforce.AGENT_APPROVED,
        AuditMetadataBuilder.forAgent(
            agent, Map.of("changesCount", String.valueOf(changes.size()))),
        RoutingKeys.WORKFORCE_AGENT_AUDIT);

    log.info(
        "Agent update approved and applied: agentId={}, changesCount={}",
        agent.getId(),
        changes.size());
  }

  private void validateCreateAccess(String requestedUserId) {
    String currentUserId = ExecutionContext.getUserId();

    if (isPrivilegedCreator()) {
      return;
    }

    if (!currentUserId.equals(requestedUserId)) {
      throw new AuthorizationException(
          ResponseCode.FORBIDDEN,
          "Access denied: authenticated user cannot create an agent for another user");
    }
  }
}
