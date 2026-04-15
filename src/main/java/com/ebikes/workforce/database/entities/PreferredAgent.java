package com.ebikes.workforce.database.entities;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
import com.ebikes.workforce.support.audit.Auditable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(
    name = "preferred_agents",
    schema = "workforce",
    indexes = {
      @Index(name = "idx_preferred_agents_agent", columnList = "agent_id"),
      @Index(name = "idx_preferred_agents_organization", columnList = "organization_id"),
      @Index(name = "idx_preferred_agents_org_priority", columnList = "organization_id, priority")
    })
public class PreferredAgent extends AuditableEntity implements Auditable {

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

  public static PreferredAgent create(
      UUID agentId, String branchId, String notes, String organizationId, Integer priority) {
    return PreferredAgent.builder()
        .agentId(agentId)
        .branchId(branchId)
        .notes(notes)
        .organizationId(organizationId)
        .priority(priority)
        .build();
  }

  @Override
  public Map<String, String> toAuditMetadata() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("agentId", this.getAgentId().toString());
    metadata.put("organizationId", this.getOrganizationId());
    metadata.put("preferredAgentId", this.getId().toString());
    metadata.put("priority", String.valueOf(this.getPriority()));
    if (this.getBranchId() != null) {
      metadata.put("branchId", this.getBranchId());
    }
    return Collections.unmodifiableMap(metadata);
  }

  public void updateNotes(String notes) {
    this.notes = notes;
  }

  public void updatePriority(Integer priority) {
    this.priority = priority;
  }
}
