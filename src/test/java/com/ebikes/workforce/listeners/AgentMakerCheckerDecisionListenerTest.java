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
import com.ebikes.workforce.dtos.events.incoming.MakerCheckerDecision;
import com.ebikes.workforce.services.agents.AgentApprovalHandler;
import com.ebikes.workforce.services.events.InboxService;
import com.ebikes.workforce.support.fixtures.AgentFixtures;
import com.ebikes.workforce.support.fixtures.MakerCheckerFixtures;
import com.ebikes.workforce.support.infrastructure.WithExecutionContext;

import tools.jackson.databind.ObjectMapper;

@DisplayName("AgentMakerCheckerDecisionListener")
@ExtendWith({MockitoExtension.class, WithExecutionContext.class})
class AgentMakerCheckerDecisionListenerTest {

  @Mock private AgentApprovalHandler agentApprovalHandler;
  @Mock private InboxService inboxService;
  @Spy private ObjectMapper objectMapper;

  @InjectMocks private AgentMakerCheckerDecisionListener handler;

  private byte[] serialize(MakerCheckerDecision decision) {
    return objectMapper.writeValueAsBytes(decision);
  }

  @Nested
  @DisplayName("handle")
  class Handle {

    @Test
    @DisplayName("should skip dispatch when inbox receives a duplicate event")
    void shouldSkipDispatchOnDuplicateEvent() {
      MakerCheckerDecision decision =
          MakerCheckerFixtures.approved(AgentFixtures.AGENT_ID, "AGENT", "CREATE");
      when(inboxService.receive(any(), any(), any())).thenReturn(false);

      handler.handle(serialize(decision));

      verify(agentApprovalHandler, never()).handleDecision(any());
      verify(inboxService, never()).markProcessed(any());
    }

    @Test
    @DisplayName("should dispatch and mark processed when event is new")
    void shouldDispatchAndMarkProcessedOnNewEvent() {
      MakerCheckerDecision decision =
          MakerCheckerFixtures.approved(AgentFixtures.AGENT_ID, "AGENT", "CREATE");
      when(inboxService.receive(any(), any(), any())).thenReturn(true);

      handler.handle(serialize(decision));

      verify(agentApprovalHandler).handleDecision(decision);
      verify(inboxService).markProcessed(decision.serviceReference());
    }
  }

  @Nested
  @DisplayName("matches")
  class Matches {

    @Test
    @DisplayName("should return false for a document maker-checker routing key")
    void shouldReturnFalseForDocumentRoutingKey() {
      assertThat(handler.matches(ExternalContracts.MAKER_CHECKER_DOCUMENT_PREFIX + "replacement"))
          .isFalse();
    }

    @Test
    @DisplayName("should return false for an unrelated routing key")
    void shouldReturnFalseForUnrelatedRoutingKey() {
      assertThat(handler.matches("orders.order.pending-assignment")).isFalse();
    }

    @Test
    @DisplayName("should return true for a workforce maker-checker routing key")
    void shouldReturnTrueForWorkforceRoutingKey() {
      assertThat(handler.matches(ExternalContracts.MAKER_CHECKER_WORKFORCE_PREFIX + "agent"))
          .isTrue();
    }
  }
}
