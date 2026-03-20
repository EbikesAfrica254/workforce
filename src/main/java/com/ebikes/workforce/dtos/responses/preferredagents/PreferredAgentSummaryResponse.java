package com.ebikes.workforce.dtos.responses.preferredagents;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PreferredAgentSummaryResponse(
    UUID agentId, String branchId, UUID id, String organizationId, Integer priority) {}
