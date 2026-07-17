package org.goplanit.geoio.test.reader;

import org.geotools.api.filter.Filter;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.goplanit.geoio.converter.zoning.GeometryZoningReaderFactory;
import org.goplanit.geoio.converter.zoning.GeometryZoningReaderSettings;
import org.goplanit.io.converter.zoning.PlanitZoningWriterFactory;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.geo.PlanitCrsUtils;
import org.goplanit.utils.id.IdMapperType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.Input;
import org.xmlunit.matchers.CompareMatcher;

import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ZoningReaderTest {

  private static Logger LOGGER = null;

  private static final String RESOURCE_PATH = Path.of("src","test","resources").toAbsolutePath().toString();

  private static final String TRAVEL_ZONE_PATH =
      Path.of(RESOURCE_PATH,"reader_test","travel_zones").toAbsolutePath().toString();

  private static final String TRAVEL_ZONE_SHAPE_PATH =
      Path.of(TRAVEL_ZONE_PATH, "input", "tz21_combined.shp").toAbsolutePath().toString();

  private static final CoordinateReferenceSystem NSW_LAMBERT_CRS =
      PlanitCrsUtils.createCoordinateReferenceSystem("EPSG:8058");

  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(ZoningReaderTest.class);
    }
  }

  @Test
  @DisplayName("Verify parsing of zoning shape works properly")
  void testZoningActivitySimZoningReader() {

    final String idFieldName = "tz21_code";
    final String layerName = "tz21_combined";

    var zoningReader = GeometryZoningReaderFactory.create(TRAVEL_ZONE_SHAPE_PATH, NSW_LAMBERT_CRS);
    zoningReader.getSettings().setZoneIdField(idFieldName);
    zoningReader.getSettings().setZoneLayerName(layerName);
    // transplant external ids onto PLANit XML ids
    zoningReader.getSettings().setIdMapperType(IdMapperType.XML);

    // Filter down to Sydney GMA
    var ff = GeometryZoningReaderSettings.getFilterFactory();
    Filter filter = ff.less(
        ff.property(idFieldName),   // attribute name
        ff.literal(7000)        // comparison value
    );

    zoningReader.getSettings().setGisFilter(filter);
    var zoning = zoningReader.read();

    assertNotNull(zoning, "Zoning should not be null");
    Assertions.assertEquals(3378, zoning.getOdZones().size(),
        "Number of OD zones does not match expected");

    final String PLANIT_REF_PATH = Path.of(TRAVEL_ZONE_PATH, "reference","zoning.xml").toAbsolutePath().toString();
    final String PLANIT_OUTPUT_PATH = TRAVEL_ZONE_PATH;
    var writer = PlanitZoningWriterFactory.create(PLANIT_OUTPUT_PATH, zoningReader.getReferenceNetwork());
    writer.getSettings().setRemoveDanglingZones(false);
    writer.write(zoning);

    // compare produced PLANit zoning in its entirety
    org.hamcrest.MatcherAssert.assertThat(
        Input.fromFile(
            Path.of(PLANIT_OUTPUT_PATH,"zoning.xml").toAbsolutePath().toString()),
        CompareMatcher.isSimilarTo(Input.fromFile(PLANIT_REF_PATH))
            .ignoreWhitespace()
            .normalizeWhitespace()
            .ignoreComments());
  }
}

