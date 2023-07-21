package org.goplanit.geoio.converter.intermodal;

import org.goplanit.utils.locale.CountryNames;

/**
 * Factory for creating Geometry (GIS) intermodal writers persisting both a network and zoning (useful for intermodal networks with pt element where transfer zones are part of the
 * zoning), or also include services (service network and routed services)
 * 
 * @author markr
 *
 */
public class GeometryIntermodalWriterFactory {
  
  /** Default factory method. Create a GeometryIntermodalWriter which can persist a PLANit network and zoning in a GIS format.
   * We assume the user sets the output directory (default now current working dir) and destination country afterwards
   * 
   * @return created writer
   */
  public static GeometryIntermodalWriter create() {
    return create(".");
  }    
  
  /** Create a GeometryIntermodalWriter which can persist a Geometry (GIS) network and zoning in GIS format. No destination country is provided, so we assume the current
   * Crs for persisting
   * 
   * @param outputDirectory the path to use for persisting
   * @return created writer
   */
  public static GeometryIntermodalWriter create(String outputDirectory) {
    return create(outputDirectory, CountryNames.GLOBAL);    
  }  
  
  /** Create a GeometryIntermodalWriter which can persist a PLANit network and zoning in GIS format
   * 
   * @param outputDirectory the path to use for persisting
   * @param countryName the country to base the projection method on if available
   * @return created writer
   */
  public static GeometryIntermodalWriter create(String outputDirectory, String countryName) {
    return new GeometryIntermodalWriter(outputDirectory, countryName);
  }

  /** Create a GeometryIntermodalWriter which can persist a PLANit network and zoning in GIS format. It is assumed all mandatory settings
   * will be provided (or are provided already) via the settings
   *
   * @param settings to inject
   * @return created writer
   */
  public static GeometryIntermodalWriter create(GeometryIntermodalWriterSettings settings) {
    return new GeometryIntermodalWriter(settings);
  }

}
