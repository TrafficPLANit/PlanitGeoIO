package org.goplanit.geoio.converter.zoning;

import org.goplanit.xml.generated.XMLElementMacroscopicZoning;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Factory for creating Geometry based Zoning Writer
 * 
 * @author markr
 *
 */
public class GeometryZoningWriterFactory {
  
  /** Create a PLANitZoningWriter which can persist a PLANit zoning in GIS based format(s) such as shape file
   * 
   * @param outputPath the location to use for persisting
   * @param countryName the country to base the projection method on if available
   * @param zoningCrs crs used by the zoning
   * @return created zoning writer 
   */
  public static GeometryZoningWriter create(final String outputPath, final String countryName, final CoordinateReferenceSystem zoningCrs) {
    return new GeometryZoningWriter(outputPath, countryName, zoningCrs);
  }

}
