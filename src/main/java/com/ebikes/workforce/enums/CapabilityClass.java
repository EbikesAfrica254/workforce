package com.ebikes.workforce.enums;

import java.util.Set;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CapabilityClass {
  BICYCLE_RIDER(
      Set.of(DocumentType.NATIONAL_ID_BACK, DocumentType.NATIONAL_ID_FRONT),
      Set.of(CertificationType.GOOD_CONDUCT_CERTIFICATE),
      (short) 2),
  LIGHT_MOTOR_RIDER(
      Set.of(
          DocumentType.MOTORCYCLE_LICENSE_COPY,
          DocumentType.NATIONAL_ID_BACK,
          DocumentType.NATIONAL_ID_FRONT),
      Set.of(
          CertificationType.GOOD_CONDUCT_CERTIFICATE,
          CertificationType.NTSA_DRIVING_LICENSE_CLASS_G),
      (short) 3),
  VEHICLE_DRIVER(
      Set.of(
          DocumentType.NATIONAL_ID_BACK,
          DocumentType.NATIONAL_ID_FRONT,
          DocumentType.VEHICLE_LICENSE_COPY),
      Set.of(
          CertificationType.GOOD_CONDUCT_CERTIFICATE,
          CertificationType.NTSA_DRIVING_LICENSE_CLASS_BCE),
      (short) 5);

  private final Set<DocumentType> requiredDocuments;
  private final Set<CertificationType> requiredCertifications;
  private final short defaultMaxConcurrentOrders;
}
