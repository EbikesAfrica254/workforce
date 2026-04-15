package com.ebikes.workforce.database.specifications;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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

import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.Suspension;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.SuspensionRepository;
import com.ebikes.workforce.dtos.requests.filters.AgentFilter;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.enums.NationalIdType;
import com.ebikes.workforce.enums.UserRole;
import com.ebikes.workforce.support.context.ExecutionContext;
import com.ebikes.workforce.support.fixtures.SecurityFixtures;
import com.ebikes.workforce.support.infrastructure.AbstractRepositoryTest;

@DisplayName("AgentSpecifications")
class AgentSpecificationsTest extends AbstractRepositoryTest {

  @Autowired private AgentRepository agentRepository;
  @Autowired private SuspensionRepository suspensionRepository;

  private Agent bicycleRider;
  private Agent lightMotorRider;
  private Agent vehicleDriver;

  @BeforeEach
  void setUp() {
    SecurityFixtures.setExecutionContext(
        SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.SYSTEM_ADMIN);

    bicycleRider =
        agentRepository.save(
            agent(
                "NID-001",
                "+254700000001",
                CapabilityClass.BICYCLE_RIDER,
                AvailabilityStatus.AVAILABLE,
                "Alice",
                "Kamau"));
    lightMotorRider =
        agentRepository.save(
            agent(
                "NID-002",
                "+254700000002",
                CapabilityClass.LIGHT_MOTOR_RIDER,
                AvailabilityStatus.OFFLINE,
                "Bob",
                "Mwangi"));
    vehicleDriver =
        agentRepository.save(
            agent(
                "NID-003",
                "+254700000003",
                CapabilityClass.VEHICLE_DRIVER,
                AvailabilityStatus.AVAILABLE,
                "Carol",
                "Odhiambo"));
  }

  @AfterEach
  void tearDown() {
    suspensionRepository.deleteAll();
    agentRepository.deleteAll();
    ExecutionContext.clear();
  }

  private Agent agent(
      String nationalId,
      String phone,
      CapabilityClass capabilityClass,
      AvailabilityStatus status,
      String firstName,
      String lastName) {
    return Agent.builder()
        .availabilityStatus(status)
        .capabilityClass(capabilityClass)
        .firstName(firstName)
        .lastName(lastName)
        .maxConcurrentOrders(capabilityClass.getDefaultMaxConcurrentOrders())
        .nationalIdNumber(nationalId)
        .nationalIdType(NationalIdType.NATIONAL_ID)
        .phoneNumber(phone)
        .userId(UUID.randomUUID().toString())
        .build();
  }

  @Nested
  @DisplayName("Filter by availability status")
  class FilterByAvailabilityStatus {

    @Test
    @DisplayName("should return only AVAILABLE agents")
    void shouldReturnOnlyAvailableAgents() {
      AgentFilter filter = new AgentFilter();
      filter.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);

      List<Agent> results = agentRepository.findAll(AgentSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(2)
          .allMatch(a -> a.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE);
    }

    @Test
    @DisplayName("should return only OFFLINE agents")
    void shouldReturnOnlyOfflineAgents() {
      AgentFilter filter = new AgentFilter();
      filter.setAvailabilityStatus(AvailabilityStatus.OFFLINE);

      List<Agent> results = agentRepository.findAll(AgentSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(1)
          .allMatch(a -> a.getAvailabilityStatus() == AvailabilityStatus.OFFLINE);
    }
  }

  @Nested
  @DisplayName("Filter by capability class")
  class FilterByCapabilityClass {

    @Test
    @DisplayName("should return only BICYCLE_RIDER agents")
    void shouldReturnOnlyBicycleRiders() {
      AgentFilter filter = new AgentFilter();
      filter.setCapabilityClass(CapabilityClass.BICYCLE_RIDER);

      List<Agent> results = agentRepository.findAll(AgentSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(1)
          .allMatch(a -> a.getCapabilityClass() == CapabilityClass.BICYCLE_RIDER);
    }

    @Test
    @DisplayName("should return empty when no agents match capability class")
    void shouldReturnEmptyWhenNoMatch() {
      AgentFilter filter = new AgentFilter();
      filter.setCapabilityClass(CapabilityClass.VEHICLE_DRIVER);

      agentRepository.deleteAll();
      agentRepository.save(
          agent(
              "NID-010",
              "+254700000010",
              CapabilityClass.BICYCLE_RIDER,
              AvailabilityStatus.AVAILABLE,
              "Dan",
              "Otieno"));

      List<Agent> results = agentRepository.findAll(AgentSpecifications.buildSpecification(filter));

      assertThat(results).isEmpty();
    }
  }

  @Nested
  @DisplayName("Filter by created at range")
  class FilterByCreatedAtRange {

    @Test
    @DisplayName("createdAtFrom in future returns empty")
    void createdAtFromInFutureReturnsEmpty() {
      AgentFilter filter = new AgentFilter();
      filter.setCreatedAtFrom(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1));

      List<Agent> results = agentRepository.findAll(AgentSpecifications.buildSpecification(filter));

      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("createdAtTo in past returns empty")
    void createdAtToInPastReturnsEmpty() {
      AgentFilter filter = new AgentFilter();
      filter.setCreatedAtTo(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));

      List<Agent> results = agentRepository.findAll(AgentSpecifications.buildSpecification(filter));

      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("createdAtFrom and createdAtTo bracketing now returns all agents")
    void bracketingNowReturnsAllAgents() {
      AgentFilter filter = new AgentFilter();
      filter.setCreatedAtFrom(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
      filter.setCreatedAtTo(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1));

      List<Agent> results = agentRepository.findAll(AgentSpecifications.buildSpecification(filter));

      assertThat(results).hasSize(3);
    }
  }

  @Nested
  @DisplayName("Filter by first name")
  class FilterByFirstName {

    @Test
    @DisplayName("should match case-insensitively")
    void shouldMatchCaseInsensitively() {
      AgentFilter filter = new AgentFilter();
      filter.setFirstName("alice");

      List<Agent> results = agentRepository.findAll(AgentSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(1)
          .first()
          .satisfies(a -> assertThat(a.getFirstName()).isEqualToIgnoringCase("alice"));
    }

    @Test
    @DisplayName("should return empty for unknown first name")
    void shouldReturnEmptyForUnknownFirstName() {
      AgentFilter filter = new AgentFilter();
      filter.setFirstName("Unknown");

      List<Agent> results = agentRepository.findAll(AgentSpecifications.buildSpecification(filter));

      assertThat(results).isEmpty();
    }
  }

  @Nested
  @DisplayName("Filter by last name")
  class FilterByLastName {

    @Test
    @DisplayName("should match case-insensitively")
    void shouldMatchCaseInsensitively() {
      AgentFilter filter = new AgentFilter();
      filter.setLastName("mwangi");

      List<Agent> results = agentRepository.findAll(AgentSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(1)
          .first()
          .satisfies(a -> assertThat(a.getLastName()).isEqualToIgnoringCase("mwangi"));
    }

    @Test
    @DisplayName("should return empty for unknown last name")
    void shouldReturnEmptyForUnknownLastName() {
      AgentFilter filter = new AgentFilter();
      filter.setLastName("Unknown");

      List<Agent> results = agentRepository.findAll(AgentSpecifications.buildSpecification(filter));

      assertThat(results).isEmpty();
    }
  }

  @Nested
  @DisplayName("Filter by national ID number")
  class FilterByNationalIdNumber {

    @Test
    @DisplayName("should return agent with matching national ID")
    void shouldReturnAgentWithMatchingNationalId() {
      AgentFilter filter = new AgentFilter();
      filter.setNationalIdNumber("NID-001");

      List<Agent> results = agentRepository.findAll(AgentSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(1)
          .first()
          .satisfies(a -> assertThat(a.getNationalIdNumber()).isEqualTo("NID-001"));
    }

    @Test
    @DisplayName("should return empty for unknown national ID")
    void shouldReturnEmptyForUnknownNationalId() {
      AgentFilter filter = new AgentFilter();
      filter.setNationalIdNumber("NID-999");

      List<Agent> results = agentRepository.findAll(AgentSpecifications.buildSpecification(filter));

      assertThat(results).isEmpty();
    }
  }

  @Nested
  @DisplayName("Filter by phone number")
  class FilterByPhoneNumber {

    @Test
    @DisplayName("should return agent with matching phone number")
    void shouldReturnAgentWithMatchingPhone() {
      AgentFilter filter = new AgentFilter();
      filter.setPhoneNumber("+254700000002");

      List<Agent> results = agentRepository.findAll(AgentSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(1)
          .first()
          .satisfies(a -> assertThat(a.getPhoneNumber()).isEqualTo("+254700000002"));
    }

    @Test
    @DisplayName("should return empty for unknown phone number")
    void shouldReturnEmptyForUnknownPhone() {
      AgentFilter filter = new AgentFilter();
      filter.setPhoneNumber("+254799999999");

      List<Agent> results = agentRepository.findAll(AgentSpecifications.buildSpecification(filter));

      assertThat(results).isEmpty();
    }
  }

  @Nested
  @DisplayName("Filter by reliability score")
  class FilterByReliabilityScore {

    @BeforeEach
    void seedScores() {
      bicycleRider.incrementCompletedDeliveries();
      bicycleRider.incrementCompletedDeliveries();
      bicycleRider.incrementCompletedDeliveries();
      bicycleRider.incrementCompletedDeliveries();
      bicycleRider.incrementCompletedDeliveries();
      bicycleRider.updateReliabilityScore(new BigDecimal("40.00"));
      agentRepository.save(bicycleRider);

      lightMotorRider.incrementCompletedDeliveries();
      lightMotorRider.incrementCompletedDeliveries();
      lightMotorRider.incrementCompletedDeliveries();
      lightMotorRider.incrementCompletedDeliveries();
      lightMotorRider.incrementCompletedDeliveries();
      lightMotorRider.updateReliabilityScore(new BigDecimal("70.00"));
      agentRepository.save(lightMotorRider);

      vehicleDriver.incrementCompletedDeliveries();
      vehicleDriver.incrementCompletedDeliveries();
      vehicleDriver.incrementCompletedDeliveries();
      vehicleDriver.incrementCompletedDeliveries();
      vehicleDriver.incrementCompletedDeliveries();
      vehicleDriver.updateReliabilityScore(new BigDecimal("90.00"));
      agentRepository.save(vehicleDriver);
    }

    @Test
    @DisplayName("should return agents with score >= min")
    void shouldReturnAgentsWithScoreAtLeastMin() {
      AgentFilter filter = new AgentFilter();
      filter.setReliabilityScoreMin(new BigDecimal("70.00"));

      List<Agent> results = agentRepository.findAll(AgentSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(2)
          .allMatch(a -> a.getReliabilityScore().compareTo(new BigDecimal("70.00")) >= 0);
    }

    @Test
    @DisplayName("should return agents with score <= max")
    void shouldReturnAgentsWithScoreAtMostMax() {
      AgentFilter filter = new AgentFilter();
      filter.setReliabilityScoreMax(new BigDecimal("70.00"));

      List<Agent> results = agentRepository.findAll(AgentSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(2)
          .allMatch(a -> a.getReliabilityScore().compareTo(new BigDecimal("70.00")) <= 0);
    }

    @Test
    @DisplayName("should return agents within min and max range")
    void shouldReturnAgentsWithinRange() {
      AgentFilter filter = new AgentFilter();
      filter.setReliabilityScoreMin(new BigDecimal("50.00"));
      filter.setReliabilityScoreMax(new BigDecimal("80.00"));

      List<Agent> results = agentRepository.findAll(AgentSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(1)
          .first()
          .satisfies(
              a ->
                  assertThat(a.getReliabilityScore())
                      .isEqualByComparingTo(new BigDecimal("70.00")));
    }
  }

  @Nested
  @DisplayName("Filter by active suspension")
  class FilterByActiveSuspension {

    @BeforeEach
    void seedSuspension() {
      suspensionRepository.save(
          Suspension.builder().agentId(bicycleRider.getId()).reason("Policy violation").build());
    }

    @Test
    @DisplayName("should return only agents with an active suspension when filter is true")
    void shouldReturnOnlySuspendedAgentsWhenTrue() {
      AgentFilter filter = new AgentFilter();
      filter.setHasActiveSuspension(true);

      List<Agent> results = agentRepository.findAll(AgentSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(1)
          .first()
          .satisfies(a -> assertThat(a.getId()).isEqualTo(bicycleRider.getId()));
    }

    @Test
    @DisplayName("should exclude agents with an active suspension when filter is false")
    void shouldExcludeSuspendedAgentsWhenFalse() {
      AgentFilter filter = new AgentFilter();
      filter.setHasActiveSuspension(false);

      List<Agent> results = agentRepository.findAll(AgentSpecifications.buildSpecification(filter));

      assertThat(results).hasSize(2).noneMatch(a -> a.getId().equals(bicycleRider.getId()));
    }

    @Test
    @DisplayName("should return all agents when filter is null")
    void shouldReturnAllAgentsWhenNull() {
      AgentFilter filter = new AgentFilter();

      List<Agent> results = agentRepository.findAll(AgentSpecifications.buildSpecification(filter));

      assertThat(results).hasSize(3);
    }
  }

  @Nested
  @DisplayName("Combined filters")
  class CombinedFilters {

    @Test
    @DisplayName("status and capability class combined returns correct subset")
    void statusAndCapabilityClassReturnsCorrectSubset() {
      AgentFilter filter = new AgentFilter();
      filter.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
      filter.setCapabilityClass(CapabilityClass.BICYCLE_RIDER);

      List<Agent> results = agentRepository.findAll(AgentSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(1)
          .first()
          .satisfies(
              a -> {
                assertThat(a.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
                assertThat(a.getCapabilityClass()).isEqualTo(CapabilityClass.BICYCLE_RIDER);
              });
    }

    @Test
    @DisplayName("first name and availability status combined returns correct subset")
    void firstNameAndStatusReturnsCorrectSubset() {
      AgentFilter filter = new AgentFilter();
      filter.setFirstName("Carol");
      filter.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);

      List<Agent> results = agentRepository.findAll(AgentSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(1)
          .first()
          .satisfies(a -> assertThat(a.getFirstName()).isEqualTo("Carol"));
    }
  }
}
