package org.goplanit.geoio.converter.network.featurecontext;

import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.physical.Node;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import java.util.List;
import java.util.function.Function;

/**
 * Track contextual relevant information for PLANit link type that is persisted
 *
 * @author markr
 */
public class PlanitLinkFeatureTypeContext extends PlanitEntityFeatureTypeContext<MacroscopicLink> {


  /**
   * The mapping from PLANIT link instance to GIS attributes
   *
   * @param linkIdMapper to apply
   * @return feature mapping
   */
  private static List<Triple<String,String, Function<MacroscopicLink, ? extends Object>>> createFeatureDescription(
          Function<MacroscopicLink, String> linkIdMapper,
          Function<Node, String> nodeIdMapper){
    return List.of(
            Triple.of("mapped_id", "java.lang.String", linkIdMapper),
            Triple.of("id", "java.lang.Long", Link::getId),
            Triple.of("link_id", "java.lang.Long", Link::getLinkId),
            Triple.of("xml_id", "String", Link::getXmlId),
            Triple.of("ext_id", "String", Link::getExternalId),
            Triple.of("name", "String", (Function<MacroscopicLink, String>) MacroscopicLink::getName),
            Triple.of("length_km", "java.lang.Double", Link::getLengthKm),
            Triple.of("node_a", "String", l -> nodeIdMapper.apply(l.getNodeA())),
            Triple.of("node_b", "String", l -> nodeIdMapper.apply(l.getNodeB())),
            Triple.of(DEFAULT_GEOMETRY_ATTRIBUTE_KEY, "LineString", (Function<MacroscopicLink, LineString>) MacroscopicLink::getGeometry));
  }

  /**
   * Constructor
   *
   * @param linkIdMapper id mapper to apply
   * @param nodeIdMapper id mapper to apply for attributes requiring node ids
   */
  protected PlanitLinkFeatureTypeContext(Function<MacroscopicLink, String> linkIdMapper, Function<Node, String> nodeIdMapper){
    super(MacroscopicLink.class, createFeatureDescription(linkIdMapper, nodeIdMapper));
  }

  /**
   * Factory method
   *
   * @param linkIdMapper to apply for creating each link's unique id when persisting
   * @param nodeIdMapper to apply for creating node id references
   * @return created instance
   */
  public static PlanitLinkFeatureTypeContext create(Function<Link, String> linkIdMapper, Function<Vertex, String> nodeIdMapper){
    return new PlanitLinkFeatureTypeContext(
            l -> linkIdMapper.apply(l) /* convert to link as type */,
            n -> nodeIdMapper.apply(n) /* convert to node as type */);
  }

}
