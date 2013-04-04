package edu.isi.ikcap.wings.catalogs.data;

import edu.isi.ikcap.ontapi.KBTriple;
import edu.isi.ikcap.wings.catalogs.data.classes.ConstraintProperty;
import edu.isi.ikcap.wings.catalogs.data.classes.DataSourceLocationObject;
import edu.isi.ikcap.wings.catalogs.data.classes.DataVariableDataObjectBinding;
import edu.isi.ikcap.wings.retrieval.KBTripleList;
import edu.isi.ikcap.wings.retrieval.solver.RetrievalSolver.SubsumptionDegree;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Properties;

/**
 * The interface for communicating with the Data Characterization Catalog.
 * 
 * @author Joshua Moody
 * @author Varun Ratnaka
 * @author Karan Vahi
 * 
 * @version $Revision: 1.1 $
 */
public interface DataCatalog {

	/**
	 * Call to initialize the data characterization service load a file,
	 * establish a database connection, connect to triple store etc.
	 * 
	 * @param properties
	 *            the properties to initialize
	 * 
	 * @return boolean indicating success
	 */
	public boolean initializeDataCharacterization(Properties properties);

	/**
	 * Close the connection to the Data Characterization service
	 */
	public void close();

	/**
	 * Q1.1
	 * <p>
	 * This step will retrieve templates in the Workflow Template Catalog and
	 * find mappings between the data variables in the request and the data
	 * variables in the templates. Data types can be used in finding the
	 * mappings. That is, for an input/output data variable in a template, we
	 * can check whether its type is consistent with an input/output data
	 * variables in the request. When there is no mapping found for a template,
	 * the template will be removed from the pool of candidate workflows.
	 * <p>
	 * After a mapping is found, data metrics on the data variables in the
	 * request can be added to the corresponding data variables in the template
	 * (as conjunctions). That is, now there will be additional constraints on
	 * the data variables in the template.
	 * <p>
	 * There could be more than one set of mappings for a given template. For
	 * each of the alternatives returned, SR will create a new template for each
	 * alternative and add it to the pool of workflow templates that are being
	 * considered.
	 * <p>
	 * Two data descriptions can be mapped when their intersection is
	 * satisfiable.
	 * 
	 * @param requestDods
	 *            data object descriptions from a request
	 * @param templateDods
	 *            data object descriptions from a template
	 * @return possible mappings of request data variables to template data
	 *         variables
	 */
	public ArrayList<KBTriple> findDataVariableMappings(ArrayList<KBTriple> requestDods, ArrayList<KBTriple> templateDods);

	/**
	 * Q3.1
	 * <p>
	 * Returns a list of data variables mapped to data source ids from the dc
	 * namespace. The data object descriptions explain constraints on and
	 * between data variables from a particular specialized template.
	 * <p>
	 * 
	 * @param dods
	 *            data object descriptions from the dc namespaces mapped to data
	 *            variables in the sr namespace
	 * @param returnPartialBindings
	 *            if true, will return [dataVariableN null] if no mapping can be
	 *            found for a data variable, otherwise returns an empty array
	 *            list.
	 * @return data variables from the sr namespace mapped to data source ids
	 *         from the dc namespace
	 */
	public ArrayList<ArrayList<DataVariableDataObjectBinding>> findDataSources(ArrayList<KBTriple> dods, boolean returnPartialBindings);

	/**
	 * Q4.1 and Q8.2
	 * <p>
	 * If metricsOrCharacteristics ArrayList is null or empty, then for the
	 * given dataSourceId this function returns an XML string that represents
	 * all the data metrics (and charateristics) of a the data source.
	 * 
	 * @param dataObjectId
	 *            the (unique) id of the dataObject
	 * @return a string of xml
	 */
	public String findDataMetricsForDataObject(String dataObjectId);

	/**
	 * Q4.1 and Q8.2
	 * <p>
	 * Tree.If metricsOrCharacteristics ArrayList is null or empty, then for the
	 * given dataSourceId this function returns an XML string that represents
	 * all the data metrics (and charateristics) of a the data source.
	 * Optionally, only the metrics/characteristics mentioned in the ArrayList
	 * will be returned.
	 * 
	 * @param dataObjectId
	 *            the (unique) id for the dataObject
	 * @param metricsAndCharacteristics
	 *            an array of metrics/characteristics to return @return a string
	 *            of xml
	 * @return a string of xml
	 */
	public String findDataMetricsForDataObject(String dataObjectId, ArrayList<String> metricsAndCharacteristics);

	/**
	 * Q4.1b
	 */
	public ArrayList<KBTriple> getDataConstraints(String dataObjectId, String varId);

	/**
	 * Q4.3a
	 * <p>
	 * request to get a dataset ID from a unique key
	 * 
	 * @param key
	 *            a descriptive key of the dataset which uniquely identifies it
	 */
	public String createDataSetIDFromDescription(String key);

	
	/**
	 * Q4.3b
	 * <p>
	 * request to get a dataset ID from the data type and data metrics
	 * 
	 * @param key
	 *            a descriptive key of the dataset which uniquely identifies it
	 */
	public String createDataSetIDFromType(String id, String type, String metrics);
	
	
	/**
	 * Q5.1a
	 * <p>
	 * Given a data source id, returns a list of
	 * <code>DataSourceLocationObject</code> objects.
	 * 
	 * @param dataObjectId
	 *            the (unique) id of the dataObject
	 * 
	 * @return a list of <code>DataSourceLocationObject</code> objects
	 */
	public Collection<DataSourceLocationObject> findDataObjectLocationsAndAccessAttributes(String dataObjectId);

	/**
	 * Q5.1a
	 * <p>
	 * Given a data source id, returns a list of
	 * <code>DataSourceLocationObject</code> objects.
	 * 
	 * @param dataObjectIds
	 *            the collection of DataObject identifiers.
	 * 
	 * @return a Map indexed the DataObject Id's. Each value is a Collection of
	 *         <code>DataSourceLocationObject</code> objects. In case of an
	 *         entry not being found, then an empty collection is associated.
	 */
	public java.util.Map<String, Collection<DataSourceLocationObject>> findDataObjectLocationsAndAccessAttributes(Collection<String> dataObjectIds);

	/**
	 * Q8.1
	 * <p>
	 * request to register a dataSourceId and set its dsla.
	 * 
	 * @param dsla
	 *            the DataSourceLocation object to be registered.
	 * 
	 * @return true iff successful
	 */
	public boolean registerDataObject(DataSourceLocationObject dsla);

	public void setRequestId(String id);

	/**
	 * Template Creation API Queries below
	 */

	public ArrayList<ConstraintProperty> getAllConstraints();

	public ArrayList<String> getAllConceptIds();

	/**
	 * <p>
	 * Check if a property from a dataset is a classification property
	 * <p>
	 * Classification properties are used to compare two datasets in the
	 * workflow retrieval
	 * 
	 * @param propertyID
	 * @return
	 */
	public boolean isClassificationProperty(KBTriple propertyID);

	/**
	 * <p>
	 * Returns the subsumption value between two classes
	 * <p>
	 * The returned value could be
	 * <ul>
	 * <li>EQUIVALENT: if they belong to the same or equivalent classes</li>
	 * <li>SUBSUMES: if the first class subsumes the second</li>
	 * <li>SUBSUMED: if the first class is subsumed by the second</li>
	 * <li>DISJOINT: otherwise</li>
	 * </ul>
	 * 
	 * @param id
	 * @param id2
	 * @return
	 */
	public SubsumptionDegree checkSubsumtion(String relation, String individualID1, String individualID2);

	/**
	 * <p>
	 * Check if first class subsumes the second class
	 * 
	 * @param cls1
	 * @param cls2
	 * @return
	 */
	public boolean subsumes(String cls1, String cls2);
	
	/**
	 * <p>
	 * Given a constraint of type "entity wfq:canBeBound dataset", this method
	 * expands it to return a list of triples that relates the entity with each
	 * of the classification properties and values of the dataset
	 * 
	 * @param constraint
	 * @return
	 */
	public KBTripleList getDatasetClassificationProperties(KBTriple constraint);

}
