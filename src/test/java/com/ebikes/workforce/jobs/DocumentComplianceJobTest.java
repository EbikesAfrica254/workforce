package com.ebikes.workforce.jobs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.DocumentRepository;

@DisplayName("DocumentComplianceJob")
@ExtendWith(MockitoExtension.class)
class DocumentComplianceJobTest {

  @Mock private AgentRepository agentRepository;
  @Mock private DocumentRepository documentRepository;

  @InjectMocks private DocumentComplianceJob job;

  @Nested
  @DisplayName("checkExpiredDocuments")
  class CheckExpiredDocuments {

    @Test
    @DisplayName("should do nothing when no expired documents are found")
    void shouldDoNothingWhenNoExpiredDocumentsFound() {
      when(documentRepository.bulkExpireDocuments(any(LocalDate.class), any())).thenReturn(0);

      job.checkExpiredDocuments();

      verify(documentRepository, never()).findAgentIdsWithExpiredRequiredDocuments(any(), any());
      verify(agentRepository, never()).bulkMarkUnavailable(any(), any());
    }

    @Test
    @DisplayName("should bulk mark affected agents unavailable when expired documents found")
    void shouldBulkMarkAffectedAgentsUnavailableWhenExpiredDocumentsFound() {
      UUID agentId = UUID.randomUUID();

      when(documentRepository.bulkExpireDocuments(any(LocalDate.class), any())).thenReturn(1);
      when(documentRepository.findAgentIdsWithExpiredRequiredDocuments(any(LocalDate.class), any()))
          .thenReturn(Set.of(agentId));
      when(agentRepository.bulkMarkUnavailable(any(), any())).thenReturn(1);

      job.checkExpiredDocuments();

      verify(agentRepository).bulkMarkUnavailable(eq(Set.of(agentId)), any());
    }
  }
}
