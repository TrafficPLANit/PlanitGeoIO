package org.goplanit.geoio.util;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.FileDataStore;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitGeoDataStoreUtils;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.misc.UrlUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Utility class that manages data store connections and related functionality
 */
public final class GeoIODataStoreManager {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(GeoIODataStoreManager.class.getCanonicalName());

  /** Track datastores by the absolute path of the output file they persist to, one datastore per file */
  private static final Map<Path, DataStore> dataStoreMap = new HashMap<>();

  /** Track the names of the feature types registered on each datastore, by identity of the datastore */
  private static final Map<DataStore, Set<String>> featureTypeNamesByDataStore = new IdentityHashMap<>();

  /**
   * Key under which the datastore of an output file is tracked
   *
   * @param outputFileNameWithPath the output file path
   * @return absolute and normalised path
   */
  private static Path createDataStoreKey(Path outputFileNameWithPath){
    return outputFileNameWithPath.toAbsolutePath().normalize();
  }

  /**
   * Create a datastore for the given file location
   *
   * @param format to create data store for
   * @param outputFileNameWithPath to use
   * @return data store created, null if not possible
   */
  protected static DataStore createDataStore(GeoIoFormat format, Path outputFileNameWithPath){
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
   * Collect the registered datastore of the given output file, if not available null is returned.
   *
   * @param outputFileNameWithPath the output file path the datastore persists to
   * @return the datastore, null when none is registered
   */
  public static DataStore getDataStore(Path outputFileNameWithPath){
    return dataStoreMap.get(createDataStoreKey(outputFileNameWithPath));
  }

  /**
   * Collect the registered datastore of the given output file, and create and register it when not yet available.
   * We track datastores here to avoid the overhead of recreating them.
   *
   * @param format to use
   * @param outputFileNameWithPath the output file path to persist to
   * @return the datastore
   */
  public static DataStore findOrCreateDataStore(GeoIoFormat format, Path outputFileNameWithPath){
    var dataStore = getDataStore(outputFileNameWithPath);
    if(dataStore != null){
      return dataStore;
    }

    dataStore = createDataStore(format, outputFileNameWithPath);
    if(dataStore == null){
      throw new PlanItRunTimeException("Unable to create new datastore for file: %s", outputFileNameWithPath);
    }
    dataStoreMap.put(createDataStoreKey(outputFileNameWithPath), dataStore);
    return dataStore;
  }

  /**
   * Reset the manager and remove any registered data stores
   */
  public static void reset(){
    dataStoreMap.values().forEach(ds -> ds.dispose());
    dataStoreMap.clear();
    featureTypeNamesByDataStore.clear();
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

    /* a file datastore such as a shapefile holds one feature type only, so a second one overwrites the first */
    var registeredTypeNames = featureTypeNamesByDataStore.computeIfAbsent(dataStore, ds -> new HashSet<>());
    if(dataStore instanceof FileDataStore && !registeredTypeNames.isEmpty() &&
        !registeredTypeNames.contains(feature.getTypeName())){
      LOGGER.warning(String.format("Feature type %s overwrites feature type %s in the same file, since that file " +
              "holds one feature type only, e.g. a shapefile; give each its own file name",
          feature.getTypeName(), registeredTypeNames.iterator().next()));
    }
    registeredTypeNames.add(feature.getTypeName());

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
