package org.goplanit.geoio.converter.network;

import org.geotools.api.data.DataStore;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.converter.network.NetworkWriter;
import org.goplanit.geoio.converter.GeometryIoWriter;
import org.goplanit.geoio.converter.network.featurecontext.PlanitLinkFeatureTypeContext;
import org.goplanit.geoio.converter.network.featurecontext.PlanitNodeFeatureTypeContext;
import org.goplanit.geoio.util.GeoIODataStoreManager;
import org.goplanit.geoio.util.GeoIoFeatureTypeBuilder;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.network.*;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.misc.FileUtils;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.network.layer.UntypedDirectedGraphLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.physical.*;

import java.nio.file.Path;
import java.util.List;
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
   * Currently supports:
   * <ul>
   *   <li>Node.class for (conjugate) node entities</li>
   *   <li>Link.class for (conjugate) link entities</li>
   *   <li>MacroscopicLinkSegment.class for macroscopic link segment entities</li>
   *   <li>ConjugateLinkSegment.class for conjugate link segment entities</li>
   * </ul>
   *
   * @param settings to use
   * @return mapping between PLANit entity class and the chosen file name
   */
  private static Map<Class<?>, String> extractPhysicalNetworkPlanitEntityBaseFileNames(
          GeometryNetworkWriterSettings settings) {
    return Map.ofEntries(
            entry(Node.class, settings.getNodesFileName()),
            entry(Link.class, settings.getLinksFileName()),
            entry(MacroscopicLinkSegment.class, settings.getLinkSegmentsFileName()),
            entry(ConjugateLinkSegment.class, settings.getLinkSegmentsFileName())
    );
  }


  /** validate before commencing actual write
   *
   * @param network to validate
   */
  private void validate(LayeredNetwork<?,?> network) {
    /* currently we only support macroscopic infrastructure networks */
    if(!(network instanceof MacroscopicNetwork)) {
      throw new PlanItRunTimeException("Currently the GeometryNetworkWriter only supports" +
              " macroscopic infrastructure networks, the provided network is not of this type");
    }
  }

  /**
   * Construct consistent file path (with file name) based on desired output file name and settings configuration,
   * taking the current layer into account.
   *
   * @param physicalNetworkLayer this applies to
   * @param outputFileName to use
   * @return created path
   */
  private Path createFullPathFromFileName(UntypedPhysicalLayer<?,?,?> physicalNetworkLayer, String outputFileName){
    PlanItRunTimeException.throwIfNull(getSettings().getOutputDirectory(), "Output directory not set");
    PlanItRunTimeException.throwIfNull(outputFileName, "Output file name not set");
    PlanItRunTimeException.throwIfNull(getSettings().getFormat(), "file name extension not set");

    var featureTypeSchemaName = GeoIoFeatureTypeBuilder.createFeatureTypeSchemaName(
            physicalNetworkLayer, layerPrefixProducer, outputFileName);
    PlanItRunTimeException.throwIf(
            StringUtils.isNullOrBlank(featureTypeSchemaName), "Feature type schema name null or empty");

    var format = getSettings().getFormat();
    assert (format!=null) : "Format for GeoIO not allowed to be null";
    return Path.of(getSettings().getOutputDirectory(),featureTypeSchemaName + format.extension());
  }

  /**
   * Initialise before actual writing starts. Called from {@link #write(LayeredNetwork)}
   *
   * @param network to writer
   * @param filePrefix prefix to apply to each file produced, may be null
   */
  private void initialiseWrite(UntypedPhysicalNetwork<?,?> network, String filePrefix) {
    getComponentIdMappers().populateMissingIdMappers(getIdMapperType());

    String layerString = (filePrefix!=null ? filePrefix : "") + "layer";
    layerPrefixProducer =
            (UntypedDirectedGraphLayer<?,?,?> l) ->
                    String.join("_", layerString,
                            getPrimaryIdMapper().getNetworkLayerIdMapper().apply((UntypedPhysicalLayer<?,?,?>)l));

    prepareCoordinateReferenceSystem(
            network.getCoordinateReferenceSystem(),
            getSettings().getDestinationCoordinateReferenceSystem(),
            getSettings().getCountry(),
            true);

    // make sure directory exists before starting to write to it
    boolean directoryAvailable = FileUtils.createDirectoryFrom(getSettings().getOutputDirectory());
    if(!directoryAvailable){
      throw new PlanItRunTimeException("Unable to persist in output location, %s not available",
              getSettings().getOutputDirectory());
    }
  }

  /**
   * Writer the network layer's nodes
   *
   * @param physicalNetworkLayer          to persist nodes for
   * @param featureType to use
   * @param nodeFeatureContext the context to convert instances to features
   * @param layerLogPrefix to use
   */
  private void writeNetworkLayerNodes(UntypedPhysicalLayer<?,?,?> physicalNetworkLayer,
                                      SimpleFeatureType featureType,
                                      PlanitNodeFeatureTypeContext nodeFeatureContext,
                                      String layerLogPrefix) {
    if(featureType==null || nodeFeatureContext == null){
      throw new PlanItRunTimeException("No Feature type description available for PLANit nodes, this shouldn't happen");
    }
    LOGGER.info(String.format("%s Nodes: %d", layerLogPrefix, physicalNetworkLayer.getNodes().size()));

    /* data store, e.g., underlying shape file(s) */
    DataStore nodeDataStore =
        findDataStore(
                nodeFeatureContext, createFullPathFromFileName(physicalNetworkLayer, getSettings().getNodesFileName()));

    /* the feature writer through which to provide each result row */
    final var nodesSchemaName = GeoIoFeatureTypeBuilder.createFeatureTypeSchemaName(
            physicalNetworkLayer, layerPrefixProducer, getSettings().getNodesFileName());

    /* perform persistence */
    writeGeometryLayerForEntity(
            featureType,
            nodeFeatureContext,
            layerLogPrefix,
            nodeDataStore,
            nodesSchemaName,
            physicalNetworkLayer.getNodes());

  }

  /**
   * Writer the network layer's links
   *
   * @param physicalNetworkLayer          to persist links for
   * @param featureType to use
   * @param linkFeatureContext the context to convert instances to features
   * @param layerLogPrefix to use
   */
  private void writeNetworkLayerLinks(UntypedPhysicalLayer<?,?,?> physicalNetworkLayer,
                                      SimpleFeatureType featureType,
                                      PlanitLinkFeatureTypeContext linkFeatureContext,
                                      String layerLogPrefix) {
    if(featureType==null || linkFeatureContext == null){
      throw new PlanItRunTimeException("No Feature type description available for PLANit links, this shouldn't happen");
    }
    LOGGER.info(String.format("%s Links: %d", layerLogPrefix, physicalNetworkLayer.getLinks().size()));

    /* data store, e.g., underlying shape file(s) */
    DataStore linksDataStore =
        findDataStore(linkFeatureContext,   createFullPathFromFileName(
                physicalNetworkLayer, getSettings().getLinksFileName()));

    /* the feature writer through which to provide each result row */
    final var linksSchemaName = GeoIoFeatureTypeBuilder.createFeatureTypeSchemaName(
            physicalNetworkLayer, layerPrefixProducer, getSettings().getLinksFileName());

    /* perform persistence */
    writeGeometryLayerForEntity(
            featureType,
            linkFeatureContext,
            layerLogPrefix,
            linksDataStore,
            linksSchemaName,
            physicalNetworkLayer.getLinks());

  }


  /**
   * Writer the network layer's link segments
   *
   * @param <LS> type of link segment
   * @param physicalNetworkLayer          to persist link segments for
   * @param featureType to use
   * @param linkSegmentFeatureContext the context to convert instances to features
   * @param layerLogPrefix to use
   */
  private <LS extends LinkSegment> void writeNetworkLayerLinkSegments(
          UntypedPhysicalLayer<?,?, LS> physicalNetworkLayer,
          SimpleFeatureType featureType,
          PlanitEntityFeatureTypeContext<LS> linkSegmentFeatureContext,
          String layerLogPrefix) {

    if(featureType==null || linkSegmentFeatureContext == null){
      throw new PlanItRunTimeException(
              "No Feature type description available for PLANit link segments, this shouldn't happen");
    }
    LOGGER.info(String.format("%s Link segments: %d", layerLogPrefix, physicalNetworkLayer.getLinkSegments().size()));

    /* data store, e.g., underlying shape file(s) */
    DataStore linkSegmentsDataStore =
        findDataStore(
                linkSegmentFeatureContext,
                createFullPathFromFileName(physicalNetworkLayer, getSettings().getLinkSegmentsFileName()));

    /* the feature writer through which to provide each result row */
    final var linkSegmentsSchemaName = GeoIoFeatureTypeBuilder.createFeatureTypeSchemaName(
            physicalNetworkLayer, layerPrefixProducer, getSettings().getLinkSegmentsFileName());

    /* perform persistence */
    writeGeometryLayerForEntity(
            featureType,
            linkSegmentFeatureContext,
            layerLogPrefix,
            linkSegmentsDataStore,
            linkSegmentsSchemaName,
            physicalNetworkLayer.getLinkSegments());

  }

  /**
   * Write layers of the network
   *
   * @param <LS> link segment type to persist
   * @param physicalNetwork to write layers for
   * @param linkSegmentClazz indicate which link segment class is to be persisted asthere are multiple option
   */
  protected <LS extends LinkSegment> void writeLayers(
          UntypedPhysicalNetwork<? extends UntypedPhysicalLayer<?,?,LS>,?> physicalNetwork,
          Class<LS> linkSegmentClazz) {

    /* Ensure all geo features are available and configured for the correct CRS once we start using them */
    for(var layer : physicalNetwork.getTransportLayers()) {

      var supportedFeatures =
          GeoIoFeatureTypeBuilder.createNetworkLayerFeatureContexts(
              getPrimaryIdMapper(), layer, getDestinationCrsTransformer());

      /* feature types per layer */
      var geoFeatureTypesByPlanitEntity =
              GeoIoFeatureTypeBuilder.createSimpleFeatureTypesByLayer(
                  supportedFeatures,
                  layer,
                  getDestinationCoordinateReferenceSystem(),
                  extractPhysicalNetworkPlanitEntityBaseFileNames(getSettings()),
                  layerPrefixProducer);

      String layerLogPrefix = LoggingUtils.surroundWithBrackets(String.join(" ",
              "layer:",getPrimaryIdMapper().getNetworkLayerIdMapper().apply(layer)));

      /* nodes */
      if(getSettings().isPersistNodes()) {
        LOGGER.info(String.format("%sPersisting nodes to: %s",
                layerLogPrefix, createFullPathFromFileName(layer, getSettings().getNodesFileName()).toAbsolutePath()));
        var featureInfo = findFeaturePairForPlanitEntity(Node.class, geoFeatureTypesByPlanitEntity);
        writeNetworkLayerNodes(
                layer, featureInfo.first(), (PlanitNodeFeatureTypeContext) featureInfo.second(), layerLogPrefix);
      }

      /* links */
      if(getSettings().isPersistLinks()){
        LOGGER.info(String.format("%sPersisting links to: %s",
                layerLogPrefix, createFullPathFromFileName(layer, getSettings().getLinksFileName()).toAbsolutePath()));
        var featureInfo = findFeaturePairForPlanitEntity(Link.class, geoFeatureTypesByPlanitEntity);
        writeNetworkLayerLinks(
                layer, featureInfo.first(), (PlanitLinkFeatureTypeContext) featureInfo.second(), layerLogPrefix);
      }

      /* link segments */
      if(getSettings().isPersistLinkSegments()){
        LOGGER.info(String.format("%sPersisting link segments to: %s",
                layerLogPrefix,
                createFullPathFromFileName(layer, getSettings().getLinkSegmentsFileName()).toAbsolutePath()));

        var featureInfo = findFeaturePairForPlanitEntity(linkSegmentClazz, geoFeatureTypesByPlanitEntity);
        writeNetworkLayerLinkSegments(
                layer, featureInfo.first(), featureInfo.second(), layerLogPrefix);
      }

    }
  }

  /**
   * Perform steps required to write the given conjugate network to disk in desired format
   *
   * @param conjugateNetwork to write to disk
   */
  protected void writeConjugateMacroscopicNetwork(ConjugateMacroscopicNetwork conjugateNetwork) {
    writeNetwork(conjugateNetwork,"conjugate_", ConjugateLinkSegment.class);
  }

  /**
   * Perform steps required to write the given macroscopic network to disk in desired format
   *
   * @param macroscopicNetwork to write to disk
   */
  protected void writeMacroscopicNetwork(MacroscopicNetwork macroscopicNetwork) {
    writeNetwork(macroscopicNetwork,"", MacroscopicLinkSegment.class);
  }

  /**
   * Perform steps required to write the given network to disk in desired format
   *
   * @param <LS> link segment type to persist
   * @param network to write to disk
   * @param filePrefix to apply
   * @param linkSegmentClazz indicate which link segment class is to be persisted asthere are multiple option
   */
  protected <LS extends LinkSegment> void writeNetwork(
          UntypedPhysicalNetwork<? extends UntypedPhysicalLayer<?,?,LS>,?> network,
          String filePrefix,
          Class<LS> linkSegmentClazz) {

    /* initialise */
    initialiseWrite(network, filePrefix);

    /* perform actual persistence */
    writeLayers(network, linkSegmentClazz);

    /* disposes of any registered data stores */
    GeoIODataStoreManager.reset();
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

    /* validate */
    validate(network);

    /* logging */
    getSettings().logSettings();

    /* regular network */
    var macroscopicNetwork = (MacroscopicNetwork) network;
    writeMacroscopicNetwork(macroscopicNetwork);

    /* conjugate network if configured as such */
    if(getSettings().isPersistConjugateNetwork()){
      writeConjugateMacroscopicNetwork(
              macroscopicNetwork.createConjugate(
                      MacroscopicNetworkUtils.generateDerivedConjugateIdGroupingToken(macroscopicNetwork),
                      null));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    super.reset();
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
