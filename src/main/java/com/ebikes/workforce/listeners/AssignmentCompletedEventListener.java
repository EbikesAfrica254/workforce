package com.ebikes.workforce.listeners;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.workforce.constants.EventConstants.ExternalContracts;
import com.ebikes.workforce.dtos.events.incoming.AssignmentCompletedEvent;
import com.ebikes.workforce.services.agents.metrics.MetricsService;
import com.ebikes.workforce.services.events.InboxService;
import com.ebikes.workforce.support.context.EventContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssignmentCompletedEventListener implements IncomingEventHandler {

  private final InboxService inboxService;
  private final MetricsService metricsService;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public void handle(byte[] payload) {
    AssignmentCompletedEvent event =
        objectMapper.readValue(payload, AssignmentCompletedEvent.class);
    log.info("Received AssignmentCompletedEvent: serviceReference={}", event.serviceReference());

    if (!inboxService.receive(
        EventContext.getEventType(), event.serviceReference(), EventContext.getSourceService())) {
      return;
    }

    metricsService.recordAssigned(event.winnerAgentId());
    inboxService.markProcessed(event.serviceReference());

    log.info(
        "Assignment recorded: orderId={}, winnerAgentId={}, serviceReference={}",
        event.orderId(),
        event.winnerAgentId(),
        event.serviceReference());
  }

  @Override
  public boolean matches(String routingKey) {
    return ExternalContracts.ASSIGNMENTS_ASSIGNMENT_COMPLETED.equals(routingKey);
  }
}
