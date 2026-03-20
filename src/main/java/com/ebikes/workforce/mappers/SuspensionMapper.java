package com.ebikes.workforce.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.ebikes.workforce.database.entities.Suspension;
import com.ebikes.workforce.dtos.responses.suspensions.SuspensionDetailResponse;
import com.ebikes.workforce.dtos.responses.suspensions.SuspensionSummaryResponse;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SuspensionMapper {

  @Mapping(target = "isActive", expression = "java(suspension.getLiftedAt() == null)")
  SuspensionDetailResponse toDetailResponse(Suspension suspension);

  @Mapping(target = "isActive", expression = "java(suspension.getLiftedAt() == null)")
  SuspensionSummaryResponse toSummaryResponse(Suspension suspension);
}
