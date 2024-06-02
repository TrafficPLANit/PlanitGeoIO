package org.goplanit.geoio.converter.network.featurecontext;

import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.geoio.util.ModeShortNameConverter;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.graph.EdgeUtils;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.locationtech.jts.geom.LineString;
import org.opengis.referencing.operation.MathTransform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Track contextual relevant information for PLANit link segment type that is persisted
 *
 * @author markr
 */
public class PlanitLinkSegmentFeatureTypeContext extends PlanitEntityFeatureTypeContext<MacroscopicLinkSegment> {

  /**
   * Create or obtain link geometry. When no dedicated geometry is present it is created in link segment direction from edge vertices
   *
   * @param linkSegment to use
   * @return geometry found
   */
  public static LineString createOrGetLinkSegmentGeometry(MacroscopicLinkSegment linkSegment){
    var geometry = linkSegment.getParentLink().getGeometry();
    if(geometry == null){
      geometry = EdgeUtils.createLineStringFromVertexLocations(linkSegment.getParentLink(), linkSegment.isDirectionAb());
    }
    return geometry;
  }

  /**
   * The mapping from PLANIT link segment instance to fixed GIS attributes of link segment (without geometry)
   *
   * @param networkIdMapper to apply
   * @return feature mapping
   */
  private static List<Triple<String,String, Function<MacroscopicLinkSegment, ?>>> createFixedFeatureDescription(
          final NetworkIdMapper networkIdMapper){
    return List.of(
            /* link segment info (fixed) */
            Triple.of("mapped_id", "java.lang.String", networkIdMapper.getLinkSegmentIdMapper()),
            Triple.of("id", "java.lang.Long", MacroscopicLinkSegment::getId),
            Triple.of("segment_id", "java.lang.Long", MacroscopicLinkSegment::getLinkSegmentId),
            Triple.of("xml_id", "String", MacroscopicLinkSegment::getXmlId),
            Triple.of("ext_id", "String", MacroscopicLinkSegment::getExternalId),
            Triple.of("parent_id", "String", ls -> networkIdMapper.getLinkIdMapper().apply(ls.getParentLink())),
            Triple.of("lanes", "Integer", MacroscopicLinkSegment::getNumberOfLanes),
            Triple.of("cap_pcuh", "Float", MacroscopicLinkSegment::getCapacityOrDefaultPcuH),    /* max flow in pcu per hour across all lanes */
            Triple.of("speed_kmh", "Float", MacroscopicLinkSegment::getPhysicalSpeedLimitKmH),   /* speed limit on sign, not mode dependent */
            Triple.of("geom_opp", "Boolean", ls -> !ls.isParentGeometryInSegmentDirection(true)),     /* does geometry run in opposite direction to travel direction */
            Triple.of("node_up", "String", ls -> networkIdMapper.getVertexIdMapper().apply(ls.getUpstreamNode())),
            Triple.of("node_down", "String", ls -> networkIdMapper.getVertexIdMapper().apply(ls.getDownstreamNode())),

            /* link segment type info (fixed) */
            Triple.of("type_id", "String", ls -> networkIdMapper.getLinkSegmentTypeIdMapper().apply(ls.getLinkSegmentType())),
            Triple.of("type_name", "String", ls -> ls.getLinkSegmentType().getName()),
            Triple.of("dens_pcukm", "Float", ls -> ls.getLinkSegmentType().getExplicitMaximumDensityPerLaneOrDefault()));
  }

  /**
   * The mapping from PLANit link instance to GIS attributes
   *
   * @param networkIdMapper to apply
   * @param supportedModes modes supported on at least a single link segment type on the layer, hence included in all records
   * @param destinationCrsTransformer to use (may be null)
   * @return feature mapping
   */
  private static List<Triple<String,String, Function<MacroscopicLinkSegment, ?>>> createFeatureDescription(
          final NetworkIdMapper networkIdMapper,
          Collection<? extends Mode> supportedModes,
          final MathTransform destinationCrsTransformer){
    /* fixed features -  always present and non-variable number */
    var fixedFeatures =
            createFixedFeatureDescription(networkIdMapper);

    /* variable features - depends on modes present */
    var modeSpecificFeatures = new ArrayList<Triple<String,String, Function<MacroscopicLinkSegment, ?>>>();
    for(final var mode : supportedModes){
      String modeAttributeShortName = ModeShortNameConverter.asShortName(mode, networkIdMapper.getModeIdMapper());

      /* mode allowed */
      modeSpecificFeatures.add(Triple.of(modeAttributeShortName + "_ban", "Boolean", ls -> !ls.isModeAllowed(mode)));
      /* mode specific maximum speed */
      modeSpecificFeatures.add(Triple.of(modeAttributeShortName + "_spd", "String", ls -> ls.getModelledSpeedLimitKmH(mode)));
      /* mode specific critical speed */
      modeSpecificFeatures.add(Triple.of(modeAttributeShortName + "_spdc", "String", ls -> ls.getLinkSegmentType().getCriticalSpeedKmH(mode)));
    }

    /* features that depend on which modes are supported on the layer */
    var allFeatures = Stream.concat(fixedFeatures.stream(),modeSpecificFeatures.stream()).collect(Collectors.toList());

    /* geometry taken from parent link, needs to be last to append srid */
    Triple<String,String, Function<MacroscopicLinkSegment, ?>> geometryFeature =
            Triple.of(DEFAULT_GEOMETRY_ATTRIBUTE_KEY, "LineString",
                ls -> PlanitJtsUtils.transformGeometrySafe(createOrGetLinkSegmentGeometry(ls),destinationCrsTransformer));

    allFeatures.add(geometryFeature);
    return allFeatures;
  }

  /**
   * Constructor
   *
   * @param networkIdMapper id mapper to apply
   * @param supportedModes modes supported on at least a single link segment type on the layer, hence included in all records
   * @param destinationCrsTransformer to use (may be null)
   */
  protected PlanitLinkSegmentFeatureTypeContext(
          final NetworkIdMapper networkIdMapper,
          final Collection<? extends Mode> supportedModes,
          final MathTransform destinationCrsTransformer){
    super(MacroscopicLinkSegment.class,
            createFeatureDescription(networkIdMapper, supportedModes, destinationCrsTransformer));
  }

  /**
   * Factory method
   *
   * @param networkIdMapper to apply for creating each ids when persisting
   * @param supportedModes modes supported on at least a single link segment type on the layer, hence included in all records
   * @param destinationCrsTransformer to use (may be null)
   * @return created instance
   */
  public static PlanitLinkSegmentFeatureTypeContext create(
          final NetworkIdMapper networkIdMapper,
          final Collection<? extends Mode> supportedModes,
          final MathTransform destinationCrsTransformer){
    return new PlanitLinkSegmentFeatureTypeContext( networkIdMapper, supportedModes, destinationCrsTransformer);
  }

}
