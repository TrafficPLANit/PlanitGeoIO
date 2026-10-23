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
import org.goplanit.utils.network.layer.macroscopic.intersection.CapacityFactorOrigin;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionSignalSegment;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionUtils;
import org.goplanit.utils.network.layer.physical.LinkSegment;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the attributes {@link PlanitIntersectionSegmentFeatureTypeContext} gives each approach and internal segment of
 * an intersection, on the small network with three intersections. Its only banned turn runs from segment 0, an
 * approach of i1, to segment 5.
 *
 * @author markr
 */
public class PlanitIntersectionSegmentFeatureTypeContextTest {

  private MacroscopicNetworkLayer layer;

  private PlanitIntersectionSegmentFeatureTypeContext context;

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
    Function<LinkSegment, ?> function = context.getAttributeDescription().stream()
        .filter(a -> a.first().equals(name)).findFirst().orElseThrow().third();
    return function.apply(row);
  }

  /**
   * The geometry attribute of a row
   *
   * @param row to take it from
   * @return its line string
   */
  private LineString geometry(MacroscopicLinkSegment row) {
    return (LineString) attribute(PlanitEntityFeatureTypeContext.DEFAULT_GEOMETRY_ATTRIBUTE_KEY, row);
  }

  /**
   * Read the three intersections network and create the intersection segments context, with the banned turns by
   * approach segment
   */
  @BeforeEach
  public void setUp() {
    layer = NetworkTestUtils.readThreeIntersectionsNetworkLayer();
    context = GeoIoFeatureTypeBuilder.createIntersectionSegmentFeatureContext(
        new NetworkIdMapper(IdMapperType.XML),
        layer.getIntersections(),
        IntersectionUtils.findBannedTurnsByApproachSegment(layer.getIntersections(), layer.getBannedTurns()),
        null);
  }

  /**
   * Reset the id generator
   */
  @AfterAll
  public static void tearDown() {
    IdGenerator.reset();
  }

  /** Attribute names are unique and fit a shapefile, one Boolean column per intersection type, geometry last */
  @Test
  public void attributeNamesAreUniqueAndShapefileSafe() {
    assertSame(LinkSegment.class, context.getPlanitEntityClass());
    var names = context.getAttributeDescription().stream().map(Triple::first).collect(Collectors.toList());
    assertEquals(names.size(), new HashSet<>(names).size());
    assertTrue(names.stream().allMatch(name -> name.length() <= 10));
    Arrays.stream(IntersectionType.values()).forEach(type -> assertTrue(names.contains(type.value())));

    var geometryAttribute = context.getAttributeDescription().get(names.size() - 1);
    assertEquals(PlanitEntityFeatureTypeContext.DEFAULT_GEOMETRY_ATTRIBUTE_KEY, geometryAttribute.first());
    assertEquals("LineString", geometryAttribute.second());
  }

  /** An approach lists the segments its banned turns go to, and copies control and kinds of its intersection */
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

  /** Internal segments are not approaches and have no banned turns */
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

  /**
   * An approach holds the green ratio and saturation flow factor the model uses, 1 when not set, with how each got its
   * value; one without a signal of its own has no green ratio. Approach 8 has 0.44 and 0.9 set directly, approach 4
   * has no signal and 0.5, approach 0 has a signal and no values
   */
  @Test
  public void approachCapacityFactors() {
    var approach8 = row("8");
    assertEquals(true, attribute("signal", approach8));
    assertEquals(0.44, attribute("green", approach8));
    assertEquals("set_directly", attribute("green_src", approach8));
    assertEquals(0.9, attribute("sat_flow", approach8));
    assertEquals("set_directly", attribute("sat_src", approach8));
    assertEquals(0.396, (Double) attribute("eff_cap", approach8), 1e-9);

    var approach4 = row("4");
    assertEquals(false, attribute("signal", approach4));
    assertNull(attribute("green", approach4));
    assertEquals("", attribute("green_src", approach4));
    assertEquals(0.5, attribute("sat_flow", approach4));
    assertEquals(0.5, attribute("eff_cap", approach4));

    var approach0 = row("0");
    assertEquals(true, attribute("signal", approach0));
    assertEquals(1.0, attribute("green", approach0));
    assertEquals("", attribute("green_src", approach0));
    assertEquals(1.0, attribute("sat_flow", approach0));
    assertEquals("", attribute("sat_src", approach0));
    assertEquals(1.0, attribute("eff_cap", approach0));
  }

  /** A computed green ratio says so */
  @Test
  public void computedGreenRatioSaysSo() {
    var approach12 = row("12");
    var intersection = layer.getIntersections().getByMemberNode(approach12.getDownstreamNode());
    ((IntersectionSignalSegment) intersection.getApproach(approach12)).setGreenRatio(0.3, CapacityFactorOrigin.COMPUTED);
    assertEquals(0.3, attribute("green", approach12));
    assertEquals("computed", attribute("green_src", approach12));
  }

  /** An internal segment with a signal holds its values, a plain one only a saturation flow factor */
  @Test
  public void internalCapacityFactors() {
    var internal11 = row("11");
    assertEquals(true, attribute("signal", internal11));
    assertEquals(0.6, attribute("green", internal11));
    assertEquals(0.9, attribute("sat_flow", internal11));

    var internal10 = row("10");
    assertEquals(false, attribute("signal", internal10));
    assertNull(attribute("green", internal10));
    assertEquals(1.0, attribute("sat_flow", internal10));
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
