package com.ebikes.workforce.support.fixtures;

import java.util.UUID;

import com.ebikes.workforce.database.entities.PreferredAgent;

public final class PreferredAgentFixtures {

  public static final UUID PREFERRED_AGENT_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000050");

  private PreferredAgentFixtures() {}

  public static PreferredAgent branchScoped(UUID agentId, String organizationId, String branchId) {
    return base(agentId, organizationId).branchId(branchId).build();
  }

  public static PreferredAgent orgScoped(UUID agentId, String organizationId) {
    return base(agentId, organizationId).build();
  }

  public static PreferredAgent withId(UUID agentId, String organizationId, UUID preferredAgentId) {
    return base(agentId, organizationId).id(preferredAgentId).build();
  }

  public static PreferredAgent withPriority(UUID agentId, String organizationId, int priority) {
    return base(agentId, organizationId).priority(priority).build();
  }

  private static PreferredAgent.PreferredAgentBuilder<?, ?> base(
      UUID agentId, String organizationId) {
    return PreferredAgent.builder()
        .id(PREFERRED_AGENT_ID)
        .agentId(agentId)
        .organizationId(organizationId)
        .priority(1)
        .notes("Preferred for reliability");
  }
}
