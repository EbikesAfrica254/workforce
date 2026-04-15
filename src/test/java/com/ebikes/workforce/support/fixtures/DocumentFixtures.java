package com.ebikes.workforce.support.fixtures;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.test.util.ReflectionTestUtils;

import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.Document;
import com.ebikes.workforce.enums.DocumentType;

public final class DocumentFixtures {

  public static final UUID DOCUMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

  private DocumentFixtures() {}

  public static Document activeExpired(Agent agent, DocumentType type) {
    return active(agent, type, LocalDate.now().minusDays(1));
  }

  public static Document activeValid(Agent agent, DocumentType type) {
    return active(agent, type, LocalDate.now().plusYears(1));
  }

  public static Document pending(Agent agent, DocumentType type) {
    Document document = base(type);
    document.associate(agent);
    return document;
  }

  public static Document uploaded(Agent agent, DocumentType type) {
    Document document = base(type);
    document.markUploaded(1024L, "application/pdf", LocalDate.now().plusYears(1));
    document.associate(agent);
    return document;
  }

  private static Document active(Agent agent, DocumentType type, LocalDate expiryDate) {
    Document document = base(type);
    document.markUploaded(1024L, "application/pdf", expiryDate);
    document.activate();
    document.associate(agent);
    return document;
  }

  private static Document base(DocumentType type) {
    Document document = Document.create(type, "test-file.pdf", "application/pdf");
    ReflectionTestUtils.setField(document, "id", DOCUMENT_ID);
    return document;
  }
}
