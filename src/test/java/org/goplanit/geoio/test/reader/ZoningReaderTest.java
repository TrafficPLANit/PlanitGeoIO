package org.goplanit.geoio.test.reader;

import org.geotools.api.filter.Filter;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.goplanit.geoio.converter.zoning.GeometryZoningReaderFactory;
import org.goplanit.geoio.converter.zoning.GeometryZoningReaderSettings;
import org.goplanit.logging.Logging;
import org.goplanit.utils.geo.PlanitCrsUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ZoningReaderTest {

  private static Logger LOGGER = null;

  private static final String TRAVEL_ZONE_PATH = "src/test/resources/reader_test/travel_zones/tz21_combined.shp";

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

    var zoningReader = GeometryZoningReaderFactory.create(TRAVEL_ZONE_PATH, NSW_LAMBERT_CRS);
    zoningReader.getSettings().setZoneIdField(idFieldName);
    zoningReader.getSettings().setZoneLayerName(layerName);

    // Filter down to Sydney GMA
    var ff = GeometryZoningReaderSettings.getFilterFactory();
    Filter filter = ff.less(
        ff.property(idFieldName),   // attribute name
        ff.literal(7000)        // comparison value
    );

    zoningReader.getSettings().setGisFilter(filter);

    var zoning = zoningReader.read();

    assertNotNull(zoning, "zoning should not be null");
    Assertions.assertEquals(3378, zoning.getOdZones().size(),
        "Number of OD zones does not match expected");
  }
}

