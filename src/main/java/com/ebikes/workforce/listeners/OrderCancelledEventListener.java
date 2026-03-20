package com.ebikes.workforce.listeners;

import java.io.IOException;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.ebikes.workforce.constants.EventConstants.RoutingKeys;
import com.ebikes.workforce.dtos.events.incoming.OrderCancelledEvent;
import com.ebikes.workforce.services.agents.AgentService;
import com.ebikes.workforce.services.events.InboxService;
import com.ebikes.workforce.support.context.EventContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCancelledEventListener implements IncomingEventHandler {

  private static final Set<String> STATUSES_REQUIRING_DECREMENT =
      Set.of("ASSIGNED", "PENDING_REASSIGNMENT");

  private final AgentService agentService;
  private final InboxService inboxService;
  private final ObjectMapper objectMapper;

  @Override
  public void handle(byte[] payload) throws IOException {
    OrderCancelledEvent event = objectMapper.readValue(payload, OrderCancelledEvent.class);
    log.info("Received OrderCancelledEvent: serviceReference={}", event.serviceReference());

    if (EventContext.absent()) {
      log.warn("No event context found, skipping event processing.");
      return;
    }

    if (!inboxService.receive(
        EventContext.getEventType(), event.serviceReference(), EventContext.getSourceService())) {
      return;
    }

    if (event.agentId() != null
        && STATUSES_REQUIRING_DECREMENT.contains(event.cancelledFromStatus())) {
      agentService.recordOrderCancelled(event.agentId());
    }

    inboxService.markProcessed(event.serviceReference());

    log.info(
        "Order cancelled processed: orderId={}, cancelledFromStatus={}, serviceReference={}",
        event.orderId(),
        event.cancelledFromStatus(),
        event.serviceReference());
  }

  @Override
  public boolean matches(String routingKey) {
    return RoutingKeys.ORDERS_ORDER_CANCELLED.equals(routingKey);
  }
}
