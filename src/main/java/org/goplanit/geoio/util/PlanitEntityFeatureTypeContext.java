package org.goplanit.geoio.util;

import org.goplanit.utils.misc.Triple;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Track contextual relevant information for each PLANit entity that is persisted
 *
 * @param <T> type of planit entity
 * @author markr
 */
public class PlanitEntityFeatureTypeContext<T> {

  /** the PLANit entity this instance pertains to */
  private final Class<T> planitEntityClass;

  /** feature description in attribute value function mapping combinations */
  private final List<Triple<String,String, Function<T,? extends Object>>> geoFeatureDescription;

  /**
   * Constructor
   *
   * @param clazz to use
   * @param geoFeatureDescription to use
   */
  protected PlanitEntityFeatureTypeContext(
          final Class<T> clazz,
          final List<Triple<String,String, Function<T, ? extends Object>>> geoFeatureDescription){
    this.planitEntityClass = clazz;
    this.geoFeatureDescription = geoFeatureDescription;
  }

  public Class<T> getPlanitEntityClass() {
    return planitEntityClass;
  }

  public List<Triple<String,String, Function<T, ? extends Object>>> getAttributeDescription() {
    return geoFeatureDescription;
  }
}
