package org.goplanit.geoio.converter.network.featurecontext;

import org.geotools.api.referencing.operation.MathTransform;
import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.graph.directed.BannedTurn;
import org.goplanit.utils.graph.directed.EdgeSegmentUtils;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.network.layer.macroscopic.intersection.CapacityFactorOrigin;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionSignalSegment;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersections;
import org.goplanit.utils.network.layer.physical.LinkSegment;

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
    extends PlanitEntityFeatureTypeContext<LinkSegment> {

  /**
   * The text of how a value got its value, as written in the origin columns
   *
   * @param origin of the value, null when not set
   * @return the origin's value, empty when not set
   */
  private static String createOriginText(CapacityFactorOrigin origin){
    return origin == null ? "" : origin.value();
  }

  /**
   * Create the sorted comma-joined mapped ids of the link segments the banned turns of an approach go to
   *
   * @param segment approach or internal segment
   * @param bansByApproachSegment banned turns per approach segment, by identity
   * @param networkIdMapper to apply
   * @return mapped ids of the banned to segments, empty for internal segments and approaches without bans
   */
  private static String createBannedToSegmentIds(
      LinkSegment segment,
      Map<LinkSegment, List<BannedTurn>> bansByApproachSegment,
      NetworkIdMapper networkIdMapper){
    /* internal segments are not in the map */
    var bans = bansByApproachSegment.get(segment);
    if(bans == null){
      return "";
    }
    return StringUtils.toSortedCommaSeparatedString(bans,
        ban -> networkIdMapper.getLinkSegmentIdMapper().apply((LinkSegment) ban.getSegmentTo()));
  }

  /**
   * The mapping from approach or internal segment instance to GIS attributes
   *
   * @param networkIdMapper to apply
   * @param intersections the segments belong to
   * @param bansByApproachSegment banned turns per approach segment, by identity
   * @param destinationCrsTransformer to use (may be null)
   * @return feature mapping
   */
  private static List<Triple<String,String, Function<LinkSegment, ?>>> createFeatureDescription(
      final NetworkIdMapper networkIdMapper,
      final Intersections intersections,
      final Map<LinkSegment, List<BannedTurn>> bansByApproachSegment,
      final MathTransform destinationCrsTransformer){
    var features = new ArrayList<Triple<String,String, Function<LinkSegment, ?>>>();
    features.add(Triple.of("isect_id", "String",
        s -> networkIdMapper.getIntersectionIdMapper().apply(intersections.getByDownstreamNodeOf(s))));
    features.add(Triple.of("approach", "Boolean",
        s -> intersections.getByDownstreamNodeOf(s).getApproachSegment(s.getId()) != null));
    features.add(Triple.of("seg_id", "String", s -> networkIdMapper.getLinkSegmentIdMapper().apply(s)));
    features.add(Triple.of("node_up", "String",
        s -> networkIdMapper.getVertexIdMapper().apply(s.getUpstreamNode())));
    features.add(Triple.of("node_down", "String",
        s -> networkIdMapper.getVertexIdMapper().apply(s.getDownstreamNode())));
    features.add(Triple.of("control", "String", s -> intersections.getByDownstreamNodeOf(s).getControlType().value()));

    /* one column per kind of intersection */
    for(var type : IntersectionType.values()){
      features.add(Triple.of(
          type.value(), "Boolean", s -> intersections.getByDownstreamNodeOf(s).getTypes().contains(type)));
    }

    features.add(Triple.of("banned_to", "String",
        s -> createBannedToSegmentIds(s, bansByApproachSegment, networkIdMapper)));

    /* green ratio and saturation flow factor the model uses, 1 when not set; no green ratio without a signal */
    features.add(Triple.of("signal", "Boolean",
        s -> intersections.getByDownstreamNodeOf(s).getApproachOrInternalSegment(s).hasSignal()));
    features.add(Triple.of("green", "java.lang.Double", s -> {
      var intersectionSegment = intersections.getByDownstreamNodeOf(s).getApproachOrInternalSegment(s);
      return intersectionSegment instanceof IntersectionSignalSegment ?
          ((IntersectionSignalSegment) intersectionSegment).getGreenRatio() : null;
    }));
    features.add(Triple.of("green_src", "String", s -> {
      var intersectionSegment = intersections.getByDownstreamNodeOf(s).getApproachOrInternalSegment(s);
      return intersectionSegment instanceof IntersectionSignalSegment ?
          createOriginText(((IntersectionSignalSegment) intersectionSegment).getGreenRatioOrigin()) : "";
    }));
    features.add(Triple.of("sat_flow", "java.lang.Double",
        s -> intersections.getByDownstreamNodeOf(s).getApproachOrInternalSegment(s).getSaturationFlowFactor()));
    features.add(Triple.of("sat_src", "String",
        s -> createOriginText(
            intersections.getByDownstreamNodeOf(s).getApproachOrInternalSegment(s).getSaturationFlowFactorOrigin())));
    features.add(Triple.of("eff_cap", "java.lang.Double",
        s -> intersections.getByDownstreamNodeOf(s).getApproachOrInternalSegment(s).getEffectiveCapacityFactor()));

    /* geometry last to append srid */
    features.add(Triple.of(DEFAULT_GEOMETRY_ATTRIBUTE_KEY, "LineString",
        s -> PlanitJtsUtils.transformGeometrySafe(
            EdgeSegmentUtils.createOrGetParentGeometryInSegmentDirection(s),
            destinationCrsTransformer)));
    return features;
  }

  /**
   * Constructor
   *
   * @param networkIdMapper id mapper to apply
   * @param intersections the segments belong to
   * @param bansByApproachSegment banned turns per approach segment, by identity
   * @param destinationCrsTransformer to use (may be null)
   */
  protected PlanitIntersectionSegmentFeatureTypeContext(
      final NetworkIdMapper networkIdMapper,
      final Intersections intersections,
      final Map<LinkSegment, List<BannedTurn>> bansByApproachSegment,
      final MathTransform destinationCrsTransformer){
    super(LinkSegment.class,
        createFeatureDescription(networkIdMapper, intersections, bansByApproachSegment, destinationCrsTransformer));
  }

  /**
   * Factory method
   *
   * @param networkIdMapper to apply for creating the ids of intersections, link segments and nodes
   * @param intersections the segments belong to
   * @param bansByApproachSegment banned turns per approach segment, by identity
   * @param destinationCrsTransformer to use (may be null)
   * @return created instance
   */
  public static PlanitIntersectionSegmentFeatureTypeContext create(
      final NetworkIdMapper networkIdMapper,
      final Intersections intersections,
      final Map<LinkSegment, List<BannedTurn>> bansByApproachSegment,
      final MathTransform destinationCrsTransformer){
    return new PlanitIntersectionSegmentFeatureTypeContext(
        networkIdMapper, intersections, bansByApproachSegment, destinationCrsTransformer);
  }

}
