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

  /** nodes file name to use */
  private String nodesFileName = DEFAULT_NODES_FILE_NAME;

  /** link segments file name to use */
  private String linkSegmentsFileName = DEFAULT_LINK_SEGMENTS_FILE_NAME;

  /** default links file name to use */
  public static final String DEFAULT_LINKS_FILE_NAME = "planit_links.shp";

  /** default nodes file name to use */
  public static final String DEFAULT_NODES_FILE_NAME = "planit_links.shp";

  /** default link segments file name to use */
  public static final String DEFAULT_LINK_SEGMENTS_FILE_NAME = "planit_link_segments.shp";

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

  public String getNodesFileName() {
    return nodesFileName;
  }

  public void setNodesFileName(String nodesFileName) {
    this.nodesFileName = nodesFileName;
  }

  public String getLinkSegmentsFileName() {
    return linkSegmentsFileName;
  }

  public void setLinkSegmentsFileName(String linkSegmentsFileName) {
    this.linkSegmentsFileName = linkSegmentsFileName;
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
    
}
