package com.ebikes.workforce.services.agents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.projections.AgentConflictCheck;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.dtos.internal.FieldChange;
import com.ebikes.workforce.dtos.requests.agents.CreateAgentRequest;
import com.ebikes.workforce.dtos.requests.agents.UpdateAgentRequest;
import com.ebikes.workforce.dtos.requests.documents.DocumentUploadInfo;
import com.ebikes.workforce.dtos.requests.filters.AgentFilter;
import com.ebikes.workforce.dtos.responses.agents.AgentDetailResponse;
import com.ebikes.workforce.dtos.responses.agents.AgentSummaryResponse;
import com.ebikes.workforce.dtos.responses.api.PaginatedResponse;
import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.enums.NationalIdType;
import com.ebikes.workforce.enums.ResponseCode;
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.exceptions.DuplicateResourceException;
import com.ebikes.workforce.exceptions.ResourceNotFoundException;
import com.ebikes.workforce.mappers.AgentMapper;
import com.ebikes.workforce.services.agents.document.DocumentService;
import com.ebikes.workforce.support.audit.AuditTemplate;
import com.ebikes.workforce.support.audit.ThrowingSupplier;
import com.ebikes.workforce.support.changes.SnapshotCreator;
import com.ebikes.workforce.support.fixtures.AgentDtoFixtures;
import com.ebikes.workforce.support.fixtures.AgentFixtures;
import com.ebikes.workforce.support.infrastructure.WithExecutionContext;
import com.ebikes.workforce.support.makerchecker.MakerCheckerTemplate;

@DisplayName("AgentsService")
@ExtendWith({MockitoExtension.class, WithExecutionContext.class})
class AgentsServiceTest {

  @Mock private AccessPolicy accessPolicy;
  @Mock private AgentMapper agentMapper;
  @Mock private AgentRepository agentRepository;
  @Mock private AuditTemplate auditTemplate;
  @Mock private DocumentService documentService;
  @Mock private MakerCheckerTemplate makerCheckerTemplate;
  @Mock private SnapshotCreator snapshotCreator;

  @InjectMocks private AgentsService agentsService;

  @SuppressWarnings("unchecked")
  private void wireAuditTemplateSupplier() {
    doAnswer(
            invocation -> {
              ThrowingSupplier<?, ?> operation = invocation.getArgument(3);
              return operation.get();
            })
        .when(auditTemplate)
        .execute(any(), any(), any(), any(ThrowingSupplier.class));
  }

  private AgentConflictCheck noConflicts() {
    AgentConflictCheck check = mock(AgentConflictCheck.class);
    when(check.isPhoneExists()).thenReturn(false);
    when(check.isNationalIdExists()).thenReturn(false);
    when(check.isUserIdExists()).thenReturn(false);
    return check;
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("should throw DuplicateResourceException when phone number already exists")
    void shouldThrowWhenPhoneExists() {
      AgentConflictCheck check = mock(AgentConflictCheck.class);
      when(check.isPhoneExists()).thenReturn(true);
      when(agentRepository.checkForConflicts(anyString(), anyString(), anyString()))
          .thenReturn(check);

      CreateAgentRequest request = AgentDtoFixtures.createRequest();
      assertThatThrownBy(() -> agentsService.create(request))
          .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("should throw DuplicateResourceException when national ID already exists")
    void shouldThrowWhenNationalIdExists() {
      AgentConflictCheck check = mock(AgentConflictCheck.class);
      when(check.isPhoneExists()).thenReturn(false);
      when(check.isNationalIdExists()).thenReturn(true);
      when(agentRepository.checkForConflicts(anyString(), anyString(), anyString()))
          .thenReturn(check);

      CreateAgentRequest request = AgentDtoFixtures.createRequest();
      assertThatThrownBy(() -> agentsService.create(request))
          .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("should throw DuplicateResourceException when userId already exists")
    void shouldThrowWhenUserIdExists() {
      AgentConflictCheck check = mock(AgentConflictCheck.class);
      when(check.isPhoneExists()).thenReturn(false);
      when(check.isNationalIdExists()).thenReturn(false);
      when(check.isUserIdExists()).thenReturn(true);
      when(agentRepository.checkForConflicts(anyString(), anyString(), anyString()))
          .thenReturn(check);

      CreateAgentRequest request = AgentDtoFixtures.createRequest();
      assertThatThrownBy(() -> agentsService.create(request))
          .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("should throw when canCreate fails")
    void shouldThrowWhenCanCreateFails() {
      AgentConflictCheck conflicts = noConflicts();
      when(agentRepository.checkForConflicts(anyString(), anyString(), anyString()))
          .thenReturn(conflicts);
      doThrow(new BusinessRuleException(ResponseCode.FORBIDDEN, "Access denied"))
          .when(accessPolicy)
          .canCreate(AgentFixtures.USER_ID);

      CreateAgentRequest request = AgentDtoFixtures.createRequest();
      assertThatThrownBy(() -> agentsService.create(request))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should create agent, publish maker-checker and return response")
    void shouldCreateAndPublish() {
      CreateAgentRequest request = AgentDtoFixtures.createRequest();
      AgentConflictCheck conflicts = noConflicts();
      Agent saved = AgentFixtures.available();
      Agent reloaded = AgentFixtures.available();
      List<FieldChange> changes = List.of(mock(FieldChange.class));
      List<String> expectedKeys =
          request.documents().stream().map(DocumentUploadInfo::key).toList();
      when(agentRepository.checkForConflicts(anyString(), anyString(), anyString()))
          .thenReturn(conflicts);
      when(agentRepository.save(any(Agent.class))).thenReturn(saved);
      when(agentRepository.findById(saved.getId())).thenReturn(Optional.of(reloaded));
      when(snapshotCreator.extractFields(reloaded)).thenReturn(changes);
      when(agentMapper.toDetailResponse(reloaded)).thenReturn(AgentDtoFixtures.detailResponse());

      AgentDetailResponse result = agentsService.create(request);

      assertThat(result).isNotNull();
      verify(documentService).associateWithAgent(expectedKeys, saved);
      verify(makerCheckerTemplate).publish(reloaded, null, "CREATE", changes);
    }

    @Test
    @DisplayName("should use capability class default when maxConcurrentOrders is null")
    void shouldUseCapabilityClassDefaultWhenMaxConcurrentOrdersNull() {
      AgentConflictCheck conflicts = noConflicts();
      CreateAgentRequest request =
          new CreateAgentRequest(
              null,
              CapabilityClass.BICYCLE_RIDER,
              AgentDtoFixtures.createRequest().documents(),
              null,
              "Test",
              "Agent",
              null,
              AgentFixtures.NATIONAL_ID_NUMBER,
              NationalIdType.NATIONAL_ID,
              AgentFixtures.PHONE_NUMBER,
              AgentFixtures.USER_ID);
      Agent saved = AgentFixtures.available();
      when(agentRepository.checkForConflicts(anyString(), anyString(), anyString()))
          .thenReturn(conflicts);
      when(agentRepository.save(any(Agent.class))).thenReturn(saved);
      when(agentRepository.findById(saved.getId())).thenReturn(Optional.of(saved));
      when(snapshotCreator.extractFields(any())).thenReturn(List.of());
      when(agentMapper.toDetailResponse(any())).thenReturn(AgentDtoFixtures.detailResponse());

      agentsService.create(request);

      verify(agentRepository).save(any(Agent.class));
    }
  }

  @Nested
  @DisplayName("deactivate")
  class Deactivate {

    @Test
    @DisplayName("should throw ResourceNotFoundException when agent not found")
    void shouldThrowWhenAgentNotFound() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> agentsService.deactivate(AgentFixtures.AGENT_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should deactivate agent and return response")
    void shouldDeactivateAndReturn() {
      wireAuditTemplateSupplier();
      Agent agent = AgentFixtures.available();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(agentRepository.save(agent)).thenReturn(agent);
      when(agentMapper.toDetailResponse(agent)).thenReturn(AgentDtoFixtures.detailResponse());

      AgentDetailResponse result = agentsService.deactivate(AgentFixtures.AGENT_ID);

      assertThat(result).isNotNull();
      verify(agentRepository).save(agent);
    }
  }

  @Nested
  @DisplayName("getById")
  class GetById {

    @Test
    @DisplayName("should throw ResourceNotFoundException when agent not found")
    void shouldThrowWhenAgentNotFound() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> agentsService.getById(AgentFixtures.AGENT_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should return mapped detail response")
    void shouldReturnDetailResponse() {
      Agent agent = AgentFixtures.available();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(agentMapper.toDetailResponse(agent)).thenReturn(AgentDtoFixtures.detailResponse());

      assertThat(agentsService.getById(AgentFixtures.AGENT_ID)).isNotNull();
    }
  }

  @Nested
  @DisplayName("getByUserId")
  class GetByUserId {

    @Test
    @DisplayName("should throw ResourceNotFoundException when agent not found")
    void shouldThrowWhenNotFound() {
      when(agentRepository.findByUserId(AgentFixtures.USER_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> agentsService.getByUserId(AgentFixtures.USER_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should return mapped detail response")
    void shouldReturnDetailResponse() {
      Agent agent = AgentFixtures.available();
      when(agentRepository.findByUserId(AgentFixtures.USER_ID)).thenReturn(Optional.of(agent));
      when(agentMapper.toDetailResponse(agent)).thenReturn(AgentDtoFixtures.detailResponse());

      assertThat(agentsService.getByUserId(AgentFixtures.USER_ID)).isNotNull();
    }
  }

  @Nested
  @DisplayName("resubmit")
  class Resubmit {

    @Test
    @DisplayName("should throw ResourceNotFoundException when agent not found")
    void shouldThrowWhenAgentNotFound() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> agentsService.resubmit(AgentFixtures.AGENT_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw when ownership check fails")
    void shouldThrowWhenOwnershipFails() {
      Agent agent = AgentFixtures.pending();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      doThrow(new BusinessRuleException(ResponseCode.FORBIDDEN, "Access denied"))
          .when(accessPolicy)
          .owns(agent);

      assertThatThrownBy(() -> agentsService.resubmit(AgentFixtures.AGENT_ID))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should throw BusinessRuleException when agent is not in PENDING status")
    void shouldThrowWhenNotPending() {
      wireAuditTemplateSupplier();
      Agent agent = AgentFixtures.available();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));

      assertThatThrownBy(() -> agentsService.resubmit(AgentFixtures.AGENT_ID))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should resubmit agent, publish maker-checker and return response")
    void shouldResubmitAndPublish() {
      wireAuditTemplateSupplier();
      Agent agent = AgentFixtures.pending();
      List<FieldChange> changes = List.of(mock(FieldChange.class));
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(agentRepository.save(agent)).thenReturn(agent);
      when(snapshotCreator.extractFields(agent)).thenReturn(changes);
      when(agentMapper.toDetailResponse(agent)).thenReturn(AgentDtoFixtures.detailResponse());

      AgentDetailResponse result = agentsService.resubmit(AgentFixtures.AGENT_ID);

      assertThat(result).isNotNull();
      verify(makerCheckerTemplate).publish(agent, null, "CREATE", changes);
    }
  }

  @Nested
  @DisplayName("search")
  class Search {

    @Test
    @DisplayName("should return paginated agent summaries")
    @SuppressWarnings("unchecked")
    void shouldReturnPaginatedResults() {
      Agent agent = AgentFixtures.available();
      when(agentRepository.findAll(any(Specification.class), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(agent)));
      when(agentMapper.toSummaryResponse(agent)).thenReturn(AgentDtoFixtures.summaryResponse());

      PaginatedResponse<AgentSummaryResponse> result = agentsService.search(new AgentFilter());

      assertThat(result.data()).hasSize(1);
    }
  }

  @Nested
  @DisplayName("update")
  class Update {

    @Test
    @DisplayName("should throw ResourceNotFoundException when agent not found")
    void shouldThrowWhenAgentNotFound() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.empty());

      UpdateAgentRequest request = AgentDtoFixtures.updateRequest();
      assertThatThrownBy(() -> agentsService.update(AgentFixtures.AGENT_ID, request))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw when ownership check fails")
    void shouldThrowWhenOwnershipFails() {
      Agent agent = AgentFixtures.available();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      doThrow(new BusinessRuleException(ResponseCode.FORBIDDEN, "Access denied"))
          .when(accessPolicy)
          .owns(agent);

      UpdateAgentRequest request = AgentDtoFixtures.updateRequest();
      assertThatThrownBy(() -> agentsService.update(AgentFixtures.AGENT_ID, request))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should return existing response without publishing when no changes detected")
    void shouldReturnEarlyWhenNoChanges() {
      Agent agent = AgentFixtures.available();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(snapshotCreator.extractChanges(AgentDtoFixtures.updateRequest(), agent))
          .thenReturn(List.of());
      when(agentMapper.toDetailResponse(agent)).thenReturn(AgentDtoFixtures.detailResponse());

      AgentDetailResponse result =
          agentsService.update(AgentFixtures.AGENT_ID, AgentDtoFixtures.updateRequest());

      assertThat(result).isNotNull();
      verify(makerCheckerTemplate, never()).publish(any(), any(), any(), any());
    }

    @Test
    @DisplayName("should publish maker-checker when changes are detected")
    void shouldPublishWhenChangesDetected() {
      Agent agent = AgentFixtures.available();
      List<FieldChange> changes = List.of(mock(FieldChange.class));
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(snapshotCreator.extractChanges(AgentDtoFixtures.updateRequest(), agent))
          .thenReturn(changes);
      when(agentMapper.toDetailResponse(agent)).thenReturn(AgentDtoFixtures.detailResponse());

      AgentDetailResponse result =
          agentsService.update(AgentFixtures.AGENT_ID, AgentDtoFixtures.updateRequest());

      assertThat(result).isNotNull();
      verify(makerCheckerTemplate).publish(agent, null, "UPDATE", changes);
    }
  }
}
