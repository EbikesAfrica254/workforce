package com.ebikes.workforce.database.projections;

public interface AgentConflictCheck {
  boolean isPhoneExists();

  boolean isNationalIdExists();

  boolean isUserIdExists();
}
