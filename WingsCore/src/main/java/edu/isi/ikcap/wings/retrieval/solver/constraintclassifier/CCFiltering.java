package edu.isi.ikcap.wings.retrieval.solver.constraintclassifier;

import java.util.ArrayList;
import java.util.HashMap;

import edu.isi.ikcap.ontapi.KBObject;
import edu.isi.ikcap.ontapi.KBTriple;
import edu.isi.ikcap.ontapi.OntFactory;
import edu.isi.ikcap.wings.retrieval.ConstraintsFilter;
import edu.isi.ikcap.wings.retrieval.KBTripleList;
import edu.isi.ikcap.wings.retrieval.Query;
import edu.isi.ikcap.wings.retrieval.solver.RetrievalSolver.JenaFiltersEnum;
import edu.isi.ikcap.wings.retrieval.util.NamespaceManager;

public class CCFiltering implements ConstraintClassifier {

	// public static final String WFQ = "http://wings-workflows.org/ontology/workflowQueries#";
	// public static final String WFQV =
	// "http://wings-workflows.org/ontology/workflowQueries/Variable#";
	// public static final String WF =
	// "http://wings-workflows.org/ontology/2007/08/workflow.owl#";
	// public static final String ACDM =
	// "http://wings-workflows.org/ontology/ac/dm/library.owl#";
	// public static final String DCDM =
	// "http://wings-workflows.org/ontology/dc/dm/ontology.owl#";

	public static final String[] WORKFLOW_DESIGN_PROPERTIES_LIST = { "hasComponent", "hasInputDataset", "hasOutputDataset", "hasIntermediateDataset",
			"hasDataset", "hasInputParameter" };

	public static final String[] STRUCTURAL_PROPERTIES_LIST = { "hasInputData", "hasOutputData", "hasComponentParameter", "precedes", "inmediatelyPrecedes",
			"datapointPrecedes", "datapointInmediatelyPrecedes" };
	
	private static OntFactory ontologyFactory = new OntFactory(OntFactory.JENA);

	public HashMap<JenaFiltersEnum, KBTripleList> classifyConstraints(Query query) {
		ArrayList<KBTripleList> filteredConstraints = ConstraintsFilter.filterConstraintsAny(query.getQueryConstraints(), CCFiltering.getJenaFilters(query
				.getWorkflowVariable()));

		HashMap<JenaFiltersEnum, KBTripleList> output = new HashMap<JenaFiltersEnum, KBTripleList>();
		for (JenaFiltersEnum i : JenaFiltersEnum.values()) {
			output.put(i, filteredConstraints.get(i.ordinal()));
		}
		return output;
	}

	public static ArrayList<KBTripleList> getJenaFilters(String workflowVariable) {
		// ArrayList filters = new ArrayList();
		ArrayList<KBTripleList> filters = new ArrayList<KBTripleList>();
		filters.add(JenaFiltersEnum.WORKFLOW_DESIGN_PROPERTIES.ordinal(), CCFiltering.getJenaWorkflowDesignPropertiesFilters(workflowVariable));
		filters.add(JenaFiltersEnum.STRUCTURAL_PROPERTIES.ordinal(), CCFiltering.getJenaStructuralPropertiesFilters());
		filters.add(JenaFiltersEnum.COMPONENT_PROPERTIES.ordinal(), CCFiltering.getJenaComponentPropertiesFilters(workflowVariable));
		filters.add(JenaFiltersEnum.DATA_PROPERTIES.ordinal(), CCFiltering.getJenaDataPropertiesFilters(workflowVariable));
		return filters;
	}

	public static KBTripleList getJenaDataPropertiesFilters(String workflowVariable) {
		KBTripleList output = new KBTripleList();
		KBTriple triple;
		// x wfq:hasType x
		triple = ontologyFactory.getTriple(null, getResourceFromID(NamespaceManager.getNamespace("wfq"), "hasType"), null);
		output.add(triple);
		// x wf:hasDataBinding x
		triple = ontologyFactory.getTriple(null, getResourceFromID(NamespaceManager.getNamespace("wflow"), "hasDataBinding"), null);
		// x wf:hasDataBinding x
		triple = ontologyFactory.getTriple(null, getResourceFromID(NamespaceManager.getNamespace("wflow"), "canBeBound"), null);
		output.add(triple);
		// All triples with domain dcdm
		KBObject resource = getResourceFromID(NamespaceManager.getNamespace("dcdom"), ConstraintsFilter.WILDCARD);
		triple = ontologyFactory.getTriple(null, resource, null);
		output.add(triple);

		return output;
	}

	public static KBTripleList getJenaComponentPropertiesFilters(String workflowVariable) {
		KBTripleList output = new KBTripleList();
		KBTriple triple;
		// x wfq:subClassOf x
		triple = ontologyFactory.getTriple(null, getResourceFromID(NamespaceManager.getNamespace("wfq"), "subClassOf"), null);
		output.add(triple);
		// All triples with domain acdm
		KBObject resource = getResourceFromID(NamespaceManager.getNamespace("acdom"), ConstraintsFilter.WILDCARD);
		triple = ontologyFactory.getTriple(null, resource, null);
		output.add(triple);
		return output;
	}

	public static KBTripleList getJenaStructuralPropertiesFilters() {
		KBTripleList output = new KBTripleList();
		KBTriple triple;
		for (int i = 0; i < STRUCTURAL_PROPERTIES_LIST.length; i++) {
			triple = ontologyFactory.getTriple(null, getResourceFromID(NamespaceManager.getNamespace("wfq"), STRUCTURAL_PROPERTIES_LIST[i]), null);
			output.add(triple);
		}
		return output;
	}

	/**
	 * <p>
	 * Returns the filters to retrieve the workflow design properties from a
	 * list of constraints for the Jena implementation of the KBAPI
	 * 
	 * TODO this should be in some other place (and maybe implement an interface
	 * for each implementation)
	 * 
	 * @return
	 */
	public static KBTripleList getJenaWorkflowDesignPropertiesFilters(String workflowVariable) {
		KBTripleList output = new KBTripleList();
		KBTriple triple;
		KBObject workflowVariableObj = getResourceFromID(NamespaceManager.getNamespace("wfqv"), workflowVariable);
		for (int i = 0; i < WORKFLOW_DESIGN_PROPERTIES_LIST.length; i++) {
			triple = ontologyFactory.getTriple(workflowVariableObj, getResourceFromID(NamespaceManager.getNamespace("wfq"),
					WORKFLOW_DESIGN_PROPERTIES_LIST[i]), null);
			output.add(triple);
		}
		return output;
	}

	
	private static KBObject getResourceFromID(String defaultDomain, String varID) {
		int pos = varID.indexOf("#");
		if (pos < 0) {
			varID = defaultDomain + "#" + varID;
		}
		return ontologyFactory.getObject(varID);
	}
	
}
