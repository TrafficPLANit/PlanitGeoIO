package org.goplanit.geoio.converter.zoning.featurecontext;

import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.converter.idmapping.ZoningIdMapper;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.TransferZone;
import org.goplanit.utils.zoning.connectoid.TransferConnectoid;
import org.geotools.api.referencing.operation.MathTransform;

import java.util.stream.Collectors;

/**
 * Track contextual relevant information for PLANit Directed connectoids that are persisted
 *
 * @author markr
 */
public class PlanitDirectedConnectoidFeatureTypeContext
    extends PlanitConnectoidFeatureTypeContext<TransferConnectoid> {

  /**
   * Add any additional features unique to directed connectoids (and not available in base description)
   * to feature description
   *
   * @param networkIdMapper to use
   * @param zoningIdMapper  to use
   */
  @SuppressWarnings("unchecked")
  protected void appendDirectedConnectoidFeatureDescription(
      final NetworkIdMapper networkIdMapper, ZoningIdMapper zoningIdMapper){

    this.appendToFeatureTypeDescription(
        Triple.of("phys_sgms", "String",
          c ->  c.getExplicitAccessLinkSegmentsStream().map(ls ->
                  networkIdMapper.getMacroscopicLinkSegmentIdMapper().apply((MacroscopicLinkSegment) ls)).collect(
                  Collectors.joining(","))));

      appendToFeatureTypeDescription(
          // todo: improve as this is now a simplification compared to PLANit data
          Triple.of("trsfzones", "String",
              c -> c.getAccessZoneStream().filter(z -> z instanceof TransferZone).map(
                  z -> zoningIdMapper.getTransferZoneIdMapper().apply((TransferZone)z)).collect(
                  Collectors.joining(","))));

      // function for modes mapping tailored to TransferZones only
      var accessZoneModesStringFunc = accessZoneModeStrFunc(
          zoningIdMapper.getTransferZoneIdMapper(), accessModesToStringConversionFunc(networkIdMapper));
      appendToFeatureTypeDescription(Triple.of("modes", "String",
          c -> c.getAccessZoneStream()
              .filter(z -> z instanceof TransferZone)
              .map(z -> accessZoneModesStringFunc.apply((TransferZone) z, c))
              .collect(Collectors.joining(","))));

      // function for lengths mapping tailored to Od zones only
      var accessZoneLengthStringFunc = accessZoneLengthsStrFunc(zoningIdMapper.getTransferZoneIdMapper());
      appendToFeatureTypeDescription(
          Triple.of("lengths_km", "String",c -> c.getAccessZoneStream()
              .filter(z -> z instanceof TransferZone)
              .map( z ->accessZoneLengthStringFunc.apply( (TransferZone)z, c))
              .collect(Collectors.joining(","))));
  }

  /**
   * Constructor
   *
   * @param zoningIdMapper id mapper to apply
   * @param networkIdMapper id mapper of parent physical network to apply
   * @param destinationCrsTransformer to use (may be null)
   */
  protected PlanitDirectedConnectoidFeatureTypeContext(
      final ZoningIdMapper zoningIdMapper,
          final NetworkIdMapper networkIdMapper,
          final MathTransform destinationCrsTransformer){
    super(TransferConnectoid.class, zoningIdMapper, networkIdMapper);

    /* add od zone specific attributes */
    appendDirectedConnectoidFeatureDescription(networkIdMapper, zoningIdMapper);

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
  public static PlanitDirectedConnectoidFeatureTypeContext create(
      final ZoningIdMapper zoningIdMapper,
          final NetworkIdMapper networkIdMapper,
          final MathTransform destinationCrsTransformer){
    return new PlanitDirectedConnectoidFeatureTypeContext(zoningIdMapper, networkIdMapper, destinationCrsTransformer);
  }

}
