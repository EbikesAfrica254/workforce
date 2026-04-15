package com.ebikes.workforce.configurations.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "workforce.h3")
@Setter
@Getter
public class H3Properties {

  private int resolution = 9;
  private int ringSize = 2;
}
