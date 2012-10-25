package edu.isi.ikcap.wings.catalogs.components;

import edu.isi.ikcap.wings.catalogs.components.classes.ComponentMapsAndRequirements;
import edu.isi.ikcap.wings.catalogs.components.classes.TransformationCharacteristics;
import edu.isi.ikcap.wings.workflows.template.Role;
import edu.isi.ikcap.wings.workflows.template.variables.ComponentVariable;


import java.util.List;
import java.util.ArrayList;
import java.util.Properties;

public interface ComponentCatalog {

	/**
	 * A connect method that connects to the catalog using properties.
	 * 
	 * @param properties
	 *            the properties to use for connection.
	 * 
	 * @return boolean
	 */
	public boolean connect(Properties properties);

	/**
	 * Close the connection to the Data Characterization service
	 */
	public void close();

	/**
	 * <b>Query 2.1</b><br/>
	 * Get a list of Concrete Components with their IO Data Requirements given a
	 * component (maybe abstract) and it's IO Data Requirements
	 * 
	 * @param mapsAndConstraints
	 *            A ComponentMapsAndRequirements Object which contains:
	 *            <ul>
	 *            <li>component,
	 *            <li>maps of component input arguments to template variables,
	 *            <li>maps of component output arguments to template variables,
	 *            <li>template variable descriptions (dods) - list of triples
	 *            </ul>
	 * @return list of ComponentMapsAndRequirements Objects for each specialized
	 *         component
	 */
	public ArrayList<ComponentMapsAndRequirements> specializeAndFindDataRequirements(ComponentMapsAndRequirements mapsAndConstraints);

	/**
	 * <b>Query 2.1b</b><br/>
	 * Get inferred IO Data Requirements given a component (maybe abstract) and
	 * it's given IO Data Requirements
	 * 
	 * @param mapsAndConstraints
	 *            A ComponentMapsAndRequirements Object which contains:
	 *            <ul>
	 *            <li>component,
	 *            <li>maps of component input arguments to template variables,
	 *            <li>maps of component output arguments to template variables,
	 *            <li>template variable descriptions (dods) - list of triples
	 *            </ul>
	 * @return ComponentMapsAndRequirements Object
	 */
	public ComponentMapsAndRequirements findDataRequirements(ComponentMapsAndRequirements mapsAndConstraints);

	/**
	 * <b>Query 2.1c</b><br/>
	 * Get inferred IO Data Requirements (Types only) given a component (maybe
	 * abstract) and it's given IO Data Requirements
	 * 
	 * @param mapsAndConstraints
	 *            A ComponentMapsAndRequirements Object which contains:
	 *            <ul>
	 *            <li>component,
	 *            <li>maps of component input arguments to template variables,
	 *            <li>maps of component output arguments to template variables,
	 *            <li>template variable descriptions (dods) - list of triples
	 *            </ul>
	 * @return ComponentMapsAndRequirements Object
	 */
	public ComponentMapsAndRequirements findDataTypeRequirements(ComponentMapsAndRequirements mapsAndConstraints);

	/**
	 * <b>Query 4.2</b><br/>
	 * This function is supposed to <b>SET</b> the DataSet Metrics, or Parameter
	 * Values for the Variables that are passed in via the input/output maps as
	 * part of mapsAndConstraints.<br/>
	 * Variables will already be bound to dataObjects, so the function will have
	 * to do something like the following :
	 * 
	 * <pre>
	 * If Variable.isParameterVariable() Variable.setParameterValue(value)
	 * If Variable.isDataVariable() Variable.getDataObjectBinding().setDataMetrics(xml)
	 * </pre>
	 * 
	 * @param mapsAndConstraints
	 *            A ComponentMapsAndRequirements Object which contains:
	 *            <ul>
	 *            <li>component,
	 *            <li>maps of component input arguments to template variables,
	 *            <li>maps of component output arguments to template variables,
	 *            <li>template variable descriptions (dods) - list of triples
	 *            </ul>
	 * @return List of extra template variable descriptions (will mostly be
	 *         empty in Q4.2 though)
	 */
	public ArrayList<ComponentMapsAndRequirements> findOutputDataPredictedDescriptions(ComponentMapsAndRequirements mapsAndConstraints);

	/**
	 * <b>Query 4.5</b><br/>
	 * Given a component, it's mapping of arguments to template variables.
	 * Assumption is that the all Parameter Variables have values, and all Data
	 * Variables have DataObject Bindings with metrics (if any)
	 * 
	 * @param mapsAndConstraints
	 *            A ComponentMapsAndRequirements Object which contains:
	 *            <ul>
	 *            <li>component,
	 *            <li>maps of component input arguments to template variables,
	 *            <li>maps of component output arguments to template variables,
	 *            <li>template variable descriptions (dods) - list of triples
	 *            </ul>
	 * @return String The Invocation String that is used in the DAX by pegasus
	 */
	public String getInvocationCommand(ComponentMapsAndRequirements mapsAndConstraints);

	/**
	 * <b>Query 5.2</b><br/>
	 * 
	 * Returns the predicted performance on a transformation/component on a
	 * particular site or architecture.
	 * 
	 * @param mapsAndConstraints
	 *            A ComponentMapsAndRequirements Object which contains:
	 *            <ul>
	 *            <li>component,
	 *            <li>maps of component input arguments to template variables,
	 *            <li>maps of component output arguments to template variables,
	 *            <li>template variable descriptions (dods) - list of triples
	 *            </ul>
	 * @param site
	 *            the site handle for which the predicted performance is
	 *            required.
	 * @param architecture
	 *            the architecture type.
	 * 
	 * @return List of TransformationCharacteristics containing the predicted
	 *         performance.
	 */
	public List<TransformationCharacteristics> getPredictedPerformance(ComponentMapsAndRequirements mapsAndConstraints, String site, String architecture);

	/**
	 * <b>Query 7.1</b><br/>
	 * 
	 * Returns the candidate installations for a component.
	 * 
	 * TO ASK VARUN, WHETHER COMPONENT OR JOBNAME
	 * 
	 * @param c
	 *            the component
	 * 
	 * @return List of TransformationCharacteristics containing the predicted
	 *         performance.
	 */
	public List<TransformationCharacteristics> findCandidateInstallations(ComponentVariable c);

	/**
	 * <b>Query 7.2</b><br/>
	 * 
	 * Returns the deployment requirements of a component on a site.
	 * 
	 * TO ASK VARUN, WHETHER COMPONENT OR JOBNAME
	 * 
	 * @param c
	 *            the component
	 * @param site
	 *            the site .
	 * 
	 * @return List of TransformationCharacteristics containing the predicted
	 *         performance.
	 */
	public List<TransformationCharacteristics> getDeploymentRequirements(ComponentVariable c, String site);

	public void setRequestId(String id);

	/**
	 * 
	 * Template Creation API queries below
	 * 
	 */

	// Flat list for now
	public ArrayList<ComponentVariable> getAllComponentTypes();

	// Return null if component doesn't exist
	public ArrayList<Role> getComponentInputs(ComponentVariable c);

	// Return null if component doesn't exist
	public ArrayList<Role> getComponentOutputs(ComponentVariable c);

	public boolean componentSubsumes(String subsumerClassID, String subsumedClassID);

}
