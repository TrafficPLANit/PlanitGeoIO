package org.goplanit.geoio.util;

import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.converter.FileBasedConverterWriterSettings;

import java.util.logging.Logger;

/**
 * Settings relevant for persisting in any Geo IO output format
 * 
 * @author markr
 *
 */
public class GeoIoWriterSettings extends FileBasedConverterWriterSettings implements ConverterWriterSettings {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(GeoIoWriterSettings.class.getCanonicalName());

  private GeoIoFormat format = DEFAULT_FORMAT;

  public static final GeoIoFormat DEFAULT_FORMAT = GeoIoFormat.SHAPE;

  /**
   * Default constructor
   */
  public GeoIoWriterSettings() {
    super();
  }

  /**
   * Constructor
   *
   *  @param outputPathDirectory to use
   */
  public GeoIoWriterSettings(final String outputPathDirectory) {
    super(outputPathDirectory);
  }

  /**
   * Constructor
   *
   * @param outputPathDirectory to use
   * @param countryName to use
   */
  public GeoIoWriterSettings(final String outputPathDirectory, final String countryName) {
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

  /**
   * format determining the type of file that is being generated
   *
   * @return file format
   */
  public GeoIoFormat getFormat() {
    return format;
  }

  /**
   * Set the format to use
   *
   * @param format the format to use
   */
  public void setFormat(GeoIoFormat format){
    this.format = format;
  }
    
}
