package com.ebikes.workforce.database.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ebikes.workforce.database.entities.Document;
import com.ebikes.workforce.enums.DocumentStatus;
import com.ebikes.workforce.enums.DocumentType;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

  @Modifying
  @Query(
      """
      UPDATE Document d
      SET d.status = DocumentStatus.EXPIRED
      WHERE d.status = DocumentStatus.ACTIVE
      AND d.expiryDate < :asOf
      AND d.documentType IN :requiredTypes
      """)
  int bulkExpireDocuments(
      @Param("asOf") LocalDate asOf, @Param("requiredTypes") Set<DocumentType> requiredTypes);

  @Query(
      """
      SELECT d.agent.id FROM Document d
      WHERE d.status = DocumentStatus.EXPIRED
      AND d.expiryDate < :asOf
      AND d.documentType IN :requiredTypes
      """)
  Set<UUID> findAgentIdsWithExpiredRequiredDocuments(
      @Param("asOf") LocalDate asOf, @Param("requiredTypes") Set<DocumentType> requiredTypes);

  List<Document> findAllByFileStorageUrlIn(List<String> storageKeys);

  List<Document> findByAgentId(UUID agentId);

  List<Document> findByAgentIdAndStatus(UUID agentId, DocumentStatus status);

  List<Document> findByAgentIdAndStatusIn(UUID agentId, List<DocumentStatus> statuses);
}
