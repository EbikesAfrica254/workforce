package com.ebikes.workforce.dtos.requests.filters;

import java.util.UUID;

import com.ebikes.workforce.dtos.requests.filters.bases.BaseFilter;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class PreferredAgentFilter extends BaseFilter {
  private UUID agentId;
  private String branchId;
  private String organizationId;
}
