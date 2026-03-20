package com.ebikes.workforce.support.audit;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.Certification;
import com.ebikes.workforce.database.entities.PaymentMethod;
import com.ebikes.workforce.database.entities.PreferredAgent;
import com.ebikes.workforce.database.entities.Suspension;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AuditMetadataBuilder {

  private static final String AGENT_ID = "agentId";

  public static Map<String, String> forAgent(Agent agent) {
    return Map.of(
        AGENT_ID,
        agent.getId().toString(),
        "availabilityStatus",
        agent.getAvailabilityStatus().name(),
        "capabilityClass",
        agent.getCapabilityClass().name(),
        "phoneNumber",
        agent.getPhoneNumber(),
        "userId",
        agent.getUserId());
  }

  public static Map<String, String> forAgent(Agent agent, Map<String, String> extra) {
    Map<String, String> metadata = new HashMap<>(forAgent(agent));
    metadata.putAll(extra);
    return Collections.unmodifiableMap(metadata);
  }

  public static Map<String, String> forCertification(Certification certification) {
    Map<String, String> metadata = new HashMap<>();
    metadata.put(AGENT_ID, certification.getAgentId().toString());
    metadata.put("certificationId", certification.getId().toString());
    metadata.put("certificationType", certification.getCertificationType().name());
    metadata.put("issuedAt", certification.getIssuedAt().toString());
    if (certification.getExpiresAt() != null) {
      metadata.put("expiresAt", certification.getExpiresAt().toString());
    }
    if (certification.getReferenceNumber() != null) {
      metadata.put("referenceNumber", certification.getReferenceNumber());
    }
    return Collections.unmodifiableMap(metadata);
  }

  public static Map<String, String> forPaymentMethod(PaymentMethod paymentMethod) {
    return Map.of(
        AGENT_ID,
        paymentMethod.getAgentId().toString(),
        "isPrimary",
        String.valueOf(paymentMethod.getIsPrimary()),
        "paymentMethodId",
        paymentMethod.getId().toString(),
        "paymentMethodType",
        paymentMethod.getPaymentMethodType().name());
  }

  public static Map<String, String> forPaymentMethod(
      PaymentMethod paymentMethod, Map<String, String> extra) {
    Map<String, String> metadata = new HashMap<>(forPaymentMethod(paymentMethod));
    metadata.putAll(extra);
    return Collections.unmodifiableMap(metadata);
  }

  public static Map<String, String> forPreferredAgent(PreferredAgent preferredAgent) {
    Map<String, String> metadata = new HashMap<>();
    metadata.put(AGENT_ID, preferredAgent.getAgentId().toString());
    metadata.put("organizationId", preferredAgent.getOrganizationId());
    metadata.put("preferredAgentId", preferredAgent.getId().toString());
    metadata.put("priority", String.valueOf(preferredAgent.getPriority()));
    if (preferredAgent.getBranchId() != null) {
      metadata.put("branchId", preferredAgent.getBranchId());
    }
    return Collections.unmodifiableMap(metadata);
  }

  public static Map<String, String> forSuspension(Suspension suspension) {
    Map<String, String> metadata = new HashMap<>();
    metadata.put(AGENT_ID, suspension.getAgentId().toString());
    metadata.put("isActive", String.valueOf(suspension.isActive()));
    metadata.put("reason", suspension.getReason());
    metadata.put("suspensionId", suspension.getId().toString());
    if (suspension.getExpiresAt() != null) {
      metadata.put("expiresAt", suspension.getExpiresAt().toString());
    }
    if (suspension.getLiftedAt() != null) {
      metadata.put("liftedAt", suspension.getLiftedAt().toString());
    }
    if (suspension.getLiftedBy() != null) {
      metadata.put("liftedBy", suspension.getLiftedBy());
    }
    return Collections.unmodifiableMap(metadata);
  }

  public static Map<String, String> forSuspension(
      Suspension suspension, Map<String, String> extra) {
    Map<String, String> metadata = new HashMap<>(forSuspension(suspension));
    metadata.putAll(extra);
    return Collections.unmodifiableMap(metadata);
  }
}
