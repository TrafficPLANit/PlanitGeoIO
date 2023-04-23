package org.goplanit.geoio.util;

import org.geotools.data.DataUtilities;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.network.layer.physical.Node;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Utility class that tracks all the feature type conversions for each PLANit entity that the GeoIO writer supports
 */
public final class GeoIoFeatureTypeManager {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(GeoIoFeatureTypeManager.class.getCanonicalName());

  private static final Map<Class<?>, SimpleFeatureType> simpleFeatureTypesByPlanitEntity = new HashMap<>();

  /**
   * Initialise all known supported simple feature types
   */
  public static void initialiseSimpleFeatureTypes(CoordinateReferenceSystem destinationCoordinateReferenceSystem){
    String sridCodeAddendum = "";
    if(destinationCoordinateReferenceSystem == null){
      LOGGER.warning("Destination CRS null, ignoring attaching it to PLANit feature types");
    }else{
      var identifiers = destinationCoordinateReferenceSystem.getIdentifiers();
      if(identifiers == null || identifiers.isEmpty()){
        LOGGER.warning(String.format("No identifiers to extract EPSG/SRID from Destination CRS %s, ignoring attaching it to PLANit feature types", destinationCoordinateReferenceSystem.getName()));
      }else{
        sridCodeAddendum = ":srid="+identifiers.stream().findFirst().get().getCode();
      }

    }

    try {
      //todo add crs, see https://www.geomesa.org/documentation/2.0.2/user/datastores/attributes.html, and https://docs.geotools.org/latest/userguide/library/main/data.html

      var featureType = DataUtilities.createType(
              "planit_nodes", "node_id:java.lang.Long,name:String,*geom:Point"+sridCodeAddendum); //the * means it is the default geometry type otherwise that gets ignored
      simpleFeatureTypesByPlanitEntity.put(Node.class,featureType);
      //todo add other entities here
    }catch(Exception e){
      throw new PlanItRunTimeException("Unable to initialise Simple Feature types for %s", GeoIoFeatureTypeManager.class.getCanonicalName());
    }
  }

  /**
   * Collect the simple feature type that goes with the given PLANit entity. PlanItRunTimeException when no known
   * feature type exists for the given parameter.
   * <p>
   *   Make sure {@link #initialiseSimpleFeatureTypes(CoordinateReferenceSystem)} has been called to ensure the types are available for the chosen
   *   coordinate reference system
   * </p>
   *
   * @param planitEntityClass                    to collect feature type for
   * @return feature type found
   */
  public static SimpleFeatureType getSimpleFeatureType(Class<?> planitEntityClass){
    if(!simpleFeatureTypesByPlanitEntity.containsKey(planitEntityClass)){
      throw new PlanItRunTimeException("Unknown or uninitialised feature type schema requested for PLANit entity %s, abort", planitEntityClass.toString());
    }

    return simpleFeatureTypesByPlanitEntity.get(planitEntityClass);
  }
}
