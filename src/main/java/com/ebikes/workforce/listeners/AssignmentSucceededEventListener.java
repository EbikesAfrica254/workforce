package com.ebikes.workforce.listeners;

import org.springframework.stereotype.Component;

import com.ebikes.workforce.constants.EventConstants.RoutingKeys;
import com.ebikes.workforce.dtos.events.incoming.AssignmentSucceededEvent;
import com.ebikes.workforce.services.agents.AgentService;
import com.ebikes.workforce.services.events.InboxService;
import com.ebikes.workforce.support.context.EventContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssignmentSucceededEventListener implements IncomingEventHandler {

  private final AgentService agentService;
  private final InboxService inboxService;
  private final ObjectMapper objectMapper;

  @Override
  public void handle(byte[] payload) {
    AssignmentSucceededEvent event =
        objectMapper.readValue(payload, AssignmentSucceededEvent.class);
    log.info("Received AssignmentSucceededEvent: serviceReference={}", event.serviceReference());

    if (EventContext.absent()) {
      log.warn("No event context found, skipping event processing.");
      return;
    }

    if (!inboxService.receive(
        EventContext.getEventType(), event.serviceReference(), EventContext.getSourceService())) {
      return;
    }

    agentService.recordAssigned(event.winnerAgentId());
    inboxService.markProcessed(event.serviceReference());

    log.info(
        "Assignment recorded: orderId={}, winnerAgentId={}, serviceReference={}",
        event.orderId(),
        event.winnerAgentId(),
        event.serviceReference());
  }

  @Override
  public boolean matches(String routingKey) {
    return RoutingKeys.ASSIGNMENTS_ASSIGNMENT_SUCCEEDED.equals(routingKey);
  }
}
