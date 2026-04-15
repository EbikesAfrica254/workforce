package com.ebikes.workforce.listeners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.workforce.constants.EventConstants.ExternalContracts;
import com.ebikes.workforce.dtos.events.incoming.OrderPendingAssignmentEvent;
import com.ebikes.workforce.dtos.events.incoming.OrderReassignmentRequestedEvent;
import com.ebikes.workforce.dtos.internal.ShortlistRequest;
import com.ebikes.workforce.mappers.ShortlistRequestMapper;
import com.ebikes.workforce.services.agents.shortlist.ShortlistService;
import com.ebikes.workforce.services.events.InboxService;
import com.ebikes.workforce.support.context.EventContext;
import com.ebikes.workforce.support.fixtures.EventFixtures;

import tools.jackson.databind.ObjectMapper;

@DisplayName("ShortlistRequestListener")
@ExtendWith(MockitoExtension.class)
class ShortlistRequestListenerTest {

  @Mock private InboxService inboxService;
  @Mock private ShortlistRequestMapper shortlistRequestMapper;
  @Mock private ShortlistService shortlistService;
  @Spy private ObjectMapper objectMapper;

  @InjectMocks private ShortlistRequestListener listener;

  @AfterEach
  void clearEventContext() {
    EventContext.clear();
  }

  @Nested
  @DisplayName("handle — OrderPendingAssignment")
  class HandlePendingAssignment {

    @BeforeEach
    void setRoutingKey() {
      EventContext.set(
          null,
          ExternalContracts.ORDERS_ORDER_PENDING_ASSIGNMENT,
          ExternalContracts.ORDERS_ORDER_PENDING_ASSIGNMENT,
          "orders");
    }

    @Test
    @DisplayName("should skip all processing when event is a duplicate")
    void shouldSkipAllProcessingOnDuplicateEvent() {
      OrderPendingAssignmentEvent event = EventFixtures.orderPendingAssignment();
      when(inboxService.receive(any(), any(), any())).thenReturn(false);

      listener.handle(objectMapper.writeValueAsBytes(event));

      verify(shortlistService, never()).resolve(any());
      verify(inboxService, never()).markProcessed(any());
    }

    @Test
    @DisplayName("should resolve shortlist and mark processed when event is new")
    void shouldResolveAndMarkProcessedOnNewEvent() {
      OrderPendingAssignmentEvent event = EventFixtures.orderPendingAssignment();
      ShortlistRequest request = EventFixtures.shortlistRequest();
      when(inboxService.receive(any(), any(), any())).thenReturn(true);
      when(shortlistRequestMapper.toShortlistRequest(any(OrderPendingAssignmentEvent.class)))
          .thenReturn(request);

      listener.handle(objectMapper.writeValueAsBytes(event));

      verify(shortlistService).resolve(request);
      verify(inboxService).markProcessed(event.serviceReference());
    }
  }

  @Nested
  @DisplayName("handle — OrderReassignmentRequested")
  class HandleReassignmentRequested {

    @BeforeEach
    void setRoutingKey() {
      EventContext.set(
          null,
          ExternalContracts.ORDERS_ORDER_REASSIGNMENT_REQUESTED,
          ExternalContracts.ORDERS_ORDER_REASSIGNMENT_REQUESTED,
          "orders");
    }

    @Test
    @DisplayName("should skip all processing when event is a duplicate")
    void shouldSkipAllProcessingOnDuplicateEvent() {
      OrderReassignmentRequestedEvent event = EventFixtures.orderReassignmentRequested();
      when(inboxService.receive(any(), any(), any())).thenReturn(false);

      listener.handle(objectMapper.writeValueAsBytes(event));

      verify(shortlistService, never()).resolve(any());
      verify(inboxService, never()).markProcessed(any());
    }

    @Test
    @DisplayName("should resolve shortlist and mark processed when event is new")
    void shouldResolveAndMarkProcessedOnNewEvent() {
      OrderReassignmentRequestedEvent event = EventFixtures.orderReassignmentRequested();
      ShortlistRequest request = EventFixtures.shortlistRequest();
      when(inboxService.receive(any(), any(), any())).thenReturn(true);
      when(shortlistRequestMapper.toShortlistRequest(any(OrderReassignmentRequestedEvent.class)))
          .thenReturn(request);

      listener.handle(objectMapper.writeValueAsBytes(event));

      verify(shortlistService).resolve(request);
      verify(inboxService).markProcessed(event.serviceReference());
    }
  }

  @Nested
  @DisplayName("matches")
  class Matches {

    @Test
    @DisplayName("should return false for an unrelated routing key")
    void shouldReturnFalseForUnrelatedRoutingKey() {
      assertThat(listener.matches("orders.order.cancelled")).isFalse();
    }

    @Test
    @DisplayName("should return true for order pending assignment routing key")
    void shouldReturnTrueForOrderPendingAssignment() {
      assertThat(listener.matches(ExternalContracts.ORDERS_ORDER_PENDING_ASSIGNMENT)).isTrue();
    }

    @Test
    @DisplayName("should return true for order reassignment requested routing key")
    void shouldReturnTrueForOrderReassignmentRequested() {
      assertThat(listener.matches(ExternalContracts.ORDERS_ORDER_REASSIGNMENT_REQUESTED)).isTrue();
    }
  }
}
