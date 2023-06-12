package org.goplanit.geoio.converter.service.featurecontext;

import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.converter.idmapping.RoutedServicesIdMapper;
import org.goplanit.converter.idmapping.ServiceNetworkIdMapper;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.service.routed.RoutedServices;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;
import org.goplanit.utils.service.routed.RoutedService;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;

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
   * @return feature mapping
   */
  private static List<Triple<String,String, Function<RoutedService, ? extends Object>>> createFixedFeatureDescription(
      final RoutedServicesIdMapper routedServicesIdMapper){
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
                (Function<RoutedService, MultiLineString>) rs -> rs.extractGeometry(true, true)));
  }

  /**
   * The mapping from PLANIT routed service instance to GIS attributes
   *
   * @param routedServicesIdMapper to apply
   * @return feature mapping
   */
  private static List<Triple<String,String, Function<RoutedService, ? extends Object>>> createFeatureDescription(
      final RoutedServicesIdMapper routedServicesIdMapper){
    return createFixedFeatureDescription(routedServicesIdMapper);
  }

  /**
   * Constructor
   *
   * @param routedServicesIdMapper id mapper to apply
   */
  protected PlanitRoutedServiceFeatureTypeContext(final RoutedServicesIdMapper routedServicesIdMapper){
    super(RoutedService.class, createFeatureDescription(routedServicesIdMapper));
  }

  /**
   * Factory method
   *
   * @param routedServicesIdMapper to apply for creating each ids when persisting
   * @return created instance
   */
  public static PlanitRoutedServiceFeatureTypeContext create(final RoutedServicesIdMapper routedServicesIdMapper){
    return new PlanitRoutedServiceFeatureTypeContext(routedServicesIdMapper);
  }

}
