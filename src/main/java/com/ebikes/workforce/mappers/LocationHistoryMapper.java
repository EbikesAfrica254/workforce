package com.ebikes.workforce.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.ebikes.workforce.database.entities.LocationHistory;
import com.ebikes.workforce.dtos.responses.agents.LocationHistoryResponse;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface LocationHistoryMapper {

  LocationHistoryResponse toResponse(LocationHistory locationHistory);
}
