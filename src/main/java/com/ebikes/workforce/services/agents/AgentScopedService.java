package com.ebikes.workforce.services.agents;

import java.util.UUID;

import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.enums.ResponseCode;
import com.ebikes.workforce.exceptions.ResourceNotFoundException;

public abstract class AgentScopedService {

  protected final AgentRepository agentRepository;

  protected AgentScopedService(AgentRepository agentRepository) {
    this.agentRepository = agentRepository;
  }

  protected final Agent requireAgentById(UUID agentId) {
    return agentRepository
        .findById(agentId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND, "Agent with ID " + agentId + " not found"));
  }
}
