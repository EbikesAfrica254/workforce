package com.ebikes.workforce.support.fixtures;

import java.math.BigDecimal;
import java.util.UUID;

import com.ebikes.workforce.database.entities.LocationHistory;
import com.ebikes.workforce.enums.LocationSource;

public final class LocationHistoryFixtures {

  public static final UUID LOCATION_HISTORY_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000060");

  private static final BigDecimal DEFAULT_LATITUDE = new BigDecimal("-1.286389");
  private static final BigDecimal DEFAULT_LONGITUDE = new BigDecimal("36.817223");
  private static final String DEFAULT_H3_INDEX = "8763a4e6fffffff";

  private LocationHistoryFixtures() {}

  public static LocationHistory itrackDevice(UUID agentId) {
    return base(agentId, LocationSource.ITRACK_DEVICE).build();
  }

  public static LocationHistory mobileBrowser(UUID agentId) {
    return base(agentId, LocationSource.MOBILE_BROWSER).build();
  }

  public static LocationHistory webBrowser(UUID agentId) {
    return base(agentId, LocationSource.WEB_BROWSER).build();
  }

  public static LocationHistory withCoordinates(
      UUID agentId,
      BigDecimal latitude,
      BigDecimal longitude,
      String h3Index,
      LocationSource source) {
    return base(agentId, source).latitude(latitude).longitude(longitude).h3Index(h3Index).build();
  }

  private static LocationHistory.LocationHistoryBuilder<?, ?> base(
      UUID agentId, LocationSource source) {
    return LocationHistory.builder()
        .id(LOCATION_HISTORY_ID)
        .agentId(agentId)
        .latitude(DEFAULT_LATITUDE)
        .longitude(DEFAULT_LONGITUDE)
        .h3Index(DEFAULT_H3_INDEX)
        .source(source);
  }
}
