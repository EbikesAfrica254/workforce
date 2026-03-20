package com.ebikes.workforce.publishers;

import com.ebikes.workforce.configurations.properties.NotificationProperties;
import org.springframework.stereotype.Component;

import com.ebikes.workforce.constants.EventConstants.RoutingKeys;
import com.ebikes.workforce.dtos.events.outgoing.NotificationRequest;
import com.ebikes.workforce.enums.ChannelType;
import com.ebikes.workforce.services.events.OutboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisher {

  private final NotificationProperties properties;
  private final OutboxService outboxService;

  public void publish(NotificationRequest request) {
    String routingKey = resolveRoutingKey(request.channel());

    if (properties.isEnabled()){
      outboxService.save(request.eventType(), request, routingKey);
    } else {
      log.info("Notification event skipped - notifications are disabled");
    }

    log.info(
        "Notification event queued - eventType={} channel={} recipient={}",
        request.eventType(),
        request.channel(),
        request.recipient());
  }

  private String resolveRoutingKey(ChannelType channel) {
    return switch (channel) {
      case EMAIL -> RoutingKeys.NOTIFICATIONS_EMAIL;
      case SMS -> RoutingKeys.NOTIFICATIONS_SMS;
      case SSE -> RoutingKeys.NOTIFICATIONS_SSE;
      case WHATSAPP -> RoutingKeys.NOTIFICATIONS_WHATSAPP;
    };
  }
}
