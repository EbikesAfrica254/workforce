package com.ebikes.workforce.integration.services.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ebikes.workforce.database.entities.Inbox;
import com.ebikes.workforce.database.repositories.InboxRepository;
import com.ebikes.workforce.services.events.InboxService;
import com.ebikes.workforce.support.infrastructure.AbstractIntegrationTest;

class InboxServiceIT extends AbstractIntegrationTest {

  @Autowired private InboxService inboxService;
  @Autowired private InboxRepository inboxRepository;

  @BeforeEach
  void setUp() {
    inboxRepository.deleteAll();
  }

  @Nested
  class Receive {

    @Test
    @DisplayName("returns true and persists record for new event")
    void returnsTrueAndPersistsRecordForNewEvent() {
      boolean result =
          inboxService.receive("maker-checker.agent.approved", "ref-001", "maker-checker");

      assertThat(result).isTrue();
      assertThat(inboxRepository.findAll())
          .hasSize(1)
          .first()
          .satisfies(
              inbox -> {
                assertThat(inbox.getEventType()).isEqualTo("maker-checker.agent.approved");
                assertThat(inbox.getSourceContext()).isEqualTo("maker-checker");
                assertThat(inbox.getReceivedAt()).isNotNull();
                assertThat(inbox.getProcessedAt()).isNull();
              });
    }

    @Test
    @DisplayName("returns false for duplicate services reference")
    void returnsFalseForDuplicateServiceReference() {
      inboxService.receive("maker-checker.agent.approved", "ref-001", "maker-checker");

      boolean result =
          inboxService.receive("maker-checker.agent.approved", "ref-001", "maker-checker");

      assertThat(result).isFalse();
      assertThat(inboxRepository.findAll()).hasSize(1);
    }
  }

  @Nested
  class MarkProcessed {

    @Test
    @DisplayName("sets processedAt")
    void setsProcessedAt() {
      inboxRepository.save(new Inbox("maker-checker.agent.approved", "ref-001", "maker-checker"));

      inboxService.markProcessed("ref-001");

      assertThat(inboxRepository.findById("ref-001"))
          .isPresent()
          .get()
          .satisfies(inbox -> assertThat(inbox.getProcessedAt()).isNotNull());
    }

    @Test
    @DisplayName("throws when already processed")
    void throwsWhenAlreadyProcessed() {
      Inbox inbox = new Inbox("maker-checker.agent.approved", "ref-001", "maker-checker");
      inbox.markProcessed();
      inboxRepository.save(inbox);

      assertThatThrownBy(() -> inboxService.markProcessed("ref-001"))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("throws when services reference not found")
    void throwsWhenServiceReferenceNotFound() {
      assertThatThrownBy(() -> inboxService.markProcessed("non-existent"))
          .isInstanceOf(IllegalStateException.class);
    }
  }
}
