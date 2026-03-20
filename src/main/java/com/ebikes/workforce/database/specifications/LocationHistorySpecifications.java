package com.ebikes.workforce.database.specifications;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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

  public static final Set<String> ALLOWED_SORT_FIELDS = Set.of(FIELD_CREATED_AT);

  private LocationHistorySpecifications() {
    // prevent instantiaton
  }

  public static Specification<LocationHistory> buildSpecification(LocationHistoryFilter filter) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      predicates.add(hasAgentId(filter.getAgentId()).toPredicate(root, query, criteriaBuilder));

      FilterUtilities.addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getCreatedAtFrom(),
          offsetDateTimeAfter(FIELD_CREATED_AT, filter.getCreatedAtFrom()));
      FilterUtilities.addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getCreatedAtTo(),
          offsetDateTimeBefore(FIELD_CREATED_AT, filter.getCreatedAtTo()));
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

  public static Specification<LocationHistory> offsetDateTimeAfter(
      String fieldPath, OffsetDateTime after) {
    return FilterUtilities.offsetDateTimeAfter(fieldPath, after);
  }

  public static Specification<LocationHistory> offsetDateTimeBefore(
      String fieldPath, OffsetDateTime before) {
    return FilterUtilities.offsetDateTimeBefore(fieldPath, before);
  }
}
