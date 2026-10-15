package org.goplanit.geoio.test.converter.network;

import org.goplanit.geoio.converter.network.GeometryNetworkWriterSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the defaults and setters of {@link GeometryNetworkWriterSettings}
 *
 * @author markr
 */
public class GeometryNetworkWriterSettingsTest {

  /** Intersections and their segments are persisted by default, under their default file names */
  @Test
  public void intersectionSettingsDefaults() {
    var settings = new GeometryNetworkWriterSettings();
    assertTrue(GeometryNetworkWriterSettings.DEFAULT_PERSIST_INTERSECTION_NODES);
    assertTrue(GeometryNetworkWriterSettings.DEFAULT_PERSIST_INTERSECTION_SEGMENTS);
    assertTrue(settings.isPersistIntersectionNodes());
    assertTrue(settings.isPersistIntersectionSegments());
    assertEquals("planit_intersection_nodes", GeometryNetworkWriterSettings.DEFAULT_INTERSECTION_NODES_FILE_NAME);
    assertEquals("planit_intersection_segments", GeometryNetworkWriterSettings.DEFAULT_INTERSECTION_SEGMENTS_FILE_NAME);
    assertEquals("planit_intersection_nodes", settings.getIntersectionNodesFileName());
    assertEquals("planit_intersection_segments", settings.getIntersectionSegmentsFileName());
  }

  /** Each intersection setting can be changed independently of the others */
  @Test
  public void intersectionSettingsSetters() {
    var settings = new GeometryNetworkWriterSettings();
    settings.setPersistIntersectionNodes(false);
    assertFalse(settings.isPersistIntersectionNodes());
    assertTrue(settings.isPersistIntersectionSegments());

    settings.setPersistIntersectionSegments(false);
    assertFalse(settings.isPersistIntersectionSegments());

    settings.setIntersectionNodesFileName("isect");
    settings.setIntersectionSegmentsFileName("isect_segs");
    assertEquals("isect", settings.getIntersectionNodesFileName());
    assertEquals("isect_segs", settings.getIntersectionSegmentsFileName());
  }
}
