package com.ebikes.workforce.database.specifications;

import java.util.Set;

import org.springframework.data.jpa.domain.Specification;

import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.PreferredAgent;
import com.ebikes.workforce.enums.ResponseCode;
import com.ebikes.workforce.enums.UserRole;
import com.ebikes.workforce.exceptions.AuthorizationException;
import com.ebikes.workforce.support.context.ExecutionContext;
import com.ebikes.workforce.support.context.ExecutionContext.UserContext;
import com.ebikes.workforce.support.security.RBACUtilities;
import com.ebikes.workforce.support.security.RBACUtilities.RoleTier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class AuthorizationSpecifications {

  private AuthorizationSpecifications() {
    // prevent instantiation
  }

  public static Specification<Agent> forAgents() {
    Set<UserRole> roles = extractRoles();
    RoleTier tier = RBACUtilities.resolveRoleTier(roles);

    return switch (tier) {
      case SYSTEM -> {
        log.debug("SYSTEM_ADMIN access: no filter applied for agents");
        yield noFilter();
      }
      case AGENT -> {
        String userId = ExecutionContext.getUserId();
        log.debug("AGENT access: filtering agents by userId={}", userId);
        yield filterAgentByUserId(userId);
      }
      case ORGANIZATION, BRANCH -> {
        log.debug("{} access: no filter applied for agents (shared pool)", tier);
        yield noFilter();
      }
      case UNKNOWN ->
          throw new AuthorizationException(
              ResponseCode.FORBIDDEN, "Access denied: insufficient role for agent access");
    };
  }

  public static Specification<PreferredAgent> forPreferredAgents() {
    Set<UserRole> roles = extractRoles();
    RoleTier tier = RBACUtilities.resolveRoleTier(roles);

    return switch (tier) {
      case SYSTEM -> {
        log.debug("SYSTEM_ADMIN access: no filter applied for preferred agents");
        yield noFilter();
      }
      case ORGANIZATION -> {
        String organizationId = validateActiveOrganization();
        log.debug(
            "ORGANIZATION access: filtering preferred agents by organizationId={}", organizationId);
        yield filterPreferredAgentByOrganizationId(organizationId);
      }
      case BRANCH -> {
        String organizationId = validateActiveOrganization();
        String branchId = validateActiveBranch();
        log.debug(
            "BRANCH access: filtering preferred agents by organizationId={}, branchId={}",
            organizationId,
            branchId);
        yield filterPreferredAgentByOrganizationId(organizationId)
            .and(filterPreferredAgentByBranchId(branchId));
      }
      case AGENT, UNKNOWN ->
          throw new AuthorizationException(
              ResponseCode.FORBIDDEN,
              "Access denied: insufficient role for preferred agent access");
    };
  }

  private static Set<com.ebikes.workforce.enums.UserRole> extractRoles() {
    return switch (ExecutionContext.get()) {
      case UserContext uc -> RBACUtilities.parseRoles(uc.roles());
      case ExecutionContext.SystemContext ignored -> Set.of();
    };
  }

  private static Specification<Agent> filterAgentByUserId(String userId) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("userId"), userId);
  }

  private static Specification<PreferredAgent> filterPreferredAgentByBranchId(String branchId) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("branchId"), branchId);
  }

  private static Specification<PreferredAgent> filterPreferredAgentByOrganizationId(
      String organizationId) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get("organizationId"), organizationId);
  }

  private static <T> Specification<T> noFilter() {
    return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
  }

  private static String validateActiveBranch() {
    if (ExecutionContext.get() instanceof UserContext uc
        && uc.activeBranch() != null
        && !uc.activeBranch().isBlank()) {
      return uc.activeBranch();
    }
    log.error("Missing active_branch claim for preferred agent access");
    throw new AuthorizationException(
        ResponseCode.FORBIDDEN, "Active branch context required for this operation");
  }

  private static String validateActiveOrganization() {
    if (ExecutionContext.get() instanceof UserContext uc
        && uc.activeOrganization() != null
        && !uc.activeOrganization().isBlank()) {
      return uc.activeOrganization();
    }
    log.error("Missing active_organization claim for preferred agent access");
    throw new AuthorizationException(
        ResponseCode.FORBIDDEN, "Active organization context required for this operation");
  }
}
