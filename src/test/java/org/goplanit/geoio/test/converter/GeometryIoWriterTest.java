package org.goplanit.geoio.test.converter;

import org.goplanit.geoio.converter.network.GeometryNetworkWriter;
import org.goplanit.geoio.converter.network.GeometryNetworkWriterSettings;
import org.goplanit.geoio.util.GeoIODataStoreManager;
import org.goplanit.geoio.util.GeoIoFeatureTypeBuilder;
import org.goplanit.geoio.util.GeoIoFormat;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.misc.Triple;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the writing of a table by the base GeoIO writer
 *
 * @author markr
 */
public class GeometryIoWriterTest {

  private static final Path OUTPUT_PATH =
      Path.of("src", "test", "resources", "converter_test", "outputs", "io_writer");

  /** Context for plain text entities, whose only attribute function fails */
  private static class FailingAttributeContext extends PlanitEntityFeatureTypeContext<String> {

    FailingAttributeContext() {
      super(String.class, List.of(
          Triple.of("name", "String", (Function<String, ?>) entity -> {
            throw new IllegalStateException("attribute function fails");
          }),
          Triple.of(DEFAULT_GEOMETRY_ATTRIBUTE_KEY, "Point", (Function<String, ?>) entity ->
              PlanitJtsUtils.createPoint(0, 0))));
    }
  }

  /** Network writer that writes one table of the given entities directly */
  private static class SingleTableWriter extends GeometryNetworkWriter {

    SingleTableWriter(GeometryNetworkWriterSettings settings) {
      super(settings);
    }

    void writeTable(PlanitEntityFeatureTypeContext<String> context, String schemaName, List<String> entities) {
      var featureType = GeoIoFeatureTypeBuilder.createSimpleFeatureType(
          context, PlanitJtsCrsUtils.CARTESIANCRS, schemaName);
      var outputFile = Path.of(getSettings().getOutputDirectory(), schemaName + getSettings().getFormat().extension());
      try {
        writeGeometryLayerForEntity(featureType, context, findDataStore(outputFile), schemaName, entities);
      } finally {
        GeoIODataStoreManager.reset();
      }
    }
  }

  /** An attribute function that throws makes the write fail */
  @Test
  public void failingAttributeFunctionFailsTheWrite() throws IOException {
    Files.createDirectories(OUTPUT_PATH);
    var settings = new GeometryNetworkWriterSettings(OUTPUT_PATH.toString());
    settings.setFormat(GeoIoFormat.GEOPACKAGE);
    var writer = new SingleTableWriter(settings);

    assertThrows(PlanItRunTimeException.class,
        () -> writer.writeTable(new FailingAttributeContext(), "failing_table", List.of("entity")));
  }
}
