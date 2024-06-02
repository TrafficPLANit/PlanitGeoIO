package org.goplanit.geoio.converter.service.featurecontext;

import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.layer.service.ServiceNode;
import org.opengis.referencing.operation.MathTransform;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Track contextual relevant information for PLANit service node type that is persisted
 *
 * @author markr
 */
public class PlanitServiceNodeFeatureTypeContext extends PlanitEntityFeatureTypeContext<ServiceNode> {


  /**
   * The mapping from PLANIT service node instance to GIS attributes
   *
   * @param serviceNodeIdMapper to apply
   * @param parentNodeIdMapper to apply
   * @param destinationCrsTransformer to use (may be null)
   * @return feature mapping
   */
  private static List<Triple<String,String, Function<ServiceNode, ?>>> createFeatureDescription(
      Function<ServiceNode, String> serviceNodeIdMapper,
      Function<Node, String> parentNodeIdMapper,
      final MathTransform destinationCrsTransformer){
    return List.of(
            Triple.of("mapped_id", "String", serviceNodeIdMapper),
            Triple.of("id", "java.lang.Long", ServiceNode::getId),
            Triple.of("xml_id", "String", ServiceNode::getXmlId),
            Triple.of("ext_id", "String", ServiceNode::getExternalId),
            Triple.of("parent", "String",
                (Function<ServiceNode, String>) sn -> sn.getPhysicalParentNodes().stream().map(parentNodeIdMapper).collect(Collectors.joining(","))),
            Triple.of(DEFAULT_GEOMETRY_ATTRIBUTE_KEY, "Point",
                n -> PlanitJtsUtils.transformGeometrySafe(n.getPosition(),destinationCrsTransformer)));
  }

  /**
   * Constructor
   *
   * @param serviceNodeIdMapper id mapper to apply
   * @param parentNodeIdMapper parent node id mapper to apply
   * @param destinationCrsTransformer to use (may be null)
   */
  protected PlanitServiceNodeFeatureTypeContext(
      Function<ServiceNode, String> serviceNodeIdMapper,
      Function<Node, String> parentNodeIdMapper,
      final MathTransform destinationCrsTransformer){
    super(ServiceNode.class, createFeatureDescription(serviceNodeIdMapper, parentNodeIdMapper, destinationCrsTransformer));
  }

  /**
   * Factory method
   *
   * @param serviceNodeIdMapper to apply for creating each service node's unique id when persisting
   * @param parentNodeIdMapper id mapper to use
   * @param destinationCrsTransformer to use (may be null)
   * @return created instance
   */
  public static PlanitServiceNodeFeatureTypeContext create(
      Function<Vertex, String> serviceNodeIdMapper,
      Function<Vertex, String> parentNodeIdMapper,
      final MathTransform destinationCrsTransformer){
    return new PlanitServiceNodeFeatureTypeContext(
        serviceNodeIdMapper::apply /* convert to node as type */,
        parentNodeIdMapper::apply,
        destinationCrsTransformer);
  }

}
