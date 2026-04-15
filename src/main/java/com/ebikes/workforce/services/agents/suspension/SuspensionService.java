package com.ebikes.workforce.services.agents.suspension;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.workforce.constants.EventConstants.DomainEvents;
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
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.exceptions.ResourceNotFoundException;
import com.ebikes.workforce.mappers.SuspensionMapper;
import com.ebikes.workforce.services.agents.AgentScopedService;
import com.ebikes.workforce.support.audit.AuditTemplate;
import com.ebikes.workforce.support.context.ExecutionContext;
import com.ebikes.workforce.support.database.FilterUtilities;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SuspensionService extends AgentScopedService {

  private final AuditTemplate auditTemplate;
  private final AvailabilityLogRepository availabilityLogRepository;
  private final SuspensionMapper suspensionMapper;
  private final SuspensionRepository suspensionRepository;

  public SuspensionService(
      AgentRepository agentRepository,
      AuditTemplate auditTemplate,
      AvailabilityLogRepository availabilityLogRepository,
      SuspensionMapper suspensionMapper,
      SuspensionRepository suspensionRepository) {
    super(agentRepository);
    this.auditTemplate = auditTemplate;
    this.availabilityLogRepository = availabilityLogRepository;
    this.suspensionMapper = suspensionMapper;
    this.suspensionRepository = suspensionRepository;
  }

  @Transactional(readOnly = true)
  public SuspensionDetailResponse getById(UUID suspensionId) {
    return suspensionMapper.toDetailResponse(requireSuspensionById(suspensionId));
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

    Suspension liftedSuspension =
        auditTemplate.execute(
            suspension,
            null,
            DomainEvents.Agent.SUSPENSION_LIFTED,
            () -> {
              suspension.lift(requireActorId());
              Suspension savedSuspension = suspensionRepository.save(suspension);

              agent.liftSuspension();
              agentRepository.save(agent);

              availabilityLogRepository.save(
                  AvailabilityLog.create(
                      agentId,
                      AvailabilityStatus.SUSPENDED,
                      AvailabilityStatus.OFFLINE,
                      request.notes()));

              return savedSuspension;
            });

    log.info("Suspension lifted: agentId={}, suspensionId={}", agentId, liftedSuspension.getId());

    return suspensionMapper.toDetailResponse(liftedSuspension);
  }

  @Transactional(readOnly = true)
  public PaginatedResponse<SuspensionSummaryResponse> search(SuspensionFilter filter) {
    log.info("Searching suspension");

    Specification<Suspension> spec = SuspensionSpecifications.buildSpecification(filter);
    Pageable pageable =
        FilterUtilities.buildPageable(filter, SuspensionSpecifications.ALLOWED_SORT_FIELDS);
    Page<SuspensionSummaryResponse> page =
        suspensionRepository.findAll(spec, pageable).map(suspensionMapper::toSummaryResponse);

    return PaginatedResponse.from("Suspensions successfully retrieved.", page);
  }

  @Transactional
  public SuspensionDetailResponse suspend(UUID agentId, SuspendAgentRequest request) {
    log.info("Suspending agent: agentId={}", agentId);

    Agent agent = requireAgentById(agentId);
    AvailabilityStatus previousStatus = agent.getAvailabilityStatus();

    Suspension suspension =
        Suspension.builder()
            .agentId(agentId)
            .expiresAt(request.expiresAt())
            .notes(request.notes())
            .reason(request.reason())
            .build();

    Suspension saved =
        auditTemplate.execute(
            suspension,
            null,
            DomainEvents.Agent.SUSPENDED,
            () -> {
              agent.suspend();
              agentRepository.save(agent);

              Suspension savedSuspension = suspensionRepository.save(suspension);

              availabilityLogRepository.save(
                  AvailabilityLog.create(
                      agentId, previousStatus, AvailabilityStatus.SUSPENDED, request.notes()));

              return savedSuspension;
            });

    log.info("Agent suspended: agentId={}, suspensionId={}", agentId, saved.getId());

    return suspensionMapper.toDetailResponse(saved);
  }

  private String requireActorId() {
    return switch (ExecutionContext.get()) {
      case ExecutionContext.SystemContext ignored ->
          throw new BusinessRuleException(
              ResponseCode.INVALID_STATE, "Suspension lifting requires a user context");
      case ExecutionContext.UserContext userContext -> userContext.userId();
    };
  }

  private Suspension requireSuspensionById(UUID suspensionId) {
    return suspensionRepository
        .findById(suspensionId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND,
                    "Suspension with ID " + suspensionId + " not found"));
  }
}
