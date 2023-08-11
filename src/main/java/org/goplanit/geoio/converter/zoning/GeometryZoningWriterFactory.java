package org.goplanit.geoio.converter.zoning;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Factory for creating Geometry based Zoning Writer
 * 
 * @author markr
 *
 */
public class GeometryZoningWriterFactory {

  /** Create a default geometryZoningWriter which can persist a PLANit zoning in GIS based format(s) such as shape file.
   * It is expected the user configures the output location and other settings afterwards
   *
   * @return created zoning writer
   */
  public static GeometryZoningWriter create() {
    return new GeometryZoningWriter();
  }
  
  /** Create a PLANitZoningWriter which can persist a PLANit zoning in GIS based format(s) such as shape file
   * 
   * @param outputPath the location to use for persisting
   * @param countryName the country to base the projection method on if available
   * @return created zoning writer
   */
  public static GeometryZoningWriter create(final String outputPath, final String countryName) {
    return new GeometryZoningWriter(outputPath, countryName);
  }

}
