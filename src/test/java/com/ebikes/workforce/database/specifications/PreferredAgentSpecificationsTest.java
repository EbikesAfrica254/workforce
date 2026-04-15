package com.ebikes.workforce.database.specifications;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ebikes.workforce.database.entities.PreferredAgent;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.PreferredAgentRepository;
import com.ebikes.workforce.dtos.requests.filters.PreferredAgentFilter;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.enums.UserRole;
import com.ebikes.workforce.support.context.ExecutionContext;
import com.ebikes.workforce.support.fixtures.AgentFixtures;
import com.ebikes.workforce.support.fixtures.SecurityFixtures;
import com.ebikes.workforce.support.infrastructure.AbstractRepositoryTest;

@DisplayName("PreferredAgentSpecifications")
class PreferredAgentSpecificationsTest extends AbstractRepositoryTest {

  private static final String ORGANIZATION_A = SecurityFixtures.TEST_ORGANIZATION_ID;
  private static final String ORGANIZATION_B = SecurityFixtures.OTHER_ORGANIZATION_ID;
  private static final String BRANCH_A = "branch-001";

  @Autowired private AgentRepository agentRepository;
  @Autowired private PreferredAgentRepository preferredAgentRepository;

  private UUID agentAId;

  @BeforeEach
  void setUp() {
    SecurityFixtures.setExecutionContext(ORGANIZATION_A, null, UserRole.SYSTEM_ADMIN);

    agentAId =
        agentRepository
            .save(
                AgentFixtures.forPersistence(
                    "NID-001",
                    "+254700000001",
                    "user-001",
                    CapabilityClass.BICYCLE_RIDER,
                    AvailabilityStatus.AVAILABLE))
            .getId();
    UUID agentBId =
        agentRepository
            .save(
                AgentFixtures.forPersistence(
                    "NID-002",
                    "+254700000002",
                    "user-002",
                    CapabilityClass.BICYCLE_RIDER,
                    AvailabilityStatus.AVAILABLE))
            .getId();
    preferredAgentRepository.saveAll(
        List.of(
            PreferredAgent.create(agentAId, null, null, ORGANIZATION_A, 1),
            PreferredAgent.create(agentBId, BRANCH_A, null, ORGANIZATION_A, 2),
            PreferredAgent.create(agentAId, null, null, ORGANIZATION_B, 1)));
  }

  @AfterEach
  void tearDown() {
    agentRepository.deleteAll();
    preferredAgentRepository.deleteAll();
    ExecutionContext.clear();
  }

  private PreferredAgentFilter filter() {
    return new PreferredAgentFilter();
  }

  @Nested
  @DisplayName("Filter by agent ID")
  class FilterByAgentId {

    @Test
    @DisplayName("should return all records for the specified agent")
    void shouldReturnAllRecordsForSpecifiedAgent() {
      PreferredAgentFilter f = filter();
      f.setAgentId(agentAId);

      List<PreferredAgent> results =
          preferredAgentRepository.findAll(PreferredAgentSpecifications.buildSpecification(f));

      assertThat(results).hasSize(2).allMatch(pa -> pa.getAgentId().equals(agentAId));
    }

    @Test
    @DisplayName("should return empty for unknown agent ID")
    void shouldReturnEmptyForUnknownAgentId() {
      PreferredAgentFilter f = filter();
      f.setAgentId(UUID.randomUUID());

      List<PreferredAgent> results =
          preferredAgentRepository.findAll(PreferredAgentSpecifications.buildSpecification(f));

      assertThat(results).isEmpty();
    }
  }

  @Nested
  @DisplayName("Filter by branch ID")
  class FilterByBranchId {

    @Test
    @DisplayName("should return only branch-scoped records")
    void shouldReturnOnlyBranchScopedRecords() {
      PreferredAgentFilter f = filter();
      f.setBranchId(BRANCH_A);

      List<PreferredAgent> results =
          preferredAgentRepository.findAll(PreferredAgentSpecifications.buildSpecification(f));

      assertThat(results)
          .hasSize(1)
          .first()
          .satisfies(pa -> assertThat(pa.getBranchId()).isEqualTo(BRANCH_A));
    }

    @Test
    @DisplayName("should return empty for unknown branch ID")
    void shouldReturnEmptyForUnknownBranchId() {
      PreferredAgentFilter f = filter();
      f.setBranchId("branch-999");

      List<PreferredAgent> results =
          preferredAgentRepository.findAll(PreferredAgentSpecifications.buildSpecification(f));

      assertThat(results).isEmpty();
    }
  }

  @Nested
  @DisplayName("Filter by organization ID")
  class FilterByOrganizationId {

    @Test
    @DisplayName("should return only records for the specified organization")
    void shouldReturnOnlyRecordsForSpecifiedOrg() {
      PreferredAgentFilter f = filter();
      f.setOrganizationId(ORGANIZATION_A);

      List<PreferredAgent> results =
          preferredAgentRepository.findAll(PreferredAgentSpecifications.buildSpecification(f));

      assertThat(results).hasSize(2).allMatch(pa -> pa.getOrganizationId().equals(ORGANIZATION_A));
    }

    @Test
    @DisplayName("should return empty for unknown organization ID")
    void shouldReturnEmptyForUnknownOrganizationId() {
      PreferredAgentFilter f = filter();
      f.setOrganizationId("00000000-0000-0000-0000-000000000000");

      List<PreferredAgent> results =
          preferredAgentRepository.findAll(PreferredAgentSpecifications.buildSpecification(f));

      assertThat(results).isEmpty();
    }
  }

  @Nested
  @DisplayName("Combined filters")
  class CombinedFilters {

    @Test
    @DisplayName("agentId and organizationId combined returns correct single record")
    void agentIdAndOrganizationIdReturnsSingleRecord() {
      PreferredAgentFilter f = filter();
      f.setAgentId(agentAId);
      f.setOrganizationId(ORGANIZATION_A);

      List<PreferredAgent> results =
          preferredAgentRepository.findAll(PreferredAgentSpecifications.buildSpecification(f));

      assertThat(results)
          .hasSize(1)
          .first()
          .satisfies(
              pa -> {
                assertThat(pa.getAgentId()).isEqualTo(agentAId);
                assertThat(pa.getOrganizationId()).isEqualTo(ORGANIZATION_A);
              });
    }

    @Test
    @DisplayName("organizationId and branchId combined returns correct single record")
    void organizationIdAndBranchIdReturnsSingleRecord() {
      PreferredAgentFilter f = filter();
      f.setOrganizationId(ORGANIZATION_A);
      f.setBranchId(BRANCH_A);

      List<PreferredAgent> results =
          preferredAgentRepository.findAll(PreferredAgentSpecifications.buildSpecification(f));

      assertThat(results)
          .hasSize(1)
          .first()
          .satisfies(
              pa -> {
                assertThat(pa.getOrganizationId()).isEqualTo(ORGANIZATION_A);
                assertThat(pa.getBranchId()).isEqualTo(BRANCH_A);
              });
    }
  }
}
