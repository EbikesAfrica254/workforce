package com.ebikes.workforce.services.agents;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.workforce.constants.EventConstants.EventTypes;
import com.ebikes.workforce.constants.EventConstants.RoutingKeys;
import com.ebikes.workforce.database.entities.PaymentMethod;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.PaymentMethodRepository;
import com.ebikes.workforce.dtos.requests.paymentmethods.CreatePaymentMethodRequest;
import com.ebikes.workforce.dtos.requests.paymentmethods.UpdatePaymentMethodRequest;
import com.ebikes.workforce.dtos.responses.paymentmethods.PaymentMethodDetailResponse;
import com.ebikes.workforce.dtos.responses.paymentmethods.PaymentMethodSummaryResponse;
import com.ebikes.workforce.enums.PaymentMethodType;
import com.ebikes.workforce.enums.ResponseCode;
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.exceptions.ResourceNotFoundException;
import com.ebikes.workforce.exceptions.ValidationException;
import com.ebikes.workforce.mappers.PaymentMethodMapper;
import com.ebikes.workforce.publishers.AuditEventPublisher;
import com.ebikes.workforce.support.audit.AuditMetadataBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class PaymentMethodService {

  private final AgentRepository agentRepository;
  private final AuditEventPublisher auditEventPublisher;
  private final PaymentMethodMapper paymentMethodMapper;
  private final PaymentMethodRepository paymentMethodRepository;

  private final Map<PaymentMethodType, Consumer<CreatePaymentMethodRequest>> typeValidators =
      buildTypeValidators();

  @Transactional
  public PaymentMethodDetailResponse create(UUID agentId, CreatePaymentMethodRequest request) {
    log.info(
        "Adding payment method: agentId={}, paymentMethodType={}",
        agentId,
        request.paymentMethodType());

    requireAgentById(agentId);
    validateTypeFields(request);

    if (request.isPrimary()) {
      paymentMethodRepository
          .findByAgentIdAndIsPrimaryTrue(agentId)
          .ifPresent(
              existing -> {
                existing.unmarkPrimary();
                paymentMethodRepository.save(existing);
              });
    }

    PaymentMethod paymentMethod = buildPaymentMethod(agentId, request);
    paymentMethod = paymentMethodRepository.save(paymentMethod);

    auditEventPublisher.publishSuccess(
        paymentMethod.getId(),
        paymentMethod.getClass().getSimpleName(),
        EventTypes.Workforce.AGENT_UPDATED,
        AuditMetadataBuilder.forPaymentMethod(paymentMethod, Map.of("action", "created")),
        RoutingKeys.WORKFORCE_PAYMENT_METHOD_AUDIT);

    log.info(
        "Payment method added: paymentMethodId={}, agentId={}, isPrimary={}",
        paymentMethod.getId(),
        agentId,
        paymentMethod.getIsPrimary());

    return paymentMethodMapper.toDetailResponse(paymentMethod);
  }

  @Transactional
  public void delete(UUID agentId, UUID paymentMethodId) {
    log.info("Removing payment method: agentId={}, paymentMethodId={}", agentId, paymentMethodId);

    PaymentMethod paymentMethod = requireByIdAndAgentId(paymentMethodId, agentId);

    if (Boolean.TRUE.equals(paymentMethod.getIsPrimary())) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Cannot remove the primary payment method. Designate another payment method as primary"
              + " first");
    }

    paymentMethodRepository.delete(paymentMethod);

    log.info("Payment method removed: paymentMethodId={}, agentId={}", paymentMethodId, agentId);
  }

  @Transactional(readOnly = true)
  public List<PaymentMethodSummaryResponse> findByAgentId(UUID agentId) {
    requireAgentById(agentId);
    return paymentMethodRepository.findByAgentId(agentId).stream()
        .map(pm -> paymentMethodMapper.toSummaryResponse(pm, maskIdentifier(pm)))
        .toList();
  }

  @Transactional(readOnly = true)
  public PaymentMethodDetailResponse getById(UUID agentId, UUID paymentMethodId) {
    return paymentMethodMapper.toDetailResponse(requireByIdAndAgentId(paymentMethodId, agentId));
  }

  @Transactional
  public PaymentMethodDetailResponse setPrimary(UUID agentId, UUID paymentMethodId) {
    log.info(
        "Setting primary payment method: agentId={}, paymentMethodId={}", agentId, paymentMethodId);

    PaymentMethod incoming = requireByIdAndAgentId(paymentMethodId, agentId);

    if (Boolean.TRUE.equals(incoming.getIsPrimary())) {
      return paymentMethodMapper.toDetailResponse(incoming);
    }

    paymentMethodRepository
        .findByAgentIdAndIsPrimaryTrue(agentId)
        .ifPresent(
            existing -> {
              existing.unmarkPrimary();
              paymentMethodRepository.save(existing);
            });

    incoming.markPrimary();
    incoming = paymentMethodRepository.save(incoming);

    auditEventPublisher.publishSuccess(
        incoming.getId(),
        incoming.getClass().getSimpleName(),
        EventTypes.Workforce.AGENT_UPDATED,
        AuditMetadataBuilder.forPaymentMethod(incoming, Map.of("action", "primary-designated")),
        RoutingKeys.WORKFORCE_PAYMENT_METHOD_AUDIT);

    log.info(
        "Primary payment method updated: paymentMethodId={}, agentId={}", paymentMethodId, agentId);

    return paymentMethodMapper.toDetailResponse(incoming);
  }

  @Transactional
  public PaymentMethodDetailResponse update(
      UUID agentId, UUID paymentMethodId, UpdatePaymentMethodRequest request) {
    log.info("Updating payment method: agentId={}, paymentMethodId={}", agentId, paymentMethodId);

    PaymentMethod paymentMethod = requireByIdAndAgentId(paymentMethodId, agentId);
    paymentMethod.updateAccountName(request.accountName());
    paymentMethod = paymentMethodRepository.save(paymentMethod);

    log.info("Payment method updated: paymentMethodId={}, agentId={}", paymentMethodId, agentId);

    return paymentMethodMapper.toDetailResponse(paymentMethod);
  }

  private PaymentMethod buildPaymentMethod(UUID agentId, CreatePaymentMethodRequest request) {
    return switch (request.paymentMethodType()) {
      case MPESA_PAYBILL ->
          PaymentMethod.mpesaPaybill(
              agentId, request.accountName(), request.accountReference(), request.paybillNumber());
      case MPESA_PERSONAL ->
          PaymentMethod.mpesaPersonal(agentId, request.accountName(), request.phoneNumber());
      case MPESA_TILL ->
          PaymentMethod.mpesaTill(agentId, request.accountName(), request.tillNumber());
    };
  }

  private String maskIdentifier(PaymentMethod paymentMethod) {
    String raw =
        switch (paymentMethod.getPaymentMethodType()) {
          case MPESA_PERSONAL -> paymentMethod.getPhoneNumber();
          case MPESA_TILL -> paymentMethod.getTillNumber();
          case MPESA_PAYBILL -> paymentMethod.getPaybillNumber();
        };

    if (raw == null || raw.length() < 4) {
      return "****";
    }

    return "*".repeat(raw.length() - 4) + raw.substring(raw.length() - 4);
  }

  private void validateTypeFields(CreatePaymentMethodRequest request) {
    requireNonNull(request, "request");
    requireNonNull(request.paymentMethodType(), "paymentMethodType");

    Consumer<CreatePaymentMethodRequest> validator =
        typeValidators.get(request.paymentMethodType());

    if (validator == null) {
      throw new ValidationException(
          ResponseCode.INVALID_ARGUMENTS,
          "Unsupported payment method type",
          "paymentMethodType",
          request.paymentMethodType());
    }

    validator.accept(request);
  }

  private Map<PaymentMethodType, Consumer<CreatePaymentMethodRequest>> buildTypeValidators() {
    Map<PaymentMethodType, Consumer<CreatePaymentMethodRequest>> validators =
        new EnumMap<>(PaymentMethodType.class);

    validators.put(
        PaymentMethodType.MPESA_PERSONAL,
        request ->
            requireNotBlank(request.phoneNumber(), "phoneNumber", request.paymentMethodType()));

    validators.put(
        PaymentMethodType.MPESA_TILL,
        request ->
            requireNotBlank(request.tillNumber(), "tillNumber", request.paymentMethodType()));

    validators.put(
        PaymentMethodType.MPESA_PAYBILL,
        request -> {
          requireNotBlank(request.paybillNumber(), "paybillNumber", request.paymentMethodType());
          requireNotBlank(
              request.accountReference(), "accountReference", request.paymentMethodType());
        });

    return Map.copyOf(validators);
  }

  private void requireNotBlank(String value, String field, PaymentMethodType type) {
    if (value == null || value.isBlank()) {
      throw new ValidationException(
          ResponseCode.INVALID_ARGUMENTS, field + " is required for " + type, field, value);
    }
  }

  private void requireNonNull(Object value, String field) {
    if (value == null) {
      throw new ValidationException(
          ResponseCode.INVALID_ARGUMENTS, field + " is required", field, null);
    }
  }

  private void requireAgentById(UUID agentId) {
    agentRepository
        .findById(agentId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND, "Agent with ID " + agentId + " not found"));
  }

  private PaymentMethod requireByIdAndAgentId(UUID paymentMethodId, UUID agentId) {
    return paymentMethodRepository
        .findById(paymentMethodId)
        .filter(pm -> pm.getAgentId().equals(agentId))
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND,
                    "Payment method with ID "
                        + paymentMethodId
                        + " not found for agent "
                        + agentId));
  }
}
