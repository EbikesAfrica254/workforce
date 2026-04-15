package com.ebikes.workforce.dtos.requests.filters;

import java.time.OffsetDateTime;

import com.ebikes.workforce.dtos.requests.filters.bases.BaseFilter;
import com.ebikes.workforce.enums.OutboxStatus;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class OutboxFilter extends BaseFilter {
  private OffsetDateTime createdAtFrom;
  private OffsetDateTime createdAtTo;
  private String eventType;
  private Integer maxRetryCount;
  private Integer minRetryCount;
  private OutboxStatus status;
  private OffsetDateTime updatedAtFrom;
  private OffsetDateTime updatedAtTo;
}
