package com.ebikes.workforce.listeners;

import org.springframework.stereotype.Component;

import com.ebikes.workforce.constants.EventConstants.ExternalContracts;
import com.ebikes.workforce.dtos.events.incoming.MakerCheckerDecision;
import com.ebikes.workforce.services.agents.document.DocumentApprovalHandler;
import com.ebikes.workforce.services.events.InboxService;

import tools.jackson.databind.ObjectMapper;

@Component
public class DocumentMakerCheckerDecisionListener extends MakerCheckerDecisionHandler {

  private final DocumentApprovalHandler documentApprovalHandler;

  public DocumentMakerCheckerDecisionListener(
      DocumentApprovalHandler documentApprovalHandler,
      InboxService inboxService,
      ObjectMapper objectMapper) {
    super(inboxService, objectMapper);
    this.documentApprovalHandler = documentApprovalHandler;
  }

  @Override
  protected void dispatch(MakerCheckerDecision event) {
    documentApprovalHandler.handleDecision(event);
  }

  @Override
  public boolean matches(String routingKey) {
    return routingKey.startsWith(ExternalContracts.MAKER_CHECKER_DOCUMENT_PREFIX);
  }
}
