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

  /** Intersections, their segments and their movements are persisted by default, under their default file names, and
   * the geometry of a movement runs through the internal segments it passes */
  @Test
  public void intersectionSettingsDefaults() {
    var settings = new GeometryNetworkWriterSettings();
    assertTrue(GeometryNetworkWriterSettings.DEFAULT_PERSIST_INTERSECTION_NODES);
    assertTrue(GeometryNetworkWriterSettings.DEFAULT_PERSIST_INTERSECTION_SEGMENTS);
    assertTrue(GeometryNetworkWriterSettings.DEFAULT_PERSIST_INTERSECTION_MOVEMENTS);
    assertTrue(GeometryNetworkWriterSettings.DEFAULT_INTERSECTION_MOVEMENT_GEOMETRY_THROUGH_INTERNAL_SEGMENTS);
    assertTrue(settings.isPersistIntersectionNodes());
    assertTrue(settings.isPersistIntersectionSegments());
    assertTrue(settings.isPersistIntersectionMovements());
    assertTrue(settings.isIntersectionMovementGeometryThroughInternalSegments());
    assertEquals("planit_intersection_nodes", GeometryNetworkWriterSettings.DEFAULT_INTERSECTION_NODES_FILE_NAME);
    assertEquals("planit_intersection_segments", GeometryNetworkWriterSettings.DEFAULT_INTERSECTION_SEGMENTS_FILE_NAME);
    assertEquals(
        "planit_intersection_movements", GeometryNetworkWriterSettings.DEFAULT_INTERSECTION_MOVEMENTS_FILE_NAME);
    assertEquals("planit_intersection_nodes", settings.getIntersectionNodesFileName());
    assertEquals("planit_intersection_segments", settings.getIntersectionSegmentsFileName());
    assertEquals("planit_intersection_movements", settings.getIntersectionMovementsFileName());
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
    assertTrue(settings.isPersistIntersectionMovements());

    settings.setPersistIntersectionMovements(false);
    assertFalse(settings.isPersistIntersectionMovements());
    settings.setIntersectionMovementGeometryThroughInternalSegments(false);
    assertFalse(settings.isIntersectionMovementGeometryThroughInternalSegments());

    settings.setIntersectionNodesFileName("isect");
    settings.setIntersectionSegmentsFileName("isect_segs");
    settings.setIntersectionMovementsFileName("isect_mvts");
    assertEquals("isect", settings.getIntersectionNodesFileName());
    assertEquals("isect_segs", settings.getIntersectionSegmentsFileName());
    assertEquals("isect_mvts", settings.getIntersectionMovementsFileName());
  }
}
