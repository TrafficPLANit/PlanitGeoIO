package org.goplanit.geoio.test.util;

import org.apache.commons.io.FileUtils;
import org.geotools.api.data.DataStore;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DataUtilities;
import org.goplanit.geoio.util.GeoIODataStoreManager;
import org.goplanit.geoio.util.GeoIoFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests which feature types {@link GeoIODataStoreManager} lets share one output file: a shapefile holds one feature
 * type only, so registering a second one is warned about, while a GeoPackage holds several
 *
 * @author markr
 */
public class GeoIODataStoreManagerTest {

  private static final Path OUTPUT_PATH =
      Path.of("src", "test", "resources", "converter_test", "outputs", "data_store_manager");

  /** logger of the manager, whose warnings are counted */
  private static final Logger MANAGER_LOGGER = Logger.getLogger(GeoIODataStoreManager.class.getCanonicalName());

  /** counts the warnings the manager logs */
  private static class WarningCounter extends Handler {
    int warnings = 0;

    /**
     * Count the record when it is a warning
     *
     * @param record logged
     */
    @Override
    public void publish(LogRecord record) {
      if (record.getLevel() == Level.WARNING) {
        ++warnings;
      }
    }

    /**
     * Nothing to flush
     */
    @Override
    public void flush() {
    }

    /**
     * Nothing to close
     */
    @Override
    public void close() {
    }
  }

  private WarningCounter warningCounter;

  /**
   * Create a feature type with a point geometry and a name. A GeoPackage needs the geometry to carry a CRS
   *
   * @param typeName of the feature type
   * @return created feature type
   */
  private static SimpleFeatureType featureType(String typeName) throws Exception {
    return DataUtilities.createType(typeName, "the_geom:Point:srid=4326,name:String");
  }

  /**
   * Register the feature types on the data store of one output file, in the given order
   *
   * @param format of the output file
   * @param typeNames of the feature types to register
   * @return the data store of the output file
   */
  private static DataStore registerOnOneFile(GeoIoFormat format, String... typeNames) throws Exception {
    var dataStore = GeoIODataStoreManager.findOrCreateDataStore(
        format, OUTPUT_PATH.resolve("shared" + format.extension()));
    for (var typeName : typeNames) {
      GeoIODataStoreManager.registerFeatureOnDataStore(dataStore, featureType(typeName));
    }
    return dataStore;
  }

  /**
   * Empty the output folder, and count the warnings the data store manager logs
   */
  @BeforeEach
  public void setUp() throws Exception {
    FileUtils.deleteDirectory(OUTPUT_PATH.toFile());
    Files.createDirectories(OUTPUT_PATH);
    warningCounter = new WarningCounter();
    MANAGER_LOGGER.addHandler(warningCounter);
  }

  /**
   * Stop counting warnings, and reset the data store manager
   */
  @AfterEach
  public void tearDown() {
    MANAGER_LOGGER.removeHandler(warningCounter);
    GeoIODataStoreManager.reset();
  }

  /** A second, different feature type on a shapefile overwrites the first, which is warned about */
  @Test
  public void secondFeatureTypeOnShapefileWarns() throws Exception {
    registerOnOneFile(GeoIoFormat.SHAPE, "shared", "other");
    assertEquals(1, warningCounter.warnings);
  }

  /** Registering the same feature type on a shapefile again is no reason to warn */
  @Test
  public void sameFeatureTypeOnShapefileAgainDoesNotWarn() throws Exception {
    registerOnOneFile(GeoIoFormat.SHAPE, "shared", "shared");
    assertEquals(0, warningCounter.warnings);
  }

  /** A GeoPackage holds several feature types, each as its own table, so nothing is warned about */
  @Test
  public void featureTypesInOneGeoPackageDoNotWarn() throws Exception {
    var dataStore = registerOnOneFile(GeoIoFormat.GEOPACKAGE, "nodes", "links");
    assertEquals(0, warningCounter.warnings);
    assertEquals(List.of("links", "nodes"), List.of(dataStore.getTypeNames()).stream().sorted()
        .collect(java.util.stream.Collectors.toList()));
  }
}
