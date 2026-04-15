package com.ebikes.workforce.services.agents.availability;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.workforce.constants.EventConstants.DomainEvents;
import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.AvailabilityLog;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.AvailabilityLogRepository;
import com.ebikes.workforce.dtos.responses.agents.AgentDetailResponse;
import com.ebikes.workforce.dtos.responses.agents.AvailabilityLogResponse;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.ResponseCode;
import com.ebikes.workforce.enums.UserRole;
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.mappers.AgentMapper;
import com.ebikes.workforce.mappers.AvailabilityLogMapper;
import com.ebikes.workforce.services.agents.AgentScopedService;
import com.ebikes.workforce.support.audit.AuditTemplate;
import com.ebikes.workforce.support.context.ExecutionContext;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AvailabilityService extends AgentScopedService {

  private static final Set<AvailabilityStatus> REQUESTABLE_STATUSES =
      Set.of(
          AvailabilityStatus.AVAILABLE,
          AvailabilityStatus.BUSY,
          AvailabilityStatus.OFFLINE,
          AvailabilityStatus.UNAVAILABLE);

  private final AgentMapper agentMapper;
  private final AuditTemplate auditTemplate;
  private final AvailabilityLogMapper availabilityLogMapper;
  private final AvailabilityLogRepository availabilityLogRepository;

  public AvailabilityService(
      AgentMapper agentMapper,
      AgentRepository agentRepository,
      AuditTemplate auditTemplate,
      AvailabilityLogMapper availabilityLogMapper,
      AvailabilityLogRepository availabilityLogRepository) {
    super(agentRepository);
    this.agentMapper = agentMapper;
    this.auditTemplate = auditTemplate;
    this.availabilityLogMapper = availabilityLogMapper;
    this.availabilityLogRepository = availabilityLogRepository;
  }

  @Transactional(readOnly = true)
  public Page<AvailabilityLogResponse> getAvailabilityLog(UUID agentId, Pageable pageable) {
    requireAgentById(agentId);

    return availabilityLogRepository
        .findAll((root, query, cb) -> cb.equal(root.get("agentId"), agentId), pageable)
        .map(availabilityLogMapper::toResponse);
  }

  @Transactional
  public AgentDetailResponse updateAvailability(
      UUID agentId, AvailabilityStatus targetStatus, String reason) {
    log.info("Updating availability: agentId={}, targetStatus={}", agentId, targetStatus);

    if (!REQUESTABLE_STATUSES.contains(targetStatus)) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE, "Cannot transition agent to status: " + targetStatus);
    }

    Agent agent = requireAgentById(agentId);
    validateAvailabilityChangePermission(targetStatus);

    AvailabilityStatus previousStatus = agent.getAvailabilityStatus();

    Agent updated =
        auditTemplate.execute(
            agent,
            null,
            DomainEvents.Agent.AVAILABILITY_CHANGED,
            () -> {
              applyAvailabilityTransition(agent, targetStatus);
              Agent saved = agentRepository.save(agent);

              availabilityLogRepository.save(
                  AvailabilityLog.create(agentId, previousStatus, targetStatus, reason));

              return saved;
            });

    log.info(
        "Availability updated: agentId={}, previousStatus={}, newStatus={}",
        agentId,
        previousStatus,
        targetStatus);

    return agentMapper.toDetailResponse(updated);
  }

  private void applyAvailabilityTransition(Agent agent, AvailabilityStatus targetStatus) {
    switch (targetStatus) {
      case AVAILABLE -> agent.markAvailable();
      case BUSY -> agent.markBusy();
      case OFFLINE -> agent.markOffline();
      case UNAVAILABLE -> agent.markUnavailable();
      default -> throw new IllegalStateException("Unhandled availability status: " + targetStatus);
    }
  }

  private void validateAvailabilityChangePermission(AvailabilityStatus targetStatus) {
    if (ExecutionContext.get() instanceof ExecutionContext.UserContext userContext
        && userContext.roles().contains(UserRole.AGENT.name())
        && targetStatus == AvailabilityStatus.BUSY) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE, "Agent cannot self-assign status: " + targetStatus);
    }
  }
}
