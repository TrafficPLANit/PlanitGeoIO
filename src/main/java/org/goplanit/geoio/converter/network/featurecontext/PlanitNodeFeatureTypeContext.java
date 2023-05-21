package org.goplanit.geoio.converter.network.featurecontext;

import org.goplanit.geoio.util.GeoIoFeatureTypeBuilder;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.misc.Triple;
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


  private static List<Triple<String,String, Function<Node, ? extends Object>>> createFeatureDescription(Function<Node, String> nodeIdMapper){
    return List.of(
            Triple.of("node_id", "java.lang.Long", nodeIdMapper),
            Triple.of("name", "String", (Function<Node, String>) Node::getName),
            Triple.of("*geom", "Point", (Function<Node, Point>) Node::getPosition));
  }

  /**
   * Constructor
   *
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
    return new PlanitNodeFeatureTypeContext(n -> nodeIdMapper.apply(n) /* convert to node as type */);
  }

}
