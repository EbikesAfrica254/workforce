package com.ebikes.workforce.enums;

import java.util.Set;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DocumentType {
  DEFENSIVE_DRIVING_CERTIFICATE_COPY(DocumentContentTypes.ALL, false),
  FIRST_AID_CERTIFICATE_COPY(DocumentContentTypes.ALL, false),
  GOOD_CONDUCT_CERTIFICATE_COPY(DocumentContentTypes.ALL, true),
  MOTORCYCLE_LICENSE_COPY(DocumentContentTypes.ALL, true),
  NATIONAL_ID_BACK(DocumentContentTypes.ALL, false),
  NATIONAL_ID_FRONT(DocumentContentTypes.ALL, false),
  VEHICLE_LICENSE_COPY(DocumentContentTypes.ALL, true);

  private final Set<String> supportedContentTypes;
  private final boolean requiresExpiryDate;

  private static final class DocumentContentTypes {
    static final Set<String> ALL = Set.of("application/pdf", "image/jpeg", "image/png");
  }
}
