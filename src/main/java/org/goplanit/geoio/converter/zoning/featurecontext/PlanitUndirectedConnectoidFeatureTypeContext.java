package org.goplanit.geoio.converter.zoning.featurecontext;

import org.geotools.api.referencing.operation.MathTransform;
import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.converter.idmapping.ZoningIdMapper;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.connectoid.OdConnectoid;

import java.util.stream.Collectors;

/**
 * Track contextual relevant information for PLANit Undirected connectoids that are persisted
 *
 * @author markr
 */
public class PlanitUndirectedConnectoidFeatureTypeContext extends PlanitConnectoidFeatureTypeContext<OdConnectoid> {

  /**
   * Add any additional features unique to undirected connectoids (and not available in base description) to
   * feature description
   *
   * @param networkIdMapper to use
   * @param zoningIdMapper  to use
   */
  @SuppressWarnings("unchecked")
  protected void appendUndirectedConnectoidFeatureDescription(
      NetworkIdMapper networkIdMapper, ZoningIdMapper zoningIdMapper){

    appendToFeatureTypeDescription(
        // todo: improve as this is now a simplification compared to PLANit data
        Triple.of("odzones", "String",
            c -> c.getAccessZoneStream().filter(z -> z instanceof OdZone).map(
                z -> zoningIdMapper.getOdZoneIdMapper().apply((OdZone)z)).collect(
                Collectors.joining(","))));

    // function for modes mapping tailored to OdZones only
    var accessZoneModesStringFunc =
        accessZoneModeStrFunc(zoningIdMapper.getOdZoneIdMapper(), accessModesToStringConversionFunc(networkIdMapper));
    appendToFeatureTypeDescription(Triple.of("modes", "String",
            c -> c.getAccessZoneStream()
                .filter(z -> z instanceof OdZone)
                .map(z -> accessZoneModesStringFunc.apply((OdZone) z, c))
                .collect(Collectors.joining(","))));

    // function for lengths mapping tailored to Od zones only
    var accessZoneLengthStringFunc = accessZoneLengthsStrFunc(zoningIdMapper.getOdZoneIdMapper());
    appendToFeatureTypeDescription(
        Triple.of("lengths_km", "String",c -> c.getAccessZoneStream()
            .filter(z -> z instanceof OdZone)
            .map( z ->accessZoneLengthStringFunc.apply( (OdZone)z, c))
            .collect(Collectors.joining(","))));
  }

  /**
   * Constructor
   *
   * @param zoningIdMapper id mapper to apply
   * @param networkIdMapper id mapper of parent physical network to apply
   * @param destinationCrsTransformer to use (may be null)
   */
  @SuppressWarnings("unchecked")
  protected PlanitUndirectedConnectoidFeatureTypeContext(
      final ZoningIdMapper zoningIdMapper,
          final NetworkIdMapper networkIdMapper,
          final MathTransform destinationCrsTransformer){
    super(OdConnectoid.class, zoningIdMapper, networkIdMapper);

    /* add od zone specific attributes */
    appendUndirectedConnectoidFeatureDescription(networkIdMapper, zoningIdMapper);

    /* finish with geometry */
    appendToFeatureTypeDescription(createGeometryFeatureDescription(destinationCrsTransformer));
  }

  /**
   * Factory method
   *
   * @param zoningIdMapper id mapper to apply
   * @param networkIdMapper id mapper of parent physical network to apply
   * @param destinationCrsTransformer to use (may be null)
   * @return created instance
   */
  public static PlanitUndirectedConnectoidFeatureTypeContext create(
      final ZoningIdMapper zoningIdMapper,
      final NetworkIdMapper networkIdMapper,
      final MathTransform destinationCrsTransformer){
    return new PlanitUndirectedConnectoidFeatureTypeContext(
            zoningIdMapper, networkIdMapper, destinationCrsTransformer);
  }

}
