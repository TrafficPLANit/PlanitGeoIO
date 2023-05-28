package org.goplanit.geoio.util;

import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.mode.Mode;

import java.util.function.Function;

/**
 * Traditional GIS formats like shape have limits on length of attribute names. Here we provide
 * short names for all modes
 */
public class ModeShortNameConverter {

  /**
   * As short name with 5 characters or less. For custom modes, use mode name if less than 5 characters, or otherwise
   * the id (which we support up to 4 characters, yielding 'm' followed by the id).
   *
   * @param mode to get short name for
   * @param modeIdMapper to apply for custom modes if needed
   * @return short name used
   */
  public static String asShortName(Mode mode, Function<Mode, String> modeIdMapper){
    switch (mode.getPredefinedModeType()){
      case CAR:
      case BUS:
      case TRAIN:
      case TRAM:
      case FERRY:
      case GOODS_VEHICLE:
      case HEAVY_GOODS_VEHICLE:
      case LARGE_HEAVY_GOODS_VEHICLE:
        return mode.getPredefinedModeType().value();
      case BICYCLE:
        return "cycle";
      case CAR_SHARE:
        return "crsh";
      case CAR_HIGH_OCCUPANCY:
        return "crhov";
      case PEDESTRIAN:
        return "pdstr";
      case MOTOR_BIKE:
        return "mtrbk";
      case SUBWAY:
        return "sbway";
      case LIGHTRAIL:
        return "lrail";
      case CUSTOM:
        if(mode.getName().length()<6){
          return mode.getName();
        }else if(modeIdMapper.apply(mode).length()<5){
          return "m" + modeIdMapper.apply(mode);
        }else{
          throw new PlanItRunTimeException("Unable to create short name for mode %s, likely exceeds 5 characters", mode.toString());
        }
    }
    return "";
  }
}
