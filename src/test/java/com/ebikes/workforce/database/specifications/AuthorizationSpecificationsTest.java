package com.ebikes.workforce.database.specifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.PreferredAgent;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.PreferredAgentRepository;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.enums.NationalIdType;
import com.ebikes.workforce.enums.UserRole;
import com.ebikes.workforce.exceptions.AuthorizationException;
import com.ebikes.workforce.support.context.ExecutionContext;
import com.ebikes.workforce.support.fixtures.SecurityFixtures;
import com.ebikes.workforce.support.infrastructure.AbstractRepositoryTest;

@DisplayName("AuthorizationSpecifications")
class AuthorizationSpecificationsTest extends AbstractRepositoryTest {

  private static final String ORGANIZATION_A = SecurityFixtures.TEST_ORGANIZATION_ID;
  private static final String ORGANIZATION_B = SecurityFixtures.OTHER_ORGANIZATION_ID;
  private static final String BRANCH_A = "branch-001";

  @Autowired private AgentRepository agentRepository;
  @Autowired private PreferredAgentRepository preferredAgentRepository;

  @BeforeEach
  void setUp() {
    SecurityFixtures.setExecutionContext(ORGANIZATION_A, null, UserRole.SYSTEM_ADMIN);

    Agent agentA =
        agentRepository.save(agent("NID-A01", "+254700000020", SecurityFixtures.TEST_USER_ID));
    Agent agentB =
        agentRepository.save(agent("NID-B01", "+254700000021", SecurityFixtures.OTHER_USER_ID));

    preferredAgentRepository.saveAll(
        List.of(
            preferredAgent(agentA.getId(), ORGANIZATION_A, null),
            preferredAgent(agentB.getId(), ORGANIZATION_A, BRANCH_A),
            preferredAgent(agentA.getId(), ORGANIZATION_B, null)));
  }

  @AfterEach
  void tearDown() {
    preferredAgentRepository.deleteAll();
    agentRepository.deleteAll();
    ExecutionContext.clear();
  }

  private Agent agent(String nationalId, String phone, String userId) {
    return Agent.builder()
        .availabilityStatus(AvailabilityStatus.AVAILABLE)
        .capabilityClass(CapabilityClass.BICYCLE_RIDER)
        .firstName("Test")
        .lastName("Agent")
        .maxConcurrentOrders(CapabilityClass.BICYCLE_RIDER.getDefaultMaxConcurrentOrders())
        .nationalIdNumber(nationalId)
        .nationalIdType(NationalIdType.NATIONAL_ID)
        .phoneNumber(phone)
        .userId(userId)
        .build();
  }

  private PreferredAgent preferredAgent(UUID agentId, String organizationId, String branchId) {
    return PreferredAgent.builder()
        .agentId(agentId)
        .organizationId(organizationId)
        .branchId(branchId)
        .priority(1)
        .build();
  }

  @Nested
  @DisplayName("forAgents")
  class ForAgents {

    @Test
    @DisplayName("SYSTEM_ADMIN sees all agents")
    void systemAdminSeesAllAgents() {
      SecurityFixtures.setExecutionContext(ORGANIZATION_A, null, UserRole.SYSTEM_ADMIN);

      List<Agent> results = agentRepository.findAll(AuthorizationSpecifications.forAgents());

      assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("AGENT sees only their own record")
    void agentSeesOnlyOwnRecord() {
      SecurityFixtures.setExecutionContext(ORGANIZATION_A, null, UserRole.AGENT);

      List<Agent> results = agentRepository.findAll(AuthorizationSpecifications.forAgents());

      assertThat(results)
          .hasSize(1)
          .first()
          .satisfies(a -> assertThat(a.getUserId()).isEqualTo(SecurityFixtures.TEST_USER_ID));
    }

    @Test
    @DisplayName("ORGANIZATION_ADMIN sees all agents (shared pool)")
    void organizationAdminSeesAllAgents() {
      SecurityFixtures.setExecutionContext(ORGANIZATION_A, null, UserRole.ORGANIZATION_ADMIN);

      List<Agent> results = agentRepository.findAll(AuthorizationSpecifications.forAgents());

      assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("BRANCH_ADMIN sees all agents (shared pool)")
    void branchAdminSeesAllAgents() {
      SecurityFixtures.setExecutionContext(ORGANIZATION_A, BRANCH_A, UserRole.BRANCH_ADMIN);

      List<Agent> results = agentRepository.findAll(AuthorizationSpecifications.forAgents());

      assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("UNKNOWN role throws AuthorizationException")
    void unknownRoleThrows() {
      SecurityFixtures.setExecutionContext(ORGANIZATION_A, null, UserRole.CUSTOMER);

      assertThatThrownBy(() -> agentRepository.findAll(AuthorizationSpecifications.forAgents()))
          .isInstanceOf(AuthorizationException.class);
    }
  }

  @Nested
  @DisplayName("forPreferredAgents")
  class ForPreferredAgents {

    @Test
    @DisplayName("SYSTEM_ADMIN sees all preferred agents")
    void systemAdminSeesAll() {
      SecurityFixtures.setExecutionContext(ORGANIZATION_A, null, UserRole.SYSTEM_ADMIN);

      List<PreferredAgent> results =
          preferredAgentRepository.findAll(AuthorizationSpecifications.forPreferredAgents());

      assertThat(results).hasSize(3);
    }

    @Test
    @DisplayName("ORGANIZATION_ADMIN sees only own org's preferred agents")
    void organizationAdminSeesOwnOrg() {
      SecurityFixtures.setExecutionContext(ORGANIZATION_A, null, UserRole.ORGANIZATION_ADMIN);

      List<PreferredAgent> results =
          preferredAgentRepository.findAll(AuthorizationSpecifications.forPreferredAgents());

      assertThat(results).hasSize(2).allMatch(pa -> pa.getOrganizationId().equals(ORGANIZATION_A));
    }

    @Test
    @DisplayName("ORGANIZATION_ADMIN without active org throws AuthorizationException")
    void organizationAdminWithoutOrgThrows() {
      SecurityFixtures.setExecutionContext(null, null, UserRole.ORGANIZATION_ADMIN);

      assertThatThrownBy(
              () ->
                  preferredAgentRepository.findAll(
                      AuthorizationSpecifications.forPreferredAgents()))
          .isInstanceOf(AuthorizationException.class);
    }

    @Test
    @DisplayName("BRANCH_ADMIN sees only own org and branch preferred agents")
    void branchAdminSeesOwnBranch() {
      SecurityFixtures.setExecutionContext(ORGANIZATION_A, BRANCH_A, UserRole.BRANCH_ADMIN);

      List<PreferredAgent> results =
          preferredAgentRepository.findAll(AuthorizationSpecifications.forPreferredAgents());

      assertThat(results)
          .hasSize(1)
          .first()
          .satisfies(
              pa -> {
                assertThat(pa.getOrganizationId()).isEqualTo(ORGANIZATION_A);
                assertThat(pa.getBranchId()).isEqualTo(BRANCH_A);
              });
    }

    @Test
    @DisplayName("BRANCH_ADMIN without active branch throws AuthorizationException")
    void branchAdminWithoutBranchThrows() {
      SecurityFixtures.setExecutionContext(ORGANIZATION_A, null, UserRole.BRANCH_ADMIN);

      assertThatThrownBy(
              () ->
                  preferredAgentRepository.findAll(
                      AuthorizationSpecifications.forPreferredAgents()))
          .isInstanceOf(AuthorizationException.class);
    }

    @Test
    @DisplayName("AGENT throws AuthorizationException")
    void agentThrows() {
      SecurityFixtures.setExecutionContext(ORGANIZATION_A, null, UserRole.AGENT);

      assertThatThrownBy(
              () ->
                  preferredAgentRepository.findAll(
                      AuthorizationSpecifications.forPreferredAgents()))
          .isInstanceOf(AuthorizationException.class);
    }

    @Test
    @DisplayName("UNKNOWN role throws AuthorizationException")
    void unknownRoleThrows() {
      SecurityFixtures.setExecutionContext(ORGANIZATION_A, null, UserRole.CUSTOMER);

      assertThatThrownBy(
              () ->
                  preferredAgentRepository.findAll(
                      AuthorizationSpecifications.forPreferredAgents()))
          .isInstanceOf(AuthorizationException.class);
    }
  }
}
