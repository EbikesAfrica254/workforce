package com.ebikes.workforce.services.agents.metrics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.workforce.constants.EventConstants.DomainEvents;
import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.services.agents.AgentScopedService;
import com.ebikes.workforce.support.audit.AuditTemplate;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MetricsService extends AgentScopedService {

  private static final int RELIABILITY_SCORE_RECALCULATION_INTERVAL = 5;

  private final AuditTemplate auditTemplate;

  public MetricsService(AgentRepository agentRepository, AuditTemplate auditTemplate) {
    super(agentRepository);
    this.auditTemplate = auditTemplate;
  }

  @Transactional
  public void recordAssigned(UUID agentId) {
    log.info("Recording assignment: agentId={}", agentId);

    Agent agent = requireAgentById(agentId);

    auditTemplate.execute(
        agent,
        null,
        DomainEvents.Agent.AVAILABILITY_CHANGED,
        () -> {
          agent.incrementCurrentOrders();
          agentRepository.save(agent);
        });

    log.info("Current orders incremented: agentId={}", agentId);
  }

  @Transactional
  public void recordDeliveryCompleted(UUID agentId, boolean onTime) {
    log.info("Recording delivery completed: agentId={}, onTime={}", agentId, onTime);

    Agent agent = requireAgentById(agentId);

    auditTemplate.execute(
        agent,
        null,
        DomainEvents.Agent.AVAILABILITY_CHANGED,
        () -> {
          agent.incrementCompletedDeliveries();

          if (onTime) {
            agent.incrementOnTimeDeliveries();
          }

          if (agent.getCompletedDeliveryCount() % RELIABILITY_SCORE_RECALCULATION_INTERVAL == 0) {
            agent.updateReliabilityScore(calculateReliabilityScore(agent));
          }

          agent.decrementCurrentOrders();
          agentRepository.save(agent);
        });

    log.info(
        "Delivery recorded: agentId={}, completedDeliveryCount={}, onTimeDeliveryCount={}",
        agentId,
        agent.getCompletedDeliveryCount(),
        agent.getOnTimeDeliveryCount());
  }

  @Transactional
  public void recordOrderCancelled(UUID agentId) {
    log.info("Recording order cancellation: agentId={}", agentId);

    Agent agent = requireAgentById(agentId);

    auditTemplate.execute(
        agent,
        null,
        DomainEvents.Agent.AVAILABILITY_CHANGED,
        () -> {
          agent.decrementCurrentOrders();
          agentRepository.save(agent);
        });

    log.info("Current orders decremented on cancellation: agentId={}", agentId);
  }

  private BigDecimal calculateReliabilityScore(Agent agent) {
    BigDecimal onTimeRate =
        BigDecimal.valueOf(agent.getOnTimeDeliveryCount())
            .divide(BigDecimal.valueOf(agent.getCompletedDeliveryCount()), 4, RoundingMode.HALF_UP);

    return BigDecimal.valueOf(0.6)
        .add(BigDecimal.valueOf(0.4).multiply(onTimeRate))
        .multiply(BigDecimal.valueOf(100))
        .setScale(2, RoundingMode.HALF_UP);
  }
}
