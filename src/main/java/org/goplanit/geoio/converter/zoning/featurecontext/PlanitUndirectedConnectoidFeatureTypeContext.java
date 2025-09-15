package org.goplanit.geoio.converter.zoning.featurecontext;

import org.geotools.api.referencing.operation.MathTransform;
import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.converter.idmapping.ZoningIdMapper;
import org.goplanit.utils.zoning.UndirectedConnectoid;

/**
 * Track contextual relevant information for PLANit Undirected connectoids that are persisted
 *
 * @author markr
 */
public class PlanitUndirectedConnectoidFeatureTypeContext extends PlanitConnectoidFeatureTypeContext<UndirectedConnectoid> {

  /**
   * Add any additional features unique to undirected connectoids (and not available in base description) to feature description
   */
  protected void appendUndirectedConnectoidFeatureDescription(){
    // none yet
  }

  /**
   * Constructor
   *
   * @param zoningIdMapper id mapper to apply
   * @param networkIdMapper id mapper of parent physical network to apply
   * @param destinationCrsTransformer to use (may be null)
   */
  protected PlanitUndirectedConnectoidFeatureTypeContext(
      final ZoningIdMapper zoningIdMapper, final NetworkIdMapper networkIdMapper, final MathTransform destinationCrsTransformer){
    super(UndirectedConnectoid.class, zoningIdMapper, networkIdMapper);

    /* add od zone specific attributes */
    appendUndirectedConnectoidFeatureDescription();

    /* finish with geometry */
    appendToFeatureTypeDescription(createGeometryFeatureDescription(destinationCrsTransformer));
  }

  /**
   * Factory method
   *
   * @param zoningIdMapper id mapper to apply
   * @param networkIdMapper id mapper of parent physical network to apply
   * @param destinationCrsTransformer to use (may be null)
   * @return created instance
   */
  public static PlanitUndirectedConnectoidFeatureTypeContext create(
      final ZoningIdMapper zoningIdMapper,
      final NetworkIdMapper networkIdMapper,
      final MathTransform destinationCrsTransformer){
    return new PlanitUndirectedConnectoidFeatureTypeContext(zoningIdMapper, networkIdMapper, destinationCrsTransformer);
  }

}
