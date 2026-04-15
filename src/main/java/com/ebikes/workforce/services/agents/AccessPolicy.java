package com.ebikes.workforce.services.agents;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.enums.ResponseCode;
import com.ebikes.workforce.enums.UserRole;
import com.ebikes.workforce.exceptions.AuthorizationException;
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.support.context.ExecutionContext;
import com.ebikes.workforce.support.security.RBACUtilities;

@Component
public class AccessPolicy {

  private static final UserRole[] PRIVILEGED_CREATE_ROLES = {
    UserRole.BRANCH_ADMIN,
    UserRole.BRANCH_FLEET_MANAGER,
    UserRole.BRANCH_FLEET_SUPPORT,
    UserRole.ORGANIZATION_ADMIN,
    UserRole.ORGANIZATION_FLEET_MANAGER,
    UserRole.ORGANIZATION_FLEET_SUPPORT,
    UserRole.SYSTEM_ADMIN
  };

  public void canCreate(String requestedUserId) {
    switch (ExecutionContext.get()) {
      case ExecutionContext.SystemContext ignored -> {}
      case ExecutionContext.UserContext userContext -> {
        Set<UserRole> roles = RBACUtilities.parseRoles(userContext.roles());

        if (RBACUtilities.hasAnyRole(roles, PRIVILEGED_CREATE_ROLES)) {
          return;
        }

        if (!userContext.userId().equals(requestedUserId)) {
          throw new AuthorizationException(
              ResponseCode.FORBIDDEN,
              "Access denied: authenticated user cannot create an agent for another user");
        }
      }
    }
  }

  public void owns(Agent agent) {
    if (ExecutionContext.get() instanceof ExecutionContext.UserContext userContext) {
      Set<UserRole> roles = RBACUtilities.parseRoles(userContext.roles());

      if (RBACUtilities.hasAnyRole(roles, UserRole.AGENT)
          && !agent.getUserId().equals(userContext.userId())) {
        throw new BusinessRuleException(
            ResponseCode.FORBIDDEN, "Access denied: agent does not own this resource");
      }
    }
  }
}
