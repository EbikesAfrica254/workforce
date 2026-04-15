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
import com.ebikes.workforce.dtos.events.incoming.OrderDeliveredEvent;
import com.ebikes.workforce.services.agents.metrics.MetricsService;
import com.ebikes.workforce.services.events.InboxService;
import com.ebikes.workforce.support.fixtures.AgentFixtures;

import tools.jackson.databind.ObjectMapper;

@DisplayName("OrderDeliveredEventListener")
@ExtendWith(MockitoExtension.class)
class OrderDeliveredEventListenerTest {

  private static final UUID ORDER_ID = UUID.randomUUID();
  private static final String SERVICE_REFERENCE = UUID.randomUUID().toString();

  @Mock private InboxService inboxService;
  @Mock private MetricsService metricsService;
  @Spy private ObjectMapper objectMapper;

  @InjectMocks private OrderDeliveredEventListener listener;

  private byte[] serialize(OrderDeliveredEvent event) {
    return objectMapper.writeValueAsBytes(event);
  }

  private OrderDeliveredEvent event(boolean onTime) {
    return new OrderDeliveredEvent(
        AgentFixtures.AGENT_ID.toString(), onTime, ORDER_ID, SERVICE_REFERENCE);
  }

  @Nested
  @DisplayName("handle")
  class Handle {

    @Test
    @DisplayName("should skip all processing when event is a duplicate")
    void shouldSkipAllProcessingOnDuplicateEvent() {
      when(inboxService.receive(any(), any(), any())).thenReturn(false);

      listener.handle(serialize(event(true)));

      verify(metricsService, never()).recordDeliveryCompleted(any(), any(Boolean.class));
      verify(inboxService, never()).markProcessed(any());
    }

    @Test
    @DisplayName("should call recordDeliveryCompleted with onTime=true when event is new")
    void shouldRecordDeliveryCompletedOnTime() {
      when(inboxService.receive(any(), any(), any())).thenReturn(true);

      listener.handle(serialize(event(true)));

      verify(metricsService).recordDeliveryCompleted(AgentFixtures.AGENT_ID, true);
    }

    @Test
    @DisplayName("should call recordDeliveryCompleted with onTime=false when event is new")
    void shouldRecordDeliveryCompletedLate() {
      when(inboxService.receive(any(), any(), any())).thenReturn(true);

      listener.handle(serialize(event(false)));

      verify(metricsService).recordDeliveryCompleted(AgentFixtures.AGENT_ID, false);
    }

    @Test
    @DisplayName("should mark inbox as processed when event is new")
    void shouldMarkProcessedOnNewEvent() {
      when(inboxService.receive(any(), any(), any())).thenReturn(true);

      listener.handle(serialize(event(true)));

      verify(inboxService).markProcessed(SERVICE_REFERENCE);
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
    @DisplayName("should return true for the order delivered routing key")
    void shouldReturnTrueForOrderDeliveredRoutingKey() {
      assertThat(listener.matches(ExternalContracts.ORDERS_ORDER_DELIVERED)).isTrue();
    }
  }
}
