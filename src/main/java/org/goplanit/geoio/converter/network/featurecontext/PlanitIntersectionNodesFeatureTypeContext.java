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
import java.util.Objects;
import java.util.function.Function;

/**
 * Track contextual relevant information for PLANit intersection type that is persisted. Lists of member nodes and
 * segments hold their mapped ids, sorted as text and joined with commas. The geometry holds the positions of the
 * member nodes.
 *
 * @author markr
 */
public class PlanitIntersectionNodesFeatureTypeContext extends PlanitEntityFeatureTypeContext<Intersection> {

  /**
   * Create the multi point of the positions of the member nodes. Member nodes without a position are left out.
   *
   * @param intersection to use
   * @return multi point of the member node positions
   */
  public static MultiPoint createMemberNodesMultiPoint(Intersection intersection){
    return PlanitJtsUtils.createMultiPoint(intersection.getMemberNodes().stream().map(Node::getPosition).filter(
        Objects::nonNull).toArray(Point[]::new));
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
    features.add(Triple.of("mapped_id", "String", networkIdMapper.getIntersectionIdMapper()));
    features.add(Triple.of("id", "java.lang.Long", Intersection::getId));
    features.add(Triple.of("xml_id", "String", i -> i.hasXmlId() ? i.getXmlId() : ""));
    features.add(Triple.of("ext_id", "String", i -> i.hasExternalId() ? i.getExternalId() : ""));
    features.add(Triple.of("control", "String", i -> i.getControlType().value()));

    /* one column per kind of intersection */
    for(var type : IntersectionType.values()){
      features.add(Triple.of(type.value(), "Boolean", i -> i.getTypes().contains(type)));
    }

    features.add(Triple.of("node_ids", "String",
        i -> StringUtils.toSortedCommaSeparatedString(i.getMemberNodes(), networkIdMapper.getVertexIdMapper())));
    features.add(Triple.of("approaches", "String",
        i -> StringUtils.toSortedCommaSeparatedString(
            i.getApproachSegments(), networkIdMapper.getMacroscopicLinkSegmentIdMapper())));
    features.add(Triple.of("internals", "String",
        i -> StringUtils.toSortedCommaSeparatedString(
            IntersectionUtils.getInternalSegmentsOf(i), networkIdMapper.getMacroscopicLinkSegmentIdMapper())));
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
