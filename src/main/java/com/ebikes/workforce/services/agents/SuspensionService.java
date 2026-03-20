package com.ebikes.workforce.services.agents;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.workforce.constants.EventConstants.EventTypes;
import com.ebikes.workforce.constants.EventConstants.RoutingKeys;
import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.AvailabilityLog;
import com.ebikes.workforce.database.entities.Suspension;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.AvailabilityLogRepository;
import com.ebikes.workforce.database.repositories.SuspensionRepository;
import com.ebikes.workforce.database.specifications.SuspensionSpecifications;
import com.ebikes.workforce.dtos.requests.agents.LiftSuspensionRequest;
import com.ebikes.workforce.dtos.requests.agents.SuspendAgentRequest;
import com.ebikes.workforce.dtos.requests.filters.SuspensionFilter;
import com.ebikes.workforce.dtos.responses.api.PaginatedResponse;
import com.ebikes.workforce.dtos.responses.suspensions.SuspensionDetailResponse;
import com.ebikes.workforce.dtos.responses.suspensions.SuspensionSummaryResponse;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.ResponseCode;
import com.ebikes.workforce.exceptions.ResourceNotFoundException;
import com.ebikes.workforce.mappers.SuspensionMapper;
import com.ebikes.workforce.publishers.AuditEventPublisher;
import com.ebikes.workforce.services.events.OutboxService;
import com.ebikes.workforce.support.audit.AuditMetadataBuilder;
import com.ebikes.workforce.support.context.ExecutionContext;
import com.ebikes.workforce.support.database.FilterUtilities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class SuspensionService {

  private final AgentRepository agentRepository;
  private final AuditEventPublisher auditEventPublisher;
  private final AvailabilityLogRepository availabilityLogRepository;
  private final OutboxService outboxService;
  private final SuspensionMapper suspensionMapper;
  private final SuspensionRepository suspensionRepository;

  @Transactional
  public SuspensionDetailResponse suspend(UUID agentId, SuspendAgentRequest request) {
    log.info("Suspending agent: agentId={}", agentId);

    Agent agent = requireAgentById(agentId);
    AvailabilityStatus previousStatus = agent.getAvailabilityStatus();

    agent.suspend();
    agentRepository.save(agent);

    Suspension suspension =
        new Suspension(agentId, request.reason(), request.expiresAt(), request.notes());
    suspension = suspensionRepository.save(suspension);

    availabilityLogRepository.save(
        new AvailabilityLog(
            agentId, previousStatus, AvailabilityStatus.SUSPENDED, request.reason()));

    auditEventPublisher.publishSuccess(
        suspension.getId(),
        suspension.getClass().getSimpleName(),
        EventTypes.Workforce.AGENT_SUSPENDED,
        AuditMetadataBuilder.forSuspension(
            suspension, Map.of("previousStatus", previousStatus.name())),
        RoutingKeys.WORKFORCE_SUSPENSION_AUDIT);

    outboxService.save(
        EventTypes.Workforce.AGENT_AVAILABILITY_CHANGED,
        agent,
        RoutingKeys.WORKFORCE_AGENT_AVAILABILITY_CHANGED);

    log.info("Agent suspended: agentId={}, suspensionId={}", agentId, suspension.getId());

    return suspensionMapper.toDetailResponse(suspension);
  }

  @Transactional
  public SuspensionDetailResponse liftSuspension(UUID agentId, LiftSuspensionRequest request) {
    log.info("Lifting suspension: agentId={}", agentId);

    Agent agent = requireAgentById(agentId);

    Suspension suspension =
        suspensionRepository
            .findByAgentIdAndLiftedAtIsNull(agentId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        ResponseCode.RESOURCE_NOT_FOUND,
                        "No active suspension found for agent " + agentId));

    suspension.lift(ExecutionContext.getUserId());
    suspensionRepository.save(suspension);

    agent.liftSuspension();
    agentRepository.save(agent);

    availabilityLogRepository.save(
        new AvailabilityLog(
            agentId, AvailabilityStatus.SUSPENDED, AvailabilityStatus.OFFLINE, request.notes()));

    auditEventPublisher.publishSuccess(
        suspension.getId(),
        suspension.getClass().getSimpleName(),
        EventTypes.Workforce.AGENT_SUSPENSION_LIFTED,
        AuditMetadataBuilder.forSuspension(suspension),
        RoutingKeys.WORKFORCE_SUSPENSION_AUDIT);

    outboxService.save(
        EventTypes.Workforce.AGENT_AVAILABILITY_CHANGED,
        agent,
        RoutingKeys.WORKFORCE_AGENT_AVAILABILITY_CHANGED);

    log.info("Suspension lifted: agentId={}, suspensionId={}", agentId, suspension.getId());

    return suspensionMapper.toDetailResponse(suspension);
  }

  @Transactional(readOnly = true)
  public SuspensionDetailResponse getById(UUID suspensionId) {
    return suspensionMapper.toDetailResponse(requireById(suspensionId));
  }

  @Transactional(readOnly = true)
  public PaginatedResponse<SuspensionSummaryResponse> search(SuspensionFilter filter) {
    log.info("Searching suspensions");
    Specification<Suspension> spec = SuspensionSpecifications.buildSpecification(filter);
    Pageable pageable =
        FilterUtilities.buildPageable(filter, SuspensionSpecifications.ALLOWED_SORT_FIELDS);
    Page<SuspensionSummaryResponse> page =
        suspensionRepository.findAll(spec, pageable).map(suspensionMapper::toSummaryResponse);
    return PaginatedResponse.from("Suspensions successfully retrieved.", page);
  }

  private Agent requireAgentById(UUID agentId) {
    return agentRepository
        .findById(agentId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND, "Agent with ID " + agentId + " not found"));
  }

  private Suspension requireById(UUID suspensionId) {
    return suspensionRepository
        .findById(suspensionId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND,
                    "Suspension with ID " + suspensionId + " not found"));
  }
}
