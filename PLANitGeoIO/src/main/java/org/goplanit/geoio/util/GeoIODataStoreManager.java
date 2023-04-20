package org.goplanit.geoio.util;

import org.apache.commons.io.FilenameUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FileDataStoreFinder;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.misc.UrlUtils;
import org.opengis.feature.type.FeatureType;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Utility class that manages data store connections and related functionality
 */
public final class GeoIODataStoreManager {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(GeoIODataStoreManager.class.getCanonicalName());

  /** Track datastores per type of PLANit entity that we might persist by their most specific class signature */
  private static final Map<Class<?>, DataStore> dataStoreMap = new HashMap<>();

  /**
   * Collect a registered datastore for a given PLANit entity class, if not available a run time exception will be thrown
   *
   * @param dataStoreReferenceClass the reference class, i.e., PLANit entity types the datastore persists
   * @return the datastore
   */
  public static DataStore getDataStore(Class<?> dataStoreReferenceClass){
    if(!dataStoreMap.containsKey(dataStoreReferenceClass)){
      throw new PlanItRunTimeException("Unable to locate datastore for provided class %s, abort",dataStoreReferenceClass.toString());
    }
    return dataStoreMap.get(dataStoreReferenceClass);
  }

  /**
   * For a given PLANit entity that we persist to file, we track its datastore here to avoid overhead of recreating it
   *
   * @param dataStoreReferenceClass the reference class, i.e., PLANit entity types the datastore persists
   * @param outputFileNameWithPath the output file path to persist to
   * @return the datastore
   */
  public static DataStore createDataStore(Class<?> dataStoreReferenceClass, Path outputFileNameWithPath){
    if(dataStoreMap.containsKey(dataStoreReferenceClass)){
      LOGGER.severe(String.format("Datastore for class %s already registered, ignoring this call, providing existing datastore", dataStoreReferenceClass.toString()));
      return dataStoreMap.get(dataStoreReferenceClass);
    }

    /* factory based on extension, implicit file type choice */
    var factory = FileDataStoreFinder.getDataStoreFactory(FilenameUtils.getExtension(outputFileNameWithPath.toAbsolutePath().toString()));
    if(factory == null){
      LOGGER.severe(String.format("Unable to create file data store factory for geo extension %s",FilenameUtils.getExtension(outputFileNameWithPath.toAbsolutePath().toString())));
    }
    Map map = Collections.singletonMap( "url", UrlUtils.createFromPath(outputFileNameWithPath));

    try {
      DataStore theDataStore = factory.createNewDataStore(map);
      dataStoreMap.put(dataStoreReferenceClass,theDataStore);
      return theDataStore;
    }catch (Exception e){
      LOGGER.severe("Cause: "+ (e.getMessage()));
      throw new PlanItRunTimeException("Unable to create new datastore for class: "+ dataStoreReferenceClass.toString(), e.getCause());
    }
  }


  /**
   * Reset the manager and remove any registered data stores
   */
  public static void reset(){
    dataStoreMap.values().forEach(ds -> ds.dispose());
    dataStoreMap.clear();
  }

}
