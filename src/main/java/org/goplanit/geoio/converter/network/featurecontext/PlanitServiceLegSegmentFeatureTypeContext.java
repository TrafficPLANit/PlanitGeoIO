package org.goplanit.geoio.converter.network.featurecontext;

import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.converter.idmapping.ServiceNetworkIdMapper;
import org.goplanit.geoio.util.ModeShortNameConverter;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;
import org.locationtech.jts.geom.LineString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Track contextual relevant information for PLANit service leg segment type that is persisted
 *
 * @author markr
 */
public class PlanitServiceLegSegmentFeatureTypeContext extends PlanitEntityFeatureTypeContext<ServiceLegSegment> {

  /**
   * The mapping from PLANIT service leg segment instance to fixed GIS attributes of link segment
   *
   * @param serviceNetworkIdMapper to apply
   * @return feature mapping
   */
  private static List<Triple<String,String, Function<ServiceLegSegment, ? extends Object>>> createFixedFeatureDescription(
          final ServiceNetworkIdMapper serviceNetworkIdMapper, NetworkIdMapper networkIdMapper){
    return List.of(
            /* service leg segment info (fixed) */
            Triple.of("mapped_id", "java.lang.String", serviceNetworkIdMapper.getServiceLegSegmentIdMapper()),
            Triple.of("id", "java.lang.Long", ServiceLegSegment::getId),
            Triple.of("xml_id", "String", ServiceLegSegment::getXmlId),
            Triple.of("ext_id", "String", ServiceLegSegment::getExternalId),
            Triple.of("parent_id", "String", sls -> serviceNetworkIdMapper.getServiceLegIdMapper().apply(sls.getParent())),
            Triple.of("phys_segs", "String", sls -> !sls.hasPhysicalParentSegments() ? "" :   /* physical parent segments that make up for the service leg segment */
                    sls.getPhysicalParentSegments().stream().map( ls -> networkIdMapper.getLinkSegmentIdMapper().apply((MacroscopicLinkSegment) ls))),
            Triple.of("snode_up", "String", sls -> serviceNetworkIdMapper.getServiceNodeIdMapper().apply(sls.getUpstreamServiceNode())),
            Triple.of("snode_down", "String", sls -> serviceNetworkIdMapper.getServiceNodeIdMapper().apply(sls.getDownstreamServiceNode())),

            /* geometry taken from parent link */
            Triple.of(DEFAULT_GEOMETRY_ATTRIBUTE_KEY, "LineString",
                    (Function<ServiceLegSegment, LineString>) ls -> ls.getParent().getGeometry()));
  }

  /**
   * The mapping from PLANIT link instance to GIS attributes
   *
   * @param serviceNetworkIdMapper to apply
   * @param networkIdMapper to apply to parent PLANit entities
   * @return feature mapping
   */
  private static List<Triple<String,String, Function<ServiceLegSegment, ? extends Object>>> createFeatureDescription(
      final ServiceNetworkIdMapper serviceNetworkIdMapper, final NetworkIdMapper networkIdMapper){
    return createFixedFeatureDescription(serviceNetworkIdMapper, networkIdMapper);
  }

  /**
   * Constructor
   *
   * @param serviceNetworkIdMapper id mapper to apply
   * @param networkIdMapper id mapper to apply
   */
  protected PlanitServiceLegSegmentFeatureTypeContext(
      final ServiceNetworkIdMapper serviceNetworkIdMapper, final NetworkIdMapper networkIdMapper){
    super(ServiceLegSegment.class, createFeatureDescription(serviceNetworkIdMapper, networkIdMapper));
  }

  /**
   * Factory method
   *
   * @param serviceNetworkIdMapper to apply for creating each ids when persisting
   * @param networkIdMapper to apply for creating parent ids when persisting
   * @return created instance
   */
  public static PlanitServiceLegSegmentFeatureTypeContext create(
      final ServiceNetworkIdMapper serviceNetworkIdMapper, final NetworkIdMapper networkIdMapper){
    return new PlanitServiceLegSegmentFeatureTypeContext(serviceNetworkIdMapper, networkIdMapper);
  }

}
