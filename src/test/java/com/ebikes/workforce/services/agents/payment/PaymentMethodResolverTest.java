package com.ebikes.workforce.services.agents.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ebikes.workforce.database.entities.PaymentMethod;
import com.ebikes.workforce.dtos.requests.paymentmethods.CreatePaymentMethodRequest;
import com.ebikes.workforce.enums.PaymentMethodType;
import com.ebikes.workforce.exceptions.ValidationException;
import com.ebikes.workforce.support.fixtures.AgentFixtures;
import com.ebikes.workforce.support.fixtures.PaymentMethodFixtures;

@DisplayName("PaymentMethodResolver")
class PaymentMethodResolverTest {

  private final PaymentMethodResolver resolver = new PaymentMethodResolver();

  @Nested
  @DisplayName("build")
  class Build {

    @Test
    @DisplayName("should build MPESA_PAYBILL payment method")
    void shouldBuildMpesaPaybill() {
      CreatePaymentMethodRequest request =
          new CreatePaymentMethodRequest(
              "ACC-001",
              false,
              "Test Business",
              "123456",
              PaymentMethodType.MPESA_PAYBILL,
              null,
              null);

      PaymentMethod result = resolver.build(AgentFixtures.AGENT_ID, request);

      assertThat(result.getPaymentMethodType()).isEqualTo(PaymentMethodType.MPESA_PAYBILL);
      assertThat(result.getAccountName()).isEqualTo("Test Business");
      assertThat(result.getAccountReference()).isEqualTo("ACC-001");
      assertThat(result.getPaybillNumber()).isEqualTo("123456");
      assertThat(result.getIsPrimary()).isFalse();
    }

    @Test
    @DisplayName("should build MPESA_PERSONAL payment method")
    void shouldBuildMpesaPersonal() {
      CreatePaymentMethodRequest request =
          new CreatePaymentMethodRequest(
              null,
              false,
              "Test Agent",
              null,
              PaymentMethodType.MPESA_PERSONAL,
              "+254700000001",
              null);

      PaymentMethod result = resolver.build(AgentFixtures.AGENT_ID, request);

      assertThat(result.getPaymentMethodType()).isEqualTo(PaymentMethodType.MPESA_PERSONAL);
      assertThat(result.getPhoneNumber()).isEqualTo("+254700000001");
      assertThat(result.getIsPrimary()).isFalse();
    }

    @Test
    @DisplayName("should build MPESA_TILL payment method")
    void shouldBuildMpesaTill() {
      CreatePaymentMethodRequest request =
          new CreatePaymentMethodRequest(
              null, false, "Test Till", null, PaymentMethodType.MPESA_TILL, null, "987654");

      PaymentMethod result = resolver.build(AgentFixtures.AGENT_ID, request);

      assertThat(result.getPaymentMethodType()).isEqualTo(PaymentMethodType.MPESA_TILL);
      assertThat(result.getTillNumber()).isEqualTo("987654");
      assertThat(result.getIsPrimary()).isFalse();
    }

    @Test
    @DisplayName("should mark payment method as primary when isPrimary is true")
    void shouldMarkPrimaryWhenRequested() {
      CreatePaymentMethodRequest request =
          new CreatePaymentMethodRequest(
              null,
              true,
              "Test Agent",
              null,
              PaymentMethodType.MPESA_PERSONAL,
              "+254700000001",
              null);

      PaymentMethod result = resolver.build(AgentFixtures.AGENT_ID, request);

      assertThat(result.getIsPrimary()).isTrue();
    }

    @Test
    @DisplayName(
        "should throw ValidationException when accountReference is blank for MPESA_PAYBILL")
    void shouldThrowWhenAccountReferenceBlank() {
      CreatePaymentMethodRequest request =
          new CreatePaymentMethodRequest(
              "  ", false, "Test Business", "123456", PaymentMethodType.MPESA_PAYBILL, null, null);

      assertThatThrownBy(() -> resolver.build(AgentFixtures.AGENT_ID, request))
          .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("should throw ValidationException when paybillNumber is null for MPESA_PAYBILL")
    void shouldThrowWhenPaybillNumberNull() {
      CreatePaymentMethodRequest request =
          new CreatePaymentMethodRequest(
              "ACC-001", false, "Test Business", null, PaymentMethodType.MPESA_PAYBILL, null, null);

      assertThatThrownBy(() -> resolver.build(AgentFixtures.AGENT_ID, request))
          .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("should throw ValidationException when phoneNumber is blank for MPESA_PERSONAL")
    void shouldThrowWhenPhoneNumberBlank() {
      CreatePaymentMethodRequest request =
          new CreatePaymentMethodRequest(
              null, false, "Test Agent", null, PaymentMethodType.MPESA_PERSONAL, "  ", null);

      assertThatThrownBy(() -> resolver.build(AgentFixtures.AGENT_ID, request))
          .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("should throw ValidationException when tillNumber is null for MPESA_TILL")
    void shouldThrowWhenTillNumberNull() {
      CreatePaymentMethodRequest request =
          new CreatePaymentMethodRequest(
              null, false, "Test Till", null, PaymentMethodType.MPESA_TILL, null, null);

      assertThatThrownBy(() -> resolver.build(AgentFixtures.AGENT_ID, request))
          .isInstanceOf(ValidationException.class);
    }
  }

  @Nested
  @DisplayName("maskIdentifier")
  class MaskIdentifier {

    @Test
    @DisplayName("should mask MPESA_PAYBILL paybill number")
    void shouldMaskPaybillNumber() {
      PaymentMethod pm = PaymentMethodFixtures.mpesaPaybill(AgentFixtures.AGENT_ID);

      String masked = resolver.maskIdentifier(pm);

      assertThat(masked).isEqualTo("**3456");
    }

    @Test
    @DisplayName("should mask MPESA_PERSONAL phone number")
    void shouldMaskPhoneNumber() {
      PaymentMethod pm = PaymentMethodFixtures.mpesaPersonal(AgentFixtures.AGENT_ID);

      String masked = resolver.maskIdentifier(pm);

      assertThat(masked).endsWith("0001").startsWith("*");
    }

    @Test
    @DisplayName("should mask MPESA_TILL till number")
    void shouldMaskTillNumber() {
      PaymentMethod pm = PaymentMethodFixtures.mpesaTill(AgentFixtures.AGENT_ID);

      String masked = resolver.maskIdentifier(pm);

      assertThat(masked).isEqualTo("**7654");
    }

    @Test
    @DisplayName("should return **** when identifier is shorter than 4 characters")
    void shouldReturnPlaceholderWhenTooShort() {
      PaymentMethod pm = PaymentMethod.mpesaTill(AgentFixtures.AGENT_ID, "Test", "123");

      String masked = resolver.maskIdentifier(pm);

      assertThat(masked).isEqualTo("****");
    }
  }
}
