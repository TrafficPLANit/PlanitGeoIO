package org.goplanit.geoio.util;

import org.geotools.data.DataUtilities;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.NetworkLayer;
import org.goplanit.utils.network.layer.physical.Node;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Utility class that builds feature types for supported PLANit entities per layer and chosen destination CRS that
 * the GeoIO writer supports
 *
 * @author markr
 */
public final class GeoIoFeatureTypeBuilder {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(GeoIoFeatureTypeBuilder.class.getCanonicalName());

  /** layer and extension agnostic name for PLANit nodes output file */
  public static final String PLANIT_NODES = "planit_nodes";

  /** layer and extension agnostic name for PLANit links output file */
  public static final String PLANIT_LINKS = "planit_links";

  /** delimiter for separating attribute key from its type within each feature */
  private static final String FEATURE_KEY_VALUE_DELIMITER = ":";

  /** delimiter for separating between attribute value pairs within each feature */
  private static final String FEATURE_DELIMITER = ",";

  /** the geotools geometry attribute name used */
  public static final String GEOTOOLS_GEOMETRY_ATTRIBUTE = "the_geom";

  /**
   * Feature description for PLANit node. (default) geometry must be last as SRID addendum is appended
   */
  public static final List<Pair<String,String>> PLANIT_NODE_FEATURE_DESCRIPTION = List.of(
          Pair.of("node_id", "java.lang.Long"),
          Pair.of("name", "String"),
          Pair.of("*geom", "Point")
  );


  /** Track all feature descriptions for the PLANit physical network */
  public static final List<PlanitEntityFeatureTypeContext> PLANIT_PHYSICAL_NETWORK_FEATURE_DESCRIPTIONS = List.of(
          PlanitEntityFeatureTypeContext.of(Node.class, PLANIT_NODE_FEATURE_DESCRIPTION)
          //todo add the rest here
  );


  /**
   * Create the addendum to each geometry entry to signify its srid based on the chosen destination CRS
   *
   * @param destinationCoordinateReferenceSystem destination CRS to use
   * @return Srid addencum string, e.g., srid=_code_ if found, otherwise empty string
   */
  protected static String createFeatureGeometrySridAddendum(CoordinateReferenceSystem destinationCoordinateReferenceSystem){
    String sridCodeAddendum = "";
    if(destinationCoordinateReferenceSystem == null){
      LOGGER.warning("Destination CRS null, ignoring attaching it to PLANit feature types");
      return sridCodeAddendum;
    }

    var identifiers = destinationCoordinateReferenceSystem.getIdentifiers();
    if(identifiers == null || identifiers.isEmpty()){
      LOGGER.warning(String.format("No identifiers to extract EPSG/SRID from Destination CRS %s, ignoring attaching it to PLANit feature types", destinationCoordinateReferenceSystem.getName()));
    }else{
      sridCodeAddendum = ":srid="+identifiers.stream().findFirst().get().getCode();
    }
    return sridCodeAddendum;
  }

  /**
   * Initialise all known supported simple feature types for the physical network (for the given layer)
   *
   * @param layer to create the simple features for
   * @param destinationCoordinateReferenceSystem to use
   * @param planitEntityBaseFileNames to use which gets prefixed with layer information and post fixed with extension
   * @param layerPrefixProducer function that provides a prefix to each layer created feature type's name (may be null)
   * @return the feature types that have been created by physical network layer and all supported PLANit entities
   */
  public static Map<Class<?>, SimpleFeatureType> createPhysicalNetworkSimpleFeatureTypesByLayer(
          MacroscopicNetworkLayer layer,
          CoordinateReferenceSystem destinationCoordinateReferenceSystem,
          Map<Class<?>, String> planitEntityBaseFileNames,
          Function<MacroscopicNetworkLayer, String> layerPrefixProducer){

    String sridAddendum = createFeatureGeometrySridAddendum(destinationCoordinateReferenceSystem);
    String layerPrefix = (layerPrefixProducer!= null ? layerPrefixProducer.apply(layer) : "");

    /** track the created/registered feature types for their respective PLANit entity class */
    final Map<Class<?>, SimpleFeatureType> simpleFeatureTypes = new HashMap<>();

    try {
      //todo add crs, see https://www.geomesa.org/documentation/2.0.2/user/datastores/attributes.html, and https://docs.geotools.org/latest/userguide/library/main/data.html

      for(var featureTypeDescription : PLANIT_PHYSICAL_NETWORK_FEATURE_DESCRIPTIONS){
        /* take description  and convert to single string */
        String simpleFeatureTypeString = featureTypeDescription.getGeoDescription().stream().map(
                e -> String.join(FEATURE_KEY_VALUE_DELIMITER, e.first(), e.second())).collect(Collectors.joining(FEATURE_DELIMITER));

        /* now append the SRID addendum, assuming the feature ends with the geometry */
        simpleFeatureTypeString = simpleFeatureTypeString + sridAddendum;

        /* create layer aware feature type schema name corresponding to the file name */
        String layerPrefixedSchemaName = createFeatureTypeSchemaName(
                layer, layerPrefixProducer, planitEntityBaseFileNames.get(featureTypeDescription.getPlanitEntityClass()));

        /* execute creation of the type */
        var featureType = DataUtilities.createType(layerPrefixedSchemaName, simpleFeatureTypeString);
        simpleFeatureTypes.put(Node.class,featureType);
      }

    }catch(Exception e){
      throw new PlanItRunTimeException("Unable to initialise Simple Feature types for %s", GeoIoFeatureTypeBuilder.class.getCanonicalName());
    }

    return simpleFeatureTypes;
  }

  /** Provide all currently supported PLANit entity classes that are supported
   *
   * @return all supported classes
   * */
  public static Set<Class<?>> getPhysicalNetworkSupportedPlanitEntityClasses(){
    return PLANIT_PHYSICAL_NETWORK_FEATURE_DESCRIPTIONS.stream().map(e -> e.getPlanitEntityClass()).collect(Collectors.toSet());
  }

  /**
   * Construct consistent file path (with file name) based on desired output file name and settings configuration, taking the
   * current layer into account
   *
   * @param physicalNetworkLayer this applies to
   * @param layerPrefixProducer to use to convert layer into a prefix
   * @param baseFileName to combine with
   * @return created featureTypeSchemaName
   */
  public static String createFeatureTypeSchemaName(
          MacroscopicNetworkLayer physicalNetworkLayer,
          Function<MacroscopicNetworkLayer, String> layerPrefixProducer,
          String baseFileName){
    String layerPrefix = (layerPrefixProducer!= null ? layerPrefixProducer.apply(physicalNetworkLayer) : "");
    return String.join("_", layerPrefix, baseFileName);
  }

}
