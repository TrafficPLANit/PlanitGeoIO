package org.goplanit.geoio.converter.zoning.featurecontext;

import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.converter.idmapping.ZoningIdMapper;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.utils.misc.IterableUtils;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.zoning.Connectoid;
import org.goplanit.utils.zoning.UndirectedConnectoid;
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
   * The mapping from PLANIT connectoid base GIS attributes (without geometry to allow for addition of other attributes until adding
   * geometry later via derived class using {@link #createGeometryFeatureDescription()}
   *
   * @param <CC> Connectoid type
   * @param zoningIdMapper to apply
   * @param networkIdMapper to apply
   * @return feature mapping
   */
  protected static <CC extends Connectoid> List<Triple<String,String, Function<CC, ? extends Object>>> createBaseFeatureDescription(
      final ZoningIdMapper zoningIdMapper, final NetworkIdMapper networkIdMapper){

    /** take access zone and its access modes and convert it to a string of comma separated 'zone:mode' entries for persistence */
    final BiFunction<Zone, Collection<Mode>, String> accessModesForZone2String = (accessZone, accessModes) ->
        accessModes.stream().map(
            m -> String.join(":", zoningIdMapper.getZoneIdMapper().apply(accessZone),networkIdMapper.getModeIdMapper().apply(m))).collect(Collectors.joining(","));

    /** take access zone and its connectoid combination and convert it to 'zone:length' entry for persistence */
    final BiFunction<Zone, Connectoid, String> accessZoneLengthString = (accessZone, connectoid) ->
        String.join(":", zoningIdMapper.getZoneIdMapper().apply(accessZone), String.format("%.1f",connectoid.getLengthKm(accessZone).orElse(Double.NaN)));

    return List.of(
        Triple.of("mapped_id", "String", c -> zoningIdMapper.getConnectoidIdMapper().apply(c)),
        Triple.of("id", "java.lang.Long", CC::getId),
        Triple.of("xml_id", "String", CC::getXmlId),
        Triple.of("ext_id", "String", CC::getExternalId),
        Triple.of("name", "String", CC::getName),
        Triple.of("phys_node", "String", c -> networkIdMapper.getVertexIdMapper().apply(c.getAccessVertex())),
        Triple.of("zones", "String", c -> c.getAccessZones().stream().map(z -> zoningIdMapper.getZoneIdMapper().apply(z)).collect(Collectors.joining(","))),
        Triple.of("modes", "String",
            c -> IterableUtils.asStream(c).map(accessZone -> c.hasExplicitlyAllowedModes(accessZone) ?
                accessModesForZone2String.apply(accessZone, c.getExplicitlyAllowedModes(accessZone)) :
                /* for implicit modes, all modes are allowed. Note not ideal because we do not yet define anywhere what ALL means */
                String.join(":", zoningIdMapper.getZoneIdMapper().apply(accessZone), "ALL")).collect(Collectors.joining(","))),
        Triple.of("lengths_km", "String",c -> IterableUtils.asStream(c).map(accessZone ->
            accessZoneLengthString.apply(accessZone, c)).collect(Collectors.joining(",")))
        );
  }

  /**
   * The mapping from PLANIT connectoid to its geometry attribute
   *
   * @return feature mapping entry created
   */
  protected Triple<String, String, Function<C, ?>> createGeometryFeatureDescription(){
    return Triple.of(DEFAULT_GEOMETRY_ATTRIBUTE_KEY,"Point", c -> c.getAccessVertex().getPosition());
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
