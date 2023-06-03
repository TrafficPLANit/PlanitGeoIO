package org.goplanit.geoio.util;

import org.geotools.data.DataUtilities;
import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.converter.idmapping.ServiceNetworkIdMapper;
import org.goplanit.geoio.converter.network.featurecontext.*;
import org.goplanit.geoio.converter.service.featurecontext.PlanitServiceLegFeatureTypeContext;
import org.goplanit.geoio.converter.service.featurecontext.PlanitServiceLegSegmentFeatureTypeContext;
import org.goplanit.geoio.converter.service.featurecontext.PlanitServiceNodeFeatureTypeContext;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.ServiceNetworkLayer;
import org.goplanit.utils.network.layer.UntypedDirectedGraphLayer;
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
          PlanitEntityFeatureTypeContext<?> featureTypeContext,
          CoordinateReferenceSystem destinationCoordinateReferenceSystem) {

    String sridAddendum = createFeatureGeometrySridAddendum(destinationCoordinateReferenceSystem);

    StringBuilder sb = new StringBuilder();
    featureTypeContext.getAttributeDescription().forEach(e -> {
      sb.append(String.join(FEATURE_KEY_VALUE_DELIMITER, e.first(), e.second()));
      sb.append(FEATURE_DELIMITER);
    });
    /* now append the SRID addendum, assuming the feature ends with the geometry */
    sb.deleteCharAt(sb.length()-1);
    sb.append(sridAddendum);
    return sb.toString();
  }

  /**
   * Construct all PLANit entities that have an associated GIS feature context containing the information require for
   * persistence
   *
   * @param primaryIdMapper to use for id conversion when persisting
   * @param layer used for these features
   * @return available network entity feature context information
   */
  public static Set<PlanitEntityFeatureTypeContext<? extends ManagedId>> createSupportedNetworkLayerFeatures(
          NetworkIdMapper primaryIdMapper, MacroscopicNetworkLayer layer){
    return Set.of(
            /* nodes */
            PlanitNodeFeatureTypeContext.create(primaryIdMapper.getVertexIdMapper()),
            /* links */
            PlanitLinkFeatureTypeContext.create(primaryIdMapper.getLinkIdMapper(), primaryIdMapper.getVertexIdMapper()),
            /* link segments */
            PlanitLinkSegmentFeatureTypeContext.create(primaryIdMapper, layer.getSupportedModes()));
  }

  /**
   * Construct all PLANit entities that have an associated GIS feature context containing the information require for
   * persistence
   *
   * @param primaryIdMapper  to use for id conversion when persisting
   * @param layer            used for these features
   * @param networkIdMappers used for parent ids related to the physical network
   * @return available service network entity feature context information
   */
  public static Set<PlanitEntityFeatureTypeContext<? extends ManagedId>> createSupportedServiceNetworkLayerFeatures(
      ServiceNetworkIdMapper primaryIdMapper, ServiceNetworkLayer layer, NetworkIdMapper networkIdMappers) {
    return Set.of(
        /* service nodes */
        PlanitServiceNodeFeatureTypeContext.create(primaryIdMapper.getServiceNodeIdMapper(), networkIdMappers.getVertexIdMapper()),
        /* legs */
        PlanitServiceLegFeatureTypeContext.create(primaryIdMapper.getServiceLegIdMapper(),primaryIdMapper.getServiceNodeIdMapper()),
        /* leg segments */
        PlanitServiceLegSegmentFeatureTypeContext.create(primaryIdMapper, networkIdMappers));
  }

  /**
   * Initialise all known supported simple feature types for the physical network (for the given layer). Schema names for
   * each feature are constructed via {@link #createFeatureTypeSchemaName(UntypedDirectedGraphLayer, Function, String)}. Hence,
   * when registering on a datastore and then retrieving a feature writer for this feature, make sure to use the same schema name
   * by using this method to retrieve the correct registered schema
   *
   * @param layerFeatures                    write the provided supported features for the layer
   * @param layer                                to create the simple features for
   * @param destinationCoordinateReferenceSystem to use
   * @param planitEntityBaseFileNames            to use which gets prefixed with layer information and post fixed with extension
   * @param layerPrefixProducer                  function that provides a prefix to each layer created feature type's name (may be null)
   * @return the feature types that have been created by physical network layer and all supported PLANit entities
   */
  public static List<Pair<SimpleFeatureType, PlanitEntityFeatureTypeContext<? extends ManagedId>>> createSimpleFeatureTypesByLayer(
          Set<PlanitEntityFeatureTypeContext<? extends ManagedId>> layerFeatures,
          UntypedDirectedGraphLayer<?,?,?> layer,
          CoordinateReferenceSystem destinationCoordinateReferenceSystem,
          Map<Class<?>, String> planitEntityBaseFileNames,
          Function<UntypedDirectedGraphLayer<?,?,?>, String> layerPrefixProducer){

    /** track the created/registered feature types for their respective PLANit entity class */
    final var simpleFeatureTypes = new ArrayList<Pair<SimpleFeatureType, PlanitEntityFeatureTypeContext<? extends ManagedId>>>();

    try {
      for (var featureContext : layerFeatures){

        /* take description  and convert to single string */
        String simpleFeatureTypeString = createFeatureTypeStringFromContext(featureContext, destinationCoordinateReferenceSystem);

        /* create layer aware feature type schema name corresponding to the file name */
        String layerPrefixedSchemaName = createFeatureTypeSchemaName(
                layer, layerPrefixProducer, planitEntityBaseFileNames.get(featureContext.getPlanitEntityClass()));

        /* execute creation of the type */
        var featureType = DataUtilities.createType(layerPrefixedSchemaName, simpleFeatureTypeString);
        simpleFeatureTypes.add(Pair.of(featureType, featureContext));
      }

    }catch(Exception e){
      LOGGER.severe(e.getMessage());
      throw new PlanItRunTimeException("Unable to initialise Simple Feature types for %s", GeoIoFeatureTypeBuilder.class.getCanonicalName());
    }

    return simpleFeatureTypes;
  }

  /**
   * Construct consistent file path (with file name) based on desired output file name and settings configuration, taking the
   * current layer into account
   *
   * @param directedGraphlayer this applies to
   * @param layerPrefixProducer to use to convert layer into a prefix
   * @param baseFileName to combine with
   * @return created featureTypeSchemaName
   */
  public static String createFeatureTypeSchemaName(
          UntypedDirectedGraphLayer<?,?,?> directedGraphlayer,
          Function<UntypedDirectedGraphLayer<?,?,?>, String> layerPrefixProducer,
          String baseFileName){
    String layerPrefix = (layerPrefixProducer!= null ? layerPrefixProducer.apply(directedGraphlayer) : "");
    if(StringUtils.isNullOrBlank(layerPrefix)){
      LOGGER.warning(String.format("IGNORE: Layer prefix for PLANit feature is null or blank, this shouldn't happen", layerPrefix));
      return null;
    }
    if(StringUtils.isNullOrBlank(baseFileName)){
      LOGGER.warning(String.format("IGNORE: Feature name not provided for PLANit entity in layer (%s) feature schema, this shouldn't happen", layerPrefix));
      return null;
    }
    return String.join("_", layerPrefix, baseFileName);
  }

}
