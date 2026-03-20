package com.ebikes.workforce.database.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ebikes.workforce.database.entities.PreferredAgent;

@Repository
public interface PreferredAgentRepository
    extends JpaRepository<PreferredAgent, UUID>, JpaSpecificationExecutor<PreferredAgent> {

  boolean existsByOrganizationIdAndAgentId(String organizationId, UUID agentId);

  boolean existsByOrganizationIdAndBranchIdAndAgentId(
      String organizationId, String branchId, UUID agentId);

  void deleteByAgentId(UUID agentId);

  @Query(
      """
      SELECT pa.agentId FROM PreferredAgent pa
      WHERE pa.organizationId = :organizationId
      """)
  List<UUID> findAgentIdsByOrganizationId(@Param("organizationId") String organizationId);

  @Query(
      """
      SELECT pa.agentId FROM PreferredAgent pa
      WHERE pa.organizationId = :organizationId
        AND pa.branchId = :branchId
      """)
  List<UUID> findAgentIdsByOrganizationIdAndBranchId(
      @Param("organizationId") String organizationId, @Param("branchId") String branchId);
}
