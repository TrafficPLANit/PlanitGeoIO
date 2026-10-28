package org.goplanit.geoio.util;

import static org.goplanit.geoio.util.NetworkOutputAssertionUtils.assertRowCount;

import java.nio.file.Path;

import org.goplanit.geoio.converter.zoning.GeometryZoningWriterSettings;
import org.goplanit.zoning.Zoning;

/**
 * Assertions on the zoning files a GeoIO zoning writer persisted, read back from disk, for tests in any project. File
 * names follow the default settings. A failed assertion throws an {@link AssertionError}, which test frameworks report
 * as a failure.
 *
 * @author markr
 */
public final class ZoningOutputAssertionUtils {

  /** static utility class, not to be instantiated */
  private ZoningOutputAssertionUtils() {
  }

  /**
   * Assert that the transfer connectoids of the zoning are written one row each, and its od connectoids too, each when
   * the zoning has any and the writer settings persist connectoids, in the output directory and format of the settings
   *
   * @param zoning that was written
   * @param settings the zoning was written with
   */
  public static void assertPersistedConnectoidRowCounts(Zoning zoning, GeometryZoningWriterSettings settings) {
    if (!settings.isPersistTransferConnectoids()) {
      return;
    }
    var outputPath = Path.of(settings.getOutputDirectory());
    if (zoning.hasTransferConnectoids()) {
      assertRowCount(zoning.getTransferConnectoids().size(),
          outputPath, settings.getTransferConnectoidsFileName(), settings.getFormat());
    }
    if (zoning.hasOdConnectoids()) {
      assertRowCount(zoning.getOdConnectoids().size(),
          outputPath, settings.getOdConnectoidsFileName(), settings.getFormat());
    }
  }
}
