package org.goplanit.geoio.test.converter.network.featurecontext;

import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.geoio.converter.network.featurecontext.PlanitIntersectionNodesFeatureTypeContext;
import org.goplanit.geoio.test.util.NetworkTestUtils;
import org.goplanit.geoio.util.GeoIoFeatureTypeBuilder;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.MultiPoint;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the attributes {@link PlanitIntersectionNodesFeatureTypeContext} gives each intersection, on the small network with
 * three intersections
 *
 * @author markr
 */
public class PlanitIntersectionNodesFeatureTypeContextTest {

  private static MacroscopicNetworkLayer layer;

  private static PlanitIntersectionNodesFeatureTypeContext context;

  private static Intersection intersection(String xmlId) {
    return layer.getIntersections().firstMatch(i -> xmlId.equals(i.getXmlId()));
  }

  /**
   * Apply the attribute function with the given name
   *
   * @param name of the attribute
   * @param intersection to apply it to
   * @return attribute value
   */
  private static Object attribute(String name, Intersection intersection) {
    Function<Intersection, ?> function = context.getAttributeDescription().stream()
        .filter(a -> a.first().equals(name)).findFirst().orElseThrow().third();
    return function.apply(intersection);
  }

  @BeforeAll
  public static void setUp() {
    layer = NetworkTestUtils.readThreeIntersectionsNetworkLayer();
    context = GeoIoFeatureTypeBuilder.createIntersectionNodesFeatureContext(new NetworkIdMapper(IdMapperType.XML), null);
  }

  @AfterAll
  public static void tearDown() {
    IdGenerator.reset();
  }

  /** Attribute names are unique and fit a shapefile, one Boolean column per intersection type, geometry last */
  @Test
  public void attributeNamesAreUniqueAndShapefileSafe() {
    assertSame(Intersection.class, context.getPlanitEntityClass());
    var names = context.getAttributeDescription().stream().map(Triple::first).collect(Collectors.toList());
    assertEquals(names.size(), new HashSet<>(names).size());
    assertTrue(names.stream().allMatch(name -> name.length() <= 10));

    Arrays.stream(IntersectionType.values()).forEach(type -> {
      assertTrue(type.value().length() <= 10);
      assertTrue(names.contains(type.value()));
    });

    var geometryAttribute = context.getAttributeDescription().get(names.size() - 1);
    assertEquals(PlanitEntityFeatureTypeContext.DEFAULT_GEOMETRY_ATTRIBUTE_KEY, geometryAttribute.first());
    assertEquals("MultiPoint", geometryAttribute.second());
  }

  /** The intersection with two member nodes lists its nodes, approaches and internal segments */
  @Test
  public void twoNodeIntersectionAttributes() {
    var i3 = intersection("i3");
    assertEquals("i3", attribute("mapped_id", i3));
    assertEquals(i3.getId(), attribute("id", i3));
    assertEquals("i3", attribute("xml_id", i3));
    assertEquals("ext-3", attribute("ext_id", i3));
    assertEquals("signalised", attribute("control", i3));
    assertEquals(true, attribute("junction", i3));
    assertEquals(false, attribute("crossing", i3));
    assertEquals("5,6", attribute("node_ids", i3));
    /* mapped ids are sorted as text, as in PLANit XML */
    assertEquals("12,14,8", attribute("approaches", i3));
    assertEquals("10,11", attribute("internals", i3));
    assertEquals(2, attribute("node_count", i3));
    assertEquals(3, attribute("appr_count", i3));

    var geometry = (MultiPoint) attribute(PlanitEntityFeatureTypeContext.DEFAULT_GEOMETRY_ATTRIBUTE_KEY, i3);
    assertEquals(2, geometry.getNumGeometries());
    assertEquals(new Coordinate(200, 0), geometry.getGeometryN(0).getCoordinate());
    assertEquals(new Coordinate(300, 0), geometry.getGeometryN(1).getCoordinate());
  }

  /** A single node intersection without external id or internal segments gives empty strings for those */
  @Test
  public void singleNodeIntersectionAttributes() {
    var i2 = intersection("i2");
    assertEquals("", attribute("ext_id", i2));
    assertEquals("unsignalised", attribute("control", i2));
    assertEquals(false, attribute("junction", i2));
    assertEquals(true, attribute("crossing", i2));
    assertEquals("4", attribute("node_ids", i2));
    assertEquals("6,9", attribute("approaches", i2));
    assertEquals("", attribute("internals", i2));
    assertEquals(1, attribute("node_count", i2));
    assertEquals(2, attribute("appr_count", i2));
    var geometry = (MultiPoint) attribute(PlanitEntityFeatureTypeContext.DEFAULT_GEOMETRY_ATTRIBUTE_KEY, i2);
    assertEquals(1, geometry.getNumGeometries());

    var i1 = intersection("i1");
    assertEquals("ext-1", attribute("ext_id", i1));
    assertEquals(true, attribute("junction", i1));
    assertEquals(true, attribute("crossing", i1));
    assertEquals("0,2,4,7", attribute("approaches", i1));
  }
}
