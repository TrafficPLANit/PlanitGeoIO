package org.goplanit.geoio.test.integration;

import org.goplanit.geoio.util.GeoIoFormat;
import org.goplanit.network.transport.TransportModelNetworkImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.converter.intermodal.IntermodalConverterFactory;
import org.goplanit.converter.network.NetworkConverterFactory;
import org.goplanit.converter.service.ServiceNetworkConverterFactory;
import org.goplanit.converter.zoning.ZoningConverterFactory;
import org.goplanit.geoio.converter.intermodal.GeometryIntermodalWriterFactory;
import org.goplanit.geoio.converter.network.GeometryNetworkWriter;
import org.goplanit.geoio.converter.network.GeometryNetworkWriterFactory;
import org.goplanit.geoio.converter.service.GeometryRoutedServicesWriterFactory;
import org.goplanit.geoio.converter.service.GeometryServiceNetworkWriterFactory;
import org.goplanit.geoio.converter.zoning.GeometryZoningWriterFactory;
import org.goplanit.geoio.util.GeoIoAssertionUtils;
import org.goplanit.io.converter.intermodal.PlanitIntermodalReaderFactory;
import org.goplanit.io.converter.network.PlanitNetworkReader;
import org.goplanit.io.converter.network.PlanitNetworkReaderFactory;
import org.goplanit.io.converter.service.PlanitServiceNetworkReaderFactory;
import org.goplanit.io.converter.zoning.PlanitZoningReaderFactory;
import org.goplanit.io.converter.zoning.PlanitZoningReaderSettings;
import org.goplanit.logging.Logging;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.locale.CountryNames;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.goplanit.geoio.util.GeoIoAssertionUtils.featureCounts;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * JUnit test cases for the converters provided in the PlanitGeoIO format. This is currently not so much a test but more
 * of an example on how to setup a simple GIS exporter for a PLANit saved network with or without services.
 * <p>
 *   Change the input or output path to persist either the Sydney or Melbourne network, respectively
 * </p>
 * 
 * @author markr
 *
 */
public class GeoIoStandardConvertersTest {

  /** the logger */
  private static Logger LOGGER = null;

  /** absolute coordinate tolerance when comparing GIS outputs */
  private static final double GIS_COORDINATE_TOLERANCE = 1e-6;

  private static final Path TEST_CASE_PATH =
      Path.of("src","test","resources");

  private static final String PROJECT_PATH =
      Path.of(TEST_CASE_PATH.toString(),"converter_test").toString();

  private static final String PROJECT_REFERENCE_PATH =
      Path.of(PROJECT_PATH, "reference").toString();

  private static final String PROJECT_OUTPUT_PATH =
      Path.of("target", "test-output", "converter_test").toString();

  /* the files in this location were originally sourced from PLANitIO converter test
  (src/test/resources/testcases/converter_test/input) */
  private static final String MELBOURNE_INPUT_PATH =
      Path.of(PROJECT_PATH, "input", "melbourne").toString();

  /* the files in this location were originally sourced from PLANitGtfs converter test output in
  (src/test/resources/testcases/sydney) */
  private static final String SYDNEY_INPUT_PATH =
      Path.of(PROJECT_PATH, "input", "sydney").toString();

  /* the files in this location were originally sourced from PLANitGtfs converter test output in
  src/test/resources/testcases/grid10x10/reference */
  private static final String GRID10x10_INPUT_PATH =
      Path.of(PROJECT_PATH, "input", "grid10x10").toString();
  private static final String MELBOURNE_GPKG_NETWORK_OUTPUT_PATH =
      Path.of(PROJECT_OUTPUT_PATH, "outputs", "melbourne", "gpkg", "network").toString();
  private static final String MELBOURNE_GPKG_SERVICE_NETWORK_OUTPUT_PATH =
      Path.of(PROJECT_OUTPUT_PATH, "outputs", "melbourne", "gpkg", "service_network").toString();
  private static final String MELBOURNE_GPKG_ZONING_NETWORK_OUTPUT_PATH =
      Path.of(PROJECT_OUTPUT_PATH, "outputs", "melbourne", "gpkg", "zoning_network").toString();
  private static final String MELBOURNE_GPKG_ROUTED_SERVICES_OUTPUT_PATH =
      Path.of(PROJECT_OUTPUT_PATH, "outputs", "melbourne", "gpkg", "routed_services").toString();
  private static final String MELBOURNE_SHAPE_ZONING_NETWORK_OUTPUT_PATH =
      Path.of(PROJECT_OUTPUT_PATH, "outputs", "melbourne", "shape", "zoning_network").toString();
  private static final String MELBOURNE_SHAPE_INTERMODAL_OUTPUT_PATH =
      Path.of(PROJECT_OUTPUT_PATH, "outputs", "melbourne", "shape", "intermodal").toString();
  private static final String SYDNEY_OUTPUT_PATH =
      Path.of(PROJECT_OUTPUT_PATH, "outputs", "sydney", "network").toString();
  private static final String GRID10x10_OUTPUT_PATH =
      Path.of(PROJECT_OUTPUT_PATH, "outputs", "grid10x10", "network").toString();
  private static final String GRID10x10_REFERENCE_PATH =
      Path.of(PROJECT_REFERENCE_PATH, "grid10x10", "network").toString();

  private static final Map<String, Integer> MELBOURNE_GPKG_NETWORK_FEATURE_COUNTS = featureCounts(
      "layer_all_planit_links.gpkg", 1353,
      "layer_all_planit_linksegments.gpkg", 2675,
      "layer_all_planit_nodes.gpkg", 1131);

  private static final Map<String, Integer> MELBOURNE_GPKG_SERVICE_NETWORK_FEATURE_COUNTS = featureCounts(
      "layer_0_planit_service_legs.gpkg", 88,
      "layer_0_planit_service_legsegments.gpkg", 88,
      "layer_0_planit_service_nodes.gpkg", 100);

  private static final Map<String, Integer> MELBOURNE_GPKG_ZONING_NETWORK_FEATURE_COUNTS = featureCounts(
      "layer_all_planit_links.gpkg", 1353,
      "layer_all_planit_linksegments.gpkg", 2675,
      "layer_all_planit_nodes.gpkg", 1131,
      "planit_connectoid_edges.gpkg", 185,
      "planit_connectoid_segments.gpkg", 185,
      "planit_connectoids_transfer.gpkg", 364,
      "planit_zones_transfer_linestring.gpkg", 38,
      "planit_zones_transfer_point.gpkg", 248);

  private static final Map<String, Integer> MELBOURNE_GPKG_ROUTED_SERVICES_FEATURE_COUNTS = featureCounts(
      "layer_0_mode_bus_planit_service.gpkg", 53,
      "layer_0_mode_ferry_planit_service.gpkg", 6,
      "layer_0_mode_light_rail_planit_service.gpkg", 2,
      "layer_0_mode_train_planit_service.gpkg", 8);

  private static final Map<String, Integer> MELBOURNE_SHAPE_ZONING_NETWORK_FEATURE_COUNTS = featureCounts(
      "layer_all_planit_links.shp", 1353,
      "layer_all_planit_linksegments.shp", 2675,
      "layer_all_planit_nodes.shp", 1131,
      "planit_connectoid_edges.shp", 185,
      "planit_connectoid_segments.shp", 185,
      "planit_connectoids_transfer.shp", 364,
      "planit_zones_transfer_linestring.shp", 38,
      "planit_zones_transfer_point.shp", 248);

  private static final Map<String, Integer> MELBOURNE_GPKG_INTERMODAL_FEATURE_COUNTS = featureCounts(
      "layer_0_mode_bus_planit_service.gpkg", 53,
      "layer_0_mode_ferry_planit_service.gpkg", 6,
      "layer_0_mode_light_rail_planit_service.gpkg", 2,
      "layer_0_mode_train_planit_service.gpkg", 8,
      "layer_0_planit_service_legs.gpkg", 88,
      "layer_0_planit_service_legsegments.gpkg", 88,
      "layer_0_planit_service_nodes.gpkg", 100,
      "layer_all_planit_links.gpkg", 1353,
      "layer_all_planit_linksegments.gpkg", 2675,
      "layer_all_planit_nodes.gpkg", 1131,
      "planit_connectoid_edges.gpkg", 185,
      "planit_connectoid_segments.gpkg", 185,
      "planit_connectoids_transfer.gpkg", 182,
      "planit_zones_transfer_linestring.gpkg", 19,
      "planit_zones_transfer_point.gpkg", 124);

  private static final Map<String, Integer> SYDNEY_GPKG_NETWORK_FEATURE_COUNTS = featureCounts(
      "layer_all_planit_links.gpkg", 1352,
      "layer_all_planit_linksegments.gpkg", 2677,
      "layer_all_planit_nodes.gpkg", 1130);

/**
 * Test reading a PLANit zoning in native format and then writing results in Shape file form
 */
  private void runPlanit2GeoIOMelbourneZoningAndNetworkConverter(GeoIoFormat format, String outputPath) {
    /* PLANit network */
    var network = PlanitNetworkReaderFactory.create(MELBOURNE_INPUT_PATH).read();

    /* PLANit zoning reader */
    var reader = PlanitZoningReaderFactory.create(
            new PlanitZoningReaderSettings(MELBOURNE_INPUT_PATH), network);

    var geometryNetworkWriter =
        GeometryNetworkWriterFactory.create(outputPath, CountryNames.AUSTRALIA);
    geometryNetworkWriter.getSettings().setDestinationCoordinateReferenceSystem(
        PlanitJtsCrsUtils.DEFAULT_GEOGRAPHIC_CRS);

    // FORMAT - NETWORK
    geometryNetworkWriter.getSettings().setFormat(format);

    geometryNetworkWriter.write(network);

    /* writer */
    var geometryZoningWriter = GeometryZoningWriterFactory.create(outputPath, CountryNames.AUSTRALIA);
    geometryZoningWriter.getSettings().setDestinationCoordinateReferenceSystem(
        PlanitJtsCrsUtils.DEFAULT_GEOGRAPHIC_CRS);

    // FORMAT - ZONING
    geometryZoningWriter.getSettings().setFormat(format);

    /* also persist virtual network, i.e., the relation between zones and connectoids, including the virtual
    edges/edge segments */
    geometryZoningWriter.getSettings().setPersistVirtualNetwork(true);
    /* make sure virtual network is populated by constructing integrated transport model network */
    new TransportModelNetworkImpl(network, reader.read()).
        integrateTransportNetworkViaConnectoids(false);

    /* convert */
    ZoningConverterFactory.create(reader, geometryZoningWriter).convert();
  }

  /**
   * Remove previous generated output and recreate the output directory.
   *
   * @param outputPath output directory
   * @throws IOException when cleanup fails
   */
  private static void resetOutputDirectory(String outputPath) throws IOException {
    var path = Path.of(outputPath);
    if (Files.exists(path)) {
      try (Stream<Path> paths = Files.walk(path)) {
        paths.sorted(Comparator.reverseOrder()).forEach(entry -> {
          try {
            Files.delete(entry);
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });
      } catch (UncheckedIOException e) {
        throw e.getCause();
      }
    }
    Files.createDirectories(path);
  }

  /**
   * Assert generated GIS output against reference output.
   *
   * @param outputPath output directory
   * @param referencePath reference directory
   * @throws IOException when files cannot be read
   */
  private static void assertGeometryOutputSimilar(String outputPath, String referencePath) throws IOException {
    GeoIoAssertionUtils.assertGeometryFilesSimilar(outputPath, referencePath, GIS_COORDINATE_TOLERANCE);
  }

  /**
   * Assert generated GIS output exposes expected feature counts.
   *
   * @param outputPath output directory
   * @param expectedFeatureCounts expected feature counts by relative dataset path
   * @throws IOException when files cannot be read
   */
  private static void assertGeometryOutputFeatureCounts(
      String outputPath, Map<String, Integer> expectedFeatureCounts) throws IOException {
    GeoIoAssertionUtils.assertGeometryFilesHaveTotalFeatureCounts(outputPath, expectedFeatureCounts);
  }

  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(GeoIoStandardConvertersTest.class);
    } 
  }

  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
    IdGenerator.reset();
  }

  @AfterEach
  public void tearDownTest() {
    System.gc();
  }
  
  /**
   * Test that reading a PLANit network in native format and then writing results in Shape file form
   */
  @Test
  public void planit2GeoIODefaultMelbourneNetworkConverterTest() {
    try {

      /* reader */
      PlanitNetworkReader planitReader = PlanitNetworkReaderFactory.create();
      planitReader.getSettings().setInputDirectory(MELBOURNE_INPUT_PATH);
      
      /* writer */
      GeometryNetworkWriter geometryWriter =
          GeometryNetworkWriterFactory.create(MELBOURNE_GPKG_NETWORK_OUTPUT_PATH, CountryNames.AUSTRALIA);
      
      resetOutputDirectory(MELBOURNE_GPKG_NETWORK_OUTPUT_PATH);

      /* convert */
      NetworkConverterFactory.create(planitReader, geometryWriter).convert();

      /* id mapping based on XML, easier to read (and knowing XML ids are unique in this case*/
      geometryWriter.setIdMapperType(IdMapperType.XML);

      assertGeometryOutputFeatureCounts(
          MELBOURNE_GPKG_NETWORK_OUTPUT_PATH, MELBOURNE_GPKG_NETWORK_FEATURE_COUNTS);

    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
      fail("testPlanit2GeoIOShapeNetworkConverter");
    }
  }

  /**
   * Test that reading a PLANit network in native format and then writing results in Shape file form using
   * a cartesian CRS
   */
  @Test
  public void planit2GeoIODefaultGrid10x10NetworkConverterTest() {
    try {

      /* reader */
      PlanitNetworkReader planitReader = PlanitNetworkReaderFactory.create();
      planitReader.getSettings().setInputDirectory(GRID10x10_INPUT_PATH);

      /* writer */
      GeometryNetworkWriter geometryWriter = GeometryNetworkWriterFactory.create(GRID10x10_OUTPUT_PATH);
      geometryWriter.getSettings().setDestinationCoordinateReferenceSystem(PlanitJtsCrsUtils.CARTESIANCRS);

      resetOutputDirectory(GRID10x10_OUTPUT_PATH);

      /* convert */
      NetworkConverterFactory.create(planitReader, geometryWriter).convert();
      geometryWriter.setIdMapperType(IdMapperType.ID);

      assertGeometryOutputSimilar(GRID10x10_OUTPUT_PATH, GRID10x10_REFERENCE_PATH);

    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
      fail("testPlanit2GeoIOShapeGrid10x10NetworkConverter");
    }
  }

  /**
   * Test that reading a PLANit network in native format and then writing results in Shape file form
   */
  @Test
  public void planit2GeoIODefaultSydneyNetworkConverterTest() {
    try {

      /* reader */
      PlanitNetworkReader planitReader = PlanitNetworkReaderFactory.create();
      planitReader.getSettings().setInputDirectory(SYDNEY_INPUT_PATH);

      /* writer */
      GeometryNetworkWriter geometryWriter =
          GeometryNetworkWriterFactory.create(SYDNEY_OUTPUT_PATH, CountryNames.AUSTRALIA);

      resetOutputDirectory(SYDNEY_OUTPUT_PATH);

      /* convert */
      NetworkConverterFactory.create(planitReader, geometryWriter).convert();

      /* id mapping based on XML, easier to read (and knowing XML ids are unique in this case*/
      geometryWriter.setIdMapperType(IdMapperType.XML);

      assertGeometryOutputFeatureCounts(SYDNEY_OUTPUT_PATH, SYDNEY_GPKG_NETWORK_FEATURE_COUNTS);

    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
      fail("testPlanit2GeoIOShapeNetworkConverterSydney");
    }
  }

  /**
   * Test that reading a PLANit network in native format and then writing results in Shape file form
   */
  @Test
  public void planit2GeoIODefaultMelbourneServiceNetworkConverterTest() {
    try {
      /* service network reader */
      var planitReader = PlanitServiceNetworkReaderFactory.create(
          MELBOURNE_INPUT_PATH, PlanitNetworkReaderFactory.create(MELBOURNE_INPUT_PATH).read());

      /* writer */
      var geometryWriter = GeometryServiceNetworkWriterFactory.create(
              MELBOURNE_GPKG_SERVICE_NETWORK_OUTPUT_PATH, CountryNames.AUSTRALIA);

      /* id mapping based on XML, easier to read (and knowing XML ids are unique in this case*/
      geometryWriter.setIdMapperType(IdMapperType.XML);

      resetOutputDirectory(MELBOURNE_GPKG_SERVICE_NETWORK_OUTPUT_PATH);

      /* convert */
      ServiceNetworkConverterFactory.create(planitReader, geometryWriter).convert();

      assertGeometryOutputFeatureCounts(
          MELBOURNE_GPKG_SERVICE_NETWORK_OUTPUT_PATH, MELBOURNE_GPKG_SERVICE_NETWORK_FEATURE_COUNTS);

    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
      fail("testPlanit2GeoIOShapeServiceNetworkConverter");
    }
  }

  /**
   * Test reading a PLANit network and zoning in native format and then writing results in Shape file form
   */
  @Test
  public void planit2GeoIOShapeMelbourneZoningAndNetworkConverterTest() {
    try {
      resetOutputDirectory(MELBOURNE_SHAPE_ZONING_NETWORK_OUTPUT_PATH);
      runPlanit2GeoIOMelbourneZoningAndNetworkConverter(
          GeoIoFormat.SHAPE, MELBOURNE_SHAPE_ZONING_NETWORK_OUTPUT_PATH);
      assertGeometryOutputFeatureCounts(
          MELBOURNE_SHAPE_ZONING_NETWORK_OUTPUT_PATH, MELBOURNE_SHAPE_ZONING_NETWORK_FEATURE_COUNTS);
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
      fail("testPlanit2GeoIOShapeZoningConverter");
    }
  }

  /**
   * Test reading a PLANit network and zoning in native format and then writing results in Geopackage file form
   */
  @Test
  public void planit2GeoIOGeopackageMelbourneZoningAndNetworkConverterTest() {
    try {
      resetOutputDirectory(MELBOURNE_GPKG_ZONING_NETWORK_OUTPUT_PATH);
      runPlanit2GeoIOMelbourneZoningAndNetworkConverter(
          GeoIoFormat.GEOPACKAGE, MELBOURNE_GPKG_ZONING_NETWORK_OUTPUT_PATH);
      assertGeometryOutputFeatureCounts(
          MELBOURNE_GPKG_ZONING_NETWORK_OUTPUT_PATH, MELBOURNE_GPKG_ZONING_NETWORK_FEATURE_COUNTS);
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
      fail("testPlanit2GeoIOShapeZoningConverter");
    }
  }

  /**
   * Test reading a PLANit routed services in native format and then writing results in Shape file form
   */
  @Test
  public void planit2GeoIODefaultMelbourneRoutedServicesConverterTest() {
    try {
      /* PLANit reader */
      var reader = PlanitIntermodalReaderFactory.create(MELBOURNE_INPUT_PATH);
      var result = reader.readWithServices();

      /* GEO writer */
      var geometryWriter = GeometryRoutedServicesWriterFactory.create(
              MELBOURNE_GPKG_ROUTED_SERVICES_OUTPUT_PATH, CountryNames.AUSTRALIA);

      resetOutputDirectory(MELBOURNE_GPKG_ROUTED_SERVICES_OUTPUT_PATH);

      /* persist */
      geometryWriter.write(result.fourth());

      assertGeometryOutputFeatureCounts(
          MELBOURNE_GPKG_ROUTED_SERVICES_OUTPUT_PATH, MELBOURNE_GPKG_ROUTED_SERVICES_FEATURE_COUNTS);

    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
      fail("testPlanit2GeoIOShapeRoutedServicesConverter");
    }
  }

  /** Test reading a complete PLANit network, zoning, service network, and routed services and then write
   * results in Shape file form using
   * intermodal writer
   * */
  @Test
  public void planit2GeoIODefaultMelbourneIntermodalNetworkZoningConverterTest() {
    try {

      /* reader */
      var planitReader = PlanitIntermodalReaderFactory.create();
      planitReader.getSettings().setInputDirectory(MELBOURNE_INPUT_PATH);

      /* writer */
      var geometryWriter =
          GeometryIntermodalWriterFactory.create(MELBOURNE_SHAPE_INTERMODAL_OUTPUT_PATH, CountryNames.AUSTRALIA);

      /* persist connectoids as well */
      geometryWriter.getSettings().getZoningSettings().setPersistVirtualNetwork(true);

      /* id mapping based on XML, easier to read (and knowing XML ids are unique in this case*/
      geometryWriter.setIdMapperType(IdMapperType.XML);

      resetOutputDirectory(MELBOURNE_SHAPE_INTERMODAL_OUTPUT_PATH);

      /* convert */
      IntermodalConverterFactory.create(planitReader, geometryWriter).convertWithServices();

      assertGeometryOutputFeatureCounts(
          MELBOURNE_SHAPE_INTERMODAL_OUTPUT_PATH, MELBOURNE_GPKG_INTERMODAL_FEATURE_COUNTS);

    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
      fail("testPlanit2GeoIOShapeIntermodalNetworkZoningConverter");
    }
  }

}
