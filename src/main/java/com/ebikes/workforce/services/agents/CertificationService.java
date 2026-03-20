package com.ebikes.workforce.services.agents;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.workforce.constants.EventConstants.EventTypes;
import com.ebikes.workforce.constants.EventConstants.RoutingKeys;
import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.Certification;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.CertificationRepository;
import com.ebikes.workforce.dtos.requests.certifications.CreateCertificationRequest;
import com.ebikes.workforce.dtos.responses.certifications.CertificationDetailResponse;
import com.ebikes.workforce.dtos.responses.certifications.CertificationSummaryResponse;
import com.ebikes.workforce.enums.ResponseCode;
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.exceptions.ResourceNotFoundException;
import com.ebikes.workforce.mappers.CertificationMapper;
import com.ebikes.workforce.publishers.AuditEventPublisher;
import com.ebikes.workforce.support.audit.AuditMetadataBuilder;
import com.ebikes.workforce.support.context.ExecutionContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class CertificationService {

  private final AgentRepository agentRepository;
  private final AuditEventPublisher auditEventPublisher;
  private final CertificationMapper certificationMapper;
  private final CertificationRepository certificationRepository;

  @Transactional
  public CertificationDetailResponse create(UUID agentId, CreateCertificationRequest request) {
    log.info(
        "Recording certification: agentId={}, certificationType={}",
        agentId,
        request.certificationType());

    Agent agent = requireAgentById(agentId);

    if (!agent
            .getCapabilityClass()
            .getRequiredCertifications()
            .contains(request.certificationType())
        && !isOptionalCertification(request)) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_ARGUMENTS,
          String.format(
              "Certification type %s is not applicable to capability class %s",
              request.certificationType(), agent.getCapabilityClass()));
    }

    Certification certification =
        Certification.builder()
            .agentId(agentId)
            .certificationType(request.certificationType())
            .createdBy(ExecutionContext.getUserId())
            .expiresAt(request.expiresAt())
            .issuedAt(request.issuedAt())
            .issuedBy(request.issuedBy())
            .notes(request.notes())
            .referenceNumber(request.referenceNumber())
            .build();

    certification = certificationRepository.save(certification);

    auditEventPublisher.publishSuccess(
        certification.getId(),
        certification.getClass().getSimpleName().toUpperCase(),
        EventTypes.Certifications.CREATED,
        AuditMetadataBuilder.forCertification(certification),
        RoutingKeys.WORKFORCE_CERTIFICATION_AUDIT);

    log.info(
        "Certification recorded: certificationId={}, agentId={}, certificationType={}",
        certification.getId(),
        agentId,
        certification.getCertificationType());

    return certificationMapper.toDetailResponse(certification);
  }

  @Transactional(readOnly = true)
  public CertificationDetailResponse getById(UUID agentId, UUID certificationId) {
    requireAgentById(agentId);
    return certificationMapper.toDetailResponse(requireById(certificationId));
  }

  @Transactional(readOnly = true)
  public List<CertificationSummaryResponse> getByAgentId(UUID agentId) {
    requireAgentById(agentId);
    return certificationRepository.findByAgentId(agentId).stream()
        .map(certificationMapper::toSummaryResponse)
        .toList();
  }

  private boolean isOptionalCertification(CreateCertificationRequest request) {
    return switch (request.certificationType()) {
      case DEFENSIVE_DRIVING, FIRST_AID -> true;
      default -> false;
    };
  }

  private Agent requireAgentById(UUID agentId) {
    return agentRepository
        .findById(agentId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND, "Agent with ID " + agentId + " not found"));
  }

  private Certification requireById(UUID certificationId) {
    return certificationRepository
        .findById(certificationId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND,
                    "Certification with ID " + certificationId + " not found"));
  }
}
