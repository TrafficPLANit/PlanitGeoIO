package org.goplanit.geoio.test.util;

import org.goplanit.geoio.converter.network.GeometryNetworkWriterSettings;
import org.goplanit.geoio.util.GeoIoFormat;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.UntypedPhysicalNetwork;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Assertions on the network files a GeoIO network writer persisted, read back from disk. File names follow the
 * default settings and the XML id mapping the writers use by default.
 *
 * @author markr
 */
public final class NetworkOutputAssertionUtils {

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
    for (var layer : network.getTransportLayers()) {
      var layerFilePrefix = filePrefix + "layer_" + layer.getXmlId() + "_";
      assertRowCount(layer.getNodes().size(),
          outputPath, layerFilePrefix + GeometryNetworkWriterSettings.DEFAULT_NODES_FILE_NAME, format);
      assertRowCount(layer.getLinks().size(),
          outputPath, layerFilePrefix + GeometryNetworkWriterSettings.DEFAULT_LINKS_FILE_NAME, format);
      assertRowCount(layer.getLinkSegments().size(),
          outputPath, layerFilePrefix + GeometryNetworkWriterSettings.DEFAULT_LINKSEGMENTS_FILE_NAME, format);
    }
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
    assertTrue(network.getTransportLayers().stream().anyMatch(layer -> layer.hasIntersections()));
    for (var layer : network.getTransportLayers()) {
      if (!layer.hasIntersections()) {
        continue;
      }
      var layerFilePrefix = "layer_" + layer.getXmlId() + "_";
      assertRowCount(layer.getIntersections().size(),
          outputPath, layerFilePrefix + GeometryNetworkWriterSettings.DEFAULT_INTERSECTION_NODES_FILE_NAME, format);

      long approachCount = layer.getIntersections().stream().mapToLong(i -> i.getApproachSegments().size()).sum();
      long internalCount = layer.getIntersections().stream().mapToLong(i -> i.getInternalSegments().size()).sum();
      var segmentRows = GeoIoOutputReaderUtils.readFeatures(
          outputPath, layerFilePrefix + GeometryNetworkWriterSettings.DEFAULT_INTERSECTION_SEGMENTS_FILE_NAME, format);
      assertEquals(approachCount + internalCount, segmentRows.size());
      assertEquals(approachCount,
          segmentRows.stream().filter(f -> GeoIoOutputReaderUtils.getBooleanAttribute(f, "approach")).count());
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
    assertEquals(expected, GeoIoOutputReaderUtils.readFeatures(outputPath, tableName, format).size(), tableName);
  }
}
