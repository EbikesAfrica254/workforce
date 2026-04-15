package com.ebikes.workforce.services.agents.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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

import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.PaymentMethod;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.PaymentMethodRepository;
import com.ebikes.workforce.dtos.requests.paymentmethods.CreatePaymentMethodRequest;
import com.ebikes.workforce.dtos.requests.paymentmethods.UpdatePaymentMethodRequest;
import com.ebikes.workforce.dtos.responses.paymentmethods.PaymentMethodDetailResponse;
import com.ebikes.workforce.dtos.responses.paymentmethods.PaymentMethodSummaryResponse;
import com.ebikes.workforce.enums.PaymentMethodType;
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.exceptions.ResourceNotFoundException;
import com.ebikes.workforce.mappers.PaymentMethodMapper;
import com.ebikes.workforce.services.agents.AccessPolicy;
import com.ebikes.workforce.support.audit.AuditTemplate;
import com.ebikes.workforce.support.audit.ThrowingRunnable;
import com.ebikes.workforce.support.audit.ThrowingSupplier;
import com.ebikes.workforce.support.fixtures.AgentFixtures;
import com.ebikes.workforce.support.fixtures.PaymentMethodFixtures;
import com.ebikes.workforce.support.infrastructure.WithExecutionContext;

@DisplayName("PaymentMethodService")
@ExtendWith({MockitoExtension.class, WithExecutionContext.class})
class PaymentMethodServiceTest {

  @Mock private AccessPolicy accessPolicy;
  @Mock private AgentRepository agentRepository;
  @Mock private AuditTemplate auditTemplate;
  @Mock private PaymentMethodMapper paymentMethodMapper;
  @Mock private PaymentMethodRepository paymentMethodRepository;
  @Mock private PaymentMethodResolver paymentMethodResolver;

  @InjectMocks private PaymentMethodService paymentMethodService;

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

  @Nested
  @DisplayName("create")
  class Create {

    private final CreatePaymentMethodRequest request =
        new CreatePaymentMethodRequest(
            null,
            false,
            "Test Agent",
            null,
            PaymentMethodType.MPESA_PERSONAL,
            "+254700000001",
            null);

    private final CreatePaymentMethodRequest primaryRequest =
        new CreatePaymentMethodRequest(
            null,
            true,
            "Test Agent",
            null,
            PaymentMethodType.MPESA_PERSONAL,
            "+254700000001",
            null);

    @Test
    @DisplayName("should throw ResourceNotFoundException when agent not found")
    void shouldThrowWhenAgentNotFound() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> paymentMethodService.create(AgentFixtures.AGENT_ID, request))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw BusinessRuleException when agent does not own the resource")
    void shouldThrowWhenOwnershipFails() {
      Agent agent = AgentFixtures.available();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      org.mockito.Mockito.doThrow(
              new BusinessRuleException(
                  com.ebikes.workforce.enums.ResponseCode.FORBIDDEN, "Access denied"))
          .when(accessPolicy)
          .owns(agent);

      assertThatThrownBy(() -> paymentMethodService.create(AgentFixtures.AGENT_ID, request))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should create payment method and save")
    void shouldCreateAndSave() {
      wireAuditTemplateSupplier();
      Agent agent = AgentFixtures.available();
      PaymentMethod paymentMethod = PaymentMethodFixtures.mpesaPersonal(AgentFixtures.AGENT_ID);
      PaymentMethodDetailResponse response = mock(PaymentMethodDetailResponse.class);
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(paymentMethodResolver.build(AgentFixtures.AGENT_ID, request)).thenReturn(paymentMethod);
      when(paymentMethodRepository.save(paymentMethod)).thenReturn(paymentMethod);
      when(paymentMethodMapper.toDetailResponse(paymentMethod)).thenReturn(response);

      PaymentMethodDetailResponse result =
          paymentMethodService.create(AgentFixtures.AGENT_ID, request);

      assertThat(result).isEqualTo(response);
      verify(paymentMethodRepository).save(paymentMethod);
      verify(paymentMethodRepository, never()).findByAgentIdAndIsPrimaryTrue(any());
    }

    @Test
    @DisplayName("should clear existing primary before saving when isPrimary is true")
    void shouldClearExistingPrimaryWhenIsPrimary() {
      wireAuditTemplateSupplier();
      Agent agent = AgentFixtures.available();
      PaymentMethod existing = PaymentMethodFixtures.primary(AgentFixtures.AGENT_ID);
      PaymentMethod paymentMethod = PaymentMethodFixtures.mpesaPersonal(AgentFixtures.AGENT_ID);
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(paymentMethodRepository.findByAgentIdAndIsPrimaryTrue(AgentFixtures.AGENT_ID))
          .thenReturn(Optional.of(existing));
      when(paymentMethodResolver.build(AgentFixtures.AGENT_ID, primaryRequest))
          .thenReturn(paymentMethod);
      when(paymentMethodRepository.save(existing)).thenReturn(existing);
      when(paymentMethodRepository.save(paymentMethod)).thenReturn(paymentMethod);
      when(paymentMethodMapper.toDetailResponse(paymentMethod))
          .thenReturn(mock(PaymentMethodDetailResponse.class));

      paymentMethodService.create(AgentFixtures.AGENT_ID, primaryRequest);

      verify(paymentMethodRepository).save(existing);
      assertThat(existing.getIsPrimary()).isFalse();
    }
  }

  @Nested
  @DisplayName("delete")
  class Delete {

    @Test
    @DisplayName("should throw ResourceNotFoundException when agent not found")
    void shouldThrowWhenAgentNotFound() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  paymentMethodService.delete(
                      AgentFixtures.AGENT_ID, PaymentMethodFixtures.PAYMENT_METHOD_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw BusinessRuleException when agent does not own the resource")
    void shouldThrowWhenOwnershipFails() {
      Agent agent = AgentFixtures.available();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      org.mockito.Mockito.doThrow(
              new BusinessRuleException(
                  com.ebikes.workforce.enums.ResponseCode.FORBIDDEN, "Access denied"))
          .when(accessPolicy)
          .owns(agent);

      assertThatThrownBy(
              () ->
                  paymentMethodService.delete(
                      AgentFixtures.AGENT_ID, PaymentMethodFixtures.PAYMENT_METHOD_ID))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when payment method not found for agent")
    void shouldThrowWhenPaymentMethodNotFound() {
      Agent agent = AgentFixtures.available();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(paymentMethodRepository.findById(PaymentMethodFixtures.PAYMENT_METHOD_ID))
          .thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  paymentMethodService.delete(
                      AgentFixtures.AGENT_ID, PaymentMethodFixtures.PAYMENT_METHOD_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw BusinessRuleException when deleting primary payment method")
    void shouldThrowWhenDeletingPrimary() {
      Agent agent = AgentFixtures.available();
      PaymentMethod primary = PaymentMethodFixtures.primary(AgentFixtures.AGENT_ID);
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(paymentMethodRepository.findById(PaymentMethodFixtures.PAYMENT_METHOD_ID))
          .thenReturn(Optional.of(primary));

      assertThatThrownBy(
              () ->
                  paymentMethodService.delete(
                      AgentFixtures.AGENT_ID, PaymentMethodFixtures.PAYMENT_METHOD_ID))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should delete non-primary payment method")
    void shouldDeleteNonPrimary() {
      wireAuditTemplateRunnable();
      Agent agent = AgentFixtures.available();
      PaymentMethod paymentMethod = PaymentMethodFixtures.mpesaPersonal(AgentFixtures.AGENT_ID);
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(paymentMethodRepository.findById(PaymentMethodFixtures.PAYMENT_METHOD_ID))
          .thenReturn(Optional.of(paymentMethod));

      paymentMethodService.delete(AgentFixtures.AGENT_ID, PaymentMethodFixtures.PAYMENT_METHOD_ID);

      verify(paymentMethodRepository).delete(paymentMethod);
    }
  }

  @Nested
  @DisplayName("findByAgentId")
  class FindByAgentId {

    @Test
    @DisplayName("should throw ResourceNotFoundException when agent not found")
    void shouldThrowWhenAgentNotFound() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> paymentMethodService.findByAgentId(AgentFixtures.AGENT_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should return mapped summary responses")
    void shouldReturnMappedSummaries() {
      Agent agent = AgentFixtures.available();
      PaymentMethod pm = PaymentMethodFixtures.mpesaPersonal(AgentFixtures.AGENT_ID);
      PaymentMethodSummaryResponse summary = mock(PaymentMethodSummaryResponse.class);
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(paymentMethodRepository.findByAgentId(AgentFixtures.AGENT_ID)).thenReturn(List.of(pm));
      when(paymentMethodResolver.maskIdentifier(pm)).thenReturn("*****0001");
      when(paymentMethodMapper.toSummaryResponse(pm, "*****0001")).thenReturn(summary);

      List<PaymentMethodSummaryResponse> result =
          paymentMethodService.findByAgentId(AgentFixtures.AGENT_ID);

      assertThat(result).containsExactly(summary);
    }
  }

  @Nested
  @DisplayName("getById")
  class GetById {

    @Test
    @DisplayName("should throw ResourceNotFoundException when payment method not found for agent")
    void shouldThrowWhenNotFound() {
      when(paymentMethodRepository.findById(PaymentMethodFixtures.PAYMENT_METHOD_ID))
          .thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  paymentMethodService.getById(
                      AgentFixtures.AGENT_ID, PaymentMethodFixtures.PAYMENT_METHOD_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should return mapped detail response")
    void shouldReturnDetailResponse() {
      PaymentMethod pm = PaymentMethodFixtures.mpesaPersonal(AgentFixtures.AGENT_ID);
      PaymentMethodDetailResponse response = mock(PaymentMethodDetailResponse.class);
      when(paymentMethodRepository.findById(PaymentMethodFixtures.PAYMENT_METHOD_ID))
          .thenReturn(Optional.of(pm));
      when(paymentMethodMapper.toDetailResponse(pm)).thenReturn(response);

      PaymentMethodDetailResponse result =
          paymentMethodService.getById(
              AgentFixtures.AGENT_ID, PaymentMethodFixtures.PAYMENT_METHOD_ID);

      assertThat(result).isEqualTo(response);
    }
  }

  @Nested
  @DisplayName("setPrimary")
  class SetPrimary {

    @Test
    @DisplayName("should throw ResourceNotFoundException when agent not found")
    void shouldThrowWhenAgentNotFound() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  paymentMethodService.setPrimary(
                      AgentFixtures.AGENT_ID, PaymentMethodFixtures.PAYMENT_METHOD_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw BusinessRuleException when agent does not own the resource")
    void shouldThrowWhenOwnershipFails() {
      Agent agent = AgentFixtures.available();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      org.mockito.Mockito.doThrow(
              new BusinessRuleException(
                  com.ebikes.workforce.enums.ResponseCode.FORBIDDEN, "Access denied"))
          .when(accessPolicy)
          .owns(agent);

      assertThatThrownBy(
              () ->
                  paymentMethodService.setPrimary(
                      AgentFixtures.AGENT_ID, PaymentMethodFixtures.PAYMENT_METHOD_ID))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should return existing response without saving when already primary")
    void shouldReturnEarlyWhenAlreadyPrimary() {
      Agent agent = AgentFixtures.available();
      PaymentMethod primary = PaymentMethodFixtures.primary(AgentFixtures.AGENT_ID);
      PaymentMethodDetailResponse response = mock(PaymentMethodDetailResponse.class);
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(paymentMethodRepository.findById(PaymentMethodFixtures.PAYMENT_METHOD_ID))
          .thenReturn(Optional.of(primary));
      when(paymentMethodMapper.toDetailResponse(primary)).thenReturn(response);

      PaymentMethodDetailResponse result =
          paymentMethodService.setPrimary(
              AgentFixtures.AGENT_ID, PaymentMethodFixtures.PAYMENT_METHOD_ID);

      assertThat(result).isEqualTo(response);
      verify(paymentMethodRepository, never()).save(any());
    }

    @Test
    @DisplayName("should clear existing primary, mark new primary and save")
    void shouldSetNewPrimary() {
      wireAuditTemplateSupplier();
      Agent agent = AgentFixtures.available();
      PaymentMethod existing = PaymentMethodFixtures.primary(AgentFixtures.AGENT_ID);
      PaymentMethod paymentMethod = PaymentMethodFixtures.mpesaTill(AgentFixtures.AGENT_ID);
      PaymentMethodDetailResponse response = mock(PaymentMethodDetailResponse.class);
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(paymentMethodRepository.findById(PaymentMethodFixtures.PAYMENT_METHOD_ID))
          .thenReturn(Optional.of(paymentMethod));
      when(paymentMethodRepository.findByAgentIdAndIsPrimaryTrue(AgentFixtures.AGENT_ID))
          .thenReturn(Optional.of(existing));
      when(paymentMethodRepository.save(existing)).thenReturn(existing);
      when(paymentMethodRepository.save(paymentMethod)).thenReturn(paymentMethod);
      when(paymentMethodMapper.toDetailResponse(paymentMethod)).thenReturn(response);

      PaymentMethodDetailResponse result =
          paymentMethodService.setPrimary(
              AgentFixtures.AGENT_ID, PaymentMethodFixtures.PAYMENT_METHOD_ID);

      assertThat(result).isEqualTo(response);
      assertThat(existing.getIsPrimary()).isFalse();
      assertThat(paymentMethod.getIsPrimary()).isTrue();
      verify(paymentMethodRepository).save(existing);
      verify(paymentMethodRepository).save(paymentMethod);
    }
  }

  @Nested
  @DisplayName("update")
  class Update {

    private final UpdatePaymentMethodRequest request = new UpdatePaymentMethodRequest("New Name");

    @Test
    @DisplayName("should throw ResourceNotFoundException when agent not found")
    void shouldThrowWhenAgentNotFound() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  paymentMethodService.update(
                      AgentFixtures.AGENT_ID, PaymentMethodFixtures.PAYMENT_METHOD_ID, request))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw BusinessRuleException when agent does not own the resource")
    void shouldThrowWhenOwnershipFails() {
      Agent agent = AgentFixtures.available();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      org.mockito.Mockito.doThrow(
              new BusinessRuleException(
                  com.ebikes.workforce.enums.ResponseCode.FORBIDDEN, "Access denied"))
          .when(accessPolicy)
          .owns(agent);

      assertThatThrownBy(
              () ->
                  paymentMethodService.update(
                      AgentFixtures.AGENT_ID, PaymentMethodFixtures.PAYMENT_METHOD_ID, request))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when payment method not found for agent")
    void shouldThrowWhenPaymentMethodNotFound() {
      Agent agent = AgentFixtures.available();
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(paymentMethodRepository.findById(PaymentMethodFixtures.PAYMENT_METHOD_ID))
          .thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  paymentMethodService.update(
                      AgentFixtures.AGENT_ID, PaymentMethodFixtures.PAYMENT_METHOD_ID, request))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should update account name and save")
    void shouldUpdateAccountNameAndSave() {
      wireAuditTemplateSupplier();
      Agent agent = AgentFixtures.available();
      PaymentMethod paymentMethod = PaymentMethodFixtures.mpesaPersonal(AgentFixtures.AGENT_ID);
      PaymentMethodDetailResponse response = mock(PaymentMethodDetailResponse.class);
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(paymentMethodRepository.findById(PaymentMethodFixtures.PAYMENT_METHOD_ID))
          .thenReturn(Optional.of(paymentMethod));
      when(paymentMethodRepository.save(paymentMethod)).thenReturn(paymentMethod);
      when(paymentMethodMapper.toDetailResponse(paymentMethod)).thenReturn(response);

      PaymentMethodDetailResponse result =
          paymentMethodService.update(
              AgentFixtures.AGENT_ID, PaymentMethodFixtures.PAYMENT_METHOD_ID, request);

      assertThat(result).isEqualTo(response);
      assertThat(paymentMethod.getAccountName()).isEqualTo("New Name");
      verify(paymentMethodRepository).save(paymentMethod);
    }
  }
}
