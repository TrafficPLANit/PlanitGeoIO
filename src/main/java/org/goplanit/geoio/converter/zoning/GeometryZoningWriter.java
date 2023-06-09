package org.goplanit.geoio.converter.zoning;

import org.geotools.data.DataStore;
import org.goplanit.converter.idmapping.ZoningIdMapper;
import org.goplanit.converter.zoning.ZoningWriter;
import org.goplanit.geoio.converter.GeometryIoWriter;
import org.goplanit.geoio.converter.service.GeometryServiceNetworkWriterSettings;
import org.goplanit.geoio.converter.zoning.featurecontext.PlanitConnectoidFeatureTypeContext;
import org.goplanit.geoio.converter.zoning.featurecontext.PlanitDirectedConnectoidFeatureTypeContext;
import org.goplanit.geoio.converter.zoning.featurecontext.PlanitUndirectedConnectoidFeatureTypeContext;
import org.goplanit.geoio.converter.zoning.featurecontext.PlanitZoneFeatureTypeContext;
import org.goplanit.geoio.util.GeoIODataStoreManager;
import org.goplanit.geoio.util.GeoIoFeatureTypeBuilder;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.network.layer.service.ServiceLeg;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;
import org.goplanit.utils.network.layer.service.ServiceNode;
import org.goplanit.utils.network.virtual.VirtualNetwork;
import org.goplanit.utils.zoning.*;
import org.goplanit.zoning.Zoning;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

import static java.util.Map.entry;

/**
 * Writer to persist a PLANit zoning to disk in a geometry centric format such as Shape files. Id mapping default is
 * set to internal ids (not XML ids) by default
 * 
 * @author markr
 *
 */
public class GeometryZoningWriter extends GeometryIoWriter<Zoning> implements ZoningWriter {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(GeometryZoningWriter.class.getCanonicalName());

  /** origin crs of the zoning geometries */
  private final CoordinateReferenceSystem originCrs;

  /**
   * Based on the settings construct the correct mapping between file names and the zoning PLANit entities (that have a fixed
   * geometry type)
   *
   * @param settings to use
   * @return mapping between PLANit entity class and the chosen file name
   */
  private static Map<Class<?>, String> extractZoningPlanitEntitySchemaNames(GeometryZoningWriterSettings settings) {
    return Map.ofEntries(
        entry(DirectedConnectoid.class, settings.getTransferConnectoidsFileName()),
        entry(UndirectedConnectoid.class, settings.getOdZonesFileName())
    );
  }

  /**
   * Construct combination of base file name supplemented with the geometry type incase multiple geometry types exist for the same PLANit entity
   *
   * @param outputFileName vanilla output name
   * @param geometryType geometry type
   * @return 'outputFileName_'geometryType'
   */
  private static String createGeometryAwareBaseFileName(String outputFileName, Class<? extends Geometry> geometryType){
    return String.join("_",outputFileName,geometryType.getSimpleName().toLowerCase());
  }

  /**
   * Place zones in separate collections based on the underlying geometry types
   *
   * @param zones to partition
   * @return partitioned by type of geometry
   * @param <Z> type of zone
   */
  private <Z extends Zone> SortedMap<Class<? extends Geometry>, SortedSet<Z>> partitionByGeometry(Zones<Z> zones) {
    var result = new TreeMap<Class<? extends Geometry>, SortedSet<Z>>(Comparator.comparing( c -> c.getSimpleName()));
    for(var zone : zones) {
      var theGeometry = zone.getGeometry(true);
      if(theGeometry == null){
        LOGGER.warning(String.format("IGNORE Found PLANit zone (%s) without geometry", zone.getIdsAsString()));
        continue;
      }

      var geometryClazz = theGeometry.getClass();
      var zonesOfGeometryType = result.get(geometryClazz);
      if(zonesOfGeometryType == null){
        zonesOfGeometryType = new TreeSet<>();
        result.put(geometryClazz, zonesOfGeometryType);
      }
      zonesOfGeometryType.add(zone);
    }
    return result;
  }

  /**
   * Construct consistent file path (with file name) based on desired output file name and settings configuration, e.g.,
   * 'zones_od_point.shp', or 'zones_od_linestring.shp'
   *
   * @param outputFileName to use
   * @param geometryType to use, as the same output file (PLANit entity) can require multiple geometry types, so we must be spcific in
   *                     what the layer reflects
   * @return created path
   */
  private Path createFullPathFromFileName(String outputFileName, Class<? extends Geometry> geometryType){
    return Path.of(
        getSettings().getOutputDirectory(),
        createGeometryAwareBaseFileName(outputFileName, geometryType) + getSettings().getFileExtension());
  }

  /**
   * Construct consistent file path (with file name) based on desired output file name and file extension, e.g.,
   * 'connectoids_od.shp'
   *
   * @param outputFileName to use
   * @return created path
   */
  private Path createFullPathFromFileName(String outputFileName){
    return Path.of(getSettings().getOutputDirectory(),outputFileName + getSettings().getFileExtension());
  }

  /** validate before commencing actual write
   *
   * @param zoning to validate
   */
  private void validate(Zoning zoning) {
    //todo
  }

  /**
   * Initialise before actual writing starts. Called from {@link #write(Zoning)}
   *
   * @param zoning to writer
   */
  private void initialiseWrite(Zoning zoning) {
    getComponentIdMappers().populateMissingIdMappers(getIdMapperType());
//    layerPrefixProducer =
//            (UntypedDirectedGraphLayer<?,?,?> l) ->
//                    String.join("_", "layer", getPrimaryIdMapper().getServiceNetworkLayerIdMapper().apply( (ServiceNetworkLayer) l));

    prepareCoordinateReferenceSystem(this.originCrs, getSettings().getDestinationCoordinateReferenceSystem(), getSettings().getCountry());
  }

  /**
   * Writer the service nodes of the layer
   *
   * @param zones          to persist
   * @param featureType to use
   * @param zoneFeatureContext the context to convert instances to features
   * @param baseFileName for the zones to use
   */
  private <Z extends Zone> void  writeZonesForGeometryType(
      Collection<Z> zones,
      SimpleFeatureType featureType,
      PlanitZoneFeatureTypeContext<Z,?> zoneFeatureContext,
      String baseFileName) {
    if(featureType==null || zoneFeatureContext == null){
      throw new PlanItRunTimeException("No Feature type description available for PLANit zones of type %s, this shouldn't happen", zoneFeatureContext.getPlanitEntityClass().getSimpleName());
    }
    LOGGER.info(String.format("Zones (type: %s geometry: %s): %d",
        zoneFeatureContext.getPlanitEntityClass().getSimpleName(), zoneFeatureContext.getGeometryTypeClass().getSimpleName(), zones.size()));

    /* data store, e.g., underlying shape file(s) */
    DataStore zoneByGeometryTypeDataStore = GeoIODataStoreManager.getDataStore(
        zoneFeatureContext.getPlanitEntityClass(), zoneFeatureContext.getGeometryTypeClass());
    if(zoneByGeometryTypeDataStore == null) {
      zoneByGeometryTypeDataStore = GeoIODataStoreManager.createDataStore(
          zoneFeatureContext.getPlanitEntityClass(),
          zoneFeatureContext.getGeometryTypeClass(),
          createFullPathFromFileName(baseFileName, zoneFeatureContext.getGeometryTypeClass()));
    }

    /* perform persistence */
    writeGeometryLayerForEntity(
        featureType,
        zoneFeatureContext,
        "",
        zoneByGeometryTypeDataStore,
        createGeometryAwareBaseFileName(baseFileName, zoneFeatureContext.getGeometryTypeClass()),
        zones);
  }

  /**
   * Write directed connectoids of the zoning
   *
   * @param connectoids to persist
   * @param featureType to use
   * @param featureDescription to use
   * @param connectoidSchemaName to use
   */
  private <C extends Connectoid> void writeConnectoids(
      Iterable<C> connectoids, SimpleFeatureType featureType, PlanitConnectoidFeatureTypeContext<C> featureDescription, String connectoidSchemaName) {
    if(featureType==null || featureDescription == null){
      throw new PlanItRunTimeException("No Feature type description available for PLANit connectoids (%s), this shouldn't happen", featureDescription.getPlanitEntityClass().getSimpleName());
    }

    /* data store, e.g., underlying shape file(s) */
    DataStore legDataStore =
        findDataStore(featureDescription,  createFullPathFromFileName(connectoidSchemaName));

    /* perform persistence */
    writeGeometryLayerForEntity(
        featureType,
        featureDescription,
        legDataStore,
        connectoidSchemaName, /* schema name = file name */
        connectoids);
  }

  /**
   * Write zones container of the zoning
   *
   * @param zones to write
   * @param zoneClazz these zones pertain to
   * @param zoneFileName to use for persisting
   */
  protected <Z extends Zone> void writeZones(Zones<Z> zones, Class<Z> zoneClazz, String zoneFileName) {

    SortedMap<Class<? extends Geometry>, SortedSet<Z>> partitionedZones = partitionByGeometry(zones);

    /* Ensure all geo features are available and configured for the correct CRS once we start using them */
    for (var entry : partitionedZones.entrySet()) {
      var geometryType = entry.getKey();
      var zonesOfGeometryType = entry.getValue();

      LOGGER.info(String.format("Persisting %s entities to: %s",
          zoneClazz.getSimpleName(), createFullPathFromFileName(zoneFileName, geometryType).toAbsolutePath()));

      /* zoning feature context is created per combination of zone and geometry type since shape files are only support one
       * type of geometry per file */
      var featureContext =
          GeoIoFeatureTypeBuilder.createZoningZoneFeatureContext(getPrimaryIdMapper(), zoneClazz, geometryType);

      /* feature type */
      var zoneSimpleFeature =
          GeoIoFeatureTypeBuilder.createSimpleZoningFeatureType(
              featureContext,
              getDestinationCoordinateReferenceSystem(),
              createGeometryAwareBaseFileName(zoneFileName, geometryType));

      /* do actual persisting to datastore */
      writeZonesForGeometryType(zonesOfGeometryType, zoneSimpleFeature, featureContext, zoneFileName);
    }
  }

  /**
   * Write transfer connectoids of the zoning
   *
   * @param zoning to write directed connectoids for
   * @param featureType to use
   * @param featureDescription to use
   */
  protected void writeTransferConnectoids(Zoning zoning, SimpleFeatureType featureType, PlanitDirectedConnectoidFeatureTypeContext featureDescription) {
    LOGGER.info(String.format("Transfer connectoids: %d", zoning.getTransferConnectoids().size()));
    writeConnectoids(zoning.getTransferConnectoids(), featureType, featureDescription, getSettings().getTransferConnectoidsFileName());
  }

  /**
   * Write od connectoids of the zoning
   *
   * @param zoning to write directed connectoids for
   * @param featureType to use
   * @param featureDescription to use
   */
  protected void writeOdConnectoids(Zoning zoning, SimpleFeatureType featureType, PlanitUndirectedConnectoidFeatureTypeContext featureDescription) {
    LOGGER.info(String.format("OD connectoids: %d", zoning.getOdConnectoids().size()));
    writeConnectoids(zoning.getOdConnectoids(), featureType, featureDescription, getSettings().getOdConnectoidsFileName());
  }

  protected void writeVirtualNetwork(VirtualNetwork virtualNetwork) {
  }

  /**
   * Write the Zoning entities eligible for persistence
   *
   * @param zoning to persist
   */
  protected void writeEntities(Zoning zoning) {

    /* zones by geometry type are treated separately */
    {
      if(getSettings().isPersistOdZones() && zoning.hasOdZones()) {
        writeZones(zoning.getOdZones(), OdZone.class, getSettings().getOdZonesFileName());
      }

      if(getSettings().isPersistTransferZones() && zoning.hasTransferZones()) {
        writeZones(zoning.getTransferZones(), TransferZone.class, getSettings().getTransferZonesFileName());
      }
    }

    /* regular (single fixed geometry type per entity) PLANit entities */
    {
      var supportedFeatures =
          GeoIoFeatureTypeBuilder.createZoningFeatureContexts(
              getPrimaryIdMapper(), getComponentIdMappers().getNetworkIdMappers());

      /* feature types per layer */
      var geoFeatureTypesByPlanitEntity =
          GeoIoFeatureTypeBuilder.createSimpleFeatureTypes(
              supportedFeatures,
              getDestinationCoordinateReferenceSystem(),
              extractZoningPlanitEntitySchemaNames(getSettings()));

      if(getSettings().isPersistTransferConnectoids() && zoning.hasTransferConnectoids()) {
        LOGGER.info(String.format("Persisting transfer connectoids to: %s",
            createFullPathFromFileName(getSettings().getTransferConnectoidsFileName()).toAbsolutePath()));
        var featureInfo = findFeaturePairForPlanitEntity(DirectedConnectoid.class, geoFeatureTypesByPlanitEntity);
        writeTransferConnectoids(zoning, featureInfo.first(), (PlanitDirectedConnectoidFeatureTypeContext)featureInfo.second());
      }

      if(getSettings().isPersistTransferConnectoids() && zoning.hasOdConnectoids()) {
        LOGGER.info(String.format("Persisting OD connectoids to: %s",
            createFullPathFromFileName(getSettings().getOdConnectoidsFileName()).toAbsolutePath()));
        var featureInfo = findFeaturePairForPlanitEntity(UndirectedConnectoid.class, geoFeatureTypesByPlanitEntity);
        writeOdConnectoids(zoning, featureInfo.first(), (PlanitUndirectedConnectoidFeatureTypeContext)featureInfo.second());
      }
    }

    if(getSettings().isPersistVirtualNetwork()){
      if(zoning.getVirtualNetwork().isEmpty()){
        LOGGER.info("IGNORE: Virtual network is empty, consider constructing integrated PLANit TransportModeNetwork before persisting, so virtual network is not empty");
      }else{
        writeVirtualNetwork(zoning.getVirtualNetwork());
      }
    }

  }


  /** Constructor
   *
   * @param originCrs applied to zoning
   */
  protected GeometryZoningWriter(final CoordinateReferenceSystem originCrs) {
    this(".", CountryNames.GLOBAL, originCrs);
  }

  /** Constructor
   *
   * @param outputPath to persist zoning on
   * @param originCrs applied to zoning
   */
  protected GeometryZoningWriter(String outputPath, final CoordinateReferenceSystem originCrs) {
    this(outputPath, CountryNames.GLOBAL, originCrs);
  }

  /** Constructor
   *
   * @param outputPath to persist service zoning on
   * @param countryName to optimise projection for (if available, otherwise ignore)
   * @param originCrs applied to zoning
   */
  protected GeometryZoningWriter(String outputPath, String countryName, final CoordinateReferenceSystem originCrs) {
    this(new GeometryZoningWriterSettings(outputPath, countryName), originCrs);
  }


  /** Constructor
   *
   * @param zoningSettings to use
   */
  protected GeometryZoningWriter(GeometryZoningWriterSettings zoningSettings, final CoordinateReferenceSystem originCrs){
    super(zoningSettings);
    this.originCrs = originCrs;
  }

  /**
   * {@inheritDoc}
   */
//  @Override
  public void write(Zoning zoning) {

    validate(zoning);

    /* initialise */
    initialiseWrite(zoning);

    /* logging */
    getSettings().logSettings();

    /* perform actual persistence */
    writeEntities(zoning);

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
  public GeometryZoningWriterSettings getSettings() {
    return (GeometryZoningWriterSettings) super.getSettings();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ZoningIdMapper getPrimaryIdMapper() {
    return getComponentIdMappers().getZoningIdMappers();
  }
}
