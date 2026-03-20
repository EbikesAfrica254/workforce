package com.ebikes.workforce.database.specifications;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.ebikes.workforce.database.entities.PreferredAgent;
import com.ebikes.workforce.dtos.requests.filters.PreferredAgentFilter;

public final class PreferredAgentSpecifications {

  public static final String FIELD_AGENT_ID = "agentId";
  public static final String FIELD_BRANCH_ID = "branchId";
  public static final String FIELD_CREATED_AT = "createdAt";
  public static final String FIELD_ORGANIZATION_ID = "organizationId";
  public static final String FIELD_PRIORITY = "priority";

  public static final Set<String> ALLOWED_SORT_FIELDS = Set.of(FIELD_CREATED_AT, FIELD_PRIORITY);

  private PreferredAgentSpecifications() {
    // prevent instantiaton
  }

  public static Specification<PreferredAgent> buildSpecification(PreferredAgentFilter filter) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      predicates.add(
          AuthorizationSpecifications.forPreferredAgents()
              .toPredicate(root, query, criteriaBuilder));

      addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getAgentId(),
          hasAgentId(filter.getAgentId()));
      addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getBranchId(),
          hasBranchId(filter.getBranchId()));
      addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getOrganizationId(),
          hasOrganizationId(filter.getOrganizationId()));

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }

  public static Specification<PreferredAgent> hasAgentId(UUID agentId) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(FIELD_AGENT_ID), agentId);
  }

  public static Specification<PreferredAgent> hasBranchId(String branchId) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(FIELD_BRANCH_ID), branchId);
  }

  public static Specification<PreferredAgent> hasOrganizationId(String organizationId) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(FIELD_ORGANIZATION_ID), organizationId);
  }

  private static void addIfPresent(
      List<Predicate> predicates,
      jakarta.persistence.criteria.Root<PreferredAgent> root,
      jakarta.persistence.criteria.CriteriaQuery<?> query,
      jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
      Object value,
      Specification<PreferredAgent> spec) {
    if (value != null && !(value instanceof String s && s.isBlank())) {
      predicates.add(spec.toPredicate(root, query, criteriaBuilder));
    }
  }
}
