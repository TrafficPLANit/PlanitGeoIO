package org.goplanit.geoio.test.integration;

import org.goplanit.converter.network.NetworkConverterFactory;
import org.goplanit.geoio.converter.network.GeometryNetworkWriter;
import org.goplanit.geoio.converter.network.GeometryNetworkWriterFactory;
import org.goplanit.geoio.converter.network.GeometryNetworkWriterSettings;
import org.goplanit.geoio.util.GeoIoFormat;
import org.goplanit.io.converter.network.PlanitNetworkReader;
import org.goplanit.io.converter.network.PlanitNetworkReaderFactory;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetworkUtils;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.locale.CountryNames;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.logging.Logger;

import static org.goplanit.geoio.test.util.NetworkOutputAssertionUtils.assertNodeLinkAndLinkSegmentRowCounts;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * JUnit test cases demonstrating how to construct a conjugate PLANit network from a PLANit regular network and persist
 * the result in shape file format
 *
 * @author markr
 *
 */
public class GeoIoConjugateNetworkConverterTest {

  /** the logger */
  private static Logger LOGGER = null;

  private static final Path TEST_CASE_PATH = Path.of("src","test","resources");

  private static final String PROJECT_PATH = Path.of(TEST_CASE_PATH.toString(),"converter_test").toString();

  private static final String SIOUX_FALLS_INPUT_PATH = Path.of(PROJECT_PATH, "input", "siouxfalls").toString();
  private static final String SIOUX_FALLS_OUTPUT_PATH = Path.of(PROJECT_PATH, "outputs","siouxfalls").toString();

  /**
   * Create the logger, once
   */
  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(GeoIoConjugateNetworkConverterTest.class);
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
  
  /**
   * Test that reading a PLANit network in native format and then writing results in Shape file form for both
   * the regular network and conjugate representation of that network
   */
  @Test
  public void testPlanit2GeoIOShapeConjugateNetworkConverter() {
    try {

      /* reader */
      PlanitNetworkReader planitReader = PlanitNetworkReaderFactory.create();
      planitReader.getSettings().setInputDirectory(SIOUX_FALLS_INPUT_PATH);

      /* writer */
      GeometryNetworkWriter geometryWriter =
              GeometryNetworkWriterFactory.create(SIOUX_FALLS_OUTPUT_PATH, CountryNames.UNITED_STATES_OF_AMERICA);

      /* trigger persisting conjugate network as well */
      geometryWriter.getSettings().setPersistConjugateNetwork(true);

      /* id mapping based on XML, easier to read (and knowing XML ids are unique in this case*/
      geometryWriter.setIdMapperType(IdMapperType.XML);
      geometryWriter.getSettings().setFormat(GeoIoFormat.SHAPE);
      
      /* convert */
      NetworkConverterFactory.create(planitReader, geometryWriter).convert();

      /* conjugate created as the writer does, conjugate layer files are named after its absent XML id */
      var network = PlanitNetworkReaderFactory.create(SIOUX_FALLS_INPUT_PATH).read();
      var conjugateNetwork = network.createConjugate(
          MacroscopicNetworkUtils.generateDerivedConjugateIdGroupingToken(network), null);
      assertNodeLinkAndLinkSegmentRowCounts(network, Path.of(SIOUX_FALLS_OUTPUT_PATH), "", GeoIoFormat.SHAPE);
      assertNodeLinkAndLinkSegmentRowCounts(
          conjugateNetwork,
          Path.of(SIOUX_FALLS_OUTPUT_PATH),
          GeometryNetworkWriterSettings.DEFAULT_CONJUGATE_PREFIX,
          GeoIoFormat.SHAPE);

    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
      fail("testPlanit2GeoIOShapeConjugateNetworkConverter");
    }
  }

}