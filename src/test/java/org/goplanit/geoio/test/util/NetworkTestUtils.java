package org.goplanit.geoio.test.util;

import org.apache.commons.io.FileUtils;
import org.goplanit.geoio.converter.network.GeometryNetworkWriter;
import org.goplanit.geoio.converter.network.GeometryNetworkWriterFactory;
import org.goplanit.geoio.util.GeoIoFormat;
import org.goplanit.io.converter.network.PlanitNetworkReaderFactory;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Reading and writing the networks the GeoIO tests use
 *
 * @author markr
 */
public final class NetworkTestUtils {

  /** directory holding the network.xml of the small test network with three intersections */
  private static final Path THREE_INTERSECTIONS_INPUT_PATH =
      Path.of("src", "test", "resources", "converter_test", "input", "intersections");

  /** static utility class, not to be instantiated */
  private NetworkTestUtils() {
  }

  /**
   * Read the small test network with three intersections in one layer (id 0): i1 a signalised single node junction
   * and crossing with four approaches, i2 an unsignalised crossing without external id, and i3 a signalised junction
   * of nodes 5 and 6 with internal segments 10 and 11. Its coordinates are cartesian.
   *
   * @return network read
   */
  public static MacroscopicNetwork readThreeIntersectionsNetwork() {
    return (MacroscopicNetwork) PlanitNetworkReaderFactory.create(THREE_INTERSECTIONS_INPUT_PATH.toString()).read();
  }

  /**
   * Read the small test network with three intersections and collect its only layer, see
   * {@link #readThreeIntersectionsNetwork()}
   *
   * @return layer with the three intersections
   */
  public static MacroscopicNetworkLayer readThreeIntersectionsNetworkLayer() {
    return readThreeIntersectionsNetwork().getTransportLayers().getByXmlId("0");
  }

  /**
   * Write a network with cartesian coordinates with the GeoIO network writer to an emptied output directory, so files
   * of earlier runs cannot be mistaken for written ones
   *
   * @param network to write
   * @param format to write in
   * @param outputPath directory to empty and write to
   * @param configureWriter changes to the writer settings before writing
   * @return output directory written to
   */
  public static Path writeCartesianNetworkToEmptiedDirectory(
      LayeredNetwork<?, ?> network,
      GeoIoFormat format,
      Path outputPath,
      Consumer<GeometryNetworkWriter> configureWriter) {
    try {
      FileUtils.deleteDirectory(outputPath.toFile());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    var writer = GeometryNetworkWriterFactory.create(outputPath.toString());
    writer.getSettings().setDestinationCoordinateReferenceSystem(PlanitJtsCrsUtils.CARTESIANCRS);
    writer.getSettings().setFormat(format);
    configureWriter.accept(writer);
    writer.write(network);
    return outputPath;
  }
}
