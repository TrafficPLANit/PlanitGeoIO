package org.goplanit.geoio.test.util;

import org.goplanit.geoio.converter.network.GeometryNetworkWriterFactory;
import org.goplanit.geoio.util.GeoIoFormat;
import org.goplanit.logging.Logging;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.id.IdGenerator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;

import java.nio.file.Path;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that {@link GeoIoOutputReaderUtils} reads back what a GeoIO writer persisted, in each format
 *
 * @author markr
 */
public class GeoIoOutputReaderUtilsTest {

  /** the logger */
  private static Logger LOGGER = null;

  private static final Path OUTPUT_PATH =
      Path.of("src", "test", "resources", "converter_test", "outputs", "test_reader");

  /**
   * Write the nine nodes of the small intersections network and read them back
   *
   * @param format to write and read in
   */
  private static void writeAndReadBackNodes(GeoIoFormat format) {
    var outputPath = OUTPUT_PATH.resolve(format.name().toLowerCase());
    var network = NetworkTestUtils.readThreeIntersectionsNetwork();
    var writer = GeometryNetworkWriterFactory.create(outputPath.toString());
    writer.getSettings().setDestinationCoordinateReferenceSystem(PlanitJtsCrsUtils.CARTESIANCRS);
    writer.getSettings().setFormat(format);
    writer.write(network);

    var nodeFeatures = GeoIoOutputReaderUtils.readFeatures(outputPath, "layer_0_planit_nodes", format);
    assertEquals(9, nodeFeatures.size());
    assertEquals(Set.of("0", "1", "2", "3", "4", "5", "6", "7", "8"),
        nodeFeatures.stream().map(f -> f.getAttribute("mapped_id").toString()).collect(Collectors.toSet()));
    assertTrue(nodeFeatures.stream().allMatch(f -> f.getDefaultGeometry() instanceof Point));
    assertTrue(GeoIoOutputReaderUtils.createTablePath(outputPath, "layer_0_planit_nodes", format).toFile().exists());
  }

  /**
   * Create the logger, once
   */
  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(GeoIoOutputReaderUtilsTest.class);
    }
  }

  /**
   * Close the logger and reset the id generator
   */
  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
    IdGenerator.reset();
  }

  /** Nodes written as a GeoPackage are read back */
  @Test
  public void readsBackGeoPackageTable() {
    writeAndReadBackNodes(GeoIoFormat.GEOPACKAGE);
  }

  /** Nodes written as a shapefile are read back */
  @Test
  public void readsBackShapefileTable() {
    writeAndReadBackNodes(GeoIoFormat.SHAPE);
  }
}
