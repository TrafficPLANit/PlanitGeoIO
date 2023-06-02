package org.goplanit.geoio.util;

import org.apache.commons.io.FilenameUtils;
import org.geotools.data.DataStore;
import org.geotools.data.FileDataStoreFinder;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.misc.UrlUtils;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.IOException;
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
   * Collect a registered datastore for a given PLANit entity class, if not available null is returned
   *
   * @param dataStoreReferenceClass the reference class, i.e., PLANit entity types the datastore persists
   * @return the datastore
   */
  public static DataStore getDataStore(Class<?> dataStoreReferenceClass){
    if(!dataStoreMap.containsKey(dataStoreReferenceClass)){
      return null;
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
    String fileType = FilenameUtils.getExtension(outputFileNameWithPath.toAbsolutePath().toString());
    var factory = FileDataStoreFinder.getDataStoreFactory(fileType);
    if(factory == null){
      LOGGER.severe(String.format("Unable to create file data store factory for geo extension %s",FilenameUtils.getExtension(outputFileNameWithPath.toAbsolutePath().toString())));
    }
    Map map = Collections.singletonMap( "url", UrlUtils.createFromLocalPath(outputFileNameWithPath));

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

  /**
   * Given a feature, register it on the datastore if not already available
   *
   * @param dataStore to register on
   * @param feature feature to register
   */
  public static void registerFeatureOnDataStore(DataStore dataStore, SimpleFeatureType feature) {
    PlanItRunTimeException.throwIfNull(feature, "Feature type null, unable to register on datastore, this shouldn't happen");

    try{
      /* trigger exception when not available to register schema once */
      var alreadyAvailable = dataStore.getSchema(feature.getName());
      if(alreadyAvailable != null){
        LOGGER.info(String.format("OVERWRITE datastore for feature %s already present, overwriting", feature.getTypeName()));
        dataStore.removeSchema(feature.getTypeName());
        dataStore.getSchema(feature.getTypeName());
        return;
      }
    }catch (Exception e){
      /* configure the datastore for the chosen feature type schema, so it can be populated */
      try {
        dataStore.createSchema(feature);
      } catch (IOException ex) {
        LOGGER.severe(ex.getMessage());
        throw new PlanItRunTimeException("Unable to register schema on datastore");
      }
    }
  }
}
