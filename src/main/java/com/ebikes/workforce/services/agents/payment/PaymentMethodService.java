package com.ebikes.workforce.services.agents.payment;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.workforce.constants.EventConstants.DomainEvents;
import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.PaymentMethod;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.PaymentMethodRepository;
import com.ebikes.workforce.dtos.requests.paymentmethods.CreatePaymentMethodRequest;
import com.ebikes.workforce.dtos.requests.paymentmethods.UpdatePaymentMethodRequest;
import com.ebikes.workforce.dtos.responses.paymentmethods.PaymentMethodDetailResponse;
import com.ebikes.workforce.dtos.responses.paymentmethods.PaymentMethodSummaryResponse;
import com.ebikes.workforce.enums.ResponseCode;
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.exceptions.ResourceNotFoundException;
import com.ebikes.workforce.mappers.PaymentMethodMapper;
import com.ebikes.workforce.services.agents.AccessPolicy;
import com.ebikes.workforce.services.agents.AgentScopedService;
import com.ebikes.workforce.support.audit.AuditTemplate;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PaymentMethodService extends AgentScopedService {

  private final AccessPolicy accessPolicy;
  private final AuditTemplate auditTemplate;
  private final PaymentMethodMapper paymentMethodMapper;
  private final PaymentMethodRepository paymentMethodRepository;
  private final PaymentMethodResolver paymentMethodResolver;

  public PaymentMethodService(
      AccessPolicy accessPolicy,
      AgentRepository agentRepository,
      AuditTemplate auditTemplate,
      PaymentMethodMapper paymentMethodMapper,
      PaymentMethodRepository paymentMethodRepository,
      PaymentMethodResolver paymentMethodResolver) {
    super(agentRepository);
    this.accessPolicy = accessPolicy;
    this.auditTemplate = auditTemplate;
    this.paymentMethodMapper = paymentMethodMapper;
    this.paymentMethodRepository = paymentMethodRepository;
    this.paymentMethodResolver = paymentMethodResolver;
  }

  @Transactional
  public PaymentMethodDetailResponse create(UUID agentId, CreatePaymentMethodRequest request) {
    log.info(
        "Creating payment method: agentId={}, paymentMethodType={}",
        agentId,
        request.paymentMethodType());

    Agent agent = requireAgentById(agentId);
    accessPolicy.owns(agent);

    if (request.isPrimary()) {
      clearPrimary(agentId);
    }

    PaymentMethod paymentMethod = paymentMethodResolver.build(agentId, request);

    PaymentMethod saved =
        auditTemplate.execute(
            paymentMethod,
            null,
            DomainEvents.PaymentMethod.CREATED,
            () -> paymentMethodRepository.save(paymentMethod));

    log.info(
        "Payment method created: paymentMethodId={}, agentId={}, isPrimary={}",
        saved.getId(),
        agentId,
        saved.getIsPrimary());

    return paymentMethodMapper.toDetailResponse(saved);
  }

  @Transactional
  public void delete(UUID agentId, UUID paymentMethodId) {
    log.info("Deleting payment method: agentId={}, paymentMethodId={}", agentId, paymentMethodId);

    Agent agent = requireAgentById(agentId);
    accessPolicy.owns(agent);

    PaymentMethod paymentMethod = requireByIdAndAgentId(paymentMethodId, agentId);

    if (Boolean.TRUE.equals(paymentMethod.getIsPrimary())) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Cannot remove the primary payment method. Designate another payment method as primary"
              + " first");
    }

    auditTemplate.execute(
        paymentMethod,
        null,
        DomainEvents.PaymentMethod.DELETED,
        () -> paymentMethodRepository.delete(paymentMethod));

    log.info("Payment method deleted: paymentMethodId={}, agentId={}", paymentMethodId, agentId);
  }

  @Transactional(readOnly = true)
  public List<PaymentMethodSummaryResponse> findByAgentId(UUID agentId) {
    requireAgentById(agentId);

    return paymentMethodRepository.findByAgentId(agentId).stream()
        .map(
            pm ->
                paymentMethodMapper.toSummaryResponse(pm, paymentMethodResolver.maskIdentifier(pm)))
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

    Agent agent = requireAgentById(agentId);
    accessPolicy.owns(agent);

    PaymentMethod paymentMethod = requireByIdAndAgentId(paymentMethodId, agentId);

    if (Boolean.TRUE.equals(paymentMethod.getIsPrimary())) {
      return paymentMethodMapper.toDetailResponse(paymentMethod);
    }

    clearPrimary(agentId);
    paymentMethod.markPrimary();

    PaymentMethod primary =
        auditTemplate.execute(
            paymentMethod,
            null,
            DomainEvents.PaymentMethod.PRIMARY_SET,
            () -> paymentMethodRepository.save(paymentMethod));

    log.info(
        "Primary payment method set: paymentMethodId={}, agentId={}", paymentMethodId, agentId);

    return paymentMethodMapper.toDetailResponse(primary);
  }

  @Transactional
  public PaymentMethodDetailResponse update(
      UUID agentId, UUID paymentMethodId, UpdatePaymentMethodRequest request) {
    log.info("Updating payment method: agentId={}, paymentMethodId={}", agentId, paymentMethodId);

    Agent agent = requireAgentById(agentId);
    accessPolicy.owns(agent);

    PaymentMethod paymentMethod = requireByIdAndAgentId(paymentMethodId, agentId);
    paymentMethod.updateAccountName(request.accountName());

    PaymentMethod updated =
        auditTemplate.execute(
            paymentMethod,
            null,
            DomainEvents.PaymentMethod.UPDATED,
            () -> paymentMethodRepository.save(paymentMethod));

    log.info("Payment method updated: paymentMethodId={}, agentId={}", paymentMethodId, agentId);

    return paymentMethodMapper.toDetailResponse(updated);
  }

  private void clearPrimary(UUID agentId) {
    paymentMethodRepository
        .findByAgentIdAndIsPrimaryTrue(agentId)
        .ifPresent(
            existing -> {
              existing.unmarkPrimary();
              paymentMethodRepository.save(existing);
            });
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
