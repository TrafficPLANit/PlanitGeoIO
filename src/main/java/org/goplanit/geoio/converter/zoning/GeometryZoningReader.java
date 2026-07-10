package org.goplanit.geoio.converter.zoning;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.goplanit.converter.BaseReaderImpl;
import org.goplanit.converter.zoning.ZoningReader;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitSimpleFeatureUtils;
import org.goplanit.utils.geo.SimpleShapeFileParser;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.Zone;
import org.goplanit.zoning.Zoning;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

import java.util.List;
import java.util.logging.Logger;

/**
 * Parse Zoning from a shape or geopackage format such that it informs the zoning both spatially and through an id
 * field that can be used to relate activities to a location area.
 * TODO: currently only support shape not geopackages, and only supports parsing OD zones and their outer geometry
 *  (no points, nor centroids)
 *
 * @author markr
 *
 */
public class GeometryZoningReader extends BaseReaderImpl<Zoning> implements ZoningReader {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(GeometryZoningReader.class.getCanonicalName());

  /** the settings the user can configure for parsing ActivitySim compatible zones */
  private final GeometryZoningReaderSettings zoningReaderSettings;

  /** reference network to use */
  private final MacroscopicNetwork referenceNetwork;

  // references

  /** zoning to populate */
  private Zoning zoningToPopulate;

  /**
   * Extract zones from chosen gis layer
   *
   * @param type type of features in layer
   * @param zoneFeatures the layer to extract from
   */
  private void extractZonesFromGisLayer(SimpleFeatureType type, List<SimpleFeature> zoneFeatures) {

    if(!PlanitSimpleFeatureUtils.hasGeometryType(type, Polygon.class, MultiPolygon.class)){
      LOGGER.warning("Expected polygon-like feature type for zoning layer, but not found, abort");
    }

    var zoneIdMapperType = getSettings().getIdMapperType();

    int missingZoneIdFieldCounter = 0;
    for(var zoneFeature : zoneFeatures){
      /* ZONE */
      final OdZone zone = zoningToPopulate.getOdZones().getFactory().registerNew();

      String zoneSourceId =
          PlanitSimpleFeatureUtils.readAsNormalizedString(zoneFeature, getSettings().getZoneIdField());
      if(zoneSourceId == null && missingZoneIdFieldCounter < 10){
        LOGGER.severe("Found zone feature with missing zone Id field, ignored");
        ++missingZoneIdFieldCounter;
        continue;
      }else if(missingZoneIdFieldCounter == 10){
        LOGGER.severe("Stopping logging on zone features with missing zone Id field, auto-ignoring all");
      }

      if(zoneIdMapperType.equals(IdMapperType.XML)){
        // sync source -> xml and external
        // sync internal -> nothing
        zone.setXmlId(zoneSourceId);
      }else if(zoneIdMapperType.equals(IdMapperType.EXTERNAL_ID)){
        // sync source -> external
        // sync internal -> xml
        zone.setXmlId(zone.getId());
      }else{
        throw new PlanItRunTimeException("Unsupported zone id mapping");
      }
      zone.setExternalId(zoneSourceId);

      if (zoneFeature.getDefaultGeometry() instanceof Geometry) {
        zone.setGeometry((Geometry) zoneFeature.getDefaultGeometry());
      }else{
        LOGGER.severe(String.format(
            "Expect Zone (%s) to have a geometry, but found none, ignored", zone.getIdsAsString()));
      }

      registerBySourceId(Zone.class, zone);
    }


  }

  /**
   * Conduct the parsing
   */
  private void readZonesFromInput() {

    // parse raw geometries -- for now this only supports shape file, but we should extend to geopackages to make it
    // more widely appealing
    var geometriesByLayer = SimpleShapeFileParser.parseShapeFileAsJtsGeometries(
        getSettings().getInputSource(), getSettings().getGisFilter(), false);

    final String layerName = getSettings().getZoneLayerName() ;

    var gisLayerWithZones = geometriesByLayer.get(layerName).second();
    if(gisLayerWithZones == null || gisLayerWithZones.isEmpty()){
      LOGGER.warning(String.format("Given layer (%s) does not exist or is empty, abort", layerName));
      return;
    }

    // convert each to a zone based on settings
    extractZonesFromGisLayer(geometriesByLayer.get(layerName).first(), gisLayerWithZones);
  }

  /**
   * Log some information about this reader's configuration
   */
  private void logInfo() {
    getSettings().logSettings();
  }

  /**
   * Log stats from parsing
   */
  private void logParsingStats() {
    LOGGER.info("----------- Parsing stats -------------------");
    LOGGER.info(String.format("%-40s: %s", "Parsed number of (OD) zones",
        zoningToPopulate.getOdZones().size()));
    LOGGER.info(String.format("%-40s: %s", "Parsed number of (transfer) zones",
        zoningToPopulate.getTransferZones().size()));
    LOGGER.info(String.format("%-40s: %s", "Parsed number of transfer zone groups",
        zoningToPopulate.getTransferZoneGroups().size()));
    LOGGER.info(String.format("%-40s: %s", "Parsed number of OD connectoids",
        zoningToPopulate.getOdConnectoids().size()));
    LOGGER.info(String.format("%-40s: %s", "Parsed number of transfer connectoids",
        zoningToPopulate.getTransferConnectoids().size()));
  }

  /**
   * Validate settings
   */
  protected void validate(){
    PlanItRunTimeException.throwIfNull(getSettings().getSourceCrs(),
        "Input CRS not set for geo zoning reader, unable to proceed");
    PlanItRunTimeException.throwIfNull(getSettings().getInputSource(),
        "Input source not set for geo zoning reader, unable to proceed");
    PlanItRunTimeException.throwIfNull(getSettings().getZoneLayerName(),
        "Layer name of input source not set, unable to proceed");
    PlanItRunTimeException.throwIfNull(getSettings().getZoneIdField(),
        "Zone id field of layer in Input source not set for geo zoning reader, unable to proceed");
    PlanItRunTimeException.throwIf(getSettings().getIdMapperType().equals(IdMapperType.ID),
        "cannot map ids to PLANit internal id, supporting only XML or EXTERNAL mapping currently");
  }

  /**
   * Constructor
   *
   * @param settings to use
   * @param referenceNetwork can be a dummy, but PLANit require a chain and id hierarchy
   * @param zoningToPopulate zoning to populate
   */
  protected GeometryZoningReader(
      GeometryZoningReaderSettings settings,
      MacroscopicNetwork referenceNetwork,
      Zoning zoningToPopulate){
    this.zoningReaderSettings = settings;
    this.referenceNetwork = referenceNetwork;
    this.zoningToPopulate = zoningToPopulate;
  }


  /**
   * Parse the input source and convert it into a PLANit Zoning instance given the configuration options
   * that have been set
   *
   * @return macroscopic zoning that has been parsed
   */
  @Override
  public Zoning read() {

    validate();

    logInfo();

    initialiseIdTrackers();

    readZonesFromInput();

    /* log stats */
    logParsingStats();

    /* return parsed zoning */
    return this.zoningToPopulate;
  }


  /**
   * initialise the id trackers and populate them,
   * so we can lay indices on the source id as well for quick lookups
   *
   */
  private void initialiseIdTrackers() {
    if(getSettings().getIdMapperType().equals(IdMapperType.XML)) {
      initialiseSourceIdMap(Zone.class, Zone::getXmlId);
    }else if(getSettings().getIdMapperType().equals(IdMapperType.EXTERNAL_ID)){
      initialiseSourceIdMap(Zone.class, Zone::getExternalId);
    }
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    /* free memory */
  }

  /**
   * Collect the settings which can be used to configure the reader
   *
   * @return the settings
   */
  @Override
  public GeometryZoningReaderSettings getSettings() {
    return zoningReaderSettings;
  }

  /**
   * Access to reference network
   * @return reference network
   */
  public MacroscopicNetwork getReferenceNetwork(){
    return referenceNetwork;
  }

}
