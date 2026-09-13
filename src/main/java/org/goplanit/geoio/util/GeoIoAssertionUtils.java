package org.goplanit.geoio.util;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.goplanit.utils.geo.PlanitGeoDataStoreUtils;
import org.goplanit.utils.geo.PlanitSimpleFeatureStringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Utilities for asserting GeoIO outputs in integration tests.
 */
public final class GeoIoAssertionUtils {

  /** Utility class */
  private GeoIoAssertionUtils() {}

  /**
   * Assert all supported GIS datasets in two directories are similar using a coordinate tolerance.
   *
   * @param resultDir result directory
   * @param referenceDir reference directory
   * @param coordinateTolerance absolute coordinate tolerance
   * @throws IOException when files cannot be read
   */
  public static void assertGeometryFilesSimilar(
      String resultDir, String referenceDir, double coordinateTolerance) throws IOException {
    var resultPath = Path.of(resultDir).toAbsolutePath();
    var referencePath = Path.of(referenceDir).toAbsolutePath();

    var resultDatasets = GeoIoDatasetUtils.collectDatasetPaths(resultPath);
    var referenceDatasets = GeoIoDatasetUtils.collectDatasetPaths(referencePath);

    var resultRelativeDatasets = GeoIoDatasetUtils.toRelativeDatasetNames(resultPath, resultDatasets);
    var referenceRelativeDatasets = GeoIoDatasetUtils.toRelativeDatasetNames(referencePath, referenceDatasets);

    if (!resultRelativeDatasets.equals(referenceRelativeDatasets)) {
      throw new AssertionError(String.format(
          "GIS dataset files differ. Result=%s, Reference=%s", resultRelativeDatasets, referenceRelativeDatasets));
    }

    for (int index = 0; index < resultDatasets.size(); index++) {
      assertDataStoresSimilar(resultDatasets.get(index), referenceDatasets.get(index), coordinateTolerance);
    }
  }

  /**
   * Assert all supported GIS datasets in two directories are similar using a coordinate tolerance.
   *
   * @param resultDir result directory
   * @param referenceDir reference directory
   * @param coordinateTolerance absolute coordinate tolerance
   * @throws IOException when files cannot be read
   */
  public static void assertGeometryFilesSimilar(
      Path resultDir, Path referenceDir, double coordinateTolerance) throws IOException {
    assertGeometryFilesSimilar(
        resultDir.toAbsolutePath().toString(), referenceDir.toAbsolutePath().toString(), coordinateTolerance);
  }

  /**
   * Assert supported GIS datasets expose expected total feature counts.
   *
   * @param resultDir result directory
   * @param expectedFeatureCountsByDataset expected total feature counts keyed by relative dataset path
   * @throws IOException when files cannot be read
   */
  public static void assertGeometryFilesHaveTotalFeatureCounts(
      String resultDir, Map<String, Integer> expectedFeatureCountsByDataset) throws IOException {
    var resultPath = Path.of(resultDir).toAbsolutePath();
    var actualFeatureCountsByDataset = new LinkedHashMap<String, Integer>();
    for (Path datasetPath : GeoIoDatasetUtils.collectDatasetPaths(resultPath)) {
      var relativeDatasetName = GeoIoDatasetUtils.toRelativeDatasetName(resultPath, datasetPath);
      actualFeatureCountsByDataset.put(relativeDatasetName, PlanitGeoDataStoreUtils.totalFeatureCount(datasetPath));
    }

    if (!actualFeatureCountsByDataset.equals(expectedFeatureCountsByDataset)) {
      throw new AssertionError(String.format(
          "GIS feature counts differ. Result=%s, Expected=%s",
          actualFeatureCountsByDataset, expectedFeatureCountsByDataset));
    }
  }

  /**
   * Assert supported GIS datasets expose expected total feature counts.
   *
   * @param resultDir result directory
   * @param expectedFeatureCountsByDataset expected total feature counts keyed by relative dataset path
   * @throws IOException when files cannot be read
   */
  public static void assertGeometryFilesHaveTotalFeatureCounts(
      Path resultDir, Map<String, Integer> expectedFeatureCountsByDataset) throws IOException {
    assertGeometryFilesHaveTotalFeatureCounts(
        resultDir.toAbsolutePath().toString(), expectedFeatureCountsByDataset);
  }

  /**
   * Assert supported GIS datasets expose expected feature counts per layer.
   *
   * @param resultDir result directory
   * @param expectedFeatureCountsByDatasetAndLayer expected counts keyed by relative dataset path and layer name
   * @throws IOException when files cannot be read
   */
  public static void assertGeometryFilesHaveLayerFeatureCounts(
      String resultDir, Map<String, Map<String, Integer>> expectedFeatureCountsByDatasetAndLayer)
      throws IOException {
    var resultPath = Path.of(resultDir).toAbsolutePath();
    var actualFeatureCountsByDatasetAndLayer = new LinkedHashMap<String, Map<String, Integer>>();
    for (Path datasetPath : GeoIoDatasetUtils.collectDatasetPaths(resultPath)) {
      var relativeDatasetName = GeoIoDatasetUtils.toRelativeDatasetName(resultPath, datasetPath);
      actualFeatureCountsByDatasetAndLayer.put(
          relativeDatasetName, PlanitGeoDataStoreUtils.layerFeatureCounts(datasetPath));
    }

    if (!actualFeatureCountsByDatasetAndLayer.equals(expectedFeatureCountsByDatasetAndLayer)) {
      throw new AssertionError(String.format(
          "GIS layer feature counts differ. Result=%s, Expected=%s",
          actualFeatureCountsByDatasetAndLayer, expectedFeatureCountsByDatasetAndLayer));
    }
  }

  /**
   * Assert supported GIS datasets expose expected feature counts per layer.
   *
   * @param resultDir result directory
   * @param expectedFeatureCountsByDatasetAndLayer expected counts keyed by relative dataset path and layer name
   * @throws IOException when files cannot be read
   */
  public static void assertGeometryFilesHaveLayerFeatureCounts(
      Path resultDir, Map<String, Map<String, Integer>> expectedFeatureCountsByDatasetAndLayer)
      throws IOException {
    assertGeometryFilesHaveLayerFeatureCounts(
        resultDir.toAbsolutePath().toString(), expectedFeatureCountsByDatasetAndLayer);
  }

  /**
   * Create ordered feature count expectations keyed by relative dataset path.
   *
   * @param entries alternating dataset path and feature count entries
   * @return feature counts
   */
  public static Map<String, Integer> featureCounts(Object... entries) {
    if (entries.length % 2 != 0) {
      throw new IllegalArgumentException("Feature count entries must be alternating dataset path and count values");
    }

    var featureCounts = new LinkedHashMap<String, Integer>();
    for (int index = 0; index < entries.length; index += 2) {
      if (!(entries[index] instanceof String) || !(entries[index + 1] instanceof Integer)) {
        throw new IllegalArgumentException("Feature count entries must be String, Integer pairs");
      }
      featureCounts.put((String) entries[index], (Integer) entries[index + 1]);
    }
    return featureCounts;
  }

  /**
   * Assert two GIS dataset files expose the same layers, schemas, and features.
   *
   * @param resultFile result GIS dataset
   * @param referenceFile reference GIS dataset
   * @param coordinateTolerance absolute coordinate tolerance
   * @throws IOException when files cannot be read
   */
  private static void assertDataStoresSimilar(
      Path resultFile, Path referenceFile, double coordinateTolerance) throws IOException {
    DataStore resultDataStore = null;
    DataStore referenceDataStore = null;
    try {
      resultDataStore = PlanitGeoDataStoreUtils.openDataStore(resultFile);
      referenceDataStore = PlanitGeoDataStoreUtils.openDataStore(referenceFile);

      if (resultDataStore == null || referenceDataStore == null) {
        throw new AssertionError(String.format(
            "Unable to open GIS dataset(s). Result=%s, Reference=%s", resultFile, referenceFile));
      }

      var resultTypeNames = PlanitGeoDataStoreUtils.sortedTypeNames(resultDataStore);
      var referenceTypeNames = PlanitGeoDataStoreUtils.sortedTypeNames(referenceDataStore);
      if (!resultTypeNames.equals(referenceTypeNames)) {
        throw new AssertionError(String.format(
            "GIS layer names differ for %s. Result=%s, Reference=%s",
            resultFile.getFileName(), resultTypeNames, referenceTypeNames));
      }

      for (String typeName : resultTypeNames) {
        assertFeatureTypeSimilar(
            resultFile, typeName, resultDataStore.getSchema(typeName), referenceDataStore.getSchema(typeName));
        assertFeatureCollectionsSimilar(
            resultFile,
            typeName,
            resultDataStore.getFeatureSource(typeName),
            referenceDataStore.getFeatureSource(typeName),
            coordinateTolerance);
      }
    } finally {
      if (resultDataStore != null) {
        resultDataStore.dispose();
      }
      if (referenceDataStore != null) {
        referenceDataStore.dispose();
      }
    }
  }

  /**
   * Assert two feature type schemas are similar.
   *
   * @param datasetFile dataset being compared
   * @param typeName layer/type name
   * @param resultType result feature type
   * @param referenceType reference feature type
   */
  private static void assertFeatureTypeSimilar(
      Path datasetFile, String typeName, SimpleFeatureType resultType, SimpleFeatureType referenceType) {
    var resultSchema = PlanitSimpleFeatureStringUtils.asSortedSchemaDescriptorStrings(resultType);
    var referenceSchema = PlanitSimpleFeatureStringUtils.asSortedSchemaDescriptorStrings(referenceType);
    if (!resultSchema.equals(referenceSchema)) {
      throw new AssertionError(String.format(
          "GIS schema differs for %s:%s. Result=%s, Reference=%s",
          datasetFile.getFileName(), typeName, resultSchema, referenceSchema));
    }
  }

  /**
   * Assert feature collections are similar.
   *
   * @param datasetFile dataset being compared
   * @param typeName layer/type name
   * @param resultSource result feature source
   * @param referenceSource reference feature source
   * @param coordinateTolerance absolute coordinate tolerance
   * @throws IOException when features cannot be read
   */
  private static void assertFeatureCollectionsSimilar(
      Path datasetFile,
      String typeName,
      SimpleFeatureSource resultSource,
      SimpleFeatureSource referenceSource,
      double coordinateTolerance) throws IOException {
    var resultFeatures =
        PlanitSimpleFeatureStringUtils.collectStableFeatureStringHashCounts(resultSource, coordinateTolerance);
    var referenceFeatures =
        PlanitSimpleFeatureStringUtils.collectStableFeatureStringHashCounts(referenceSource, coordinateTolerance);

    if (!resultFeatures.equals(referenceFeatures)) {
      var firstMismatch = firstHashCountMismatch(resultFeatures, referenceFeatures);
      throw new AssertionError(String.format(
          "GIS features differ for %s:%s. Result unique hashes=%d, Reference unique hashes=%d, First mismatch=%s",
          datasetFile.getFileName(), typeName, resultFeatures.size(), referenceFeatures.size(), firstMismatch));
    }
  }

  /**
   * Find a first differing hash count for diagnostics.
   *
   * @param resultFeatures result hash counts
   * @param referenceFeatures reference hash counts
   * @return mismatch description
   */
  private static String firstHashCountMismatch(
      Map<String, Integer> resultFeatures, Map<String, Integer> referenceFeatures) {
    return Stream.concat(resultFeatures.keySet().stream(), referenceFeatures.keySet().stream())
        .distinct()
        .sorted()
        .filter(hash -> !Objects.equals(resultFeatures.get(hash), referenceFeatures.get(hash)))
        .findFirst()
        .map(hash -> String.format(
            "%s result=%s reference=%s", hash, resultFeatures.get(hash), referenceFeatures.get(hash)))
        .orElse("<none>");
  }
}
