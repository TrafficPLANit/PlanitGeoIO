package org.goplanit.geoio.test.converter.network.featurecontext;

import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.geoio.converter.network.featurecontext.PlanitIntersectionMovementFeatureTypeContext;
import org.goplanit.geoio.test.util.NetworkTestUtils;
import org.goplanit.geoio.util.GeoIoFeatureTypeBuilder;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovement;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the attributes {@link PlanitIntersectionMovementFeatureTypeContext} gives each movement of a spatial
 * intersection, on the small network with three intersections. Spatial intersection i3 over nodes 5 and 6 has four
 * movements; its approach 8 has green ratio 0.44 and saturation flow factor 0.9
 * <pre>
 *                    7 (200,100)   8 (300,100)
 *                    12|  ^13    14|  ^15
 *                      v  |        v  |
 *   4 (100,0) --8-->  5 (200,0) --10--> 6 (300,0)
 *             <--9--            <--11--
 * </pre>
 * m1 runs from 8 to 15 through 10, m4 from 8 to 13, m2 from 12 to 9 and m3 from 14 to 9 through 11.
 *
 * @author markr
 */
public class PlanitIntersectionMovementFeatureTypeContextTest {

  private MacroscopicNetworkLayer layer;

  private PlanitIntersectionMovementFeatureTypeContext context;

  /**
   * Create the movements context on the layer
   *
   * @param geometryThroughInternalSegments true for a geometry through the internal segments passed, false for only the
   *                                        from and to segment
   * @return created context
   */
  private PlanitIntersectionMovementFeatureTypeContext createContext(boolean geometryThroughInternalSegments) {
    return GeoIoFeatureTypeBuilder.createIntersectionMovementFeatureContext(
        new NetworkIdMapper(IdMapperType.XML), layer, geometryThroughInternalSegments, null);
  }

  /**
   * Collect the row of the given movement, which is the movement itself
   *
   * @param movementXmlId of the movement
   * @return row
   */
  private SpatialMovement row(String movementXmlId) {
    return layer.getSpatialMovements().stream().filter(movement -> movementXmlId.equals(movement.getXmlId()))
        .findFirst().orElseThrow();
  }

  /**
   * Apply the attribute function with the given name
   *
   * @param name of the attribute
   * @param row to apply it to
   * @return attribute value
   */
  private Object attribute(String name, SpatialMovement row) {
    Function<SpatialMovement, ?> function = context.getAttributeDescription().stream()
        .filter(a -> a.first().equals(name)).findFirst().orElseThrow().third();
    return function.apply(row);
  }

  /**
   * The geometry attribute of a row
   *
   * @param row to take it from
   * @return its multi line string
   */
  private MultiLineString geometry(SpatialMovement row) {
    return (MultiLineString) attribute(PlanitEntityFeatureTypeContext.DEFAULT_GEOMETRY_ATTRIBUTE_KEY, row);
  }

  /**
   * Reset the id generator
   */
  @AfterAll
  public static void tearDown() {
    IdGenerator.reset();
  }

  /**
   * Read the three intersections network and create the movements context, with geometries through the internal
   * segments passed
   */
  @BeforeEach
  public void setUp() {
    layer = NetworkTestUtils.readThreeIntersectionsNetworkLayer();
    context = createContext(true);
  }

  /** Attribute names are unique and fit a shapefile, one Boolean column per intersection type, geometry last */
  @Test
  public void attributeNamesAreUniqueAndShapefileSafe() {
    assertSame(SpatialMovement.class, context.getPlanitEntityClass());
    var names = context.getAttributeDescription().stream().map(Triple::first).collect(Collectors.toList());
    assertEquals(names.size(), new HashSet<>(names).size());
    assertTrue(names.stream().allMatch(name -> name.length() <= 10));
    Arrays.stream(IntersectionType.values()).forEach(type -> assertTrue(names.contains(type.value())));

    var geometryAttribute = context.getAttributeDescription().get(names.size() - 1);
    assertEquals(PlanitEntityFeatureTypeContext.DEFAULT_GEOMETRY_ATTRIBUTE_KEY, geometryAttribute.first());
    assertEquals("MultiLineString", geometryAttribute.second());
  }

  /** A movement lists its intersection, segments, length and direct ban, and copies control and kinds */
  @Test
  public void movementAttributes() {
    var m1 = row("m1");
    assertEquals("i3", attribute("isect_id", m1));
    assertEquals("m1", attribute("mvmt_id", m1));
    assertEquals("8", attribute("from_seg", m1));
    assertEquals("15", attribute("to_seg", m1));
    assertEquals(0.1, attribute("length_km", m1));
    assertEquals("signalised", attribute("control", m1));
    assertEquals(true, attribute("junction", m1));
    assertEquals(false, attribute("crossing", m1));
    assertEquals(true, attribute("direct_ban", m1));
    assertEquals("", attribute("ban_modes", m1));

    var m3 = row("m3");
    assertEquals(true, attribute("direct_ban", m3));
    assertEquals("car", attribute("ban_modes", m3));
    assertEquals(false, attribute("direct_ban", row("m2")));
  }

  /**
   * A movement holds its own values, empty when not set, and the values it uses: its own when set, otherwise those of
   * its approach
   */
  @Test
  public void movementCapacityFactors() {
    var m1 = row("m1");
    assertEquals(0.15, attribute("green", m1));
    assertNull(attribute("sat_flow", m1));
    assertEquals(0.15, attribute("green_used", m1));
    assertEquals(0.9, attribute("sat_used", m1));
    assertEquals(0.135, (Double) attribute("eff_cap", m1), 1e-9);

    var m4 = row("m4");
    assertEquals(1.0, attribute("green", m4));
    assertEquals(0.8, attribute("sat_flow", m4));
    assertEquals(1.0, attribute("green_used", m4));
    assertEquals(0.8, attribute("sat_used", m4));

    var m2 = row("m2");
    assertNull(attribute("green", m2));
    assertNull(attribute("sat_flow", m2));
    assertEquals(1.0, attribute("green_used", m2));
    assertEquals(1.0, attribute("sat_used", m2));
    assertEquals(1.0, attribute("eff_cap", m2));
  }

  /** The geometry runs along the from segment, the internal segments passed and the to segment */
  @Test
  public void geometryRunsThroughInternalSegments() {
    var m1 = geometry(row("m1"));
    assertEquals(3, m1.getNumGeometries());
    var internal = (LineString) m1.getGeometryN(1);
    assertEquals(new Coordinate(200, 0), internal.getCoordinateN(0));
    assertEquals(new Coordinate(300, 0), internal.getCoordinateN(internal.getNumPoints() - 1));
    var to = (LineString) m1.getGeometryN(2);
    assertEquals(new Coordinate(300, 100), to.getCoordinateN(to.getNumPoints() - 1));

    assertEquals(2, geometry(row("m4")).getNumGeometries());
  }

  /** Without the geometry through the internal segments, the geometry holds only the from and to segment */
  @Test
  public void geometryOfFromAndToSegmentOnly() {
    context = createContext(false);
    var m1 = geometry(row("m1"));
    assertEquals(2, m1.getNumGeometries());
    var from = (LineString) m1.getGeometryN(0);
    assertEquals(new Coordinate(200, 0), from.getCoordinateN(from.getNumPoints() - 1));
    var to = (LineString) m1.getGeometryN(1);
    assertEquals(new Coordinate(300, 0), to.getCoordinateN(0));
  }
}
