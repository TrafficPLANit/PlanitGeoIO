package org.goplanit.geoio.converter.zoning.featurecontext;

import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.converter.idmapping.VirtualNetworkIdMapper;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.virtual.ConnectoidEdge;
import org.goplanit.utils.network.virtual.ConnectoidSegment;
import org.locationtech.jts.geom.LineString;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Track contextual relevant information for PLANit connectoid edge segment type that is persisted
 *
 * @author markr
 */
public class PlanitConnectoidSegmentFeatureTypeContext extends PlanitEntityFeatureTypeContext<ConnectoidSegment> {

  /**
   * The mapping from PLANIT connectoid edge segment instance to fixed GIS attributes of connectoid edge segment
   *
   * @param virtualNetworkIdMapper to apply
   * @return feature mapping
   */
  private static List<Triple<String,String, Function<ConnectoidSegment, ? extends Object>>> createFixedFeatureDescription(
      final VirtualNetworkIdMapper virtualNetworkIdMapper){
    return List.of(
            /* link segment info (fixed) */
            Triple.of("mapped_id", "java.lang.String", virtualNetworkIdMapper.getConnectoidSegmentIdMapper()),
            Triple.of("id", "java.lang.Long", ConnectoidSegment::getId),
            Triple.of("segment_id", "java.lang.Long", ConnectoidSegment::getConnectoidSegmentId),
            Triple.of("xml_id", "String", ConnectoidSegment::getXmlId),
            Triple.of("ext_id", "String", ConnectoidSegment::getExternalId),
            Triple.of("parent_id", "String", cs -> virtualNetworkIdMapper.getConnectoidEdgeIdMapper().apply((ConnectoidEdge) cs.getParent())),
            Triple.of("cap_pcuh", "Float", ConnectoidSegment::getCapacityOrDefaultPcuH),    /* max flow in pcu per hour across all lanes */
            Triple.of("geom_opp", "Boolean", cs -> !cs.isParentGeometryInSegmentDirection()),     /* does geometry run in opposite direction to travel direction */
            Triple.of("vertx_up", "String", cs -> virtualNetworkIdMapper.getVertexIdMapper().apply(cs.getUpstreamVertex())),
            Triple.of("vertx_down", "String", cs -> virtualNetworkIdMapper.getVertexIdMapper().apply(cs.getDownstreamVertex())),

            /* geometry taken from parent link */
            Triple.of(DEFAULT_GEOMETRY_ATTRIBUTE_KEY, "LineString",
                    (Function<ConnectoidSegment, LineString>) cs -> cs.getParent().getGeometry()));
  }

  /**
   * The mapping from PLANIT link instance to GIS attributes
   *
   * @param virtualNetworkIdMapper to apply
   * @return feature mapping
   */
  private static List<Triple<String,String, Function<ConnectoidSegment, ? extends Object>>> createFeatureDescription(
      final VirtualNetworkIdMapper virtualNetworkIdMapper){
    return createFixedFeatureDescription(virtualNetworkIdMapper);
  }

  /**
   * Constructor
   *
   * @param virtualNetworkIdMapper id mapper to apply
   */
  protected PlanitConnectoidSegmentFeatureTypeContext(final VirtualNetworkIdMapper virtualNetworkIdMapper){
    super(ConnectoidSegment.class, createFeatureDescription(virtualNetworkIdMapper));
  }

  /**
   * Factory method
   *
   * @param virtualNetworkIdMapper to apply for creating ids when persisting
   * @return created instance
   */
  public static PlanitConnectoidSegmentFeatureTypeContext create(final VirtualNetworkIdMapper virtualNetworkIdMapper){
    return new PlanitConnectoidSegmentFeatureTypeContext(virtualNetworkIdMapper);
  }

}
