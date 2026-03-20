package com.ebikes.workforce.dtos.responses.preferredagents;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PreferredAgentDetailResponse(
    UUID agentId,
    String branchId,
    OffsetDateTime createdAt,
    String createdBy,
    UUID id,
    String notes,
    String organizationId,
    Integer priority,
    OffsetDateTime updatedAt,
    String updatedBy,
    Long version) {}
