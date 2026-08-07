package org.goplanit.geoio.converter.zoning.featurecontext;

import org.geotools.api.referencing.operation.MathTransform;
import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.converter.idmapping.ZoningIdMapper;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.zoning.connectoid.Connectoid;
import org.goplanit.utils.zoning.Zone;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Track contextual relevant information for PLANit connectoids that are persisted
 *
 * @author markr
 */
public class PlanitConnectoidFeatureTypeContext<C extends Connectoid> extends PlanitEntityFeatureTypeContext<C> {

  /* takes an access zone and its access modes and convert it to a string of comma separated 'zone:mode'
    entries for persistence */
  protected static Function<Collection<Mode>, String> accessModesToStringConversionFunc(
      final NetworkIdMapper networkIdMapper){
    return (accessModes) -> accessModes.stream().map(
            m -> networkIdMapper.getModeIdMapper().apply(m)).collect(Collectors.joining(","));
  }

  /**
   * Based on provided zone id mapper construct zone lengths in comma separate list
   *
   * @param zoneIdMapper to use
   * @return string of 'zone:[type:length,type:length,..]'
   * @param <Z> type of zone
   */
  protected static <Z extends Zone> BiFunction<Z, Connectoid, String> accessZoneLengthsStrFunc(
      final Function<Z, String> zoneIdMapper){
    /* 'zone:[type:length,type:length,..]' */
    return (accessZone, connectoid) ->
        String.join(":",
            zoneIdMapper.apply(accessZone),
            "[" + connectoid.getAccessZoneEntriesByType(accessZone).values().stream().map( entry ->
                String.join(":",
                    entry.getType().toString(),
                    String.valueOf(entry.getLengthKm().get()))
            ).collect(Collectors.joining(",")) + "]"
        );
  }

  /**
   * Based on provided zone id mapper construct access zone modes in comma separate list
   *
   * @param zoneIdMapper to use
   * @return string of 'zone:[type:(mode1,mode2,...),..]'
   * @param <Z> type of zone
   */
  protected static <Z extends Zone> BiFunction<Z, Connectoid, String> accessZoneModeStrFunc(
      final Function<Z, String> zoneIdMapper,
      final Function<Collection<Mode>, String> modeStrMapper){
    /* 'zone:[type:(mode1,mode2,...),..]' */
    return (accessZone, connectoid) ->
        String.join(":",
            zoneIdMapper.apply(accessZone),
            "[" + connectoid.getAccessZoneEntriesByType(accessZone).values().stream().map( entry ->
                String.join(":",
                    entry.getType().toString(),
                    entry.hasExplicitlyAllowedModes() ?
                        "(" + modeStrMapper.apply(entry.getExplicitlyAllowedModes()) + ")" : "ALL")
            ).collect(Collectors.joining(",")) + "]"
        );
  }

  /**
   * The mapping from PLANit connectoid base GIS attributes (without geometry to allow for addition of other
   * attributes until adding geometry later via derived class using
   * {@link #createGeometryFeatureDescription(MathTransform)}
   *
   * @param <CC> Connectoid type
   * @param zoningIdMapper to apply
   * @param networkIdMapper to apply
   * @return feature mapping
   */
  protected static <CC extends Connectoid> List<Triple<String,String, Function<CC, ?>>>
  createBaseFeatureDescription(
      final ZoningIdMapper zoningIdMapper, final NetworkIdMapper networkIdMapper){

    return List.of(
        Triple.of("mapped_id", "String", c -> zoningIdMapper.getConnectoidIdMapper().apply(c)),
        Triple.of("id", "java.lang.Long", CC::getId),
        Triple.of("xml_id", "String", CC::getXmlId),
        Triple.of("ext_id", "String", CC::getExternalId),
        Triple.of("name", "String", CC::getName),
        Triple.of("phys_node", "String",
                c -> networkIdMapper.getVertexIdMapper().apply(c.getReferenceVertex()))
        );
  }

  /**
   * The mapping from PLANIT connectoid to its geometry attribute
   *
   * @param destinationCrsTransformer to use (may be null)
   * @return feature mapping entry created
   */
  protected Triple<String, String, Function<C, ?>> createGeometryFeatureDescription(
      final MathTransform destinationCrsTransformer){
    return Triple.of(DEFAULT_GEOMETRY_ATTRIBUTE_KEY,"Point",
        c -> PlanitJtsUtils.transformGeometrySafe(c.getReferenceVertex().getPosition(), destinationCrsTransformer));
  }

  /**
   * Constructor
   *
   * @param connectoidClass this context represents
   * @param zoningIdMapper id mapper to apply
   * @param networkIdMapper id mapper of parent physical network to apply
   */
  protected PlanitConnectoidFeatureTypeContext(
      Class<C> connectoidClass, final ZoningIdMapper zoningIdMapper, final NetworkIdMapper networkIdMapper){
    super(connectoidClass, createBaseFeatureDescription(zoningIdMapper, networkIdMapper));
  }


}
