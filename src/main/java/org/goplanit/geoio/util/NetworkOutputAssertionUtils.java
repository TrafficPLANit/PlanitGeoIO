package org.goplanit.geoio.util;

import org.goplanit.geoio.converter.network.GeometryNetworkWriterSettings;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.UntypedPhysicalNetwork;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionUtils;

import java.nio.file.Path;

/**
 * Assertions on the network files a GeoIO network writer persisted, read back from disk, for tests in any project.
 * File names follow the default settings and the XML id mapping the writers use by default. A failed assertion throws
 * an {@link AssertionError}, which test frameworks report as a failure.
 *
 * @author markr
 */
public final class NetworkOutputAssertionUtils {

  /**
   * Assert a count is the one expected
   *
   * @param expected count
   * @param actual count
   * @param counted what is counted, named when the assertion fails
   */
  private static void assertCount(long expected, long actual, String counted) {
    if (expected != actual) {
      throw new AssertionError(String.format("Expected %d %s, found %d", expected, counted, actual));
    }
  }

  /**
   * Assert that each layer of the network has one row per node, link and link segment in its files, for each of them
   * asked for
   *
   * @param network that was written
   * @param outputPath the writer persisted to
   * @param filePrefix prepended to each file name, "" for a regular network, "conjugate_" for a conjugate network
   * @param format of the files
   * @param nodes whether to assert the nodes
   * @param links whether to assert the links
   * @param linkSegments whether to assert the link segments
   */
  private static void assertRowCounts(UntypedPhysicalNetwork<?, ?> network, Path outputPath, String filePrefix,
      GeoIoFormat format, boolean nodes, boolean links, boolean linkSegments) {
    for (var layer : network.getTransportLayers()) {
      var layerFilePrefix = filePrefix + "layer_" + layer.getXmlId() + "_";
      if (nodes) {
        assertRowCount(layer.getNodes().size(),
            outputPath, layerFilePrefix + GeometryNetworkWriterSettings.DEFAULT_NODES_FILE_NAME, format);
      }
      if (links) {
        assertRowCount(layer.getLinks().size(),
            outputPath, layerFilePrefix + GeometryNetworkWriterSettings.DEFAULT_LINKS_FILE_NAME, format);
      }
      if (linkSegments) {
        assertRowCount(layer.getLinkSegments().size(),
            outputPath, layerFilePrefix + GeometryNetworkWriterSettings.DEFAULT_LINKSEGMENTS_FILE_NAME, format);
      }
    }
  }

  /** static utility class, not to be instantiated */
  private NetworkOutputAssertionUtils() {
  }

  /**
   * Assert that each layer of the network has one row per node, link and link segment in its files
   *
   * @param network that was written
   * @param outputPath the writer persisted to
   * @param filePrefix prepended to each file name, "" for a regular network, "conjugate_" for a conjugate network
   * @param format of the files
   */
  public static void assertNodeLinkAndLinkSegmentRowCounts(
      UntypedPhysicalNetwork<?, ?> network, Path outputPath, String filePrefix, GeoIoFormat format) {
    assertRowCounts(network, outputPath, filePrefix, format, true, true, true);
  }

  /**
   * Assert that each layer of the network has one row per node, link and link segment in its files, for those the
   * writer settings persist, in the output directory and format of the settings
   *
   * @param network that was written
   * @param settings the network was written with
   */
  public static void assertPersistedNodeLinkAndLinkSegmentRowCounts(
      UntypedPhysicalNetwork<?, ?> network, GeometryNetworkWriterSettings settings) {
    assertRowCounts(network, Path.of(settings.getOutputDirectory()), "", settings.getFormat(),
        settings.isPersistNodes(), settings.isPersistLinks(), settings.isPersistLinkSegments());
  }

  /**
   * Assert that the network has intersections, and that each layer with intersections has one row per intersection in
   * its intersection nodes file, and one row per approach and internal segment in its intersection segments file, with
   * exactly the approaches marked as approach
   *
   * @param network that was written
   * @param outputPath the writer persisted to
   * @param format of the files
   */
  public static void assertIntersectionNodeAndSegmentRowCounts(
      MacroscopicNetwork network, Path outputPath, GeoIoFormat format) {
    if (network.getTransportLayers().stream().noneMatch(layer -> layer.hasIntersections())) {
      throw new AssertionError("The network has no intersections");
    }
    for (var layer : network.getTransportLayers()) {
      if (!layer.hasIntersections()) {
        continue;
      }
      var layerFilePrefix = "layer_" + layer.getXmlId() + "_";
      assertRowCount(layer.getIntersections().size(),
          outputPath, layerFilePrefix + GeometryNetworkWriterSettings.DEFAULT_INTERSECTION_NODES_FILE_NAME, format);

      long approachCount = layer.getIntersections().stream().mapToLong(i -> i.getApproachSegments().size()).sum();
      long internalCount = layer.getIntersections().stream()
          .mapToLong(i -> IntersectionUtils.getInternalSegmentsOf(i).size()).sum();
      var segmentRows = GeoIoOutputReaderUtils.readFeatures(
          outputPath, layerFilePrefix + GeometryNetworkWriterSettings.DEFAULT_INTERSECTION_SEGMENTS_FILE_NAME, format);
      assertCount(approachCount + internalCount, segmentRows.size(), "intersection segment rows");
      assertCount(approachCount,
          segmentRows.stream().filter(f -> GeoIoOutputReaderUtils.getBooleanAttribute(f, "approach")).count(),
          "approach rows");
    }
  }

  /**
   * Assert the number of rows in one table
   *
   * @param expected number of rows
   * @param outputPath the writer persisted to
   * @param tableName of the table
   * @param format of the file
   */
  public static void assertRowCount(long expected, Path outputPath, String tableName, GeoIoFormat format) {
    assertCount(expected,
        GeoIoOutputReaderUtils.readFeatures(outputPath, tableName, format).size(), "rows in " + tableName);
  }
}
