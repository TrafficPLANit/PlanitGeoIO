package org.goplanit.geoio.converter.network.featurecontext;

import org.geotools.api.referencing.operation.MathTransform;
import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.graph.directed.BannedMovement;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersections;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Track contextual relevant information for each approach and internal segment of a PLANit intersection that is
 * persisted. Both end at a member node, so the intersection of a segment is the one holding its downstream node.
 * Control and kinds are copied from that intersection. The geometry runs in the direction of travel, so an approach
 * ends at its member node.
 *
 * @author markr
 */
public class PlanitIntersectionSegmentFeatureTypeContext
    extends PlanitEntityFeatureTypeContext<MacroscopicLinkSegment> {

  /**
   * Find the intersection of an approach or internal segment: the one holding its downstream node as a member node
   *
   * @param intersections to search
   * @param segment approach or internal segment
   * @return intersection of the segment
   */
  private static Intersection findIntersection(Intersections intersections, MacroscopicLinkSegment segment){
    return intersections.getByMemberNode(segment.getDownstreamNode());
  }

  /**
   * Create the sorted comma-joined mapped ids of the link segments the banned movements of an approach go to
   *
   * @param segment approach or internal segment
   * @param bansByApproachSegment banned movements per approach segment, by identity
   * @param networkIdMapper to apply
   * @return mapped ids of the banned to segments, empty for internal segments and approaches without bans
   */
  private static String createBannedToSegmentIds(
      MacroscopicLinkSegment segment,
      Map<MacroscopicLinkSegment, List<BannedMovement>> bansByApproachSegment,
      NetworkIdMapper networkIdMapper){
    /* internal segments are not in the map */
    var bans = bansByApproachSegment.get(segment);
    if(bans == null){
      return "";
    }
    return StringUtils.toSortedCommaSeparatedString(bans,
        ban -> networkIdMapper.getMacroscopicLinkSegmentIdMapper().apply((MacroscopicLinkSegment) ban.getSegmentTo()));
  }

  /**
   * The mapping from approach or internal segment instance to GIS attributes
   *
   * @param networkIdMapper to apply
   * @param intersections the segments belong to
   * @param bansByApproachSegment banned movements per approach segment, by identity
   * @param destinationCrsTransformer to use (may be null)
   * @return feature mapping
   */
  private static List<Triple<String,String, Function<MacroscopicLinkSegment, ?>>> createFeatureDescription(
      final NetworkIdMapper networkIdMapper,
      final Intersections intersections,
      final Map<MacroscopicLinkSegment, List<BannedMovement>> bansByApproachSegment,
      final MathTransform destinationCrsTransformer){
    var features = new ArrayList<Triple<String,String, Function<MacroscopicLinkSegment, ?>>>();
    features.add(Triple.of("isect_id", "String",
        s -> networkIdMapper.getIntersectionIdMapper().apply(findIntersection(intersections, s))));
    features.add(Triple.of("approach", "Boolean",
        s -> findIntersection(intersections, s).getApproachSegment(s.getId()) != null));
    features.add(Triple.of("seg_id", "String", s -> networkIdMapper.getMacroscopicLinkSegmentIdMapper().apply(s)));
    features.add(Triple.of("node_up", "String",
        s -> networkIdMapper.getVertexIdMapper().apply(s.getUpstreamNode())));
    features.add(Triple.of("node_down", "String",
        s -> networkIdMapper.getVertexIdMapper().apply(s.getDownstreamNode())));
    features.add(Triple.of("control", "String", s -> findIntersection(intersections, s).getControlType().value()));

    /* one column per kind of intersection */
    for(var type : IntersectionType.values()){
      features.add(Triple.of(
          type.value(), "Boolean", s -> findIntersection(intersections, s).getTypes().contains(type)));
    }

    features.add(Triple.of("banned_to", "String",
        s -> createBannedToSegmentIds(s, bansByApproachSegment, networkIdMapper)));

    /* geometry last to append srid */
    features.add(Triple.of(DEFAULT_GEOMETRY_ATTRIBUTE_KEY, "LineString",
        s -> PlanitJtsUtils.transformGeometrySafe(
            PlanitMacroscopicLinkSegmentFeatureTypeContext.createOrGetLinkSegmentGeometryInTravelDirection(s),
            destinationCrsTransformer)));
    return features;
  }

  /**
   * Constructor
   *
   * @param networkIdMapper id mapper to apply
   * @param intersections the segments belong to
   * @param bansByApproachSegment banned movements per approach segment, by identity
   * @param destinationCrsTransformer to use (may be null)
   */
  protected PlanitIntersectionSegmentFeatureTypeContext(
      final NetworkIdMapper networkIdMapper,
      final Intersections intersections,
      final Map<MacroscopicLinkSegment, List<BannedMovement>> bansByApproachSegment,
      final MathTransform destinationCrsTransformer){
    super(MacroscopicLinkSegment.class,
        createFeatureDescription(networkIdMapper, intersections, bansByApproachSegment, destinationCrsTransformer));
  }

  /**
   * Factory method
   *
   * @param networkIdMapper to apply for creating the ids of intersections, link segments and nodes
   * @param intersections the segments belong to
   * @param bansByApproachSegment banned movements per approach segment, by identity
   * @param destinationCrsTransformer to use (may be null)
   * @return created instance
   */
  public static PlanitIntersectionSegmentFeatureTypeContext create(
      final NetworkIdMapper networkIdMapper,
      final Intersections intersections,
      final Map<MacroscopicLinkSegment, List<BannedMovement>> bansByApproachSegment,
      final MathTransform destinationCrsTransformer){
    return new PlanitIntersectionSegmentFeatureTypeContext(
        networkIdMapper, intersections, bansByApproachSegment, destinationCrsTransformer);
  }

}
