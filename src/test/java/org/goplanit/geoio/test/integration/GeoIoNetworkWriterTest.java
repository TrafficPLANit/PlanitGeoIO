package org.goplanit.geoio.test.integration;

import org.goplanit.geoio.test.util.GeoIoOutputReaderUtils;
import org.goplanit.geoio.test.util.NetworkTestUtils;
import org.goplanit.geoio.util.GeoIoFormat;
import org.goplanit.io.converter.network.PlanitNetworkReaderFactory;
import org.goplanit.logging.Logging;
import org.goplanit.utils.id.IdGenerator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.logging.Logger;

import static org.goplanit.geoio.test.util.NetworkOutputAssertionUtils.assertRowCount;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the GeoIO network writer that read the written files back
 *
 * @author markr
 */
public class GeoIoNetworkWriterTest {

  /** the logger */
  private static Logger LOGGER = null;

  /** network with layer 0 (car), the small network with three intersections, and layer 1 (bus) with three nodes, two
   * links and one intersection */
  private static final Path TWO_LAYERS_INPUT_PATH =
      Path.of("src", "test", "resources", "converter_test", "input", "intersections_two_layers");

  private static final Path OUTPUT_PATH =
      Path.of("src", "test", "resources", "converter_test", "outputs", "network_writer");

  /**
   * Write the two layer network and read back each layer's tables from its own files
   *
   * @param format to write in
   */
  private static void writeAndReadBackTwoLayers(GeoIoFormat format) {
    var outputPath = NetworkTestUtils.writeCartesianNetworkToEmptiedDirectory(
        PlanitNetworkReaderFactory.create(TWO_LAYERS_INPUT_PATH.toString()).read(),
        format,
        OUTPUT_PATH.resolve("two_layers").resolve(format.name().toLowerCase()),
        writer -> {});

    assertRowCount(9, outputPath, "layer_0_planit_nodes", format);
    assertRowCount(8, outputPath, "layer_0_planit_links", format);
    assertRowCount(16, outputPath, "layer_0_planit_linksegments", format);
    assertRowCount(3, outputPath, "layer_0_planit_intersection_nodes", format);
    assertRowCount(11, outputPath, "layer_0_planit_intersection_segments", format);

    assertRowCount(3, outputPath, "layer_1_planit_nodes", format);
    assertRowCount(2, outputPath, "layer_1_planit_links", format);
    assertRowCount(4, outputPath, "layer_1_planit_linksegments", format);
    assertRowCount(1, outputPath, "layer_1_planit_intersection_nodes", format);
    assertRowCount(2, outputPath, "layer_1_planit_intersection_segments", format);
  }

  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(GeoIoNetworkWriterTest.class);
    }
  }

  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
    IdGenerator.reset();
  }

  /** Each layer of a two layer network is written to its own GeoPackages */
  @Test
  public void twoLayersWriteOwnGeoPackages() {
    writeAndReadBackTwoLayers(GeoIoFormat.GEOPACKAGE);
  }

  /** Each layer of a two layer network is written to its own shapefiles */
  @Test
  public void twoLayersWriteOwnShapefiles() {
    writeAndReadBackTwoLayers(GeoIoFormat.SHAPE);
  }

  /** A custom layer prefix replaces "layer" at the start of each layer's file names */
  @Test
  public void customLayerPrefixIsUsedInFileNames() {
    var format = GeoIoFormat.GEOPACKAGE;
    var outputPath = NetworkTestUtils.writeCartesianNetworkToEmptiedDirectory(
        NetworkTestUtils.readThreeIntersectionsNetwork(),
        format,
        OUTPUT_PATH.resolve("layer_prefix"),
        writer -> writer.getSettings().setLayerPrefix("custom"));

    assertRowCount(9, outputPath, "custom_0_planit_nodes", format);
    assertRowCount(3, outputPath, "custom_0_planit_intersection_nodes", format);
    assertTrue(Arrays.stream(outputPath.toFile().listFiles()).noneMatch(f -> f.getName().startsWith("layer")));
  }

  /** With link segments switched off no link segments file is written, while links still are */
  @Test
  public void linkSegmentsOffWritesNoLinkSegmentsFile() {
    var format = GeoIoFormat.GEOPACKAGE;
    var outputPath = NetworkTestUtils.writeCartesianNetworkToEmptiedDirectory(
        NetworkTestUtils.readThreeIntersectionsNetwork(),
        format,
        OUTPUT_PATH.resolve("link_segments_off"),
        writer -> writer.getSettings().setPersistLinkSegments(false));

    assertRowCount(8, outputPath, "layer_0_planit_links", format);
    assertFalse(
        GeoIoOutputReaderUtils.createTablePath(outputPath, "layer_0_planit_linksegments", format).toFile().exists());
  }
}
