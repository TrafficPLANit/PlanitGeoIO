package org.goplanit.geoio.util;

import org.geotools.api.data.DataStore;

import java.io.IOException;
import java.util.logging.Logger;

public class DataStoreUtils {

  private static final Logger LOGGER = Logger.getLogger(DataStoreUtils.class.getCanonicalName());

  /** the geotools geometry attribute name used */
  public static final String DEFAULT_GEOMETRY_ATTRIBUTE = "the_geom";

  /**
   * Find the name used to describe the geometry for a row
   *
   * @param entityDataStore to use
   * @param layerName to get descriptor for
   * @return geometry column name
   */
  public static String getDataStoreGeometryAttributeDescriptor(
          DataStore entityDataStore, String layerName){

    try{
      var schema = entityDataStore.getSchema(layerName);
      return schema.getGeometryDescriptor().getLocalName();
    }catch(IOException e){
      LOGGER.severe("Unable to determine geometry attribute name for layer"  + layerName +
              "reverting to default backup" + DEFAULT_GEOMETRY_ATTRIBUTE);
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
    }
    return DEFAULT_GEOMETRY_ATTRIBUTE;
  }
}
