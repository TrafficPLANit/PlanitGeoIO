package org.goplanit.geoio.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utilities for discovering GeoIO dataset files and creating stable dataset names.
 */
public final class GeoIoDatasetUtils {

  /** Primary GIS dataset extensions supported by GeoIO comparisons */
  public static final List<String> SUPPORTED_DATASET_EXTENSIONS =
      List.of(GeoIoFormat.GEOPACKAGE.extension(), GeoIoFormat.SHAPE.extension());

  /** Utility class */
  private GeoIoDatasetUtils() {}

  /**
   * Collect supported primary GIS dataset files.
   *
   * @param directory to inspect
   * @return sorted dataset paths
   * @throws IOException when files cannot be read
   */
  public static List<Path> collectDatasetPaths(Path directory) throws IOException {
    if (!Files.isDirectory(directory)) {
      throw new AssertionError("GIS dataset directory does not exist: " + directory);
    }

    try (Stream<Path> paths = Files.walk(directory)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(GeoIoDatasetUtils::isSupportedDatasetFile)
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
  public static boolean isSupportedDatasetFile(Path path) {
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
  public static List<String> toRelativeDatasetNames(Path root, List<Path> datasets) {
    return datasets.stream()
        .map(path -> toRelativeDatasetName(root, path))
        .collect(Collectors.toList());
  }

  /**
   * Convert a dataset path to a stable relative name using forward slashes on all platforms.
   *
   * @param root root path
   * @param dataset dataset path
   * @return relative dataset name
   */
  public static String toRelativeDatasetName(Path root, Path dataset) {
    var relativePath = root.toAbsolutePath().normalize().relativize(dataset.toAbsolutePath().normalize());
    return StreamSupport.stream(relativePath.spliterator(), false)
        .map(Path::toString)
        .collect(Collectors.joining("/"));
  }
}
