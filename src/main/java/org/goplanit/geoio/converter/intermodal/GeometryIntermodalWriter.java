package org.goplanit.geoio.converter.intermodal;

import org.goplanit.converter.idmapping.IdMapperType;
import org.goplanit.converter.intermodal.IntermodalWriter;
import org.goplanit.geoio.converter.network.GeometryNetworkWriter;
import org.goplanit.geoio.converter.network.GeometryNetworkWriterFactory;
import org.goplanit.geoio.converter.service.GeometryRoutedServicesWriterFactory;
import org.goplanit.geoio.converter.service.GeometryServiceNetworkWriterFactory;
import org.goplanit.geoio.converter.zoning.GeometryZoningWriter;
import org.goplanit.geoio.converter.zoning.GeometryZoningWriterFactory;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.service.routed.RoutedServices;
import org.goplanit.utils.misc.Pair;
import org.goplanit.zoning.Zoning;

import java.util.logging.Logger;

/**
 * Geometry intermodal writer to persist in GIS format, wrapping a Geometry network writer and Geometry zoning writer (and optionally a
 * service network and routes services writer )in one
 * 
 * @author markr
 *
 */
public class GeometryIntermodalWriter implements IntermodalWriter<ServiceNetwork, RoutedServices> {

  /**Logger to use */
  private static final Logger LOGGER = Logger.getLogger(GeometryIntermodalWriter.class.getCanonicalName());
  
  /** intermodal writer settings to use */
  protected final GeometryIntermodalWriterSettings settings;

  /**
   * the id mapper to use
   */
  protected IdMapperType idMapper;

  /**
   * Persist network and zoning and return writers used. If the virtual network of the combination of zoning and network is
   * indicated to be persisted, then this is done automatically.
   *
   * @param macroscopicNetwork to persist
   * @param zoning to persist
   * @return used writers, network and zoning, respectively
   */
  protected Pair<GeometryNetworkWriter, GeometryZoningWriter> writeNetworkAndZoning(
      MacroscopicNetwork macroscopicNetwork, Zoning zoning) {

    /* also persist virtual network, i.e., the relation between zones and connectoids, including the virtual edges/edge segments */
    if(getSettings().getZoningSettings().isPersistVirtualNetwork()){
      if(zoning.getVirtualNetwork() != null && !zoning.getVirtualNetwork().isEmpty()){
        LOGGER.info("Virtual network present on zoning, using existing virtual network to persist");
      }else{
        LOGGER.info("Virtual network not present on zoning, integrating network and zoning to be able to persist virtual network");
        // zoning virtual network populated as a result of the below integration
        new TransportModelNetwork(macroscopicNetwork, zoning).integrateTransportNetworkViaConnectoids();
      }
    }

    /* network writer */
    var networkSettings = getSettings().getNetworkSettings();
    var networkWriter = GeometryNetworkWriterFactory.create(networkSettings.getOutputDirectory(), networkSettings.getCountry());
    networkWriter.setIdMapperType(getIdMapperType());
    networkWriter.write(macroscopicNetwork);

    /* zoning writer - with pt component via transfer zones */
    var zoningSettings = getSettings().getZoningSettings();
    var zoningWriter =
            GeometryZoningWriterFactory.create(zoningSettings.getOutputDirectory(), zoningSettings.getCountry(), macroscopicNetwork.getCoordinateReferenceSystem());
    zoningWriter.setParentIdMappers(networkWriter.getPrimaryIdMapper()); // pass on parent ref mapping
    zoningWriter.setIdMapperType(getIdMapperType());
    zoningWriter.write(zoning);

    return Pair.of(networkWriter, zoningWriter);
  }

  /** Constructor
   *
   * @param outputDirectory to persist on
   * @param countryName to optimise projection for (if available, otherwise ignore)
   */
  protected GeometryIntermodalWriter(
          String outputDirectory,
          String countryName) {
    this(new GeometryIntermodalWriterSettings(outputDirectory, countryName));
  }

  /** Constructor
   * @param settings to use
   */
  protected GeometryIntermodalWriter(GeometryIntermodalWriterSettings settings) {
    this.idMapper = IdMapperType.XML;
    this.settings = settings;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(MacroscopicNetwork macroscopicNetwork, Zoning zoning){
    writeNetworkAndZoning(macroscopicNetwork, zoning);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeWithServices(MacroscopicNetwork macroscopicNetwork, Zoning zoning, ServiceNetwork serviceNetwork, RoutedServices routedServices) {

    /* perform persistence without services first */
    var networkAndZoningWriter = writeNetworkAndZoning(macroscopicNetwork, zoning);

    /* perform persistence for services */

    /* service network writer */
    var serviceNetworkSettings = getSettings().getServiceNetworkSettings();
    var serviceNetworkWriter =
            GeometryServiceNetworkWriterFactory.create(
                    serviceNetworkSettings.getOutputDirectory(), serviceNetworkSettings.getCountry());

    // service network writer requires physical network id ref mapping and possibly zoning one as well
    var networkIdMapper = networkAndZoningWriter.first().getPrimaryIdMapper();
    var zoningIdMapper = networkAndZoningWriter.second().getPrimaryIdMapper();
    serviceNetworkWriter.setParentIdMappers(networkIdMapper, zoningIdMapper);

    serviceNetworkWriter.setIdMapperType(getIdMapperType());
    serviceNetworkWriter.write(serviceNetwork);

    /* routed services writer */
    var routedServicesSettings = getSettings().getRoutedServicesSettings();
    var routedServicesWriter =
        GeometryRoutedServicesWriterFactory.create(
            routedServicesSettings.getOutputDirectory(), routedServicesSettings.getCountry());

    // routed services only requires service network entity references, those are present on the service network writer id mappings
    routedServicesWriter.setParentIdMappers(networkIdMapper, zoningIdMapper, serviceNetworkWriter.getPrimaryIdMapper());

    routedServicesWriter.setIdMapperType(getIdMapperType());
    routedServicesWriter.write(routedServices);
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public IdMapperType getIdMapperType() {
    return idMapper;
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public void setIdMapperType(IdMapperType idMapper) {
    this.idMapper = idMapper;
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public void reset() {
  }

  /**
   * {@inheritDoc}
   */    
  @Override
  public GeometryIntermodalWriterSettings getSettings() {
    return this.settings;
  }

}
