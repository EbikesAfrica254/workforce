package com.ebikes.workforce.listeners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.workforce.constants.EventConstants.ExternalContracts;
import com.ebikes.workforce.dtos.events.incoming.OrderCancelledEvent;
import com.ebikes.workforce.services.agents.metrics.MetricsService;
import com.ebikes.workforce.services.events.InboxService;
import com.ebikes.workforce.support.fixtures.AgentFixtures;
import com.ebikes.workforce.support.fixtures.EventFixtures;

import tools.jackson.databind.ObjectMapper;

@DisplayName("OrderCancelledEventListener")
@ExtendWith(MockitoExtension.class)
class OrderCancelledEventListenerTest {

  private static final String DECREMENT_STATUS =
      ExternalContracts.ORDER_STATUSES_REQUIRING_AGENT_DECREMENT.iterator().next();
  private static final String NON_DECREMENT_STATUS = "DELIVERED";
  private static final UUID ORDER_ID = UUID.randomUUID();

  @Mock private InboxService inboxService;
  @Mock private MetricsService metricsService;
  @Spy private ObjectMapper objectMapper;

  @InjectMocks private OrderCancelledEventListener listener;

  private byte[] serialize(OrderCancelledEvent event) {
    return objectMapper.writeValueAsBytes(event);
  }

  @Nested
  @DisplayName("handle")
  class Handle {

    @Test
    @DisplayName("should skip all processing when event is a duplicate")
    void shouldSkipAllProcessingOnDuplicateEvent() {
      when(inboxService.receive(any(), any(), any())).thenReturn(false);

      listener.handle(
          serialize(
              EventFixtures.orderCancelledEvent(
                  AgentFixtures.AGENT_ID, DECREMENT_STATUS, ORDER_ID)));

      verify(metricsService, never()).recordOrderCancelled(any());
      verify(inboxService, never()).markProcessed(any());
    }

    @Test
    @DisplayName("should not call recordOrderCancelled when agentId is null")
    void shouldNotCallMetricsWhenAgentIdIsNull() {
      when(inboxService.receive(any(), any(), any())).thenReturn(true);

      OrderCancelledEvent event =
          EventFixtures.orderCancelledEvent(null, DECREMENT_STATUS, ORDER_ID);
      listener.handle(serialize(event));

      verify(metricsService, never()).recordOrderCancelled(any());
      verify(inboxService).markProcessed(event.serviceReference());
    }

    @Test
    @DisplayName("should not call recordOrderCancelled when status is not in the decrement set")
    void shouldNotCallMetricsWhenStatusNotInDecrementSet() {
      when(inboxService.receive(any(), any(), any())).thenReturn(true);

      OrderCancelledEvent event =
          EventFixtures.orderCancelledEvent(AgentFixtures.AGENT_ID, NON_DECREMENT_STATUS, ORDER_ID);
      listener.handle(serialize(event));

      verify(metricsService, never()).recordOrderCancelled(any());
      verify(inboxService).markProcessed(event.serviceReference());
    }

    @Test
    @DisplayName(
        "should call recordOrderCancelled and mark processed when agentId present and status in"
            + " decrement set")
    void shouldCallMetricsAndMarkProcessedWhenEligible() {
      when(inboxService.receive(any(), any(), any())).thenReturn(true);

      OrderCancelledEvent event =
          EventFixtures.orderCancelledEvent(AgentFixtures.AGENT_ID, DECREMENT_STATUS, ORDER_ID);
      listener.handle(serialize(event));

      verify(metricsService).recordOrderCancelled(AgentFixtures.AGENT_ID);
      verify(inboxService).markProcessed(event.serviceReference());
    }
  }

  @Nested
  @DisplayName("matches")
  class Matches {

    @Test
    @DisplayName("should return false for an unrelated routing key")
    void shouldReturnFalseForUnrelatedRoutingKey() {
      assertThat(listener.matches("orders.order.pending-assignment")).isFalse();
    }

    @Test
    @DisplayName("should return true for the order cancelled routing key")
    void shouldReturnTrueForOrderCancelledRoutingKey() {
      assertThat(listener.matches(ExternalContracts.ORDERS_ORDER_CANCELLED)).isTrue();
    }
  }
}
