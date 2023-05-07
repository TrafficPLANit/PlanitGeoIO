package org.goplanit.geoio.util;

import org.goplanit.utils.misc.Pair;

import java.util.List;

/**
 * Track contextual relevant information for each PLANit entity that is persisted
 *
 * @author markr
 */
public class PlanitEntityFeatureTypeContext {

  /** the PLANit entity this instance pertains to */
  private final Class<?> planitEntityClass;

  /** feature description in attribute value pair combinations */
  private final List<Pair<String,String>> geoFeatureDescription;

  /**
   * Constructor
   *
   * @param clazz to use
   * @param geoFeatureDescription to use
   */
  protected PlanitEntityFeatureTypeContext(
          final Class<?> clazz, final List<Pair<String,String>> geoFeatureDescription){
    this.planitEntityClass = clazz;
    this.geoFeatureDescription = geoFeatureDescription;
  }

  /**
   * Factory method
   *
   * @param clazz to use
   * @param geoFeatureDescription to use
   * @return created instance
   */
  public static PlanitEntityFeatureTypeContext of(
          Class<?> clazz, List<Pair<String,String>> geoFeatureDescription){
    return new PlanitEntityFeatureTypeContext(clazz, geoFeatureDescription);
  }

  public Class<?> getPlanitEntityClass() {
    return planitEntityClass;
  }

  public List<Pair<String, String>> getGeoDescription() {
    return geoFeatureDescription;
  }
}
