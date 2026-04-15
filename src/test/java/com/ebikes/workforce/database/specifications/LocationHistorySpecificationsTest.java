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

import com.ebikes.workforce.database.entities.LocationHistory;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.LocationHistoryRepository;
import com.ebikes.workforce.dtos.requests.filters.LocationHistoryFilter;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.enums.LocationSource;
import com.ebikes.workforce.enums.UserRole;
import com.ebikes.workforce.support.context.ExecutionContext;
import com.ebikes.workforce.support.fixtures.AgentDtoFixtures;
import com.ebikes.workforce.support.fixtures.AgentFixtures;
import com.ebikes.workforce.support.fixtures.SecurityFixtures;
import com.ebikes.workforce.support.infrastructure.AbstractRepositoryTest;

@DisplayName("LocationHistorySpecifications")
class LocationHistorySpecificationsTest extends AbstractRepositoryTest {

  @Autowired private AgentRepository agentRepository;
  @Autowired private LocationHistoryRepository locationHistoryRepository;

  private UUID agentId;

  @BeforeEach
  void setUp() {
    SecurityFixtures.setExecutionContext(
        SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.SYSTEM_ADMIN);

    agentId =
        agentRepository
            .save(
                AgentFixtures.forPersistence(
                    "NID-001",
                    "+254700000001",
                    "user-001",
                    CapabilityClass.BICYCLE_RIDER,
                    AvailabilityStatus.AVAILABLE))
            .getId();

    locationHistoryRepository.saveAll(
        List.of(
            LocationHistory.create(
                agentId,
                AgentDtoFixtures.LATITUDE,
                AgentDtoFixtures.LONGITUDE,
                "8763a4e6fffffff",
                LocationSource.WEB_BROWSER),
            LocationHistory.create(
                agentId,
                AgentDtoFixtures.LATITUDE,
                AgentDtoFixtures.LONGITUDE,
                "8763a4e6fffffff",
                LocationSource.MOBILE_BROWSER)));
  }

  @AfterEach
  void tearDown() {
    locationHistoryRepository.deleteAll();
    agentRepository.deleteAll();
    ExecutionContext.clear();
  }

  private LocationHistoryFilter filter() {
    LocationHistoryFilter f = new LocationHistoryFilter();
    f.setAgentId(agentId);
    return f;
  }

  @Nested
  @DisplayName("Filter by agent ID")
  class FilterByAgentId {

    @Test
    @DisplayName("should return all records for the specified agent")
    void shouldReturnAllRecordsForSpecifiedAgent() {
      List<LocationHistory> results =
          locationHistoryRepository.findAll(
              LocationHistorySpecifications.buildSpecification(filter()));

      assertThat(results).hasSize(2).allMatch(l -> l.getAgentId().equals(agentId));
    }

    @Test
    @DisplayName("should return empty for unknown agent ID")
    void shouldReturnEmptyForUnknownAgentId() {
      LocationHistoryFilter f = new LocationHistoryFilter();
      f.setAgentId(UUID.randomUUID());

      List<LocationHistory> results =
          locationHistoryRepository.findAll(LocationHistorySpecifications.buildSpecification(f));

      assertThat(results).isEmpty();
    }
  }

  @Nested
  @DisplayName("Filter by source")
  class FilterBySource {

    @Test
    @DisplayName("should return only WEB_BROWSER records")
    void shouldReturnOnlyWebBrowserRecords() {
      LocationHistoryFilter f = filter();
      f.setSource(LocationSource.WEB_BROWSER);

      List<LocationHistory> results =
          locationHistoryRepository.findAll(LocationHistorySpecifications.buildSpecification(f));

      assertThat(results).hasSize(1).allMatch(l -> l.getSource() == LocationSource.WEB_BROWSER);
    }

    @Test
    @DisplayName("should return only MOBILE_BROWSER records")
    void shouldReturnOnlyMobileBrowserRecords() {
      LocationHistoryFilter f = filter();
      f.setSource(LocationSource.MOBILE_BROWSER);

      List<LocationHistory> results =
          locationHistoryRepository.findAll(LocationHistorySpecifications.buildSpecification(f));

      assertThat(results).hasSize(1).allMatch(l -> l.getSource() == LocationSource.MOBILE_BROWSER);
    }

    @Test
    @DisplayName("should return empty when no records match source")
    void shouldReturnEmptyWhenNoMatchForSource() {
      LocationHistoryFilter f = filter();
      f.setSource(LocationSource.ITRACK_DEVICE);

      List<LocationHistory> results =
          locationHistoryRepository.findAll(LocationHistorySpecifications.buildSpecification(f));

      assertThat(results).isEmpty();
    }
  }

  @Nested
  @DisplayName("Filter by created at range")
  class FilterByCreatedAtRange {

    @Test
    @DisplayName("createdAtFrom in future returns empty")
    void createdAtFromInFutureReturnsEmpty() {
      LocationHistoryFilter f = filter();
      f.setCreatedAtFrom(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1));

      List<LocationHistory> results =
          locationHistoryRepository.findAll(LocationHistorySpecifications.buildSpecification(f));

      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("createdAtTo in past returns empty")
    void createdAtToInPastReturnsEmpty() {
      LocationHistoryFilter f = filter();
      f.setCreatedAtTo(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));

      List<LocationHistory> results =
          locationHistoryRepository.findAll(LocationHistorySpecifications.buildSpecification(f));

      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("bracketing range returns all agent records")
    void bracketingRangeReturnsAllRecords() {
      LocationHistoryFilter f = filter();
      f.setCreatedAtFrom(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
      f.setCreatedAtTo(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1));

      List<LocationHistory> results =
          locationHistoryRepository.findAll(LocationHistorySpecifications.buildSpecification(f));

      assertThat(results).hasSize(2);
    }
  }

  @Nested
  @DisplayName("Combined filters")
  class CombinedFilters {

    @Test
    @DisplayName("agentId and source combined returns correct single record")
    void agentIdAndSourceReturnsSingleRecord() {
      LocationHistoryFilter f = filter();
      f.setSource(LocationSource.WEB_BROWSER);

      List<LocationHistory> results =
          locationHistoryRepository.findAll(LocationHistorySpecifications.buildSpecification(f));

      assertThat(results)
          .hasSize(1)
          .first()
          .satisfies(
              l -> {
                assertThat(l.getAgentId()).isEqualTo(agentId);
                assertThat(l.getSource()).isEqualTo(LocationSource.WEB_BROWSER);
              });
    }
  }
}
