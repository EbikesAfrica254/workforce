package com.ebikes.workforce.database.repositories;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ebikes.workforce.database.entities.Agent;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.CapabilityClass;

@Repository
public interface AgentRepository
    extends JpaRepository<Agent, UUID>, JpaSpecificationExecutor<Agent> {

  boolean existsByNationalIdNumber(String nationalIdNumber);

  boolean existsByPhoneNumber(String phoneNumber);

  boolean existsByUserId(String userId);

  @Query(
      """
      SELECT a FROM Agent a
      WHERE a.availabilityStatus = :status
        AND a.currentH3Index IN :h3Cells
        AND a.lastLocationUpdatedAt >= :freshnessThreshold
        AND a.currentOrders < a.maxConcurrentOrders
        AND a.capabilityClass IN :capabilityClasses
      """)
  List<Agent> findEligibleForShortlist(
      @Param("status") AvailabilityStatus status,
      @Param("h3Cells") Collection<String> h3Cells,
      @Param("freshnessThreshold") OffsetDateTime freshnessThreshold,
      @Param("capabilityClasses") Set<CapabilityClass> capabilityClasses);
}
