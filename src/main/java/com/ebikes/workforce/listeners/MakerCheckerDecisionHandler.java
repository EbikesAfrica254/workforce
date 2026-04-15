package com.ebikes.workforce.listeners;

import org.springframework.transaction.annotation.Transactional;

import com.ebikes.workforce.dtos.events.incoming.MakerCheckerDecision;
import com.ebikes.workforce.services.events.InboxService;
import com.ebikes.workforce.support.context.EventContext;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
public abstract class MakerCheckerDecisionHandler implements IncomingEventHandler {

  private final InboxService inboxService;
  private final ObjectMapper objectMapper;

  protected MakerCheckerDecisionHandler(InboxService inboxService, ObjectMapper objectMapper) {
    this.inboxService = inboxService;
    this.objectMapper = objectMapper;
  }

  @Override
  @Transactional
  public void handle(byte[] payload) {
    MakerCheckerDecision event = objectMapper.readValue(payload, MakerCheckerDecision.class);

    log.debug(
        "Received maker-checker decision: handler={}, entityId={}, operation={}, outcome={}",
        getClass().getSimpleName(),
        event.entityId(),
        event.operation(),
        event.outcome());

    if (!inboxService.receive(
        EventContext.getEventType(), event.serviceReference(), EventContext.getSourceService())) {
      return;
    }

    dispatch(event);
    inboxService.markProcessed(event.serviceReference());
  }

  protected abstract void dispatch(MakerCheckerDecision event);
}
