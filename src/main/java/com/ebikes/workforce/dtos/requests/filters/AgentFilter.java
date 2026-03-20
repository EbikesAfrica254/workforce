package com.ebikes.workforce.dtos.requests.filters;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.ebikes.workforce.dtos.requests.filters.bases.BaseFilter;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.CapabilityClass;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class AgentFilter extends BaseFilter {

  private AvailabilityStatus availabilityStatus;
  private CapabilityClass capabilityClass;
  private OffsetDateTime createdAtFrom;
  private OffsetDateTime createdAtTo;
  private String firstName;
  private Boolean hasActiveSuspension;
  private String lastName;
  private String nationalIdNumber;
  private String phoneNumber;
  private BigDecimal reliabilityScoreMax;
  private BigDecimal reliabilityScoreMin;
}
