package org.goplanit.geoio.converter.network;

import org.apache.commons.io.FilenameUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Transaction;
import org.goplanit.converter.CrsWriterImpl;
import org.goplanit.converter.idmapping.IdMapperType;
import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.converter.network.NetworkWriter;
import org.goplanit.geoio.util.GeoIODataStoreManager;
import org.goplanit.geoio.util.GeoIoFeatureTypeManager;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerImpl;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitEntityGeoUtils;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.misc.UrlUtils;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.NetworkLayer;
import org.goplanit.utils.network.layer.physical.Node;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

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

  /* track logging prefix for current layer */
  private String currLayerLogPrefix;

  /**
   * Construct consistent file path (with file name) based on desired output file name and settings configuration, taking the
   * current layer into account
   *
   * @param outputfileName to use
   * @param physicalNetworkLayer this applies to
   * @return created path
   */
  private Path createFullPathFromFileName(String outputfileName, MacroscopicNetworkLayer physicalNetworkLayer){
    return Path.of(getSettings().getOutputDirectory(),
            getSettings().getLayerPrefix()+"_"+
                    getPrimaryIdMapper().getNetworkLayerIdMapper().apply(physicalNetworkLayer)+
                    "_"+
                    getSettings().getNodesFileName());
  }

  /**
   * Write the nodes
   *
   * @param network to persist nodes for
   */
  private void writeNodes(MacroscopicNetwork network) {
    network.getTransportLayers().streamSorted(getPrimaryIdMapper().getNetworkLayerIdMapper()).forEach( l -> {
      this.currLayerLogPrefix = LoggingUtils.surroundwithBrackets("layer: "+getPrimaryIdMapper().getNetworkLayerIdMapper().apply(l));
      writeNetworkLayerNodes(l, currLayerLogPrefix);
    });
  }

  /**
   * Writer the network layer's nodes
   *
   * @param physicalNetworkLayer to persist nodes for
   * @param layerLoggingPrefix prefix to use when logging to user
   */
  private void writeNetworkLayerNodes(MacroscopicNetworkLayer physicalNetworkLayer, final String layerLoggingPrefix) {
    /* nodes */
    LOGGER.info(String.format("%s Nodes: %d", currLayerLogPrefix, physicalNetworkLayer.getNodes().size()));

    DataStore nodeDataStore =
            GeoIODataStoreManager.createDataStore(Node.class, createFullPathFromFileName(getSettings().getNodesFileName(), physicalNetworkLayer));

    try{
      SimpleFeatureType nodeFeatureType = GeoIoFeatureTypeManager.getSimpleFeatureType(Node.class);
      if(nodeDataStore.getSchema(nodeFeatureType.getTypeName()) != null){
        throw new PlanItRunTimeException("Feature type for nodes already registered on datastore, this shouldn't happen");
      }
      /* configure the datastore for the chosen feature type schema, so it can be populated */
      nodeDataStore.createSchema(nodeFeatureType);
    }catch (Exception e){
      LOGGER.severe((e.getMessage()));
      throw new PlanItRunTimeException("Unable to create node feature type schema for data store", e.getCause());
    }

    /* the feature writer through which to provide each result row */
    try ( var nodesFeatureWriter =
                  nodeDataStore.getFeatureWriter(FilenameUtils.getBaseName(getSettings().getNodesFileName()), Transaction.AUTO_COMMIT)) {
      for(var node : physicalNetworkLayer.getNodes()){
        var nodeFeature = nodesFeatureWriter.next();
        //todo: change the below in something that ties to the feature type in GeoIoFeatureTypeManager
        nodeFeature.setAttribute("node_id", getPrimaryIdMapper().getVertexIdMapper().apply(node));
        nodeFeature.setAttribute("name", node.getName());
        nodeFeature.setAttribute("geom", node.getPosition());
      }
    }catch (Exception e){
      LOGGER.severe((e.getMessage()));
      throw new PlanItRunTimeException("Unable to persist nodes", e.getCause());
    }
  }

  /**
   * Write the links
   *
   * @param network to persist links for
   */
  private void writeLinks(MacroscopicNetwork network) {
    network.getTransportLayers().streamSorted(getPrimaryIdMapper().getNetworkLayerIdMapper()).forEach( l -> {
      this.currLayerLogPrefix = LoggingUtils.surroundwithBrackets("layer: "+getPrimaryIdMapper().getNetworkLayerIdMapper().apply(l));
      writeNetworkLayerLinks(l, currLayerLogPrefix);
    });
  }

  /**
   * Writer the network layer's links
   *
   * @param physicalNetworkLayer to persist links for
   * @param layerLoggingPrefix prefix to use when logging to user
   */
  protected void writeNetworkLayerLinks(MacroscopicNetworkLayer physicalNetworkLayer, final String layerLoggingPrefix) {

    /* links */
    LOGGER.info(String.format("%s Links: %d", currLayerLogPrefix, physicalNetworkLayer.getLinks().size()));
    LOGGER.info(String.format("%s Link segments: %d", currLayerLogPrefix, physicalNetworkLayer.getLinkSegments().size()));


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
    getComponentIdMappers().populateMissingIdMappers(getIdMapperType());
    prepareCoordinateReferenceSystem(macroscopicNetwork.getCoordinateReferenceSystem(), getSettings().getDestinationCoordinateReferenceSystem(), getSettings().getCountry());

    getSettings().logSettings();

    if(getSettings().isPersistNodes()) {
      LOGGER.info(String.format("Persisting nodes geometry to: %s", Paths.get(getSettings().getOutputDirectory(), getSettings().getNodesFileName()).toString()));
      writeNodes(macroscopicNetwork);
    }
    if(getSettings().isPersistLinks()){
      LOGGER.info(String.format("Persisting links geometry to: %s",Paths.get(getSettings().getOutputDirectory(), getSettings().getLinksFileName()).toString()));
      writeLinks(macroscopicNetwork);
    }

    /* disposes of any registered data stores */
    GeoIODataStoreManager.reset();
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    currLayerLogPrefix = null;
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
