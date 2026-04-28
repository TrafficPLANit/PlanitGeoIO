package org.goplanit.geoio.converter.network.featurecontext;

import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.graph.EdgeUtils;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.physical.ConjugateLinkSegment;
import org.locationtech.jts.geom.LineString;
import org.geotools.api.referencing.operation.MathTransform;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Track contextual relevant information for PLANit conjugate link segment type that is persisted
 *
 * @author markr
 */
public class PlanitConjugateLinkSegmentFeatureTypeContext extends PlanitEntityFeatureTypeContext<ConjugateLinkSegment> {

  /**
   * Create geometry in link segment direction using parent vertex locations. It is assumed parent's vertices
   * have a coordinate to be able to support this.
   *
   * @param linkSegment to use
   * @return geometry found
   */
  public static LineString createOrGetLinkSegmentGeometry(ConjugateLinkSegment linkSegment){
    return EdgeUtils.createLineStringFromVertexLocations(linkSegment.getParent(), linkSegment.isDirectionAb());
  }

  /**
   * The mapping from PLANIT link segment instance to fixed GIS attributes of link segment (without geometry)
   *
   * @param networkIdMapper to apply
   * @return feature mapping
   */
  private static List<Triple<String,String, Function<ConjugateLinkSegment, ?>>> createFixedFeatureDescription(
          final NetworkIdMapper networkIdMapper){
    return List.of(
            /* link segment info (fixed) */
            Triple.of("mapped_id", "java.lang.String",
                    networkIdMapper.getConjugateLinkSegmentIdMapper()),
            Triple.of("id", "java.lang.Long", ConjugateLinkSegment::getId),
            Triple.of("segment_id", "java.lang.Long", ConjugateLinkSegment::getLinkSegmentId),
            Triple.of("xml_id", "String", ConjugateLinkSegment::getXmlId),
            Triple.of("ext_id", "String", ConjugateLinkSegment::getExternalId),
            Triple.of("parent_id", "String",
                    ls -> networkIdMapper.getLinkIdMapper().apply(ls.getParent())),
            Triple.of("geom_opp", "Boolean",
                    /* does geometry run in opposite direction to travel direction */
                    ls -> !ls.isParentGeometryInSegmentDirection(true)),
            Triple.of("node_up", "String",
                    ls -> networkIdMapper.getVertexIdMapper().apply(ls.getUpstreamNode())),
            Triple.of("node_down", "String",
                    ls -> networkIdMapper.getVertexIdMapper().apply(ls.getDownstreamNode())));

            /* conjugate specific - original underlying link segments */
            // todo, but for now partially captured in the XMLid which is setup to be auto-generated from underlying
            //  originals as part of default PLANit behaviour upon creation.
  }

  /**
   * The mapping from PLANit link instance to GIS attributes
   *
   * @param networkIdMapper to apply
   * @param destinationCrsTransformer to use (may be null)
   * @return feature mapping
   */
  private static List<Triple<String,String, Function<ConjugateLinkSegment, ?>>> createFeatureDescription(
          final NetworkIdMapper networkIdMapper,
          final MathTransform destinationCrsTransformer){
    /* fixed features -  always present and non-variable number */
    var fixedFeatures =
            createFixedFeatureDescription(networkIdMapper);

    /* geometry taken from parent link, needs to be last to append srid */
    Triple<String,String, Function<ConjugateLinkSegment, ?>> geometryFeature =
            Triple.of(DEFAULT_GEOMETRY_ATTRIBUTE_KEY, "LineString",
                ls -> PlanitJtsUtils.transformGeometrySafe(
                        createOrGetLinkSegmentGeometry(ls),destinationCrsTransformer));

    return Stream.concat(fixedFeatures.stream(), Stream.of(geometryFeature)).collect(Collectors.toList());
  }

  /**
   * Constructor
   *
   * @param networkIdMapper id mapper to apply
   * @param supportedModes modes supported on at least a single link segment type on the layer, hence included in all records
   * @param destinationCrsTransformer to use (may be null)
   */
  protected PlanitConjugateLinkSegmentFeatureTypeContext(
          final NetworkIdMapper networkIdMapper,
          final Collection<? extends Mode> supportedModes,
          final MathTransform destinationCrsTransformer){
    super(ConjugateLinkSegment.class, createFeatureDescription(networkIdMapper, destinationCrsTransformer));
  }

  /**
   * Factory method
   *
   * @param networkIdMapper to apply for creating each ids when persisting
   * @param supportedModes modes supported on at least a single link segment type on the layer, hence included in all records
   * @param destinationCrsTransformer to use (may be null)
   * @return created instance
   */
  public static PlanitConjugateLinkSegmentFeatureTypeContext create(
          final NetworkIdMapper networkIdMapper,
          final Collection<? extends Mode> supportedModes,
          final MathTransform destinationCrsTransformer){
    return new PlanitConjugateLinkSegmentFeatureTypeContext(
            networkIdMapper, supportedModes, destinationCrsTransformer);
  }

}
