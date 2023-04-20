package org.goplanit.geoio.converter.network;

import org.goplanit.utils.locale.CountryNames;

/**
 * Factory for creating GeometryNetworkWriters
 * @author markr
 *
 */
public class GeometryNetworkWriterFactory {
  
  /** Create a GeometryNetworkWriter which persists PLANit networks in common GIS formats in current working directory
   * 
   * @return created GeometryNetworkWriter
   */
  public static GeometryNetworkWriter create() {
    return create(".");
  }  
  
  /** Create a GeometryNetworkWriter which persists PLANit networks in common GIS formats
   * 
   * @param outputDirectory to use
   * @return created GeometryNetworkWriter
   */
  public static GeometryNetworkWriter create(String outputDirectory) {
    return create(outputDirectory, CountryNames.GLOBAL);    
  }
  
  /** Create a GeometryNetworkWriter which persists PLANit networks in in common GIS formats
   * 
   * @param outputDirectory to use
   * @param countryName country which the input file represents, used to determine defaults in case not specifically specified in OSM data, when left blank global defaults will be used
   * based on a right hand driving approach
   * @return created GeometryNetworkWriter
   */
  public static GeometryNetworkWriter create(String outputDirectory, String countryName) {
    return create(new GeometryNetworkWriterSettings(outputDirectory, countryName));
  }  
  
  /** Create a GeometryNetworkWriter which persists PLANit networks in common GIS formats
   * 
   * @param networkSettings to use
   * @return created GeometryNetworkWriter
   */
  public static GeometryNetworkWriter create(GeometryNetworkWriterSettings networkSettings) {
    return new GeometryNetworkWriter(networkSettings);
  }  
    
}
