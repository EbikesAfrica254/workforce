package com.ebikes.workforce.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.ebikes.workforce.database.entities.AvailabilityLog;
import com.ebikes.workforce.dtos.responses.agents.AvailabilityLogResponse;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AvailabilityLogMapper {

  AvailabilityLogResponse toResponse(AvailabilityLog availabilityLog);
}
