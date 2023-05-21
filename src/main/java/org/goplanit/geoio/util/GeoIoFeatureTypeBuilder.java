package org.goplanit.geoio.util;

import org.geotools.data.DataUtilities;
import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.geoio.converter.network.featurecontext.PlanitNodeFeatureTypeContext;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

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
   * Construct  feature type string based on the provided context information
   *
   * @param featureTypeContext to extract feature type information from
   * @param destinationCoordinateReferenceSystem destination CRS
   * @return created feature type string representative for this PLANit entity
   */
  private static String createFeatureTypeStringFromContext(
          PlanitNodeFeatureTypeContext featureTypeContext,
          CoordinateReferenceSystem destinationCoordinateReferenceSystem) {

    String sridAddendum = createFeatureGeometrySridAddendum(destinationCoordinateReferenceSystem);

    StringBuilder sb = new StringBuilder();
    featureTypeContext.getAttributeDescription().forEach(e -> {
      sb.append(String.join(FEATURE_KEY_VALUE_DELIMITER, e.first(), e.second()));
      sb.append(FEATURE_DELIMITER);
    });
    /* now append the SRID addendum, assuming the feature ends with the geometry */
    sb.append(sridAddendum);
    return sb.toString();
  }

  /**
   * Initialise all known supported simple feature types for the physical network (for the given layer). Schema names for
   * each feature are constructed via {@link #createFeatureTypeSchemaName(MacroscopicNetworkLayer, Function, String)}. Hence,
   * when registering on a datastore and then retrieving a feature writer for this feature, make sure to use the same schema name
   * by using this method to retrieve the correct registered schema
   *
   * @param primaryIdMapper                      id mappers in use
   * @param layer                                to create the simple features for
   * @param destinationCoordinateReferenceSystem to use
   * @param planitEntityBaseFileNames            to use which gets prefixed with layer information and post fixed with extension
   * @param layerPrefixProducer                  function that provides a prefix to each layer created feature type's name (may be null)
   * @return the feature types that have been created by physical network layer and all supported PLANit entities
   */
  public static List<Pair<SimpleFeatureType, PlanitEntityFeatureTypeContext<?>>> createPhysicalNetworkSimpleFeatureTypesByLayer(
          NetworkIdMapper primaryIdMapper,
          MacroscopicNetworkLayer layer,
          CoordinateReferenceSystem destinationCoordinateReferenceSystem,
          Map<Class<?>, String> planitEntityBaseFileNames,
          Function<MacroscopicNetworkLayer, String> layerPrefixProducer){

    /** track the created/registered feature types for their respective PLANit entity class */
    final var simpleFeatureTypes = new ArrayList<Pair<SimpleFeatureType, PlanitEntityFeatureTypeContext<?>>>();

    try {

      var featureTypeContext =  PlanitNodeFeatureTypeContext.create(primaryIdMapper.getVertexIdMapper());

      /* take description  and convert to single string */
      String simpleFeatureTypeString = createFeatureTypeStringFromContext(featureTypeContext, destinationCoordinateReferenceSystem);

      /* create layer aware feature type schema name corresponding to the file name */
      String layerPrefixedSchemaName = createFeatureTypeSchemaName(
              layer, layerPrefixProducer, planitEntityBaseFileNames.get(featureTypeContext.getPlanitEntityClass()));

      /* execute creation of the type */
      var featureType = DataUtilities.createType(layerPrefixedSchemaName, simpleFeatureTypeString);
      simpleFeatureTypes.add(Pair.of(featureType, featureTypeContext));

    }catch(Exception e){
      throw new PlanItRunTimeException("Unable to initialise Simple Feature types for %s", GeoIoFeatureTypeBuilder.class.getCanonicalName());
    }

    return simpleFeatureTypes;
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
