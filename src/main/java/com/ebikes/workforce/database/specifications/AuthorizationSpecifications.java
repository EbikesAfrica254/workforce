package com.ebikes.workforce.database.specifications;

import java.util.Set;

import org.springframework.data.jpa.domain.Specification;

import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.PreferredAgent;
import com.ebikes.workforce.enums.ResponseCode;
import com.ebikes.workforce.exceptions.AuthorizationException;
import com.ebikes.workforce.support.context.ExecutionContext;
import com.ebikes.workforce.support.security.RBACUtilities;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class AuthorizationSpecifications {

  private AuthorizationSpecifications() {
    // prevent instantiaton
  }

  public static Specification<Agent> forAgents() {
    Set<String> roles = ExecutionContext.getRoles();

    if (RBACUtilities.hasSystemAdminRole(RBACUtilities.parseRoles(roles))) {
      log.debug("SYSTEM_ADMIN access: no filter applied for agents");
      return noFilter();
    }

    String userId = ExecutionContext.getUserId();
    log.debug("Filtering agents by userId={}", userId);

    return filterAgentByUserId(userId);
  }

  public static Specification<PreferredAgent> forPreferredAgents() {
    Set<String> roles = ExecutionContext.getRoles();

    if (RBACUtilities.hasSystemAdminRole(RBACUtilities.parseRoles(roles))) {
      log.debug("SYSTEM_ADMIN access: no filter applied for preferred agents");
      return noFilter();
    }

    String activeOrganization = validateActiveOrganization();
    String activeBranch = ExecutionContext.getActiveBranch();

    if (activeBranch != null && !activeBranch.isBlank()) {
      log.debug(
          "Filtering preferred agents by organizationId={}, branchId={}",
          activeOrganization,
          activeBranch);
      return filterPreferredAgentByOrganization(activeOrganization)
          .and(filterPreferredAgentByBranch(activeBranch));
    }

    log.debug("Filtering preferred agents by organizationId={}", activeOrganization);
    return filterPreferredAgentByOrganization(activeOrganization);
  }

  private static Specification<Agent> filterAgentByUserId(String userId) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("userId"), userId);
  }

  private static Specification<PreferredAgent> filterPreferredAgentByBranch(String branchId) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("branchId"), branchId);
  }

  private static Specification<PreferredAgent> filterPreferredAgentByOrganization(
      String organizationId) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get("organizationId"), organizationId);
  }

  private static <T> Specification<T> noFilter() {
    return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
  }

  private static String validateActiveOrganization() {
    String activeOrganization = ExecutionContext.getActiveOrganization();
    if (activeOrganization == null || activeOrganization.isBlank()) {
      log.error("Missing active_organization claim for {} access", "preferred agents");
      throw new AuthorizationException(
          ResponseCode.FORBIDDEN, "Active organization context required for this operation");
    }
    return activeOrganization;
  }
}
