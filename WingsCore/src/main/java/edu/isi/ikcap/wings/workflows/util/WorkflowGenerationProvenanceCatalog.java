package edu.isi.ikcap.wings.workflows.util;

import edu.isi.ikcap.wings.catalogs.components.classes.ComponentMapsAndRequirements;
import edu.isi.ikcap.wings.catalogs.data.classes.DataVariableDataObjectBinding;
import edu.isi.ikcap.wings.workflows.template.Role;
import edu.isi.ikcap.wings.workflows.template.Seed;
import edu.isi.ikcap.wings.workflows.template.Template;
import edu.isi.ikcap.wings.workflows.template.variables.ComponentVariable;
import edu.isi.ikcap.wings.workflows.template.variables.Variable;

import java.io.ObjectOutputStream;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


import java.io.Serializable;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;

import edu.isi.ikcap.ontapi.KBTriple;

/**
 * <p>
 * Title:
 * </p>
 * 
 * <p>
 * Description:
 * </p>
 * 
 * <p>
 * Copyright: Copyright (c) 2005
 * </p>
 * 
 * <p>
 * Company:
 * </p>
 * 
 * @author not attributable
 * @version 1.0
 */
public class WorkflowGenerationProvenanceCatalog {
	/**
	 * a connector to the provenance catalog
	 */
	public WorkflowGenerationProvenanceCatalogConnector wgpcConnector;

	/**
	 * the current seed
	 */
	public Seed currentSeed;

	public String userId;
	public String ipAddress;
	public String hostName;
	public String timeStamp;

	public WorkflowGenerationProvenanceCatalog() {
		this.wgpcConnector = new WorkflowGenerationProvenanceCatalogConnector();
		initVars();
	}

	public WorkflowGenerationProvenanceCatalog(boolean storeProvenance) {
		if (storeProvenance)
			this.wgpcConnector = new WorkflowGenerationProvenanceCatalogConnector();
		initVars();
	}

	private void initVars() {
		this.userId = System.getProperty("user.name");
		try {
			InetAddress addr = InetAddress.getLocalHost();
			this.ipAddress = addr.getHostAddress();
			this.hostName = addr.getHostName();
		} catch (Exception e) {
		}
		this.timeStamp = (new Date()).toString();
	}

	/**
	 * Q0.1 a unique seed id will be generated
	 * 
	 * @param seed
	 *            a seed
	 * @return true if successful
	 */
	public boolean addSeedToProvenanceCatalog(Seed seed) {
		ArrayList<Object> values = new ArrayList<Object>(6);
		values.add(seed.getName());
		values.add(seed.getSeedId());
		values.add(this.userId);
		values.add(this.ipAddress);
		values.add(this.hostName);
		values.add(this.timeStamp);
		values.add(seed.deriveInternalRepresentation());
		WorkflowGenerationProvenanceCatalogConnector wgpc = this.getWgpcConnector();
		return wgpc.insertIntoTable(wgpc.getSeedTable(), values);
	}

	/**
	 * register the candidate templates to the provenance catalog candidate
	 * templates are generated in step 1
	 * 
	 * @param templates
	 *            a list of templates
	 * @return true iff successful
	 */
	public boolean addSeedsToProvenanceCatalog(String seedId, ArrayList<Seed> seeds) {
		WorkflowGenerationProvenanceCatalogConnector wgpc = this.getWgpcConnector();
		for (Seed seed : seeds) {
			ArrayList<Object> values = new ArrayList<Object>(3);
			values.add(seedId);
			values.add(seed.getSeedId());
			values.add(seed.deriveInternalRepresentation());
			wgpc.insertIntoTable(wgpc.getAtomicSeedsTable(), values);
		}
		return true;
	}

	/**
	 * register query 2.1 results to the provenance catalog
	 * 
	 * @param sentMapsAndRequirements
	 *            component, input map , output map, and requirements/dods
	 * @param componentMapsAndRequirements
	 *            A List of {component, input map, output map, and requirements}
	 * @return true iff success
	 */
	public boolean addQuery2point1ToProvenanceCatalog(String seedId, ComponentMapsAndRequirements sentMapsAndRequirements,
			ArrayList<ComponentMapsAndRequirements> componentMapsAndRequirements) {
		ArrayList<Object> values = new ArrayList<Object>(4);
		values.add(seedId);
		values.add(UuidGen.generateAUuid("q2.1"));
		StringBuilder query = new StringBuilder();
		query.append("component=");
		query.append(sentMapsAndRequirements.getComponent().getID());
		query.append(" ");
		query.append("inputVariableMaps=");
		query.append(sentMapsAndRequirements.getInputMaps());
		query.append(" ");
		query.append("outputVariableMaps=");
		query.append(sentMapsAndRequirements.getOutputMaps());
		query.append(" ");
		query.append("redBox=");
		query.append(sentMapsAndRequirements.getRequirements());
		values.add(query.toString());
		StringBuilder result = new StringBuilder();
		result.append("componentMapsAndRequirements=");
		result.append(componentMapsAndRequirements);
		values.add(result.toString());
		WorkflowGenerationProvenanceCatalogConnector wgpc = this.getWgpcConnector();
		return wgpc.insertIntoTable(wgpc.getQuery2point1Table(), values);
	}

	/**
	 * register query 2.1 results to the provenance catalog AC will be using
	 * SOAP, so we should too
	 * 
	 * @param soapQuery
	 *            the soap query sent to ac
	 * @param soapResult
	 *            the soap result set back
	 * @return true iff successful
	 */
	public boolean addQuery2point1ToProvenanceCatalog(String seedId, String soapQuery, String soapResult) {
		ArrayList<Object> values = new ArrayList<Object>(4);
		values.add(seedId);
		values.add(UuidGen.generateAUuid("q2.1"));
		values.add(soapQuery);
		values.add(soapResult);
		WorkflowGenerationProvenanceCatalogConnector wgpc = this.getWgpcConnector();
		return wgpc.insertIntoTable(wgpc.getQuery2point1Table(), values);
	}

	/**
	 * register the specialized templates to the provenance catalog specialized
	 * templates are generated by the backward sweep
	 * 
	 * @param templates
	 *            a list of templates
	 * @return true iff successful
	 */
	public boolean addCandidateWorkflowsToProvenanceCatalog(String seedId, ArrayList<Template> templates) {
		WorkflowGenerationProvenanceCatalogConnector wgpc = this.getWgpcConnector();
		for (Template template : templates) {
			ArrayList<Object> values = new ArrayList<Object>(3);
			values.add(seedId);
			values.add(template.getID());
			values.add(template.deriveInternalRepresentation());
			wgpc.insertIntoTable(wgpc.getCandidateWorkflowsTable(), values);
		}
		return true;
	}

	/**
	 * register partially specified candidate template instances to the
	 * provenance catalog partially specified candidate template instances are
	 * generated in the forward sweep
	 * 
	 * @param templates
	 *            a list of templates
	 * @return true iff successful
	 */
	public boolean addBoundWorkflowsToProvenanceCatalog(String seedId, ArrayList<Template> templates) {
		WorkflowGenerationProvenanceCatalogConnector wgpc = this.getWgpcConnector();
		for (Template template : templates) {
			ArrayList<Object> values = new ArrayList<Object>(3);
			values.add(seedId);
			values.add(template.getID());
			// TODO: the template should be modified to include Dataset Bindings
			// values.add(template.toRdf());
			values.add("not yet implemented");
			wgpc.insertIntoTable(wgpc.getBoundWorkflowsTable(), values);
		}
		return true;
	}

	/**
	 * register query 3.1 to the provenance catalog a unique query id will be
	 * generated
	 * 
	 * @param sparqlQuery
	 *            the sparql query issued the DC
	 * @param xmlResult
	 *            the result of the sparql query
	 * @return true iff successful
	 */
	public boolean addQuery3point1ToProvenanceCatalog(String seedId, String sparqlQuery, String xmlResult) {
		ArrayList<Object> values = new ArrayList<Object>(4);
		values.add(seedId);
		values.add(UuidGen.generateAUuid("q3.1"));
		values.add(sparqlQuery);
		values.add(xmlResult);
		WorkflowGenerationProvenanceCatalogConnector wgpc = this.getWgpcConnector();
		return wgpc.insertIntoTable(wgpc.getQuery3point1Table(), values);
	}

	/**
	 * since DC has indicated they will use a Java interface so I include a
	 * method for their Tripples and ResultSet a unique query id will be
	 * generated
	 * 
	 * @param triples
	 *            the triples we sent to DC
	 * @param dataObjectBindings
	 *            the data object bindings
	 * @return true iff successful
	 */

	public boolean addQuery3point1ToProvenanceCatalog(String seedId, ArrayList<KBTriple> triples, boolean returnPartialBindings,
			ArrayList<ArrayList<DataVariableDataObjectBinding>> result) {
		ArrayList<Object> values = new ArrayList<Object>(4);
		values.add(seedId);
		values.add(UuidGen.generateAUuid("q3.1"));
		StringBuilder query = new StringBuilder();
		query.append("triples=");
		query.append(triples);
		query.append(" ");
		query.append("returnPartialBindings=");
		query.append(returnPartialBindings);
		values.add(query.toString());
		values.add(result.toString());
		WorkflowGenerationProvenanceCatalogConnector wgpc = this.getWgpcConnector();
		return wgpc.insertIntoTable(wgpc.getQuery3point1Table(), values);
	}

	/**
	 * register query 4.5 to the provenance catalog a unique id will be
	 * generated
	 * 
	 * @param sentMapsAndRequirements
	 *            the component
	 * @param invocationCommand
	 *            the invocation command returned by AC
	 * @return true iff sucessful
	 */
	public boolean addQuery4point5ToProvenanceCatalog(String seedId, ComponentMapsAndRequirements sentMapsAndRequirements, String invocationCommand) {
		ArrayList<Object> values = new ArrayList<Object>(4);
		values.add(seedId);
		values.add(UuidGen.generateAUuid("q4.5"));
		StringBuilder query = new StringBuilder();
		query.append("component=");
		query.append(sentMapsAndRequirements.getComponent().getID());
		query.append(" ");
		query.append("inputVariableMaps=");
		query.append(sentMapsAndRequirements.getInputMaps());
		query.append(" ");
		query.append("outputVariableMaps=");
		query.append(sentMapsAndRequirements.getOutputMaps());
		query.append(" ");
		values.add(query.toString());
		values.add(invocationCommand);
		WorkflowGenerationProvenanceCatalogConnector wgpc = this.getWgpcConnector();
		return wgpc.insertIntoTable(wgpc.getQuery4point5Table(), values);
	}

	/**
	 * register query 4.5 to the provenance catalog a unique id will be
	 * generated
	 * 
	 * @param soapQuery
	 *            the soap query sent to AC
	 * @param invocationCommand
	 *            the invocation command return by AC
	 * @return true iff successful
	 */
	public boolean addQuery4point5ToProvenanceCatalog(String seedId, String soapQuery, String invocationCommand) {
		ArrayList<Object> values = new ArrayList<Object>(4);
		values.add(seedId);
		values.add(UuidGen.generateAUuid("q4.5"));
		values.add(soapQuery);
		values.add(invocationCommand);
		WorkflowGenerationProvenanceCatalogConnector wgpc = this.getWgpcConnector();
		return wgpc.insertIntoTable(wgpc.getQuery4point5Table(), values);
	}

	/**
	 * hmm
	 * 
	 * @param configuredWorkflows
	 *            fully specified templates
	 * @return true iff successful
	 */
	public boolean addConfiguredWorkflowsToProvenanceCatalog(String seedId, ArrayList<Template> configuredWorkflows, ArrayList<DAX> daxes) {
		WorkflowGenerationProvenanceCatalogConnector wgpc = this.getWgpcConnector();
		int i = 0;
		for (Template configuredWorkflow : configuredWorkflows) {
			DAX dax = daxes.get(i);
			ArrayList<Object> values = new ArrayList<Object>(5);
			values.add(seedId);
			values.add(configuredWorkflow.getID());
			values.add(dax.getID());
			// TODO: Write hasDataBinding/hasParameterValue into the template
			// values.add(fullySpecifiedTemplate.toRdf());
			values.add("not yet implemented");
			values.add(dax.getString());
			wgpc.insertIntoTable(wgpc.getTemplatesAndDaxesTable(), values);
			i++;
		}
		return true;
	}

	/**
	 * Q4.3
	 * <p/>
	 * Sets the predictive data metrics for a dataSourceId to an xml string
	 * inside the Predictive Data Catalog.
	 * <p/>
	 * 
	 * @param dataObjectId
	 *            a data object id
	 * @param xml
	 *            a string of xml describing the data source data
	 *            metrics/characteristics
	 * @return a boolean indicating success or failure
	 */
	public boolean setDataMetrics(String seedId, String dataObjectId, String xml) {
		boolean ret = true;
		String xml2 = getDataMetrics(dataObjectId);

		WorkflowGenerationProvenanceCatalogConnector wgpc = this.getWgpcConnector();
		if (xml2 == null && xml != null) {
			ArrayList<Object> values = new ArrayList<Object>(2);
			values.add(dataObjectId);
			values.add(wgpc.compress(xml));
			ret = wgpc.insertIntoTable(wgpc.getDataMetricsTable(), values);
		}

		// On the side, also insert data object ids into the seed_data table
		ArrayList<Object> values2 = new ArrayList<Object>(2);
		values2.add(seedId);
		values2.add(dataObjectId);
		return ret && wgpc.insertIntoTable("seed_data", values2);
	}

	/**
	 * Q4.4
	 * <p/>
	 * retrieves the data metrices/characteristics (xml string) for a data
	 * source id from the Predictive Data Catalog
	 * 
	 * @param dataObjectId
	 *            a data source id
	 * @return an xml string describing the data metrics/characteristics for a
	 *         data source id
	 */
	public String getDataMetrics(String dataObjectId) {
		WorkflowGenerationProvenanceCatalogConnector wgpc = this.getWgpcConnector();
		return wgpc.getDataMetrics(dataObjectId);
	}

	/**
	 * Map a Job in the DAX to the Componentid and the Component's Input/Output
	 * Parameters
	 * <p>
	 * Store Mappings of a JobID in the DAX to the Componentid and the
	 * Component's Input/Output Parameters as required by the AC
	 * <p>
	 * 
	 * @param daxId
	 *            the dax id/label
	 * @param jobId
	 *            the job id as written in the dax
	 * @param componentId
	 *            the component URI as defined in the AC
	 * @param inputMap
	 *            HashMap with (String)dataobject name <=> (String)input
	 *            parameter name mappings
	 * @param outputMap
	 *            HashMap with (String)dataobject name <=> (String)output
	 *            parameter name mappings
	 * @return a boolean indicating success or failure
	 */
	public boolean storeJobInformation(String seedId, String daxId, String jobId, ComponentMapsAndRequirements mapsAndRequirements) {
		WorkflowGenerationProvenanceCatalogConnector wgpc = this.getWgpcConnector();
		ArrayList<Object> values = new ArrayList<Object>(6);
		values.add(seedId);
		values.add(daxId);
		values.add(jobId);

		values.add(mapsAndRequirements.getComponent().getID());
		// values.add(serialize(mapsAndRequirements.getInputMaps()).toString());
		// values.add(serialize(mapsAndRequirements.getInputMaps()).toString());
		// wgpc.insertIntoTable("dax_component_argument_maps", values);

		wgpc.storeComponentInformation(seedId, daxId, jobId, mapsAndRequirements.getComponent().getID(), mapsAndRequirements.getComponent().getComponentType(),
				serialize(mapsAndRequirements.getInputMaps()), serialize(mapsAndRequirements.getOutputMaps()));
		return false;
	}

	/**
	 * Map a Job in the DAX to the Componentid and the Component's Input/Output
	 * Parameters
	 * <p>
	 * Get Mappings of a JobID in the DAX to the Componentid and the Component's
	 * Input/Output Parameters as required by the AC
	 * <p>
	 * 
	 * @param seedId
	 *            the seed id
	 * @param daxId
	 *            the dax id/label
	 * @param jobId
	 *            the job id as written in the dax
	 * @return an arraylist of {(String)ComponentId, (HashMap)inputMap,
	 *         (HashMap)outputMap
	 */
	public ComponentMapsAndRequirements getJobInformation(String seedId, String daxId, String jobId) {
		WorkflowGenerationProvenanceCatalogConnector wgpc = this.getWgpcConnector();
		ArrayList l = wgpc.getComponentInformation(seedId, daxId, jobId);
		if (l == null || l.size() == 0)
			return null;
		String componentId = (String) l.get(0);
		String componentType = (String) l.get(1);
		byte[] imap = (byte[]) l.get(2);
		byte[] omap = (byte[]) l.get(3);
		HashMap<Role, Variable> inputMaps = (HashMap<Role, Variable>) deserialize(imap);
		HashMap<Role, Variable> outputMaps = (HashMap<Role, Variable>) deserialize(omap);
		// TODO: Need to start using component-type everywhere than
		// component-ID.. Treat component-ID as skolems in the template !!!
		// TODO: The ABOVE IS AN IMPORTANT SR12 requirement

		ComponentVariable c = new ComponentVariable(componentId);
		c.setComponentType(componentType);
		ComponentMapsAndRequirements mapsAndRequirements = new ComponentMapsAndRequirements(c, inputMaps, outputMaps, new ArrayList<KBTriple>());
		return mapsAndRequirements;
	}

	private static byte[] serialize(Serializable obj) {
		byte[] byteArray = null;
		ByteArrayOutputStream baos = null;
		ObjectOutputStream out = null;
		try {
			// These objects are closed in the finally.
			baos = new ByteArrayOutputStream();
			out = new ObjectOutputStream(baos);
			out.writeObject(obj);
			byteArray = baos.toByteArray();
			out.flush();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return byteArray;
	}

	private static Serializable deserialize(byte[] byteArray) {
		ByteArrayInputStream bais = null;
		ObjectInputStream in = null;
		try {
			// These objects are closed in the finally.
			bais = new ByteArrayInputStream(byteArray);
			in = new ObjectInputStream(bais);
			return (Serializable) in.readObject();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Getter for property 'wgpcConnector'.
	 * 
	 * @return Value for property 'wgpcConnector'.
	 */
	public WorkflowGenerationProvenanceCatalogConnector getWgpcConnector() {
		return wgpcConnector;
	}

	/**
	 * Setter for property 'wgpcConnector'.
	 * 
	 * @param wgpcConnector
	 *            Value to set for property 'wgpcConnector'.
	 */
	public void setWgpcConnector(WorkflowGenerationProvenanceCatalogConnector wgpcConnector) {
		this.wgpcConnector = wgpcConnector;
	}
}
