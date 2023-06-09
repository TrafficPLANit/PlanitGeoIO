package org.goplanit.geoio.converter.zoning.featurecontext;

import org.goplanit.converter.idmapping.VirtualNetworkIdMapper;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.virtual.ConnectoidEdge;
import org.locationtech.jts.geom.LineString;

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
   * @return feature mapping
   */
  private static List<Triple<String,String, Function<ConnectoidEdge, ? extends Object>>> createFeatureDescription(
      VirtualNetworkIdMapper virtualNetworkIdMapper){
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
            Triple.of(DEFAULT_GEOMETRY_ATTRIBUTE_KEY, "LineString", (Function<ConnectoidEdge, LineString>) ConnectoidEdge::getGeometry));
  }

  /**
   * Constructor
   *
   * @param virtualNetworkIdMapper id mapper to apply
   */
  protected PlanitConnectoidEdgeFeatureTypeContext(VirtualNetworkIdMapper virtualNetworkIdMapper){
    super(ConnectoidEdge.class, createFeatureDescription(virtualNetworkIdMapper));
  }

  /**
   * Factory method
   *
   * @param virtualNetworkIdMapper to apply for creating each connectoid edge's unique id when persisting
   * @return created instance
   */
  public static PlanitConnectoidEdgeFeatureTypeContext create(VirtualNetworkIdMapper virtualNetworkIdMapper){
    return new PlanitConnectoidEdgeFeatureTypeContext(virtualNetworkIdMapper);
  }

}
