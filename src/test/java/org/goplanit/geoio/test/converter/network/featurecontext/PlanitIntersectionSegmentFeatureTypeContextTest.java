package org.goplanit.geoio.test.converter.network.featurecontext;

import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.geoio.converter.network.featurecontext.PlanitIntersectionSegmentFeatureTypeContext;
import org.goplanit.geoio.test.util.NetworkTestUtils;
import org.goplanit.geoio.util.GeoIoFeatureTypeBuilder;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the attributes {@link PlanitIntersectionSegmentFeatureTypeContext} gives each approach and internal segment of
 * an intersection, on the small network with three intersections. Its only banned movement runs from segment 0, an
 * approach of i1, to segment 5.
 *
 * @author markr
 */
public class PlanitIntersectionSegmentFeatureTypeContextTest {

  private MacroscopicNetworkLayer layer;

  private PlanitIntersectionSegmentFeatureTypeContext context;

  @BeforeEach
  public void setUp() {
    layer = NetworkTestUtils.readThreeIntersectionsNetworkLayer();
    context = GeoIoFeatureTypeBuilder.createIntersectionSegmentFeatureContext(
        new NetworkIdMapper(IdMapperType.XML),
        layer.getIntersections(),
        IntersectionUtils.findBannedMovementsByApproachSegment(layer.getIntersections(), layer.getBannedMovements()),
        null);
  }

  @AfterAll
  public static void tearDown() {
    IdGenerator.reset();
  }

  /**
   * Collect the row of the given approach or internal segment, which is the link segment itself
   *
   * @param segmentXmlId of the link segment
   * @return row
   */
  private MacroscopicLinkSegment row(String segmentXmlId) {
    return layer.getLinkSegments().getByXmlId(segmentXmlId);
  }

  /**
   * Apply the attribute function with the given name
   *
   * @param name of the attribute
   * @param row to apply it to
   * @return attribute value
   */
  private Object attribute(String name, MacroscopicLinkSegment row) {
    Function<MacroscopicLinkSegment, ?> function = context.getAttributeDescription().stream()
        .filter(a -> a.first().equals(name)).findFirst().orElseThrow().third();
    return function.apply(row);
  }

  private LineString geometry(MacroscopicLinkSegment row) {
    return (LineString) attribute(PlanitEntityFeatureTypeContext.DEFAULT_GEOMETRY_ATTRIBUTE_KEY, row);
  }

  /** Attribute names are unique and fit a shapefile, one Boolean column per intersection type, geometry last */
  @Test
  public void attributeNamesAreUniqueAndShapefileSafe() {
    assertSame(MacroscopicLinkSegment.class, context.getPlanitEntityClass());
    var names = context.getAttributeDescription().stream().map(Triple::first).collect(Collectors.toList());
    assertEquals(names.size(), new HashSet<>(names).size());
    assertTrue(names.stream().allMatch(name -> name.length() <= 10));
    Arrays.stream(IntersectionType.values()).forEach(type -> assertTrue(names.contains(type.value())));

    var geometryAttribute = context.getAttributeDescription().get(names.size() - 1);
    assertEquals(PlanitEntityFeatureTypeContext.DEFAULT_GEOMETRY_ATTRIBUTE_KEY, geometryAttribute.first());
    assertEquals("LineString", geometryAttribute.second());
  }

  /** An approach lists the segments its banned movements go to, and copies control and kinds of its intersection */
  @Test
  public void approachAttributes() {
    var approach0 = row("0");
    assertEquals("i1", attribute("isect_id", approach0));
    assertEquals(true, attribute("approach", approach0));
    assertEquals("0", attribute("seg_id", approach0));
    assertEquals("1", attribute("node_up", approach0));
    assertEquals("0", attribute("node_down", approach0));
    assertEquals("signalised", attribute("control", approach0));
    assertEquals(true, attribute("junction", approach0));
    assertEquals(true, attribute("crossing", approach0));
    assertEquals("5", attribute("banned_to", approach0));

    var approach8 = row("8");
    assertEquals("i3", attribute("isect_id", approach8));
    assertEquals("", attribute("banned_to", approach8));
    assertEquals("5", attribute("node_down", approach8));
    assertEquals(false, attribute("crossing", approach8));
    assertEquals(new Coordinate(200, 0), geometry(approach8).getCoordinateN(geometry(approach8).getNumPoints() - 1));
  }

  /** Internal segments are not approaches and have no banned movements */
  @Test
  public void internalAttributes() {
    var internal10 = row("10");
    var internal11 = row("11");
    assertEquals(false, attribute("approach", internal10));
    assertEquals(false, attribute("approach", internal11));
    assertEquals("", attribute("banned_to", internal10));
    assertEquals("", attribute("banned_to", internal11));
    assertEquals("i3", attribute("isect_id", internal10));
    assertEquals("6", attribute("node_down", internal10));
    assertEquals("5", attribute("node_down", internal11));
  }

  /** The geometry of an approach on the b_a segment of a link with geometry from a to b ends at the member node */
  @Test
  public void approachGeometryEndsAtMemberNode() {
    layer.getLinks().getByXmlId("3").setGeometry(PlanitJtsUtils.createLineString(
        new Coordinate(0, 0), new Coordinate(50, 10), new Coordinate(100, 0)));
    var approach7 = row("7");
    var geometry = geometry(approach7);
    assertEquals(new Coordinate(100, 0), geometry.getCoordinateN(0));
    assertEquals(new Coordinate(0, 0), geometry.getCoordinateN(geometry.getNumPoints() - 1));
  }
}
