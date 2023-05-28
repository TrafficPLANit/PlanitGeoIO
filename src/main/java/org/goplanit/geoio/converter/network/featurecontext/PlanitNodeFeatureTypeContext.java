package org.goplanit.geoio.converter.network.featurecontext;

import org.goplanit.geoio.util.GeoIoFeatureTypeBuilder;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.physical.Node;
import org.locationtech.jts.geom.Point;

import java.util.List;
import java.util.function.Function;

/**
 * Track contextual relevant information for PLANit node type that is persisted
 *
 * @author markr
 */
public class PlanitNodeFeatureTypeContext extends PlanitEntityFeatureTypeContext<Node> {


  /**
   * The mapping from PLANIT link instance to GIS attributes
   *
   * @param nodeIdMapper to apply
   * @return feature mapping
   */
  private static List<Triple<String,String, Function<Node, ? extends Object>>> createFeatureDescription(Function<Node, String> nodeIdMapper){
    return List.of(
            Triple.of("mapped_id", "String", nodeIdMapper),
            Triple.of("id", "java.lang.Long", Node::getId),
            Triple.of("node_id", "java.lang.Long", Node::getNodeId),
            Triple.of("xml_id", "String", Node::getXmlId),
            Triple.of("ext_id", "String", Node::getExternalId),
            Triple.of("name", "String", (Function<Node, String>) Node::getName),
            Triple.of(DEFAULT_GEOMETRY_ATTRIBUTE_KEY, "Point", (Function<Node, Point>) Node::getPosition));
  }

  /**
   * Constructor
   *
   * @param nodeIdMapper id mapper to apply
   */
  protected PlanitNodeFeatureTypeContext(Function<Node, String> nodeIdMapper){
    super(Node.class, createFeatureDescription(nodeIdMapper));
  }

  /**
   * Factory method
   *
   * @param nodeIdMapper to apply for creating each node's unique id when persisting
   * @return created instance
   */
  public static PlanitNodeFeatureTypeContext create(Function<Vertex, String> nodeIdMapper){
    return new PlanitNodeFeatureTypeContext(nodeIdMapper::apply /* convert to node as type */);
  }

}
