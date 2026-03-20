package com.ebikes.workforce.listeners;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.workforce.dtos.events.incoming.MakerCheckerDecision;
import com.ebikes.workforce.services.documents.DocumentService;
import com.ebikes.workforce.services.events.InboxService;
import com.ebikes.workforce.support.context.EventContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentReplacementMakerCheckerDecisionHandler implements IncomingEventHandler {

  private final DocumentService documentService;
  private final InboxService inboxService;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public void handle(byte[] payload) throws IOException {
    MakerCheckerDecision event = objectMapper.readValue(payload, MakerCheckerDecision.class);
    log.debug(
        "Received document replacement maker-checker decision: entityId={}, outcome={}",
        event.entityId(),
        event.outcome());

    if (EventContext.absent()) {
      log.warn("No event context found, skipping event processing.");
      return;
    }

    if (!inboxService.receive(
        EventContext.getEventType(), event.serviceReference(), EventContext.getSourceService())) {
      return;
    }

    try {
      documentService.handleReplacementDecision(event);
      inboxService.markProcessed(event.serviceReference());
    } catch (Exception e) {
      log.error(
          "Failed to process document replacement maker-checker decision: serviceReference={}",
          event.serviceReference(),
          e);
    }
  }

  @Override
  public boolean matches(String routingKey) {
    return routingKey.startsWith("maker-checker.document.");
  }
}
