package com.ebikes.workforce.support.fixtures;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.ebikes.workforce.database.entities.Suspension;

public final class SuspensionFixtures {

  public static final UUID SUSPENSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000040");

  private SuspensionFixtures() {}

  public static Suspension active(UUID agentId) {
    return base(agentId).build();
  }

  public static Suspension expiring(UUID agentId) {
    return base(agentId).expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(3)).build();
  }

  public static Suspension lifted(UUID agentId) {
    Suspension suspension = base(agentId).build();
    suspension.lift(SecurityFixtures.TEST_USER_ID);
    return suspension;
  }

  public static Suspension withId(UUID agentId, UUID suspensionId) {
    return base(agentId).id(suspensionId).build();
  }

  private static Suspension.SuspensionBuilder<?, ?> base(UUID agentId) {
    return Suspension.builder()
        .id(SUSPENSION_ID)
        .agentId(agentId)
        .reason("Policy violation")
        .notes("Suspended pending review");
  }
}
