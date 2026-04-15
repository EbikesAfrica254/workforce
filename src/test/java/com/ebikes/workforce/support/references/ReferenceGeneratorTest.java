package com.ebikes.workforce.support.references;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.ebikes.workforce.enums.ChannelType;

@DisplayName("ReferenceGenerator")
class ReferenceGeneratorTest {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

  private static String today() {
    return LocalDate.now().format(DATE_FORMATTER);
  }

  @Nested
  @DisplayName("generateErrorReference")
  class GenerateErrorReference {

    @Test
    @DisplayName("should produce different references on successive calls")
    void shouldProduceDifferentReferencesOnSuccessiveCalls() {
      assertThat(ReferenceGenerator.generateErrorReference())
          .isNotEqualTo(ReferenceGenerator.generateErrorReference());
    }

    @Test
    @DisplayName("should contain today's date in the reference")
    void shouldContainTodaysDate() {
      assertThat(ReferenceGenerator.generateErrorReference()).contains(today());
    }

    @Test
    @DisplayName("should match format ERR-{yyyyMMdd}-{6 uppercase alphanumeric chars}")
    void shouldMatchExpectedFormat() {
      assertThat(ReferenceGenerator.generateErrorReference()).matches("ERR-\\d{8}-[A-Z0-9]{6}");
    }

    @Test
    @DisplayName("should start with ERR-")
    void shouldStartWithErrorPrefix() {
      assertThat(ReferenceGenerator.generateErrorReference()).startsWith("ERR-");
    }
  }

  @Nested
  @DisplayName("generateMessageReference")
  class GenerateMessageReference {

    static Stream<ChannelType> allChannels() {
      return Stream.of(ChannelType.values());
    }

    @Test
    @DisplayName("should contain today's date in the reference")
    void shouldContainTodaysDate() {
      assertThat(ReferenceGenerator.generateMessageReference(ChannelType.SMS)).contains(today());
    }

    @Test
    @DisplayName("should embed the channel name in the reference")
    void shouldEmbedChannelName() {
      String reference = ReferenceGenerator.generateMessageReference(ChannelType.EMAIL);
      assertThat(reference).contains("EMAIL");
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("should match format MSG-{CHANNEL}-{yyyyMMdd}-{8 uppercase alphanumeric chars}")
    @MethodSource("allChannels")
    void shouldMatchExpectedFormatForAllChannels(ChannelType channel) {
      assertThat(ReferenceGenerator.generateMessageReference(channel))
          .matches("MSG-" + channel.name() + "-\\d{8}-[A-Z0-9]{8}");
    }

    @Test
    @DisplayName("should produce different references on successive calls")
    void shouldProduceDifferentReferencesOnSuccessiveCalls() {
      assertThat(ReferenceGenerator.generateMessageReference(ChannelType.SMS))
          .isNotEqualTo(ReferenceGenerator.generateMessageReference(ChannelType.SMS));
    }

    @Test
    @DisplayName("should start with MSG-")
    void shouldStartWithMessagePrefix() {
      assertThat(ReferenceGenerator.generateMessageReference(ChannelType.SMS)).startsWith("MSG-");
    }
  }

  @Nested
  @DisplayName("generateServiceReference")
  class GenerateServiceReference {

    @Test
    @DisplayName("should contain a valid UUID suffix after the colon")
    void shouldContainValidUuidSuffix() {
      String reference = ReferenceGenerator.generateServiceReference("my-service");
      String uuidPart = reference.substring(reference.indexOf(':') + 1);
      assertThat(UUID.fromString(uuidPart)).isNotNull();
    }

    @Test
    @DisplayName("should preserve the service name exactly")
    void shouldPreserveServiceNameExactly() {
      assertThat(ReferenceGenerator.generateServiceReference("my-service"))
          .startsWith("my-service:");
    }

    @Test
    @DisplayName("should produce different references on successive calls")
    void shouldProduceDifferentReferencesOnSuccessiveCalls() {
      assertThat(ReferenceGenerator.generateServiceReference("svc"))
          .isNotEqualTo(ReferenceGenerator.generateServiceReference("svc"));
    }

    @Test
    @DisplayName("should use colon as delimiter between service name and UUID")
    void shouldUseColonAsDelimiter() {
      assertThat(ReferenceGenerator.generateServiceReference("svc")).contains("svc:");
    }
  }
}
