package com.ebikes.workforce.services.agents;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.enums.UserRole;
import com.ebikes.workforce.exceptions.AuthorizationException;
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.support.context.ExecutionContext;
import com.ebikes.workforce.support.fixtures.AgentFixtures;
import com.ebikes.workforce.support.fixtures.SecurityFixtures;

@DisplayName("AccessPolicy")
@ExtendWith(MockitoExtension.class)
class AccessPolicyTest {

  private final AccessPolicy accessPolicy = new AccessPolicy();

  @AfterEach
  void tearDown() {
    ExecutionContext.clear();
  }

  @Test
  @DisplayName("canCreate: SystemContext → no exception")
  void canCreateSystemContextNoException() {
    ExecutionContext.setSystem();

    assertThatNoException().isThrownBy(() -> accessPolicy.canCreate(AgentFixtures.USER_ID));
  }

  @Test
  @DisplayName("canCreate: privileged role → no exception regardless of userId mismatch")
  void canCreatePrivilegedRoleNoException() {
    SecurityFixtures.setExecutionContext(
        SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.BRANCH_ADMIN);

    assertThatNoException().isThrownBy(() -> accessPolicy.canCreate(AgentFixtures.USER_ID));
  }

  @Test
  @DisplayName("canCreate: AGENT role, userId matches → no exception")
  void canCreateAgentRoleUserIdMatchesNoException() {
    ExecutionContext.set(
        AgentFixtures.USER_ID,
        SecurityFixtures.TEST_ORGANIZATION_ID,
        null,
        SecurityFixtures.TEST_EMAIL,
        Set.of(),
        SecurityFixtures.TEST_PHONE_NUMBER,
        Set.of(UserRole.AGENT.name()));

    assertThatNoException().isThrownBy(() -> accessPolicy.canCreate(AgentFixtures.USER_ID));
  }

  @Test
  @DisplayName("canCreate: AGENT role, userId mismatch → AuthorizationException")
  void canCreateAgentRoleUserIdMismatchThrowsAuthorizationException() {
    SecurityFixtures.setExecutionContext(
        SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.AGENT);

    assertThatThrownBy(() -> accessPolicy.canCreate(AgentFixtures.USER_ID))
        .isInstanceOf(AuthorizationException.class);
  }

  @Test
  @DisplayName("canCreate: no role, userId matches → no exception")
  void canCreateNoRoleUserIdMatchesNoException() {
    ExecutionContext.set(
        AgentFixtures.USER_ID,
        SecurityFixtures.TEST_ORGANIZATION_ID,
        null,
        SecurityFixtures.TEST_EMAIL,
        Set.of(),
        SecurityFixtures.TEST_PHONE_NUMBER,
        Set.of());

    assertThatNoException().isThrownBy(() -> accessPolicy.canCreate(AgentFixtures.USER_ID));
  }

  @Test
  @DisplayName("canCreate: no role, userId mismatch → AuthorizationException")
  void canCreateNoRoleUserIdMismatchThrowsAuthorizationException() {
    SecurityFixtures.setExecutionContext(SecurityFixtures.TEST_ORGANIZATION_ID, null);

    assertThatThrownBy(() -> accessPolicy.canCreate(AgentFixtures.USER_ID))
        .isInstanceOf(AuthorizationException.class);
  }

  @Test
  @DisplayName("owns: SystemContext → no exception")
  void ownsSystemContextNoException() {
    ExecutionContext.setSystem();
    Agent agent = AgentFixtures.available();

    assertThatNoException().isThrownBy(() -> accessPolicy.owns(agent));
  }

  @Test
  @DisplayName("owns: AGENT role, userId matches agent → no exception")
  void ownsAgentRoleUserIdMatchesNoException() {
    ExecutionContext.set(
        AgentFixtures.USER_ID,
        SecurityFixtures.TEST_ORGANIZATION_ID,
        null,
        SecurityFixtures.TEST_EMAIL,
        Set.of(),
        SecurityFixtures.TEST_PHONE_NUMBER,
        Set.of(UserRole.AGENT.name()));

    assertThatNoException().isThrownBy(() -> accessPolicy.owns(AgentFixtures.available()));
  }

  @Test
  @DisplayName("owns: AGENT role, userId mismatch → BusinessRuleException")
  void ownsAgentRoleUserIdMismatchThrowsBusinessRuleException() {
    SecurityFixtures.setExecutionContext(
        SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.AGENT);

    Agent testAgent = AgentFixtures.available();
    assertThatThrownBy(() -> accessPolicy.owns(testAgent))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  @DisplayName("owns: non-agent role, userId mismatch → no exception")
  void ownsNonAgentRoleUserIdMismatchNoException() {
    SecurityFixtures.setExecutionContext(
        SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.BRANCH_OPERATOR);

    assertThatNoException().isThrownBy(() -> accessPolicy.owns(AgentFixtures.available()));
  }
}
