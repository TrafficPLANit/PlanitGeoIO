package org.goplanit.geoio.converter.service;

import org.geotools.data.DataStore;
import org.goplanit.converter.idmapping.RoutedServicesIdMapper;
import org.goplanit.converter.service.RoutedServicesWriter;
import org.goplanit.geoio.converter.GeometryIoWriter;
import org.goplanit.geoio.converter.service.featurecontext.PlanitRoutedServiceFeatureTypeContext;
import org.goplanit.geoio.converter.service.featurecontext.PlanitServiceNodeFeatureTypeContext;
import org.goplanit.geoio.util.GeoIODataStoreManager;
import org.goplanit.geoio.util.GeoIoFeatureTypeBuilder;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.service.routed.RoutedServices;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.service.ServiceNode;
import org.goplanit.utils.service.routed.*;
import org.opengis.feature.simple.SimpleFeatureType;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

import static java.util.Map.entry;

/**
 * Writer to persist PLANit routed services to disk in GIS based format(s) such as shape files.
 *
 * @author markr
 *
 */
public class GeometryRoutedServicesWriter extends GeometryIoWriter<RoutedServices> implements RoutedServicesWriter {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(GeometryRoutedServicesWriter.class.getCanonicalName());

  /** routed services writer settings to use */
  private final GeometryRoutedServicesWriterSettings settings;

  /** construct prefix for a given layer in String format */
  private Function<RoutedServicesLayer, String> layerPrefixProducer = null;

  /** Default constructor
   *
   */
  protected GeometryRoutedServicesWriter() {
    this(null, CountryNames.GLOBAL);
  }

  /** Constructor
   *
   * @param outputPath to persist in
   */
  protected GeometryRoutedServicesWriter(String outputPath) {
    this(outputPath, CountryNames.GLOBAL);
  }

  /**
   * Validate before persisting
   *
   * @param routedServices to validate
   */
  private void validate(RoutedServices routedServices) {

    /* service network is needed */
    if(routedServices.getParentNetwork() == null) {
      throw new PlanItRunTimeException("No service network presented for routed services");
    }

    /* currently we only support macroscopic infrastructure networks */
    if(!(routedServices.getParentNetwork().getParentNetwork() instanceof MacroscopicNetwork)) {
      throw new PlanItRunTimeException("Currently the GeometryRoutedServicesWriter only supports parent networks that are macroscopic infrastructure networks, the provided network is not of this type");
    }
  }

  /**
   * Prepare for persistence
   *
   * @param routedServices to prepare persistence for
   */
  private void initialiseWrite(RoutedServices routedServices) {
    getComponentIdMappers().populateMissingIdMappers(getIdMapperType());
    layerPrefixProducer =
        (RoutedServicesLayer l) ->
            String.join("_", "layer", getPrimaryIdMapper().getRoutedServiceLayerIdMapper().apply(l));

    prepareCoordinateReferenceSystem(
        routedServices.getParentNetwork().getCoordinateReferenceSystem(), getSettings().getDestinationCoordinateReferenceSystem(), getSettings().getCountry());
  }

  /**
   * Construct consistent file path (with file name) based on desired output file name and settings configuration, taking the
   * current layer into account
   *
   * @param layer to use
   * @param layerMode to use
   * @param outputFileName vanilla output name
   * @return created path
   */
  private Path createFullPathFromFileName(RoutedServicesLayer layer, Mode layerMode, String outputFileName){
    return Path.of(
        getSettings().getOutputDirectory(),
        createLayerModeAwareBaseFileName(layer, layerMode, outputFileName) + getSettings().getFileExtension());
  }

  /**
   * Construct combination of base file name supplemented with the layer and layer mode
   *
   * @param layer to use
   * @param layerMode to use
   * @param outputFileName vanilla output name
   * @return 'layer_prefix'_'layer_id'_'mode'_'modeid'_outputFileName'
   */
  private String createLayerModeAwareBaseFileName(RoutedServicesLayer layer, Mode layerMode, String outputFileName){
    return String.join("_",
        layerPrefixProducer.apply(layer),
        "mode",
        getPrimaryIdMapper().getModeIdMapper().apply(layerMode),
        outputFileName);
  }

  /**
   * Based on the settings construct the correct mapping between file names and the routed services' PLANit entities
   *
   * @param settings to use
   * @return mapping between PLANit entity class and the chosen file name
   */
  private Map<Class<?>, String> extractRoutedServicesPlanitEntityBaseFileNames(
      RoutedServicesLayer layer, Mode layerMode, GeometryRoutedServicesWriterSettings settings) {
    return Map.ofEntries(
        entry(RoutedService.class, createLayerModeAwareBaseFileName(layer, layerMode, settings.getServicesFileName())),
        entry(RoutedTripSchedule.class, createLayerModeAwareBaseFileName(layer, layerMode, settings.getTripsScheduleFileName())),
        entry(RoutedTripFrequency.class, createLayerModeAwareBaseFileName(layer, layerMode, settings.getTripsFrequencyFileName()))
    );
  }

  /**
   * Find data store to use, if not present, create it if possible
   *
   * @param featureContext to create data store for and register on GeoIODataStoreManager
   * @param mode mode specific version of the same data store feature (but in different location for entries of that mode)
   * @param fullOutputPath on where to store results
   * @return dataStore to use
   *
   * @param <TT> type of PLANit entity the data store is to be used for
   */
  protected <TT extends ManagedId> DataStore findDataStore(
      PlanitEntityFeatureTypeContext<TT> featureContext, Mode mode, Path fullOutputPath){
    /* data store, e.g., underlying shape file(s) */
    DataStore modeAwareDataStore = GeoIODataStoreManager.getDataStore(
        featureContext.getPlanitEntityClass(), mode);
    if(modeAwareDataStore == null) {
      modeAwareDataStore = GeoIODataStoreManager.createDataStore(
          featureContext.getPlanitEntityClass(),
          mode,
          fullOutputPath);
    }
    return modeAwareDataStore;
  }

  /**
   * Writer the services of the layer mode combination
   *
   * @param layer          to persist services for
   * @param layerMode      to persist services for
   * @param featureType     to use
   * @param featureDescription the context to convert instances to features
   * @param layerLogPrefix to use
   */
  protected void writeRoutedServicesLayerServices(
      RoutedServicesLayer layer, Mode layerMode, SimpleFeatureType featureType, PlanitRoutedServiceFeatureTypeContext featureDescription, String layerLogPrefix) {

    if(featureType==null || featureDescription == null){
      throw new PlanItRunTimeException("No Feature type description available for PLANit routed services - services, this shouldn't happen");
    }
    var servicesByMode = layer.getServicesByMode(layerMode);
    LOGGER.info(String.format("%s Services (mode: %s): %d", layerLogPrefix, layerMode.getIdsAsString(), servicesByMode.size()));

    /* data store, e.g., underlying shape file(s) */
    DataStore dataStore = findDataStore(
        featureDescription, layerMode, createFullPathFromFileName(layer, layerMode, getSettings().getServicesFileName()));

    /* the feature writer through which to provide each result row */
    final var schemaName = createLayerModeAwareBaseFileName(layer, layerMode, getSettings().getServicesFileName());

    /* perform persistence */
    writeGeometryLayerForEntity(featureType, featureDescription, layerLogPrefix, dataStore, schemaName, servicesByMode);
  }

  /**
   * Write layers of the routed services
   *
   * @param routedServices to write layers for
   */
  protected void writeLayers(RoutedServices routedServices) {

    /* Ensure all geo features are available and configured for the correct CRS once we start using them */
    for( var layer : routedServices.getLayers()) {

      String layerLogPrefix = LoggingUtils.surroundwithBrackets(String.join(" ",
          "layer:",getPrimaryIdMapper().getRoutedServiceLayerIdMapper().apply(layer)));

      for (var layerMode : layer.getSupportedModes()) {

        if(layer.isServicesByModeEmpty(layerMode)){
          continue;
        }

        var supportedFeatures =
            GeoIoFeatureTypeBuilder.createRoutedServicesLayerFeatureContexts(
                getPrimaryIdMapper(), layerMode, getComponentIdMappers().getServiceNetworkIdMapper(), getDestinationCrsTransformer());

        /* feature types per layer */
        var geoFeatureTypesByPlanitEntity =
            GeoIoFeatureTypeBuilder.createSimpleFeatureTypes(
                supportedFeatures,
                getDestinationCoordinateReferenceSystem(),
                extractRoutedServicesPlanitEntityBaseFileNames(layer, layerMode, getSettings()));

        /* services */
        if(getSettings().isPersistServices()) {
          LOGGER.info(String.format("%sPersisting services to: %s",
              layerLogPrefix, createFullPathFromFileName(layer, layerMode, getSettings().getServicesFileName()).toAbsolutePath()));
          var featureInfo = findFeaturePairForPlanitEntity(RoutedService.class, geoFeatureTypesByPlanitEntity);
          writeRoutedServicesLayerServices(
              layer, layerMode, featureInfo.first(), (PlanitRoutedServiceFeatureTypeContext) featureInfo.second(), layerLogPrefix);
        }

        //todo: trips schedules

        //todo: trips frequencies
      }
    }
  }

  /** Constructor
   *
   * @param outputPath to persist in
   * @param countryName to optimise projection for (if available, otherwise ignore)
   */
  protected GeometryRoutedServicesWriter(String outputPath, String countryName) {
    super();
    this.settings = new GeometryRoutedServicesWriterSettings(outputPath, countryName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServicesIdMapper getPrimaryIdMapper() {
    return getComponentIdMappers().getRoutedServicesIdMapper();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(RoutedServices routedServices) {

    validate(routedServices);

    /* initialise */
    initialiseWrite(routedServices);

    /* logging */
    getSettings().logSettings();

    /* perform actual persistence */
    writeLayers(routedServices);

    /* disposes of any registered data stores */
    GeoIODataStoreManager.reset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    GeoIODataStoreManager.reset();
  }  
  
  // GETTERS/SETTERS
  
  /**
   * {@inheritDoc}
   */
  @Override
  public GeometryRoutedServicesWriterSettings getSettings() {
    return this.settings;
  }

}
