package org.goplanit.geoio.util;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.goplanit.utils.geo.PlanitGeoDataStoreUtils;
import org.goplanit.utils.misc.Pair;
import org.locationtech.jts.geom.CoordinateFilter;
import org.locationtech.jts.geom.Geometry;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilities for asserting GeoIO outputs in integration tests.
 */
public final class GeoIoAssertionUtils {

  /** Default absolute tolerance for coordinate comparisons */
  public static final double DEFAULT_COORDINATE_TOLERANCE = 1e-6;

  /** Supported primary GIS dataset extensions */
  private static final List<String> SUPPORTED_DATASET_EXTENSIONS = List.of(".gpkg", ".shp");

  /** Utility class */
  private GeoIoAssertionUtils() {}

  /**
   * Assert all supported GIS datasets in two directories are similar.
   *
   * @param resultDir result directory
   * @param referenceDir reference directory
   * @throws IOException when files cannot be read
   */
  public static void assertGeometryFilesSimilar(String resultDir, String referenceDir) throws IOException {
    assertGeometryFilesSimilarWithCoordinateTolerance(resultDir, referenceDir, DEFAULT_COORDINATE_TOLERANCE);
  }

  /**
   * Assert all supported GIS datasets in two directories are similar.
   *
   * @param resultDir result directory
   * @param referenceDir reference directory
   * @throws IOException when files cannot be read
   */
  public static void assertGeometryFilesSimilar(Path resultDir, Path referenceDir) throws IOException {
    assertGeometryFilesSimilar(resultDir.toAbsolutePath().toString(), referenceDir.toAbsolutePath().toString());
  }

  /**
   * Assert all supported GIS datasets in two directories are similar using a coordinate tolerance.
   *
   * @param resultDir result directory
   * @param referenceDir reference directory
   * @param coordinateTolerance absolute coordinate tolerance
   * @throws IOException when files cannot be read
   */
  public static void assertGeometryFilesSimilarWithCoordinateTolerance(
      String resultDir, String referenceDir, double coordinateTolerance) throws IOException {
    var resultPath = Path.of(resultDir).toAbsolutePath();
    var referencePath = Path.of(referenceDir).toAbsolutePath();

    var resultDatasets = collectDatasetPaths(resultPath);
    var referenceDatasets = collectDatasetPaths(referencePath);

    var resultRelativeDatasets = toRelativeDatasetNames(resultPath, resultDatasets);
    var referenceRelativeDatasets = toRelativeDatasetNames(referencePath, referenceDatasets);

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
  public static void assertGeometryFilesSimilarWithCoordinateTolerance(
      Path resultDir, Path referenceDir, double coordinateTolerance) throws IOException {
    assertGeometryFilesSimilarWithCoordinateTolerance(
        resultDir.toAbsolutePath().toString(), referenceDir.toAbsolutePath().toString(), coordinateTolerance);
  }

  /**
   * Collect supported primary GIS dataset files.
   *
   * @param directory to inspect
   * @return sorted dataset paths
   * @throws IOException when files cannot be read
   */
  private static List<Path> collectDatasetPaths(Path directory) throws IOException {
    if (!Files.isDirectory(directory)) {
      throw new AssertionError("GIS dataset directory does not exist: " + directory);
    }

    try (Stream<Path> paths = Files.walk(directory)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(GeoIoAssertionUtils::isSupportedDatasetFile)
          .sorted()
          .collect(Collectors.toList());
    }
  }

  /**
   * Check if a file is a supported primary GIS dataset file.
   *
   * @param path to check
   * @return true when supported
   */
  private static boolean isSupportedDatasetFile(Path path) {
    var fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
    return SUPPORTED_DATASET_EXTENSIONS.stream().anyMatch(fileName::endsWith);
  }

  /**
   * Convert absolute paths to stable relative names.
   *
   * @param root root path
   * @param datasets dataset paths
   * @return relative dataset names
   */
  private static List<String> toRelativeDatasetNames(Path root, List<Path> datasets) {
    return datasets.stream()
        .map(path -> root.relativize(path).toString().replace('\\', '/'))
        .collect(Collectors.toList());
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
      resultDataStore = openDataStore(resultFile);
      referenceDataStore = openDataStore(referenceFile);

      if (resultDataStore == null || referenceDataStore == null) {
        throw new AssertionError(String.format(
            "Unable to open GIS dataset(s). Result=%s, Reference=%s", resultFile, referenceFile));
      }

      var resultTypeNames = sortedTypeNames(resultDataStore);
      var referenceTypeNames = sortedTypeNames(referenceDataStore);
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
   * Open a supported GIS dataset file.
   *
   * @param datasetFile dataset file
   * @return data store
   * @throws IOException when file cannot be read
   */
  private static DataStore openDataStore(Path datasetFile) throws IOException {
    var fileName = datasetFile.getFileName().toString().toLowerCase(Locale.ROOT);
    if (fileName.endsWith(".gpkg")) {
      return PlanitGeoDataStoreUtils.findFileDataBaseDataStoreWithParams(
          datasetFile.toAbsolutePath().toString(),
          Pair.of("dbtype", "geopkg"),
          Pair.of("read-only", true));
    }
    return PlanitGeoDataStoreUtils.findFileDataStore(datasetFile.toAbsolutePath().toString());
  }

  /**
   * Collect sorted type names.
   *
   * @param dataStore to use
   * @return sorted type names
   * @throws IOException when type names cannot be read
   */
  private static List<String> sortedTypeNames(DataStore dataStore) throws IOException {
    return Arrays.stream(dataStore.getTypeNames()).sorted().collect(Collectors.toList());
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
    var resultSchema = canonicalFeatureType(resultType);
    var referenceSchema = canonicalFeatureType(referenceType);
    if (!resultSchema.equals(referenceSchema)) {
      throw new AssertionError(String.format(
          "GIS schema differs for %s:%s. Result=%s, Reference=%s",
          datasetFile.getFileName(), typeName, resultSchema, referenceSchema));
    }
  }

  /**
   * Create a stable schema representation.
   *
   * @param featureType to represent
   * @return canonical schema entries
   */
  private static List<String> canonicalFeatureType(SimpleFeatureType featureType) {
    return featureType.getAttributeDescriptors().stream()
        .map(descriptor -> String.format(
            "%s:%s",
            descriptor.getLocalName(),
            descriptor.getType().getBinding().getCanonicalName()))
        .sorted()
        .collect(Collectors.toList());
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
    var resultFeatures = canonicalFeatures(resultSource, coordinateTolerance);
    var referenceFeatures = canonicalFeatures(referenceSource, coordinateTolerance);

    if (!resultFeatures.equals(referenceFeatures)) {
      throw new AssertionError(String.format(
          "GIS features differ for %s:%s. Result count=%d, Reference count=%d",
          datasetFile.getFileName(), typeName, resultFeatures.size(), referenceFeatures.size()));
    }
  }

  /**
   * Collect stable feature representations.
   *
   * @param featureSource to read
   * @param coordinateTolerance absolute coordinate tolerance
   * @return sorted canonical features
   * @throws IOException when features cannot be read
   */
  private static List<String> canonicalFeatures(
      SimpleFeatureSource featureSource, double coordinateTolerance) throws IOException {
    var canonicalFeatures = new ArrayList<String>();
    var featureCollection = featureSource.getFeatures();
    try (SimpleFeatureIterator iterator = featureCollection.features()) {
      while (iterator.hasNext()) {
        canonicalFeatures.add(canonicalFeature(iterator.next(), coordinateTolerance));
      }
    }
    canonicalFeatures.sort(Comparator.naturalOrder());
    return canonicalFeatures;
  }

  /**
   * Create a stable feature representation.
   *
   * @param feature to represent
   * @param coordinateTolerance absolute coordinate tolerance
   * @return canonical feature
   */
  private static String canonicalFeature(SimpleFeature feature, double coordinateTolerance) {
    return feature.getProperties().stream()
        .sorted(Comparator.comparing(property -> property.getName().toString()))
        .map(property -> canonicalProperty(property, coordinateTolerance))
        .collect(Collectors.joining("|"));
  }

  /**
   * Create a stable property representation.
   *
   * @param property to represent
   * @param coordinateTolerance absolute coordinate tolerance
   * @return canonical property
   */
  private static String canonicalProperty(Property property, double coordinateTolerance) {
    return property.getName() + "=" + canonicalValue(property.getValue(), coordinateTolerance);
  }

  /**
   * Create a stable value representation.
   *
   * @param value to represent
   * @param coordinateTolerance absolute coordinate tolerance
   * @return canonical value
   */
  private static String canonicalValue(Object value, double coordinateTolerance) {
    if (value == null) {
      return "<null>";
    }
    if (value instanceof Geometry) {
      return canonicalGeometry((Geometry) value, coordinateTolerance);
    }
    if (value instanceof Float || value instanceof Double) {
      return String.format(Locale.ROOT, "%.9f", roundToTolerance(((Number) value).doubleValue(), coordinateTolerance));
    }
    if (value instanceof Number || value instanceof Boolean || value instanceof CharSequence) {
      return Objects.toString(value);
    }
    if (value.getClass().isArray()) {
      var entries = new ArrayList<String>();
      for (int index = 0; index < Array.getLength(value); index++) {
        entries.add(canonicalValue(Array.get(value, index), coordinateTolerance));
      }
      return entries.toString();
    }
    if (value instanceof Collection<?>) {
      return ((Collection<?>) value).stream()
          .map(entry -> canonicalValue(entry, coordinateTolerance))
          .collect(Collectors.joining(",", "[", "]"));
    }
    return Objects.toString(value);
  }

  /**
   * Create a stable geometry representation.
   *
   * @param geometry to represent
   * @param coordinateTolerance absolute coordinate tolerance
   * @return canonical geometry
   */
  private static String canonicalGeometry(Geometry geometry, double coordinateTolerance) {
    var roundedGeometry = (Geometry) geometry.copy();
    roundedGeometry.apply((CoordinateFilter) coordinate -> {
      coordinate.x = roundToTolerance(coordinate.x, coordinateTolerance);
      coordinate.y = roundToTolerance(coordinate.y, coordinateTolerance);
      coordinate.z = roundToTolerance(coordinate.z, coordinateTolerance);
    });
    roundedGeometry.geometryChanged();
    roundedGeometry.normalize();
    return roundedGeometry.toText();
  }

  /**
   * Round a value to the configured tolerance.
   *
   * @param value to round
   * @param tolerance tolerance to use
   * @return rounded value
   */
  private static double roundToTolerance(double value, double tolerance) {
    if (!Double.isFinite(value) || tolerance <= 0) {
      return value;
    }
    return Math.rint(value / tolerance) * tolerance;
  }
}
