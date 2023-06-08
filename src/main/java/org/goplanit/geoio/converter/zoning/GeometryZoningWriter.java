package org.goplanit.geoio.converter.zoning;

import org.geotools.data.DataStore;
import org.goplanit.converter.idmapping.ZoningIdMapper;
import org.goplanit.converter.zoning.ZoningWriter;
import org.goplanit.geoio.converter.GeometryIoWriter;
import org.goplanit.geoio.converter.zoning.featurecontext.PlanitZoneFeatureTypeContext;
import org.goplanit.geoio.util.GeoIODataStoreManager;
import org.goplanit.geoio.util.GeoIoFeatureTypeBuilder;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.TransferZone;
import org.goplanit.utils.zoning.Zone;
import org.goplanit.utils.zoning.Zones;
import org.goplanit.zoning.Zoning;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

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

  /** validate before commencing actual write
   *
   * @param zoning to validate
   */
  private void validate(Zoning zoning) {
    //todo
  }

  /**
   * Construct combinatino of base file name supplemented with the geometry type incase multiple geometry types exist for the same PLANit entity
   *
   * @param outputFileName vanilla output name
   * @param geometryType geometry type
   * @return 'outputFileName_'geometryType'
   */
  private static String createGeometryAwareBaseFileName(String outputFileName, Class<? extends Geometry> geometryType){
    return String.join("_",outputFileName,geometryType.getSimpleName().toLowerCase());
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
        zones,
        Z::getGeometry);

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

      LOGGER.info(String.format("%sPersisting %s entities to: %s",
          zoneClazz.getSimpleName(), createFullPathFromFileName(zoneFileName, geometryType).toAbsolutePath()));

      /* zoning feature context is created per combination of zone and geometry type since shape files are only support one
       * type of geometry per file */
      var featureContext =
          GeoIoFeatureTypeBuilder.createZoningFeatureContext(getPrimaryIdMapper(), zoneClazz, geometryType);

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
   * Write the Zoning entities eligible for persistence
   *
   * @param zoning to persist
   */
  private void writeEntities(Zoning zoning) {

    if(getSettings().isPersistOdZones()) {
      writeZones(zoning.getOdZones(), OdZone.class, getSettings().getOdZonesFileName());
    }

    if(getSettings().isPersistTransferZones()) {
      writeZones(zoning.getTransferZones(), TransferZone.class, getSettings().getTransferZonesFileName());
    }
    //todo: other entities
  }


  /**
   * Place zones in separate collections based on the underlying geometry types
   *
   * @param zones to partition
   * @return partitioned by type of geometry
   * @param <Z> type of zone
   */
  private <Z extends Zone> SortedMap<Class<? extends Geometry>, SortedSet<Z>> partitionByGeometry(Zones<Z> zones) {
    var result = new TreeMap<Class<? extends Geometry>, SortedSet<Z>>();
    for(var zone : zones) {
      var geometryClazz = zone.getGeometry().getClass();
      var zonesOfGeometryType = result.get(geometryClazz);
      if(zonesOfGeometryType == null){
        zonesOfGeometryType = new TreeSet<>();
        result.put(geometryClazz, zonesOfGeometryType);
      }
      zonesOfGeometryType.add(zone);
    }
    return result;
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
