package com.ebikes.workforce.support.capability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.enums.VehicleClass;

@DisplayName("CapabilityClassResolver")
class CapabilityClassResolverTest {

  @Nested
  @DisplayName("fromVehicleClass")
  class FromVehicleClass {

    static Stream<Arguments> vehicleClassMappings() {
      return Stream.of(
          Arguments.of(VehicleClass.BICYCLE, CapabilityClass.BICYCLE_RIDER),
          Arguments.of(VehicleClass.CAR, CapabilityClass.VEHICLE_DRIVER),
          Arguments.of(VehicleClass.MOTORCYCLE, CapabilityClass.LIGHT_MOTOR_RIDER));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @DisplayName("should map each VehicleClass to the correct CapabilityClass set")
    @MethodSource("vehicleClassMappings")
    void shouldMapVehicleClassToCapabilityClass(
        VehicleClass vehicleClass, CapabilityClass expected) {
      assertThat(CapabilityClassResolver.fromVehicleClass(vehicleClass)).containsExactly(expected);
    }
  }

  @Nested
  @DisplayName("toVehicleClass")
  class ToVehicleClass {

    static Stream<Arguments> capabilityClassMappings() {
      return Stream.of(
          Arguments.of(CapabilityClass.BICYCLE_RIDER, VehicleClass.BICYCLE),
          Arguments.of(CapabilityClass.LIGHT_MOTOR_RIDER, VehicleClass.MOTORCYCLE),
          Arguments.of(CapabilityClass.VEHICLE_DRIVER, VehicleClass.CAR));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @DisplayName("should map each CapabilityClass to the correct VehicleClass")
    @MethodSource("capabilityClassMappings")
    void shouldMapCapabilityClassToVehicleClass(
        CapabilityClass capabilityClass, VehicleClass expected) {
      assertThat(CapabilityClassResolver.toVehicleClass(capabilityClass)).isEqualTo(expected);
    }
  }
}
