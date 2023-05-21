package org.goplanit.geoio.converter.network;

import org.apache.commons.io.FilenameUtils;
import org.geotools.data.DataStore;
import org.geotools.data.Transaction;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.util.MapEntry;
import org.goplanit.converter.CrsWriterImpl;
import org.goplanit.converter.idmapping.IdMapperType;
import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.converter.network.NetworkWriter;
import org.goplanit.geoio.converter.network.featurecontext.PlanitNodeFeatureTypeContext;
import org.goplanit.geoio.util.GeoIODataStoreManager;
import org.goplanit.geoio.util.GeoIoFeatureTypeBuilder;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.physical.Node;
import org.opengis.feature.simple.SimpleFeatureType;

import java.nio.file.Path;
import java.nio.file.Paths;
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
public class GeometryNetworkWriter extends CrsWriterImpl<LayeredNetwork<?,?>> implements NetworkWriter {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(GeometryNetworkWriter.class.getCanonicalName());

  /** network writer settings to use */
  private final GeometryNetworkWriterSettings settings;

  /** construct prefix for a given layer in String format */
  private Function<MacroscopicNetworkLayer, String> layerPrefixProducer = null;

  /**
   * BAsed on the settings construct the correct mapping between file names and the PLANit entities
   *
   * @param settings to use
   * @return mapping between PLANit entity class and the chosen file name
   */
  private static Map<Class<?>, String> extractPhysicalNetworkPlanitEntityBaseFileNames(GeometryNetworkWriterSettings settings) {
    return Map.ofEntries(
            entry(Node.class, settings.getNodesFileName()),
            entry(Link.class, settings.getLinksFileName())
            //todo add other types support
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
            (MacroscopicNetworkLayer l) ->
                    String.join("_", "layer", getPrimaryIdMapper().getNetworkLayerIdMapper().apply(l));

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
    DataStore nodeDataStore = GeoIODataStoreManager.getDataStore(Node.class);
    if(nodeDataStore == null) {
      nodeDataStore = GeoIODataStoreManager.createDataStore(
              Node.class, createFullPathFromFileName(physicalNetworkLayer, getSettings().getNodesFileName()));
    }

    /* place feature on data store */
    GeoIODataStoreManager.registerFeatureOnDataStore(nodeDataStore, featureType);

    /* the feature writer through which to provide each result row */
    final var nodesSchemaName = GeoIoFeatureTypeBuilder.createFeatureTypeSchemaName(
            physicalNetworkLayer, layerPrefixProducer, getSettings().getNodesFileName());

    try ( var nodesFeatureWriter =
                  nodeDataStore.getFeatureWriter(nodesSchemaName, Transaction.AUTO_COMMIT)) {
      for(var node : physicalNetworkLayer.getNodes()){
        var nodeFeature = nodesFeatureWriter.next();
        var attributeConversions = nodeFeatureContext.getAttributeDescription();
        for(var attributeConversion : attributeConversions) {

          //todo change the literal to some constant on the base context class so we don't have it in multiple places ->
          // then use on the derived classes
          if(attributeConversion.first().equals("*geom")) {
            /* geometry attribute */
            nodeFeature.setAttribute(GeoIoFeatureTypeBuilder.GEOTOOLS_GEOMETRY_ATTRIBUTE, node.getPosition());
          }else{
            /* regular attribute */
            nodeFeature.setAttribute(attributeConversion.first(), attributeConversion.third().apply(node));
          }
        }
        nodesFeatureWriter.write();
      }
    }catch (Exception e){
      LOGGER.severe((e.getMessage()));
      throw new PlanItRunTimeException("%s Unable to persist nodes", layerLogPrefix, e.getCause());
    }
  }

  /**
   * Write layers of the network
   *
   * @param macroscopicNetwork to write layers for
   */
  protected void writeLayers(MacroscopicNetwork macroscopicNetwork) {

    /* Ensure all geo features are available and configured for the correct CRS once we start using them */
    for( var layer : macroscopicNetwork.getTransportLayers()) {

      final var planitEntityBaseFileNames = extractPhysicalNetworkPlanitEntityBaseFileNames(getSettings());

      /* feature types per layer */
      var geoFeatureTypesByPlanitEntity =
              GeoIoFeatureTypeBuilder.createPhysicalNetworkSimpleFeatureTypesByLayer(
                      getPrimaryIdMapper(),
                      layer,
                      getDestinationCoordinateReferenceSystem(),
                      planitEntityBaseFileNames,
                      layerPrefixProducer);

      String layerLogPrefix = LoggingUtils.surroundwithBrackets(String.join(" ",
              "layer:",getPrimaryIdMapper().getNetworkLayerIdMapper().apply(layer)));

      //todo: writing layer should become trivial now, so we can consolidate and group repeating aspects

      /* nodes */
      if(getSettings().isPersistNodes()) {
        LOGGER.info(String.format("%sPersisting node geometries to: %s",
                layerLogPrefix, createFullPathFromFileName(layer, getSettings().getNodesFileName()).toAbsolutePath()));
        var featureInfo = geoFeatureTypesByPlanitEntity.stream().filter(
                p -> p.second().getPlanitEntityClass().equals(Node.class)).findFirst().orElseThrow(() -> new PlanItRunTimeException("No Node feature information found"));
        writeNetworkLayerNodes(layer, featureInfo.first(), (PlanitNodeFeatureTypeContext) featureInfo.second(), layerLogPrefix);
      }

      /* links */
      if(getSettings().isPersistLinks()){
        LOGGER.info(String.format("%sPersisting link geometries to: %s",
                layerLogPrefix, Paths.get(getSettings().getOutputDirectory(), getSettings().getLinksFileName())));
        var featureInfo = geoFeatureTypesByPlanitEntity.stream().filter(
                p -> p.second().getPlanitEntityClass().equals(Link.class)).findFirst().orElseThrow(() -> new PlanItRunTimeException("No Link feature information found"));

                //todo
                /* links */
                //LOGGER.info(String.format("%s Links: %d", layerLogPrefix, physicalNetworkLayer.getLinks().size()));
                //LOGGER.info(String.format("%s Link segments: %d", layerLogPrefix, physicalNetworkLayer.getLinkSegments().size()));
                //writeNetworkLayerLinks(layer, featureInfo.first(), featureInfo.second(), layerLogPrefix);
      }

    }

  }

  /** find feature and context absed on the class present in context
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
