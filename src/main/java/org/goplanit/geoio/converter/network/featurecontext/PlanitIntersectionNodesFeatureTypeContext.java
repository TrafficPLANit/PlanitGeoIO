package org.goplanit.geoio.converter.network.featurecontext;

import org.geotools.api.referencing.operation.MathTransform;
import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionUtils;
import org.goplanit.utils.network.layer.physical.Node;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Track contextual relevant information for PLANit intersection type that is persisted, one entry per intersection.
 * The geometry holds a point per member node with a position, and the node ids list those nodes in the same order, so
 * the k-th id is the node of the k-th point. The lists of approach and internal segments hold their mapped ids, sorted
 * as text and joined with commas.
 *
 * @author markr
 */
public class PlanitIntersectionNodesFeatureTypeContext extends PlanitEntityFeatureTypeContext<Intersection> {

  /**
   * The member nodes of an intersection that have a position, in the order of its member nodes: those its multi point
   * holds a point for, see {@link #createMemberNodesMultiPoint}
   *
   * @param intersection to use
   * @return the member nodes with a position
   */
  private static List<Node> findMemberNodesWithPosition(Intersection intersection){
    return intersection.getMemberNodes().stream().filter(Node::hasPosition).collect(Collectors.toList());
  }

  /**
   * Create the multi point of the positions of the member nodes. Member nodes without a position are left out.
   *
   * @param intersection to use
   * @return multi point of the member node positions
   */
  public static MultiPoint createMemberNodesMultiPoint(Intersection intersection){
    return PlanitJtsUtils.createMultiPoint(
        findMemberNodesWithPosition(intersection).stream().map(Node::getPosition).toArray(Point[]::new));
  }

  /**
   * The mapping from PLANit intersection instance to GIS attributes
   *
   * @param networkIdMapper to apply
   * @param destinationCrsTransformer to use (may be null)
   * @return feature mapping
   */
  private static List<Triple<String,String, Function<Intersection, ?>>> createFeatureDescription(
      final NetworkIdMapper networkIdMapper, final MathTransform destinationCrsTransformer){
    var features = new ArrayList<Triple<String,String, Function<Intersection, ?>>>();
    features.add(Triple.of("isect_id", "String", networkIdMapper.getIntersectionIdMapper()));
    features.add(Triple.of("xml_id", "String", i -> i.hasXmlId() ? i.getXmlId() : ""));
    features.add(Triple.of("ext_id", "String", i -> i.hasExternalId() ? i.getExternalId() : ""));
    features.add(Triple.of("control", "String", i -> i.getControlType().value()));

    /* one column per kind of intersection */
    for(var type : IntersectionType.values()){
      features.add(Triple.of(type.value(), "Boolean", i -> i.getTypes().contains(type)));
    }

    /* in the order of the points of the geometry */
    features.add(Triple.of("node_ids", "String", i -> findMemberNodesWithPosition(i).stream()
        .map(networkIdMapper.getVertexIdMapper()).collect(Collectors.joining(","))));
    features.add(Triple.of("apprch_ids", "String",
        i -> StringUtils.toSortedCommaSeparatedString(
            i.getApproachSegments(), networkIdMapper.getLinkSegmentIdMapper())));
    features.add(Triple.of("intrnl_ids", "String",
        i -> StringUtils.toSortedCommaSeparatedString(
            IntersectionUtils.getInternalSegmentsOf(i), networkIdMapper.getLinkSegmentIdMapper())));
    features.add(Triple.of("node_count", "Integer", i -> i.getMemberNodes().size()));
    features.add(Triple.of("appr_count", "Integer", i -> i.getApproachSegments().size()));

    /* geometry last to append srid */
    features.add(Triple.of(DEFAULT_GEOMETRY_ATTRIBUTE_KEY, "MultiPoint",
        i -> PlanitJtsUtils.transformGeometrySafe(createMemberNodesMultiPoint(i), destinationCrsTransformer)));
    return features;
  }

  /**
   * Constructor
   *
   * @param networkIdMapper id mapper to apply
   * @param destinationCrsTransformer to use (may be null)
   */
  protected PlanitIntersectionNodesFeatureTypeContext(
      final NetworkIdMapper networkIdMapper, final MathTransform destinationCrsTransformer){
    super(Intersection.class, createFeatureDescription(networkIdMapper, destinationCrsTransformer));
  }

  /**
   * Factory method
   *
   * @param networkIdMapper to apply for creating the ids of the intersection and its member nodes and segments
   * @param destinationCrsTransformer to use (may be null)
   * @return created instance
   */
  public static PlanitIntersectionNodesFeatureTypeContext create(
      final NetworkIdMapper networkIdMapper, final MathTransform destinationCrsTransformer){
    return new PlanitIntersectionNodesFeatureTypeContext(networkIdMapper, destinationCrsTransformer);
  }

}
