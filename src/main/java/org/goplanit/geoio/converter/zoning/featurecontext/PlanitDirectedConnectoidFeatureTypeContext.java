package org.goplanit.geoio.converter.zoning.featurecontext;

import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.converter.idmapping.ZoningIdMapper;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.zoning.DirectedConnectoid;
import org.goplanit.utils.zoning.UndirectedConnectoid;

/**
 * Track contextual relevant information for PLANit Directed connectoids that are persisted
 *
 * @author markr
 */
public class PlanitDirectedConnectoidFeatureTypeContext extends PlanitConnectoidFeatureTypeContext<DirectedConnectoid> {

  /**
   * Add any additional features unique to directed connectoids (and not available in base description) to feature description
   */
  protected void appendDirectedConnectoidFeatureDescription(final NetworkIdMapper networkIdMapper){
    this.appendToFeatureTypeDescription(
        Triple.of("phys_segm", "String",
          c -> networkIdMapper.getLinkSegmentIdMapper().apply((MacroscopicLinkSegment) c.getAccessLinkSegment())),
        Triple.of("segm2node", "String",
          c -> c.isNodeAccessDownstream() ? "PHYS_NODE_DOWNSTREAM" : "PHYS_NODE_UPSTREAM"));
  }

  /**
   * Constructor
   *
   * @param zoningIdMapper id mapper to apply
   * @param networkIdMapper id mapper of parent physical network to apply
   */
  protected PlanitDirectedConnectoidFeatureTypeContext(final ZoningIdMapper zoningIdMapper, final NetworkIdMapper networkIdMapper){
    super(DirectedConnectoid.class, zoningIdMapper, networkIdMapper);

    /* add od zone specific attributes */
    appendDirectedConnectoidFeatureDescription(networkIdMapper);

    /* finish with geometry */
    appendToFeatureTypeDescription(createGeometryFeatureDescription());
  }

  /**
   * Factory method
   *
   * @param zoningIdMapper id mapper to apply
   * @param networkIdMapper id mapper of parent physical network to apply
   * @return created instance
   */
  public static PlanitDirectedConnectoidFeatureTypeContext create(
      final ZoningIdMapper zoningIdMapper, final NetworkIdMapper networkIdMapper){
    return new PlanitDirectedConnectoidFeatureTypeContext(zoningIdMapper, networkIdMapper);
  }

}
