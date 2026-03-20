package com.ebikes.workforce.configurations;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.uber.h3core.H3Core;

@Configuration
public class H3Configuration {

  @Bean
  public H3Core h3Core() throws IOException {
    return H3Core.newInstance();
  }
}
