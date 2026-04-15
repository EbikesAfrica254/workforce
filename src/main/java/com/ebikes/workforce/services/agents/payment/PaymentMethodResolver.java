package com.ebikes.workforce.services.agents.payment;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.ebikes.workforce.database.entities.PaymentMethod;
import com.ebikes.workforce.dtos.requests.paymentmethods.CreatePaymentMethodRequest;
import com.ebikes.workforce.enums.PaymentMethodType;
import com.ebikes.workforce.enums.ResponseCode;
import com.ebikes.workforce.exceptions.ValidationException;

@Component
public class PaymentMethodResolver {

  public PaymentMethod build(UUID agentId, CreatePaymentMethodRequest request) {
    validate(request);

    PaymentMethod paymentMethod =
        switch (request.paymentMethodType()) {
          case MPESA_PAYBILL ->
              PaymentMethod.mpesaPaybill(
                  agentId,
                  request.accountName(),
                  request.accountReference(),
                  request.paybillNumber());
          case MPESA_PERSONAL ->
              PaymentMethod.mpesaPersonal(agentId, request.accountName(), request.phoneNumber());
          case MPESA_TILL ->
              PaymentMethod.mpesaTill(agentId, request.accountName(), request.tillNumber());
        };

    if (request.isPrimary()) {
      paymentMethod.markPrimary();
    }

    return paymentMethod;
  }

  public String maskIdentifier(PaymentMethod paymentMethod) {
    String raw =
        switch (paymentMethod.getPaymentMethodType()) {
          case MPESA_PAYBILL -> paymentMethod.getPaybillNumber();
          case MPESA_PERSONAL -> paymentMethod.getPhoneNumber();
          case MPESA_TILL -> paymentMethod.getTillNumber();
        };

    if (raw == null || raw.length() < 4) {
      return "****";
    }

    return "*".repeat(raw.length() - 4) + raw.substring(raw.length() - 4);
  }

  private void validate(CreatePaymentMethodRequest request) {
    switch (request.paymentMethodType()) {
      case MPESA_PAYBILL -> {
        requireNotBlank(
            request.accountReference(), "accountReference", PaymentMethodType.MPESA_PAYBILL);
        requireNotBlank(request.paybillNumber(), "paybillNumber", PaymentMethodType.MPESA_PAYBILL);
      }
      case MPESA_PERSONAL ->
          requireNotBlank(request.phoneNumber(), "phoneNumber", PaymentMethodType.MPESA_PERSONAL);
      case MPESA_TILL ->
          requireNotBlank(request.tillNumber(), "tillNumber", PaymentMethodType.MPESA_TILL);
    }
  }

  private void requireNotBlank(String value, String field, PaymentMethodType type) {
    if (value == null || value.isBlank()) {
      throw new ValidationException(
          ResponseCode.INVALID_ARGUMENTS, field + " is required for " + type, field, value);
    }
  }
}
