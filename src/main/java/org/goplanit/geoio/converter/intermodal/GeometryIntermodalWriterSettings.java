package org.goplanit.geoio.converter.intermodal;

import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.geoio.converter.network.GeometryNetworkWriterSettings;
import org.goplanit.geoio.converter.service.GeometryRoutedServicesWriterSettings;
import org.goplanit.geoio.converter.service.GeometryServiceNetworkWriterSettings;
import org.goplanit.geoio.converter.zoning.GeometryZoningWriterSettings;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Settings for Geometry (GIS) intermodal writer
 * 
 * @author markr
 *
 */
public class GeometryIntermodalWriterSettings implements ConverterWriterSettings {

  /** the network settings to use */
  protected final GeometryNetworkWriterSettings networkSettings;
  
  /** the zoning settings to use */
  protected final GeometryZoningWriterSettings zoningSettings;

  /** the service network settings to use */
  protected final GeometryServiceNetworkWriterSettings serviceNetworkSettings;

  /** the routed services settings to use */
  protected final GeometryRoutedServicesWriterSettings routedServicesSettings;

  /**
   * Default constructor
   */
  public GeometryIntermodalWriterSettings() {
    this( new GeometryNetworkWriterSettings(),
          new GeometryZoningWriterSettings(),
          new GeometryServiceNetworkWriterSettings(),
          new GeometryRoutedServicesWriterSettings());
  }
  
  /**
   * Constructor
   * 
   * @param outputDirectory to use
   * @param countryName to use
   */
  public GeometryIntermodalWriterSettings(final String outputDirectory, final String countryName) {
    this(
            new GeometryNetworkWriterSettings(outputDirectory, countryName),
            new GeometryZoningWriterSettings(outputDirectory, countryName),
            new GeometryServiceNetworkWriterSettings(outputDirectory, countryName),
            new GeometryRoutedServicesWriterSettings(outputDirectory, countryName));
  }      
  
  /**
   * Constructor
   * 
   * @param networkSettings to use
   * @param zoningSettings to use
   * @param serviceNetworkSettings to use
   * @param routedServicesSettings to use
   */
  public GeometryIntermodalWriterSettings(
          final GeometryNetworkWriterSettings networkSettings,
          final GeometryZoningWriterSettings zoningSettings,
          final GeometryServiceNetworkWriterSettings serviceNetworkSettings,
          final GeometryRoutedServicesWriterSettings routedServicesSettings) {
    this.networkSettings = networkSettings;
    this.zoningSettings = zoningSettings;
    this.serviceNetworkSettings = serviceNetworkSettings;
    this.routedServicesSettings = routedServicesSettings;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    getNetworkSettings().reset();
    getZoningSettings().reset();
    getServiceNetworkSettings().reset();
    getRoutedServicesSettings().reset();
  }

  /** Collect zoning settings
   * 
   * @return zoning settings
   */
  public GeometryZoningWriterSettings getZoningSettings() {
    return zoningSettings;
  }

  /** Collect network settings
   * 
   * @return network settings
   */
  public  GeometryNetworkWriterSettings getNetworkSettings() {
    return networkSettings;
  }

  /** Collect service network settings
   *
   * @return service network settings
   */
  public  GeometryServiceNetworkWriterSettings getServiceNetworkSettings() {
    return serviceNetworkSettings;
  }

  /** Collect routed services settings
   *
   * @return routed services settings
   */
  public  GeometryRoutedServicesWriterSettings getRoutedServicesSettings() {
    return routedServicesSettings;
  }

  /** Set the outputPathDirectory used on both zoning and (service) network settings
   * 
   * @param outputDirectory to use
   */
  public void setOutputDirectory(String outputDirectory) {
    getZoningSettings().setOutputDirectory(outputDirectory);
    getNetworkSettings().setOutputDirectory(outputDirectory);
    getServiceNetworkSettings().setOutputDirectory(outputDirectory);
    getRoutedServicesSettings().setOutputDirectory(outputDirectory);
  }

  /** Set country name used on both zoning and (service) network settings
   * 
   * @param countryName to use
   */
  public void setCountry(String countryName) {
    getZoningSettings().setCountry(countryName);
    getNetworkSettings().setCountry(countryName);
    getServiceNetworkSettings().setCountry(countryName);
    getRoutedServicesSettings().setCountry(countryName);
  }
  
  /** Set the destination Crs to use (if not set, network's native Crs will be used, unless the user has specified a
   * specific country for which we have a more appropriate Crs registered) 
   * 
   * @param destinationCoordinateReferenceSystem to use
   */
  public void setDestinationCoordinateReferenceSystem(CoordinateReferenceSystem destinationCoordinateReferenceSystem) {
    getZoningSettings().setDestinationCoordinateReferenceSystem(destinationCoordinateReferenceSystem);
    getNetworkSettings().setDestinationCoordinateReferenceSystem(destinationCoordinateReferenceSystem);
    getServiceNetworkSettings().setDestinationCoordinateReferenceSystem(destinationCoordinateReferenceSystem);
    getRoutedServicesSettings().setDestinationCoordinateReferenceSystem(destinationCoordinateReferenceSystem);
  }  
  
}
