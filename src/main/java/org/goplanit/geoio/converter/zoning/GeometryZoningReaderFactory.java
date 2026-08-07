package org.goplanit.geoio.converter.zoning;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.zoning.Zoning;

/**
 * Factory for creating geometry/gis zoning readers. Only used to provide the spatial setup of the zoning system and map
 * its ids to the demand when that is parsed afterwards.
 *
 * @author markr
 *
 */
public class GeometryZoningReaderFactory {

  /** Create a GeometryZoningReader
   *
   * @param inputFile to use
   * @param sourceCrs source CRS
   * @return created GeometryZoningReader
   */
  public static GeometryZoningReader create(
      String inputFile, CoordinateReferenceSystem sourceCrs) {
   return create(new GeometryZoningReaderSettings(inputFile, sourceCrs));
  }

  /** Create a GeometryZoningReader
   *
   * @param settings to use
   * @return created GeometryZoningReader
   */
  public static GeometryZoningReader create(
      GeometryZoningReaderSettings settings) {
    MacroscopicNetwork dummyNetwork = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    dummyNetwork.setCoordinateReferenceSystem(settings.getSourceCrs());
    return create(
        settings,
        dummyNetwork);
  }

  /** Create a GeometryZoningReader
   *
   * @param settings to use
   * @param referenceNetwork to use (typically empty since ActivitySim has no network and is demand only)
   * @return created ActivitySimZoningReader
   */
  public static GeometryZoningReader create(
      GeometryZoningReaderSettings settings,
      MacroscopicNetwork referenceNetwork) {
    return create(
        settings,
        referenceNetwork,
        new Zoning(referenceNetwork.getIdGroupingToken(), referenceNetwork.getNetworkGroupingTokenId()));
  }

  /** Create a GeometryZoningReader
   *
   * @param settings to use
   * @param referenceNetwork to use (typically empty since ActivitySim has no network and is demand only)
   * @param zoningToPopulate zoning to populate
   * @return created ActivitySimZoningReader
   */
  public static GeometryZoningReader create(
      GeometryZoningReaderSettings settings,
      MacroscopicNetwork referenceNetwork,
      Zoning zoningToPopulate) {
    return new GeometryZoningReader(settings, referenceNetwork, zoningToPopulate);
  }

}

