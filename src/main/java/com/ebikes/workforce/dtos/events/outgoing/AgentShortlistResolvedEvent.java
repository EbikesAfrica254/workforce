package com.ebikes.workforce.dtos.events.outgoing;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.ebikes.workforce.enums.VehicleClass;

public record AgentShortlistResolvedEvent(
    String branchId,
    List<Candidate> candidates,
    OffsetDateTime expiresAt,
    UUID orderId,
    String organizationId,
    OffsetDateTime resolvedAt,
    String serviceReference)
    implements Serializable {

  public AgentShortlistResolvedEvent {
    candidates = candidates == null ? null : List.copyOf(candidates);
  }

  public record Candidate(
      String agentId,
      boolean isPreferred,
      BigDecimal latitude,
      BigDecimal longitude,
      VehicleClass vehicleClass) {}
}
