package org.goplanit.geoio.converter.zoning.featurecontext;

import org.goplanit.converter.idmapping.VirtualNetworkIdMapper;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.network.virtual.ConnectoidEdge;
import org.goplanit.utils.network.virtual.ConnectoidSegment;
import org.opengis.referencing.operation.MathTransform;

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
   * @param destinationCrsTransformer to use (may be null)
   * @return feature mapping
   */
  private static List<Triple<String,String, Function<ConnectoidSegment, ?>>> createFixedFeatureDescription(
      final VirtualNetworkIdMapper virtualNetworkIdMapper,
      final MathTransform destinationCrsTransformer){
    return List.of(
            /* link segment info (fixed) */
            Triple.of("mapped_id", "java.lang.String", virtualNetworkIdMapper.getConnectoidSegmentIdMapper()),
            Triple.of("id", "java.lang.Long", ConnectoidSegment::getId),
            Triple.of("segment_id", "java.lang.Long", ConnectoidSegment::getConnectoidSegmentId),
            Triple.of("xml_id", "String", ConnectoidSegment::getXmlId),
            Triple.of("ext_id", "String", ConnectoidSegment::getExternalId),
            Triple.of("parent_id", "String", cs -> virtualNetworkIdMapper.getConnectoidEdgeIdMapper().apply((ConnectoidEdge) cs.getParent())),
            Triple.of("cap_pcuh", "Float", ConnectoidSegment::getCapacityOrDefaultPcuH),    /* max flow in pcu per hour across all lanes */
            Triple.of("geom_opp", "Boolean", cs -> !cs.isParentGeometryInSegmentDirection(true)),     /* does geometry run in opposite direction to travel direction */
            Triple.of("vertx_up", "String", cs -> virtualNetworkIdMapper.getVertexIdMapper().apply(cs.getUpstreamVertex())),
            Triple.of("vertx_down", "String", cs -> virtualNetworkIdMapper.getVertexIdMapper().apply(cs.getDownstreamVertex())),

            /* geometry taken from parent link */
            Triple.of(DEFAULT_GEOMETRY_ATTRIBUTE_KEY, "LineString",
                    cs -> PlanitJtsUtils.transformGeometrySafe(cs.getParent().getGeometry(), destinationCrsTransformer)));
  }

  /**
   * The mapping from PLANIT link instance to GIS attributes
   *
   * @param virtualNetworkIdMapper to apply
   * @param destinationCrsTransformer to use (may be null)
   * @return feature mapping
   */
  private static List<Triple<String,String, Function<ConnectoidSegment, ?>>> createFeatureDescription(
      final VirtualNetworkIdMapper virtualNetworkIdMapper,
      final MathTransform destinationCrsTransformer){
    return createFixedFeatureDescription(virtualNetworkIdMapper, destinationCrsTransformer);
  }

  /**
   * Constructor
   *
   * @param virtualNetworkIdMapper id mapper to apply
   * @param destinationCrsTransformer to use (may be null)
   */
  protected PlanitConnectoidSegmentFeatureTypeContext(
      final VirtualNetworkIdMapper virtualNetworkIdMapper,
      final MathTransform destinationCrsTransformer){
    super(ConnectoidSegment.class, createFeatureDescription(virtualNetworkIdMapper, destinationCrsTransformer));
  }

  /**
   * Factory method
   *
   * @param virtualNetworkIdMapper to apply for creating ids when persisting
   * @param destinationCrsTransformer to use (may be null)
   * @return created instance
   */
  public static PlanitConnectoidSegmentFeatureTypeContext create(
      final VirtualNetworkIdMapper virtualNetworkIdMapper,
      final MathTransform destinationCrsTransformer){
    return new PlanitConnectoidSegmentFeatureTypeContext(virtualNetworkIdMapper, destinationCrsTransformer);
  }

}
