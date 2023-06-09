package org.goplanit.geoio.util;

import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.misc.Triple;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Track contextual relevant information for each PLANit entity that is persisted
 *
 * @param <T> type of planit entity
 * @author markr
 */
public class PlanitEntityFeatureTypeContext<T> {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitEntityFeatureTypeContext.class.getCanonicalName());

  /** the PLANit entity this instance pertains to */
  private final Class<T> planitEntityClass;

  /** feature description in attribute value function mapping combinations */
  private ArrayList<Triple<String,String, Function<T,? extends Object>>> geoFeatureDescription;

  /** append one or more additional entries to the description
   *
   * @param featureDescriptionEntries to append
   */
  protected void appendToFeatureTypeDescription(
      Triple<String,String, Function<T,? extends Object>>... featureDescriptionEntries){
    for(var entry : featureDescriptionEntries){
      geoFeatureDescription.add(entry);
    }
  }

  /**
   * Constructor
   *
   * @param clazz to use
   * @param geoFeatureDescription to use
   */
  protected PlanitEntityFeatureTypeContext(
          final Class<T> clazz,
          Collection<Triple<String,String, Function<T, ? extends Object>>> geoFeatureDescription){
    this.planitEntityClass = clazz;
    this.geoFeatureDescription = new ArrayList<>(geoFeatureDescription);
  }

  /** geotools attribute key for default geometry attribute as used here across entities */
  public static final String DEFAULT_GEOMETRY_ATTRIBUTE_KEY = "*geom";

  public Class<T> getPlanitEntityClass() {
    return planitEntityClass;
  }

  public List<Triple<String,String, Function<T, ? extends Object>>> getAttributeDescription() {
    return geoFeatureDescription;
  }

  /**
   * Access to default geometry attribute key as used
   *
   * @return attribute key used
   */
  public String getDefaultGeometryAttributeKey(){
    return DEFAULT_GEOMETRY_ATTRIBUTE_KEY;
  }

  /**
   * Given a type of JTS geometry in class form, provide the crresponding string representation for
   * GIS based persistence
   *
   * @param geometryClazz to get string representation for
   * @return string representation if supported, otherwise throw exception
   * @param <T> type of geometry
   */
  public static <T extends Geometry> String getGisTypeFromJtsGeometryClass(Class<T> geometryClazz) {
    if (geometryClazz.equals(Point.class)) {
      return "Point";
    }
    if (geometryClazz.equals(LineString.class)) {
      return "LineString";
    }
    PlanItRunTimeException.throwNew("Geometry type %s not yet added as GIS geometry type, please add, aborting", geometryClazz.getCanonicalName());
    return "";
  }

}
