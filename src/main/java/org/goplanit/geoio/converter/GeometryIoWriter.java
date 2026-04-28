package org.goplanit.geoio.converter;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DefaultTransaction;
import org.goplanit.converter.CrsWriterImpl;
import org.goplanit.geoio.util.*;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.geoio.converter.network.GeometryNetworkWriterSettings;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.misc.Pair;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Writer to persist a PLANit network to disk in a geometry centric format such as Shape files. Id mapping default is
 * set to XML ids by default
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
   * @param <F> underlying feature type
   * @param planitEntityClass to find entry for
   * @param geoFeatureTypesByPlanitEntity available entries to search in
   * @return found entry or throw run time exception
   */
  @SuppressWarnings("unchecked")
  protected <F extends ManagedId> Pair<SimpleFeatureType, PlanitEntityFeatureTypeContext<F>>
  findFeaturePairForPlanitEntity(
          Class<F> planitEntityClass,
          List<Pair<SimpleFeatureType, PlanitEntityFeatureTypeContext<? extends ManagedId>>> geoFeatureTypesByPlanitEntity) {

    // orElsethrow is playing hardball. It does not like cast to final type,
    // instead do partially, put in variable, and then cast afterwards
    var result =  (Pair<SimpleFeatureType, ? extends PlanitEntityFeatureTypeContext<? extends ManagedId>>)
            geoFeatureTypesByPlanitEntity.stream().filter(
                    p -> p.second().getPlanitEntityClass().equals(planitEntityClass)).findFirst().orElseThrow(() ->
                    new PlanItRunTimeException(
                            "No feature information found for %s, available: [%s]", planitEntityClass.getName(),
                            geoFeatureTypesByPlanitEntity.stream().map(
                                    p -> p.second().getPlanitEntityClass().getName()).collect(
                                            Collectors.joining(","))));
    return (Pair<SimpleFeatureType, PlanitEntityFeatureTypeContext<F>>) result;
  }

  /** find feature and context based on the class present in context
   *
   * @param <TT> the type of geometry
   * @param clazz to find feature for
   * @param geoFeatureTypes to find from
   * @return found entry, null if not present
   */
  protected <TT extends ExternalIdAble> Pair<SimpleFeatureType, PlanitEntityFeatureTypeContext<TT>> findFeature(
      Class<TT> clazz,
      Map<SimpleFeatureType, PlanitEntityFeatureTypeContext<? extends ExternalIdAble>> geoFeatureTypes) {
    var result = geoFeatureTypes.entrySet().stream().filter(
        e -> e.getValue().getPlanitEntityClass().equals(clazz)).findFirst();
    return result.isPresent() ?
        Pair.of(result.get().getKey(), (PlanitEntityFeatureTypeContext<TT>) result.get().getValue()) : Pair.empty();
  }

  /**
   * Writer the geometry layer with the PLANit entities available in the container
   *
   * @param <TT> type of PLANit entity to write
   * @param featureType to use
   * @param planitEntityFeatureContext the context to convert instances to features
   * @param loggingPrefix to use
   * @param entityDataStore to use for persistence
   * @param featureSchemaName the feature lives under on the datastore
   * @param planitEntities container to persist
   */
  protected <TT extends ManagedId> void writeGeometryLayerForEntity(
          SimpleFeatureType featureType,
          PlanitEntityFeatureTypeContext<TT> planitEntityFeatureContext,
          String loggingPrefix,
          DataStore entityDataStore,
          String featureSchemaName,
          Iterable<? extends TT> planitEntities) {

    /* place feature on data store */
    GeoIODataStoreManager.registerFeatureOnDataStore(entityDataStore, featureType);
    var geometryDescriptor =
            DataStoreUtils.getDataStoreGeometryAttributeDescriptor(entityDataStore, featureSchemaName);

    Transaction transaction = new DefaultTransaction("create");
    try ( var featureWriter =
              entityDataStore.getFeatureWriterAppend(featureSchemaName, transaction)) {
      for(var planitEntity : planitEntities){
        var entityFeature = featureWriter.next();
        var attributeConversions = planitEntityFeatureContext.getAttributeDescription();
        for(var attributeConversion : attributeConversions) {

          if(attributeConversion.first().equals(planitEntityFeatureContext.getDefaultGeometryAttributeKey())) {
            /* geometry attribute */
            entityFeature.setAttribute(geometryDescriptor, attributeConversion.third().apply(planitEntity));
          }else{
            /* regular attribute */
            entityFeature.setAttribute(attributeConversion.first(), attributeConversion.third().apply(planitEntity));
          }
        }
        featureWriter.write();
      }
      transaction.commit();
    }catch (Exception e){
      LOGGER.severe(String.format(
              "Error occurred when persisting an attribute for a PLANit entity for schema %s",
              featureSchemaName));
      LOGGER.severe((e.getMessage()));
      try {
        transaction.rollback();
      } catch (IOException rollbackEx) {
        e.addSuppressed(rollbackEx);
      }
    }finally{
      try {
        transaction.close();
      } catch (IOException closeEx) {
        // Log or handle this, but don't let it mask the original error
        System.err.println("Warning: Failed to close transaction: " + closeEx.getMessage());
      }
    }
  }

  /** {@link #writeGeometryLayerForEntity(SimpleFeatureType, PlanitEntityFeatureTypeContext, String, DataStore, String, Iterable)}
   *
   * @param featureType to write
   * @param planitEntityFeatureContext contextual information on feature type
   * @param entityDataStore datastore to use
   * @param featureSchemaName to use
   * @param planitEntities to persist
   * @param <TT> type of planit entity
   */
  protected <TT extends ManagedId> void writeGeometryLayerForEntity(
          SimpleFeatureType featureType,
          PlanitEntityFeatureTypeContext<TT> planitEntityFeatureContext,
          DataStore entityDataStore,
          String featureSchemaName,
          Iterable<TT> planitEntities) {

    writeGeometryLayerForEntity(
            featureType, planitEntityFeatureContext,"", entityDataStore, featureSchemaName, planitEntities);
  }

  /**
   * Find data store to use, if not present, create it if possible
   *
   * @param featureContext to create data store for and register on GeoIODataStoreManager
   * @param fullOutputPath on where to store results
   * @return dataStore to use
   *
   * @param <TT> type of PLANit entity the data store is to be used for
   */
  protected <TT extends ManagedId> DataStore findDataStore(
      PlanitEntityFeatureTypeContext<TT> featureContext, Path fullOutputPath){
    /* data store, e.g., underlying file(s) */
    DataStore dataStore = GeoIODataStoreManager.getDataStore(featureContext.getPlanitEntityClass());
    if(dataStore == null) {
      dataStore = GeoIODataStoreManager.createSingleEntityTypeDataStore(
              featureContext.getPlanitEntityClass(),
              getSettings().getFormat(),
              fullOutputPath);
    }



    return dataStore;
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
    super(IdMapperType.XML);
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
