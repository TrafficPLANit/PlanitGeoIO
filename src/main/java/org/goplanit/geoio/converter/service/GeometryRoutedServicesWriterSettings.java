package org.goplanit.geoio.converter.service;

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
public class GeometryRoutedServicesWriterSettings extends GeoIoWriterSettings implements ConverterWriterSettings {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(GeometryRoutedServicesWriterSettings.class.getCanonicalName());

  /** services file name to use (without extension) */
  private String servicesFileName = DEFAULT_SERVICES_FILE_NAME;

  /** trip schedules file name to use (without extension) */
  private String tripSchedulesFileName = DEFAULT_TRIP_SCHEDULES_FILE_NAME;

  /** trip frequencies file name to use (without extension) */
  private String tripFrequencyFileName = DEFAULT_TRIP_FREQUENCIES_FILE_NAME;

  /** flag indicating whether to persist services */
  private boolean persistServices = DEFAULT_PERSIST_SERVICES;

  /** flag indicating whether to persist schedule-based trips */
  private boolean persistTripsSchedule = DEFAULT_PERSIST_TRIPS_SCHEDULE;

  /** flag indicating whether to persist frequency-based trips */
  private boolean persistTripsFrequency = DEFAULT_PERSIST_TRIPS_FREQUENCY;

  /** each layer gets a prefix prepended to the file name,e.g., #layer_prefix_#id_#filename */
  private String layerPrefix = DEFAULT_LAYER_PREFIX;

  /** default layer prefix to use */
  public static final String DEFAULT_LAYER_PREFIX = "layer";

  /** default services file name to use (without extension) */
  public static final String DEFAULT_SERVICES_FILE_NAME = "planit_service";

  /** default trip schedules file name to use (without extension) */
  public static final String DEFAULT_TRIP_SCHEDULES_FILE_NAME = "planit_trip_schedule";

  /** default trip frequencies file name to use (without extension) */
  public static final String DEFAULT_TRIP_FREQUENCIES_FILE_NAME = "planit_trip_frequency";

  /** default persist services flag value */
  public static boolean DEFAULT_PERSIST_SERVICES = true;

  /** default persist trips (schedule based) flag value */
  public static boolean DEFAULT_PERSIST_TRIPS_SCHEDULE = true;

  /** default persist trips (frequency based) flag value */
  public static boolean DEFAULT_PERSIST_TRIPS_FREQUENCY = true;

  /**
   * Default constructor
   */
  public GeometryRoutedServicesWriterSettings() {
    super();
  }

  /**
   * Constructor
   *
   *  @param outputPathDirectory to use
   */
  public GeometryRoutedServicesWriterSettings(final String outputPathDirectory) {
    super(outputPathDirectory);
  }

  /**
   * Constructor
   *
   * @param outputPathDirectory to use
   * @param countryName to use
   */
  public GeometryRoutedServicesWriterSettings(final String outputPathDirectory, final String countryName) {
    super(outputPathDirectory, countryName);
  }

  /**
   * Convenience method to log all the current settings
   */
  public void logSettings() {
    LOGGER.info(LoggingUtils.settingsHeader("Geometry Routed Services Writer Settings"));
    super.logSettings();
    LOGGER.info(LoggingUtils.settingsValue("Persist services", isPersistServices(), 0));
    LOGGER.info(LoggingUtils.settingsValue("Persist trips schedule", isPersistTripsSchedule(), 0));
    LOGGER.info(LoggingUtils.settingsValue("Persist trips frequency", isPersistTripsFrequency(), 0));
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
   * The services file name to use (without extension)
   *
   * @return services file name
   */
  public String getServicesFileName() {
    return servicesFileName;
  }

  /**
   * Set the services file name to use (without extension)
   *
   * @param servicesFileName to use
   */
  public void setServicesFileName(String servicesFileName) {
    this.servicesFileName = servicesFileName;
  }

  /**
   * The schedule-based trips file name to use (without extension)
   *
   * @return schedule-based trips file name
   */
  public String getTripsScheduleFileName() {
    return tripSchedulesFileName;
  }

  /**
   * Set the schedule-based trips file name to use (without extension)
   *
   * @param tripSchedulesFileName to use
   */
  public void setTripsScheduleFileName(String tripSchedulesFileName) {
    this.tripSchedulesFileName = tripSchedulesFileName;
  }

  /**
   * The frequency-based trips file name to use (without extension)
   *
   * @return frequency-based trips file name
   */
  public String getTripsFrequencyFileName() {
    return tripFrequencyFileName;
  }

  /**
   * Set the frequency-based trips file name to use (without extension)
   *
   * @param tripFrequencyFileName to use
   */
  public void setTripsFrequencyFileName(String tripFrequencyFileName) {
    this.tripFrequencyFileName = tripFrequencyFileName;
  }

  /**
   * Verify if services are persisted
   *
   * @return true when persisted, false otherwise
   */
  public boolean isPersistServices() {
    return persistServices;
  }

  /**
   * Set whether services are persisted
   *
   * @param persistServices true to persist, false otherwise
   */
  public void setPersistServices(boolean persistServices) {
    this.persistServices = persistServices;
  }

  /**
   * Verify if schedule-based trips are persisted
   *
   * @return true when persisted, false otherwise
   */
  public boolean isPersistTripsSchedule() {
    return persistTripsSchedule;
  }

  /**
   * Set whether schedule-based trips are persisted
   *
   * @param persistTripsSchedule true to persist, false otherwise
   */
  public void setPersistTripsSchedule(boolean persistTripsSchedule) {
    this.persistTripsSchedule = persistTripsSchedule;
  }

  /**
   * Verify if frequency-based trips are persisted
   *
   * @return true when persisted, false otherwise
   */
  public boolean isPersistTripsFrequency() {
    return persistTripsFrequency;
  }

  /**
   * Set whether frequency-based trips are persisted
   *
   * @param persistTripsFrequency true to persist, false otherwise
   */
  public void setPersistTripsFrequency(boolean persistTripsFrequency) {
    this.persistTripsFrequency = persistTripsFrequency;
  }

}
