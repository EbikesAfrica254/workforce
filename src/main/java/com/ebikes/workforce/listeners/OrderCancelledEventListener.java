package com.ebikes.workforce.listeners;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.workforce.constants.EventConstants.ExternalContracts;
import com.ebikes.workforce.dtos.events.incoming.OrderCancelledEvent;
import com.ebikes.workforce.services.agents.metrics.MetricsService;
import com.ebikes.workforce.services.events.InboxService;
import com.ebikes.workforce.support.context.EventContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCancelledEventListener implements IncomingEventHandler {

  private final InboxService inboxService;
  private final MetricsService metricsService;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public void handle(byte[] payload) {
    OrderCancelledEvent event = objectMapper.readValue(payload, OrderCancelledEvent.class);
    log.info("Received OrderCancelledEvent: serviceReference={}", event.serviceReference());

    if (!inboxService.receive(
        EventContext.getEventType(), event.serviceReference(), EventContext.getSourceService())) {
      return;
    }

    if (event.agentId() != null
        && ExternalContracts.ORDER_STATUSES_REQUIRING_AGENT_DECREMENT.contains(
            event.cancelledFromStatus())) {
      metricsService.recordOrderCancelled(event.agentId());
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
    return ExternalContracts.ORDERS_ORDER_CANCELLED.equals(routingKey);
  }
}
