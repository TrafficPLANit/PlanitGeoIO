package org.goplanit.geoio.converter.zoning.featurecontext;

import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.Zone;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.operation.MathTransform;

import java.util.function.Function;

/**
 * Track contextual relevant information for PLANit OD zones that are persisted
 *
 * @author markr
 */
public class PlanitOdZoneFeatureTypeContext<T extends Geometry> extends PlanitZoneFeatureTypeContext<OdZone, T> {

  /**
   * Add any additional features unique to OD zones (and not available in base zone description) to feature description
   */
  protected void appendOdZoneFeatureDescription(){

    // currently no specific OD zone related properties persisted
    // example if needed: appendToFeatureTypeDescription(
    //    Triple.of("mapped_id", "String", OdZone::getName),
    //    );
  }

  /**
   * Constructor
   *
   * @param zoneIdMapper id mapper to apply
   * @param geometryType to apply (for the subset of PLANit od zones of this type)
   * @param destinationCrsTransformer to use (may be null)
   */
  protected PlanitOdZoneFeatureTypeContext(
      Function<OdZone, String> zoneIdMapper,
      Class<T> geometryType,
      final MathTransform destinationCrsTransformer){
    super(OdZone.class, geometryType, zoneIdMapper);

    /* add od zone specific attributes */
    appendOdZoneFeatureDescription();

    /* finish with geometry */
    appendToFeatureTypeDescription(createGeometryFeatureDescription(destinationCrsTransformer));
  }

  /**
   * Factory method
   *
   * @param <TT> the type of geometry
   * @param zoneIdMapper to apply for creating each od zone's unique id when persisting
   * @param geometryType to apply for this context
   * @param destinationCrsTransformer to use (may be null)
   * @return created instance
   */
  public static <TT extends Geometry> PlanitOdZoneFeatureTypeContext<TT> create(
      Function<? super Zone, String> zoneIdMapper,Class<TT> geometryType,
      final MathTransform destinationCrsTransformer){
    return new PlanitOdZoneFeatureTypeContext<>(
        zoneIdMapper::apply /* convert to OdZone as type */,
        geometryType,
        destinationCrsTransformer);
  }

}
