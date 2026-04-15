package com.ebikes.workforce.database.entities;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ebikes.workforce.database.entities.bases.BaseEntity;
import com.ebikes.workforce.enums.LocationSource;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(
    name = "location_history",
    schema = "workforce",
    indexes = {
      @Index(name = "idx_location_history_agent", columnList = "agent_id"),
      @Index(name = "idx_location_history_created_at", columnList = "created_at")
    })
public class LocationHistory extends BaseEntity {

  @Column(name = "agent_id", nullable = false, updatable = false)
  @NotNull private UUID agentId;

  @Column(name = "h3_index", nullable = false, updatable = false, length = 20)
  @NotBlank @Size(max = 20) private String h3Index;

  @Column(name = "latitude", nullable = false, updatable = false, precision = 10, scale = 8)
  @DecimalMax(value = "90.0") @DecimalMin(value = "-90.0") @NotNull private BigDecimal latitude;

  @Column(name = "longitude", nullable = false, updatable = false, precision = 11, scale = 8)
  @DecimalMax(value = "180.0") @DecimalMin(value = "-180.0") @NotNull private BigDecimal longitude;

  @Column(name = "source", nullable = false, updatable = false, length = 20)
  @Enumerated(EnumType.STRING)
  @NotNull private LocationSource source;

  public static LocationHistory create(
      UUID agentId,
      BigDecimal latitude,
      BigDecimal longitude,
      String h3Index,
      LocationSource source) {
    return LocationHistory.builder()
        .agentId(agentId)
        .latitude(latitude)
        .longitude(longitude)
        .h3Index(h3Index)
        .source(source)
        .build();
  }
}
