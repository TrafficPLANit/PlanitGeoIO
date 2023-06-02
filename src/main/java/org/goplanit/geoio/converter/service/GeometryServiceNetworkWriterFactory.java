package org.goplanit.geoio.converter.service;

import org.goplanit.utils.locale.CountryNames;

/**
 * Factory for creating GeometryServiceNetworkWriters
 * @author markr
 *
 */
public class GeometryServiceNetworkWriterFactory {
  
  /** Create a GeometryServiceNetworkWriter which persists PLANit service networks in common GIS formats in current working directory
   * 
   * @return created GeometryServiceNetworkWriter
   */
  public static GeometryServiceNetworkWriter create() {
    return create(".");
  }  
  
  /** Create a GeometryNetworkWriter which persists PLANit service networks in common GIS formats
   * 
   * @param outputDirectory to use
   * @return created GeometryNetworkWriter
   */
  public static GeometryServiceNetworkWriter create(String outputDirectory) {
    return create(outputDirectory, CountryNames.GLOBAL);    
  }
  
  /** Create a GeometryServiceNetworkWriter which persists PLANit service networks in common GIS formats
   * 
   * @param outputDirectory to use
   * @param countryName country which the input file represents, used to determine defaults in case not specifically specified, when left blank global defaults will be used
   * based on a right hand driving approach
   * @return created GeometryServiceNetworkWriter
   */
  public static GeometryServiceNetworkWriter create(String outputDirectory, String countryName) {
    return create(new GeometryServiceNetworkWriterSettings(outputDirectory, countryName));
  }  
  
  /** Create a GeometryNetworkWriter which persists PLANit networks in common GIS formats
   * 
   * @param serviceNetworkSettings to use
   * @return created GeometryServiceNetworkWriter
   */
  public static GeometryServiceNetworkWriter create(GeometryServiceNetworkWriterSettings serviceNetworkSettings) {
    return new GeometryServiceNetworkWriter(serviceNetworkSettings);
  }  
    
}
