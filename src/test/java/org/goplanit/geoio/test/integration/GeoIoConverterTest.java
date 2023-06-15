package org.goplanit.geoio.test.integration;

import org.goplanit.converter.idmapping.IdMapperType;
import org.goplanit.converter.network.NetworkConverterFactory;
import org.goplanit.converter.service.ServiceNetworkConverterFactory;
import org.goplanit.converter.zoning.ZoningConverterFactory;
import org.goplanit.geoio.converter.network.GeometryNetworkWriter;
import org.goplanit.geoio.converter.network.GeometryNetworkWriterFactory;
import org.goplanit.geoio.converter.service.GeometryRoutedServicesWriterFactory;
import org.goplanit.geoio.converter.service.GeometryServiceNetworkWriterFactory;
import org.goplanit.geoio.converter.zoning.GeometryZoningWriterFactory;
import org.goplanit.io.converter.intermodal.PlanitIntermodalReaderFactory;
import org.goplanit.io.converter.network.PlanitNetworkReader;
import org.goplanit.io.converter.network.PlanitNetworkReaderFactory;
import org.goplanit.io.converter.service.PlanitServiceNetworkReaderFactory;
import org.goplanit.io.converter.zoning.PlanitZoningReaderFactory;
import org.goplanit.io.converter.zoning.PlanitZoningReaderSettings;
import org.goplanit.logging.Logging;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.locale.CountryNames;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.logging.Logger;

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
public class GeoIoConverterTest {

  /** the logger */
  private static Logger LOGGER = null;

  private static final Path testCasePath = Path.of("src","test","resources");

  private final String projectPath = Path.of(testCasePath.toString(),"converter_test").toString();

  /* the files in this location were originally sourced from PLANitIO converter test (src/test/resources/testcases/converter_test/input) */
  private final String inputPath = Path.of(projectPath, "input", "melbourne").toString();
  private final String outputPath = Path.of(projectPath, "outputs","melbourne").toString();

  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(GeoIoConverterTest.class);
    } 
  }

  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
    IdGenerator.reset();
  }
  
  /**
   * Test that reading a PLANit network in native format and then writing results in Shape file form
   */
  @Test
  public void testPlanit2GeoIOShapeNetworkConverter() {
    try {

      /* reader */
      PlanitNetworkReader planitReader = PlanitNetworkReaderFactory.create();
      planitReader.getSettings().setInputDirectory(inputPath);
      
      /* writer */
      GeometryNetworkWriter geometryWriter = GeometryNetworkWriterFactory.create(outputPath, CountryNames.AUSTRALIA);
      
      /* convert */
      NetworkConverterFactory.create(planitReader, geometryWriter).convert();

      /* id mapping based on XML, easier to read (and knowing XML ids are unique in this case*/
      geometryWriter.setIdMapperType(IdMapperType.XML);

      //todo used as an example rather than test

    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
      fail("testPlanit2GeoIOShapeNetworkConverter");
    }
  }

  /**
   * Test that reading a PLANit network in native format and then writing results in Shape file form
   */
  @Test
  public void testPlanit2GeoIOShapeServiceNetworkConverter() {
    try {
      /* service network reader */
      var planitReader = PlanitServiceNetworkReaderFactory.create(inputPath, PlanitNetworkReaderFactory.create(inputPath).read());

      /* writer */
      var geometryWriter = GeometryServiceNetworkWriterFactory.create(outputPath, CountryNames.AUSTRALIA);

      /* id mapping based on XML, easier to read (and knowing XML ids are unique in this case*/
      geometryWriter.setIdMapperType(IdMapperType.XML);

      /* convert */
      ServiceNetworkConverterFactory.create(planitReader, geometryWriter).convert();

      //todo used as an example rather than test

    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
      fail("testPlanit2GeoIOShapeServiceNetworkConverter");
    }
  }

  /**
   * Test reading a PLANit zoning in native format and then writing results in Shape file form
   */
  @Test
  public void testPlanit2GeoIOShapeZoningConverter() {
    try {
      /* PLANit network */
      var network = PlanitNetworkReaderFactory.create(inputPath).read();

      /* PLANit zoning reader */
      var reader = PlanitZoningReaderFactory.create(
          new PlanitZoningReaderSettings(inputPath), network);

      /* writer */
      var geometryWriter = GeometryZoningWriterFactory.create(
          outputPath, CountryNames.AUSTRALIA, network.getCoordinateReferenceSystem());

      /* also persist virtual network, i.e., the relation between zones and connectoids, including the virtual edges/edge segments */
      geometryWriter.getSettings().setPersistVirtualNetwork(true);
      /* make sure virtual network is populated by constructing integrated transport model network */
      new TransportModelNetwork(network, reader.read()).integrateTransportNetworkViaConnectoids();

      /* convert */
      ZoningConverterFactory.create(reader, geometryWriter).convert();

      //todo used as an example rather than test

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
  public void testPlanit2GeoIOShapeRoutedServicesConverter() {
    try {
      /* PLANit reader */
      var reader = PlanitIntermodalReaderFactory.create(inputPath);
      var result = reader.readWithServices();

      /* GEO writer */
      var geometryWriter = GeometryRoutedServicesWriterFactory.create(outputPath, CountryNames.AUSTRALIA);

      /* persist */
      geometryWriter.write(result.fourth());

      //todo add assertions...

    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
      fail("testPlanit2GeoIOShapeRoutedServicesConverter");
    }
  }

}