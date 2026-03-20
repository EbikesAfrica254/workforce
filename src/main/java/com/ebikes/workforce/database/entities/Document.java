package com.ebikes.workforce.database.entities;

import java.io.Serial;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ebikes.workforce.database.entities.bases.AuditableEntity;
import com.ebikes.workforce.enums.DocumentStatus;
import com.ebikes.workforce.enums.DocumentType;
import com.ebikes.workforce.enums.ResponseCode;
import com.ebikes.workforce.exceptions.ValidationException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "documents",
    schema = "workforce",
    indexes = {
      @Index(name = "idx_documents_agent", columnList = "agent_id"),
      @Index(name = "idx_documents_agent_type", columnList = "agent_id, document_type"),
      @Index(name = "idx_documents_expiry_date", columnList = "expiry_date"),
      @Index(name = "idx_documents_status", columnList = "status")
    })
public class Document extends AuditableEntity {

  @Serial private static final long serialVersionUID = 1L;

  @JoinColumn(name = "agent_id")
  @ManyToOne(fetch = FetchType.LAZY)
  private Agent agent;

  @Column(name = "document_type", nullable = false, updatable = false, length = 50)
  @Enumerated(EnumType.STRING)
  @NotNull private DocumentType documentType;

  @Column(name = "expiry_date")
  private LocalDate expiryDate;

  @Column(name = "file_name")
  @Size(max = 255) private String fileName;

  @Column(name = "file_size_bytes")
  private Long fileSizeBytes;

  @Column(name = "file_storage_url", length = 1000)
  @Size(max = 1000) private String fileStorageUrl;

  @Column(name = "mime_type", length = 100)
  @Size(max = 100) private String mimeType;

  @JoinColumn(name = "replaces_document_id")
  @ManyToOne(fetch = FetchType.LAZY)
  private Document replacesDocument;

  @Column(name = "status", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  @NotNull private DocumentStatus status = DocumentStatus.PENDING;

  @Column(name = "uploaded_at", columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime uploadedAt;

  @Column(nullable = false)
  @Version
  private Long version;

  public Document(
      @NotNull DocumentType documentType, @NotBlank String fileName, @NotBlank String mimeType) {

    this.documentType = documentType;
    this.fileName = fileName;
    this.mimeType = mimeType;
    this.status = DocumentStatus.PENDING;
    this.uploadedAt = OffsetDateTime.now(ZoneOffset.UTC);
    this.version = 0L;
  }

  public void activate() {
    if (this.status != DocumentStatus.UPLOADED) {
      throw new ValidationException(
          ResponseCode.INVALID_STATE,
          "Only UPLOADED documents can be activated. Current status: " + this.status,
          "status",
          this.status);
    }
    this.status = DocumentStatus.ACTIVE;
  }

  public void assignStorageKey(@NotBlank String key) {
    this.fileStorageUrl = key;
  }

  public void associate(Agent agent) {
    if (agent == null) {
      throw new IllegalArgumentException("Agent is required");
    }

    if (this.agent != null && !this.agent.equals(agent)) {
      throw new ValidationException(
          ResponseCode.INVALID_STATE,
          "Document is already associated with another agent",
          "agent",
          this.agent.getId());
    }

    this.agent = agent;
  }

  public void designateAsReplacementFor(@NotNull Document oldDocument) {
    this.replacesDocument = oldDocument;
  }

  public void expire() {
    if (this.status != DocumentStatus.ACTIVE) {
      throw new ValidationException(
          ResponseCode.INVALID_STATE,
          "Only ACTIVE documents can be expired. Current status: " + this.status,
          "status",
          this.status);
    }
    this.status = DocumentStatus.EXPIRED;
  }

  public void markReplaced() {
    if (this.status != DocumentStatus.ACTIVE && this.status != DocumentStatus.EXPIRED) {
      throw new ValidationException(
          ResponseCode.INVALID_STATE,
          "Only ACTIVE or EXPIRED documents can be replaced. Current status: " + this.status,
          "status",
          this.status);
    }
    this.status = DocumentStatus.REPLACED;
  }

  public void markUploaded(Long fileSizeBytes, String mimeType, LocalDate expiryDate) {
    if (this.status != DocumentStatus.PENDING) {
      throw new ValidationException(
          ResponseCode.INVALID_STATE,
          "Only PENDING documents can be marked as uploaded. Current status: " + this.status,
          "status",
          this.status);
    }
    this.status = DocumentStatus.UPLOADED;
    this.uploadedAt = OffsetDateTime.now(ZoneOffset.UTC);
    this.fileSizeBytes = fileSizeBytes;
    this.mimeType = mimeType;
    this.expiryDate = expiryDate;
  }

  public void reject() {
    if (this.status != DocumentStatus.UPLOADED) {
      throw new ValidationException(
          ResponseCode.INVALID_STATE,
          "Only UPLOADED documents can be rejected. Current status: " + this.status,
          "status",
          this.status);
    }
    this.status = DocumentStatus.REJECTED;
  }
}
