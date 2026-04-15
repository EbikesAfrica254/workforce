package com.ebikes.workforce.services.agents;

import com.ebikes.workforce.constants.EventConstants.DomainEvents;
import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.projections.AgentConflictCheck;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.specifications.AgentSpecifications;
import com.ebikes.workforce.dtos.internal.FieldChange;
import com.ebikes.workforce.dtos.requests.agents.CreateAgentRequest;
import com.ebikes.workforce.dtos.requests.agents.UpdateAgentRequest;
import com.ebikes.workforce.dtos.requests.documents.DocumentUploadInfo;
import com.ebikes.workforce.dtos.requests.filters.AgentFilter;
import com.ebikes.workforce.dtos.responses.agents.AgentDetailResponse;
import com.ebikes.workforce.dtos.responses.agents.AgentSummaryResponse;
import com.ebikes.workforce.dtos.responses.api.PaginatedResponse;
import com.ebikes.workforce.enums.ResponseCode;
import com.ebikes.workforce.exceptions.DuplicateResourceException;
import com.ebikes.workforce.exceptions.ResourceNotFoundException;
import com.ebikes.workforce.mappers.AgentMapper;
import com.ebikes.workforce.services.agents.document.DocumentService;
import com.ebikes.workforce.support.audit.AuditTemplate;
import com.ebikes.workforce.support.changes.SnapshotCreator;
import com.ebikes.workforce.support.database.FilterUtilities;
import com.ebikes.workforce.support.makerchecker.MakerCheckerTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class AgentsService extends AgentScopedService {

  private static final String OPERATION_CREATE = "CREATE";
  private static final String OPERATION_UPDATE = "UPDATE";

  private final AccessPolicy accessPolicy;
  private final AgentMapper agentMapper;
  private final AuditTemplate auditTemplate;
  private final DocumentService documentService;
  private final MakerCheckerTemplate makerCheckerTemplate;
  private final SnapshotCreator snapshotCreator;

  public AgentsService(
      AccessPolicy accessPolicy,
      AgentMapper agentMapper,
      AgentRepository agentRepository,
      AuditTemplate auditTemplate,
      DocumentService documentService,
      MakerCheckerTemplate makerCheckerTemplate,
      SnapshotCreator snapshotCreator) {
    super(agentRepository);
    this.accessPolicy = accessPolicy;
    this.agentMapper = agentMapper;
    this.auditTemplate = auditTemplate;
    this.documentService = documentService;
    this.makerCheckerTemplate = makerCheckerTemplate;
    this.snapshotCreator = snapshotCreator;
  }

  @Transactional
  public AgentDetailResponse create(CreateAgentRequest request) {
    log.info("Registering agent: phoneNumber={}", request.phoneNumber());

    AgentConflictCheck conflicts =
        agentRepository.checkForConflicts(
            request.phoneNumber(), request.nationalIdNumber(), request.userId());

    if (conflicts.isPhoneExists()) {
      throw new DuplicateResourceException(
          ResponseCode.DUPLICATE_RESOURCE,
          "Agent with phone number " + request.phoneNumber() + " already exists");
    }
    if (conflicts.isNationalIdExists()) {
      throw new DuplicateResourceException(
          ResponseCode.DUPLICATE_RESOURCE,
          "Agent with national ID " + request.nationalIdNumber() + " already exists");
    }
    if (conflicts.isUserIdExists()) {
      throw new DuplicateResourceException(
          ResponseCode.DUPLICATE_RESOURCE, "An agent record already exists for this user");
    }

    accessPolicy.canCreate(request.userId());

    Agent agent = buildAgent(request);
    agent = agentRepository.save(agent);

    documentService.associateWithAgent(
        request.documents().stream().map(DocumentUploadInfo::key).toList(), agent);

    agent = requireAgentById(agent.getId());

    List<FieldChange> changes = snapshotCreator.extractFields(agent);
    makerCheckerTemplate.publish(agent, null, OPERATION_CREATE, changes);

    log.info("Agent registered: agentId={}, phoneNumber={}", agent.getId(), agent.getPhoneNumber());

    return agentMapper.toDetailResponse(agent);
  }

  @Transactional
  public AgentDetailResponse deactivate(UUID agentId) {
    log.info("Deactivating agent: agentId={}", agentId);

    Agent agent = requireAgentById(agentId);

    Agent updated =
        auditTemplate.execute(
            agent,
            null,
            DomainEvents.Agent.DEACTIVATED,
            () -> {
              agent.deactivate();
              return agentRepository.save(agent);
            });

    log.info("Agent deactivated: agentId={}", agentId);

    return agentMapper.toDetailResponse(updated);
  }

  @Transactional(readOnly = true)
  public AgentDetailResponse getById(UUID agentId) {
    return agentMapper.toDetailResponse(requireAgentById(agentId));
  }

  @Transactional(readOnly = true)
  public AgentDetailResponse getByUserId(String userId) {
    return agentMapper.toDetailResponse(requireByUserId(userId));
  }

  @Transactional
  public AgentDetailResponse resubmit(UUID agentId) {
    log.info("Resubmitting agent after rejection: agentId={}", agentId);

    Agent agent = requireAgentById(agentId);
    accessPolicy.owns(agent);

    Agent updated =
        auditTemplate.execute(
            agent,
            null,
            DomainEvents.Agent.RESUBMITTED,
            () -> {
              agent.resubmit();
              return agentRepository.save(agent);
            });

    List<FieldChange> changes = snapshotCreator.extractFields(updated);
    makerCheckerTemplate.publish(updated, null, OPERATION_CREATE, changes);

    log.info("Agent resubmitted: agentId={}", agentId);

    return agentMapper.toDetailResponse(updated);
  }

  @Transactional(readOnly = true)
  public PaginatedResponse<AgentSummaryResponse> search(AgentFilter filter) {
    log.info("Searching agents");

    Pageable pageable =
        FilterUtilities.buildPageable(filter, AgentSpecifications.ALLOWED_SORT_FIELDS);
    Page<AgentSummaryResponse> page =
        agentRepository
            .findAll(AgentSpecifications.buildSpecification(filter), pageable)
            .map(agentMapper::toSummaryResponse);

    return PaginatedResponse.from("Agent successfully retrieved.", page);
  }

  @Transactional
  public AgentDetailResponse update(UUID agentId, UpdateAgentRequest request) {
    log.info("Submitting agent update for approval: agentId={}", agentId);

    Agent existing = requireAgentById(agentId);
    accessPolicy.owns(existing);

    List<FieldChange> changes = snapshotCreator.extractChanges(request, existing);
    if (changes.isEmpty()) {
      log.info("No changes detected for agent: agentId={}", agentId);
      return agentMapper.toDetailResponse(existing);
    }

    makerCheckerTemplate.publish(existing, null, OPERATION_UPDATE, changes);

    log.info(
        "Agent update submitted for approval: agentId={}, changesCount={}",
        agentId,
        changes.size());

    return agentMapper.toDetailResponse(existing);
  }

  private static Agent buildAgent(CreateAgentRequest request) {
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
        .maxConcurrentOrders(maxConcurrentOrders)
        .nationalIdNumber(request.nationalIdNumber())
        .nationalIdType(request.nationalIdType())
        .phoneNumber(request.phoneNumber())
        .userId(request.userId())
        .build();
  }

  private Agent requireByUserId(String userId) {
    return agentRepository
        .findByUserId(userId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND, "Agent not found for userId=" + userId));
  }
}
