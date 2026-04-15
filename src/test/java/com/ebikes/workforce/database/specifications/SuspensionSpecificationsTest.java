package com.ebikes.workforce.database.specifications;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ebikes.workforce.database.entities.Suspension;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.SuspensionRepository;
import com.ebikes.workforce.dtos.requests.filters.SuspensionFilter;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.enums.UserRole;
import com.ebikes.workforce.support.context.ExecutionContext;
import com.ebikes.workforce.support.fixtures.AgentFixtures;
import com.ebikes.workforce.support.fixtures.SecurityFixtures;
import com.ebikes.workforce.support.infrastructure.AbstractRepositoryTest;

@DisplayName("SuspensionSpecifications")
class SuspensionSpecificationsTest extends AbstractRepositoryTest {

  @Autowired private AgentRepository agentRepository;
  @Autowired private SuspensionRepository suspensionRepository;

  private UUID agentAId;

  @BeforeEach
  void setUp() {
    SecurityFixtures.setExecutionContext(
        SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.SYSTEM_ADMIN);

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

    Suspension lifted = Suspension.builder().agentId(agentAId).reason("Policy violation").build();

    suspensionRepository.save(lifted);
    lifted.lift(SecurityFixtures.TEST_USER_ID);
    suspensionRepository.saveAndFlush(lifted);

    suspensionRepository.save(
        Suspension.builder()
            .agentId(agentAId)
            .reason("Policy violation")
            .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(7))
            .build());

    suspensionRepository.save(
        Suspension.builder().agentId(agentBId).reason("Policy violation").build());
  }

  @AfterEach
  void tearDown() {
    suspensionRepository.deleteAll();
    agentRepository.deleteAll();
    ExecutionContext.clear();
  }

  private SuspensionFilter filter() {
    return new SuspensionFilter();
  }

  @Nested
  @DisplayName("Filter by agent ID")
  class FilterByAgentId {

    @Test
    @DisplayName("should return all records for the specified agent")
    void shouldReturnAllRecordsForSpecifiedAgent() {
      SuspensionFilter f = filter();
      f.setAgentId(agentAId);

      List<Suspension> results =
          suspensionRepository.findAll(SuspensionSpecifications.buildSpecification(f));

      assertThat(results).hasSize(2).allMatch(s -> s.getAgentId().equals(agentAId));
    }

    @Test
    @DisplayName("should return empty for unknown agent ID")
    void shouldReturnEmptyForUnknownAgentId() {
      SuspensionFilter f = filter();
      f.setAgentId(UUID.randomUUID());

      List<Suspension> results =
          suspensionRepository.findAll(SuspensionSpecifications.buildSpecification(f));

      assertThat(results).isEmpty();
    }
  }

  @Nested
  @DisplayName("Filter by isActive")
  class FilterByIsActive {

    @Test
    @DisplayName("should return only active suspensions when true")
    void shouldReturnOnlyActiveWhenTrue() {
      SuspensionFilter f = filter();
      f.setIsActive(true);

      List<Suspension> results =
          suspensionRepository.findAll(SuspensionSpecifications.buildSpecification(f));

      assertThat(results).hasSize(2).allMatch(s -> s.getLiftedAt() == null);
    }

    @Test
    @DisplayName("should return only lifted suspensions when false")
    void shouldReturnOnlyLiftedWhenFalse() {
      SuspensionFilter f = filter();
      f.setIsActive(false);

      List<Suspension> results =
          suspensionRepository.findAll(SuspensionSpecifications.buildSpecification(f));

      assertThat(results)
          .hasSize(1)
          .first()
          .satisfies(s -> assertThat(s.getLiftedAt()).isNotNull());
    }

    @Test
    @DisplayName("should return all suspensions when null")
    void shouldReturnAllWhenNull() {
      List<Suspension> results =
          suspensionRepository.findAll(SuspensionSpecifications.buildSpecification(filter()));

      assertThat(results).hasSize(3);
    }
  }

  @Nested
  @DisplayName("Filter by expiresAt range")
  class FilterByExpiresAtRange {

    @Test
    @DisplayName("expiresAtFrom in future returns empty")
    void expiresAtFromInFutureReturnsEmpty() {
      SuspensionFilter f = filter();
      f.setExpiresAtFrom(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));

      List<Suspension> results =
          suspensionRepository.findAll(SuspensionSpecifications.buildSpecification(f));

      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("expiresAtTo in past returns empty")
    void expiresAtToInPastReturnsEmpty() {
      SuspensionFilter f = filter();
      f.setExpiresAtTo(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));

      List<Suspension> results =
          suspensionRepository.findAll(SuspensionSpecifications.buildSpecification(f));

      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("bracketing range returns only the suspension with expiresAt set")
    void bracketingRangeReturnsOnlyExpiringRecord() {
      SuspensionFilter f = filter();
      f.setExpiresAtFrom(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
      f.setExpiresAtTo(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));

      List<Suspension> results =
          suspensionRepository.findAll(SuspensionSpecifications.buildSpecification(f));

      assertThat(results)
          .hasSize(1)
          .first()
          .satisfies(s -> assertThat(s.getExpiresAt()).isNotNull());
    }
  }

  @Nested
  @DisplayName("Combined filters")
  class CombinedFilters {

    @Test
    @DisplayName("agentId and isActive combined returns correct subset")
    void agentIdAndIsActiveReturnsCorrectSubset() {
      SuspensionFilter f = filter();
      f.setAgentId(agentAId);
      f.setIsActive(false);

      List<Suspension> results =
          suspensionRepository.findAll(SuspensionSpecifications.buildSpecification(f));

      assertThat(results)
          .hasSize(1)
          .first()
          .satisfies(
              s -> {
                assertThat(s.getAgentId()).isEqualTo(agentAId);
                assertThat(s.getLiftedAt()).isNotNull();
              });
    }
  }
}
