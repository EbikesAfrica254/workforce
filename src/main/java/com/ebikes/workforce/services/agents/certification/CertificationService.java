package com.ebikes.workforce.services.agents.certification;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.workforce.constants.EventConstants.DomainEvents;
import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.Certification;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.CertificationRepository;
import com.ebikes.workforce.dtos.requests.certifications.CreateCertificationRequest;
import com.ebikes.workforce.dtos.responses.certifications.CertificationDetailResponse;
import com.ebikes.workforce.dtos.responses.certifications.CertificationSummaryResponse;
import com.ebikes.workforce.enums.CertificationType;
import com.ebikes.workforce.enums.ResponseCode;
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.exceptions.ResourceNotFoundException;
import com.ebikes.workforce.mappers.CertificationMapper;
import com.ebikes.workforce.services.agents.AgentScopedService;
import com.ebikes.workforce.support.audit.AuditTemplate;
import com.ebikes.workforce.support.context.ExecutionContext;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CertificationService extends AgentScopedService {

  private final AuditTemplate auditTemplate;
  private final CertificationMapper certificationMapper;
  private final CertificationRepository certificationRepository;

  public CertificationService(
      AgentRepository agentRepository,
      AuditTemplate auditTemplate,
      CertificationMapper certificationMapper,
      CertificationRepository certificationRepository) {
    super(agentRepository);
    this.auditTemplate = auditTemplate;
    this.certificationMapper = certificationMapper;
    this.certificationRepository = certificationRepository;
  }

  @Transactional
  public CertificationDetailResponse create(UUID agentId, CreateCertificationRequest request) {
    log.info(
        "Creating certification: agentId={}, certificationType={}",
        agentId,
        request.certificationType());

    Agent agent = requireAgentById(agentId);
    validateCertificationType(agent, request.certificationType());

    Certification certification =
        Certification.builder()
            .agentId(agentId)
            .certificationType(request.certificationType())
            .createdBy(requireActorId())
            .expiresAt(request.expiresAt())
            .issuedAt(request.issuedAt())
            .issuedBy(request.issuedBy())
            .notes(request.notes())
            .referenceNumber(request.referenceNumber())
            .build();

    Certification saved =
        auditTemplate.execute(
            certification,
            null,
            DomainEvents.Certifications.CREATED,
            () -> certificationRepository.save(certification));

    log.info(
        "Certification created: certificationId={}, agentId={}, certificationType={}",
        saved.getId(),
        agentId,
        saved.getCertificationType());

    return certificationMapper.toDetailResponse(saved);
  }

  @Transactional(readOnly = true)
  public CertificationDetailResponse getById(UUID agentId, UUID certificationId) {
    requireAgentById(agentId);
    return certificationMapper.toDetailResponse(requireByIdAndAgentId(certificationId, agentId));
  }

  @Transactional(readOnly = true)
  public List<CertificationSummaryResponse> getByAgentId(UUID agentId) {
    requireAgentById(agentId);

    return certificationRepository.findByAgentId(agentId).stream()
        .map(certificationMapper::toSummaryResponse)
        .toList();
  }

  private String requireActorId() {
    return switch (ExecutionContext.get()) {
      case ExecutionContext.SystemContext ignored ->
          throw new BusinessRuleException(
              ResponseCode.INVALID_STATE, "Certification creation requires a user context");
      case ExecutionContext.UserContext userContext -> userContext.userId();
    };
  }

  private Certification requireByIdAndAgentId(UUID certificationId, UUID agentId) {
    return certificationRepository
        .findById(certificationId)
        .filter(certification -> certification.getAgentId().equals(agentId))
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND,
                    "Certification with ID "
                        + certificationId
                        + " not found for agent "
                        + agentId));
  }

  private void validateCertificationType(Agent agent, CertificationType certificationType) {
    if (!agent.getCapabilityClass().getRequiredCertifications().contains(certificationType)
        && !isSupplementaryCertification(certificationType)) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_ARGUMENTS,
          String.format(
              "Certification type %s is not applicable to capability class %s",
              certificationType, agent.getCapabilityClass()));
    }
  }

  private boolean isSupplementaryCertification(CertificationType certificationType) {
    return switch (certificationType) {
      case DEFENSIVE_DRIVING, FIRST_AID -> true;
      default -> false;
    };
  }
}
