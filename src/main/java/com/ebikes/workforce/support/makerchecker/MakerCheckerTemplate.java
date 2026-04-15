package com.ebikes.workforce.support.makerchecker;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.ebikes.workforce.constants.EventConstants.Source;
import com.ebikes.workforce.database.entities.bases.BaseEntity;
import com.ebikes.workforce.dtos.events.outgoing.MakerCheckerRequest;
import com.ebikes.workforce.dtos.internal.FieldChange;
import com.ebikes.workforce.publishers.MakerCheckerRequestPublisher;
import com.ebikes.workforce.support.context.ExecutionContext;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MakerCheckerTemplate {

  private static final String OPERATION = "operation";

  private final MakerCheckerRequestPublisher makerCheckerRequestPublisher;

  public <T extends BaseEntity> void publish(
      T entity, String organizationId, String operation, List<FieldChange> changes) {
    publish(entity, organizationId, operation, changes, Map.of());
  }

  public <T extends BaseEntity> void publish(
      T entity,
      String organizationId,
      String operation,
      List<FieldChange> changes,
      Map<String, Object> extraContext) {

    String entityType = entity.getClass().getSimpleName().toUpperCase();

    Map<String, Object> operationContext = new HashMap<>(extraContext);
    operationContext.put(OPERATION, operation);

    MakerCheckerRequest request =
        new MakerCheckerRequest(
            null,
            changes,
            entity.getId(),
            entityType,
            ExecutionContext.getUserId(),
            operationContext,
            organizationId,
            Instant.now(),
            null);

    makerCheckerRequestPublisher.publish(request, buildRoutingKey(entityType));
  }

  private String buildRoutingKey(String entityType) {
    return Source.HOST_SERVICE + "." + entityType.toLowerCase() + ".maker-checker-request";
  }
}
