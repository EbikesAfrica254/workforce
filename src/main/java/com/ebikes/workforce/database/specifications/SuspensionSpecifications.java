package com.ebikes.workforce.database.specifications;

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
    // prevent instantiation
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
      FilterUtilities.addDateRange(
          predicates,
          root,
          query,
          criteriaBuilder,
          FIELD_EXPIRES_AT,
          filter.getCreatedAtFrom(),
          filter.getCreatedAtTo());

      FilterUtilities.addDateRange(
          predicates,
          root,
          query,
          criteriaBuilder,
          FIELD_EXPIRES_AT,
          filter.getExpiresAtFrom(),
          filter.getExpiresAtTo());

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
}
