package org.goplanit.geoio.converter.network.featurecontext;

import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.physical.Node;
import org.locationtech.jts.geom.LineString;

import java.util.List;
import java.util.function.Function;

/**
 * Track contextual relevant information for PLANit link segment type that is persisted
 *
 * @author markr
 */
public class PlanitLinkSegmentFeatureTypeContext extends PlanitEntityFeatureTypeContext<MacroscopicLinkSegment> {


  /**
   * The mapping from PLANIT link instance to GIS attributes
   *
   * @param linkSegmentIdMapper to apply
   * @param linkIdMapper to use
   * @param nodeIdMapper to use
   * @param linkSegmentTypeIdMapper to use
   * @return feature mapping
   */
  private static List<Triple<String,String, Function<MacroscopicLinkSegment, ? extends Object>>> createFeatureDescription(
          final Function<MacroscopicLinkSegment, String> linkSegmentIdMapper,
          final Function<MacroscopicLink, String> linkIdMapper,
          final Function<Node, String> nodeIdMapper,
          Function<MacroscopicLinkSegmentType, String> linkSegmentTypeIdMapper){
    return List.of(
            Triple.of("mapped_id", "java.lang.String", linkSegmentIdMapper),
            Triple.of("id", "java.lang.Long", MacroscopicLinkSegment::getId),
            Triple.of("linksegment_id", "java.lang.Long", MacroscopicLinkSegment::getLinkSegmentId),
            Triple.of("xml_id", "String", MacroscopicLinkSegment::getXmlId),
            Triple.of("ext_id", "String", MacroscopicLinkSegment::getExternalId),
            Triple.of("parent_id", "String", ls -> linkIdMapper.apply(ls.getParentLink())),

            Triple.of("type_id", "String", ls -> linkSegmentTypeIdMapper.apply(ls.getLinkSegmentType())),
            // todo: add all contents of type

            //todo: add other link segment contents + activate on writer

            Triple.of("node_up", "String", ls -> nodeIdMapper.apply(ls.getUpstreamNode())),
            Triple.of("node_down", "String", ls -> nodeIdMapper.apply(ls.getDownstreamNode())),
            Triple.of(DEFAULT_GEOMETRY_ATTRIBUTE_KEY, "LineString", /* taken from parent link */
                    (Function<MacroscopicLinkSegment, LineString>) ls -> ls.getParentLink().getGeometry()));
  }

  /**
   * Constructor
   *
   * @param linkSegmentIdMapper id mapper to apply
   * @param linkIdMapper id mapper to apply for link id attributes
   * @param nodeIdMapper id mapper to apply for attributes requiring node ids
   * @param linkSegmentTypeIdMapper id mapper to apply for attributes requiring link segment type ids
   */
  protected PlanitLinkSegmentFeatureTypeContext(
          Function<MacroscopicLinkSegment, String> linkSegmentIdMapper,
          Function<MacroscopicLink, String> linkIdMapper,
          Function<Node, String> nodeIdMapper,
          Function<MacroscopicLinkSegmentType, String> linkSegmentTypeIdMapper){
    super(MacroscopicLinkSegment.class,
            createFeatureDescription( linkSegmentIdMapper, linkIdMapper, nodeIdMapper, linkSegmentTypeIdMapper));
  }

  /**
   * Factory method
   *
   * @param linkSegmentIdMapper to apply for creating each link's unique id when persisting
   * @param linkIdMapper to apply for creating link id references
   * @param nodeIdMapper to apply for creating node id references
   * @param linkSegmentTypeIdMapper to apply for creating link segment type id references
   * @return created instance
   */
  public static PlanitLinkSegmentFeatureTypeContext create(
          Function<MacroscopicLinkSegment, String> linkSegmentIdMapper,
          Function<Link, String> linkIdMapper,
          Function<Vertex, String> nodeIdMapper,
          Function<MacroscopicLinkSegmentType, String> linkSegmentTypeIdMapper){
    return new PlanitLinkSegmentFeatureTypeContext(
            linkSegmentIdMapper,
            l -> linkIdMapper.apply(l) /* convert to link as type */,
            n -> nodeIdMapper.apply(n) /* convert to node as type */,
            linkSegmentTypeIdMapper);
  }

}
