package com.ebikes.workforce.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "workforce.shortlist")
@Data
public class ShortlistProperties {
  private int defaultRingSize = 2;
  private int expirySeconds = 120;
  private int freshThresholdMinutes = 10;
}
