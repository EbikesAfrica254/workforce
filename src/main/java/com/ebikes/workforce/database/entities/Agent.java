package com.ebikes.workforce.database.entities;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.ebikes.workforce.database.entities.bases.AuditableEntity;
import com.ebikes.workforce.enums.AvailabilityStatus;
import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.enums.LocationSource;
import com.ebikes.workforce.enums.NationalIdType;
import com.ebikes.workforce.enums.ResponseCode;
import com.ebikes.workforce.exceptions.BusinessRuleException;
import com.ebikes.workforce.support.audit.Auditable;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(
    name = "agents",
    schema = "workforce",
    indexes = {
      @Index(name = "idx_agents_availability_status", columnList = "availability_status"),
      @Index(
          name = "idx_agents_availability_h3",
          columnList = "availability_status, current_h3_index"),
      @Index(name = "idx_agents_capability_class", columnList = "capability_class"),
      @Index(name = "idx_agents_h3_index", columnList = "current_h3_index")
    })
public class Agent extends AuditableEntity implements Auditable {

  @Builder.Default
  @Column(name = "availability_status", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  @NotNull private AvailabilityStatus availabilityStatus = AvailabilityStatus.PENDING;

  @Setter(AccessLevel.PACKAGE)
  @Column(name = "alternate_phone_number", length = 20)
  @Pattern(regexp = "^\\+[1-9]\\d{1,14}$") @Size(max = 20) private String alternatePhoneNumber;

  @Setter(AccessLevel.PACKAGE)
  @Column(name = "capability_class", nullable = false, length = 25)
  @Enumerated(EnumType.STRING)
  @NotNull private CapabilityClass capabilityClass;

  @Builder.Default
  @Column(name = "completed_delivery_count", nullable = false)
  @Min(0) private Integer completedDeliveryCount = 0;

  @Column(name = "current_h3_index", length = 20)
  @Size(max = 20) private String currentH3Index;

  @Column(name = "current_latitude", precision = 10, scale = 8)
  @DecimalMax(value = "90.0") @DecimalMin(value = "-90.0") private BigDecimal currentLatitude;

  @Column(name = "current_longitude", precision = 11, scale = 8)
  @DecimalMax(value = "180.0") @DecimalMin(value = "-180.0") private BigDecimal currentLongitude;

  @Builder.Default
  @Column(name = "current_orders", nullable = false)
  @Min(0) private Integer currentOrders = 0;

  @Setter(AccessLevel.PACKAGE)
  @Column(name = "email")
  @Email @Size(max = 255) private String email;

  @Column(name = "first_name", nullable = false, length = 100)
  @NotBlank @Size(max = 100) private String firstName;

  @Column(name = "last_location_updated_at", columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime lastLocationUpdatedAt;

  @Column(name = "last_name", nullable = false, length = 100)
  @NotBlank @Size(max = 100) private String lastName;

  @Column(name = "location_source", length = 20)
  @Enumerated(EnumType.STRING)
  private LocationSource locationSource;

  @Setter(AccessLevel.PACKAGE)
  @Column(name = "max_concurrent_orders", nullable = false)
  @Min(1) private Short maxConcurrentOrders;

  @Column(name = "national_id_number", nullable = false, unique = true, length = 20)
  @NotBlank @Size(max = 20) private String nationalIdNumber;

  @Builder.Default
  @Column(name = "national_id_type", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  @NotNull private NationalIdType nationalIdType = NationalIdType.NATIONAL_ID;

  @Builder.Default
  @Column(name = "on_time_delivery_count", nullable = false)
  @Min(0) private Integer onTimeDeliveryCount = 0;

  @Column(name = "phone_number", nullable = false, unique = true, length = 20)
  @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{1,14}$") @Size(max = 20) private String phoneNumber;

  @Column(name = "rejection_reason", length = 1000)
  @Size(max = 1000) private String rejectionReason;

  @Column(name = "reliability_score", precision = 5, scale = 2)
  @DecimalMax(value = "100.0") @DecimalMin(value = "0.0") private BigDecimal reliabilityScore;

  @Column(name = "user_id", nullable = false, updatable = false, length = 36)
  @NotBlank @Size(max = 36) private String userId;

  @Builder.Default
  @Column(nullable = false)
  @Version
  private Long version = 0L;

  public void approve() {
    if (this.availabilityStatus != AvailabilityStatus.PENDING) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Cannot approve agent not in PENDING status: current status=" + this.availabilityStatus);
    }
    this.availabilityStatus = AvailabilityStatus.OFFLINE;
  }

  public void deactivate() {
    if (this.availabilityStatus == AvailabilityStatus.DEACTIVATED) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE, "Agent is already deactivated: " + this.getId());
    }
    this.availabilityStatus = AvailabilityStatus.DEACTIVATED;
  }

  public void decrementCurrentOrders() {
    if (this.currentOrders <= 0) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Cannot decrement current orders below zero: agentId=" + this.getId());
    }
    this.currentOrders--;
  }

  public void incrementCompletedDeliveries() {
    this.completedDeliveryCount++;
  }

  public void incrementCurrentOrders() {
    if (this.currentOrders >= this.maxConcurrentOrders) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Cannot exceed max concurrent orders: agentId="
              + this.getId()
              + ", max="
              + this.maxConcurrentOrders);
    }
    this.currentOrders++;
  }

  public void incrementOnTimeDeliveries() {
    this.onTimeDeliveryCount++;
  }

  public void liftSuspension() {
    if (this.availabilityStatus != AvailabilityStatus.SUSPENDED) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Cannot lift suspension for agent not in SUSPENDED status: current status="
              + this.availabilityStatus);
    }
    this.availabilityStatus = AvailabilityStatus.OFFLINE;
  }

  public void markAvailable() {
    if (this.availabilityStatus != AvailabilityStatus.OFFLINE
        && this.availabilityStatus != AvailabilityStatus.UNAVAILABLE) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Cannot mark agent AVAILABLE from status: " + this.availabilityStatus);
    }
    this.availabilityStatus = AvailabilityStatus.AVAILABLE;
  }

  public void markBusy() {
    if (this.availabilityStatus != AvailabilityStatus.AVAILABLE) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Cannot mark agent BUSY from status: " + this.availabilityStatus);
    }
    this.availabilityStatus = AvailabilityStatus.BUSY;
  }

  public void markOffline() {
    if (this.availabilityStatus == AvailabilityStatus.SUSPENDED
        || this.availabilityStatus == AvailabilityStatus.DEACTIVATED) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Cannot mark agent OFFLINE from status: " + this.availabilityStatus);
    }
    this.availabilityStatus = AvailabilityStatus.OFFLINE;
  }

  public void markUnavailable() {
    if (this.availabilityStatus == AvailabilityStatus.SUSPENDED
        || this.availabilityStatus == AvailabilityStatus.DEACTIVATED) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Cannot mark agent UNAVAILABLE from status: " + this.availabilityStatus);
    }
    this.availabilityStatus = AvailabilityStatus.UNAVAILABLE;
  }

  public void reject(String reason) {
    if (this.availabilityStatus != AvailabilityStatus.PENDING) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Cannot reject agent not in PENDING status: current status=" + this.availabilityStatus);
    }
    this.rejectionReason = reason;
  }

  public void resubmit() {
    if (this.availabilityStatus != AvailabilityStatus.PENDING) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Cannot resubmit agent not in PENDING status: current status=" + this.availabilityStatus);
    }
    this.rejectionReason = null;
  }

  public void suspend() {
    if (this.availabilityStatus == AvailabilityStatus.DEACTIVATED
        || this.availabilityStatus == AvailabilityStatus.SUSPENDED) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE, "Cannot suspend agent in status: " + this.availabilityStatus);
    }
    this.availabilityStatus = AvailabilityStatus.SUSPENDED;
  }

  @Override
  public Map<String, String> toAuditMetadata() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("availabilityStatus", this.getAvailabilityStatus().name());
    metadata.put("capabilityClass", this.getCapabilityClass().name());
    metadata.put("id", this.getId().toString());
    metadata.put("phoneNumber", this.getPhoneNumber());
    metadata.put("userId", this.getUserId());
    if (this.getEmail() != null) {
      metadata.put("email", this.getEmail());
    }
    return Collections.unmodifiableMap(metadata);
  }

  public void updateLocation(
      BigDecimal latitude, BigDecimal longitude, String h3Index, LocationSource source) {
    this.currentLatitude = latitude;
    this.currentLongitude = longitude;
    this.currentH3Index = h3Index;
    this.locationSource = source;
    this.lastLocationUpdatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public void updateReliabilityScore(BigDecimal score) {
    if (this.completedDeliveryCount < 5) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Cannot compute reliability score before 5 completed deliveries: count="
              + this.completedDeliveryCount);
    }
    this.reliabilityScore = score;
  }
}
