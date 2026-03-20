package com.ebikes.workforce.services.agents;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.workforce.configurations.properties.H3Properties;
import com.ebikes.workforce.configurations.properties.ShortlistProperties;
import com.ebikes.workforce.constants.EventConstants.EventSource;
import com.ebikes.workforce.constants.EventConstants.EventTypes;
import com.ebikes.workforce.constants.EventConstants.RoutingKeys;
import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.repositories.AgentRepository;
import com.ebikes.workforce.database.repositories.PreferredAgentRepository;
import com.ebikes.workforce.dtos.events.outgoing.AgentShortlistEmptyEvent;
import com.ebikes.workforce.dtos.events.outgoing.AgentShortlistResolvedEvent;
import com.ebikes.workforce.dtos.internal.ShortlistCandidate;
import com.ebikes.workforce.dtos.internal.ShortlistRequest;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.enums.VehicleClass;
import com.ebikes.workforce.services.events.OutboxService;
import com.ebikes.workforce.support.workforce.CapabilityClassMapper;
import com.uber.h3core.H3Core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class ShortlistService {

  private final AgentRepository agentRepository;
  private final H3Core h3Core;
  private final H3Properties h3Properties;
  private final OutboxService outboxService;
  private final PreferredAgentRepository preferredAgentRepository;
  private final ShortlistProperties shortlistProperties;

  @Transactional
  public void resolve(ShortlistRequest request) {
    log.info(
        "Resolving shortlist: orderId={}, vehicleClass={}, pickupLat={}, pickupLng={}",
        request.orderId(),
        request.vehicleClass(),
        request.pickupLatitude(),
        request.pickupLongitude());

    String pickupH3Index =
        h3Core.latLngToCellAddress(
            request.pickupLatitude().doubleValue(),
            request.pickupLongitude().doubleValue(),
            h3Properties.getResolution());

    List<String> searchCells =
        h3Core.gridDisk(pickupH3Index, shortlistProperties.getDefaultRingSize());

    OffsetDateTime freshnessThreshold =
        OffsetDateTime.now().minusMinutes(shortlistProperties.getFreshThresholdMinutes());

    Set<CapabilityClass> eligibleCapabilityClasses =
        CapabilityClassMapper.fromVehicleClass(request.vehicleClass());

    List<Agent> eligibleAgents =
        agentRepository.findEligibleForShortlist(
            AvailabilityStatus.AVAILABLE,
            searchCells,
            freshnessThreshold,
            eligibleCapabilityClasses);

    if (eligibleAgents.isEmpty()) {
      log.info(
          "No eligible agents found for shortlist: orderId={}, h3Index={}",
          request.orderId(),
          pickupH3Index);

      outboxService.save(
          EventTypes.Workforce.AGENT_SHORTLIST_EMPTY,
          buildEmptyEvent(request, pickupH3Index),
          RoutingKeys.WORKFORCE_SHORTLIST_EMPTY);

      return;
    }

    Set<String> preferredAgentIds = resolvePreferredAgentIds(request);

    List<ShortlistCandidate> candidates =
        eligibleAgents.stream().map(agent -> toCandidate(agent, preferredAgentIds)).toList();

    OffsetDateTime resolvedAt = OffsetDateTime.now();
    OffsetDateTime expiresAt = resolvedAt.plusSeconds(shortlistProperties.getExpirySeconds());

    outboxService.save(
        EventTypes.Workforce.AGENT_SHORTLIST_RESOLVED,
        buildResolvedEvent(request, candidates, resolvedAt, expiresAt),
        RoutingKeys.WORKFORCE_SHORTLIST_RESOLVED);

    log.info(
        "Shortlist resolved: orderId={}, candidateCount={}", request.orderId(), candidates.size());
  }

  private AgentShortlistEmptyEvent buildEmptyEvent(ShortlistRequest request, String pickupH3Index) {
    return new AgentShortlistEmptyEvent(
        request.branchId(),
        request.orderId(),
        request.organizationId(),
        pickupH3Index,
        OffsetDateTime.now(),
        EventSource.serviceReference(),
        request.vehicleClass());
  }

  private AgentShortlistResolvedEvent buildResolvedEvent(
      ShortlistRequest request,
      List<ShortlistCandidate> candidates,
      OffsetDateTime resolvedAt,
      OffsetDateTime expiresAt) {

    List<AgentShortlistResolvedEvent.Candidate> candidatePayloads =
        candidates.stream()
            .map(
                c ->
                    new AgentShortlistResolvedEvent.Candidate(
                        c.agentId().toString(),
                        c.isPreferred(),
                        c.latitude(),
                        c.longitude(),
                        c.vehicleClass()))
            .toList();

    return new AgentShortlistResolvedEvent(
        request.branchId(),
        candidatePayloads,
        expiresAt,
        request.orderId(),
        request.organizationId(),
        resolvedAt,
        EventSource.serviceReference());
  }

  private Set<String> resolvePreferredAgentIds(ShortlistRequest request) {
    if (request.organizationId() == null) {
      return Set.of();
    }

    List<UUID> preferredIds =
        request.branchId() != null
            ? preferredAgentRepository.findAgentIdsByOrganizationIdAndBranchId(
                request.organizationId(), request.branchId())
            : preferredAgentRepository.findAgentIdsByOrganizationId(request.organizationId());

    return Set.copyOf(preferredIds.stream().map(UUID::toString).toList());
  }

  private ShortlistCandidate toCandidate(Agent agent, Set<String> preferredAgentIds) {
    VehicleClass vehicleClass = CapabilityClassMapper.toVehicleClass(agent.getCapabilityClass());
    boolean isPreferred = preferredAgentIds.contains(agent.getId().toString());

    return new ShortlistCandidate(
        agent.getId(),
        agent.getCurrentLatitude(),
        agent.getCurrentLongitude(),
        vehicleClass,
        isPreferred);
  }
}
