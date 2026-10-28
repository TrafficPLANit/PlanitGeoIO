package org.goplanit.geoio.util;

import org.geotools.api.data.DataStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitGeoDataStoreUtils;
import org.goplanit.utils.misc.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads back the features a GeoIO writer persisted, so tests can check what was written. Each output file holds one
 * table, named as the file without its extension.
 *
 * @author markr
 */
public final class GeoIoOutputReaderUtils {

  /** static utility class, not to be instantiated */
  private GeoIoOutputReaderUtils() {
  }

  /**
   * Path of the file that holds the given table
   *
   * @param outputDirectory the writer persisted to
   * @param tableName of the table, the file name without extension
   * @param format of the file
   * @return path of the file
   */
  public static Path createTablePath(Path outputDirectory, String tableName, GeoIoFormat format) {
    return outputDirectory.resolve(tableName + format.extension());
  }

  /**
   * Collect a Boolean attribute of a feature read back. A shapefile gives a Boolean, a GeoPackage stores it as an
   * integer, 1 for true.
   *
   * @param feature to collect from
   * @param attributeName of the Boolean attribute
   * @return value of the attribute
   */
  public static boolean getBooleanAttribute(SimpleFeature feature, String attributeName) {
    var value = feature.getAttribute(attributeName);
    if (value instanceof Number) {
      return ((Number) value).intValue() == 1;
    }
    return (Boolean) value;
  }

  /**
   * Read all features of one table. The data store is disposed of before returning.
   *
   * @param outputDirectory the writer persisted to
   * @param tableName of the table, the file name without extension
   * @param format of the file
   * @return features of the table in the order they are stored
   */
  public static List<SimpleFeature> readFeatures(Path outputDirectory, String tableName, GeoIoFormat format) {
    var tablePath = createTablePath(outputDirectory, tableName, format).toString();
    PlanItRunTimeException.throwIf(!new File(tablePath).exists(), "Output file %s does not exist", tablePath);
    DataStore dataStore = null;
    try {
      if (format == GeoIoFormat.GEOPACKAGE) {
        dataStore = PlanitGeoDataStoreUtils.findFileDataBaseDataStoreWithParams(
            tablePath, Pair.of("dbtype", "geopkg"), Pair.of("read_only", Boolean.TRUE));
      } else {
        dataStore = PlanitGeoDataStoreUtils.findFileDataStore(tablePath);
      }
      PlanItRunTimeException.throwIfNull(dataStore, "No data store found for %s", tablePath);

      var features = new ArrayList<SimpleFeature>();
      try (var featureIterator = dataStore.getFeatureSource(tableName).getFeatures().features()) {
        while (featureIterator.hasNext()) {
          features.add(featureIterator.next());
        }
      }
      return features;
    } catch (IOException e) {
      throw new PlanItRunTimeException("Unable to read table %s from %s", e, tableName, tablePath);
    } finally {
      if (dataStore != null) {
        dataStore.dispose();
      }
    }
  }
}
