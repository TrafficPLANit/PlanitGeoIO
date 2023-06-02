package org.goplanit.geoio.converter;

import org.geotools.data.DataStore;
import org.geotools.data.Transaction;
import org.goplanit.converter.CrsWriterImpl;
import org.goplanit.converter.idmapping.IdMapperType;
import org.goplanit.geoio.converter.network.GeometryNetworkWriterSettings;
import org.goplanit.geoio.util.GeoIODataStoreManager;
import org.goplanit.geoio.util.GeoIoFeatureTypeBuilder;
import org.goplanit.geoio.util.GeoIoWriterSettings;
import org.goplanit.geoio.util.PlanitEntityFeatureTypeContext;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.id.ManagedIdEntities;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.misc.Pair;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeatureType;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Writer to persist a PLANit network to disk in a geometry centric format such as Shape files. Id mapping default is
 * set to internal ids (not XML ids) by default
 * 
 * @author markr
 *
 */
public abstract class GeometryIoWriter<T> extends CrsWriterImpl<T> {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(GeometryIoWriter.class.getCanonicalName());

  /** writer settings to use */
  private final GeoIoWriterSettings settings;

  /**
   * Given the feature contexts for the available GIS features, find the one where the context matches a given PLANit entity class
   *
   * @param planitEntityClass to find entry for
   * @param geoFeatureTypesByPlanitEntity available entries to search in
   * @return found entry or throw run time exception
   */
  protected Pair<SimpleFeatureType, PlanitEntityFeatureTypeContext<? extends ManagedId>> findFeaturePairForPlanitEntity(
          Class<? extends ManagedId> planitEntityClass, List<Pair<SimpleFeatureType, PlanitEntityFeatureTypeContext<? extends ManagedId>>> geoFeatureTypesByPlanitEntity) {
    return geoFeatureTypesByPlanitEntity.stream().filter(
            p -> p.second().getPlanitEntityClass().equals(planitEntityClass)).findFirst().orElseThrow(() ->
            new PlanItRunTimeException("No feature information found for %s, available: [%s]", planitEntityClass.getName(),
                    geoFeatureTypesByPlanitEntity.stream().map(p -> p.second().getPlanitEntityClass().getName()).collect(Collectors.joining(","))));
  }

  /** find feature and context based on the class present in context
   *
   * @param clazz to find feature for
   * @return found entry, null if not present
   */
  protected <T extends ExternalIdAble> Pair<SimpleFeatureType, PlanitEntityFeatureTypeContext<T>> findFeature(
          Class<T> clazz, Map<SimpleFeatureType, PlanitEntityFeatureTypeContext<? extends ExternalIdAble>> geoFeatureTypes) {
    var result =
            geoFeatureTypes.entrySet().stream().filter(e -> e.getValue().getPlanitEntityClass().equals(clazz)).findFirst();
    return result.isPresent() ? Pair.of(result.get().getKey(), (PlanitEntityFeatureTypeContext<T>) result.get().getValue()) : Pair.empty();
  }

  /**
   * Writer the geometry layer with the PLANit entities available in the container
   *
   * @param <T> type of PLANit entity to write
   * @param featureType to use
   * @param planitEntityFeatureContext the context to convert instances to features
   * @param layerLogPrefix to use
   * @param entityDataStore to use for persistence
   * @param featureSchemaName the feature lives under on the datastore
   * @param planitEntities container to persist
   * @param entityToGeometry conversion from entity to geometry it contains
   */
  protected <T extends ManagedId> void writeGeometryLayerForEntity(SimpleFeatureType featureType,
                                                                   PlanitEntityFeatureTypeContext<T> planitEntityFeatureContext,
                                                                   String layerLogPrefix,
                                                                   DataStore entityDataStore,
                                                                   String featureSchemaName,
                                                                   ManagedIdEntities<T> planitEntities,
                                                                   Function<T, Geometry> entityToGeometry) {

    /* place feature on data store */
    GeoIODataStoreManager.registerFeatureOnDataStore(entityDataStore, featureType);

    try ( var featureWriter =
              entityDataStore.getFeatureWriter(featureSchemaName, Transaction.AUTO_COMMIT)) {
      for(var planitEntity : planitEntities){
        var entityFeature = featureWriter.next();
        var attributeConversions = planitEntityFeatureContext.getAttributeDescription();
        for(var attributeConversion : attributeConversions) {

          if(attributeConversion.first().equals(planitEntityFeatureContext.getDefaultGeometryAttributeKey())) {
            /* geometry attribute */
            entityFeature.setAttribute(GeoIoFeatureTypeBuilder.GEOTOOLS_GEOMETRY_ATTRIBUTE, entityToGeometry.apply(planitEntity));
          }else{
            /* regular attribute */
            entityFeature.setAttribute(attributeConversion.first(), attributeConversion.third().apply(planitEntity));
          }
        }
        featureWriter.write();
      }
    }catch (Exception e){
      LOGGER.severe((e.getMessage()));
      throw new PlanItRunTimeException("%s Unable to persist PLANit entities for %s",
          layerLogPrefix, planitEntities.getManagedIdClass().getName(), e.getCause());
    }
  }

  /** Constructor
   *
   */
  protected GeometryIoWriter() {
    this(".", CountryNames.GLOBAL);
  }

  /** Constructor
   *
   * @param networkPath to persist network on
   */
  protected GeometryIoWriter(String networkPath) {
    this(networkPath, CountryNames.GLOBAL);
  }

  /** Constructor
   *
   * @param networkPath to persist network on
   * @param countryName to optimise projection for (if available, otherwise ignore)
   */
  protected GeometryIoWriter(String networkPath, String countryName) {
    this(new GeometryNetworkWriterSettings(networkPath, countryName));
  }

  /** Constructor
   *
   * @param settings to use
   */
  protected GeometryIoWriter(GeoIoWriterSettings settings){
    super(IdMapperType.ID);
    this.settings = settings;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    GeoIODataStoreManager.reset();
  }
  
  // GETTERS/SETTERS
  
  /**
   * {@inheritDoc}
   */
  @Override
  public GeoIoWriterSettings getSettings(){
    return settings;
  }

}
