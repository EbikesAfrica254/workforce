package com.ebikes.workforce.support.fixtures;

import java.util.UUID;

import com.ebikes.workforce.database.entities.PaymentMethod;

public final class PaymentMethodFixtures {

  public static final UUID PAYMENT_METHOD_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000030");

  private PaymentMethodFixtures() {}

  public static PaymentMethod mpesaPaybill(UUID agentId) {
    return PaymentMethod.mpesaPaybill(agentId, "Test Business", "ACC-001", "123456");
  }

  public static PaymentMethod mpesaPersonal(UUID agentId) {
    return PaymentMethod.mpesaPersonal(agentId, "Test Agent", SecurityFixtures.TEST_PHONE_NUMBER);
  }

  public static PaymentMethod mpesaTill(UUID agentId) {
    return PaymentMethod.mpesaTill(agentId, "Test Till", "987654");
  }

  public static PaymentMethod primary(UUID agentId) {
    PaymentMethod pm = mpesaPersonal(agentId);
    pm.markPrimary();
    return pm;
  }
}
