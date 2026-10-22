package org.goplanit.geoio.util;

/**
 * supported formats as aprt of PLANit GeoIo converters
 */
public enum GeoIoFormat {

  /** shape file format, one file per table */
  SHAPE (".shp"),
  /** GeoPackage format */
  GEOPACKAGE(".gpkg");

  /** file extension of the format, with its dot */
  private final String extension;

  /**
   * Constructor
   * @param extension file extension of the format
   */
  GeoIoFormat(String extension){
    this.extension = extension;
  }

  /**
   * The file extension of the format, with its dot, e.g. ".gpkg"
   *
   * @return file extension
   */
  public String extension(){
    return extension;
  }
}
