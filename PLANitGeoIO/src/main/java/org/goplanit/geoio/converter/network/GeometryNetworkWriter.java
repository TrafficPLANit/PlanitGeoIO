package org.goplanit.geoio.converter.network;

import org.apache.commons.io.FilenameUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FileDataStoreFinder;
import org.goplanit.converter.CrsWriterImpl;
import org.goplanit.converter.idmapping.IdMapperType;
import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.converter.network.NetworkWriter;
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

    /* factory based on extension, implicit file type choice */
    var factory = FileDataStoreFinder.getDataStoreFactory(FilenameUtils.getExtension(getSettings().getNodesFileName()));

    // see https://docs.geotools.org/latest/userguide/library/data/datastore.html to proceed
    Path nodesFilePath = Path.of(getSettings().getOutputDirectory(),
            getSettings().getLayerPrefix()+"_"+
                    getPrimaryIdMapper().getNetworkLayerIdMapper().apply(physicalNetworkLayer)+
                    "_"+
                    getSettings().getNodesFileName());
    Map map = Collections.singletonMap( "url", UrlUtils.createFromPath(nodesFilePath.toAbsolutePath()));

    try {
      DataStore myData = factory.createNewDataStore(map);
      // todo: all geometry supporting PLANit entities should implement an interface (perhaps as part of default interface implementation)
      //  allows for:
      // 1: construct the instance as a simple feature, 2: provides the feature type, constructs the PLANit instance from a feature
      FeatureType featureType = DataUtilities.createType("my", "geom:Point,name:String,age:Integer,description:String");

      myData.createSchema(DataUtilities.simple(featureType));
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
    super(IdMapperType.ID);
    this.settings = new GeometryNetworkWriterSettings(networkPath, countryName);
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

  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    currLayerLogPrefix = null;
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
