package org.goplanit.geoio.util;

import org.goplanit.utils.geo.PlanitGeoDataStoreUtils;

/**
 * supported formats as aprt of PLANit GeoIo converters
 */
public enum GeoIoFormat {

  SHAPE (PlanitGeoDataStoreUtils.SHAPE_FILE_EXTENSION),
  GEOPACKAGE(PlanitGeoDataStoreUtils.GEOPACKAGE_EXTENSION);

  private final String extension;

  /**
   * Constructor
   * @param extension file extension of the format
   */
  GeoIoFormat(String extension){
    this.extension = extension;
  }

  public String extension(){
    return extension;
  }
}
