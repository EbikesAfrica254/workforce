package com.ebikes.workforce.support.fixtures;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.ebikes.workforce.constants.EventConstants.Source;
import com.ebikes.workforce.dtos.events.incoming.AssignmentCompletedEvent;
import com.ebikes.workforce.dtos.events.incoming.OrderCancelledEvent;
import com.ebikes.workforce.dtos.events.incoming.OrderPendingAssignmentEvent;
import com.ebikes.workforce.dtos.events.incoming.OrderReassignmentRequestedEvent;
import com.ebikes.workforce.dtos.events.outgoing.AgentShortlistEmptyEvent;
import com.ebikes.workforce.dtos.events.outgoing.AgentShortlistResolvedEvent;
import com.ebikes.workforce.dtos.internal.ShortlistRequest;
import com.ebikes.workforce.enums.VehicleClass;

import net.datafaker.Faker;

public final class EventFixtures {

  private static final Faker FAKER = new Faker();

  private EventFixtures() {}

  public static AgentShortlistEmptyEvent agentShortlistEmpty() {
    return new AgentShortlistEmptyEvent(
        FAKER.internet().uuid(),
        UUID.randomUUID(),
        FAKER.internet().uuid(),
        FAKER.address().zipCode(),
        OffsetDateTime.now(),
        FAKER.internet().uuid(),
        VehicleClass.BICYCLE);
  }

  public static AgentShortlistResolvedEvent agentShortlistResolved() {
    return new AgentShortlistResolvedEvent(
        FAKER.internet().uuid(),
        List.of(
            new AgentShortlistResolvedEvent.Candidate(
                FAKER.internet().uuid(),
                FAKER.bool().bool(),
                new BigDecimal(FAKER.address().latitude()),
                new BigDecimal(FAKER.address().longitude()),
                VehicleClass.BICYCLE),
            new AgentShortlistResolvedEvent.Candidate(
                FAKER.internet().uuid(),
                FAKER.bool().bool(),
                new BigDecimal(FAKER.address().latitude()),
                new BigDecimal(FAKER.address().longitude()),
                VehicleClass.BICYCLE)),
        OffsetDateTime.now().plusHours(1),
        UUID.randomUUID(),
        FAKER.internet().uuid(),
        OffsetDateTime.now(),
        Source.serviceReference());
  }

  public static AssignmentCompletedEvent assignmentCompleted() {
    return new AssignmentCompletedEvent(
        UUID.randomUUID(),
        UUID.randomUUID(),
        null,
        Source.serviceReference(),
        AgentFixtures.AGENT_ID);
  }

  public static AssignmentCompletedEvent assignmentCompleted(UUID agentId) {
    return new AssignmentCompletedEvent(
        UUID.randomUUID(), UUID.randomUUID(), null, Source.serviceReference(), agentId);
  }

  public static OrderCancelledEvent orderCancelledEvent(
      UUID agentId, String cancelledFromStatus, UUID orderId) {
    return new OrderCancelledEvent(
        agentId, cancelledFromStatus, orderId, Source.serviceReference());
  }

  public static OrderPendingAssignmentEvent orderPendingAssignment() {
    return new OrderPendingAssignmentEvent(
        null,
        UUID.randomUUID(),
        UUID.randomUUID(),
        SecurityFixtures.TEST_ORGANIZATION_ID,
        new BigDecimal("-1.286389"),
        new BigDecimal("36.817223"),
        Source.serviceReference(),
        VehicleClass.BICYCLE);
  }

  public static OrderReassignmentRequestedEvent orderReassignmentRequested() {
    return new OrderReassignmentRequestedEvent(
        null,
        UUID.randomUUID(),
        UUID.randomUUID(),
        SecurityFixtures.TEST_ORGANIZATION_ID,
        new BigDecimal("-1.286389"),
        new BigDecimal("36.817223"),
        "Agent unavailable",
        UUID.randomUUID().toString(),
        VehicleClass.BICYCLE);
  }

  public static ShortlistRequest shortlistRequest() {
    return new ShortlistRequest(
        UUID.randomUUID(),
        SecurityFixtures.TEST_ORGANIZATION_ID,
        null,
        new BigDecimal("-1.286389"),
        new BigDecimal("36.817223"),
        VehicleClass.BICYCLE);
  }
}
