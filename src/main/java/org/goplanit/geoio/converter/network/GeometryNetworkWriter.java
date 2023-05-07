package org.goplanit.geoio.converter.network;

import org.apache.commons.io.FilenameUtils;
import org.geotools.data.DataStore;
import org.geotools.data.Transaction;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.goplanit.converter.CrsWriterImpl;
import org.goplanit.converter.idmapping.IdMapperType;
import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.converter.network.NetworkWriter;
import org.goplanit.geoio.util.GeoIODataStoreManager;
import org.goplanit.geoio.util.GeoIoFeatureTypeBuilder;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.misc.LoggingUtils;
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
   * @param nodeFeatureType the feature type describing what can be persisted in geo format
   * @param layerLogPrefix to use
   */
  private void writeNetworkLayerNodes(MacroscopicNetworkLayer physicalNetworkLayer,
                                      SimpleFeatureType nodeFeatureType,
                                      String layerLogPrefix) {
    if(nodeFeatureType==null){
      throw new PlanItRunTimeException("No Feature type description available for PLANit nodes, this shouldn't happen");
    }

    /* nodes */
    LOGGER.info(String.format("%s Nodes: %d", layerLogPrefix, physicalNetworkLayer.getNodes().size()));

    DataStore nodeDataStore = GeoIODataStoreManager.getDataStore(Node.class);
    if(nodeDataStore == null) {
      nodeDataStore = GeoIODataStoreManager.createDataStore(
              Node.class, createFullPathFromFileName(physicalNetworkLayer, getSettings().getNodesFileName()));
    }

    // todo make sure datastore AND feature type names for each PLANit entity are synced to see if this avoid the issue
    // of the error SEVERE ] Schema 'planit_nodes' does not exist thrown by the creation of the feature writer
    // perhaps the issue is in the datastore not finding the feature since the provided name does not match the shape files name of "layer_0_planit_nodes.shp"
    // the latter I think is in fact what we provide when creating the datastore and NOT the feature type name...
    GeoIODataStoreManager.registerFeatureOnDataStore(nodeDataStore, nodeFeatureType);

    /* the feature writer through which to provide each result row */
    final var nodesSchemaName = GeoIoFeatureTypeBuilder.createFeatureTypeSchemaName(
            physicalNetworkLayer, layerPrefixProducer, getSettings().getNodesFileName());
    var nodeFeatureBuilder = new SimpleFeatureBuilder(nodeFeatureType);

    try ( var nodesFeatureWriter =
                  nodeDataStore.getFeatureWriter(nodesSchemaName, Transaction.AUTO_COMMIT)) {
      for(var node : physicalNetworkLayer.getNodes()){
        var nodeFeature = nodesFeatureWriter.next();
        //todo: change the below in something that ties to the feature type in GeoIoFeatureTypeManager
        // todo : do so by somehow adding the method that produces the desired result given a node for that attribtue in the description
        nodeFeature.setAttribute("node_id", getPrimaryIdMapper().getVertexIdMapper().apply(node));
        nodeFeature.setAttribute("name", node.getName());
        nodeFeature.setAttribute(GeoIoFeatureTypeBuilder.GEOTOOLS_GEOMETRY_ATTRIBUTE, node.getPosition());
        nodesFeatureWriter.write();
      }
    }catch (Exception e){
      LOGGER.severe((e.getMessage()));
      throw new PlanItRunTimeException("%s Unable to persist nodes", layerLogPrefix, e.getCause());
    }
  }

  /**
   * Writer the network layer's links
   *
   * @param physicalNetworkLayer          to persist links for
   * @param linkFeatureType the feature type describing what can be persisted in geo format
   * @param layerLogPrefix to use
   */
  protected void writeNetworkLayerLinks(
          MacroscopicNetworkLayer physicalNetworkLayer, SimpleFeatureType linkFeatureType, String layerLogPrefix) {

    /* links */
    LOGGER.info(String.format("%s Links: %d", layerLogPrefix, physicalNetworkLayer.getLinks().size()));
    LOGGER.info(String.format("%s Link segments: %d", layerLogPrefix, physicalNetworkLayer.getLinkSegments().size()));


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
    
    /* currently we only support macroscopic infrastructure networks */
    if(!(network instanceof MacroscopicNetwork)) {
      throw new PlanItRunTimeException("Currently the GeometryNetworkWriter only supports macroscopic infrastructure networks, the provided network is not of this type");
    }
    MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork)network;

    /* initialise */
    initialiseWrite(macroscopicNetwork);

    getSettings().logSettings();

    /* Ensure all geo features are available and configured for the correct CRS once we start using them */
    for( var layer : macroscopicNetwork.getTransportLayers()) {

      var planitEntityBaseFileNames = extractPhysicalNetworkPlanitEntityBaseFileNames(getSettings());

      /* feature types for all supported PLANit entities on physical network layer */
      var geoFeatureTypesByPlanitEntity =
              GeoIoFeatureTypeBuilder.createPhysicalNetworkSimpleFeatureTypesByLayer(
                      layer,
                      getDestinationCoordinateReferenceSystem(),
                      planitEntityBaseFileNames,
                      layerPrefixProducer);

      String layerLogPrefix = LoggingUtils.surroundwithBrackets(String.join(" ",
              "layer:",getPrimaryIdMapper().getNetworkLayerIdMapper().apply(layer)));

      if(getSettings().isPersistNodes()) {
        LOGGER.info(String.format("%sPersisting nodes geometry to: %s",
                layerLogPrefix, createFullPathFromFileName(layer, getSettings().getNodesFileName()).toAbsolutePath()));
        writeNetworkLayerNodes(layer, geoFeatureTypesByPlanitEntity.get(Node.class), layerLogPrefix);
      }
      if(getSettings().isPersistLinks()){
        LOGGER.info(String.format("%s Persisting links geometry to: %s",
                layerLogPrefix, Paths.get(getSettings().getOutputDirectory(), getSettings().getLinksFileName())));
        writeNetworkLayerLinks(layer, geoFeatureTypesByPlanitEntity.get(Link.class), layerLogPrefix);
      }

    }

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
