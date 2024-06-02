package org.goplanit.geoio.converter.zoning.featurecontext;

import org.goplanit.converter.idmapping.VirtualNetworkIdMapper;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.network.virtual.ConnectoidEdge;
import org.opengis.referencing.operation.MathTransform;

import java.util.List;
import java.util.function.Function;

/**
 * Track contextual relevant information for PLANit connectoid edge type that is persisted
 *
 * @author markr
 */
public class PlanitConnectoidEdgeFeatureTypeContext extends PlanitEntityFeatureTypeContext<ConnectoidEdge> {


  /**
   * The mapping from PLANIT connectoid edge instance to GIS attributes
   *
   * @param virtualNetworkIdMapper to apply
   * @param destinationCrsTransformer to use (may be null)
   * @return feature mapping
   */
  private static List<Triple<String,String, Function<ConnectoidEdge, ?>>> createFeatureDescription(
      VirtualNetworkIdMapper virtualNetworkIdMapper,
      final MathTransform destinationCrsTransformer){
    return List.of(
            Triple.of("mapped_id", "java.lang.String", virtualNetworkIdMapper.getConnectoidEdgeIdMapper()),
            Triple.of("id", "java.lang.Long", ConnectoidEdge::getId),
            Triple.of("link_id", "java.lang.Long", ConnectoidEdge::getConnectoidEdgeId),
            Triple.of("xml_id", "String", ConnectoidEdge::getXmlId),
            Triple.of("ext_id", "String", ConnectoidEdge::getExternalId),
            Triple.of("name", "String", ConnectoidEdge::getName),
            Triple.of("length_km", "java.lang.Double", ConnectoidEdge::getLengthKm),
            Triple.of("node_a", "String", l -> virtualNetworkIdMapper.getVertexIdMapper().apply(l.getVertexA())),
            Triple.of("node_b", "String", l -> virtualNetworkIdMapper.getVertexIdMapper().apply(l.getVertexB())),
            Triple.of(DEFAULT_GEOMETRY_ATTRIBUTE_KEY, "LineString",
                ce -> PlanitJtsUtils.transformGeometrySafe(ce.getGeometry(), destinationCrsTransformer)));
  }

  /**
   * Constructor
   *
   * @param virtualNetworkIdMapper id mapper to apply
   * @param destinationCrsTransformer to use (may be null)
   */
  protected PlanitConnectoidEdgeFeatureTypeContext(
      VirtualNetworkIdMapper virtualNetworkIdMapper,
      final MathTransform destinationCrsTransformer){
    super(ConnectoidEdge.class, createFeatureDescription(virtualNetworkIdMapper, destinationCrsTransformer));
  }

  /**
   * Factory method
   *
   * @param virtualNetworkIdMapper to apply for creating each connectoid edge's unique id when persisting
   * @param destinationCrsTransformer to use (may be null)
   * @return created instance
   */
  public static PlanitConnectoidEdgeFeatureTypeContext create(
      VirtualNetworkIdMapper virtualNetworkIdMapper,
      final MathTransform destinationCrsTransformer){
    return new PlanitConnectoidEdgeFeatureTypeContext(virtualNetworkIdMapper, destinationCrsTransformer);
  }

}
