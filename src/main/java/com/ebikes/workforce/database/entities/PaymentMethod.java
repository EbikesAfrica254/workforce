package com.ebikes.workforce.database.entities;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.ebikes.workforce.database.entities.bases.AuditableEntity;
import com.ebikes.workforce.enums.PaymentMethodType;
import com.ebikes.workforce.enums.ResponseCode;
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.support.audit.Auditable;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(
    name = "payment_methods",
    schema = "workforce",
    indexes = {@Index(name = "idx_payment_methods_agent", columnList = "agent_id")})
public class PaymentMethod extends AuditableEntity implements Auditable {

  @Column(name = "account_name", nullable = false)
  @NotBlank @Size(max = 255) private String accountName;

  @Column(name = "account_reference", length = 100)
  @Size(max = 100) private String accountReference;

  @Column(name = "agent_id", nullable = false, updatable = false)
  @NotNull private UUID agentId;

  @Builder.Default
  @Column(name = "is_primary", nullable = false)
  private Boolean isPrimary = Boolean.FALSE;

  @Column(name = "paybill_number", length = 20)
  @Size(max = 20) private String paybillNumber;

  @Column(name = "payment_method_type", nullable = false, updatable = false, length = 20)
  @Enumerated(EnumType.STRING)
  @NotNull private PaymentMethodType paymentMethodType;

  @Column(name = "phone_number", length = 20)
  @Pattern(regexp = "^\\+[1-9]\\d{1,14}$") @Size(max = 20) private String phoneNumber;

  @Column(name = "till_number", length = 20)
  @Size(max = 20) private String tillNumber;

  @Column(nullable = false)
  @Version
  private Long version;

  public static PaymentMethod mpesaPaybill(
      UUID agentId, String accountName, String accountReference, String paybillNumber) {
    return PaymentMethod.builder()
        .agentId(agentId)
        .accountName(accountName)
        .accountReference(accountReference)
        .paybillNumber(paybillNumber)
        .paymentMethodType(PaymentMethodType.MPESA_PAYBILL)
        .build();
  }

  public static PaymentMethod mpesaPersonal(UUID agentId, String accountName, String phoneNumber) {
    return PaymentMethod.builder()
        .agentId(agentId)
        .accountName(accountName)
        .phoneNumber(phoneNumber)
        .paymentMethodType(PaymentMethodType.MPESA_PERSONAL)
        .build();
  }

  public static PaymentMethod mpesaTill(UUID agentId, String accountName, String tillNumber) {
    return PaymentMethod.builder()
        .agentId(agentId)
        .accountName(accountName)
        .tillNumber(tillNumber)
        .paymentMethodType(PaymentMethodType.MPESA_TILL)
        .build();
  }

  public void markPrimary() {
    this.isPrimary = Boolean.TRUE;
  }

  @Override
  public Map<String, String> toAuditMetadata() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("agentId", this.agentId.toString());
    metadata.put("isPrimary", this.isPrimary.toString());
    metadata.put("paymentMethodType", this.paymentMethodType.name());
    return metadata;
  }

  public void unmarkPrimary() {
    if (Boolean.FALSE.equals(this.isPrimary)) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Cannot unmark a payment method that is not primary: " + this.getId());
    }
    this.isPrimary = Boolean.FALSE;
  }

  public void updateAccountName(String accountName) {
    this.accountName = accountName;
  }
}
