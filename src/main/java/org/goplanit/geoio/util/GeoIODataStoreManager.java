package org.goplanit.geoio.util;

import org.geotools.api.data.DataStore;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitGeoDataStoreUtils;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.misc.UrlUtils;
import org.goplanit.utils.mode.Mode;
import org.locationtech.jts.geom.Geometry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

  /** Track datastores per type of PLANit entity (for which we have multiple entries differentiated by geometry type) that we might persist by their most specific class signature */
  private static final Map<Pair<Class<?>,Class<? extends Geometry>>, DataStore> dataStoreMapGeoType = new HashMap<>();

  /** Track datastores per type of PLANit entity (for which we have multiple entries differentiated by mode) that we might persist by their most specific class signature */
  private static final Map<Pair<Class<?>, Mode>, DataStore> dataStoreMapMode = new HashMap<>();

  /**
   * Create a datastore for the given file location
   *
   * @param format to create data store for
   * @param outputFileNameWithPath to use
   * @return data store created, null if not possible
   */
  protected static DataStore createSingleEntityTypeDataStore(GeoIoFormat format, Path outputFileNameWithPath){
    DataStore dataStore = null;
    try {
      // obtain full local path in case only relative path is provided
      var fullPath =
          UrlUtils.asLocalPath(
              UrlUtils.createFromLocalAbsoluteOrRelativePath(outputFileNameWithPath)).toAbsolutePath();
      // shape = file-based using a factory
      if(format == GeoIoFormat.SHAPE){

        // NOTE: relies on PLANitUtils - only certain formats are in the build of PLANitUtils. If it is not in that
        // build but it is in the build of PLANitGeoIO null will still be returned! --> solution add to PLANitUtils
        dataStore = PlanitGeoDataStoreUtils.findOrCreateFileDataStore(fullPath.toString());
      }
      // geopackage is database-based type that requires a different setup to get the datastore
      else if(format == GeoIoFormat.GEOPACKAGE){
        // issue https://github.com/TrafficPLANit/PlanitGeoIO/issues/9

        // remove geopackage if it already exists, then recreate it from scratch (avoid we're adding more to existing)
        Files.deleteIfExists(outputFileNameWithPath.toAbsolutePath());

        // NOTE: relies on PLANitUtils - only certain formats are in the build of PLANitUtils. If it is not in that
        // build but it is in the build of PLANitGeoIO null will still be returned! --> solution add to PLANitUtils
        dataStore = PlanitGeoDataStoreUtils.createFileDataBaseDataStore(
                // location             dbtype for GPKG
            fullPath.toString(), Pair.of("dbtype", "geopkg"));
      }
    }catch (Exception e){
      LOGGER.severe("Cause: "+ (e.getMessage()));
    }
    return dataStore;
  }

  /**
   * Collect a registered datastore for a given PLANit entity class (for which only a single geometry type exists),
   * if not available null is returned.
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
   * Collect a registered datastore for a given PLANit entity class and geometry type (in case multiple geometry types require multiple
   * stores with one store per type), if not available null is returned.
   *
   * @param dataStoreReferenceClass the reference class, i.e., PLANit entity types the datastore persists
   * @param geometryTypeClass for the reference class
   * @return the datastore
   */
  public static DataStore getDataStore(Class<?> dataStoreReferenceClass, Class<? extends Geometry> geometryTypeClass){
    var key = Pair.of(dataStoreReferenceClass, geometryTypeClass);
    if(!dataStoreMapGeoType.containsKey(key)){
      return null;
    }
    return dataStoreMapGeoType.get(key);
  }

  /**
   * Collect a registered datastore for a given PLANit entity class and mode (in case multiple modes require multiple
   * stores with one store per type), if not available null is returned.
   *
   * @param dataStoreReferenceClass the reference class, i.e., PLANit entity types the datastore persists
   * @param mode for the reference class
   * @return the datastore
   */
  public static DataStore getDataStore(Class<?> dataStoreReferenceClass, Mode mode){
    var key = Pair.of(dataStoreReferenceClass, mode);
    if(!dataStoreMapMode.containsKey(key)){
      return null;
    }
    return dataStoreMapMode.get(key);
  }

  /**
   * For a given PLANit entity that we persist to file, we track its datastore here to avoid overhead of recreating it
   *
   * @param dataStoreReferenceClass the reference class, i.e., PLANit entity types the datastore persists
   * @param format to use
   * @param outputFileNameWithPath the output file path to persist to
   * @return the datastore
   */
  public static DataStore createSingleEntityTypeDataStore(
      Class<?> dataStoreReferenceClass, GeoIoFormat format, Path outputFileNameWithPath){
    if(dataStoreMap.containsKey(dataStoreReferenceClass)){
      LOGGER.severe(String.format("Datastore for class %s already registered, ignoring this call, providing " +
              "existing datastore", dataStoreReferenceClass.toString()));
      return dataStoreMap.get(dataStoreReferenceClass);
    }

    DataStore theDataStore = createSingleEntityTypeDataStore(format, outputFileNameWithPath);
    if(theDataStore == null){
      throw new PlanItRunTimeException("Unable to create new datastore for class: "+
              dataStoreReferenceClass.toString());
    }
    dataStoreMap.put(dataStoreReferenceClass,theDataStore);
    return theDataStore;
  }

  /**
   * For a given PLANit entity that we persist to file, we track its datastore here to avoid overhead of recreating it
   *
   * @param dataStoreReferenceClass the reference class, i.e., PLANit entity types the datastore persists
   * @param format to use
   * @param geometryTypeClass for the reference class
   * @param outputFileNameWithPath the output file path to persist to
   * @return the datastore
   */
  public static DataStore createSingleEntityTypeDataStore(
          Class<?> dataStoreReferenceClass,
          GeoIoFormat format,
          Class<? extends Geometry> geometryTypeClass,
          Path outputFileNameWithPath){
    Pair<Class<?>,Class<? extends Geometry>> key = Pair.of(dataStoreReferenceClass, geometryTypeClass);
    if(dataStoreMapGeoType.containsKey(key)){
      LOGGER.severe(String.format("Datastore for %s > already registered, ignoring this call," +
              " providing existing datastore", key));
      return dataStoreMapGeoType.get(key);
    }
    DataStore theDataStore = createSingleEntityTypeDataStore(format, outputFileNameWithPath);
    if(theDataStore == null){
      throw new PlanItRunTimeException("Unable to create new datastore for class: %s, geometry type: ",
          dataStoreReferenceClass.toString(), geometryTypeClass.toString());
    }
    dataStoreMapGeoType.put(key, theDataStore);
    return theDataStore;
  }

  /**
   * For a given PLANit entity that we persist to file, we track its datastore here to avoid overhead of recreating it
   *
   * @param dataStoreReferenceClass the reference class, i.e., PLANit entity types the datastore persists
   * @param format to use
   * @param mode for the reference class
   * @param outputFileNameWithPath the output file path to persist to
   * @return the datastore
   */
  public static DataStore createSingleEntityTypeDataStore(
          Class<?> dataStoreReferenceClass,
          GeoIoFormat format,
          Mode mode,
          Path outputFileNameWithPath){
    Pair<Class<?>,Mode> key = Pair.of(dataStoreReferenceClass, mode);
    if(dataStoreMapMode.containsKey(key)){
      LOGGER.severe(String.format("Datastore for %s > already registered, ignoring this call, " +
              "providing existing datastore", key));
      return dataStoreMapMode.get(key);
    }

    DataStore theDataStore = createSingleEntityTypeDataStore(format, outputFileNameWithPath);
    if(theDataStore == null){
      throw new PlanItRunTimeException("Unable to create new datastore for class: %s, mode: ",
          dataStoreReferenceClass.toString(), mode.toString());
    }
    dataStoreMapMode.put(key, theDataStore);
    return theDataStore;
  }

  /**
   * Reset the manager and remove any registered data stores
   */
  public static void reset(){
    dataStoreMap.values().forEach(ds -> ds.dispose());
    dataStoreMap.clear();
    dataStoreMapGeoType.values().forEach(ds -> ds.dispose());
    dataStoreMapGeoType.clear();
    dataStoreMapMode.values().forEach(ds -> ds.dispose());
    dataStoreMapMode.clear();
  }

  /**
   * Given a feature, register it on the datastore if not already available
   *
   * @param dataStore to register on
   * @param feature feature to register
   */
  public static void registerFeatureOnDataStore(DataStore dataStore, SimpleFeatureType feature) {
    PlanItRunTimeException.throwIfNull(feature, "Feature type null, unable to register on datastore, " +
            "this shouldn't happen");
    PlanItRunTimeException.throwIfNull(dataStore, "Data store null, unable to register feature on datastore, " +
            "this shouldn't happen");

    try{
      /* trigger exception when not available to register schema once */
      var alreadyAvailable = dataStore.getSchema(feature.getName());
      if(alreadyAvailable != null){
        LOGGER.info(String.format("OVERWRITE datastore for feature %s already present, overwriting",
                feature.getTypeName()));
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
