package org.goplanit.geoio.test.converter.network.featurecontext;

import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.geoio.converter.network.featurecontext.PlanitMacroscopicLinkSegmentFeatureTypeContext;
import org.goplanit.geoio.test.util.NetworkTestUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the link segment geometry of {@link PlanitMacroscopicLinkSegmentFeatureTypeContext}, on link 3 of the small
 * network with three intersections, which runs from node 0 at (0,0) to node 4 at (100,0) and has segment 6 (a_b) and
 * segment 7 (b_a)
 *
 * @author markr
 */
public class PlanitMacroscopicLinkSegmentFeatureTypeContextTest {

  @AfterAll
  public static void tearDown() {
    IdGenerator.reset();
  }

  /** On a link with geometry from a to b, the b_a segment is reversed to run in the direction of travel, the a_b
   * segment and the link geometry are left as they are */
  @Test
  public void geometryInTravelDirectionReversesOppositeSegment() {
    var layer = NetworkTestUtils.readThreeIntersectionsNetworkLayer();
    var link = layer.getLinks().getByXmlId("3");
    link.setGeometry(PlanitJtsUtils.createLineString(
        new Coordinate(0, 0), new Coordinate(50, 10), new Coordinate(100, 0)));

    var segmentBa = layer.getLinkSegments().getByXmlId("7");
    var geometryBa =
        PlanitMacroscopicLinkSegmentFeatureTypeContext.createOrGetLinkSegmentGeometryInTravelDirection(segmentBa);
    assertEquals(3, geometryBa.getNumPoints());
    assertEquals(new Coordinate(100, 0), geometryBa.getCoordinateN(0));
    assertEquals(new Coordinate(50, 10), geometryBa.getCoordinateN(1));
    assertEquals(new Coordinate(0, 0), geometryBa.getCoordinateN(2));

    var segmentAb = layer.getLinkSegments().getByXmlId("6");
    var geometryAb =
        PlanitMacroscopicLinkSegmentFeatureTypeContext.createOrGetLinkSegmentGeometryInTravelDirection(segmentAb);
    assertEquals(new Coordinate(0, 0), geometryAb.getCoordinateN(0));
    assertEquals(new Coordinate(100, 0), geometryAb.getCoordinateN(2));

    assertEquals(new Coordinate(0, 0), link.getGeometry().getCoordinateN(0));
  }

  /** On a link without geometry, the line built from the node positions already runs in the direction of travel and
   * is not reversed */
  @Test
  public void geometryInTravelDirectionWithoutLinkGeometryIsNotReversed() {
    var layer = NetworkTestUtils.readThreeIntersectionsNetworkLayer();
    layer.getLinks().getByXmlId("3").setGeometry(null);

    var segmentBa = layer.getLinkSegments().getByXmlId("7");
    var geometryBa =
        PlanitMacroscopicLinkSegmentFeatureTypeContext.createOrGetLinkSegmentGeometryInTravelDirection(segmentBa);
    assertEquals(new Coordinate(100, 0), geometryBa.getCoordinateN(0));
    assertEquals(new Coordinate(0, 0), geometryBa.getCoordinateN(geometryBa.getNumPoints() - 1));
  }

  /**
   * Apply the geom_opp attribute function of the link segment context to a link segment
   *
   * @param layer of the link segment
   * @param segmentXmlId of the link segment
   * @return attribute value
   */
  private static Object geometryOppositeAttribute(MacroscopicNetworkLayer layer, String segmentXmlId) {
    var context = PlanitMacroscopicLinkSegmentFeatureTypeContext.create(
        new NetworkIdMapper(IdMapperType.XML), layer.getSupportedModes(), null);
    Function<MacroscopicLinkSegment, ?> function = context.getAttributeDescription().stream()
        .filter(a -> a.first().equals("geom_opp")).findFirst().orElseThrow().third();
    return function.apply(layer.getLinkSegments().getByXmlId(segmentXmlId));
  }

  /** geom_opp is true only for a segment running opposite to the geometry of its link */
  @Test
  public void geometryOppositeOnLinkWithGeometry() {
    var layer = NetworkTestUtils.readThreeIntersectionsNetworkLayer();
    layer.getLinks().getByXmlId("3").setGeometry(PlanitJtsUtils.createLineString(
        new Coordinate(0, 0), new Coordinate(50, 10), new Coordinate(100, 0)));
    assertEquals(false, geometryOppositeAttribute(layer, "6"));
    assertEquals(true, geometryOppositeAttribute(layer, "7"));
  }

  /** On a link without geometry the line is built in the direction of travel, so geom_opp is false for both
   * segments */
  @Test
  public void geometryOppositeOnLinkWithoutGeometryIsFalse() {
    var layer = NetworkTestUtils.readThreeIntersectionsNetworkLayer();
    layer.getLinks().getByXmlId("3").setGeometry(null);
    assertEquals(false, geometryOppositeAttribute(layer, "6"));
    assertEquals(false, geometryOppositeAttribute(layer, "7"));
  }
}
