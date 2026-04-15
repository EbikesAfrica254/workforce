package com.ebikes.workforce.listeners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.workforce.constants.EventConstants.ExternalContracts;
import com.ebikes.workforce.dtos.events.incoming.AssignmentCompletedEvent;
import com.ebikes.workforce.services.agents.metrics.MetricsService;
import com.ebikes.workforce.services.events.InboxService;
import com.ebikes.workforce.support.fixtures.AgentFixtures;
import com.ebikes.workforce.support.fixtures.EventFixtures;

import tools.jackson.databind.ObjectMapper;

@DisplayName("AssignmentCompletedEventListener")
@ExtendWith(MockitoExtension.class)
class AssignmentCompletedEventListenerTest {

  @Mock private InboxService inboxService;
  @Mock private MetricsService metricsService;
  @Spy private ObjectMapper objectMapper;

  @InjectMocks private AssignmentCompletedEventListener listener;

  private byte[] serialize(AssignmentCompletedEvent event) {
    return objectMapper.writeValueAsBytes(event);
  }

  @Nested
  @DisplayName("handle")
  class Handle {

    @Test
    @DisplayName("should not call metricsService when event is a duplicate")
    void shouldSkipMetricsOnDuplicateEvent() {
      when(inboxService.receive(any(), any(), any())).thenReturn(false);

      listener.handle(serialize(EventFixtures.assignmentCompleted()));

      verify(metricsService, never()).recordAssigned(any());
      verify(inboxService, never()).markProcessed(any());
    }

    @Test
    @DisplayName("should call recordAssigned with the winner agent ID when event is new")
    void shouldRecordAssignedWithCorrectAgentId() {
      when(inboxService.receive(any(), any(), any())).thenReturn(true);

      listener.handle(serialize(EventFixtures.assignmentCompleted()));

      verify(metricsService).recordAssigned(AgentFixtures.AGENT_ID);
    }

    @Test
    @DisplayName("should mark inbox as processed when event is new")
    void shouldMarkProcessedOnNewEvent() {
      when(inboxService.receive(any(), any(), any())).thenReturn(true);

      AssignmentCompletedEvent event = EventFixtures.assignmentCompleted();
      listener.handle(serialize(event));

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
    @DisplayName("should return true for the assignments completed routing key")
    void shouldReturnTrueForAssignmentCompletedRoutingKey() {
      assertThat(listener.matches(ExternalContracts.ASSIGNMENTS_ASSIGNMENT_COMPLETED)).isTrue();
    }
  }
}
