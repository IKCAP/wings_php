package edu.isi.ikcap.wings.retrieval.solver.structuralchecker;

import edu.isi.ikcap.ontapi.KBObject;
import edu.isi.ikcap.ontapi.KBTriple;
import edu.isi.ikcap.wings.retrieval.KBTripleList;
import edu.isi.ikcap.wings.retrieval.MappingsList;
import edu.isi.ikcap.wings.retrieval.util.NamespaceManager;
import edu.isi.ikcap.wings.retrieval.wfc.WorkflowCatalogFactory;
import edu.isi.ikcap.wings.workflows.template.Template;

public class SPCAdHoc implements StructuralPropertiesChecker {

	public float checkStructuralProperties(KBTripleList structuralConstraints, Template workflow, MappingsList mappings) {
		for (KBTriple triple : structuralConstraints) {
			String predicateNamespace = triple.getPredicate().getNamespace();
			if (predicateNamespace.equals(NamespaceManager.getNamespace("wfq"))) {
				String predicateName = triple.getPredicate().getName();
				KBObject mappedSubject = mappings.getEntity(triple.getSubject());
				KBObject mappedObject = mappings.getEntity(triple.getObject());
				if (predicateName.equals("precedes")) {
					if (!checkPrecedes(mappedSubject, mappedObject, workflow))
						return 0f;
				} else if (predicateName.equals("inmediatelyPrecedes")) {
					if (!checkInmediatelyPrecedes(mappedSubject, mappedObject, workflow))
						return 0f;
				} else if (predicateName.equals("datapointPrecedes")) {
					if (!checkDatapointPrecedes(mappedSubject, mappedObject, workflow))
						return 0f;
				} else if (predicateName.equals("datapointInmediatelyPrecedes")) {
					if (!checkDatapointInmediatelyPrecedes(mappedSubject, mappedObject, workflow))
						return 0f;
				} else if (predicateName.equals("hasInputData")) {
					if (!checkInputData(mappedSubject, mappedObject, workflow))
						return 0f;
				} else if (predicateName.equals("hasOutputData")) {
					if (!checkOutputData(mappedSubject, mappedObject, workflow))
						return 0f;
				} else {
					// TODO Store the unused component constraints to notify the
					// user or something
				}
			}
		}
		return 1.0f;
	}

	private boolean checkOutputData(KBObject mappedSubject, KBObject mappedObject, Template workflow) {
		return WorkflowCatalogFactory.getCatalog().checkOutputData(workflow, mappedSubject, mappedObject);
	}

	private boolean checkInputData(KBObject mappedSubject, KBObject mappedObject, Template workflow) {
		return WorkflowCatalogFactory.getCatalog().checkInputData(workflow, mappedSubject, mappedObject);
	}

	private boolean checkDatapointInmediatelyPrecedes(KBObject mappedSubject, KBObject mappedObject, Template workflow) {
		return WorkflowCatalogFactory.getCatalog().checkDatapointInmediatelyPrecedes(workflow, mappedSubject, mappedObject);
	}

	private boolean checkDatapointPrecedes(KBObject mappedSubject, KBObject mappedObject, Template workflow) {
		return WorkflowCatalogFactory.getCatalog().checkDatapointPrecedes(workflow, mappedSubject, mappedObject);
	}

	private boolean checkInmediatelyPrecedes(KBObject mappedSubject, KBObject mappedObject, Template workflow) {
		return WorkflowCatalogFactory.getCatalog().checkComponentInmediatelyPrecedes(workflow, mappedSubject, mappedObject);
	}

	private boolean checkPrecedes(KBObject mappedSubject, KBObject mappedObject, Template workflow) {
		return WorkflowCatalogFactory.getCatalog().checkComponentPrecedes(workflow, mappedSubject, mappedObject);
	}

}
