package com.ebikes.workforce.dtos.requests.filters;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import com.ebikes.workforce.dtos.requests.filters.bases.BaseFilter;
import com.ebikes.workforce.enums.LocationSource;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class LocationHistoryFilter extends BaseFilter {

  @NotNull private UUID agentId;
  private OffsetDateTime createdAtFrom;
  private OffsetDateTime createdAtTo;
  private LocationSource source;
}
