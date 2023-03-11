package org.goplanit.geoio.converter.network;

import org.goplanit.converter.NetworkIdMapper;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.network.layer.NetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinks;
import org.goplanit.utils.network.layer.physical.Nodes;

import java.util.logging.Logger;

/**
 * Writer to persist a PLANit network to disk in a geometry centric format such as Shape files.
 * 
 * @author markr
 *
 */
public class GeometryNetworkWriter /*implements NetworkWriter*/ {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(GeometryNetworkWriter.class.getCanonicalName());

//  /** network writer settings to use */
//  private final GeometryNetworkWriterSettings settings;

  /* track logging prefix for current layer */
  private String currLayerLogPrefix;

  /** id mappers for network entities */
  private NetworkIdMapper networkIdMapper;

  /**
   * Write the nodes
   *
   * @param nodes to persist
   */
  private void writeNodes(Nodes nodes) {
  }

  /**
   * Write the links
   *
   * @param links to persist
   */
  private void writeLinks(MacroscopicLinks links) {
  }

  /**
   * Writer the network layer
   *
   * @param physicalNetworkLayer to persist
   * @param network to extract from
   */
  protected void writeNetworkLayer(MacroscopicNetworkLayerImpl physicalNetworkLayer, MacroscopicNetwork network) {

    /* links */
    LOGGER.info(String.format("%s Links: %d", currLayerLogPrefix, physicalNetworkLayer.getLinks().size()));
    LOGGER.info(String.format("%s Link segments: %d", currLayerLogPrefix, physicalNetworkLayer.getLinkSegments().size()));
    writeLinks(physicalNetworkLayer.getLinks());

    /* nodes */
    LOGGER.info(String.format("%s Nodes: %d", currLayerLogPrefix, physicalNetworkLayer.getNodes().size()));
    writeNodes(physicalNetworkLayer.getNodes());
  }

  /** Writer the available network layers
   *
   * @param network to extract layers from and populate xml
   */
  protected void writeNetworkLayers(MacroscopicNetwork network) {

    LOGGER.info("Network layers:" + network.getTransportLayers().size());
    for(NetworkLayer networkLayer : network.getTransportLayers()) {
      if(networkLayer instanceof MacroscopicNetworkLayerImpl) {
        MacroscopicNetworkLayerImpl physicalNetworkLayer = ((MacroscopicNetworkLayerImpl)networkLayer);

        /* XML id */
        //todo

        this.currLayerLogPrefix = LoggingUtils.surroundwithBrackets("layer: "+physicalNetworkLayer.getXmlId());

        writeNetworkLayer(physicalNetworkLayer, network);
      }else {
        LOGGER.severe(String.format("Unsupported macroscopic infrastructure layer %s encountered", networkLayer.getXmlId()));
      }
    }
  }




  /** Constructor
   *
   */
  protected GeometryNetworkWriter() {
    this(null, CountryNames.GLOBAL);
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
//    super(IdMapperType.XML);
//    this.settings = new GeometryNetworkWriterSettings(networkPath, DEFAULT_NETWORK_FILE_NAME, countryName);
  }

  /** default network file name to use */
  public static final String DEFAULT_NETWORK_FILE_NAME = "network.shp";

  /**
   * {@inheritDoc}
   */
//  @Override
  public void write(LayeredNetwork<?,?> network) throws PlanItException {
    
//    /* currently we only support macroscopic infrastructure networks */
//    if(!(network instanceof MacroscopicNetwork)) {
//      throw new PlanItRunTimeException("Currently the GeometryNetworkWriter only supports macroscopic infrastructure networks, the provided network is not of this type");
//    }
//    MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork)network;
//
//    /* initialise */
//    this.networkIdMapper = new NetworkIdMapper(getSettings().getIdMappingType());
//
//    super.prepareCoordinateReferenceSystem(macroscopicNetwork.getCoordinateReferenceSystem());
//    LOGGER.info(String.format("Persisting network geometry to: %s",Paths.get(getSettings().getOutputPathDirectory(), getSettings().getFileName()).toString()));
//    getSettings().logSettings();
//
//    /* network layers */
//    writeNetworkLayers(macroscopicNetwork);

  }
  
  /**
   * {@inheritDoc}
   */
//  @Override
//  public void reset() {
//    currLayerLogPrefix = null;
//  }
  
  // GETTERS/SETTERS
  
//  /**
//   * {@inheritDoc}
//   */
//  @Override
//  public GeometryNetworkWriterSettings getSettings() {
//    return this.settings;
//  }
//
//  /** the country name of the network to write (if any is set)
//   *
//   * @return country name, null if unknown
//   */
//  public String getCountryName() {
//    return getSettings().getCountry();
//  }

}