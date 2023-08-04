package org.goplanit.geoio.converter.service;

import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.geoio.util.GeoIoWriterSettings;

import java.util.logging.Logger;

/**
 * Settings relevant for persisting the PLANit network in any Geo IO output format
 * 
 * @author markr
 *
 */
public class GeometryServiceNetworkWriterSettings extends GeoIoWriterSettings implements ConverterWriterSettings {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(GeometryServiceNetworkWriterSettings.class.getCanonicalName());

  /** service legs file name to use */
  private String serviceLegsFileName = DEFAULT_SERVICE_LEGS_FILE_NAME;

  /** service leg segments file name to use */
  private String serviceLegSegmentsFileName = DEFAULT_SERVICE_LEGSEGMENTS_FILE_NAME;

  /** service nodes file name to use */
  private String serviceNodesFileName = DEFAULT_SERVICE_NODES_FILE_NAME;

  /** flag indicating whether to persist legs or not */
  private boolean persistServiceLegs = DEFAULT_PERSIST_SERVICE_LEGS;

  /** flag indicating whether to persist service leg segments or not */
  private boolean persistServiceLegSegments = DEFAULT_PERSIST_SERVICE_LEGSEGMENTS;

  /** flag indicating whether to persist service nodes or not */
  private boolean persistServiceNodes = DEFAULT_PERSIST_SERVICE_NODES;

  /** each layer gets a prefix prepended to the file name,e.g., #layer_prefix_#id_#filename */
  private String layerPrefix = DEFAULT_LAYER_PREFIX;

  /** default links file name to use (without extension)*/
  public static final String DEFAULT_SERVICE_LEGS_FILE_NAME = "planit_service_legs";

  /** default link segments file name to use (without extension)*/
  public static final String DEFAULT_SERVICE_LEGSEGMENTS_FILE_NAME = "planit_service_legsegments";

  /** default nodes file name to use (without extension) */
  public static final String DEFAULT_SERVICE_NODES_FILE_NAME = "planit_service_nodes";

  public static final String DEFAULT_LAYER_PREFIX = "layer";

  /** default persist links flag value */
  public static boolean DEFAULT_PERSIST_SERVICE_LEGS = true;

  /** default persist link segments flag value */
  public static boolean DEFAULT_PERSIST_SERVICE_LEGSEGMENTS = true;


  /** default persist nodes flag value */
  public static boolean DEFAULT_PERSIST_SERVICE_NODES = true;

  /**
   * Default constructor
   */
  public GeometryServiceNetworkWriterSettings() {
    super();
  }

  /**
   * Constructor
   *
   *  @param outputPathDirectory to use
   */
  public GeometryServiceNetworkWriterSettings(final String outputPathDirectory) {
    super(outputPathDirectory);
  }

  /**
   * Constructor
   *
   * @param outputPathDirectory to use
   * @param countryName to use
   */
  public GeometryServiceNetworkWriterSettings(final String outputPathDirectory, final String countryName) {
    super(outputPathDirectory, countryName);
  }

  public String getServiceLegsFileName() {
    return serviceLegsFileName;
  }

  public void setServiceLegsFileName(String serviceLegsFileName) {
    this.serviceLegsFileName = serviceLegsFileName;
  }

  public String getServiceLegSegmentsFileName() {
    return serviceLegSegmentsFileName;
  }

  public void setServiceLegSegmentsFileName(String serviceLegSegmentsFileName) {
    this.serviceLegSegmentsFileName = serviceLegSegmentsFileName;
  }

  public String getServiceNodesFileName() {
    return serviceNodesFileName;
  }

  public void setServiceNodesFileName(String serviceNodesFileName) {
    this.serviceNodesFileName = serviceNodesFileName;
  }

  public void setPersistServiceLegs(boolean persistServiceLegs) {
    this.persistServiceLegs = persistServiceLegs;
  }

  public boolean isPersistServiceLegs() {
    return persistServiceLegs;
  }

  public void setPersistServiceLegSegments(boolean persistServiceLegSegments) {
    this.persistServiceLegSegments = persistServiceLegSegments;
  }

  public boolean isPersistServiceLegSegments() {
    return persistServiceLegSegments;
  }

  public boolean isPersistServiceNodes() {
    return persistServiceNodes;
  }

  public void setPersistServiceNodes(boolean persistServiceNodes) {
    this.persistServiceNodes = persistServiceNodes;
  }

  /**
   * Convenience method to log all the current settings
   */
  public void logSettings() {
    super.logSettings();
  }  

  /**
   * Reset content
   */
  public void reset() {
    super.reset();
  }

  public String getLayerPrefix() {
    return layerPrefix;
  }

  public void setLayerPrefix(String layerPrefix) {
    this.layerPrefix = layerPrefix;
  }

}
