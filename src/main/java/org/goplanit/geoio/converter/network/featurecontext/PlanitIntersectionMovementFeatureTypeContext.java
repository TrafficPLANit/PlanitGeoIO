package org.goplanit.geoio.converter.network.featurecontext;

import org.geotools.api.referencing.operation.MathTransform;
import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.network.layer.macroscopic.SpatialMovementUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.graph.directed.EdgeSegmentUtils;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovement;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Track contextual relevant information for each movement of a PLANit spatial intersection that is persisted. A
 * movement starts on an approach, which ends at a member node, so the intersection of a movement is the one holding the
 * downstream node of its from segment. Control and kinds are copied from that intersection. The geometry is a multi
 * line string in the direction of travel: the from segment, optionally the internal segments the movement passes, and
 * the to segment.
 *
 * @author markr
 */
public class PlanitIntersectionMovementFeatureTypeContext extends PlanitEntityFeatureTypeContext<SpatialMovement> {

  /**
   * Create the geometry of a movement, each segment in the direction of travel: its from segment, when asked the
   * internal segments it passes on its shortest connection, and its to segment. A movement whose from and to segment
   * are no longer connected gets only those two
   *
   * @param networkLayer the movement is on
   * @param movement to create the geometry of
   * @param geometryThroughInternalSegments true to include the internal segments passed, false for only the from and to
   *                                        segment
   * @return multi line string of the segments, in order
   */
  private static MultiLineString createMovementGeometry(
      MacroscopicNetworkLayer networkLayer, SpatialMovement movement, boolean geometryThroughInternalSegments){
    var segments = new ArrayList<LinkSegment>();
    segments.add(movement.getLinkSegmentFrom());
    if(geometryThroughInternalSegments){
      var passed = SpatialMovementUtils.findInternalSegmentsPassed(
          networkLayer, networkLayer.getIntersections().getSpatialIntersectionOf(movement), movement);
      if(passed != null){
        segments.addAll(passed);
      }
    }
    segments.add(movement.getLinkSegmentTo());
    return PlanitJtsUtils.createMultiLineString(segments.stream().map(
        EdgeSegmentUtils::createOrGetParentGeometryInSegmentDirection)
        .toArray(LineString[]::new));
  }

  /**
   * The mapping from movement instance to GIS attributes
   *
   * @param networkIdMapper to apply
   * @param networkLayer the movements are on
   * @param geometryThroughInternalSegments true for a geometry through the internal segments passed, false for only the
   *                                        from and to segment
   * @param destinationCrsTransformer to use (may be null)
   * @return feature mapping
   */
  private static List<Triple<String,String, Function<SpatialMovement, ?>>> createFeatureDescription(
      final NetworkIdMapper networkIdMapper,
      final MacroscopicNetworkLayer networkLayer,
      final boolean geometryThroughInternalSegments,
      final MathTransform destinationCrsTransformer){
    var intersections = networkLayer.getIntersections();
    var features = new ArrayList<Triple<String,String, Function<SpatialMovement, ?>>>();
    features.add(Triple.of("isect_id", "String",
        m -> networkIdMapper.getIntersectionIdMapper().apply(intersections.getSpatialIntersectionOf(m))));
    features.add(Triple.of("mvmt_id", "String", networkIdMapper.getSpatialMovementIdMapper()));
    features.add(Triple.of("from_seg", "String",
        m -> networkIdMapper.getLinkSegmentIdMapper().apply(m.getLinkSegmentFrom())));
    features.add(Triple.of("to_seg", "String",
        m -> networkIdMapper.getLinkSegmentIdMapper().apply(m.getLinkSegmentTo())));
    features.add(Triple.of("length_km", "java.lang.Double", SpatialMovement::getLengthKm));
    features.add(Triple.of("control", "String",
        m -> intersections.getSpatialIntersectionOf(m).getControlType().value()));

    /* one column per kind of intersection */
    for(var type : IntersectionType.values()){
      features.add(Triple.of(
          type.value(), "Boolean", m -> intersections.getSpatialIntersectionOf(m).getTypes().contains(type)));
    }

    features.add(Triple.of("direct_ban", "Boolean", SpatialMovement::hasDirectBan));
    features.add(Triple.of("ban_modes", "String", m -> StringUtils.toSortedCommaSeparatedString(
        m.getExplicitlyDirectlyBannedModes(), networkIdMapper.getModeIdMapper())));

    /* own values, empty when not set, and the values used: its own when set, otherwise those of its approach */
    features.add(Triple.of("green", "java.lang.Double", SpatialMovement::getGreenRatio));
    features.add(Triple.of("sat_flow", "java.lang.Double", SpatialMovement::getSaturationFlowFactor));
    features.add(Triple.of("green_used", "java.lang.Double",
        m -> intersections.getSpatialIntersectionOf(m).getGreenRatioUsedBy(m)));
    features.add(Triple.of("sat_used", "java.lang.Double",
        m -> intersections.getSpatialIntersectionOf(m).getSaturationFlowFactorUsedBy(m)));
    features.add(Triple.of("eff_cap", "java.lang.Double",
        m -> intersections.getSpatialIntersectionOf(m).getEffectiveCapacityFactorUsedBy(m)));

    /* geometry last to append srid */
    features.add(Triple.of(DEFAULT_GEOMETRY_ATTRIBUTE_KEY, "MultiLineString",
        m -> PlanitJtsUtils.transformGeometrySafe(
            createMovementGeometry(networkLayer, m, geometryThroughInternalSegments), destinationCrsTransformer)));
    return features;
  }

  /**
   * Constructor
   *
   * @param networkIdMapper id mapper to apply
   * @param networkLayer the movements are on
   * @param geometryThroughInternalSegments true for a geometry through the internal segments passed, false for only the
   *                                        from and to segment
   * @param destinationCrsTransformer to use (may be null)
   */
  protected PlanitIntersectionMovementFeatureTypeContext(
      final NetworkIdMapper networkIdMapper,
      final MacroscopicNetworkLayer networkLayer,
      final boolean geometryThroughInternalSegments,
      final MathTransform destinationCrsTransformer){
    super(SpatialMovement.class, createFeatureDescription(
        networkIdMapper, networkLayer, geometryThroughInternalSegments, destinationCrsTransformer));
  }

  /**
   * Factory method
   *
   * @param networkIdMapper to apply for creating the ids of intersections, movements, link segments and modes
   * @param networkLayer the movements are on
   * @param geometryThroughInternalSegments true for a geometry through the internal segments passed, false for only the
   *                                        from and to segment
   * @param destinationCrsTransformer to use (may be null)
   * @return created instance
   */
  public static PlanitIntersectionMovementFeatureTypeContext create(
      final NetworkIdMapper networkIdMapper,
      final MacroscopicNetworkLayer networkLayer,
      final boolean geometryThroughInternalSegments,
      final MathTransform destinationCrsTransformer){
    return new PlanitIntersectionMovementFeatureTypeContext(
        networkIdMapper, networkLayer, geometryThroughInternalSegments, destinationCrsTransformer);
  }

}
