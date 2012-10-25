package edu.isi.ikcap.wings.retrieval.wfc;

import java.util.ArrayList;

import edu.isi.ikcap.ontapi.KBObject;
import edu.isi.ikcap.wings.workflows.template.Seed;
import edu.isi.ikcap.wings.workflows.template.Template;
import edu.isi.ikcap.wings.workflows.template.variables.ComponentVariable;

//TODO Add methods to the implementations so we could check if one implementation is adequate to be used for a workflow 

public interface WorkflowCatalog {
	// /**
	// * <p>Given a list of requirements for the workflows, in the form of a
	// list of triples, the method should return all the
	// * workflows in the catalog that follow the requirements.
	// * <p>The requirements are a list of triples of the form (wfVariable,
	// relation, value). Note that we are always
	// * considering that we cannot compare two workflows, so the workflow
	// variable (wfVariable) is supposed to refer always to
	// * the same workflow.
	// * @param requirements a list of triples of the form (wfVariable,
	// relation, value) that express
	// * the requirements on the retrieved workflows
	// * @return list of workflow templates and seeds that conform to all the
	// requirements
	// */
	// ArrayList<Template> findWorkflows(ArrayList<KBTriple> requirements);

	/**
	 * <p>
	 * Returns all the workflows in the workflow catalog (templates and seeds)
	 */
	ArrayList<Template> getAllWorkflows();

	public ArrayList<Template> getAllTemplates();

	public ArrayList<Seed> getAllSeeds();

	public ArrayList<? extends KBObject> getComponents(Template t);

	public ArrayList<? extends KBObject> getDatasets(Template t);

	public ArrayList<? extends KBObject> getInputDatasets(Template t);

	public ArrayList<? extends KBObject> getOutputDatasets(Template t);

	ArrayList<? extends KBObject> getParameters(Template t);

	ArrayList<? extends KBObject> getIntermediateDatasets(Template t);

	ComponentVariable getComponent(Template template, KBObject mappedEntity);

	boolean checkComponentInmediatelyPrecedes(Template workflow, KBObject precedingComponent, KBObject precededComponent);

	boolean checkDatapointInmediatelyPrecedes(Template workflow, KBObject precedingDatapoint, KBObject precededDatapoint);

	boolean checkInputData(Template workflow, KBObject component, KBObject data);

	boolean checkOutputData(Template workflow, KBObject component, KBObject data);

	boolean checkDatapointPrecedes(Template workflow, KBObject precedingDatapoint, KBObject precededDatapoint);

	boolean checkComponentPrecedes(Template workflow, KBObject precedingComponent, KBObject precededComponent);

	public Template getWorkflow(String name);

	public Template getTemplate(String name);

	public Seed getSeed(String name);

}
