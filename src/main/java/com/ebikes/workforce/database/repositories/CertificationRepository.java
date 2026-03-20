package com.ebikes.workforce.database.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ebikes.workforce.database.entities.Certification;
import com.ebikes.workforce.enums.CertificationType;

@Repository
public interface CertificationRepository extends JpaRepository<Certification, UUID> {

  @Query(
      """
      SELECT c FROM Certification c
      WHERE c.expiresAt < :asOf
      AND c.certificationType IN :requiredTypes
      """)
  List<Certification> findExpiredRequiredCertifications(
      @Param("asOf") LocalDate asOf, @Param("requiredTypes") List<CertificationType> requiredTypes);

  List<Certification> findByAgentId(UUID agentId);
}
