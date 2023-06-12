package org.goplanit.geoio.converter.service;

import org.goplanit.xml.generated.XMLElementRoutedServices;

/**
 * Factory for creating PLANit Routed Services writers for GIS based output format(s), e.g., shape files
 * 
 * @author markr
 *
 */
public class GeometryRoutedServicesWriterFactory {
  
  /** Create a GeometryRoutedServicesWriter which can persist a PLANit RoutedServices in GIS format with all defaults. It is expected the user sets the required
   * minimum configuration afterwards to be able to persist
   * 
   * @return created PLANit GeometryRoutedServicesWriter writer
   */
  public static GeometryRoutedServicesWriter create() {
    return new GeometryRoutedServicesWriter();
  }  
  
  /** Create a GeometryRoutedServicesWriter which can persist a PLANit RoutedServices in GIS format
   * 
   * @param outputPath the path to use for persisting
   * @return created GeometryRoutedServicesWriter
   */
  public static GeometryRoutedServicesWriter create(String outputPath) {
    return create(outputPath, null);
  }  
  
  /** Create a GeometryRoutedServicesWriter which can persist a PLANit RoutedServices in GIS format
   * 
   * @param outputPath the path to use for persisting
   * @param countryName the country to base the projection method on if available
   * @return created GeometryRoutedServicesWriter
   */
  public static GeometryRoutedServicesWriter create(String outputPath, String countryName) {
    return new GeometryRoutedServicesWriter(outputPath, countryName);
  }
     
}
