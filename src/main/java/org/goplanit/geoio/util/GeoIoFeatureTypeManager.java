package org.goplanit.geoio.util;

import org.geotools.data.DataUtilities;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.network.layer.physical.Node;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.w3.xlink.Simple;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class that tracks all the feature type conversions for each PLANit entity that the GeoIO writer supports
 */
public final class GeoIoFeatureTypeManager {

  private static final Map<Class<?>, SimpleFeatureType> simpleFeatureTypesByPlanitEntity = new HashMap<>();

  static{
    initialiseSimpleFeatureTypes();
  }

  /**
   * Initialise all known supported simple feature types
   */
  private static void initialiseSimpleFeatureTypes(){
    try {
      //todo add crs, see https://www.geomesa.org/documentation/2.0.2/user/datastores/attributes.html, and https://docs.geotools.org/latest/userguide/library/main/data.html
      //todo: typename appears to ha
      simpleFeatureTypesByPlanitEntity.put(Node.class, DataUtilities.createType("nodes", "node_id:java.lang.Long,name:String,geom:Point"));
      //todo add other entities here
    }catch(Exception e){
      throw new PlanItRunTimeException("Unable to initialise Simple Feature types for %s", GeoIoFeatureTypeManager.class.getCanonicalName());
    }
  }

  /**
   * Collect the simple feature type that goes with the given PLANit entity. PlanItRunTimeException when no known
   * feature type exists for the given parameter
   *
   * @param planitEntityClass to collect feature type for
   * @return feature type found
   */
  public static SimpleFeatureType getSimpleFeatureType(Class<?> planitEntityClass){
    if(!simpleFeatureTypesByPlanitEntity.containsKey(planitEntityClass)){
      throw new PlanItRunTimeException("Unknown feature type schema requested for PLANit entity %s, abort", planitEntityClass.toString());
    }

    return simpleFeatureTypesByPlanitEntity.get(planitEntityClass);
  }
}
