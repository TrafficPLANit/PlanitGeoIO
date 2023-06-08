package org.goplanit.geoio.converter.zoning.featurecontext;

import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.TransferZone;
import org.goplanit.utils.zoning.TransferZoneGroup;
import org.goplanit.utils.zoning.Zone;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import java.util.function.Function;

/**
 * Track contextual relevant information for PLANit Transfer zones that are persisted
 *
 * @author markr
 */
public class PlanitTransferZoneFeatureTypeContext<T extends Geometry> extends PlanitZoneFeatureTypeContext<TransferZone, T> {

  /**
   * Add any additional features unique to Transfer zones (and not available in base zone description) to feature description
   */
  protected void appendTransferZoneFeatureDescription(){

    // currently no specific OD zone related properties persisted
    // example if needed: appendToFeatureTypeDescription(
    //    Triple.of("mapped_id", "String", OdZone::getName),
    //    );
  }

  /**
   * Constructor
   *
   * @param zoneIdMapper id mapper to apply
   */
  protected PlanitTransferZoneFeatureTypeContext(Function<TransferZone, String> zoneIdMapper, Class<T> geometryType){
    super(TransferZone.class, geometryType, zoneIdMapper);

    /* add od zone specific attributes */
    appendTransferZoneFeatureDescription();

    /* finish with geometry */
    appendToFeatureTypeDescription(createGeometryFeatureDescription());
  }

  /**
   * Factory method
   *
   * @param zoneIdMapper to apply for creating each service node's unique id when persisting
   * @return created instance
   */
  public static <TT extends Geometry> PlanitTransferZoneFeatureTypeContext create(
    Function<? super Zone, String> zoneIdMapper, Class<TT> geometryType){

    return new PlanitTransferZoneFeatureTypeContext<>(
          (z) -> zoneIdMapper.apply(z) /* convert to TransferZone as type */,
          geometryType);
  }

}
