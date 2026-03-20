package com.ebikes.workforce.database.specifications;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;

import org.springframework.data.jpa.domain.Specification;

import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.database.entities.Suspension;
import com.ebikes.workforce.dtos.requests.filters.AgentFilter;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.support.database.FilterUtilities;

public final class AgentSpecifications {

  public static final String FIELD_AVAILABILITY_STATUS = "availabilityStatus";
  public static final String FIELD_CAPABILITY_CLASS = "capabilityClass";
  public static final String FIELD_CREATED_AT = "createdAt";
  public static final String FIELD_FIRST_NAME = "firstName";
  public static final String FIELD_LAST_NAME = "lastName";
  public static final String FIELD_NATIONAL_ID_NUMBER = "nationalIdNumber";
  public static final String FIELD_PHONE_NUMBER = "phoneNumber";
  public static final String FIELD_RELIABILITY_SCORE = "reliabilityScore";

  public static final Set<String> ALLOWED_SORT_FIELDS =
      Set.of(
          FIELD_AVAILABILITY_STATUS,
          FIELD_CAPABILITY_CLASS,
          FIELD_CREATED_AT,
          FIELD_FIRST_NAME,
          FIELD_LAST_NAME,
          FIELD_RELIABILITY_SCORE);

  private AgentSpecifications() {
    // prevent instantiaton
  }

  public static Specification<Agent> buildSpecification(AgentFilter filter) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      predicates.add(
          AuthorizationSpecifications.forAgents().toPredicate(root, query, criteriaBuilder));

      FilterUtilities.addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getAvailabilityStatus(),
          hasAvailabilityStatus(filter.getAvailabilityStatus()));
      FilterUtilities.addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getCapabilityClass(),
          hasCapabilityClass(filter.getCapabilityClass()));
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
          filter.getFirstName(),
          FilterUtilities.likeIgnoreCase(FIELD_FIRST_NAME, filter.getFirstName()));
      FilterUtilities.addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getLastName(),
          FilterUtilities.likeIgnoreCase(FIELD_LAST_NAME, filter.getLastName()));
      FilterUtilities.addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getNationalIdNumber(),
          hasNationalIdNumber(filter.getNationalIdNumber()));
      FilterUtilities.addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getPhoneNumber(),
          hasPhoneNumber(filter.getPhoneNumber()));

      if (filter.getReliabilityScoreMin() != null || filter.getReliabilityScoreMax() != null) {
        predicates.add(
            hasReliabilityScoreRange(
                    filter.getReliabilityScoreMin(), filter.getReliabilityScoreMax())
                .toPredicate(root, query, criteriaBuilder));
      }

      if (Boolean.TRUE.equals(filter.getHasActiveSuspension())) {
        predicates.add(hasActiveSuspension().toPredicate(root, query, criteriaBuilder));
      }

      if (Boolean.FALSE.equals(filter.getHasActiveSuspension())) {
        predicates.add(
            criteriaBuilder.not(hasActiveSuspension().toPredicate(root, query, criteriaBuilder)));
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }

  public static Specification<Agent> hasActiveSuspension() {
    return (root, query, criteriaBuilder) -> {
      Subquery<Long> subquery = query.subquery(Long.class);
      var suspensionRoot = subquery.from(Suspension.class);
      subquery
          .select(criteriaBuilder.literal(1L))
          .where(
              criteriaBuilder.and(
                  criteriaBuilder.equal(suspensionRoot.get("agentId"), root.get("id")),
                  criteriaBuilder.isNull(suspensionRoot.get("liftedAt"))));
      return criteriaBuilder.exists(subquery);
    };
  }

  public static Specification<Agent> hasAvailabilityStatus(AvailabilityStatus status) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(FIELD_AVAILABILITY_STATUS), status);
  }

  public static Specification<Agent> hasCapabilityClass(CapabilityClass capabilityClass) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(FIELD_CAPABILITY_CLASS), capabilityClass);
  }

  public static Specification<Agent> hasNationalIdNumber(String nationalIdNumber) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(FIELD_NATIONAL_ID_NUMBER), nationalIdNumber);
  }

  public static Specification<Agent> hasPhoneNumber(String phoneNumber) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(FIELD_PHONE_NUMBER), phoneNumber);
  }

  public static Specification<Agent> hasReliabilityScoreRange(BigDecimal min, BigDecimal max) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (min != null) {
        predicates.add(
            criteriaBuilder.greaterThanOrEqualTo(root.get(FIELD_RELIABILITY_SCORE), min));
      }
      if (max != null) {
        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(FIELD_RELIABILITY_SCORE), max));
      }
      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }

  public static Specification<Agent> offsetDateTimeAfter(String fieldPath, OffsetDateTime after) {
    return FilterUtilities.offsetDateTimeAfter(fieldPath, after);
  }

  public static Specification<Agent> offsetDateTimeBefore(String fieldPath, OffsetDateTime before) {
    return FilterUtilities.offsetDateTimeBefore(fieldPath, before);
  }
}
