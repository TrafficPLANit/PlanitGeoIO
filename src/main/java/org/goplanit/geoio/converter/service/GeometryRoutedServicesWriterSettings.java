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
public class GeometryRoutedServicesWriterSettings extends GeoIoWriterSettings implements ConverterWriterSettings {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(GeometryRoutedServicesWriterSettings.class.getCanonicalName());

  private String servicesFileName = DEFAULT_SERVICES_FILE_NAME;

  private String tripSchedulesFileName = DEFAULT_TRIP_SCHEDULES_FILE_NAME;

  private String tripFrequenciesFileName = DEFAULT_TRIP_FREQUENCIES_FILE_NAME;

  private boolean persistServices = DEFAULT_PERSIST_SERVICES;

  private boolean persistTripsSchedule = DEFAULT_PERSIST_TRIPS_SCHEDULE;

  private boolean persistTripsFrequency = DEFAULT_PERSIST_TRIPS_FREQUENCY;

  /** each layer gets a prefix prepended to the file name,e.g., #layer_prefix_#id_#filename */
  private String layerPrefix = DEFAULT_LAYER_PREFIX;

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

  public String getServicesFileName() {
    return servicesFileName;
  }

  public void setServicesFileName(String servicesFileName) {
    this.servicesFileName = servicesFileName;
  }

  public String getTripsScheduleFileName() {
    return tripSchedulesFileName;
  }

  public void setTripSchedulesFileName(String tripSchedulesFileName) {
    this.tripSchedulesFileName = tripSchedulesFileName;
  }

  public String getTripsFrequencyFileName() {
    return tripFrequenciesFileName;
  }

  public void setTripFrequenciesFileName(String tripFrequenciesFileName) {
    this.tripFrequenciesFileName = tripFrequenciesFileName;
  }

  public boolean isPersistServices() {
    return persistServices;
  }

  public void setPersistServices(boolean persistServices) {
    this.persistServices = persistServices;
  }

  public boolean isPersistTripsSchedule() {
    return persistTripsSchedule;
  }

  public void setPersistTripsSchedule(boolean persistTripsSchedule) {
    this.persistTripsSchedule = persistTripsSchedule;
  }

  public boolean isPersistTripsFrequency() {
    return persistTripsFrequency;
  }

  public void setPersistTripsFrequency(boolean persistTripsFrequency) {
    this.persistTripsFrequency = persistTripsFrequency;
  }

}
