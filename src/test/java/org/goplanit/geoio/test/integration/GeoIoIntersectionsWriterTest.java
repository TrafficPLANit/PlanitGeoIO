package org.goplanit.geoio.test.integration;

import org.goplanit.geoio.converter.network.GeometryNetworkWriter;
import org.goplanit.geoio.test.util.GeoIoOutputReaderUtils;
import org.goplanit.geoio.test.util.NetworkTestUtils;
import org.goplanit.geoio.util.GeoIoFormat;
import org.goplanit.io.converter.network.PlanitNetworkReaderFactory;
import org.goplanit.logging.Logging;
import org.goplanit.utils.id.IdGenerator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the intersections export of the GeoIO network writer end to end, by writing the small network with three
 * intersections and reading the written files back
 *
 * @author markr
 */
public class GeoIoIntersectionsWriterTest {

  /** the logger */
  private static Logger LOGGER = null;

  private static final Path OUTPUT_PATH =
      Path.of("src", "test", "resources", "converter_test", "outputs", "intersections");

  private static final Path GRID10x10_INPUT_PATH =
      Path.of("src", "test", "resources", "converter_test", "input", "grid10x10");

  private static final String INTERSECTION_NODES_TABLE = "layer_0_planit_intersection_nodes";

  private static final String INTERSECTION_SEGMENTS_TABLE = "layer_0_planit_intersection_segments";

  /**
   * Write the small network with three intersections to an emptied output directory
   *
   * @param format to write in
   * @param outputSubDirectory under the output path for this case
   * @param configureWriter changes to the writer settings before writing
   * @return output directory written to
   */
  private static Path writeThreeIntersectionsNetwork(
      GeoIoFormat format, String outputSubDirectory, Consumer<GeometryNetworkWriter> configureWriter) {
    return NetworkTestUtils.writeCartesianNetworkToEmptiedDirectory(
        NetworkTestUtils.readThreeIntersectionsNetwork(),
        format,
        OUTPUT_PATH.resolve(outputSubDirectory).resolve(format.name().toLowerCase()),
        configureWriter);
  }

  /**
   * Write with the intersection export on by default, then read back both intersection tables
   *
   * @param format to write in
   */
  private static void writeAndReadBackIntersections(GeoIoFormat format) {
    var outputPath = writeThreeIntersectionsNetwork(format, "default", writer -> {});

    var intersections = GeoIoOutputReaderUtils.readFeatures(outputPath, INTERSECTION_NODES_TABLE, format);
    assertEquals(3, intersections.size());
    assertTrue(intersections.stream().allMatch(f -> f.getDefaultGeometry() instanceof MultiPoint));
    var i3 = intersections.stream().filter(f -> "i3".equals(f.getAttribute("mapped_id"))).findFirst().orElseThrow();
    assertEquals("5,6", i3.getAttribute("node_ids"));
    assertEquals("10,11", i3.getAttribute("internals"));
    assertTrue(GeoIoOutputReaderUtils.getBooleanAttribute(i3, "junction"));
    assertFalse(GeoIoOutputReaderUtils.getBooleanAttribute(i3, "crossing"));
    assertEquals(2, ((MultiPoint) i3.getDefaultGeometry()).getNumGeometries());

    var segments = GeoIoOutputReaderUtils.readFeatures(outputPath, INTERSECTION_SEGMENTS_TABLE, format);
    assertEquals(11, segments.size());
    assertEquals(9, segments.stream().filter(f -> GeoIoOutputReaderUtils.getBooleanAttribute(f, "approach")).count());
    assertEquals(2, segments.stream().filter(f -> !GeoIoOutputReaderUtils.getBooleanAttribute(f, "approach")).count());
    /* a shapefile holds line strings as multi line strings */
    assertTrue(segments.stream().allMatch(
        f -> f.getDefaultGeometry() instanceof LineString || f.getDefaultGeometry() instanceof MultiLineString));
    var approach0 = segments.stream().filter(f -> "0".equals(f.getAttribute("seg_id"))).findFirst().orElseThrow();
    assertEquals("i1", approach0.getAttribute("isect_id"));
    assertEquals("5", approach0.getAttribute("banned_to"));
  }

  /**
   * Collect the names of the files in the output directory that hold intersection tables
   *
   * @param outputPath to look in
   * @return names of files with "intersection" in their name
   */
  private static List<String> findIntersectionFileNames(Path outputPath) {
    var files = outputPath.toFile().listFiles();
    if (files == null) {
      return List.of();
    }
    return Arrays.stream(files).map(File::getName).filter(name -> name.contains("intersection"))
        .collect(Collectors.toList());
  }

  /**
   * Create the logger, once
   */
  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(GeoIoIntersectionsWriterTest.class);
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

  /** Both intersection tables are written to and read back from GeoPackages */
  @Test
  public void writesIntersectionsAsGeoPackage() {
    writeAndReadBackIntersections(GeoIoFormat.GEOPACKAGE);
  }

  /** Both intersection tables are written to and read back from shapefiles */
  @Test
  public void writesIntersectionsAsShapefile() {
    writeAndReadBackIntersections(GeoIoFormat.SHAPE);
  }

  /** With both intersection flags off no intersection file is written, while nodes still are */
  @Test
  public void bothFlagsOffWritesNoIntersectionFiles() {
    var outputPath = writeThreeIntersectionsNetwork(GeoIoFormat.GEOPACKAGE, "flags_off", writer -> {
      writer.getSettings().setPersistIntersectionNodes(false);
      writer.getSettings().setPersistIntersectionSegments(false);
    });
    assertTrue(findIntersectionFileNames(outputPath).isEmpty());
    assertEquals(9,
        GeoIoOutputReaderUtils.readFeatures(outputPath, "layer_0_planit_nodes", GeoIoFormat.GEOPACKAGE).size());
  }

  /** Each intersection flag controls only its own file */
  @Test
  public void eachFlagControlsOnlyItsFile() {
    var format = GeoIoFormat.GEOPACKAGE;
    var segmentsOnlyPath = writeThreeIntersectionsNetwork(format, "segments_only",
        writer -> writer.getSettings().setPersistIntersectionNodes(false));
    assertEquals(
        List.of(INTERSECTION_SEGMENTS_TABLE + format.extension()), findIntersectionFileNames(segmentsOnlyPath));

    var intersectionsOnlyPath = writeThreeIntersectionsNetwork(format, "intersections_only",
        writer -> writer.getSettings().setPersistIntersectionSegments(false));
    assertEquals(
        List.of(INTERSECTION_NODES_TABLE + format.extension()), findIntersectionFileNames(intersectionsOnlyPath));
  }

  /** A network without intersections gets no intersection files */
  @Test
  public void networkWithoutIntersectionsWritesNoIntersectionFiles() {
    var outputPath = NetworkTestUtils.writeCartesianNetworkToEmptiedDirectory(
        PlanitNetworkReaderFactory.create(GRID10x10_INPUT_PATH.toString()).read(),
        GeoIoFormat.GEOPACKAGE,
        OUTPUT_PATH.resolve("grid10x10"),
        writer -> {});

    assertTrue(outputPath.resolve("layer_0_planit_nodes" + GeoIoFormat.GEOPACKAGE.extension()).toFile().exists());
    assertTrue(findIntersectionFileNames(outputPath).isEmpty());
  }

  /** A conjugate network has no intersections, so only the regular network gets intersection files */
  @Test
  public void conjugateNetworkWritesNoIntersectionFiles() {
    var format = GeoIoFormat.SHAPE;
    var outputPath = writeThreeIntersectionsNetwork(format, "conjugate",
        writer -> writer.getSettings().setPersistConjugateNetwork(true));

    var intersectionFileNames = findIntersectionFileNames(outputPath);
    assertTrue(intersectionFileNames.stream().noneMatch(name -> name.startsWith("conjugate_")));
    assertTrue(intersectionFileNames.contains(INTERSECTION_NODES_TABLE + format.extension()));
    assertTrue(intersectionFileNames.contains(INTERSECTION_SEGMENTS_TABLE + format.extension()));
    assertTrue(Arrays.stream(outputPath.toFile().listFiles()).anyMatch(f -> f.getName().startsWith("conjugate_")));
  }
}
