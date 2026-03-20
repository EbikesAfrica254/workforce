package com.ebikes.workforce.support.workforce;

import java.util.Set;

import com.ebikes.workforce.enums.CapabilityClass;
import com.ebikes.workforce.enums.VehicleClass;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CapabilityClassMapper {

  public static VehicleClass toVehicleClass(CapabilityClass capabilityClass) {
    return switch (capabilityClass) {
      case BICYCLE_RIDER -> VehicleClass.BICYCLE;
      case LIGHT_MOTOR_RIDER -> VehicleClass.MOTORCYCLE;
      case VEHICLE_DRIVER -> VehicleClass.CAR;
    };
  }

  public static Set<CapabilityClass> fromVehicleClass(VehicleClass vehicleClass) {
    return switch (vehicleClass) {
      case BICYCLE -> Set.of(CapabilityClass.BICYCLE_RIDER);
      case MOTORCYCLE -> Set.of(CapabilityClass.LIGHT_MOTOR_RIDER);
      case CAR -> Set.of(CapabilityClass.VEHICLE_DRIVER);
    };
  }
}
