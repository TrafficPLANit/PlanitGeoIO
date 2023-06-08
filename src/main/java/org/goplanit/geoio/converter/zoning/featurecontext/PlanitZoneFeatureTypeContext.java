package org.goplanit.geoio.converter.zoning.featurecontext;

import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.zoning.Zone;
import org.locationtech.jts.geom.Geometry;

import java.util.List;
import java.util.function.Function;

/**
 * Track contextual relevant information for PLANit zones that are persisted
 *
 * @author markr
 */
public class PlanitZoneFeatureTypeContext<Z extends Zone, T extends Geometry> extends PlanitEntityFeatureTypeContext<Z> {

  /** the type of geometry the zone(s) are using */
  private final Class<T> geometryClassType;

  /**
   * The mapping from PLANIT zone base GIS attributes (without geometry to allow for addition of other attributes until adding
   * geometry later via derived class using {@link #createGeometryFeatureDescription()}
   *
   * @param zoneIdMapper to apply
   * @return feature mapping
   */
  protected static <ZZ extends Zone, TT extends Geometry> List<Triple<String,String, Function<ZZ, ? extends Object>>> createBaseFeatureDescription(
      Function<ZZ, String> zoneIdMapper){

    return List.of(
            Triple.of("mapped_id", "String", zoneIdMapper),
            Triple.of("id", "java.lang.Long", ZZ::getId),
            Triple.of("xml_id", "String", ZZ::getXmlId),
            Triple.of("ext_id", "String", ZZ::getExternalId),
        Triple.of("name", "String", ZZ::getName));
  }

  /**
   * The mapping from PLANIT zone to its geometry attribute
   *
   * @return feature mapping entry created
   */
  protected Triple<String,String, Function<Z, ? extends Object>> createGeometryFeatureDescription(){

    return Triple.of(
        DEFAULT_GEOMETRY_ATTRIBUTE_KEY,
        getGisTypeFromJtsGeometryClass(geometryClassType),
        z -> geometryClassType.cast(z.getGeometry()));
  }

  /**
   * Constructor
   *
   * @param zoneIdMapper id mapper to apply
   */
  protected PlanitZoneFeatureTypeContext(
      final Class<Z> zoneClass, final Class<T> geometryClassType, Function<Z, String> zoneIdMapper){
    super(zoneClass, createBaseFeatureDescription(zoneIdMapper));
    this.geometryClassType = geometryClassType;
  }

  /**
   * The type of geometry this context represents
   *
   * @return type of geometry as class
   */
  public Class<T> getGeometryTypeClass() {
    return geometryClassType;
  }


}
