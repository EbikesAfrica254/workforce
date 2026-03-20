package com.ebikes.workforce.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "workforce.h3")
@Getter
@Setter
public class H3Properties {

  private int resolution = 9;
  private int ringSize = 2;
}
