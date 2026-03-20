package com.ebikes.workforce.constants;

import com.ebikes.workforce.support.references.ReferenceGenerator;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EventConstants {

  @UtilityClass
  public static final class EventSource {
    public static final String ASSIGNMENTS = "assignments";
    public static final String HOST = "workforce";
    public static final String NOTIFICATIONS = "notifications";
    public static final String ORDERS = "orders";

    public static String serviceReference() {
      return ReferenceGenerator.generateServiceReference(HOST);
    }
  }

  @UtilityClass
  public static final class EventTypes {

    @UtilityClass
    public static final class Assignments {
      public static final String ASSIGNMENT_SUCCEEDED = "assignment.succeeded";
    }

    @UtilityClass
    public static final class Certifications {
      public static final String CREATED = EventSource.HOST + ".certification.created";
    }

    @UtilityClass
    public static final class Documents {
      public static final String REPLACED = EventSource.HOST + ".document.replaced";
    }

    @UtilityClass
    public static final class Orders {
      public static final String ORDER_CANCELLED = "order.cancelled";
      public static final String ORDER_DELIVERED = "order.delivered";
      public static final String ORDER_PENDING_ASSIGNMENT = "orders.order.pending-assignment";
      public static final String ORDER_REASSIGNMENT_REQUESTED =
          "orders.order.reassignment-requested";
    }

    @UtilityClass
    public static final class Workforce {
      public static final String AGENT_APPROVED = EventSource.HOST + ".agent.approved";
      public static final String AGENT_AVAILABILITY_CHANGED =
          EventSource.HOST + ".agent.availability-changed";
      public static final String AGENT_CREATED = EventSource.HOST + ".agent.created";
      public static final String AGENT_DEACTIVATED = EventSource.HOST + ".agent.deactivated";
      public static final String AGENT_LOCATION_UPDATED =
          EventSource.HOST + ".agent.location-updated";
      public static final String AGENT_REJECTED = EventSource.HOST + ".agent.rejected";
      public static final String AGENT_RESUBMITTED = EventSource.HOST + ".agent.resubmitted";
      public static final String AGENT_SHORTLIST_EMPTY =
          EventSource.HOST + ".agent_shortlist.empty";
      public static final String AGENT_SHORTLIST_RESOLVED =
          EventSource.HOST + ".agent_shortlist.resolved";
      public static final String AGENT_SUSPENDED = EventSource.HOST + ".agent.suspended";
      public static final String AGENT_SUSPENSION_LIFTED =
          EventSource.HOST + ".agent.suspension-lifted";
      public static final String AGENT_UPDATED = EventSource.HOST + ".agent.updated";
    }
  }

  @UtilityClass
  public static final class MessageHeaders {
    public static final String EVENT_TYPE = "eventType";
    public static final String OUTBOX_ID = "outboxId";
    public static final String ROUTING_KEY = "routingKey";
  }

  @UtilityClass
  public static final class RoutingKeys {

    // outbound — audit
    public static final String WORKFORCE_AGENT_AUDIT = audit(EventSource.HOST + ".agent");
    public static final String WORKFORCE_CERTIFICATION_AUDIT =
        audit(EventSource.HOST + ".certification");
    public static final String WORKFORCE_DOCUMENT_AUDIT = audit(EventSource.HOST + ".document");
    public static final String WORKFORCE_PAYMENT_METHOD_AUDIT =
        audit(EventSource.HOST + ".payment-method");
    public static final String WORKFORCE_PREFERRED_AGENT_AUDIT =
        audit(EventSource.HOST + ".preferred-agent");
    public static final String WORKFORCE_SUSPENSION_AUDIT = audit(EventSource.HOST + ".suspension");

    // outbound — domain events
    public static final String WORKFORCE_AGENT_AVAILABILITY_CHANGED =
        EventTypes.Workforce.AGENT_AVAILABILITY_CHANGED;
    public static final String WORKFORCE_AGENT_LOCATION_UPDATED =
        EventTypes.Workforce.AGENT_LOCATION_UPDATED;
    public static final String WORKFORCE_SHORTLIST_EMPTY =
        EventTypes.Workforce.AGENT_SHORTLIST_EMPTY;
    public static final String WORKFORCE_SHORTLIST_RESOLVED =
        EventTypes.Workforce.AGENT_SHORTLIST_RESOLVED;

    // inbound
    public static final String ASSIGNMENTS_ASSIGNMENT_SUCCEEDED =
        EventSource.ASSIGNMENTS + "." + EventTypes.Assignments.ASSIGNMENT_SUCCEEDED;
    public static final String ORDERS_ORDER_CANCELLED =
        EventSource.ORDERS + "." + EventTypes.Orders.ORDER_CANCELLED;
    public static final String ORDERS_ORDER_DELIVERED =
        EventSource.ORDERS + "." + EventTypes.Orders.ORDER_DELIVERED;
    public static final String ORDERS_ORDER_PENDING_ASSIGNMENT =
        EventSource.ORDERS + "." + EventTypes.Orders.ORDER_PENDING_ASSIGNMENT;
    public static final String ORDERS_ORDER_REASSIGNMENT_REQUESTED =
        EventSource.ORDERS + "." + EventTypes.Orders.ORDER_REASSIGNMENT_REQUESTED;

    // notifications
    public static final String NOTIFICATIONS_EMAIL = notifications("email");
    public static final String NOTIFICATIONS_SMS = notifications("sms");
    public static final String NOTIFICATIONS_SSE = notifications("sse");
    public static final String NOTIFICATIONS_WHATSAPP = notifications("whatsapp");

    public static String audit(String domain) {
      return domain + ".audit";
    }

    public static String makerCheckerRequest(String sourceService, String entityType) {
      return sourceService
          + "."
          + entityType.toLowerCase().replace("_", "-")
          + ".maker-checker-request";
    }

    public static String notifications(String channel) {
      return EventSource.NOTIFICATIONS + "." + channel.toLowerCase();
    }
  }
}
