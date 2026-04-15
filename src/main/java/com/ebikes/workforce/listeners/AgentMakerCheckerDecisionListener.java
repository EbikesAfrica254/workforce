package com.ebikes.workforce.listeners;

import org.springframework.stereotype.Component;

import com.ebikes.workforce.constants.EventConstants.ExternalContracts;
import com.ebikes.workforce.dtos.events.incoming.MakerCheckerDecision;
import com.ebikes.workforce.services.agents.AgentApprovalHandler;
import com.ebikes.workforce.services.events.InboxService;

import tools.jackson.databind.ObjectMapper;

@Component
public class AgentMakerCheckerDecisionListener extends MakerCheckerDecisionHandler {

  private final AgentApprovalHandler agentApprovalHandler;

  public AgentMakerCheckerDecisionListener(
      AgentApprovalHandler agentApprovalHandler,
      InboxService inboxService,
      ObjectMapper objectMapper) {
    super(inboxService, objectMapper);
    this.agentApprovalHandler = agentApprovalHandler;
  }

  @Override
  protected void dispatch(MakerCheckerDecision event) {
    agentApprovalHandler.handleDecision(event);
  }

  @Override
  public boolean matches(String routingKey) {
    return routingKey.startsWith(ExternalContracts.MAKER_CHECKER_WORKFORCE_PREFIX);
  }
}
