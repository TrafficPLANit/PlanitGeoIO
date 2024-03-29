package org.goplanit.geoio.converter.zoning;

import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.geoio.util.GeoIoWriterSettings;

import java.util.logging.Logger;

/**
 * Settings relevant for persisting the PLANit zoning in any Geo IO output format
 * 
 * @author markr
 *
 */
public class GeometryZoningWriterSettings extends GeoIoWriterSettings implements ConverterWriterSettings {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(GeometryZoningWriterSettings.class.getCanonicalName());

  private String odZonesFileName = DEFAULT_OD_ZONES_FILE_NAME;

  private String odConnectoidsFileName = DEFAULT_OD_CONNECTOIDS_FILE_NAME;

  private String transferZonesFileName = DEFAULT_TRANSFER_ZONES_FILE_NAME;

  private String transferConnectoidsFileName = DEFAULT_TRANSFER_CONNECTOIDS_FILE_NAME;

  private String connectoidEdgesFileName = DEFAULT_CONNECTOID_EDGES_FILE_NAME;

  private String connectoidSegmentsFileName = DEFAULT_CONNECTOID_SEGMENTS_FILE_NAME;

  private boolean persistOdZones = DEFAULT_PERSIST_OD_ZONES;

  private boolean persistOdConnectoids = DEFAULT_PERSIST_OD_CONNECTOIDS;

  private boolean persistTransferZones = DEFAULT_PERSIST_TRANSFER_ZONES;

  private boolean persistTransferConnectoids = DEFAULT_PERSIST_TRANSFER_CONNECTOIDS;

  private boolean persistVirtualNetwork = DEFAULT_PERSIST_VIRTUAL_NETWORK;

  /** default od zones file name to use (without extension) */
  public static final String DEFAULT_OD_ZONES_FILE_NAME = "planit_zones_od";

  /** default od connectoids file name to use (without extension) */
  public static final String DEFAULT_OD_CONNECTOIDS_FILE_NAME = "planit_connectoids_od";

  /** default transfer zones file name to use (without extension) */
  public static final String DEFAULT_TRANSFER_ZONES_FILE_NAME = "planit_zones_transfer";

  /** default transfer connectoids file name to use (without extension) */
  public static final String DEFAULT_TRANSFER_CONNECTOIDS_FILE_NAME = "planit_connectoids_transfer";

  /** default connectoid edges file name to use (without extension) */
  public static final String DEFAULT_CONNECTOID_EDGES_FILE_NAME = "planit_connectoid_edges";

  /** default connectoid segments file name to use (without extension) */
  public static final String DEFAULT_CONNECTOID_SEGMENTS_FILE_NAME = "planit_connectoid_segments";

  /** default persist OD zones flag value */
  public static boolean DEFAULT_PERSIST_OD_ZONES = true;

  /** default persist OD connectoids flag value */
  public static boolean DEFAULT_PERSIST_OD_CONNECTOIDS = true;

  /** default persist transfer zones flag value */
  public static boolean DEFAULT_PERSIST_TRANSFER_ZONES = true;

  /** default persist transfer connectoids flag value */
  public static boolean DEFAULT_PERSIST_TRANSFER_CONNECTOIDS = true;

  /** default persist virtual network flag value */
  public static boolean DEFAULT_PERSIST_VIRTUAL_NETWORK = true;

  /**
   * Default constructor
   */
  public GeometryZoningWriterSettings() {
    super();
  }

  /**
   * Constructor
   *
   *  @param outputPathDirectory to use
   */
  public GeometryZoningWriterSettings(final String outputPathDirectory) {
    super(outputPathDirectory);
  }

  /**
   * Constructor
   *
   * @param outputPathDirectory to use
   * @param countryName to use
   */
  public GeometryZoningWriterSettings(final String outputPathDirectory, final String countryName) {
    super(outputPathDirectory, countryName);
  }

  /**
   * Reset content
   */
  public void reset() {
    super.reset();
  }

  /** flag indicating whether to persist od zones or not
   *
   * @return  true when persisting od zones, false otherwise
   */
  public boolean isPersistOdZones() {
    return persistOdZones;
  }

  /**
   * Indicate whether to persist OD zones
   *
   * @param persistOdZones flag to set
   */
  public void setPersistOdZones(boolean persistOdZones) {
    this.persistOdZones = persistOdZones;
  }

  /** flag indicating whether to persist transfer zones or not
   *
   * @return  true when persisting transfer zones, false otherwise
   * */
  public boolean isPersistTransferZones() {
    return persistTransferZones;
  }

  /**
   * Indicate whether to persist transfer zones
   *
   * @param persistTransferZones flag to set
   */
  public void setPersistTransferZones(boolean persistTransferZones) {
    this.persistTransferZones = persistTransferZones;
  }

  /** File name to use (without extension or geometry type suffix)
   *
   * @return file name chosen */
  public String getOdZonesFileName() {
    return odZonesFileName;
  }

  /**
   * The file name to use  (without extension or geometry type suffix)
   *
   * @param fileName file name to use
   */
  public void setOdZonesFileName(String fileName) {
    this.odZonesFileName = fileName;
  }

  /** transfer zones file name to use  (without extension or geometry type suffix)
   *
   * @return  the transfer zone file name used
   * */
  public String getTransferZonesFileName() {
    return transferZonesFileName;
  }

  /**
   * The file name to use  (without extension or geometry type suffix)
   *
   * @param fileName file name to use
   */
  public void setTransferZonesFileName(String fileName) {
    this.transferZonesFileName = fileName;
  }

  /** flag indicating whether we should persist the virtual network connecting the zones to a physical network
   * via connectoids edges and edge segments, needs to be true in order for any settings related to the connectoid
   * edges and edges segments to be picked up
   *
   * @return flag
   */
  public boolean isPersistVirtualNetwork() {
    return persistVirtualNetwork;
  }

  /** Set flag indicating whether we should persist the virtual network connecting the zones to a physical network
   * via connectoids edges and edge segments, needs to be true in order for any settings related to the connectoid
   * edges and edges segments to be picked up
   *
   * @param flag to set
   */
  public void setPersistVirtualNetwork(boolean flag) {
    this.persistVirtualNetwork = flag;
  }

  public boolean isPersistOdConnectoids() {
    return persistOdConnectoids;
  }

  public void setPersistOdConnectoids(boolean persistOdConnectoids) {
    this.persistOdConnectoids = persistOdConnectoids;
  }

  public boolean isPersistTransferConnectoids() {
    return persistTransferConnectoids;
  }

  /**
   * The file name to use  (without extension or geometry type suffix)
   *
   * @param fileName file name to use
   */
  public void setPersistTransferConnectoids(boolean fileName) {
    this.persistTransferConnectoids = fileName;
  }

  /** File name to use (without extension or geometry type suffix)
   *
   * @return file name chosen */
  public String getOdConnectoidsFileName() {
    return odConnectoidsFileName;
  }

  /**
   * The file name to use  (without extension or geometry type suffix)
   *
   * @param fileName file name to use
   */
  public void setOdConnectoidsFileName(String fileName) {
    this.odConnectoidsFileName = fileName;
  }

  /** File name to use (without extension or geometry type suffix)
   *
   * @return file name chosen */
  public String getTransferConnectoidsFileName() {
    return transferConnectoidsFileName;
  }

  /**
   * The file name to use  (without extension or geometry type suffix)
   *
   * @param fileName file name to use
   */
  public void setTransferConnectoidsFileName(String fileName) {
    this.transferConnectoidsFileName = fileName;
  }

  /** File name to use (without extension or geometry type suffix)
   *
   * @return file name chosen */
  public String getConnectoidEdgesFileName() {
    return connectoidEdgesFileName;
  }

  /**
   * The file name to use  (without extension or geometry type suffix)
   *
   * @param fileName file name to use
   */
  public void setConnectoidEdgesFileName(String fileName) {
    this.connectoidEdgesFileName = fileName;
  }

  /** File name to use (without extension or geometry type suffix)
   *
   * @return file name chosen */
  public String getConnectoidSegmentsFileName() {
    return connectoidSegmentsFileName;
  }

  /**
   * The file name to use  (without extension or geometry type suffix)
   *
   * @param fileName file name to use
   */
  public void setConnectoidSegmentsFileName(String fileName) {
    this.connectoidSegmentsFileName = fileName;
  }
}
