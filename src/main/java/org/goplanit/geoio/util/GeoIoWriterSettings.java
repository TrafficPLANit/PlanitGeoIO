package org.goplanit.geoio.util;

import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.converter.FileBasedConverterWriterSettings;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.CharacterUtils;
import org.goplanit.utils.misc.StringUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.text.DecimalFormat;
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

  /** extension determining type of file */
  private String fileExtension = DEFAULT_EXTENSION;

  public static final String DEFAULT_EXTENSION = ".shp";

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
   * File extension determining the type of file that is being generated
   *
   * @return file extension
   */
  public String getFileExtension() {
    return fileExtension;
  }
    
}
