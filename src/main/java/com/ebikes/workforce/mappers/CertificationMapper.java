package com.ebikes.workforce.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.ebikes.workforce.database.entities.Certification;
import com.ebikes.workforce.dtos.responses.certifications.CertificationDetailResponse;
import com.ebikes.workforce.dtos.responses.certifications.CertificationSummaryResponse;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CertificationMapper {

  @Mapping(
      target = "isExpired",
      expression =
          "java(certification.getExpiresAt() != null &&"
              + " certification.getExpiresAt().isBefore(java.time.LocalDate.now()))")
  CertificationDetailResponse toDetailResponse(Certification certification);

  @Mapping(
      target = "isExpired",
      expression =
          "java(certification.getExpiresAt() != null &&"
              + " certification.getExpiresAt().isBefore(java.time.LocalDate.now()))")
  CertificationSummaryResponse toSummaryResponse(Certification certification);
}
