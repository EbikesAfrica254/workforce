package com.ebikes.workforce.support.fixtures;

import java.util.UUID;

import org.springframework.test.util.ReflectionTestUtils;

import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.enums.NationalIdType;

public final class AgentFixtures {

  public static final UUID AGENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
  public static final String USER_ID = "00000000-0000-0000-0000-000000000011";
  public static final String PHONE_NUMBER = "+254700000010";
  public static final String NATIONAL_ID_NUMBER = "12345678";

  private AgentFixtures() {}

  public static Agent available() {
    return base(AGENT_ID, CapabilityClass.BICYCLE_RIDER, AvailabilityStatus.AVAILABLE);
  }

  public static Agent deactivated() {
    return base(AGENT_ID, CapabilityClass.BICYCLE_RIDER, AvailabilityStatus.DEACTIVATED);
  }

  public static Agent forPersistence(
      String nationalId,
      String phone,
      String userId,
      CapabilityClass capabilityClass,
      AvailabilityStatus status) {
    return Agent.builder()
        .availabilityStatus(status)
        .capabilityClass(capabilityClass)
        .firstName("Test")
        .lastName("Agent")
        .maxConcurrentOrders(capabilityClass.getDefaultMaxConcurrentOrders())
        .nationalIdNumber(nationalId)
        .nationalIdType(NationalIdType.NATIONAL_ID)
        .phoneNumber(phone)
        .userId(userId)
        .build();
  }

  public static Agent offline() {
    return base(AGENT_ID, CapabilityClass.BICYCLE_RIDER, AvailabilityStatus.OFFLINE);
  }

  public static Agent pending() {
    return base(AGENT_ID, CapabilityClass.BICYCLE_RIDER, AvailabilityStatus.PENDING);
  }

  public static Agent suspended() {
    return base(AGENT_ID, CapabilityClass.BICYCLE_RIDER, AvailabilityStatus.SUSPENDED);
  }

  public static Agent withCapabilityClass(CapabilityClass capabilityClass) {
    return base(AGENT_ID, capabilityClass, AvailabilityStatus.AVAILABLE);
  }

  public static Agent withCurrentOrders(int currentOrders) {
    Agent agent = available();
    for (int i = 0; i < currentOrders; i++) {
      agent.incrementCurrentOrders();
    }
    return agent;
  }

  public static Agent withDeliveryMetrics(
      int completedDeliveries, int onTimeDeliveries, int currentOrders) {
    Agent agent = available();
    for (int i = 0; i < completedDeliveries; i++) {
      agent.incrementCompletedDeliveries();
    }
    for (int i = 0; i < onTimeDeliveries; i++) {
      agent.incrementOnTimeDeliveries();
    }
    for (int i = 0; i < currentOrders; i++) {
      agent.incrementCurrentOrders();
    }
    return agent;
  }

  public static Agent withId(UUID id) {
    return base(id, CapabilityClass.BICYCLE_RIDER, AvailabilityStatus.AVAILABLE);
  }

  public static Agent withIdAndStatus(UUID id, AvailabilityStatus status) {
    return base(id, CapabilityClass.BICYCLE_RIDER, status);
  }

  private static Agent base(UUID id, CapabilityClass capabilityClass, AvailabilityStatus status) {
    Agent agent =
        Agent.builder()
            .availabilityStatus(status)
            .capabilityClass(capabilityClass)
            .firstName("Test")
            .lastName("Agent")
            .maxConcurrentOrders(capabilityClass.getDefaultMaxConcurrentOrders())
            .nationalIdNumber(NATIONAL_ID_NUMBER)
            .nationalIdType(NationalIdType.NATIONAL_ID)
            .phoneNumber(PHONE_NUMBER)
            .userId(USER_ID)
            .build();
    ReflectionTestUtils.setField(agent, "id", id);
    return agent;
  }
}
