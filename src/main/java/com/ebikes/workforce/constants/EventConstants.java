package com.ebikes.workforce.constants;

import java.util.Set;

import com.ebikes.workforce.support.references.ReferenceGenerator;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class EventConstants {

  public static final class Source {
    private Source() {
      // prevent instantiation
    }

    public static final String HOST_SERVICE = "workforce";

    public static String serviceReference() {
      return ReferenceGenerator.generateServiceReference(HOST_SERVICE);
    }
  }

  @UtilityClass
  public static final class DomainEvents {

    @UtilityClass
    public static final class Agent {
      public static final String APPROVED = Source.HOST_SERVICE + ".agent.approved";
      public static final String AVAILABILITY_CHANGED =
          Source.HOST_SERVICE + ".agent.availability-changed";
      public static final String CREATED = Source.HOST_SERVICE + ".agent.created";
      public static final String DEACTIVATED = Source.HOST_SERVICE + ".agent.deactivated";
      public static final String LOCATION_UPDATED = Source.HOST_SERVICE + ".agent.location-updated";
      public static final String REJECTED = Source.HOST_SERVICE + ".agent.rejected";
      public static final String RESUBMITTED = Source.HOST_SERVICE + ".agent.resubmitted";
      public static final String SUSPENDED = Source.HOST_SERVICE + ".agent.suspended";
      public static final String SUSPENSION_LIFTED =
          Source.HOST_SERVICE + ".agent.suspension-lifted";
      public static final String UPDATE_APPROVED = Source.HOST_SERVICE + ".agent.update-approved";
    }

    @UtilityClass
    public static final class Certifications {
      public static final String CREATED = Source.HOST_SERVICE + ".certification.created";
    }

    @UtilityClass
    public static final class Documents {
      public static final String REPLACED = Source.HOST_SERVICE + ".document.replaced";
    }

    @UtilityClass
    public static final class PaymentMethod {
      public static final String CREATED = Source.HOST_SERVICE + ".payment-method.created";
      public static final String DELETED = Source.HOST_SERVICE + ".payment-method.deleted";
      public static final String PRIMARY_SET = Source.HOST_SERVICE + ".payment-method.primary-set";
      public static final String UPDATED = Source.HOST_SERVICE + ".payment-method.updated";
    }

    @UtilityClass
    public static final class PreferredAgent {
      public static final String CREATED = Source.HOST_SERVICE + ".preferred-agent.created";
      public static final String DELETED = Source.HOST_SERVICE + ".preferred-agent.deleted";
      public static final String UPDATED = Source.HOST_SERVICE + ".preferred-agent.updated";
    }

    @UtilityClass
    public static final class Shortlist {
      public static final String EMPTY = Source.HOST_SERVICE + ".shortlist.empty";
      public static final String RESOLVED = Source.HOST_SERVICE + ".shortlist.resolved";
    }
  }

  @UtilityClass
  public static final class ExternalContracts {

    public static final Set<String> ORDER_STATUSES_REQUIRING_AGENT_DECREMENT =
        Set.of("ASSIGNED", "PENDING_REASSIGNMENT");

    public static final String ASSIGNMENTS_ASSIGNMENT_COMPLETED =
        "assignments.assignment.completed";
    public static final String MAKER_CHECKER_DOCUMENT_PREFIX = "maker-checker.document.";
    public static final String MAKER_CHECKER_WORKFORCE_PREFIX = "maker-checker.workforce.";
    public static final String ORDERS_ORDER_CANCELLED = "orders.order.cancelled";
    public static final String ORDERS_ORDER_DELIVERED = "orders.order.delivered";
    public static final String ORDERS_ORDER_PENDING_ASSIGNMENT = "orders.order.pending-assignment";
    public static final String ORDERS_ORDER_REASSIGNMENT_REQUESTED =
        "orders.order.reassignment-requested";
  }

  @UtilityClass
  public static final class RoutingKeys {
    // outbound audit
    public static final String WORKFORCE_AGENT_AUDIT = audit(Source.HOST_SERVICE + ".agent");
    public static final String WORKFORCE_CERTIFICATION_AUDIT =
        audit(Source.HOST_SERVICE + ".certification");
    public static final String WORKFORCE_DOCUMENT_AUDIT = audit(Source.HOST_SERVICE + ".document");
    public static final String WORKFORCE_PAYMENT_METHOD_AUDIT =
        audit(Source.HOST_SERVICE + ".payment-method");
    public static final String WORKFORCE_PREFERRED_AGENT_AUDIT =
        audit(Source.HOST_SERVICE + ".preferred-agent");
    public static final String WORKFORCE_SUSPENSION_AUDIT =
        audit(Source.HOST_SERVICE + ".suspension");

    // outbound domain events
    public static final String WORKFORCE_AGENT_AVAILABILITY_CHANGED =
        DomainEvents.Agent.AVAILABILITY_CHANGED;
    public static final String WORKFORCE_LOCATION_UPDATED = DomainEvents.Agent.LOCATION_UPDATED;
    public static final String WORKFORCE_SHORTLIST_EMPTY = DomainEvents.Shortlist.EMPTY;
    public static final String WORKFORCE_SHORTLIST_RESOLVED = DomainEvents.Shortlist.RESOLVED;

    public static final String NOTIFICATIONS_EMAIL = notifications("email");
    public static final String NOTIFICATIONS_SMS = notifications("sms");
    public static final String NOTIFICATIONS_SSE = notifications("sse");
    public static final String NOTIFICATIONS_WHATSAPP = notifications("whatsapp");

    public static String notifications(String channel) {
      return "notifications." + channel.toLowerCase();
    }

    public static String audit(String domain) {
      return domain + ".audit";
    }
  }
}
