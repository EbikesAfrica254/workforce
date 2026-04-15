package com.ebikes.workforce.services.agents.certification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.Certification;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.CertificationRepository;
import com.ebikes.workforce.dtos.requests.certifications.CreateCertificationRequest;
import com.ebikes.workforce.dtos.responses.certifications.CertificationDetailResponse;
import com.ebikes.workforce.dtos.responses.certifications.CertificationSummaryResponse;
import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.enums.CertificationType;
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.exceptions.ResourceNotFoundException;
import com.ebikes.workforce.mappers.CertificationMapper;
import com.ebikes.workforce.support.audit.AuditTemplate;
import com.ebikes.workforce.support.audit.ThrowingSupplier;
import com.ebikes.workforce.support.context.ExecutionContext;
import com.ebikes.workforce.support.fixtures.AgentFixtures;
import com.ebikes.workforce.support.fixtures.CertificationFixtures;
import com.ebikes.workforce.support.infrastructure.WithExecutionContext;

@DisplayName("CertificationService")
@ExtendWith({MockitoExtension.class, WithExecutionContext.class})
class CertificationServiceTest {

  @Mock private AgentRepository agentRepository;
  @Mock private AuditTemplate auditTemplate;
  @Mock private CertificationMapper certificationMapper;
  @Mock private CertificationRepository certificationRepository;

  @InjectMocks private CertificationService certificationService;

  @SuppressWarnings("unchecked")
  private void wireAuditTemplate() {
    doAnswer(
            invocation -> {
              ThrowingSupplier<?, ?> operation = invocation.getArgument(3);
              return operation.get();
            })
        .when(auditTemplate)
        .execute(any(), any(), any(), any(ThrowingSupplier.class));
  }

  private CreateCertificationRequest request(CertificationType type) {
    return new CreateCertificationRequest(
        type,
        LocalDate.now().plusYears(1),
        LocalDate.now().minusMonths(1),
        "Kenya National Police Service",
        null,
        "REF-001");
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("should throw ResourceNotFoundException when agent not found")
    void shouldThrowWhenAgentNotFound() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.empty());

      CreateCertificationRequest testRequest = request(CertificationType.GOOD_CONDUCT_CERTIFICATE);
      assertThatThrownBy(() -> certificationService.create(AgentFixtures.AGENT_ID, testRequest))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName(
        "should throw BusinessRuleException when certification type not applicable to capability"
            + " class")
    void shouldThrowWhenCertificationTypeNotApplicable() {
      Agent agent = AgentFixtures.withCapabilityClass(CapabilityClass.BICYCLE_RIDER);
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));

      CreateCertificationRequest testRequest =
          request(CertificationType.NTSA_DRIVING_LICENSE_CLASS_BCE);
      assertThatThrownBy(() -> certificationService.create(AgentFixtures.AGENT_ID, testRequest))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should throw BusinessRuleException when executed in system context")
    void shouldThrowWhenSystemContext() {
      ExecutionContext.setSystem();
      Agent agent = AgentFixtures.withCapabilityClass(CapabilityClass.BICYCLE_RIDER);
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));

      CreateCertificationRequest testRequest = request(CertificationType.GOOD_CONDUCT_CERTIFICATE);
      assertThatThrownBy(() -> certificationService.create(AgentFixtures.AGENT_ID, testRequest))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should save and return response for a required certification type")
    void shouldCreateForRequiredCertificationType() {
      wireAuditTemplate();
      Agent agent = AgentFixtures.withCapabilityClass(CapabilityClass.BICYCLE_RIDER);
      Certification saved =
          CertificationFixtures.withType(
              AgentFixtures.AGENT_ID, CertificationType.GOOD_CONDUCT_CERTIFICATE);
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(certificationRepository.save(any(Certification.class))).thenReturn(saved);
      when(certificationMapper.toDetailResponse(saved))
          .thenReturn(mock(CertificationDetailResponse.class));

      CertificationDetailResponse result =
          certificationService.create(
              AgentFixtures.AGENT_ID, request(CertificationType.GOOD_CONDUCT_CERTIFICATE));

      verify(certificationRepository).save(any(Certification.class));
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName(
        "should save for supplementary type DEFENSIVE_DRIVING regardless of capability class")
    void shouldCreateForSupplementaryTypeDefensiveDriving() {
      wireAuditTemplate();
      Agent agent = AgentFixtures.withCapabilityClass(CapabilityClass.BICYCLE_RIDER);
      Certification saved =
          CertificationFixtures.withType(
              AgentFixtures.AGENT_ID, CertificationType.DEFENSIVE_DRIVING);
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(certificationRepository.save(any(Certification.class))).thenReturn(saved);
      when(certificationMapper.toDetailResponse(saved))
          .thenReturn(mock(CertificationDetailResponse.class));

      certificationService.create(
          AgentFixtures.AGENT_ID, request(CertificationType.DEFENSIVE_DRIVING));

      verify(certificationRepository).save(any(Certification.class));
    }

    @Test
    @DisplayName("should save for supplementary type FIRST_AID regardless of capability class")
    void shouldCreateForSupplementaryTypeFirstAid() {
      wireAuditTemplate();
      Agent agent = AgentFixtures.withCapabilityClass(CapabilityClass.BICYCLE_RIDER);
      Certification saved =
          CertificationFixtures.withType(AgentFixtures.AGENT_ID, CertificationType.FIRST_AID);
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.of(agent));
      when(certificationRepository.save(any(Certification.class))).thenReturn(saved);
      when(certificationMapper.toDetailResponse(saved))
          .thenReturn(mock(CertificationDetailResponse.class));

      certificationService.create(AgentFixtures.AGENT_ID, request(CertificationType.FIRST_AID));

      verify(certificationRepository).save(any(Certification.class));
    }
  }

  @Nested
  @DisplayName("getById")
  class GetById {

    @Test
    @DisplayName("should throw ResourceNotFoundException when agent not found")
    void shouldThrowWhenAgentNotFound() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  certificationService.getById(
                      AgentFixtures.AGENT_ID, CertificationFixtures.CERTIFICATION_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when certification not found")
    void shouldThrowWhenCertificationNotFound() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID))
          .thenReturn(Optional.of(AgentFixtures.available()));
      when(certificationRepository.findById(CertificationFixtures.CERTIFICATION_ID))
          .thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  certificationService.getById(
                      AgentFixtures.AGENT_ID, CertificationFixtures.CERTIFICATION_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName(
        "should throw ResourceNotFoundException when certification belongs to a different agent")
    void shouldThrowWhenCertificationBelongsToDifferentAgent() {
      UUID otherAgentId = UUID.fromString("00000000-0000-0000-0000-000000000099");
      Certification certification =
          CertificationFixtures.withId(otherAgentId, CertificationFixtures.CERTIFICATION_ID);
      when(agentRepository.findById(AgentFixtures.AGENT_ID))
          .thenReturn(Optional.of(AgentFixtures.available()));
      when(certificationRepository.findById(CertificationFixtures.CERTIFICATION_ID))
          .thenReturn(Optional.of(certification));

      assertThatThrownBy(
              () ->
                  certificationService.getById(
                      AgentFixtures.AGENT_ID, CertificationFixtures.CERTIFICATION_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should return mapped response when agent and certification found")
    void shouldReturnMappedResponse() {
      Certification certification =
          CertificationFixtures.withId(
              AgentFixtures.AGENT_ID, CertificationFixtures.CERTIFICATION_ID);
      when(agentRepository.findById(AgentFixtures.AGENT_ID))
          .thenReturn(Optional.of(AgentFixtures.available()));
      when(certificationRepository.findById(CertificationFixtures.CERTIFICATION_ID))
          .thenReturn(Optional.of(certification));
      when(certificationMapper.toDetailResponse(certification))
          .thenReturn(mock(CertificationDetailResponse.class));

      CertificationDetailResponse result =
          certificationService.getById(
              AgentFixtures.AGENT_ID, CertificationFixtures.CERTIFICATION_ID);

      assertThat(result).isNotNull();
    }
  }

  @Nested
  @DisplayName("getByAgentId")
  class GetByAgentId {

    @Test
    @DisplayName("should throw ResourceNotFoundException when agent not found")
    void shouldThrowWhenAgentNotFound() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> certificationService.getByAgentId(AgentFixtures.AGENT_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should return empty list when agent has no certifications")
    void shouldReturnEmptyListWhenNoCertifications() {
      when(agentRepository.findById(AgentFixtures.AGENT_ID))
          .thenReturn(Optional.of(AgentFixtures.available()));
      when(certificationRepository.findByAgentId(AgentFixtures.AGENT_ID)).thenReturn(List.of());

      List<CertificationSummaryResponse> result =
          certificationService.getByAgentId(AgentFixtures.AGENT_ID);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return mapped summaries for all certifications")
    void shouldReturnMappedSummaries() {
      Certification c1 = CertificationFixtures.valid(AgentFixtures.AGENT_ID);
      Certification c2 =
          CertificationFixtures.withType(
              AgentFixtures.AGENT_ID, CertificationType.DEFENSIVE_DRIVING);
      when(agentRepository.findById(AgentFixtures.AGENT_ID))
          .thenReturn(Optional.of(AgentFixtures.available()));
      when(certificationRepository.findByAgentId(AgentFixtures.AGENT_ID))
          .thenReturn(List.of(c1, c2));
      when(certificationMapper.toSummaryResponse(any(Certification.class)))
          .thenReturn(mock(CertificationSummaryResponse.class));

      List<CertificationSummaryResponse> result =
          certificationService.getByAgentId(AgentFixtures.AGENT_ID);

      assertThat(result).hasSize(2);
    }
  }
}
