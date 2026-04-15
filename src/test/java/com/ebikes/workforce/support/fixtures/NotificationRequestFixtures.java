package com.ebikes.workforce.support.fixtures;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

import com.ebikes.workforce.constants.EventConstants.Source;
import com.ebikes.workforce.dtos.events.outgoing.NotificationRequest;
import com.ebikes.workforce.enums.ChannelType;
import com.ebikes.workforce.enums.NotificationCategory;

import net.datafaker.Faker;

public final class NotificationRequestFixtures {

  private static final Faker FAKER = new Faker();

  private NotificationRequestFixtures() {}

  public static NotificationRequest accountVerification() {
    return base(
        ChannelType.EMAIL,
        "iam.user-extension.activation-requested",
        "ACCOUNT_VERIFICATION",
        Map.of("name", FAKER.name().firstName()));
  }

  private static NotificationRequest base(
      ChannelType channel,
      String eventType,
      String templateName,
      Map<String, Serializable> variables) {
    return new NotificationRequest(
        null,
        NotificationCategory.OPERATIONAL,
        channel,
        eventType,
        UUID.randomUUID().toString(),
        FAKER.internet().emailAddress(),
        Source.serviceReference(),
        UUID.randomUUID().toString(),
        templateName,
        null,
        variables);
  }
}
