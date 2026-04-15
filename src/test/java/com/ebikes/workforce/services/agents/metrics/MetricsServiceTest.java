package com.ebikes.workforce.services.agents.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.exceptions.ResourceNotFoundException;
import com.ebikes.workforce.support.audit.AuditTemplate;
import com.ebikes.workforce.support.audit.ThrowingRunnable;
import com.ebikes.workforce.support.fixtures.AgentFixtures;

@DisplayName("MetricsService")
@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

  @Mock private AgentRepository agentRepository;
  @Mock private AuditTemplate auditTemplate;

  @InjectMocks private MetricsService metricsService;

  @SuppressWarnings("unchecked")
  private void wireAuditTemplate() {
    doAnswer(
            invocation -> {
              ThrowingRunnable<?> operation = invocation.getArgument(3);
              operation.run();
              return null;
            })
        .when(auditTemplate)
        .execute(any(), any(), any(), any(ThrowingRunnable.class));
  }

  @Nested
  @DisplayName("recordAssigned")
  class RecordAssigned {

    @Test
    @DisplayName("should throw ResourceNotFoundException when agent not found")
    void shouldThrowWhenAgentNotFound() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> metricsService.recordAssigned(AgentFixtures.AGENT_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should increment current orders and save")
    void shouldIncrementCurrentOrdersAndSave() {
      Agent agent = AgentFixtures.withCurrentOrders(0);
      wireAuditTemplate();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(agentRepository.save(agent)).thenReturn(agent);

      metricsService.recordAssigned(AgentFixtures.AGENT_ID);

      assertThat(agent.getCurrentOrders()).isEqualTo(1);
      verify(agentRepository).save(agent);
    }

    @Test
    @DisplayName("should throw BusinessRuleException when current orders at max")
    void shouldThrowWhenCurrentOrdersAtMax() {
      Agent agent = AgentFixtures.withCurrentOrders(2);
      wireAuditTemplate();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));

      assertThatThrownBy(() -> metricsService.recordAssigned(AgentFixtures.AGENT_ID))
          .isInstanceOf(BusinessRuleException.class);
    }
  }

  @Nested
  @DisplayName("recordDeliveryCompleted")
  class RecordDeliveryCompleted {

    @Test
    @DisplayName("should throw ResourceNotFoundException when agent not found")
    void shouldThrowWhenAgentNotFound() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> metricsService.recordDeliveryCompleted(AgentFixtures.AGENT_ID, true))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw BusinessRuleException when current orders already zero")
    void shouldThrowWhenCurrentOrdersAlreadyZero() {
      Agent agent = AgentFixtures.withCurrentOrders(0);
      wireAuditTemplate();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));

      assertThatThrownBy(
              () -> metricsService.recordDeliveryCompleted(AgentFixtures.AGENT_ID, false))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should increment completed deliveries, decrement current orders and save")
    void shouldIncrementCompletedAndDecrementCurrentOrders() {
      Agent agent = AgentFixtures.withCurrentOrders(1);
      wireAuditTemplate();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(agentRepository.save(agent)).thenReturn(agent);

      metricsService.recordDeliveryCompleted(AgentFixtures.AGENT_ID, false);

      assertThat(agent.getCompletedDeliveryCount()).isEqualTo(1);
      assertThat(agent.getCurrentOrders()).isZero();
      verify(agentRepository).save(agent);
    }

    @Test
    @DisplayName("should increment on-time deliveries when onTime is true")
    void shouldIncrementOnTimeDeliveriesWhenOnTime() {
      Agent agent = AgentFixtures.withCurrentOrders(1);
      wireAuditTemplate();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(agentRepository.save(agent)).thenReturn(agent);

      metricsService.recordDeliveryCompleted(AgentFixtures.AGENT_ID, true);

      assertThat(agent.getOnTimeDeliveryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("should not increment on-time deliveries when onTime is false")
    void shouldNotIncrementOnTimeDeliveriesWhenNotOnTime() {
      Agent agent = AgentFixtures.withCurrentOrders(1);
      wireAuditTemplate();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(agentRepository.save(agent)).thenReturn(agent);

      metricsService.recordDeliveryCompleted(AgentFixtures.AGENT_ID, false);

      assertThat(agent.getOnTimeDeliveryCount()).isZero();
    }

    @Test
    @DisplayName("should recalculate reliability score at 5th completed delivery with score 100.00")
    void shouldRecalculateReliabilityScoreAtInterval() {
      Agent agent = AgentFixtures.withDeliveryMetrics(4, 4, 1);
      wireAuditTemplate();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(agentRepository.save(agent)).thenReturn(agent);

      metricsService.recordDeliveryCompleted(AgentFixtures.AGENT_ID, true);

      assertThat(agent.getCompletedDeliveryCount()).isEqualTo(5);
      assertThat(agent.getOnTimeDeliveryCount()).isEqualTo(5);
      assertThat(agent.getReliabilityScore()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("should not recalculate reliability score before interval threshold")
    void shouldNotRecalculateReliabilityScoreBeforeInterval() {
      Agent agent = AgentFixtures.withDeliveryMetrics(3, 3, 1);
      wireAuditTemplate();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(agentRepository.save(agent)).thenReturn(agent);

      metricsService.recordDeliveryCompleted(AgentFixtures.AGENT_ID, true);

      assertThat(agent.getCompletedDeliveryCount()).isEqualTo(4);
      assertThat(agent.getReliabilityScore()).isNull();
    }
  }

  @Nested
  @DisplayName("recordOrderCancelled")
  class RecordOrderCancelled {

    @Test
    @DisplayName("should throw ResourceNotFoundException when agent not found")
    void shouldThrowWhenAgentNotFound() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> metricsService.recordOrderCancelled(AgentFixtures.AGENT_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should decrement current orders and save")
    void shouldDecrementCurrentOrdersAndSave() {
      Agent agent = AgentFixtures.withCurrentOrders(1);
      wireAuditTemplate();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(agentRepository.save(agent)).thenReturn(agent);

      metricsService.recordOrderCancelled(AgentFixtures.AGENT_ID);

      assertThat(agent.getCurrentOrders()).isZero();
      verify(agentRepository).save(agent);
    }

    @Test
    @DisplayName("should throw BusinessRuleException when current orders already zero")
    void shouldThrowWhenCurrentOrdersAlreadyZero() {
      Agent agent = AgentFixtures.withCurrentOrders(0);
      wireAuditTemplate();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));

      assertThatThrownBy(() -> metricsService.recordOrderCancelled(AgentFixtures.AGENT_ID))
          .isInstanceOf(BusinessRuleException.class);
    }
  }
}
