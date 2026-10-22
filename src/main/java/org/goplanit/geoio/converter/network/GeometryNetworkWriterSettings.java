package org.goplanit.geoio.converter.network;

import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.geoio.util.GeoIoWriterSettings;
import org.goplanit.utils.misc.LoggingUtils;

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

  /** flag indicating if conjugate network is to be persisted */
  private boolean persistConjugate = DEFAULT_PERSIST_CONJUGATE_NETWORK;

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

  /** flag indicating whether to persist the intersection nodes or not: one row per intersection, with its properties
   * and its member nodes as a multi point */
  private boolean persistIntersectionNodes = DEFAULT_PERSIST_INTERSECTION_NODES;

  /** flag indicating whether to persist the approach and internal segments of intersections or not */
  private boolean persistIntersectionSegments = DEFAULT_PERSIST_INTERSECTION_SEGMENTS;

  /** intersection nodes file name to use */
  private String intersectionNodesFileName = DEFAULT_INTERSECTION_NODES_FILE_NAME;

  /** intersection segments file name to use */
  private String intersectionSegmentsFileName = DEFAULT_INTERSECTION_SEGMENTS_FILE_NAME;

  /** each layer gets a prefix prepended to the file name,e.g., #layer_prefix_#id_#filename */
  private String layerPrefix = DEFAULT_LAYER_PREFIX;

  /** default links file name to use (without extension)*/
  public static final String DEFAULT_LINKS_FILE_NAME = "planit_links";

  /** default link segments file name to use (without extension)*/
  public static final String DEFAULT_LINKSEGMENTS_FILE_NAME = "planit_linksegments";

  /** default nodes file name to use (without extension) */
  public static final String DEFAULT_NODES_FILE_NAME = "planit_nodes";

  /** default intersection nodes file name to use (without extension) */
  public static final String DEFAULT_INTERSECTION_NODES_FILE_NAME = "planit_intersection_nodes";

  /** default intersection segments file name to use (without extension) */
  public static final String DEFAULT_INTERSECTION_SEGMENTS_FILE_NAME = "planit_intersection_segments";

  /** default prefix each layer prepends to its file names */
  public static final String DEFAULT_LAYER_PREFIX = "layer";

  /** default prefix of the conjugate network's file names */
  public static final String DEFAULT_CONJUGATE_PREFIX = "conjugate_";

  /** default persist links flag value */
  public static boolean DEFAULT_PERSIST_LINKS = true;

  /** default persist link segments flag value */
  public static boolean DEFAULT_PERSIST_LINKSEGMENTS = true;

  /** default persist conjugate network flag value */
  public static boolean DEFAULT_PERSIST_CONJUGATE_NETWORK = false;


  /** default persist nodes flag value */
  public static boolean DEFAULT_PERSIST_NODES = true;

  /** default persist intersection nodes flag value */
  public static boolean DEFAULT_PERSIST_INTERSECTION_NODES = true;

  /** default persist intersection segments flag value */
  public static boolean DEFAULT_PERSIST_INTERSECTION_SEGMENTS = true;

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

  /**
   * The links file name to use (without extension)
   *
   * @return links file name
   */
  public String getLinksFileName() {
    return linksFileName;
  }

  /**
   * Set the links file name to use (without extension)
   *
   * @param linksFileName to use
   */
  public void setLinksFileName(String linksFileName) {
    this.linksFileName = linksFileName;
  }

  /**
   * The link segments file name to use (without extension)
   *
   * @return link segments file name
   */
  public String getLinkSegmentsFileName() {
    return linkSegmentsFileName;
  }

  /**
   * Set the link segments file name to use (without extension)
   *
   * @param linkSegmentsFileName to use
   */
  public void setLinkSegmentsFileName(String linkSegmentsFileName) {
    this.linkSegmentsFileName = linkSegmentsFileName;
  }

  /**
   * The nodes file name to use (without extension)
   *
   * @return nodes file name
   */
  public String getNodesFileName() {
    return nodesFileName;
  }

  /**
   * Set the nodes file name to use (without extension)
   *
   * @param nodesFileName to use
   */
  public void setNodesFileName(String nodesFileName) {
    this.nodesFileName = nodesFileName;
  }

  /**
   * Verify if links are persisted
   *
   * @return true when persisted, false otherwise
   */
  public boolean isPersistLinks() {
    return persistLinks;
  }

  /**
   * Verify if link segments are persisted
   *
   * @return true when persisted, false otherwise
   */
  public boolean isPersistLinkSegments() {
    return persistLinkSegments;
  }

  /**
   * Set whether links are persisted
   *
   * @param persistLinks true to persist, false otherwise
   */
  public void setPersistLinks(boolean persistLinks) {
    this.persistLinks = persistLinks;
  }

  /**
   * Set whether link segments are persisted
   *
   * @param persistLinkSegments true to persist, false otherwise
   */
  public void setPersistLinkSegments(boolean persistLinkSegments) {
    this.persistLinkSegments = persistLinkSegments;
  }

  /**
   * Verify if nodes are persisted
   *
   * @return true when persisted, false otherwise
   */
  public boolean isPersistNodes() {
    return persistNodes;
  }

  /**
   * Set whether nodes are persisted
   *
   * @param persistNodes true to persist, false otherwise
   */
  public void setPersistNodes(boolean persistNodes) {
    this.persistNodes = persistNodes;
  }

  /**
   * Verify if the intersection nodes are persisted: one row per intersection, with its properties and its member
   * nodes as a multi point
   *
   * @return true when persisted, false otherwise
   */
  public boolean isPersistIntersectionNodes() {
    return persistIntersectionNodes;
  }

  /**
   * Set whether the intersection nodes are persisted, see {@link #isPersistIntersectionNodes()}
   *
   * @param persistIntersectionNodes true to persist, false otherwise
   */
  public void setPersistIntersectionNodes(boolean persistIntersectionNodes) {
    this.persistIntersectionNodes = persistIntersectionNodes;
  }

  /**
   * Verify if the approach and internal segments of intersections are persisted
   *
   * @return true when persisted, false otherwise
   */
  public boolean isPersistIntersectionSegments() {
    return persistIntersectionSegments;
  }

  /**
   * Set whether the approach and internal segments of intersections are persisted
   *
   * @param persistIntersectionSegments true to persist, false otherwise
   */
  public void setPersistIntersectionSegments(boolean persistIntersectionSegments) {
    this.persistIntersectionSegments = persistIntersectionSegments;
  }

  /**
   * The intersection nodes file name to use (without extension)
   *
   * @return intersection nodes file name
   */
  public String getIntersectionNodesFileName() {
    return intersectionNodesFileName;
  }

  /**
   * Set the intersection nodes file name to use (without extension)
   *
   * @param intersectionNodesFileName to use
   */
  public void setIntersectionNodesFileName(String intersectionNodesFileName) {
    this.intersectionNodesFileName = intersectionNodesFileName;
  }

  /**
   * The intersection segments file name to use (without extension)
   *
   * @return intersection segments file name
   */
  public String getIntersectionSegmentsFileName() {
    return intersectionSegmentsFileName;
  }

  /**
   * Set the intersection segments file name to use (without extension)
   *
   * @param intersectionSegmentsFileName to use
   */
  public void setIntersectionSegmentsFileName(String intersectionSegmentsFileName) {
    this.intersectionSegmentsFileName = intersectionSegmentsFileName;
  }

  /**
   * Convenience method to log all the current settings
   */
  public void logSettings() {
    LOGGER.info(LoggingUtils.settingsHeader("Geometry Network Writer Settings"));
    super.logSettings();
    LOGGER.info(LoggingUtils.settingsValue("Persist nodes", isPersistNodes(), 0));
    LOGGER.info(LoggingUtils.settingsValue("Persist links", isPersistLinks(), 0));
    LOGGER.info(LoggingUtils.settingsValue("Persist link segments", isPersistLinkSegments(), 0));
    LOGGER.info(LoggingUtils.settingsValue("Persist intersection nodes", isPersistIntersectionNodes(), 0));
    LOGGER.info(LoggingUtils.settingsValue("Persist intersection segments", isPersistIntersectionSegments(), 0));
    LOGGER.info(LoggingUtils.settingsValue("Persist conjugate network", isPersistConjugateNetwork(), 0));
  }  

  /**
   * Reset content
   */
  public void reset() {
    super.reset();
  }

  /**
   * The prefix each layer prepends to its file names, as in prefix_layer id_file name
   *
   * @return layer prefix
   */
  public String getLayerPrefix() {
    return layerPrefix;
  }

  /**
   * Set the prefix each layer prepends to its file names
   *
   * @param layerPrefix to use
   */
  public void setLayerPrefix(String layerPrefix) {
    this.layerPrefix = layerPrefix;
  }

  /**
   * When set to true a conjugate version of the regular network is created and persisted as well.
   * All files will be prefixed with "conjugate_" to differentiate from the regular files
   *
   * @param flag to indicate choice made
   */
  public void setPersistConjugateNetwork(boolean flag) {
    this.persistConjugate = flag;
  }

  /**
   * Check status of flag regarding persistence of conjugate network
   *
   * @return flag
   */
  public boolean isPersistConjugateNetwork() {
    return persistConjugate;
  }
}
