package org.goplanit.geoio.converter.service.featurecontext;

import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.network.layer.service.ServiceLeg;
import org.goplanit.utils.network.layer.service.ServiceNode;
import org.opengis.referencing.operation.MathTransform;

import java.util.List;
import java.util.function.Function;

/**
 * Track contextual relevant information for PLANit service leg type that is persisted
 *
 * @author markr
 */
public class PlanitServiceLegFeatureTypeContext extends PlanitEntityFeatureTypeContext<ServiceLeg> {


  /**
   * The mapping from PLANIT service leg instance to GIS attributes
   *
   * @param legIdMapper to apply
   * @param serviceNodeIdMapper to apply
   * @param destinationCrsTransformer to use (may be null)
   * @return feature mapping
   */
  private static List<Triple<String,String, Function<ServiceLeg, ?>>> createFeatureDescription(
          Function<ServiceLeg, String> legIdMapper,
          Function<ServiceNode, String> serviceNodeIdMapper,
          final MathTransform destinationCrsTransformer){
    return List.of(
            Triple.of("mapped_id", "java.lang.String", legIdMapper),
            Triple.of("id", "java.lang.Long", ServiceLeg::getId),
            Triple.of("xml_id", "String", ServiceLeg::getXmlId),
            Triple.of("ext_id", "String", ServiceLeg::getExternalId),
            Triple.of("name", "String", (Function<ServiceLeg, String>) ServiceLeg::getName),
            Triple.of("length_km", "java.lang.Double", l -> l.getLengthKm(ServiceLeg.LengthType.AVERAGE)),
            Triple.of("snode_a", "String", l -> serviceNodeIdMapper.apply(l.getServiceNodeA())),
            Triple.of("snode_b", "String", l -> serviceNodeIdMapper.apply(l.getServiceNodeB())),
            Triple.of(DEFAULT_GEOMETRY_ATTRIBUTE_KEY, "LineString",
                sl -> PlanitJtsUtils.transformGeometrySafe(sl.getGeometry(), destinationCrsTransformer)));
  }

  /**
   * Constructor
   *
   * @param legIdMapper id mapper to apply
   * @param serviceNodeIdMapper id mapper to apply for attributes requiring node ids
   * @param destinationCrsTransformer to use (may be null)
   */
  protected PlanitServiceLegFeatureTypeContext(
      Function<ServiceLeg, String> legIdMapper,
      Function<ServiceNode, String> serviceNodeIdMapper,
      final MathTransform destinationCrsTransformer){
    super(ServiceLeg.class, createFeatureDescription(legIdMapper, serviceNodeIdMapper, destinationCrsTransformer));
  }

  /**
   * Factory method
   *
   * @param legIdMapper to apply for creating each link's unique id when persisting
   * @param serviceNodeIdMapper to apply for creating node id references
   * @param destinationCrsTransformer to use (may be null)
   * @return created instance
   */
  public static PlanitServiceLegFeatureTypeContext create(
      Function<ServiceLeg, String> legIdMapper,
      Function<Vertex, String> serviceNodeIdMapper,
      final MathTransform destinationCrsTransformer){
    return new PlanitServiceLegFeatureTypeContext(
        legIdMapper /* convert to link as type */,
        serviceNodeIdMapper::apply /* convert to node as type */,
        destinationCrsTransformer);
  }

}
