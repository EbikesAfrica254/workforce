package com.ebikes.workforce.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.ebikes.workforce.database.entities.PreferredAgent;
import com.ebikes.workforce.dtos.responses.preferredagents.PreferredAgentDetailResponse;
import com.ebikes.workforce.dtos.responses.preferredagents.PreferredAgentSummaryResponse;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PreferredAgentMapper {

  PreferredAgentDetailResponse toDetailResponse(PreferredAgent preferredAgent);

  PreferredAgentSummaryResponse toSummaryResponse(PreferredAgent preferredAgent);
}
