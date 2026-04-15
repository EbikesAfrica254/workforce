package com.ebikes.workforce.support.fixtures;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.ebikes.workforce.dtos.requests.agents.CreateAgentRequest;
import com.ebikes.workforce.dtos.requests.agents.LiftSuspensionRequest;
import com.ebikes.workforce.dtos.requests.agents.SuspendAgentRequest;
import com.ebikes.workforce.dtos.requests.agents.UpdateAgentRequest;
import com.ebikes.workforce.dtos.requests.agents.UpdateLocationRequest;
import com.ebikes.workforce.dtos.requests.certifications.CreateCertificationRequest;
import com.ebikes.workforce.dtos.requests.documents.DocumentUploadInfo;
import com.ebikes.workforce.dtos.requests.paymentmethods.CreatePaymentMethodRequest;
import com.ebikes.workforce.dtos.requests.paymentmethods.UpdatePaymentMethodRequest;
import com.ebikes.workforce.dtos.responses.agents.AgentDetailResponse;
import com.ebikes.workforce.dtos.responses.agents.AgentSummaryResponse;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.enums.CertificationType;
import com.ebikes.workforce.enums.DocumentType;
import com.ebikes.workforce.enums.NationalIdType;
import com.ebikes.workforce.enums.PaymentMethodType;

public final class AgentDtoFixtures {

  public static final BigDecimal LATITUDE = new BigDecimal("1.28600");
  public static final BigDecimal LONGITUDE = new BigDecimal("36.82300");

  private AgentDtoFixtures() {}

  public static CreateAgentRequest createRequest() {
    return new CreateAgentRequest(
        null,
        CapabilityClass.BICYCLE_RIDER,
        List.of(nationalIdFrontUpload(), nationalIdBackUpload()),
        null,
        "Test",
        "Agent",
        CapabilityClass.BICYCLE_RIDER.getDefaultMaxConcurrentOrders(),
        AgentFixtures.NATIONAL_ID_NUMBER,
        NationalIdType.NATIONAL_ID,
        AgentFixtures.PHONE_NUMBER,
        AgentFixtures.USER_ID);
  }

  public static CreateCertificationRequest createCertificationRequest() {
    return new CreateCertificationRequest(
        CertificationType.GOOD_CONDUCT_CERTIFICATE,
        LocalDate.now().plusYears(1),
        LocalDate.now().minusMonths(1),
        "Kenya National Police Service",
        null,
        "GC-2024-001");
  }

  public static CreatePaymentMethodRequest createPaymentMethodRequest() {
    return new CreatePaymentMethodRequest(
        null,
        false,
        "Test Agent",
        null,
        PaymentMethodType.MPESA_PERSONAL,
        AgentFixtures.PHONE_NUMBER,
        null);
  }

  public static AgentDetailResponse detailResponse() {
    return new AgentDetailResponse(
        null,
        AvailabilityStatus.OFFLINE,
        CapabilityClass.BICYCLE_RIDER,
        0,
        OffsetDateTime.now(),
        SecurityFixtures.TEST_USER_ID,
        null,
        null,
        null,
        null,
        "Test",
        AgentFixtures.AGENT_ID,
        null,
        "Agent",
        null,
        CapabilityClass.BICYCLE_RIDER.getDefaultMaxConcurrentOrders(),
        AgentFixtures.NATIONAL_ID_NUMBER,
        NationalIdType.NATIONAL_ID,
        AgentFixtures.PHONE_NUMBER,
        null,
        OffsetDateTime.now(),
        SecurityFixtures.TEST_USER_ID,
        AgentFixtures.USER_ID,
        0L);
  }

  public static LiftSuspensionRequest liftSuspensionRequest() {
    return new LiftSuspensionRequest(null);
  }

  public static AgentSummaryResponse summaryResponse() {
    return new AgentSummaryResponse(
        AvailabilityStatus.OFFLINE,
        CapabilityClass.BICYCLE_RIDER,
        OffsetDateTime.now(),
        null,
        "Test",
        AgentFixtures.AGENT_ID,
        "Agent",
        AgentFixtures.PHONE_NUMBER,
        null);
  }

  public static SuspendAgentRequest suspendRequest() {
    return new SuspendAgentRequest(null, null, "Policy violation");
  }

  public static UpdateAgentRequest updateRequest() {
    return new UpdateAgentRequest(
        null,
        CapabilityClass.BICYCLE_RIDER,
        null,
        CapabilityClass.BICYCLE_RIDER.getDefaultMaxConcurrentOrders());
  }

  public static UpdateLocationRequest updateLocationRequest() {
    return new UpdateLocationRequest(LATITUDE, LONGITUDE);
  }

  public static UpdatePaymentMethodRequest updatePaymentMethodRequest() {
    return new UpdatePaymentMethodRequest("Updated Account Name");
  }

  private static DocumentUploadInfo nationalIdBackUpload() {
    return new DocumentUploadInfo(
        DocumentType.NATIONAL_ID_BACK,
        null,
        "national_id_back.jpg",
        102400L,
        "image/jpeg",
        "uploads/agents/national_id_back.jpg");
  }

  private static DocumentUploadInfo nationalIdFrontUpload() {
    return new DocumentUploadInfo(
        DocumentType.NATIONAL_ID_FRONT,
        null,
        "national_id_front.jpg",
        102400L,
        "image/jpeg",
        "uploads/agents/national_id_front.jpg");
  }
}
