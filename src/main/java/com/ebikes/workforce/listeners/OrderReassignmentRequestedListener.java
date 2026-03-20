package com.ebikes.workforce.listeners;

import org.springframework.stereotype.Component;

import com.ebikes.workforce.constants.EventConstants.RoutingKeys;
import com.ebikes.workforce.dtos.events.incoming.OrderReassignmentRequestedEvent;
import com.ebikes.workforce.dtos.internal.ShortlistRequest;
import com.ebikes.workforce.services.agents.ShortlistService;
import com.ebikes.workforce.services.events.InboxService;
import com.ebikes.workforce.support.context.EventContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderReassignmentRequestedListener implements IncomingEventHandler {

  private final InboxService inboxService;
  private final ObjectMapper objectMapper;
  private final ShortlistService shortlistService;

  @Override
  public void handle(byte[] payload) {
    OrderReassignmentRequestedEvent event =
        objectMapper.readValue(payload, OrderReassignmentRequestedEvent.class);
    log.info(
        "Received OrderReassignmentRequestedEvent: serviceReference={}", event.serviceReference());

    if (EventContext.absent()) {
      log.warn("No event context found, skipping event processing.");
      return;
    }

    if (!inboxService.receive(
        EventContext.getEventType(), event.serviceReference(), EventContext.getSourceService())) {
      return;
    }

    ShortlistRequest request =
        new ShortlistRequest(
            event.orderId(),
            event.organizationId(),
            event.branchId(),
            event.pickupLatitude(),
            event.pickupLongitude(),
            event.vehicleClass());

    shortlistService.resolve(request);

    inboxService.markProcessed(event.serviceReference());

    log.info(
        "OrderReassignmentRequested processed: orderId={}, serviceReference={}",
        event.orderId(),
        event.serviceReference());
  }

  @Override
  public boolean matches(String routingKey) {
    return RoutingKeys.ORDERS_ORDER_REASSIGNMENT_REQUESTED.equals(routingKey);
  }
}
