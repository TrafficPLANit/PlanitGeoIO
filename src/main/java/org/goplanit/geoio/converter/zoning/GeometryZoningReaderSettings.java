package org.goplanit.geoio.converter.zoning;

import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.factory.CommonFactoryFinder;
import org.goplanit.converter.ConverterReaderSettings;
import org.goplanit.utils.id.IdMapperType;

import java.util.logging.Logger;

/**
 * ActivitySim zoning reader settings. Serves to provide a spatial representation for the zones
 */
public class GeometryZoningReaderSettings implements ConverterReaderSettings {

  private static final Logger LOGGER = Logger.getLogger(GeometryZoningReaderSettings.class.getCanonicalName());

  /** location of the zoning system GIS entity to parse that is expected to contain the geometries and zoning mapping
   * to match the ActivitySim demand
   */
  protected String inputSource;

  /** the source CRS to inform any required projection/transformation to match the destination CRS
   * (if any) */
  protected CoordinateReferenceSystem sourceCrs;

  /** the name of the layer containing the zones */
  protected String zoneLayerName = null;

  /** the name of the field containing the id reflecting the zone id */
  protected String zoneIdField = null;

  /** gis filter to apply to the parsing of zones to make it mroe selective */
  protected Filter gisFilter;

  /** determines how we map ids, default is EXTERNAL, so we are guaranteed that we obtain internal ids that are
   * contiguous and number based
   */
  protected IdMapperType idMapper = IdMapperType.EXTERNAL_ID;

  /**
   * Access to filter factory to construct filters on spatial zone parsing
   *
   * @return filter factory
   */
  public static FilterFactory getFilterFactory(){
    return CommonFactoryFinder.getFilterFactory();
  }


  /** Default constructor
   *
   */
  public GeometryZoningReaderSettings(){
    this(null, null);
  }

  /**
   * Constructor
   *
   * @param inputSource to use
   * @param sourceCrs related to the zoning Uri content
   */
  public GeometryZoningReaderSettings(final String inputSource, final CoordinateReferenceSystem sourceCrs){
    this.inputSource = inputSource;
    this.sourceCrs = sourceCrs;
  }

  public String getInputSource() {
    return inputSource;
  }

  public void setInputSource(String inputSource) {
    this.inputSource = inputSource;
  }

  public CoordinateReferenceSystem getSourceCrs() {
    return sourceCrs;
  }

  public void setSourceCrs(CoordinateReferenceSystem sourceCrs) {
    this.sourceCrs = sourceCrs;
  }

  /**
   * get layer name
   * @return zoneLayerName
   */
  public String getZoneLayerName() {
    return zoneLayerName;
  }

  /**
   * set layer name
   * @param zoneLayerName to use
   */
  public void setZoneLayerName(String zoneLayerName) {
    this.zoneLayerName = zoneLayerName;
  }

  public String getZoneIdField() {
    return zoneIdField;
  }

  public void setZoneIdField(String zoneIdField) {
    this.zoneIdField = zoneIdField;
  }


  /**
   * Set a filter on the parsing of zones gis layer
   *
   * @param gisFilter to apply
   */
  public void setGisFilter(Filter gisFilter) {
    this.gisFilter = gisFilter;
  }

  /**
   * get filter
   *
   * @return gisFilter
   */
  public Filter getGisFilter() {
    return this.gisFilter;
  }

  /**
   * Indicates how we map the ids when reading (to planit internal ids, XML ids, or external ids)
   *
   * @return idMapper
   */
  public IdMapperType getIdMapperType() {
    return idMapper;
  }

  /**
   * Indicates how we map the ids when reading (to planit internal ids, XML ids, or external ids)
   *
   * @param idMapper to apply
   */
  public void setIdMapperType(IdMapperType idMapper) {
    this.idMapper = idMapper;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    inputSource = null;
    sourceCrs = null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void logSettings() {
    LOGGER.info(String.format("%-40s: %s", "Input source", getInputSource()));
    LOGGER.info(String.format("%-40s: %s", "Input source CRS", getSourceCrs().getName()));
    LOGGER.info(String.format("%-40s: %s", "Layer name", getZoneLayerName()));
    LOGGER.info(String.format("%-40s: %s", "Layer Zone id field name", getZoneIdField()));
    LOGGER.info(String.format("%-40s: %s", "GIS zone filter", getGisFilter()));
    LOGGER.info(String.format("%-40s: %s", "Id mapping set to", getIdMapperType()));
  }

}
