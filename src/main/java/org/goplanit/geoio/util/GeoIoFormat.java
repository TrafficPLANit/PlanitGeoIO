package org.goplanit.geoio.util;

/**
 * supported formats as aprt of PLANit GeoIo converters
 */
public enum GeoIoFormat {

  SHAPE (".shp"),
  GEOPACKAGE(".gpkg");

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
