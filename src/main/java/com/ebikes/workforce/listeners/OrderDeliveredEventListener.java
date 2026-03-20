package com.ebikes.workforce.listeners;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.ebikes.workforce.constants.EventConstants.RoutingKeys;
import com.ebikes.workforce.dtos.events.incoming.OrderDeliveredEvent;
import com.ebikes.workforce.services.agents.AgentService;
import com.ebikes.workforce.services.events.InboxService;
import com.ebikes.workforce.support.context.EventContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderDeliveredEventListener implements IncomingEventHandler {

  private final AgentService agentService;
  private final InboxService inboxService;
  private final ObjectMapper objectMapper;

  @Override
  public void handle(byte[] payload) {
    OrderDeliveredEvent event = objectMapper.readValue(payload, OrderDeliveredEvent.class);
    log.info("Received OrderDeliveredEvent: serviceReference={}", event.serviceReference());

    if (EventContext.absent()) {
      log.warn("No event context found, skipping event processing.");
      return;
    }

    if (!inboxService.receive(
        EventContext.getEventType(), event.serviceReference(), EventContext.getSourceService())) {
      return;
    }

    agentService.recordDeliveryCompleted(UUID.fromString(event.agentId()), event.onTime());

    inboxService.markProcessed(event.serviceReference());

    log.info(
        "Order delivered processed: orderId={}, agentId={}, onTime={}, serviceReference={}",
        event.orderId(),
        event.agentId(),
        event.onTime(),
        event.serviceReference());
  }

  @Override
  public boolean matches(String routingKey) {
    return RoutingKeys.ORDERS_ORDER_DELIVERED.equals(routingKey);
  }
}
