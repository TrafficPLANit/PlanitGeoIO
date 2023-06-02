package org.goplanit.geoio.converter.network;

import org.geotools.data.DataStore;
import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.converter.network.NetworkWriter;
import org.goplanit.geoio.converter.GeometryIoWriter;
import org.goplanit.geoio.converter.network.featurecontext.PlanitLinkFeatureTypeContext;
import org.goplanit.geoio.converter.network.featurecontext.PlanitLinkSegmentFeatureTypeContext;
import org.goplanit.geoio.converter.network.featurecontext.PlanitNodeFeatureTypeContext;
import org.goplanit.geoio.util.GeoIODataStoreManager;
import org.goplanit.geoio.util.GeoIoFeatureTypeBuilder;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.UntypedDirectedGraphLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.physical.Node;
import org.opengis.feature.simple.SimpleFeatureType;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

import static java.util.Map.entry;

/**
 * Writer to persist a PLANit network to disk in a geometry centric format such as Shape files. Id mapping default is
 * set to internal ids (not XML ids) by default
 * 
 * @author markr
 *
 */
public class GeometryNetworkWriter extends GeometryIoWriter<LayeredNetwork<?,?>> implements NetworkWriter {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(GeometryNetworkWriter.class.getCanonicalName());

  /** construct prefix for a given layer in String format */
  private Function<UntypedDirectedGraphLayer<?,?,?>, String> layerPrefixProducer = null;

  /**
   * Based on the settings construct the correct mapping between file names and the PLANit entities
   *
   * @param settings to use
   * @return mapping between PLANit entity class and the chosen file name
   */
  private static Map<Class<?>, String> extractPhysicalNetworkPlanitEntityBaseFileNames(GeometryNetworkWriterSettings settings) {
    return Map.ofEntries(
            entry(Node.class, settings.getNodesFileName()),
            entry(MacroscopicLink.class, settings.getLinksFileName()),
            entry(MacroscopicLinkSegment.class, settings.getLinkSegmentsFileName())
    );
  }


  /** validate before commencing actual write
   *
   * @param network to validate
   */
  private void validate(LayeredNetwork<?,?> network) {
    /* currently we only support macroscopic infrastructure networks */
    if(!(network instanceof MacroscopicNetwork)) {
      throw new PlanItRunTimeException("Currently the GeometryNetworkWriter only supports macroscopic infrastructure networks, the provided network is not of this type");
    }
  }

  /**
   * Construct consistent file path (with file name) based on desired output file name and settings configuration, taking the
   * current layer into account
   *
   * @param physicalNetworkLayer this applies to
   * @param outputFileName to use
   * @return created path
   */
  private Path createFullPathFromFileName(MacroscopicNetworkLayer physicalNetworkLayer, String outputFileName){
    return Path.of(
            getSettings().getOutputDirectory(),
            GeoIoFeatureTypeBuilder.createFeatureTypeSchemaName(physicalNetworkLayer, layerPrefixProducer, outputFileName)
                    + getSettings().getFileExtension());
  }

  /**
   * Initialise before actual writing starts. Called from {@link #write(LayeredNetwork)}
   *
   * @param macroscopicNetwork to writer
   */
  private void initialiseWrite(MacroscopicNetwork macroscopicNetwork) {
    getComponentIdMappers().populateMissingIdMappers(getIdMapperType());
    layerPrefixProducer =
            (UntypedDirectedGraphLayer<?,?,?> l) ->
                    String.join("_", "layer", getPrimaryIdMapper().getNetworkLayerIdMapper().apply( (MacroscopicNetworkLayer) l));

    prepareCoordinateReferenceSystem(
            macroscopicNetwork.getCoordinateReferenceSystem(), getSettings().getDestinationCoordinateReferenceSystem(), getSettings().getCountry());
  }

  /**
   * Writer the network layer's nodes
   *
   * @param physicalNetworkLayer          to persist nodes for
   * @param featureType to use
   * @param nodeFeatureContext the context to convert instances to features
   * @param layerLogPrefix to use
   */
  private void writeNetworkLayerNodes(MacroscopicNetworkLayer physicalNetworkLayer,
                                      SimpleFeatureType featureType,
                                      PlanitNodeFeatureTypeContext nodeFeatureContext,
                                      String layerLogPrefix) {
    if(featureType==null || nodeFeatureContext == null){
      throw new PlanItRunTimeException("No Feature type description available for PLANit nodes, this shouldn't happen");
    }
    LOGGER.info(String.format("%s Nodes: %d", layerLogPrefix, physicalNetworkLayer.getNodes().size()));

    /* data store, e.g., underlying shape file(s) */
    DataStore nodeDataStore = GeoIODataStoreManager.getDataStore(nodeFeatureContext.getPlanitEntityClass());
    if(nodeDataStore == null) {
      nodeDataStore = GeoIODataStoreManager.createDataStore(
              nodeFeatureContext.getPlanitEntityClass(),
              createFullPathFromFileName(physicalNetworkLayer, getSettings().getNodesFileName()));
    }

    /* the feature writer through which to provide each result row */
    final var nodesSchemaName = GeoIoFeatureTypeBuilder.createFeatureTypeSchemaName(
            physicalNetworkLayer, layerPrefixProducer, getSettings().getNodesFileName());

    /* perform persistence */
    writeGeometryLayerForEntity(
            featureType, nodeFeatureContext, layerLogPrefix, nodeDataStore, nodesSchemaName, physicalNetworkLayer.getNodes(), Node::getPosition);

  }

  /**
   * Writer the network layer's links
   *
   * @param physicalNetworkLayer          to persist links for
   * @param featureType to use
   * @param linkFeatureContext the context to convert instances to features
   * @param layerLogPrefix to use
   */
  private void writeNetworkLayerLinks(MacroscopicNetworkLayer physicalNetworkLayer,
                                      SimpleFeatureType featureType,
                                      PlanitLinkFeatureTypeContext linkFeatureContext,
                                      String layerLogPrefix) {
    if(featureType==null || linkFeatureContext == null){
      throw new PlanItRunTimeException("No Feature type description available for PLANit links, this shouldn't happen");
    }
    LOGGER.info(String.format("%s Links: %d", layerLogPrefix, physicalNetworkLayer.getLinks().size()));

    /* data store, e.g., underlying shape file(s) */
    DataStore linksDataStore = GeoIODataStoreManager.getDataStore(linkFeatureContext.getPlanitEntityClass());
    if(linksDataStore == null) {
      linksDataStore = GeoIODataStoreManager.createDataStore(
              linkFeatureContext.getPlanitEntityClass(),
              createFullPathFromFileName(physicalNetworkLayer, getSettings().getLinksFileName()));
    }

    /* the feature writer through which to provide each result row */
    final var linksSchemaName = GeoIoFeatureTypeBuilder.createFeatureTypeSchemaName(
            physicalNetworkLayer, layerPrefixProducer, getSettings().getLinksFileName());

    /* perform persistence */
    writeGeometryLayerForEntity(
            featureType, linkFeatureContext, layerLogPrefix, linksDataStore, linksSchemaName, physicalNetworkLayer.getLinks(), MacroscopicLink::getGeometry);

  }

  /**
   * Writer the network layer's link segments
   *
   * @param physicalNetworkLayer          to persist link segments for
   * @param featureType to use
   * @param linkSegmentFeatureContext the context to convert instances to features
   * @param layerLogPrefix to use
   */
  private void writeNetworkLayerLinkSegments(MacroscopicNetworkLayer physicalNetworkLayer,
                                      SimpleFeatureType featureType,
                                      PlanitLinkSegmentFeatureTypeContext linkSegmentFeatureContext,
                                      String layerLogPrefix) {
    if(featureType==null || linkSegmentFeatureContext == null){
      throw new PlanItRunTimeException("No Feature type description available for PLANit link segments, this shouldn't happen");
    }
    LOGGER.info(String.format("%s Link segments: %d", layerLogPrefix, physicalNetworkLayer.getLinkSegments().size()));

    /* data store, e.g., underlying shape file(s) */
    DataStore linkSegmentsDataStore = GeoIODataStoreManager.getDataStore(linkSegmentFeatureContext.getPlanitEntityClass());
    if(linkSegmentsDataStore == null) {
      linkSegmentsDataStore = GeoIODataStoreManager.createDataStore(
              linkSegmentFeatureContext.getPlanitEntityClass(),
              createFullPathFromFileName(physicalNetworkLayer, getSettings().getLinkSegmentsFileName()));
    }

    /* the feature writer through which to provide each result row */
    final var linkSegmentsSchemaName = GeoIoFeatureTypeBuilder.createFeatureTypeSchemaName(
            physicalNetworkLayer, layerPrefixProducer, getSettings().getLinkSegmentsFileName());

    /* perform persistence */
    writeGeometryLayerForEntity(
            featureType, linkSegmentFeatureContext, layerLogPrefix, linkSegmentsDataStore, linkSegmentsSchemaName, physicalNetworkLayer.getLinkSegments(), ls -> ls.getParentLink().getGeometry());

  }

  /**
   * Write layers of the network
   *
   * @param macroscopicNetwork to write layers for
   */
  protected void writeLayers(MacroscopicNetwork macroscopicNetwork) {

    /* Ensure all geo features are available and configured for the correct CRS once we start using them */
    for( var layer : macroscopicNetwork.getTransportLayers()) {

      var supportedFeatures =
          GeoIoFeatureTypeBuilder.createSupportedNetworkLayerFeatures(getPrimaryIdMapper(), layer);

      /* feature types per layer */
      var geoFeatureTypesByPlanitEntity =
              GeoIoFeatureTypeBuilder.createSimpleFeatureTypesByLayer(
                  supportedFeatures,
                  layer,
                  getDestinationCoordinateReferenceSystem(),
                  extractPhysicalNetworkPlanitEntityBaseFileNames(getSettings()),
                  layerPrefixProducer);

      String layerLogPrefix = LoggingUtils.surroundwithBrackets(String.join(" ",
              "layer:",getPrimaryIdMapper().getNetworkLayerIdMapper().apply(layer)));

      /* nodes */
      if(getSettings().isPersistNodes()) {
        LOGGER.info(String.format("%sPersisting nodes to: %s",
                layerLogPrefix, createFullPathFromFileName(layer, getSettings().getNodesFileName()).toAbsolutePath()));
        var featureInfo = findFeaturePairForPlanitEntity(Node.class, geoFeatureTypesByPlanitEntity);
        writeNetworkLayerNodes(layer, featureInfo.first(), (PlanitNodeFeatureTypeContext) featureInfo.second(), layerLogPrefix);
      }

      /* links */
      if(getSettings().isPersistLinks()){
        LOGGER.info(String.format("%sPersisting links to: %s",
                layerLogPrefix, createFullPathFromFileName(layer, getSettings().getLinksFileName()).toAbsolutePath()));
        var featureInfo = findFeaturePairForPlanitEntity(MacroscopicLink.class, geoFeatureTypesByPlanitEntity);
        writeNetworkLayerLinks(layer, featureInfo.first(), (PlanitLinkFeatureTypeContext) featureInfo.second(), layerLogPrefix);
      }

      /* link segments */
      if(getSettings().isPersistLinkSegments()){
        LOGGER.info(String.format("%sPersisting link segments to: %s",
                layerLogPrefix, createFullPathFromFileName(layer, getSettings().getLinkSegmentsFileName()).toAbsolutePath()));
        var featureInfo = findFeaturePairForPlanitEntity(MacroscopicLinkSegment.class, geoFeatureTypesByPlanitEntity);
        writeNetworkLayerLinkSegments(layer, featureInfo.first(), (PlanitLinkSegmentFeatureTypeContext) featureInfo.second(), layerLogPrefix);
      }

    }

  }

  /** Constructor
   *
   */
  protected GeometryNetworkWriter() {
    this(".", CountryNames.GLOBAL);
  }

  /** Constructor
   *
   * @param networkPath to persist network on
   */
  protected GeometryNetworkWriter(String networkPath) {
    this(networkPath, CountryNames.GLOBAL);
  }

  /** Constructor
   *
   * @param networkPath to persist network on
   * @param countryName to optimise projection for (if available, otherwise ignore)
   */
  protected GeometryNetworkWriter(String networkPath, String countryName) {
    this(new GeometryNetworkWriterSettings(networkPath, countryName));
  }

  /** Constructor
   *
   * @param networkSettings to use
   */
  protected GeometryNetworkWriter(GeometryNetworkWriterSettings networkSettings){
    super(networkSettings);
  }

  /**
   * {@inheritDoc}
   */
//  @Override
  public void write(LayeredNetwork<?,?> network) {

    validate(network);
    MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork)network;

    /* initialise */
    initialiseWrite(macroscopicNetwork);

    /* logging */
    getSettings().logSettings();

    /* perform actual persistence */
    writeLayers(macroscopicNetwork);

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
  public GeometryNetworkWriterSettings getSettings() {
    return (GeometryNetworkWriterSettings) super.getSettings();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NetworkIdMapper getPrimaryIdMapper() {
    return getComponentIdMappers().getNetworkIdMappers();
  }
}
