package com.ebikes.workforce.services.agents.location;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.workforce.configurations.properties.H3Properties;
import com.ebikes.workforce.constants.EventConstants.DomainEvents;
import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.LocationHistory;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.LocationHistoryRepository;
import com.ebikes.workforce.database.specifications.LocationHistorySpecifications;
import com.ebikes.workforce.dtos.requests.agents.UpdateLocationRequest;
import com.ebikes.workforce.dtos.requests.filters.LocationHistoryFilter;
import com.ebikes.workforce.dtos.responses.agents.AgentDetailResponse;
import com.ebikes.workforce.dtos.responses.agents.LocationHistoryResponse;
import com.ebikes.workforce.enums.LocationSource;
import com.ebikes.workforce.mappers.AgentMapper;
import com.ebikes.workforce.mappers.LocationHistoryMapper;
import com.ebikes.workforce.services.agents.AccessPolicy;
import com.ebikes.workforce.services.agents.AgentScopedService;
import com.ebikes.workforce.support.audit.AuditTemplate;
import com.uber.h3core.H3Core;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LocationService extends AgentScopedService {

  private final AccessPolicy accessPolicy;
  private final AgentMapper agentMapper;
  private final AuditTemplate auditTemplate;
  private final H3Core h3Core;
  private final H3Properties h3Properties;
  private final LocationHistoryMapper locationHistoryMapper;
  private final LocationHistoryRepository locationHistoryRepository;

  public LocationService(
      AccessPolicy accessPolicy,
      AgentMapper agentMapper,
      AgentRepository agentRepository,
      AuditTemplate auditTemplate,
      H3Core h3Core,
      H3Properties h3Properties,
      LocationHistoryMapper locationHistoryMapper,
      LocationHistoryRepository locationHistoryRepository) {
    super(agentRepository);
    this.accessPolicy = accessPolicy;
    this.agentMapper = agentMapper;
    this.auditTemplate = auditTemplate;
    this.h3Core = h3Core;
    this.h3Properties = h3Properties;
    this.locationHistoryMapper = locationHistoryMapper;
    this.locationHistoryRepository = locationHistoryRepository;
  }

  @Transactional(readOnly = true)
  public Page<LocationHistoryResponse> getLocationHistory(
      LocationHistoryFilter filter, Pageable pageable) {
    requireAgentById(filter.getAgentId());

    return locationHistoryRepository
        .findAll(LocationHistorySpecifications.buildSpecification(filter), pageable)
        .map(locationHistoryMapper::toResponse);
  }

  @Transactional
  public AgentDetailResponse updateLocation(
      UUID agentId, UpdateLocationRequest request, LocationSource source) {
    log.info("Updating location: agentId={}", agentId);

    Agent agent = requireAgentById(agentId);
    accessPolicy.owns(agent);

    String h3Index =
        h3Core.latLngToCellAddress(
            request.latitude().doubleValue(),
            request.longitude().doubleValue(),
            h3Properties.getResolution());

    Agent updated =
        auditTemplate.execute(
            agent,
            null,
            DomainEvents.Agent.LOCATION_UPDATED,
            () -> {
              agent.updateLocation(request.latitude(), request.longitude(), h3Index, source);
              Agent saved = agentRepository.save(agent);

              locationHistoryRepository.save(
                  LocationHistory.create(
                      agentId, request.latitude(), request.longitude(), h3Index, source));

              return saved;
            });

    log.info("Location updated: agentId={}, h3Index={}", agentId, h3Index);

    return agentMapper.toDetailResponse(updated);
  }
}
