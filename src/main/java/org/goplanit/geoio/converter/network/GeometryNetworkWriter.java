package org.goplanit.geoio.converter.network;

import org.geotools.data.DataStore;
import org.geotools.data.Transaction;
import org.goplanit.converter.CrsWriterImpl;
import org.goplanit.converter.idmapping.IdMapperType;
import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.converter.network.NetworkWriter;
import org.goplanit.geoio.converter.network.featurecontext.PlanitLinkFeatureTypeContext;
import org.goplanit.geoio.converter.network.featurecontext.PlanitLinkSegmentFeatureTypeContext;
import org.goplanit.geoio.converter.network.featurecontext.PlanitNodeFeatureTypeContext;
import org.goplanit.geoio.util.GeoIODataStoreManager;
import org.goplanit.geoio.util.GeoIoFeatureTypeBuilder;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.id.ManagedIdEntities;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.physical.Node;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeatureType;

import javax.crypto.Mac;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.Map.entry;

/**
 * Writer to persist a PLANit network to disk in a geometry centric format such as Shape files. Id mapping default is
 * set to internal ids (not XML ids) by default
 * 
 * @author markr
 *
 */
public class GeometryNetworkWriter extends CrsWriterImpl<LayeredNetwork<?,?>> implements NetworkWriter {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(GeometryNetworkWriter.class.getCanonicalName());

  /** network writer settings to use */
  private final GeometryNetworkWriterSettings settings;

  /** construct prefix for a given layer in String format */
  private Function<MacroscopicNetworkLayer, String> layerPrefixProducer = null;

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

  /**
   * Given the feature contexts for the available GIS features, find the one where the context matches a given PLANit entity class
   *
   * @param planitEntityClass to find entry for
   * @param geoFeatureTypesByPlanitEntity available entries to search in
   * @return found entry or throw run time exception
   */
  private Pair<SimpleFeatureType, PlanitEntityFeatureTypeContext<? extends ManagedId>> findFeaturePairForPlanitEntity(
          Class<? extends ManagedId> planitEntityClass, List<Pair<SimpleFeatureType, PlanitEntityFeatureTypeContext<? extends ManagedId>>> geoFeatureTypesByPlanitEntity) {
    return geoFeatureTypesByPlanitEntity.stream().filter(
            p -> p.second().getPlanitEntityClass().equals(planitEntityClass)).findFirst().orElseThrow(() ->
            new PlanItRunTimeException("No feature information found for %s, available: [%s]", planitEntityClass.getName(),
                    geoFeatureTypesByPlanitEntity.stream().map(p -> p.second().getPlanitEntityClass().getName()).collect(Collectors.joining(","))));
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
            (MacroscopicNetworkLayer l) ->
                    String.join("_", "layer", getPrimaryIdMapper().getNetworkLayerIdMapper().apply(l));

    prepareCoordinateReferenceSystem(
            macroscopicNetwork.getCoordinateReferenceSystem(), getSettings().getDestinationCoordinateReferenceSystem(), getSettings().getCountry());
  }

  /**
   * Writer the network layer's nodes
   *
   * @param <T> type of PLANit entity to write
   * @param featureType to use
   * @param planitEntityFeatureContext the context to convert instances to features
   * @param layerLogPrefix to use
   * @param entityDataStore to use for persistence
   * @param featureSchemaName the feature lives under on the datastore
   * @param planitEntities container to persist
   * @param entityToGeometry conversion from entity to geometry it contains
   */
  private <T extends ManagedId> void writeNetworkLayerForEntity(SimpleFeatureType featureType,
                                                                PlanitEntityFeatureTypeContext<T> planitEntityFeatureContext,
                                                                String layerLogPrefix,
                                                                DataStore entityDataStore,
                                                                String featureSchemaName,
                                                                ManagedIdEntities<T> planitEntities,
                                                                Function<T, Geometry> entityToGeometry) {

    /* place feature on data store */
    GeoIODataStoreManager.registerFeatureOnDataStore(entityDataStore, featureType);

    try ( var featureWriter =
                  entityDataStore.getFeatureWriter(featureSchemaName, Transaction.AUTO_COMMIT)) {
      for(var planitEntity : planitEntities){
        var entityFeature = featureWriter.next();
        var attributeConversions = planitEntityFeatureContext.getAttributeDescription();
        for(var attributeConversion : attributeConversions) {

          if(attributeConversion.first().equals(planitEntityFeatureContext.getDefaultGeometryAttributeKey())) {
            /* geometry attribute */
            entityFeature.setAttribute(GeoIoFeatureTypeBuilder.GEOTOOLS_GEOMETRY_ATTRIBUTE, entityToGeometry.apply(planitEntity));
          }else{
            /* regular attribute */
            entityFeature.setAttribute(attributeConversion.first(), attributeConversion.third().apply(planitEntity));
          }
        }
        featureWriter.write();
      }
    }catch (Exception e){
      LOGGER.severe((e.getMessage()));
      throw new PlanItRunTimeException("%s Unable to persist PLANit entities for %s",
              layerLogPrefix, planitEntities.getManagedIdClass().getName(), e.getCause());
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
    writeNetworkLayerForEntity(
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
    writeNetworkLayerForEntity(
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
    writeNetworkLayerForEntity(
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

      /* feature types per layer */
      var geoFeatureTypesByPlanitEntity =
              GeoIoFeatureTypeBuilder.createPhysicalNetworkSimpleFeatureTypesByLayer(
                      getPrimaryIdMapper(),
                      layer,
                      getDestinationCoordinateReferenceSystem(),
                      extractPhysicalNetworkPlanitEntityBaseFileNames(getSettings()),
                      layerPrefixProducer);

      String layerLogPrefix = LoggingUtils.surroundwithBrackets(String.join(" ",
              "layer:",getPrimaryIdMapper().getNetworkLayerIdMapper().apply(layer)));

      //todo: writing layer should become trivial now, so we can consolidate and group repeating aspects

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

  /** find feature and context based on the class present in context
   *
   * @param clazz to find feature for
   * @return found entry, null if not present
   */
  private static <T extends ExternalIdAble> Pair<SimpleFeatureType, PlanitEntityFeatureTypeContext<T>> findFeature(
          Class<T> clazz, Map<SimpleFeatureType, PlanitEntityFeatureTypeContext<? extends ExternalIdAble>> geoFeatureTypes) {
    var result =
            geoFeatureTypes.entrySet().stream().filter(e -> e.getValue().getPlanitEntityClass().equals(clazz)).findFirst();
    return result.isPresent() ? Pair.of(result.get().getKey(), (PlanitEntityFeatureTypeContext<T>) result.get().getValue()) : Pair.empty();
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
    super(IdMapperType.ID);
    this.settings = networkSettings;
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
    return this.settings;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NetworkIdMapper getPrimaryIdMapper() {
    return getComponentIdMappers().getNetworkIdMappers();
  }
}
