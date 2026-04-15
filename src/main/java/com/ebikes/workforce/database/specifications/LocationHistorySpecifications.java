package com.ebikes.workforce.database.specifications;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.ebikes.workforce.database.entities.LocationHistory;
import com.ebikes.workforce.dtos.requests.filters.LocationHistoryFilter;
import com.ebikes.workforce.enums.LocationSource;
import com.ebikes.workforce.support.database.FilterUtilities;

public final class LocationHistorySpecifications {

  public static final String FIELD_AGENT_ID = "agentId";
  public static final String FIELD_CREATED_AT = "createdAt";
  public static final String FIELD_SOURCE = "source";

  private LocationHistorySpecifications() {
    // prevent instantiation
  }

  public static Specification<LocationHistory> buildSpecification(LocationHistoryFilter filter) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      predicates.add(hasAgentId(filter.getAgentId()).toPredicate(root, query, criteriaBuilder));

      FilterUtilities.addDateRange(
          predicates,
          root,
          query,
          criteriaBuilder,
          FIELD_CREATED_AT,
          filter.getCreatedAtFrom(),
          filter.getCreatedAtTo());
      FilterUtilities.addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getSource(),
          hasSource(filter.getSource()));

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }

  public static Specification<LocationHistory> hasAgentId(UUID agentId) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(FIELD_AGENT_ID), agentId);
  }

  public static Specification<LocationHistory> hasSource(LocationSource source) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(FIELD_SOURCE), source);
  }
}
