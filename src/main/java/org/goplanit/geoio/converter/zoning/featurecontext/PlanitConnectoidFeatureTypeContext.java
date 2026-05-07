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

  /**
   * The mapping from PLANIT connectoid base GIS attributes (without geometry to allow for addition of other
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

    /* take access zone and its access modes and convert it to a string of comma separated 'zone:mode'
    entries for persistence */
    final Function<Collection<Mode>, String> accessModes2String =
        (accessModes) -> accessModes.stream().map(
              m -> networkIdMapper.getModeIdMapper().apply(m)).collect(Collectors.joining(","));

    /* 'zone:[type:length,type:length,..]' */
    final BiFunction<Zone, Connectoid, String> accessZoneLengthString =
        (accessZone, connectoid) ->
            String.join(":",
                zoningIdMapper.getZoneIdMapper().apply(accessZone),
                "[" + connectoid.getAccessZoneEntriesByType(accessZone).values().stream().map( entry ->
                    String.join(":",
                        entry.getType().toString(),
                        String.valueOf(entry.getLengthKm().get()))
                ).collect(Collectors.joining(",")) + "]"
            );

    /* 'zone:[type:(mode1,mode2,...),..]' */
    final BiFunction<Zone, Connectoid, String> accessZoneModesString =
        (accessZone, connectoid) ->
            String.join(":",
                zoningIdMapper.getZoneIdMapper().apply(accessZone),
                "[" + connectoid.getAccessZoneEntriesByType(accessZone).values().stream().map( entry ->
                    String.join(":",
                        entry.getType().toString(),
                        entry.hasExplicitlyAllowedModes() ?
                            "(" + accessModes2String.apply(entry.getExplicitlyAllowedModes()) + ")" : "ALL")
                ).collect(Collectors.joining(",")) + "]"
            );

    return List.of(
        Triple.of("mapped_id", "String", c -> zoningIdMapper.getConnectoidIdMapper().apply(c)),
        Triple.of("id", "java.lang.Long", CC::getId),
        Triple.of("xml_id", "String", CC::getXmlId),
        Triple.of("ext_id", "String", CC::getExternalId),
        Triple.of("name", "String", CC::getName),
        Triple.of("phys_node", "String",
                c -> networkIdMapper.getVertexIdMapper().apply(c.getReferenceVertex())),
        // todo: improve as this is now a simplification compared to PLANit data
        Triple.of("zones", "String",
                c -> c.getAccessZoneStream().map(
                        z -> zoningIdMapper.getZoneIdMapper().apply(z)).collect(
                            Collectors.joining(","))),
        // zone:[type:modes,type:modes]
        Triple.of("modes", "String",
            c -> c.getAccessZoneStream().map(z -> accessZoneModesString.apply(z, c)).collect(
            Collectors.joining(","))),
        Triple.of("lengths_km", "String",c -> c.getAccessZoneStream().map( z ->
            accessZoneLengthString.apply(z, c)).collect(
            Collectors.joining(",")))
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
