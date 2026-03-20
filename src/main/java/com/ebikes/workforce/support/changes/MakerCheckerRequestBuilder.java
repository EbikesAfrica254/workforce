package com.ebikes.workforce.support.changes;

import java.util.List;
import java.util.Map;

import com.ebikes.workforce.constants.EventConstants.EventSource;
import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.Document;
import com.ebikes.workforce.dtos.events.outgoing.MakerCheckerRequest;
import com.ebikes.workforce.dtos.internal.FieldChange;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MakerCheckerRequestBuilder {

  private static final String ENTITY_TYPE_DOCUMENT = "DOCUMENT";
  private static final String ENTITY_TYPE_AGENT = "AGENT";
  private static final String OPERATION_LABEL = "operation";

  public static MakerCheckerRequest forDocumentReplacement(
      Document newDocument, Document oldDocument, String makerId) {
    return new MakerCheckerRequest(
        null,
        List.of(),
        newDocument.getId(),
        ENTITY_TYPE_DOCUMENT,
        makerId,
        Map.of(
            "documentType",
            oldDocument.getDocumentType(),
            "oldDocumentId",
            oldDocument.getId(),
            OPERATION_LABEL,
            "REPLACE_DOCUMENT"),
        oldDocument.getAgent().getId().toString(),
        null,
        EventSource.serviceReference());
  }

  public static MakerCheckerRequest forAgentCreate(
      Agent agent, List<FieldChange> changes, String makerId) {
    return new MakerCheckerRequest(
        null,
        changes,
        agent.getId(),
        ENTITY_TYPE_AGENT,
        makerId,
        Map.of(OPERATION_LABEL, "CREATE"),
        agent.getId().toString(),
        null,
        EventSource.serviceReference());
  }
}
