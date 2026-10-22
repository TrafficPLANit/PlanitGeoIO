package org.goplanit.geoio.test.integration;

import org.apache.commons.io.FileUtils;
import org.goplanit.converter.intermodal.IntermodalConverterFactory;
import org.goplanit.geoio.converter.intermodal.GeometryIntermodalWriter;
import org.goplanit.geoio.converter.intermodal.GeometryIntermodalWriterFactory;
import org.goplanit.geoio.converter.service.GeometryRoutedServicesWriterSettings;
import org.goplanit.geoio.converter.service.GeometryServiceNetworkWriterSettings;
import org.goplanit.geoio.test.util.GeoIoOutputReaderUtils;
import org.goplanit.geoio.test.util.NetworkTestUtils;
import org.goplanit.geoio.util.GeoIoFormat;
import org.goplanit.io.converter.intermodal.PlanitIntermodalReaderFactory;
import org.goplanit.logging.Logging;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.zoning.Zoning;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the GeoIO intermodal writer applies the network, zoning, service network and routed services settings set
 * through its own settings
 *
 * @author markr
 */
public class GeoIoIntermodalWriterSettingsAppliedTest {

  /** the logger */
  private static Logger LOGGER = null;

  private static final Path GRID10x10_INPUT_PATH =
      Path.of("src", "test", "resources", "converter_test", "input", "grid10x10");

  private static final Path SYDNEY_INPUT_PATH =
      Path.of("src", "test", "resources", "converter_test", "input", "sydney");

  private static final Path OUTPUT_PATH =
      Path.of("src", "test", "resources", "converter_test", "outputs", "intermodal_writer");

  /**
   * Create an intermodal writer for an emptied output directory, writing shapefiles with cartesian coordinates for
   * both network and zoning
   *
   * @param outputPath directory to empty and write to
   * @return created writer
   */
  private static GeometryIntermodalWriter createCartesianShapefileWriter(Path outputPath) throws IOException {
    FileUtils.deleteDirectory(outputPath.toFile());
    var writer = GeometryIntermodalWriterFactory.create(outputPath.toString(), CountryNames.GLOBAL);
    writer.getSettings().getNetworkSettings().setFormat(GeoIoFormat.SHAPE);
    writer.getSettings().getNetworkSettings().setDestinationCoordinateReferenceSystem(PlanitJtsCrsUtils.CARTESIANCRS);
    writer.getSettings().getZoningSettings().setFormat(GeoIoFormat.SHAPE);
    writer.getSettings().getZoningSettings().setDestinationCoordinateReferenceSystem(PlanitJtsCrsUtils.CARTESIANCRS);
    return writer;
  }

  /**
   * Verify if a table of the given name was written in the given format
   *
   * @param outputPath to look in
   * @param tableName of the table
   * @param format written in
   * @return true when written, false otherwise
   */
  private static boolean hasFile(Path outputPath, String tableName, GeoIoFormat format) {
    return GeoIoOutputReaderUtils.createTablePath(outputPath, tableName, format).toFile().exists();
  }

  /**
   * Verify if any GeoPackage file was written
   *
   * @param outputPath to look in
   * @return true when one was, false otherwise
   */
  private static boolean hasAnyGeoPackage(Path outputPath) {
    return Arrays.stream(outputPath.toFile().listFiles())
        .anyMatch(f -> f.getName().endsWith(GeoIoFormat.GEOPACKAGE.extension()));
  }

  /**
   * Verify if any file written has a name containing the given text
   *
   * @param outputPath to look in
   * @param fileName the text the file name contains
   * @return true when one has, false otherwise
   */
  private static boolean hasFileNameContaining(Path outputPath, String fileName) {
    return Arrays.stream(outputPath.toFile().listFiles()).anyMatch(f -> f.getName().contains(fileName));
  }

  /**
   * Write the Sydney network, zoning and services with the given writer
   *
   * @param writer to use
   * @throws PlanItException when reading or writing fails
   */
  private static void writeSydneyWithServices(GeometryIntermodalWriter writer) throws PlanItException {
    var reader = PlanitIntermodalReaderFactory.create(SYDNEY_INPUT_PATH.toString());
    IntermodalConverterFactory.create(reader, writer).convertWithServices();
  }

  /**
   * Create the logger, once
   */
  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(GeoIoIntermodalWriterSettingsAppliedTest.class);
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

  /** The format and the intersection flag set on the network settings of the intermodal writer are applied */
  @Test
  public void networkSettingsAreApplied() throws IOException {
    var outputPath = OUTPUT_PATH.resolve("network_settings");
    var writer = createCartesianShapefileWriter(outputPath);
    writer.getSettings().getNetworkSettings().setPersistIntersectionNodes(false);
    writer.getSettings().getZoningSettings().setPersistVirtualNetwork(false);

    var network = NetworkTestUtils.readThreeIntersectionsNetwork();
    var emptyZoning = new Zoning(IdGroupingToken.collectGlobalToken(), network.getNetworkGroupingTokenId());
    emptyZoning.setCoordinateReferenceSystem(network.getCoordinateReferenceSystem());
    writer.write(network, emptyZoning);

    assertTrue(hasFile(outputPath, "layer_0_planit_nodes", GeoIoFormat.SHAPE));
    assertTrue(hasFile(outputPath, "layer_0_planit_intersection_segments", GeoIoFormat.SHAPE));
    assertFalse(hasFile(outputPath, "layer_0_planit_intersection_nodes", GeoIoFormat.SHAPE));
    assertFalse(hasAnyGeoPackage(outputPath));
  }

  /** The format set on the zoning settings of the intermodal writer is applied */
  @Test
  public void zoningSettingsAreApplied() throws IOException {
    var outputPath = OUTPUT_PATH.resolve("zoning_settings");
    var writer = createCartesianShapefileWriter(outputPath);

    var networkAndZoning = PlanitIntermodalReaderFactory.create(GRID10x10_INPUT_PATH.toString()).read();
    writer.write(networkAndZoning.first(), networkAndZoning.second());

    assertTrue(hasFile(outputPath, "planit_connectoids_od", GeoIoFormat.SHAPE));
    assertFalse(hasAnyGeoPackage(outputPath));
  }

  /** The file name set on the service network settings of the intermodal writer is applied */
  @Test
  public void serviceNetworkSettingsAreApplied() throws IOException, PlanItException {
    var outputPath = OUTPUT_PATH.resolve("service_network_settings");
    var writer = createCartesianShapefileWriter(outputPath);
    writer.getSettings().getServiceNetworkSettings().setServiceNodesFileName("custom_service_nodes");

    writeSydneyWithServices(writer);

    assertTrue(hasFileNameContaining(outputPath, "custom_service_nodes"));
    assertFalse(hasFileNameContaining(outputPath,
        GeometryServiceNetworkWriterSettings.DEFAULT_SERVICE_NODES_FILE_NAME));
  }

  /** The file name set on the routed services settings of the intermodal writer is applied */
  @Test
  public void routedServicesSettingsAreApplied() throws IOException, PlanItException {
    var outputPath = OUTPUT_PATH.resolve("routed_services_settings");
    var writer = createCartesianShapefileWriter(outputPath);
    writer.getSettings().getRoutedServicesSettings().setServicesFileName("custom_services");

    writeSydneyWithServices(writer);

    assertTrue(hasFileNameContaining(outputPath, "custom_services"));
    /* the dot keeps the service network's planit_service_legs and similar from matching */
    assertFalse(hasFileNameContaining(outputPath,
        GeometryRoutedServicesWriterSettings.DEFAULT_SERVICES_FILE_NAME + "."));
  }
}
