package com.ebikes.workforce.listeners;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import com.ebikes.workforce.constants.ApplicationConstants.MessageHeaders;
import com.ebikes.workforce.constants.EventConstants.ExternalContracts;
import com.ebikes.workforce.support.context.EventContext;
import com.ebikes.workforce.support.infrastructure.AbstractListenerTest;

@DisplayName("IncomingEventListener")
class IncomingEventListenerTest extends AbstractListenerTest {

  @Mock private IncomingEventHandler handlerA;
  @Mock private IncomingEventHandler handlerB;

  private IncomingEventListener listenerWith(IncomingEventHandler... handlers) {
    return new IncomingEventListener(List.of(handlers));
  }

  private Message<byte[]> messageWith(String routingKey) {
    return MessageBuilder.withPayload("{}".getBytes())
        .setHeader(MessageHeaders.ROUTING_KEY, routingKey)
        .setHeader(MessageHeaders.EVENT_TYPE, "some.event.type")
        .setHeader(MessageHeaders.OUTBOX_ID, "outbox-router-001")
        .build();
  }

  private Message<byte[]> messageWithoutRoutingKey() {
    return MessageBuilder.withPayload("{}".getBytes())
        .setHeader(MessageHeaders.EVENT_TYPE, "some.event.type")
        .build();
  }

  @Nested
  @DisplayName("route")
  class Route {

    @Test
    @DisplayName("delegates to matching handler")
    void delegatesToMatchingHandler() {
      String routingKey = ExternalContracts.ORDERS_ORDER_DELIVERED;
      when(handlerA.matches(routingKey)).thenReturn(false);
      when(handlerB.matches(routingKey)).thenReturn(true);

      listenerWith(handlerA, handlerB).route(messageWith(routingKey));

      verify(handlerB).handle(any());
      verify(handlerA, never()).handle(any());
    }

    @Test
    @DisplayName("discards message when no handler matches")
    void discardsWhenNoHandlerMatches() {
      when(handlerA.matches(anyString())).thenReturn(false);
      when(handlerB.matches(anyString())).thenReturn(false);

      listenerWith(handlerA, handlerB).route(messageWith("unknown.routing.key"));

      verify(handlerA, never()).handle(any());
      verify(handlerB, never()).handle(any());
    }

    @Test
    @DisplayName("discards message when routing key header is absent")
    void discardsWhenRoutingKeyAbsent() {
      listenerWith(handlerA, handlerB).route(messageWithoutRoutingKey());

      verify(handlerA, never()).handle(any());
      verify(handlerB, never()).handle(any());
    }

    @Test
    @DisplayName("clears EventContext after routing regardless of handler outcome")
    void clearsEventContextAfterRouting() {
      when(handlerA.matches(anyString())).thenReturn(true);
      org.mockito.Mockito.doThrow(new RuntimeException("handler blew up"))
          .when(handlerA)
          .handle(any());

      try {
        listenerWith(handlerA).route(messageWith(ExternalContracts.ORDERS_ORDER_DELIVERED));
      } catch (RuntimeException ignored) {
        // EventContextDecorator re-throws from handler — context must still be cleared
      }

      assertThat(EventContext.absent()).isTrue();
    }

    @Test
    @DisplayName("swallows handler exception and clears context")
    void swallowsHandlerExceptionAndClearsContext() {
      when(handlerA.matches(anyString())).thenReturn(true);
      org.mockito.Mockito.doThrow(new RuntimeException("channel failure"))
          .when(handlerA)
          .handle(any());

      org.assertj.core.api.Assertions.assertThatNoException()
          .isThrownBy(
              () ->
                  listenerWith(handlerA)
                      .route(messageWith(ExternalContracts.ORDERS_ORDER_DELIVERED)));

      assertThat(EventContext.absent()).isTrue();
    }
  }
}
