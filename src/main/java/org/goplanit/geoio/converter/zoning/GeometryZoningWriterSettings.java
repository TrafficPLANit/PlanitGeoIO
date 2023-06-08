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

  private String transferZonesFileName = DEFAULT_TRANSFER_ZONES_FILE_NAME;

  private boolean persistOdZones = DEFAULT_PERSIST_OD_ZONES;

  private boolean persistTransferZones = DEFAULT_PERSIST_TRANSFER_ZONES;

  /** default od zones file name to use (without extension) */
  public static final String DEFAULT_OD_ZONES_FILE_NAME = "planit_zones_od";

  /** default transfer zones file name to use (without extension) */
  public static final String DEFAULT_TRANSFER_ZONES_FILE_NAME = "planit_zones_transfer";

  /** default persist OD zones flag value */
  public static boolean DEFAULT_PERSIST_OD_ZONES = true;

  /** default persist OD zones flag value */
  public static boolean DEFAULT_PERSIST_TRANSFER_ZONES = true;

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

  /** flag indicating whether to persist od zones or not */
  public boolean isPersistOdZones() {
    return persistOdZones;
  }

  public void setPersistOdZones(boolean persistOdZones) {
    this.persistOdZones = persistOdZones;
  }

  /** flag indicating whether to persist transfer zones or not */
  public boolean isPersistTransferZones() {
    return persistTransferZones;
  }

  public void setPersistTransferZones(boolean persistTransferZones) {
    this.persistTransferZones = persistTransferZones;
  }

  /** od zones file name to use */
  public String getOdZonesFileName() {
    return odZonesFileName;
  }

  public void setOdZonesFileName(String odZonesFileName) {
    this.odZonesFileName = odZonesFileName;
  }

  /** transfer zones file name to use */
  public String getTransferZonesFileName() {
    return transferZonesFileName;
  }

  public void setTransferZonesFileName(String transferZonesFileName) {
    this.transferZonesFileName = transferZonesFileName;
  }
}
