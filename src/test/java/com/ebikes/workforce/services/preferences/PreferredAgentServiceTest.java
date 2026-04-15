package com.ebikes.workforce.services.preferences;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
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
import com.ebikes.workforce.database.entities.PreferredAgent;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.PreferredAgentRepository;
import com.ebikes.workforce.dtos.requests.filters.PreferredAgentFilter;
import com.ebikes.workforce.dtos.requests.preferredagents.CreatePreferredAgentRequest;
import com.ebikes.workforce.dtos.requests.preferredagents.UpdatePreferredAgentRequest;
import com.ebikes.workforce.dtos.responses.api.PaginatedResponse;
import com.ebikes.workforce.dtos.responses.preferredagents.PreferredAgentDetailResponse;
import com.ebikes.workforce.dtos.responses.preferredagents.PreferredAgentSummaryResponse;
import com.ebikes.workforce.enums.UserRole;
import com.ebikes.workforce.exceptions.AuthorizationException;
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.exceptions.DuplicateResourceException;
import com.ebikes.workforce.exceptions.ResourceNotFoundException;
import com.ebikes.workforce.exceptions.ValidationException;
import com.ebikes.workforce.mappers.PreferredAgentMapper;
import com.ebikes.workforce.support.audit.AuditTemplate;
import com.ebikes.workforce.support.audit.ThrowingRunnable;
import com.ebikes.workforce.support.audit.ThrowingSupplier;
import com.ebikes.workforce.support.context.ExecutionContext;
import com.ebikes.workforce.support.fixtures.AgentFixtures;
import com.ebikes.workforce.support.fixtures.PreferredAgentFixtures;
import com.ebikes.workforce.support.fixtures.SecurityFixtures;
import com.ebikes.workforce.support.infrastructure.WithExecutionContext;

@DisplayName("PreferredAgentService")
@ExtendWith({MockitoExtension.class, WithExecutionContext.class})
class PreferredAgentServiceTest {

  @Mock private AgentRepository agentRepository;
  @Mock private AuditTemplate auditTemplate;
  @Mock private PreferredAgentMapper preferredAgentMapper;
  @Mock private PreferredAgentRepository preferredAgentRepository;

  @InjectMocks private PreferredAgentService preferredAgentService;

  @SuppressWarnings("unchecked")
  private void wireAuditTemplateRunnable() {
    doAnswer(
            invocation -> {
              ThrowingRunnable<?> operation = invocation.getArgument(3);
              operation.run();
              return null;
            })
        .when(auditTemplate)
        .execute(any(), any(), any(), any(ThrowingRunnable.class));
  }

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

  private CreatePreferredAgentRequest createRequest(String organizationId, String branchId) {
    return new CreatePreferredAgentRequest(
        AgentFixtures.AGENT_ID, branchId, "Reliable agent", organizationId, 1);
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("should throw ResourceNotFoundException when agent not found")
    void shouldThrowWhenAgentNotFound() {
      SecurityFixtures.setExecutionContext(
          SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  preferredAgentService.create(
                      createRequest(SecurityFixtures.TEST_ORGANIZATION_ID, null)))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw BusinessRuleException when agent is deactivated")
    void shouldThrowWhenAgentDeactivated() {
      SecurityFixtures.setExecutionContext(
          SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);
      Agent agent = AgentFixtures.deactivated();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));

      assertThatThrownBy(
              () ->
                  preferredAgentService.create(
                      createRequest(SecurityFixtures.TEST_ORGANIZATION_ID, null)))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should throw DuplicateResourceException when org-scoped duplicate exists")
    void shouldThrowOnOrgScopedDuplicate() {
      SecurityFixtures.setExecutionContext(
          SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);
      Agent agent = AgentFixtures.available();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(preferredAgentRepository.existsByOrganizationIdAndAgentId(
              SecurityFixtures.TEST_ORGANIZATION_ID, AgentFixtures.AGENT_ID))
          .thenReturn(true);

      assertThatThrownBy(
              () ->
                  preferredAgentService.create(
                      createRequest(SecurityFixtures.TEST_ORGANIZATION_ID, null)))
          .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("should throw DuplicateResourceException when branch-scoped duplicate exists")
    void shouldThrowOnBranchScopedDuplicate() {
      SecurityFixtures.setExecutionContext(
          SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);
      Agent agent = AgentFixtures.available();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(preferredAgentRepository.existsByOrganizationIdAndBranchIdAndAgentId(
              SecurityFixtures.TEST_ORGANIZATION_ID, "branch-001", AgentFixtures.AGENT_ID))
          .thenReturn(true);

      assertThatThrownBy(
              () ->
                  preferredAgentService.create(
                      createRequest(SecurityFixtures.TEST_ORGANIZATION_ID, "branch-001")))
          .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("should throw AuthorizationException when user has no active organization")
    void shouldThrowWhenNoActiveOrganization() {
      SecurityFixtures.setExecutionContext(null, null, UserRole.ORGANIZATION_ADMIN);

      assertThatThrownBy(() -> preferredAgentService.create(createRequest(null, null)))
          .isInstanceOf(AuthorizationException.class);
    }

    @Test
    @DisplayName(
        "should throw AuthorizationException when requested organizationId does not match active"
            + " organization")
    void shouldThrowWhenOrganizationIdMismatch() {
      SecurityFixtures.setExecutionContext(
          SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);

      assertThatThrownBy(
              () ->
                  preferredAgentService.create(
                      createRequest(SecurityFixtures.OTHER_ORGANIZATION_ID, null)))
          .isInstanceOf(AuthorizationException.class);
    }

    @Test
    @DisplayName("should throw ValidationException when system context and organizationId is null")
    void shouldThrowWhenSystemContextAndOrganizationIdNull() {
      ExecutionContext.setSystem();

      assertThatThrownBy(() -> preferredAgentService.create(createRequest(null, null)))
          .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName(
        "should use active organization from user context when no organizationId in request")
    void shouldUseActiveOrganizationWhenNoneInRequest() {
      wireAuditTemplateSupplier();
      SecurityFixtures.setExecutionContext(
          SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);
      Agent agent = AgentFixtures.available();
      PreferredAgent saved =
          PreferredAgentFixtures.orgScoped(
              AgentFixtures.AGENT_ID, SecurityFixtures.TEST_ORGANIZATION_ID);
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(preferredAgentRepository.existsByOrganizationIdAndAgentId(
              SecurityFixtures.TEST_ORGANIZATION_ID, AgentFixtures.AGENT_ID))
          .thenReturn(false);
      when(preferredAgentRepository.save(any(PreferredAgent.class))).thenReturn(saved);
      when(preferredAgentMapper.toDetailResponse(saved))
          .thenReturn(mock(PreferredAgentDetailResponse.class));

      PreferredAgentDetailResponse result = preferredAgentService.create(createRequest(null, null));

      assertThat(result).isNotNull();
      verify(preferredAgentRepository).save(any(PreferredAgent.class));
    }

    @Test
    @DisplayName("should save and return mapped response on success")
    void shouldSaveAndReturnResponse() {
      wireAuditTemplateSupplier();
      SecurityFixtures.setExecutionContext(
          SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);
      Agent agent = AgentFixtures.available();
      PreferredAgent saved =
          PreferredAgentFixtures.orgScoped(
              AgentFixtures.AGENT_ID, SecurityFixtures.TEST_ORGANIZATION_ID);
      PreferredAgentDetailResponse response = mock(PreferredAgentDetailResponse.class);
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(preferredAgentRepository.existsByOrganizationIdAndAgentId(
              SecurityFixtures.TEST_ORGANIZATION_ID, AgentFixtures.AGENT_ID))
          .thenReturn(false);
      when(preferredAgentRepository.save(any(PreferredAgent.class))).thenReturn(saved);
      when(preferredAgentMapper.toDetailResponse(saved)).thenReturn(response);

      assertThat(
              preferredAgentService.create(
                  createRequest(SecurityFixtures.TEST_ORGANIZATION_ID, null)))
          .isEqualTo(response);
    }
  }

  @Nested
  @DisplayName("delete")
  class Delete {

    @Test
    @DisplayName("should throw ResourceNotFoundException when preferred agent not found")
    void shouldThrowWhenNotFound() {
      SecurityFixtures.setExecutionContext(
          SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);
      when(preferredAgentRepository.findById(PreferredAgentFixtures.PREFERRED_AGENT_ID))
          .thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> preferredAgentService.delete(PreferredAgentFixtures.PREFERRED_AGENT_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName(
        "should throw AuthorizationException when preferred agent belongs to a different"
            + " organization")
    void shouldThrowWhenOrganizationMismatch() {
      SecurityFixtures.setExecutionContext(
          SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);
      PreferredAgent preferredAgent =
          PreferredAgentFixtures.orgScoped(
              AgentFixtures.AGENT_ID, SecurityFixtures.OTHER_ORGANIZATION_ID);
      when(preferredAgentRepository.findById(PreferredAgentFixtures.PREFERRED_AGENT_ID))
          .thenReturn(Optional.of(preferredAgent));

      assertThatThrownBy(
              () -> preferredAgentService.delete(PreferredAgentFixtures.PREFERRED_AGENT_ID))
          .isInstanceOf(AuthorizationException.class);
    }

    @Test
    @DisplayName("should delete successfully for system admin")
    void shouldDeleteForSystemAdmin() {
      wireAuditTemplateRunnable();
      SecurityFixtures.setExecutionContext(
          SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.SYSTEM_ADMIN);
      PreferredAgent preferredAgent =
          PreferredAgentFixtures.orgScoped(
              AgentFixtures.AGENT_ID, SecurityFixtures.OTHER_ORGANIZATION_ID);
      when(preferredAgentRepository.findById(PreferredAgentFixtures.PREFERRED_AGENT_ID))
          .thenReturn(Optional.of(preferredAgent));

      preferredAgentService.delete(PreferredAgentFixtures.PREFERRED_AGENT_ID);

      verify(preferredAgentRepository).delete(preferredAgent);
    }

    @Test
    @DisplayName("should delete successfully when organization matches")
    void shouldDeleteWhenOrganizationMatches() {
      wireAuditTemplateRunnable();
      SecurityFixtures.setExecutionContext(
          SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);
      PreferredAgent preferredAgent =
          PreferredAgentFixtures.orgScoped(
              AgentFixtures.AGENT_ID, SecurityFixtures.TEST_ORGANIZATION_ID);
      when(preferredAgentRepository.findById(PreferredAgentFixtures.PREFERRED_AGENT_ID))
          .thenReturn(Optional.of(preferredAgent));

      preferredAgentService.delete(PreferredAgentFixtures.PREFERRED_AGENT_ID);

      verify(preferredAgentRepository).delete(preferredAgent);
    }
  }

  @Nested
  @DisplayName("deleteByAgentId")
  class DeleteByAgentId {

    @Test
    @DisplayName("should delegate to repository deleteByAgentId")
    void shouldDelegateToRepository() {
      preferredAgentService.deleteByAgentId(AgentFixtures.AGENT_ID);

      verify(preferredAgentRepository).deleteByAgentId(AgentFixtures.AGENT_ID);
    }
  }

  @Nested
  @DisplayName("getById")
  class GetById {

    @Test
    @DisplayName("should throw ResourceNotFoundException when not found")
    void shouldThrowWhenNotFound() {
      SecurityFixtures.setExecutionContext(
          SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);
      when(preferredAgentRepository.findById(PreferredAgentFixtures.PREFERRED_AGENT_ID))
          .thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> preferredAgentService.getById(PreferredAgentFixtures.PREFERRED_AGENT_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should return mapped detail response")
    void shouldReturnDetailResponse() {
      SecurityFixtures.setExecutionContext(
          SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);
      PreferredAgent preferredAgent =
          PreferredAgentFixtures.orgScoped(
              AgentFixtures.AGENT_ID, SecurityFixtures.TEST_ORGANIZATION_ID);
      PreferredAgentDetailResponse response = mock(PreferredAgentDetailResponse.class);
      when(preferredAgentRepository.findById(PreferredAgentFixtures.PREFERRED_AGENT_ID))
          .thenReturn(Optional.of(preferredAgent));
      when(preferredAgentMapper.toDetailResponse(preferredAgent)).thenReturn(response);

      assertThat(preferredAgentService.getById(PreferredAgentFixtures.PREFERRED_AGENT_ID))
          .isEqualTo(response);
    }
  }

  @Nested
  @DisplayName("search")
  class Search {

    @Test
    @DisplayName("should return paginated preferred agent summaries")
    @SuppressWarnings("unchecked")
    void shouldReturnPaginatedResults() {
      PreferredAgent preferredAgent =
          PreferredAgentFixtures.orgScoped(
              AgentFixtures.AGENT_ID, SecurityFixtures.TEST_ORGANIZATION_ID);
      PreferredAgentSummaryResponse summary = mock(PreferredAgentSummaryResponse.class);
      when(preferredAgentRepository.findAll(any(Specification.class), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(preferredAgent)));
      when(preferredAgentMapper.toSummaryResponse(preferredAgent)).thenReturn(summary);

      PaginatedResponse<PreferredAgentSummaryResponse> result =
          preferredAgentService.search(new PreferredAgentFilter());

      assertThat(result.data()).containsExactly(summary);
    }
  }

  @Nested
  @DisplayName("update")
  class Update {

    @Test
    @DisplayName("should throw ValidationException when both notes and priority are null")
    void shouldThrowWhenBothFieldsNull() {
      SecurityFixtures.setExecutionContext(
          SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);

      assertThatThrownBy(
              () ->
                  preferredAgentService.update(
                      PreferredAgentFixtures.PREFERRED_AGENT_ID,
                      new UpdatePreferredAgentRequest(null, null)))
          .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when preferred agent not found")
    void shouldThrowWhenNotFound() {
      SecurityFixtures.setExecutionContext(
          SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);
      when(preferredAgentRepository.findById(PreferredAgentFixtures.PREFERRED_AGENT_ID))
          .thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  preferredAgentService.update(
                      PreferredAgentFixtures.PREFERRED_AGENT_ID,
                      new UpdatePreferredAgentRequest("updated notes", null)))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should update only notes when priority is null")
    void shouldUpdateNotesOnly() {
      wireAuditTemplateSupplier();
      SecurityFixtures.setExecutionContext(
          SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);
      PreferredAgent preferredAgent =
          PreferredAgentFixtures.orgScoped(
              AgentFixtures.AGENT_ID, SecurityFixtures.TEST_ORGANIZATION_ID);
      when(preferredAgentRepository.findById(PreferredAgentFixtures.PREFERRED_AGENT_ID))
          .thenReturn(Optional.of(preferredAgent));
      when(preferredAgentRepository.save(preferredAgent)).thenReturn(preferredAgent);
      when(preferredAgentMapper.toDetailResponse(preferredAgent))
          .thenReturn(mock(PreferredAgentDetailResponse.class));

      preferredAgentService.update(
          PreferredAgentFixtures.PREFERRED_AGENT_ID,
          new UpdatePreferredAgentRequest("updated notes", null));

      assertThat(preferredAgent.getNotes()).isEqualTo("updated notes");
      assertThat(preferredAgent.getPriority()).isEqualTo(1);
    }

    @Test
    @DisplayName("should update only priority when notes is null")
    void shouldUpdatePriorityOnly() {
      wireAuditTemplateSupplier();
      SecurityFixtures.setExecutionContext(
          SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);
      PreferredAgent preferredAgent =
          PreferredAgentFixtures.orgScoped(
              AgentFixtures.AGENT_ID, SecurityFixtures.TEST_ORGANIZATION_ID);
      when(preferredAgentRepository.findById(PreferredAgentFixtures.PREFERRED_AGENT_ID))
          .thenReturn(Optional.of(preferredAgent));
      when(preferredAgentRepository.save(preferredAgent)).thenReturn(preferredAgent);
      when(preferredAgentMapper.toDetailResponse(preferredAgent))
          .thenReturn(mock(PreferredAgentDetailResponse.class));

      preferredAgentService.update(
          PreferredAgentFixtures.PREFERRED_AGENT_ID, new UpdatePreferredAgentRequest(null, 5));

      assertThat(preferredAgent.getPriority()).isEqualTo(5);
      assertThat(preferredAgent.getNotes()).isEqualTo("Preferred for reliability");
    }

    @Test
    @DisplayName("should update both notes and priority when both provided")
    void shouldUpdateBothFields() {
      wireAuditTemplateSupplier();
      SecurityFixtures.setExecutionContext(
          SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);
      PreferredAgent preferredAgent =
          PreferredAgentFixtures.orgScoped(
              AgentFixtures.AGENT_ID, SecurityFixtures.TEST_ORGANIZATION_ID);
      when(preferredAgentRepository.findById(PreferredAgentFixtures.PREFERRED_AGENT_ID))
          .thenReturn(Optional.of(preferredAgent));
      when(preferredAgentRepository.save(preferredAgent)).thenReturn(preferredAgent);
      when(preferredAgentMapper.toDetailResponse(preferredAgent))
          .thenReturn(mock(PreferredAgentDetailResponse.class));

      preferredAgentService.update(
          PreferredAgentFixtures.PREFERRED_AGENT_ID,
          new UpdatePreferredAgentRequest("new notes", 3));

      assertThat(preferredAgent.getNotes()).isEqualTo("new notes");
      assertThat(preferredAgent.getPriority()).isEqualTo(3);
    }

    @Test
    @DisplayName("should return mapped response on success")
    void shouldReturnMappedResponse() {
      wireAuditTemplateSupplier();
      SecurityFixtures.setExecutionContext(
          SecurityFixtures.TEST_ORGANIZATION_ID, null, UserRole.ORGANIZATION_ADMIN);
      PreferredAgent preferredAgent =
          PreferredAgentFixtures.orgScoped(
              AgentFixtures.AGENT_ID, SecurityFixtures.TEST_ORGANIZATION_ID);
      PreferredAgentDetailResponse response = mock(PreferredAgentDetailResponse.class);
      when(preferredAgentRepository.findById(PreferredAgentFixtures.PREFERRED_AGENT_ID))
          .thenReturn(Optional.of(preferredAgent));
      when(preferredAgentRepository.save(preferredAgent)).thenReturn(preferredAgent);
      when(preferredAgentMapper.toDetailResponse(preferredAgent)).thenReturn(response);

      assertThat(
              preferredAgentService.update(
                  PreferredAgentFixtures.PREFERRED_AGENT_ID,
                  new UpdatePreferredAgentRequest("notes", 2)))
          .isEqualTo(response);
    }
  }
}
