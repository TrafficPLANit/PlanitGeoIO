package org.goplanit.geoio.converter.service;

import org.geotools.data.DataStore;
import org.goplanit.converter.idmapping.ServiceNetworkIdMapper;
import org.goplanit.converter.service.ServiceNetworkWriter;
import org.goplanit.geoio.converter.GeometryIoWriter;
import org.goplanit.geoio.converter.service.featurecontext.PlanitServiceLegFeatureTypeContext;
import org.goplanit.geoio.converter.service.featurecontext.PlanitServiceLegSegmentFeatureTypeContext;
import org.goplanit.geoio.converter.service.featurecontext.PlanitServiceNodeFeatureTypeContext;
import org.goplanit.geoio.util.GeoIODataStoreManager;
import org.goplanit.geoio.util.GeoIoFeatureTypeBuilder;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.network.layer.ServiceNetworkLayer;
import org.goplanit.utils.network.layer.UntypedDirectedGraphLayer;
import org.goplanit.utils.network.layer.service.ServiceLeg;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;
import org.goplanit.utils.network.layer.service.ServiceNode;
import org.opengis.feature.simple.SimpleFeatureType;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

import static java.util.Map.entry;

/**
 * Writer to persist a PLANit service network to disk in a geometry centric format such as Shape files. Id mapping default is
 * set to internal ids (not XML ids) by default
 * 
 * @author markr
 *
 */
public class GeometryServiceNetworkWriter extends GeometryIoWriter<ServiceNetwork> implements ServiceNetworkWriter {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(GeometryServiceNetworkWriter.class.getCanonicalName());

  /** construct prefix for a given layer in String format */
  private Function<UntypedDirectedGraphLayer<?,?,?>, String> layerPrefixProducer = null;

  /**
   * Based on the settings construct the correct mapping between file names and the PLANit entities
   *
   * @param settings to use
   * @return mapping between PLANit entity class and the chosen file name
   */
  private static Map<Class<?>, String> extractServiceNetworkPlanitEntityBaseFileNames(GeometryServiceNetworkWriterSettings settings) {
    return Map.ofEntries(
            entry(ServiceNode.class, settings.getServiceNodesFileName()),
            entry(ServiceLeg.class, settings.getServiceLegsFileName()),
            entry(ServiceLegSegment.class, settings.getServiceLegSegmentsFileName())
    );
  }

  /** validate before commencing actual write
   *
   * @param serviceNetwork to validate
   */
  private void validate(ServiceNetwork serviceNetwork) {
    /* currently we only support macroscopic infrastructure networks */
    if(!(serviceNetwork.getParentNetwork() instanceof MacroscopicNetwork)) {
      throw new PlanItRunTimeException("Currently the GeometryServiceNetworkWriter only supports parent networks that are macroscopic infrastructure networks, the provided network is not of this type");
    }
  }

  /**
   * Construct consistent file path (with file name) based on desired output file name and settings configuration, taking the
   * current layer into account
   *
   * @param serviceNetworkLayer this applies to
   * @param outputFileName to use
   * @return created path
   */
  private Path createFullPathFromFileName(ServiceNetworkLayer serviceNetworkLayer, String outputFileName){
    return Path.of(
            getSettings().getOutputDirectory(),
            GeoIoFeatureTypeBuilder.createFeatureTypeSchemaName(serviceNetworkLayer, layerPrefixProducer, outputFileName)
                    + getSettings().getFileExtension());
  }

  /**
   * Initialise before actual writing starts. Called from {@link #write(ServiceNetwork)}
   *
   * @param serviceNetwork to writer
   */
  private void initialiseWrite(ServiceNetwork serviceNetwork) {
    getComponentIdMappers().populateMissingIdMappers(getIdMapperType());
    layerPrefixProducer =
            (UntypedDirectedGraphLayer<?,?,?> l) ->
                    String.join("_", "layer", getPrimaryIdMapper().getServiceNetworkLayerIdMapper().apply( (ServiceNetworkLayer) l));

    prepareCoordinateReferenceSystem(
            serviceNetwork.getCoordinateReferenceSystem(), getSettings().getDestinationCoordinateReferenceSystem(), getSettings().getCountry());
  }


  /**
   * Writer the service nodes of the layer
   *
   * @param serviceNetworkLayer          to persist service nodes for
   * @param featureType to use
   * @param serviceNodeFeatureContext the context to convert instances to features
   * @param layerLogPrefix to use
   */
  private void writeServiceNetworkLayerServiceNodes(ServiceNetworkLayer serviceNetworkLayer,
                                      SimpleFeatureType featureType,
                                      PlanitServiceNodeFeatureTypeContext serviceNodeFeatureContext,
                                      String layerLogPrefix) {
    if(featureType==null || serviceNodeFeatureContext == null){
      throw new PlanItRunTimeException("No Feature type description available for PLANit service nodes, this shouldn't happen");
    }
    LOGGER.info(String.format("%s Service nodes: %d", layerLogPrefix, serviceNetworkLayer.getServiceNodes().size()));

    /* data store, e.g., underlying shape file(s) */
    DataStore serviceNodeDataStore = GeoIODataStoreManager.getDataStore(serviceNodeFeatureContext.getPlanitEntityClass());
    if(serviceNodeDataStore == null) {
      serviceNodeDataStore = GeoIODataStoreManager.createDataStore(
              serviceNodeFeatureContext.getPlanitEntityClass(),
              createFullPathFromFileName(serviceNetworkLayer, getSettings().getServiceNodesFileName()));
    }

    /* the feature writer through which to provide each result row */
    final var serviceNodesSchemaName = GeoIoFeatureTypeBuilder.createFeatureTypeSchemaName(
            serviceNetworkLayer, layerPrefixProducer, getSettings().getServiceNodesFileName());

    /* perform persistence */
    writeGeometryLayerForEntity(
        featureType,
        serviceNodeFeatureContext,
        layerLogPrefix,
        serviceNodeDataStore,
        serviceNodesSchemaName,
        serviceNetworkLayer.getServiceNodes(),
        ServiceNode::getPosition);

  }

  /**
   * Writer the service network layer's service legs
   *
   * @param serviceNetworkLayer to persist service legs for
   * @param featureType         to use
   * @param serviceLegFeatureContext  the context to convert instances to features
   * @param layerLogPrefix      to use
   */
  private void writeServiceNetworkLayerServiceLegs(ServiceNetworkLayer serviceNetworkLayer,
                                      SimpleFeatureType featureType,
                                      PlanitServiceLegFeatureTypeContext serviceLegFeatureContext,
                                      String layerLogPrefix) {
    if(featureType==null || serviceLegFeatureContext == null){
      throw new PlanItRunTimeException("No Feature type description available for PLANit service legs, this shouldn't happen");
    }
    LOGGER.info(String.format("%s Service legs: %d", layerLogPrefix, serviceNetworkLayer.getLegs().size()));

    /* data store, e.g., underlying shape file(s) */
    DataStore legDataStore = GeoIODataStoreManager.getDataStore(serviceLegFeatureContext.getPlanitEntityClass());
    if(legDataStore == null) {
      legDataStore = GeoIODataStoreManager.createDataStore(
              serviceLegFeatureContext.getPlanitEntityClass(),
              createFullPathFromFileName(serviceNetworkLayer, getSettings().getServiceLegsFileName()));
    }

    /* the feature writer through which to provide each result row */
    final var legsSchemaName = GeoIoFeatureTypeBuilder.createFeatureTypeSchemaName(
            serviceNetworkLayer, layerPrefixProducer, getSettings().getServiceLegsFileName());

    /* perform persistence */
    writeGeometryLayerForEntity(
        featureType,
        serviceLegFeatureContext,
        layerLogPrefix,
        legDataStore,
        legsSchemaName,
        serviceNetworkLayer.getLegs(),
        ServiceLeg::getGeometry);
  }

  /**
   * Writer the service network layer's service leg segments
   *
   * @param serviceNetworkLayer          to persist link segments for
   * @param featureType to use
   * @param serviceLegSegmentFeatureContext the context to convert instances to features
   * @param layerLogPrefix to use
   */
  private void writeServiceNetworkLayerServiceLegSegments(ServiceNetworkLayer serviceNetworkLayer,
                                      SimpleFeatureType featureType,
                                      PlanitServiceLegSegmentFeatureTypeContext serviceLegSegmentFeatureContext,
                                      String layerLogPrefix) {
    if(featureType==null || serviceLegSegmentFeatureContext == null){
      throw new PlanItRunTimeException("No Feature type description available for PLANit service leg segments, this shouldn't happen");
    }
    LOGGER.info(String.format("%s Service leg segments: %d", layerLogPrefix, serviceNetworkLayer.getLegSegments().size()));

    /* data store, e.g., underlying shape file(s) */
    DataStore serviceLegSegmentsDataStore = GeoIODataStoreManager.getDataStore(serviceLegSegmentFeatureContext.getPlanitEntityClass());
    if(serviceLegSegmentsDataStore == null) {
      serviceLegSegmentsDataStore = GeoIODataStoreManager.createDataStore(
              serviceLegSegmentFeatureContext.getPlanitEntityClass(),
              createFullPathFromFileName(serviceNetworkLayer, getSettings().getServiceLegSegmentsFileName()));
    }

    /* the feature writer through which to provide each result row */
    final var serviceLegSegmentsSchemaName = GeoIoFeatureTypeBuilder.createFeatureTypeSchemaName(
            serviceNetworkLayer, layerPrefixProducer, getSettings().getServiceLegSegmentsFileName());

    /* perform persistence */
    writeGeometryLayerForEntity(
        featureType,
        serviceLegSegmentFeatureContext,
        layerLogPrefix,
        serviceLegSegmentsDataStore,
        serviceLegSegmentsSchemaName,
        serviceNetworkLayer.getLegSegments(),
        ServiceLegSegment::getGeometry);
  }

  /**
   * Write layers of the service network
   *
   * @param serviceNetwork to write layers for
   */
  protected void writeLayers(ServiceNetwork serviceNetwork) {

    /* Ensure all geo features are available and configured for the correct CRS once we start using them */
    for( var layer : serviceNetwork.getTransportLayers()) {

      var supportedFeatures =
          GeoIoFeatureTypeBuilder.createSupportedServiceNetworkLayerFeatures(
              getPrimaryIdMapper(), layer, getComponentIdMappers().getNetworkIdMappers());

      /* feature types per layer */
      var geoFeatureTypesByPlanitEntity =
              GeoIoFeatureTypeBuilder.createSimpleFeatureTypesByLayer(
                  supportedFeatures,
                  layer,
                  getDestinationCoordinateReferenceSystem(),
                  extractServiceNetworkPlanitEntityBaseFileNames(getSettings()),
                  layerPrefixProducer);

      String layerLogPrefix = LoggingUtils.surroundwithBrackets(String.join(" ",
              "layer:",getPrimaryIdMapper().getServiceNetworkLayerIdMapper().apply(layer)));

      /* nodes */
      if(getSettings().isPersistServiceNodes()) {
        LOGGER.info(String.format("%sPersisting service nodes to: %s",
                layerLogPrefix, createFullPathFromFileName(layer, getSettings().getServiceNodesFileName()).toAbsolutePath()));
        var featureInfo = findFeaturePairForPlanitEntity(ServiceNode.class, geoFeatureTypesByPlanitEntity);
        writeServiceNetworkLayerServiceNodes(layer, featureInfo.first(), (PlanitServiceNodeFeatureTypeContext) featureInfo.second(), layerLogPrefix);
      }

      /* links */
      if(getSettings().isPersistServiceLegs()){
        LOGGER.info(String.format("%sPersisting service legs to: %s",
                layerLogPrefix, createFullPathFromFileName(layer, getSettings().getServiceLegsFileName()).toAbsolutePath()));
        var featureInfo = findFeaturePairForPlanitEntity(ServiceLeg.class, geoFeatureTypesByPlanitEntity);
        writeServiceNetworkLayerServiceLegs(layer, featureInfo.first(), (PlanitServiceLegFeatureTypeContext) featureInfo.second(), layerLogPrefix);
      }

      /* link segments */
      if(getSettings().isPersistServiceLegSegments()){
        LOGGER.info(String.format("%sPersisting service leg segments to: %s",
                layerLogPrefix, createFullPathFromFileName(layer, getSettings().getServiceLegSegmentsFileName()).toAbsolutePath()));
        var featureInfo = findFeaturePairForPlanitEntity(ServiceLegSegment.class, geoFeatureTypesByPlanitEntity);
        writeServiceNetworkLayerServiceLegSegments(layer, featureInfo.first(), (PlanitServiceLegSegmentFeatureTypeContext) featureInfo.second(), layerLogPrefix);
      }

    }
  }

  /** Constructor
   *
   */
  protected GeometryServiceNetworkWriter() {
    this(".", CountryNames.GLOBAL);
  }

  /** Constructor
   *
   * @param networkPath to persist network on
   */
  protected GeometryServiceNetworkWriter(String networkPath) {
    this(networkPath, CountryNames.GLOBAL);
  }

  /** Constructor
   *
   * @param serviceNetworkPath to persist service network on
   * @param countryName to optimise projection for (if available, otherwise ignore)
   */
  protected GeometryServiceNetworkWriter(String serviceNetworkPath, String countryName) {
    this(new GeometryServiceNetworkWriterSettings(serviceNetworkPath, countryName));
  }

  /** Constructor
   *
   * @param serviceNetworkSettings to use
   */
  protected GeometryServiceNetworkWriter(GeometryServiceNetworkWriterSettings serviceNetworkSettings){
    super(serviceNetworkSettings);
  }

  /**
   * {@inheritDoc}
   */
//  @Override
  public void write(ServiceNetwork serviceNetwork) {

    validate(serviceNetwork);

    /* initialise */
    initialiseWrite(serviceNetwork);

    /* logging */
    getSettings().logSettings();

    /* perform actual persistence */
    writeLayers(serviceNetwork);

    /* disposes of any registered data stores */
    GeoIODataStoreManager.reset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    GeoIODataStoreManager.reset();
  }
  
  // GETTERS/SETTERS
  
  /**
   * {@inheritDoc}
   */
  @Override
  public GeometryServiceNetworkWriterSettings getSettings() {
    return (GeometryServiceNetworkWriterSettings) super.getSettings();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNetworkIdMapper getPrimaryIdMapper() {
    return getComponentIdMappers().getServiceNetworkIdMapper();
  }
}
