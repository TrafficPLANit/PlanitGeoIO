package org.goplanit.geoio.converter.service.featurecontext;

import org.goplanit.converter.idmapping.RoutedServicesIdMapper;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.service.routed.RoutedService;
import org.opengis.referencing.operation.MathTransform;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Track contextual relevant information for PLANit routed service that is persisted
 *
 * @author markr
 */
public class PlanitRoutedServiceFeatureTypeContext extends PlanitEntityFeatureTypeContext<RoutedService> {

  /**
   * The mapping from PLANIT routed service instance to fixed GIS attributes of that service
   *
   * @param routedServicesIdMapper to apply
   * @param destinationCrsTransformer to use (may be null)
   * @return feature mapping
   */
  private static List<Triple<String,String, Function<RoutedService, ?>>> createFixedFeatureDescription(
      final RoutedServicesIdMapper routedServicesIdMapper, final MathTransform destinationCrsTransformer){
    return List.of(
        /* service leg segment info (fixed) */
        Triple.of("mapped_id", "java.lang.String", routedServicesIdMapper.getRoutedServiceRefIdMapper()),
        Triple.of("id", "java.lang.Long", RoutedService::getId),
        Triple.of("xml_id", "String", RoutedService::getXmlId),
        Triple.of("ext_id", "String", RoutedService::getExternalId),
        Triple.of("name", "String", RoutedService::getName),
        Triple.of("name_descr", "String", RoutedService::getNameDescription),
        Triple.of("serv_descr", "String", RoutedService::getServiceDescription),
        Triple.of("trips_schd", "String",
            (rs) -> rs.getTripInfo().getScheduleBasedTrips().stream().map( t -> routedServicesIdMapper.getRoutedTripRefIdMapper().apply(t)).collect(Collectors.joining(","))),
        Triple.of("trips_freq", "String",
            (rs) -> rs.getTripInfo().getFrequencyBasedTrips().stream().map( t -> routedServicesIdMapper.getRoutedTripRefIdMapper().apply(t)).collect(Collectors.joining(","))),

        /* geometry taken from underlying trips */
        Triple.of(DEFAULT_GEOMETRY_ATTRIBUTE_KEY, "MultiLineString",
                rs -> PlanitJtsUtils.transformGeometrySafe(
                    rs.extractGeometry(true, true), destinationCrsTransformer)));
  }

  /**
   * The mapping from PLANIT routed service instance to GIS attributes
   *
   * @param routedServicesIdMapper to apply
   * @param destinationCrsTransformer to use (may be null)
   * @return feature mapping
   */
  private static List<Triple<String,String, Function<RoutedService, ?>>> createFeatureDescription(
      final RoutedServicesIdMapper routedServicesIdMapper,
      final MathTransform destinationCrsTransformer){
    return createFixedFeatureDescription(routedServicesIdMapper, destinationCrsTransformer);
  }

  /**
   * Constructor
   *
   * @param routedServicesIdMapper id mapper to apply
   * @param destinationCrsTransformer to use (may be null)
   */
  protected PlanitRoutedServiceFeatureTypeContext(
      final RoutedServicesIdMapper routedServicesIdMapper,
      final MathTransform destinationCrsTransformer){
    super(RoutedService.class, createFeatureDescription(routedServicesIdMapper, destinationCrsTransformer));
  }

  /**
   * Factory method
   *
   * @param routedServicesIdMapper to apply for creating each ids when persisting
   * @param destinationCrsTransformer to use (may be null)
   * @return created instance
   */
  public static PlanitRoutedServiceFeatureTypeContext create(
      final RoutedServicesIdMapper routedServicesIdMapper,
      final MathTransform destinationCrsTransformer){
    return new PlanitRoutedServiceFeatureTypeContext(routedServicesIdMapper, destinationCrsTransformer);
  }

}
