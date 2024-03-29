package org.goplanit.geoio.converter.network;

import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.geoio.util.GeoIoWriterSettings;

import java.util.logging.Logger;

/**
 * Settings relevant for persisting the PLANit network in any Geo IO output format
 * 
 * @author markr
 *
 */
public class GeometryNetworkWriterSettings extends GeoIoWriterSettings implements ConverterWriterSettings {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(GeometryNetworkWriterSettings.class.getCanonicalName());

  /** links file name to use */
  private String linksFileName = DEFAULT_LINKS_FILE_NAME;

  /** link segments file name to use */
  private String linkSegmentsFileName = DEFAULT_LINKSEGMENTS_FILE_NAME;

  /** nodes file name to use */
  private String nodesFileName = DEFAULT_NODES_FILE_NAME;

  /** flag indicating whether to persist links or not */
  private boolean persistLinks = DEFAULT_PERSIST_LINKS;

  /** flag indicating whether to persist link segments or not */
  private boolean persistLinkSegments = DEFAULT_PERSIST_LINKSEGMENTS;

  /** flag indicating whether to persist nodes or not */
  private boolean persistNodes =  DEFAULT_PERSIST_NODES;

  /** each layer gets a prefix prepended to the file name,e.g., #layer_prefix_#id_#filename */
  private String layerPrefix = DEFAULT_LAYER_PREFIX;

  /** default links file name to use (without extension)*/
  public static final String DEFAULT_LINKS_FILE_NAME = "planit_links";

  /** default link segments file name to use (without extension)*/
  public static final String DEFAULT_LINKSEGMENTS_FILE_NAME = "planit_linksegments";

  /** default nodes file name to use (without extension) */
  public static final String DEFAULT_NODES_FILE_NAME = "planit_nodes";

  public static final String DEFAULT_LAYER_PREFIX = "layer";

  public static final String DEFAULT_EXTENSION = ".shp";

  /** default persist links flag value */
  public static boolean DEFAULT_PERSIST_LINKS = true;

  /** default persist link segments flag value */
  public static boolean DEFAULT_PERSIST_LINKSEGMENTS = true;


  /** default persist nodes flag value */
  public static boolean DEFAULT_PERSIST_NODES = true;

  /**
   * Default constructor
   */
  public GeometryNetworkWriterSettings() {
    super();
  }

  /**
   * Constructor
   *
   *  @param outputPathDirectory to use
   */
  public GeometryNetworkWriterSettings(final String outputPathDirectory) {
    super(outputPathDirectory);
  }

  /**
   * Constructor
   *
   * @param outputPathDirectory to use
   * @param countryName to use
   */
  public GeometryNetworkWriterSettings(final String outputPathDirectory, final String countryName) {
    super(outputPathDirectory, countryName);
  }

  public String getLinksFileName() {
    return linksFileName;
  }

  public void setLinksFileName(String linksFileName) {
    this.linksFileName = linksFileName;
  }

  public String getLinkSegmentsFileName() {
    return linkSegmentsFileName;
  }

  public void setLinkSegmentsFileName(String linkSegmentsFileName) {
    this.linkSegmentsFileName = linkSegmentsFileName;
  }

  public String getNodesFileName() {
    return nodesFileName;
  }

  public void setNodesFileName(String nodesFileName) {
    this.nodesFileName = nodesFileName;
  }

  public boolean isPersistLinks() {
    return persistLinks;
  }

  public boolean isPersistLinkSegments() {
    return persistLinkSegments;
  }

  public void setPersistLinks(boolean persistLinks) {
    this.persistLinks = persistLinks;
  }

  public boolean isPersistNodes() {
    return persistNodes;
  }

  public void setPersistNodes(boolean persistNodes) {
    this.persistNodes = persistNodes;
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
