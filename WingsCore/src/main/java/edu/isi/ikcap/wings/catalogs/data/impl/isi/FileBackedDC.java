////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2007, Your Corporation. All Rights Reserved.
////////////////////////////////////////////////////////////////////////////////

package edu.isi.ikcap.wings.catalogs.data.impl.isi;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.isi.ikcap.ontapi.KBAPI;
import edu.isi.ikcap.ontapi.KBObject;
import edu.isi.ikcap.ontapi.KBTriple;
import edu.isi.ikcap.ontapi.OntFactory;
import edu.isi.ikcap.ontapi.OntSpec;
import edu.isi.ikcap.ontapi.SparqlFactory;
import edu.isi.ikcap.ontapi.SparqlQuery;
import edu.isi.ikcap.ontapi.SparqlQuerySolution;
import edu.isi.ikcap.wings.catalogs.data.DataCatalog;
import edu.isi.ikcap.wings.catalogs.data.classes.ConstraintProperty;
import edu.isi.ikcap.wings.catalogs.data.classes.DataSourceLocationObject;
import edu.isi.ikcap.wings.catalogs.data.classes.DataVariableDataObjectBinding;
import edu.isi.ikcap.wings.retrieval.KBTripleList;
import edu.isi.ikcap.wings.retrieval.solver.RetrievalSolver.SubsumptionDegree;
import edu.isi.ikcap.wings.retrieval.util.NamespaceManager;
import edu.isi.ikcap.wings.util.Storage;
import edu.isi.ikcap.wings.workflows.util.AWGLoggerHelper;
import edu.isi.ikcap.wings.workflows.util.MetricsHelper;
import edu.isi.ikcap.wings.workflows.util.PropertiesHelper;
import edu.isi.ikcap.wings.workflows.util.WflowGenFactory;
import edu.isi.ikcap.wings.workflows.util.WorkflowGenerationProvenanceCatalogConnector;


/**
 * Name: FileBackedDC Package: edu.isi.ikcap.wings.catalogs.data User: moody Date: Aug
 * 3, 2007 Time: 3:22:00 PM
 */
public class FileBackedDC implements DataCatalog {
	/**
	 * the info logger
	 */
	private Logger logger;

	/**
	 * The KBAPI api
	 */
	public KBAPI api;

	public KBAPI ontapi;
	public KBAPI ontapi_plain;

	/**
	 * the url of thd dc (eg dc, dcv1.0, dcAlt) built in the constructor:
	 * uriPrefix + dcDirectoryName + dcOntologyFile
	 */
	public String dcUrl;

	/**
	 * url of the domain (eg. weka, sr-6, sr-12) built in the constructor:
	 * uriPrefix + dcDirectoryName + domain + domainOntologyFile
	 */
	public String domainUrl;

	/**
	 * the url of the library built in the in constructor: uriPrefix +
	 * dcDirectoryName + domain + libraryFile
	 */
	public String libraryUrl;

	/**
	 * the dc namespace built in the constructor: dcUrl + "#"
	 */
	public String dcNamespace;

	/**
	 * the domain namespace built in the constructor: domainUrl + "#"
	 */
	public String domainNamespace;

	/**
	 * set in constructor e.g.: http://wings-workflows.org/ontology
	 */
	public String uriPrefix;

	/**
	 * used in ontFactory.setLocal(uriPrefix, dirBase) - e.g. file:new-ontology
	 */
	public String rootOntologyPath;

	/**
	 * a map from owlObjectProperties to KBObjects
	 */
	public HashMap<String, KBObject> propertyMap;

	/**
	 * a map from owlDatatypeProperties to KBObjects
	 */
	public HashMap<String, KBObject> datatypeMap;

	/**
	 * a map from owlConcepts to KBObjects
	 */
	public HashMap<String, KBObject> conceptMap;

	/**
	 * a map from owlConcepts to name Formats
	 */
	public HashMap<String, String> conceptNameFormat;
	
	/**
	 * a list of owl object properties in the dc ontology
	 */
	public ArrayList<KBObject> owlObjectProperties;

	/**
	 * a list of owl datatype properties in the dc ontology
	 */
	public ArrayList<KBObject> owlDatatypeProperties;

	/**
	 * a list of owl concepts (classes) in the dc ontology
	 */
	public ArrayList<KBObject> owlConcepts;

	/**
	 * given a local (shouldn't this be a locale?) - libraryUrl and an ontology
	 * specification, this can return a KBAPI instance
	 */
	protected OntFactory ontologyFactory;

	/**
	 * a factory for generating SparqlQuery objects
	 */
	public SparqlFactory sparqlFactory;

	public boolean noOntologyMode = false;

	String request_id;

	public FileBackedDC(String ldid) {
		// Check if the logfile & ontology directory exist..
		// If they both don't exist then assume this is a purposefully created
		// "dumb DC" (used by Grid worker nodes to register)

		this.logger = PropertiesHelper.getLogger(this.getClass().getName(), ldid);

		String dclibPrefix = PropertiesHelper.getDCNewDataPrefix();
		this.libraryUrl = PropertiesHelper.getDCPrefixNSMap().get(dclibPrefix);
		if (this.libraryUrl.endsWith("#"))
			this.libraryUrl = this.libraryUrl.substring(0, this.libraryUrl.lastIndexOf('#'));
		
		HashMap<String, String> nsmap = PropertiesHelper.getDCPrefixNSMap();
		this.dcNamespace = nsmap.get("dc");
		this.domainNamespace = nsmap.get("dcdom");

		this.sparqlFactory = new SparqlFactory(this.dcNamespace, this.domainNamespace, this.libraryUrl);

		this.domainUrl = PropertiesHelper.getDCDomainURL()+"/ontology.owl";
		this.dcUrl = PropertiesHelper.getDCURL()+"/ontology.owl";
		
		// TODO: Add arguments to select ontology spec {PLAIN | MICRO | DL | FULL}
		this.ontologyFactory = new OntFactory(OntFactory.JENA);
		WflowGenFactory.setLocalRedirects(this.ontologyFactory);
		this.api = ontologyFactory.getAPI(this.libraryUrl, OntSpec.PELLET);
		this.api.importFrom(ontologyFactory.getAPI(this.domainUrl, OntSpec.PLAIN));
		this.api.importFrom(ontologyFactory.getAPI(this.dcUrl, OntSpec.PLAIN));

		this.ontapi = ontologyFactory.getAPI(this.domainUrl, OntSpec.PELLET);
		this.ontapi_plain = ontologyFactory.getAPI(this.domainUrl, OntSpec.PLAIN);

		this.propertyMap = new HashMap<String, KBObject>();
		this.datatypeMap = new HashMap<String, KBObject>();
		this.conceptMap = new HashMap<String, KBObject>();
		this.conceptNameFormat = new HashMap<String, String>();

		this.owlObjectProperties = new ArrayList<KBObject>();
		this.owlDatatypeProperties = new ArrayList<KBObject>();
		this.owlConcepts = new ArrayList<KBObject>();

		this.owlObjectProperties = this.api.getAllObjectProperties();
		this.owlDatatypeProperties = this.api.getAllDatatypeProperties();
		this.owlConcepts = this.api.getAllClasses();
		this.request_id = ldid;

		this.initializeDataCharacterization(null);
	}

	/**
	 * Call to initialize the data characterization service load a file,
	 * establish a database connection, connect to triple store etc.
	 * 
	 * @param properties
	 *            the properties to initialize the instance with.
	 * 
	 * @return boolean indicating success
	 */

	public boolean initializeDataCharacterization(Properties properties) {
		for (KBObject prop : owlObjectProperties) {
			this.propertyMap.put(prop.getName(), prop);
		}
		for (KBObject con : owlConcepts) {
			this.conceptMap.put(con.getName(), con);
			for(String comment: this.api.getAllComments(con)) {
				if(comment.startsWith("NameFormat=")) {
					this.conceptNameFormat.put(con.getID(), comment.replaceFirst("NameFormat=", ""));
				}
			}
		}
		for (KBObject odp : owlDatatypeProperties) {
			this.datatypeMap.put(odp.getName(), odp);
		}
		return true;
	}

	/**
	 * Q1.1
	 * <p/>
	 * This step will retrieve templates in the Workflow Template Catalog and
	 * find mappings between the data variables in the request and the data
	 * variables in the templates. Data types can be used in finding the
	 * mappings. That is, for an input/output data variable in a template, we
	 * can check whether its type is consistent with an input/output data
	 * variables in the request. When there is no mapping found for a template,
	 * the template will be removed from the pool of candidate workflows.
	 * <p/>
	 * After a mapping is found, data metrics on the data variables in the
	 * request can be added to the corresponding data variables in the template
	 * (as conjunctions). That is, now there will be additional constraints on
	 * the data variables in the template.
	 * <p/>
	 * There could be more than one set of mappings for a given template. For
	 * each of the alternatives returned, SR will create a new template for each
	 * alternative and add it to the pool of workflow templates that are being
	 * considered.
	 * <p/>
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
	public ArrayList<KBTriple> findDataVariableMappings(ArrayList<KBTriple> requestDods, ArrayList<KBTriple> templateDods) {
		if (logger.isInfoEnabled()) {
			HashMap<String, Object> argumentMap = new HashMap<String, Object>(2);
			argumentMap.put("requestDods", requestDods);
			argumentMap.put("templateDods", templateDods);
			String arguments = AWGLoggerHelper.getArgumentString("q1.1", argumentMap);
			logger.debug(arguments);
		}

		if (logger.isInfoEnabled()) {
			String returnValue = AWGLoggerHelper.getReturnString("q1.1", new ArrayList<ArrayList>(0));
			logger.debug(returnValue);
		}

		return new ArrayList<KBTriple>(0);
	}

	/**
	 * Q3.1
	 * <p/>
	 * Returns a list of data variables mapped to data source ids from the dc
	 * namespace. The data object descriptions explain constraints on and
	 * between data variables from a particular specialized template.
	 * <p/>
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
	public ArrayList<ArrayList<DataVariableDataObjectBinding>> findDataSources(ArrayList<KBTriple> dods, boolean returnPartialBindings) {
		if (logger.isInfoEnabled()) {
			HashMap<String, Object> argumentMap = new HashMap<String, Object>(2);
			argumentMap.put("dods", dods);
			argumentMap.put("returnPartialBindings", returnPartialBindings);
			String arguments = AWGLoggerHelper.getArgumentString("<findDataSources> q3.1", argumentMap);
			logger.debug(arguments);
		}

		if (dods.size() == 0)
			return null;

		ArrayList<ArrayList<DataVariableDataObjectBinding>> result;

		if (returnPartialBindings) {
			result = this.findDataSourcesWithPartialBindings(dods);
		} else {
			KBAPI api = this.getApi();
			result = new ArrayList<ArrayList<DataVariableDataObjectBinding>>();

			SparqlQuery sq = this.getSparqlFactory().makeSparqlQueryFromDataObjectDescriptions(dods);
			HashMap<String, KBObject> variableMap = sq.getVariableMap();
			String query = sq.getQuery();

			ArrayList<ArrayList<SparqlQuerySolution>> queryResults = api.sparqlQuery(query);
			for (ArrayList<SparqlQuerySolution> queryResult : queryResults) {
				ArrayList<DataVariableDataObjectBinding> listOfBindings = new ArrayList<DataVariableDataObjectBinding>();
				for (SparqlQuerySolution sparqlQuerySolution : queryResult) {
					String variableName = sparqlQuerySolution.getVariable();
					KBObject kboVariable = variableMap.get(variableName);
					KBObject dataObject = sparqlQuerySolution.getObject();
					DataVariableDataObjectBinding dvdob = new DataVariableDataObjectBinding(kboVariable, dataObject);
					listOfBindings.add(dvdob);
				}
				result.add(listOfBindings);
			}
		}

		// ArrayList<ArrayList<ArrayList<KBObject>>> coerced =
		// new ArrayList<ArrayList<ArrayList<KBObject>>>();
		// for (ArrayList<DataVariableDataObjectBinding>
		// dataVariableDataObjectBindings : result) {
		// ArrayList<ArrayList<KBObject>> inner = new
		// ArrayList<ArrayList<KBObject>>();
		// for (DataVariableDataObjectBinding dataVariableDataObjectBinding :
		// dataVariableDataObjectBindings) {
		// inner.add(dataVariableDataObjectBinding.toArrayList());
		// }
		// coerced.add(inner);
		// }

		if (logger.isInfoEnabled()) {
			String returnString = AWGLoggerHelper.getReturnString("<findDataSources> q3.1", result);
			logger.debug(returnString);
		}
		return result;
	}

	/**
	 * suppose some of the dataVariables can be bound, but others can not - we
	 * call this partial bindings. this method will return all sets of partial
	 * bindings. NOTE: this assumes that EVERY dod contains as its subject a
	 * dataVariable [[[dataVariable0 weather-xxx, dataVariable1
	 * weather-model-xxx, ... dataVariableN null] [dataVariable0 weather-yyy,
	 * dataVariable1 weather-model-yyy, ... dataVariableN null]]]
	 * 
	 * @param dods
	 *            array list of data object descriptions
	 * @return lists of lists of dataVariable <==> DataObject mappings
	 */
	public ArrayList<ArrayList<DataVariableDataObjectBinding>> findDataSourcesWithPartialBindings(ArrayList<KBTriple> dods) {

		if (logger.isInfoEnabled()) {
			logger.debug("<Q3.1> Data Characterization is looking for partial bindings.");
		}

		KBAPI api = this.getApi();
		SparqlFactory sparqlFactory = this.getSparqlFactory();
		HashMap<String, ArrayList<KBTriple>> dataVariableMap = sparqlFactory.dodsForDataVariables(dods);

		ArrayList<SparqlQuery> sparqlQueries = new ArrayList<SparqlQuery>();
		Set<String> dataVariableMapKeys = dataVariableMap.keySet();
		for (String dataVariableMapKey : dataVariableMapKeys) {
			ArrayList<KBTriple> dodsForVariable = dataVariableMap.get(dataVariableMapKey);
			SparqlQuery sparqlQuery = sparqlFactory.makeSparqlQueryFromDataObjectDescriptions(dodsForVariable);
			sparqlQueries.add(sparqlQuery);
		}

		ArrayList<ArrayList<DataVariableDataObjectBinding>> allBindings = new ArrayList<ArrayList<DataVariableDataObjectBinding>>();

		// int indexCounter = -1;
		// HashMap<Integer, DataVariableDataObjectBinding> indexesMap =
		// new HashMap<Integer, DataVariableDataObjectBinding>();
		// ArrayList<ArrayList>indexList = new ArrayList<ArrayList>();

		for (SparqlQuery sparqlQuery : sparqlQueries) {
			ArrayList<String> variables = sparqlQuery.getVariables();
			HashMap<String, KBObject> variableMap = sparqlQuery.getVariableMap();
			String query = sparqlQuery.getQuery();
			ArrayList<ArrayList<SparqlQuerySolution>> queryResults = api.sparqlQuery(query);

			ArrayList<DataVariableDataObjectBinding> listOfBindings = new ArrayList<DataVariableDataObjectBinding>();

			// ArrayList listOfIndexes = new ArrayList();

			DataVariableDataObjectBinding dvdob;

			if (queryResults.isEmpty()) {
				KBObject dataVariable = variableMap.get(variables.get(0));
				dvdob = new DataVariableDataObjectBinding(dataVariable, null);
				listOfBindings.add(dvdob);
				allBindings.add(listOfBindings);
				// int newIndex = ++indexCounter;
				// indexesMap.put(newIndex, dvdob);
				// listOfIndexes.add(newIndex);
				// indexList.add(listOfIndexes);
			} else {
				for (ArrayList<SparqlQuerySolution> queryResult : queryResults) {
					for (SparqlQuerySolution sparqlQuerySolution : queryResult) {
						String variableName = sparqlQuerySolution.getVariable();
						KBObject kboVariable = variableMap.get(variableName);
						KBObject dataObject = sparqlQuerySolution.getObject();
						dvdob = new DataVariableDataObjectBinding(kboVariable, dataObject);
						listOfBindings.add(dvdob);
						// int newIndex = ++indexCounter;
						// indexesMap.put(newIndex, dvdob);
						// listOfIndexes.add(newIndex);
					}
				}
				allBindings.add(listOfBindings);
				// indexList.add(listOfIndexes);
			}
		}

		// ArrayList<ArrayList>allCombosOfIndexes =
		// this.allCombinations(indexList);
		// System.out.println("allCombosOfIndexes = " + allCombosOfIndexes);
		// System.out.println("indexesMap = " + indexesMap);
		ArrayList<ArrayList> downcast = new ArrayList<ArrayList>();
		for (ArrayList<DataVariableDataObjectBinding> bindings : allBindings) {
			downcast.add(bindings);
		}
		ArrayList<ArrayList> allCombos = this.allCombinations(downcast);
		// System.out.println("allCombos = " + allCombos);

		ArrayList<ArrayList<DataVariableDataObjectBinding>> result = new ArrayList<ArrayList<DataVariableDataObjectBinding>>();
		for (ArrayList combos : allCombos) {
			ArrayList<DataVariableDataObjectBinding> inner = new ArrayList<DataVariableDataObjectBinding>();
			for (Object combo : combos) {
				if (combo instanceof DataVariableDataObjectBinding) {
					DataVariableDataObjectBinding o = (DataVariableDataObjectBinding) combo;
					inner.add(o);
				}
			}
			result.add(inner);
		}

		return result;
	}

	/**
	 * a recursive function that returns all combinations of a list of
	 * dataVariable DataObject pairs: e.g. ArrayList a = ((a 1) (a 2) (a 3) (a
	 * 4)) ArrayList b = ((b 1) (b 2) (b 3))) ArrayList c = ((c 1))
	 * allCombinations((a b c)) => (((a 1) (b 1) (c 1)) ((a 1) (b 2) (c 1)) ((a
	 * 1) (b 3) (c 1)) ((a 2) (b 1) (c 1)) ((a 2) (b 2) (c 1)) ((a 2) (b 3) (c
	 * 1)) ((a 3) (b 1) (c 1)) ((a 3) (b 2) (c 1)) ((a 3) (b 3) (c 1)) ((a 4) (b
	 * 1) (c 1)) ((a 4) (b 2) (c 1)) ((a 4) (b 3) (c 1))) This is an example of
	 * why Java really stinks. This function cannot handle lists over, say, size
	 * 10.
	 * 
	 * @param lists
	 *            a list of lists of dataVariable/DataObject pairs
	 * @return a list of all combinations of a list of dataVariable/DataObject
	 *         pairs.
	 */
	protected ArrayList<ArrayList> allCombinations(ArrayList<ArrayList> lists) {
		if (lists.size() == 1) {
			ArrayList<ArrayList> result = new ArrayList<ArrayList>();
			for (ArrayList list : lists) {
				for (Object o : list) {
					ArrayList<Object> tmp = new ArrayList<Object>();
					tmp.add(o);
					result.add(tmp);
				}
			}
			return result;
		} else {
			ArrayList<ArrayList> restOfLists = new ArrayList<ArrayList>();
			boolean skip = true;
			for (ArrayList list : lists) {
				if (skip) {
					skip = false;
				} else {
					restOfLists.add(list);
				}
			}
			ArrayList<ArrayList> combinations = this.allCombinations(restOfLists);
			ArrayList firstList = lists.get(0);
			ArrayList<ArrayList> mapCanResult = new ArrayList<ArrayList>();
			for (Object firstListElm : firstList) {
				for (ArrayList combinationElm : combinations) {
					ArrayList cons = new ArrayList();
					cons.add(firstListElm);
					cons.addAll(combinationElm);
					mapCanResult.add(cons);
				}
			}
			return mapCanResult;
		}
	}

	/**
	 * returns a data object for an object name or id (url vs. name)
	 * 
	 * @param dataObjectNameOrId
	 *            the id or name of the dataObject
	 * @return a KBObject
	 */
	public KBObject dataObjectForDataObjectNameOrId(String dataObjectNameOrId) {
		KBAPI api = this.getApi();
		KBObject dataObject;
		if ((dataObject = api.getIndividual(dataObjectNameOrId)) != null) {
			return dataObject;
		} else {
			String defaultNamespace = (String) api.getPrefixNamespaceMap().get("");
			dataObject = api.getIndividual(defaultNamespace + dataObjectNameOrId);
			return dataObject;
		}
	}

	/**
	 * Q4.1 and Q8.2
	 * <p/>
	 * If metricsOrCharacteristics ArrayList is null or empty, then for the
	 * given dataSourceId this function returns an XML string that represents
	 * all the data metrics (and charateristics) of a the data source.
	 * 
	 * @param dataObjectId
	 *            the (unique) id of the dataObject
	 * @return a string of xml
	 */
	public String findDataMetricsForDataObject(String dataObjectId) {

		if (logger.isInfoEnabled()) {
			HashMap<String, Object> argumentMap = new HashMap<String, Object>(1);
			argumentMap.put("dataObjectId", dataObjectId);
			String arguments = AWGLoggerHelper.getArgumentString("<findDataMetricsForDataObject> q4.1", argumentMap);
			logger.debug(arguments);
		}

		StringBuilder result = new StringBuilder();
		result.append(MetricsHelper.HEADER);

		KBAPI api = this.getApi();
		KBObject dataObject = this.dataObjectForDataObjectNameOrId(dataObjectId);

		HashMap<String, KBObject> opmap = this.getPropertyMap();
		HashMap<String, KBObject> dpmap = this.getDatatypeMap();

		for (KBObject prop : api.getSubPropertiesOf(opmap.get("hasMetrics"))) {
			KBObject val = api.getPropertyValue(dataObject, prop);
			if (val != null) {
				result.append(MetricsHelper.getMetricXML(prop.getName(), val.getName(), "dataCharacteristics", null));
			}
		}

		for (KBObject prop : api.getSubPropertiesOf(dpmap.get("hasDataMetrics"))) {
			KBObject val = api.getDatatypePropertyValue(dataObject, prop);
			if (val != null && val.getValue() != null) {
				result.append(MetricsHelper.getMetricXML(prop.getName(), val.getValue(), "dataMetrics", val.getValue().getClass()));
			}
		}
		
		KBObject val = api.getClassOfInstance(dataObject);
		if (val != null) {
			result.append(MetricsHelper.getMetricXML("type", val.getName(), "rdfMetrics", null));
		}

		result.append(MetricsHelper.FOOTER);

		if (logger.isInfoEnabled()) {
			String resultValue = AWGLoggerHelper.getReturnString("<findDataMetricsForDataObject> q4.1", "<some xml>");
			logger.debug(resultValue);
		}

		return result.toString();
	}

	public ArrayList<KBTriple> getDataConstraints(String dataObjectId, String varId) {

		if (logger.isInfoEnabled()) {
			HashMap<String, Object> argumentMap = new HashMap<String, Object>(1);
			argumentMap.put("dataObjectId", dataObjectId);
			argumentMap.put("varid", varId);
			String arguments = AWGLoggerHelper.getArgumentString("<setDataConstraintsForVariable> q4.1b", argumentMap);
			logger.debug(arguments);
		}

		KBAPI api = this.getApi();
		KBAPI tapi = ontologyFactory.getAPI(OntSpec.PLAIN);

		ArrayList<KBTriple> constraints = new ArrayList<KBTriple>();

		KBObject dataObject = this.dataObjectForDataObjectNameOrId(dataObjectId);
		KBObject varObject = tapi.getResource(varId);

		HashMap<String, KBObject> opmap = this.getPropertyMap();
		HashMap<String, KBObject> dpmap = this.getDatatypeMap();

		for (KBObject prop : api.getSubPropertiesOf(opmap.get("hasMetrics"))) {
			KBObject val = api.getPropertyValue(dataObject, prop);
			if (val != null) {
				constraints.add(tapi.addTriple(varObject, prop, val));
			}
		}

		for (KBObject prop : api.getSubPropertiesOf(dpmap.get("hasDataMetrics"))) {
			KBObject val = api.getDatatypePropertyValue(dataObject, prop);
			if (val != null && val.getValue() != null) {
				constraints.add(tapi.addTriple(varObject, prop, val));
			}
		}

		return constraints;
	}

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
	 *            the (unique) id of the dataObject
	 * @param metricsAndCharacteristics
	 *            an array of metrics/characteristics to return
	 * @return a string of xml
	 * @return a string of xml
	 */
	public String findDataMetricsForDataObject(String dataObjectId, ArrayList<String> metricsAndCharacteristics) {

		if (logger.isInfoEnabled()) {
			HashMap<String, Object> argumentMap = new HashMap<String, Object>(2);
			argumentMap.put("dataObjectId", dataObjectId);
			argumentMap.put("metricsAndCharacteristics", metricsAndCharacteristics);
			String arguments = AWGLoggerHelper.getArgumentString("<findDataMetricsForDataObject> q4.1/q8.2", argumentMap);
			logger.debug(arguments);
		}

		KBAPI api = this.getApi();
		HashMap<String, KBObject> pmap = this.getPropertyMap();
		HashMap<String, KBObject> dtmap = this.getDatatypeMap();
		ArrayList<KBObject> properties = new ArrayList<KBObject>();
		ArrayList<KBObject> datatypes = new ArrayList<KBObject>();

		for (String metricOrCharacteristic : metricsAndCharacteristics) {
			KBObject kbo;
			if ((kbo = pmap.get(metricOrCharacteristic)) != null) {
				properties.add(kbo);
			} else if ((kbo = dtmap.get(metricOrCharacteristic)) != null) {
				datatypes.add(kbo);
			} else if (logger.isDebugEnabled()) {
				logger.warn("No metric or characteristic found for " + metricOrCharacteristic);
			}
		}

		KBObject dataObject = this.dataObjectForDataObjectNameOrId(dataObjectId);

		HashMap<String, String> propertyValues = new HashMap<String, String>();
		for (KBObject property : properties) {
			KBObject propertyValue = api.getPropertyValue(dataObject, property);
			propertyValues.put(property.getName(), propertyValue.getName());
		}

		HashMap<String, String> datatypeValues = new HashMap<String, String>();
		for (KBObject datatype : datatypes) {
			KBObject datatypeValue = api.getDatatypePropertyValue(dataObject, datatype);
			Object value = datatypeValue.getValue();
			String stringValue = "";
			if (value instanceof String) {
				stringValue = (String) value;
			} else if (value instanceof Boolean) {
				stringValue = stringValue + value;
			} else if (value instanceof Number) {
				stringValue = stringValue + value;
			}
			datatypeValues.put(datatype.getName(), stringValue);
		}

		// todo 2007-08-17 What is the distinction between a Metric and a
		// Dimension? How is it expressed in the DC ontology
		String newLineChar = System.getProperty("line.separator");
		String header = "<MetricResults xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"dc_results_draft.xsd\">"
				+ newLineChar;
		String footer = "</MetricResults>";
		String metricBegin = "  <Metric name=\"";
		String metricEnd = "\">" + newLineChar;
		String metricClose = "  </Metric>" + newLineChar;
		String dimensionBegin = "    <Dimension name=\"";
		String dimensionEnd = "\">" + newLineChar;
		String dimensionClose = "    </Dimension>" + newLineChar;
		String valueBegin = "      <Value>";
		String valueClose = "</Value>" + newLineChar;

		StringBuilder result = new StringBuilder();
		result.append(header);

		Set<String> keys = propertyValues.keySet();
		for (String propertyValue : keys) {
			result.append(metricBegin);
			result.append("dataCharacteristic");
			result.append(metricEnd);
			result.append(dimensionBegin);
			result.append(propertyValue);
			result.append(dimensionEnd);
			result.append(valueBegin);
			result.append(propertyValues.get(propertyValue));
			result.append(valueClose);
			result.append(dimensionClose);
			result.append(metricClose);
		}

		keys = datatypeValues.keySet();
		for (String datatypeValue : keys) {
			result.append(metricBegin);
			result.append("dataMetric");
			result.append(metricEnd);
			result.append(dimensionBegin);
			result.append(datatypeValue);
			result.append(dimensionEnd);
			result.append(valueBegin);
			result.append(datatypeValues.get(datatypeValue));
			result.append(valueClose);
			result.append(dimensionClose);
			result.append(metricClose);
		}

		result.append(footer);
		if (logger.isInfoEnabled()) {
			String resultValue = AWGLoggerHelper.getReturnString("<findDataMetricsForDataObject> q4.1/8.2", result.toString());
			logger.debug(resultValue);
		}

		return result.toString();
	}

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
	public Collection<DataSourceLocationObject> findDataObjectLocationsAndAccessAttributes(String dataObjectId) {
		if (logger.isInfoEnabled()) {
			HashMap<String, Object> argumentMap = new HashMap<String, Object>(1);
			argumentMap.put("dataObjectId", dataObjectId);
			String arguments = AWGLoggerHelper.getArgumentString("q5.1a", argumentMap);
			logger.debug(arguments);
		}
		KBAPI api = this.getApi();
		HashMap<String, KBObject> propertyMap = this.getPropertyMap();
		HashMap<String, KBObject> datatypeMap = this.getDatatypeMap();

		Collection<DataSourceLocationObject> result = new ArrayList<DataSourceLocationObject>();

		KBObject dataObject = this.dataObjectForDataObjectNameOrId(dataObjectId);
		if (dataObject == null) {
			// This is an intermediate/output data object, so just stop and
			// return empty
			return result;
		}
		KBObject hasLocationProperty = propertyMap.get("hasLocation");
		KBObject hasProtocolProperty = propertyMap.get("hasProtocol");
		KBObject hasSiteProperty = propertyMap.get("hasSite");
		KBObject hasURIProperty = datatypeMap.get("hasURI");
		KBObject hasMd5Sum = datatypeMap.get("hasMd5sum");
		KBObject md5sum = api.getDatatypePropertyValue(dataObject, hasMd5Sum);
		KBObject hasSizeKB = datatypeMap.get("hasSizeInKB");
		KBObject sizeKb = api.getDatatypePropertyValue(dataObject, hasSizeKB);

		ArrayList<KBObject> locations = api.getPropertyValues(dataObject, hasLocationProperty);

		float size = Float.parseFloat(sizeKb.getValue().toString());
		float availabilityTime = 0;

		for (KBObject location : locations) {
			DataSourceLocationObject dsla = new DataSourceLocationObject();
			dsla.setAvailabilityTime(0);
			dsla.setSite(api.getPropertyValue(location, hasSiteProperty).getName());
			// dsla.setLocation(api.getPropertyValue(location,
			// hasSiteProperty).getName());
			dsla.setChecksum(md5sum.getValue().toString());
			dsla.setAvailabilityTime(availabilityTime);
			KBObject dataObjectProtocol = api.getPropertyValue(location, hasProtocolProperty);
			KBObject dataObjectURI = api.getDatatypePropertyValue(location, hasURIProperty);
			if (dataObjectURI != null && dataObjectProtocol != null) {
				String URI = dataObjectProtocol.getName() + "://" + dataObjectURI.getValue() + dataObject.getName();
				dsla.setLocation(URI);
			}
			dsla.setFileSize(new Storage(size, Storage.UNIT.KB));
			dsla.setDataObjectID(dataObjectId);

			result.add(dsla);
		}

		if (logger.isInfoEnabled()) {
			String resultValue = AWGLoggerHelper.getReturnString("q5.1a", result.toString());
			logger.debug(resultValue);
		}
		return result;
	}

	/**
	 * Q5.1a
	 * <p>
	 * Given a collection of data source id, returns a map of dsid to collection
	 * of <code>DataSourceLocationObject</code> objects.
	 * 
	 * @param dataObjectIds
	 *            the collection of DataObject identifiers.
	 * 
	 * @return a Map indexed the DataObject Id's. Each value is a Collection of
	 *         <code>DataSourceLocationObject</code> objects. In case of an
	 *         entry not being found, then an empty collection is associated.
	 */
	public Map<String, Collection<DataSourceLocationObject>> findDataObjectLocationsAndAccessAttributes(Collection<String> dataObjectIds) {
		HashMap<String, Collection<DataSourceLocationObject>> result = new HashMap<String, Collection<DataSourceLocationObject>>();
		for (String dataObjectId : dataObjectIds) {
			result.put(dataObjectId, findDataObjectLocationsAndAccessAttributes(dataObjectId));
		}
		return result;
	}

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
	public boolean registerDataObject(DataSourceLocationObject dsla) {
		WorkflowGenerationProvenanceCatalogConnector wgpcConn = new WorkflowGenerationProvenanceCatalogConnector();
		ArrayList values = new ArrayList(2);
		values.add(dsla.getDataObjectID());
		values.add(dsla.getLocation());
		return wgpcConn.insertIntoTable("data_location", values);
	}

	public void close() {

	}

	/**
	 * Q8.1
	 * <p/>
	 * request to register a dataSourceId and set its dsla.
	 * 
	 * @param dataObjectId
	 *            a data source id
	 * @param dsla
	 *            (id url site size checksum availabilityTime)
	 * @return a boolean indicating success or failure
	 * 
	 *         public boolean registerDataObject(String dataObjectId, String
	 *         dsla) { return false; }
	 * 
	 * 
	 ** 
	 *         Q8.1-alt
	 *         <p/>
	 *         request to register a dataSourceId and set its dsla.
	 * 
	 * @param dataObjectId
	 *            the (unique) id of the dataObject
	 * @param dsla
	 *            (id url site size checksum availabilityTime)
	 * @param metrics
	 *            key value pairs for each of the metrics @return a boolean
	 *            indicating success or failure
	 * @return true iff successful
	 * 
	 *         public boolean registerDataObject(String dataObjectId, String
	 *         dsla, HashMap<String, String> metrics) { return false; }
	 */

	/**
	 * Getter for property 'api'.
	 * 
	 * @return Value for property 'api'.
	 */
	public KBAPI getApi() {
		return api;
	}

	/**
	 * Setter for property 'api'.
	 * 
	 * @param api
	 *            Value to set for property 'api'.
	 */
	public void setApi(KBAPI api) {
		this.api = api;
	}

	/**
	 * Getter for property 'dcUrl'.
	 * 
	 * @return Value for property 'dcUrl'.
	 */
	public String getDcUrl() {
		return dcUrl;
	}

	/**
	 * Setter for property 'dcUrl'.
	 * 
	 * @param dcUrl
	 *            Value to set for property 'dcUrl'.
	 */
	public void setDcUrl(String dcUrl) {
		this.dcUrl = dcUrl;
	}

	/**
	 * Getter for property 'domainUrl'.
	 * 
	 * @return Value for property 'domainUrl'.
	 */
	public String getDomainUrl() {
		return domainUrl;
	}

	/**
	 * Setter for property 'domainUrl'.
	 * 
	 * @param domainUrl
	 *            Value to set for property 'domainUrl'.
	 */
	public void setDomainUrl(String domainUrl) {
		this.domainUrl = domainUrl;
	}

	/**
	 * Getter for property 'libraryUrl'.
	 * 
	 * @return Value for property 'libraryUrl'.
	 */
	public String getLibraryUrl() {
		return libraryUrl;
	}

	/**
	 * Setter for property 'libraryUrl'.
	 * 
	 * @param libraryUrl
	 *            Value to set for property 'libraryUrl'.
	 */
	public void setLibraryUrl(String libraryUrl) {
		this.libraryUrl = libraryUrl;
	}

	/**
	 * Getter for property 'dcNamespace'.
	 * 
	 * @return Value for property 'dcNamespace'.
	 */
	public String getDcNamespace() {
		return dcNamespace;
	}

	/**
	 * Setter for property 'dcNamespace'.
	 * 
	 * @param dcNamespace
	 *            Value to set for property 'dcNamespace'.
	 */
	public void setDcNamespace(String dcNamespace) {
		this.dcNamespace = dcNamespace;
	}

	/**
	 * Getter for property 'domainNamespace'.
	 * 
	 * @return Value for property 'domainNamespace'.
	 */
	public String getDomainNamespace() {
		return domainNamespace;
	}

	/**
	 * Setter for property 'domainNamespace'.
	 * 
	 * @param domainNamespace
	 *            Value to set for property 'domainNamespace'.
	 */
	public void setDomainNamespace(String domainNamespace) {
		this.domainNamespace = domainNamespace;
	}

	/**
	 * Getter for property 'uriPrefix'.
	 * 
	 * @return Value for property 'uriPrefix'.
	 */
	public String getUriPrefix() {
		return uriPrefix;
	}

	/**
	 * Setter for property 'uriPrefix'.
	 * 
	 * @param uriPrefix
	 *            Value to set for property 'uriPrefix'.
	 */
	public void setUriPrefix(String uriPrefix) {
		this.uriPrefix = uriPrefix;
	}

	/**
	 * Getter for property 'rootOntologyPath'.
	 * 
	 * @return Value for property 'rootOntologyPath'.
	 */
	public String getRootOntologyPath() {
		return rootOntologyPath;
	}

	/**
	 * Setter for property 'rootOntologyPath'.
	 * 
	 * @param rootOntologyPath
	 *            Value to set for property 'rootOntologyPath'.
	 */
	public void setRootOntologyPath(String rootOntologyPath) {
		this.rootOntologyPath = rootOntologyPath;
	}

	/**
	 * Getter for property 'propertyMap'.
	 * 
	 * @return Value for property 'propertyMap'.
	 */
	public HashMap<String, KBObject> getPropertyMap() {
		return propertyMap;
	}

	/**
	 * Setter for property 'propertyMap'.
	 * 
	 * @param propertyMap
	 *            Value to set for property 'propertyMap'.
	 */
	public void setPropertyMap(HashMap<String, KBObject> propertyMap) {
		this.propertyMap = propertyMap;
	}

	/**
	 * Getter for property 'conceptMap'.
	 * 
	 * @return Value for property 'conceptMap'.
	 */
	public HashMap<String, KBObject> getConceptMap() {
		return conceptMap;
	}

	/**
	 * Setter for property 'conceptMap'.
	 * 
	 * @param conceptMap
	 *            Value to set for property 'conceptMap'.
	 */
	public void setConceptMap(HashMap<String, KBObject> conceptMap) {
		this.conceptMap = conceptMap;
	}

	/**
	 * Getter for property 'owlObjectProperties'.
	 * 
	 * @return Value for property 'owlObjectProperties'.
	 */
	public ArrayList<KBObject> getOwlObjectProperties() {
		return owlObjectProperties;
	}

	/**
	 * Setter for property 'owlObjectProperties'.
	 * 
	 * @param owlObjectProperties
	 *            Value to set for property 'owlObjectProperties'.
	 */
	public void setOwlObjectProperties(ArrayList<KBObject> owlObjectProperties) {
		this.owlObjectProperties = owlObjectProperties;
	}

	/**
	 * Getter for property 'owlConcepts'.
	 * 
	 * @return Value for property 'owlConcepts'.
	 */
	public ArrayList<KBObject> getOwlConcepts() {
		return owlConcepts;
	}

	/**
	 * Setter for property 'owlConcepts'.
	 * 
	 * @param owlConcepts
	 *            Value to set for property 'owlConcepts'.
	 */
	public void setOwlConcepts(ArrayList<KBObject> owlConcepts) {
		this.owlConcepts = owlConcepts;
	}

	/**
	 * Getter for property 'ontologyFactory'.
	 * 
	 * @return Value for property 'ontologyFactory'.
	 */
	public OntFactory getOntologyFactory() {
		return ontologyFactory;
	}

	/**
	 * Setter for property 'ontologyFactory'.
	 * 
	 * @param ontologyFactory
	 *            Value to set for property 'ontologyFactory'.
	 */
	public void setOntologyFactory(OntFactory ontologyFactory) {
		this.ontologyFactory = ontologyFactory;
	}

	/**
	 * Getter for property 'datatypeMap'.
	 * 
	 * @return Value for property 'datatypeMap'.
	 */
	public HashMap<String, KBObject> getDatatypeMap() {
		return datatypeMap;
	}

	/**
	 * Setter for property 'datatypeMap'.
	 * 
	 * @param datatypeMap
	 *            Value to set for property 'datatypeMap'.
	 */
	public void setDatatypeMap(HashMap<String, KBObject> datatypeMap) {
		this.datatypeMap = datatypeMap;
	}

	/**
	 * Getter for property 'owlDatatypeProperties'.
	 * 
	 * @return Value for property 'owlDatatypeProperties'.
	 */
	public ArrayList<KBObject> getOwlDatatypeProperties() {
		return owlDatatypeProperties;
	}

	/**
	 * Setter for property 'owlDatatypeProperties'.
	 * 
	 * @param owlDatatypeProperties
	 *            Value to set for property 'owlDatatypeProperties'.
	 */
	public void setOwlDatatypeProperties(ArrayList<KBObject> owlDatatypeProperties) {
		this.owlDatatypeProperties = owlDatatypeProperties;
	}

	/**
	 * Getter for property 'sparqlFactory'.
	 * 
	 * @return Value for property 'sparqlFactory'.
	 */
	public SparqlFactory getSparqlFactory() {
		return sparqlFactory;
	}

	/**
	 * Setter for property 'sparqlFactory'.
	 * 
	 * @param sparqlFactory
	 *            Value to set for property 'sparqlFactory'.
	 */
	public void setSparqlFactory(SparqlFactory sparqlFactory) {
		this.sparqlFactory = sparqlFactory;
	}

	/*
	 * Template creation API queries
	 */

	public ArrayList<ConstraintProperty> getAllConstraints() {
		ArrayList<ConstraintProperty> cons = new ArrayList<ConstraintProperty>();
		KBObject mprop = ontapi_plain.getProperty(this.dcNamespace + "hasMetrics");
		KBObject dmprop = ontapi_plain.getProperty(this.dcNamespace + "hasDataMetrics");

		for (KBObject prop : ontapi_plain.getSubPropertiesOf(mprop)) {
			ConstraintProperty c = new ConstraintProperty(prop.getID(), ConstraintProperty.OBJECTCONSTRAINT);
			KBObject propdom = ontapi_plain.getPropertyDomain(prop);
			KBObject proprange = ontapi_plain.getPropertyRange(prop);
			if (propdom != null)
				c.addDomainClassId(propdom.getID());
			if (proprange != null) {
				KBObject propRangeCls = ontapi.getConcept(proprange.getID());
				if (propRangeCls != null)
					for (KBObject inst : ontapi.getInstancesOfClass(propRangeCls))
						c.addRangeObjectId(inst.getID());
			}
			cons.add(c);
		}
		for (KBObject prop : ontapi_plain.getSubPropertiesOf(dmprop)) {
			ConstraintProperty c = new ConstraintProperty(prop.getID(), ConstraintProperty.DATACONSTRAINT);
			KBObject propdom = ontapi_plain.getPropertyDomain(prop);
			KBObject proprange = ontapi_plain.getPropertyRange(prop);
			if (propdom != null)
				c.addDomainClassId(propdom.getID());
			if (proprange != null)
				c.addRangeObjectId(proprange.getID());
			cons.add(c);
		}

		// System.out.println(cons);
		return cons;
	}

	public ArrayList<String> getAllConceptIds() {
		ArrayList<String> conceptIds = new ArrayList<String>();
		for (KBObject cls : ontapi.getAllClasses()) {
			if (cls.getNamespace() != null && cls.getNamespace().equals(this.domainNamespace)) {
				conceptIds.add(cls.getID());
			}
		}
		return conceptIds;
	}

	public void setRequestId(String id) {
		this.request_id = id;
	}

	public String createDataSetIDFromType(String type, String metrics) {
		String nameformat = this.conceptNameFormat.get(type);
		if(nameformat != null && metrics != null) {
			HashMap<String, ArrayList> propValMap = MetricsHelper.parseMetricsXML(metrics);
			Pattern pat = Pattern.compile("\\[(.+?)\\]");
			Matcher m = pat.matcher(nameformat);
			StringBuffer sb = new StringBuffer();
			while(m.find()) {
				ArrayList<Object> tmp = propValMap.get(m.group(1));
				if(tmp != null && tmp.size() > 0 && tmp.get(0) != null)
					m.appendReplacement(sb, MetricsHelper.getValueString(tmp.get(0)));
				else
					m.appendReplacement(sb, "");
			}
			m.appendTail(sb);
			return sb.toString().replaceAll("[^a-zA-Z0-9_]", "_");
		}
		
		return null;
	}
	
	public String createDataSetIDFromDescription(String key) {
		// FIXME: Keep state of the key here somehow..
		// or just return a shahash of the key ?
		// <rdfs:comment>name=[hasNamePrefix]-[hasOffsets]</rdfs:comment>
		if (key == null)
			return null;
		MessageDigest m;
		try {
			m = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		m.update(key.getBytes(), 0, key.length());
		return new BigInteger(1, m.digest()).toString(16);
		// return UuidGen.generateAUuid("");
	}

	/**
	 * <p>
	 * Checks if the predicate of a triple is a sub-property of
	 * classificationProperty
	 */
	public boolean isClassificationProperty(KBTriple triple) {
		KBObject property = triple.getPredicate();
		if (property.getID().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
			KBObject dataObject = conceptMap.get("DataObject");
			KBObject thisObject = conceptMap.get(triple.getObject().getName());
			return api.hasSubClass(dataObject, thisObject);
		}
		if (property.getID().equals(PropertiesHelper.getQueryNamespace() + "type"))
			return true;
		
		KBObject classificationProperty = propertyMap.get("hasMetrics");
		KBObject candidateSubProperty = propertyMap.get(property.getName());
		if (api.hasSubProperty(classificationProperty, candidateSubProperty))
			return true;
		
		classificationProperty = propertyMap.get("hasDataMetrics");
		candidateSubProperty = datatypeMap.get(property.getName());
		return api.hasSubProperty(classificationProperty, candidateSubProperty);
	}

	/**
	 * <p>
	 * Checks if one individual belongs to class that is subsumed by the class
	 * of the other
	 * <p>
	 * In the case of the property rdf:type, the ids are from classes, not from
	 * individuals
	 */
	public SubsumptionDegree checkSubsumtion(String relation, String classID1, String classID2) {
		if (classID1.equals(classID2))
			return SubsumptionDegree.EQUIVALENT;
		KBObject class1;
		KBObject class2;
		// If the relation is type then
		if (relation.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#" + "type") || (relation.equals(NamespaceManager.getNamespace("wfq") + "hasType"))) {
			class1 = api.getConcept(classID1);
			class2 = api.getConcept(classID2);
		} else {
			KBObject individual1 = api.getIndividual(classID1);
			KBObject individual2 = api.getIndividual(classID2);
			if (individual1 == null || individual2 == null)
				return SubsumptionDegree.DISJOINT;
			class1 = api.getClassOfInstance(individual1);
			class2 = api.getClassOfInstance(individual2);
		}
		if (api.hasSubClass(class1, class2))
			return SubsumptionDegree.SUBSUMES;
		else if (api.hasSubClass(class2, class1))
			return SubsumptionDegree.SUBSUMED;
		return SubsumptionDegree.DISJOINT;
	}
	
	/**
	 * <p>
	 * Check if first class subsumes the second class
	 */
	public boolean subsumes(String cls1, String cls2) {
		SubsumptionDegree deg = this.checkSubsumtion("http://www.w3.org/1999/02/22-rdf-syntax-ns#" + "type", cls1, cls2);
		if(deg == SubsumptionDegree.EQUIVALENT || deg == SubsumptionDegree.SUBSUMES)
			return true;
		return false;
	}

	public KBTripleList getDatasetClassificationProperties(KBTriple constraint) {
		KBTripleList output = new KBTripleList();
		KBObject individual = api.getIndividual(constraint.getObject().getID());
		if (individual == null)
			return output;
		KBObject classificationProperty = propertyMap.get("hasMetrics");
		if (classificationProperty == null) {
			System.err.println("classificationProperty not found");
			return output;
		}
		KBObject subject = constraint.getSubject();
		ArrayList<KBObject> subproperties = api.getSubPropertiesOf(classificationProperty);
		for (KBObject property : subproperties) {
			KBObject value = api.getPropertyValue(individual, property);
			if (value != null) {
				ArrayList<KBObject> triple = new ArrayList<KBObject>();
				triple.add(subject);
				triple.add(property);
				triple.add(value);
				output.add(api.tripleFromArrayList(triple));
			}
		}
		
		classificationProperty = propertyMap.get("hasDataMetrics");
		if (classificationProperty == null) {
			System.err.println("classificationProperty not found");
			return output;
		}
		subject = constraint.getSubject();
		subproperties = api.getSubPropertiesOf(classificationProperty);
		for (KBObject property : subproperties) {
			KBObject value = api.getPropertyValue(individual, property);
			if (value != null) {
				ArrayList<KBObject> triple = new ArrayList<KBObject>();
				triple.add(subject);
				triple.add(property);
				triple.add(value);
				output.add(api.tripleFromArrayList(triple));
			}
		}
		return output;
	}

}

// //////////////////////////////////////////////////////////////////////////////
// DEAD SEA
// //////////////////////////////////////////////////////////////////////////////

// we only have one dataVariable
// if (dataVariableMapKeys.size() == 1) {
// filterMap = shortestMap;
// } else {
// System.out.println("dataVariableMapKeys = " + dataVariableMapKeys);
// for (KBObject key : dataVariableMapKeys) {
// System.out.println("key = " + key);
// ArrayList<KBObject> shortestForDataVariable = shortestMap.get(key);
// System.out.println("shortestForDataVariable = " + shortestForDataVariable);
// filterMap.put(key, new ArrayList<KBObject>());
// if (!shortestForDataVariable.isEmpty()) {
// ArrayList<ArrayList<KBObject>>listOfListsOfDataObjectsThatMatchedNthDod =
// dataVariableMap.get(key);
// listOfListsOfDataObjectsThatMatchedNthDod.remove(shortestForDataVariable);
// for (KBObject refDataObject : shortestForDataVariable) {
// boolean missing = false;
// for (ArrayList<KBObject> listOfDataObjects :
// listOfListsOfDataObjectsThatMatchedNthDod) {
// if (!this.listContainsKBObject(listOfDataObjects, refDataObject)) {
// missing = true;
// System.out.println("missing = " + missing);
// }
// }
// if (!missing) {
// filterMap.get(key).add(refDataObject);
// }
// }
// }
// }
// }

// protected ArrayList<ArrayList<KBObject>>
// makeListOfDataVariableDataObjectPairs(HashMap<KBObject, ArrayList<KBObject>>
// map,
// KBObject key) {
// ArrayList<ArrayList<KBObject>> result = new ArrayList<ArrayList<KBObject>>();
// ArrayList<KBObject> values = map.get(key);
// if (values.isEmpty()) {
// ArrayList<KBObject>inner = new ArrayList<KBObject>();
// inner.add(key);
// inner.add(null);
// result.add(inner);
// } else {
// for (KBObject value : values) {
// ArrayList<KBObject>inner = new ArrayList<KBObject>();
// inner.add(key);
// inner.add(value);
// result.add(inner);
// }
// }
// return result;
// }

// protected ArrayList<ArrayList<ArrayList<KBObject>>>
// hashMapToListOfDataVariableDataObjectPairs(HashMap<KBObject,
// ArrayList<KBObject>> map) {
// ArrayList<ArrayList<ArrayList<KBObject>>> result = new
// ArrayList<ArrayList<ArrayList<KBObject>>>();
// Set<KBObject>keys = map.keySet();
// for (KBObject key : keys) {
// result.add(this.makeListOfDataVariableDataObjectPairs(map, key));
// }
// return result;
// }

// protected ArrayList<ArrayList<ArrayList>>
// hashMapToListOfDataVariableDataObjectPairs(HashMap<KBObject,
// ArrayList<KBObject>> map) {
// ArrayList<ArrayList<ArrayList>> result = new
// ArrayList<ArrayList<ArrayList>>();
// Set<KBObject>keys = map.keySet();
// for (KBObject key : keys) {
// result.add(this.makeListOfDataVariableDataObjectPairs(map, key));
// }
// return result;
// }
