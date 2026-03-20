package com.ebikes.workforce.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.dtos.responses.agents.AgentDetailResponse;
import com.ebikes.workforce.dtos.responses.agents.AgentSummaryResponse;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AgentMapper {

  AgentDetailResponse toDetailResponse(Agent agent);

  AgentSummaryResponse toSummaryResponse(Agent agent);
}
