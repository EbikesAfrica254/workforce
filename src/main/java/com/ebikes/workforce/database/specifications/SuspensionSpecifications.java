package com.ebikes.workforce.database.specifications;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.ebikes.workforce.database.entities.Suspension;
import com.ebikes.workforce.dtos.requests.filters.SuspensionFilter;
import com.ebikes.workforce.support.database.FilterUtilities;

public final class SuspensionSpecifications {

  public static final String FIELD_AGENT_ID = "agentId";
  public static final String FIELD_CREATED_AT = "createdAt";
  public static final String FIELD_EXPIRES_AT = "expiresAt";
  public static final String FIELD_LIFTED_AT = "liftedAt";

  public static final Set<String> ALLOWED_SORT_FIELDS = Set.of(FIELD_CREATED_AT, FIELD_EXPIRES_AT);

  private SuspensionSpecifications() {
    // prevent instantiaton
  }

  public static Specification<Suspension> buildSpecification(SuspensionFilter filter) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      FilterUtilities.addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getAgentId(),
          hasAgentId(filter.getAgentId()));
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
          filter.getExpiresAtFrom(),
          offsetDateTimeAfter(FIELD_EXPIRES_AT, filter.getExpiresAtFrom()));
      FilterUtilities.addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getExpiresAtTo(),
          offsetDateTimeBefore(FIELD_EXPIRES_AT, filter.getExpiresAtTo()));

      if (Boolean.TRUE.equals(filter.getIsActive())) {
        predicates.add(isActive().toPredicate(root, query, criteriaBuilder));
      }

      if (Boolean.FALSE.equals(filter.getIsActive())) {
        predicates.add(criteriaBuilder.not(isActive().toPredicate(root, query, criteriaBuilder)));
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }

  public static Specification<Suspension> hasAgentId(UUID agentId) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(FIELD_AGENT_ID), agentId);
  }

  public static Specification<Suspension> isActive() {
    return (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get(FIELD_LIFTED_AT));
  }

  public static Specification<Suspension> offsetDateTimeAfter(
      String fieldPath, OffsetDateTime after) {
    return FilterUtilities.offsetDateTimeAfter(fieldPath, after);
  }

  public static Specification<Suspension> offsetDateTimeBefore(
      String fieldPath, OffsetDateTime before) {
    return FilterUtilities.offsetDateTimeBefore(fieldPath, before);
  }
}
