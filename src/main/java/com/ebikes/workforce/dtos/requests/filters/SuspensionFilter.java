package com.ebikes.workforce.dtos.requests.filters;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.workforce.dtos.requests.filters.bases.BaseFilter;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SuspensionFilter extends BaseFilter {

  private UUID agentId;
  private OffsetDateTime createdAtFrom;
  private OffsetDateTime createdAtTo;
  private OffsetDateTime expiresAtFrom;
  private OffsetDateTime expiresAtTo;
  private Boolean isActive;
}
