package com.ebikes.workforce.listeners;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.workforce.constants.EventConstants.ExternalContracts;
import com.ebikes.workforce.dtos.events.incoming.OrderPendingAssignmentEvent;
import com.ebikes.workforce.dtos.events.incoming.OrderReassignmentRequestedEvent;
import com.ebikes.workforce.mappers.ShortlistRequestMapper;
import com.ebikes.workforce.services.agents.shortlist.ShortlistService;
import com.ebikes.workforce.services.events.InboxService;
import com.ebikes.workforce.support.context.EventContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShortlistRequestListener implements IncomingEventHandler {

  private final InboxService inboxService;
  private final ObjectMapper objectMapper;
  private final ShortlistRequestMapper shortlistRequestMapper;
  private final ShortlistService shortlistService;

  @Override
  @Transactional
  public void handle(byte[] payload) {
    String routingKey = EventContext.getRoutingKey();

    if (ExternalContracts.ORDERS_ORDER_PENDING_ASSIGNMENT.equals(routingKey)) {
      handlePendingAssignment(payload);
    } else {
      handleReassignmentRequested(payload);
    }
  }

  @Override
  public boolean matches(String routingKey) {
    return ExternalContracts.ORDERS_ORDER_PENDING_ASSIGNMENT.equals(routingKey)
        || ExternalContracts.ORDERS_ORDER_REASSIGNMENT_REQUESTED.equals(routingKey);
  }

  private void handlePendingAssignment(byte[] payload) {
    OrderPendingAssignmentEvent event =
        objectMapper.readValue(payload, OrderPendingAssignmentEvent.class);
    log.info("Received OrderPendingAssignmentEvent: serviceReference={}", event.serviceReference());

    if (!inboxService.receive(
        EventContext.getEventType(), event.serviceReference(), EventContext.getSourceService())) {
      return;
    }

    shortlistService.resolve(shortlistRequestMapper.toShortlistRequest(event));
    inboxService.markProcessed(event.serviceReference());

    log.info(
        "OrderPendingAssignment processed: orderId={}, serviceReference={}",
        event.orderId(),
        event.serviceReference());
  }

  private void handleReassignmentRequested(byte[] payload) {
    OrderReassignmentRequestedEvent event =
        objectMapper.readValue(payload, OrderReassignmentRequestedEvent.class);
    log.info(
        "Received OrderReassignmentRequestedEvent: serviceReference={}", event.serviceReference());

    if (!inboxService.receive(
        EventContext.getEventType(), event.serviceReference(), EventContext.getSourceService())) {
      return;
    }

    shortlistService.resolve(shortlistRequestMapper.toShortlistRequest(event));
    inboxService.markProcessed(event.serviceReference());

    log.info(
        "OrderReassignmentRequested processed: orderId={}, serviceReference={}",
        event.orderId(),
        event.serviceReference());
  }
}
