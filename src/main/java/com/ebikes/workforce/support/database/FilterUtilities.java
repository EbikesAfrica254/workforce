package com.ebikes.workforce.support.database;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.ebikes.workforce.dtos.requests.filters.bases.BaseFilter;
import com.ebikes.workforce.enums.ResponseCode;
import com.ebikes.workforce.exceptions.ValidationException;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FilterUtilities {

  public static <T> void addIfPresent(
      List<Predicate> predicates,
      Root<T> root,
      CriteriaQuery<?> query,
      CriteriaBuilder criteriaBuilder,
      Object value,
      Specification<T> spec) {
    if (value != null) {
      predicates.add(spec.toPredicate(root, query, criteriaBuilder));
    }
  }

  public static Pageable buildPageable(BaseFilter filter, Set<String> allowedSortFields) {
    String sortBy = filter.getSortBy();
    if (!allowedSortFields.contains(sortBy)) {
      throw new ValidationException(
          ResponseCode.INVALID_ARGUMENTS,
          "Invalid sort field specified. Allowed fields: " + allowedSortFields,
          "sortBy",
          sortBy);
    }
    Sort.Direction direction = Sort.Direction.fromString(filter.getSortDirection());
    Sort sort = Sort.by(direction, sortBy);
    int zeroIndexedPage = filter.getPage() - 1;
    return PageRequest.of(zeroIndexedPage, filter.getSize(), sort);
  }

  public static <T> Specification<T> likeIgnoreCase(String fieldPath, String searchTerm) {
    return (root, query, criteriaBuilder) -> {
      String pattern = "%" + searchTerm.toLowerCase().trim() + "%";
      return criteriaBuilder.like(criteriaBuilder.lower(root.get(fieldPath)), pattern);
    };
  }

  public static <T> Specification<T> offsetDateTimeAfter(String fieldPath, OffsetDateTime after) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.greaterThanOrEqualTo(root.get(fieldPath), after);
  }

  public static <T> Specification<T> offsetDateTimeBefore(String fieldPath, OffsetDateTime before) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.lessThanOrEqualTo(root.get(fieldPath), before);
  }
}
