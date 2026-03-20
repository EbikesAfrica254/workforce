package com.ebikes.workforce.database.entities;

import java.io.Serial;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ebikes.workforce.database.entities.bases.AuditableEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "preferred_agents",
    schema = "workforce",
    indexes = {
      @Index(name = "idx_preferred_agents_agent", columnList = "agent_id"),
      @Index(name = "idx_preferred_agents_organization", columnList = "organization_id"),
      @Index(name = "idx_preferred_agents_org_priority", columnList = "organization_id, priority")
    })
public class PreferredAgent extends AuditableEntity {

  @Serial private static final long serialVersionUID = 1L;

  @Column(name = "agent_id", nullable = false, updatable = false)
  @NotNull private UUID agentId;

  @Column(name = "branch_id")
  @Size(max = 36) private String branchId;

  @Column(name = "notes")
  private String notes;

  @Column(name = "organization_id", nullable = false, updatable = false, length = 36)
  @NotBlank @Size(max = 36) private String organizationId;

  @Column(name = "priority", nullable = false)
  @Min(1) private Integer priority;

  @Column(nullable = false)
  @Version
  private Long version;

  public PreferredAgent(
      UUID agentId, String branchId, String notes, String organizationId, Integer priority) {
    this.agentId = agentId;
    this.branchId = branchId;
    this.organizationId = organizationId;
    this.priority = priority;
    this.notes = notes;
    this.version = 0L;
  }

  public void updatePriority(Integer priority) {
    this.priority = priority;
  }

  public void updateNotes(String notes) {
    this.notes = notes;
  }
}
