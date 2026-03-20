package com.ebikes.workforce.publishers;

import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.ebikes.workforce.constants.MDCKeys;
import com.ebikes.workforce.dtos.events.outgoing.AuditEvent;
import com.ebikes.workforce.enums.AuditOutcome;
import com.ebikes.workforce.services.events.OutboxService;
import com.ebikes.workforce.support.context.ExecutionContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventPublisher {

  private final OutboxService outboxService;

  public void publishSuccess(
      UUID entityId,
      String entityType,
      String eventType,
      Map<String, String> metadata,
      String routingKey) {

    var event =
        new AuditEvent(
            entityId,
            entityType,
            eventType,
            null,
            MDC.get(MDCKeys.IP_ADDRESS),
            metadata,
            ExecutionContext.getActiveOrganization(),
            AuditOutcome.SUCCESS,
            null,
            null,
            ExecutionContext.getUserId());

    outboxService.save(eventType, event, routingKey);
  }

  public void publishSuccess(
      UUID entityId,
      String entityType,
      String eventType,
      Map<String, String> metadata,
      String organizationId,
      String routingKey) {

    var event =
        new AuditEvent(
            entityId,
            entityType,
            eventType,
            null,
            MDC.get(MDCKeys.IP_ADDRESS),
            metadata,
            organizationId,
            AuditOutcome.SUCCESS,
            null,
            null,
            ExecutionContext.getUserId());

    outboxService.save(eventType, event, routingKey);
  }
}
