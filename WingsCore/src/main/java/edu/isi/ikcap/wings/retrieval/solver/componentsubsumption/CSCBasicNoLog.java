/**
 * 
 */
package edu.isi.ikcap.wings.retrieval.solver.componentsubsumption;

import edu.isi.ikcap.ontapi.KBObject;
import edu.isi.ikcap.ontapi.KBTriple;
import edu.isi.ikcap.wings.catalogs.components.ComponentCatalog;
import edu.isi.ikcap.wings.retrieval.KBTripleList;
import edu.isi.ikcap.wings.retrieval.MappingsList;
import edu.isi.ikcap.wings.retrieval.util.NamespaceManager;
import edu.isi.ikcap.wings.retrieval.wfc.WorkflowCatalogFactory;
import edu.isi.ikcap.wings.workflows.template.Template;

/**
 * @author gonzalo
 * 
 */
public class CSCBasicNoLog implements ComponentSubsumptionChecker {

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.isi.ikcap.wings.retrieval.ComponentSubsumptionChecker#
	 * checkComponentsSubsumption
	 * (edu.isi.ikcap.wings.retrieval.KBTripleList,
	 * edu.isi.ikcap.wings.workflows.template.Template,
	 * edu.isi.ikcap.wings.retrieval.MappingsList)
	 */
	public float checkComponentsSubsumption(KBTripleList tripleList, Template currentWorkflow, MappingsList mappings, ComponentCatalog pc) {
		for (KBTriple triple : tripleList) {
			// Query: get the class of the current query variable
			if (triple.getPredicate().getNamespace().equals(NamespaceManager.getNamespace("wfq")) && triple.getPredicate().getName().equals("subClassOf")) {
				String queryClass = triple.getObject().getID();
				KBObject queryVariable = triple.getSubject();
				KBObject mappedEntity = mappings.getEntity(queryVariable);
				if (mappedEntity == null) {
					// TODO notify the user that the mapping is missing
					continue;
				}
				// WS: Get the class of the current mapped variable
				String mappedEntityClass = WorkflowCatalogFactory.getCatalog().getComponent(currentWorkflow, mappedEntity).getComponentType();
				// CS: Check subsumption (mapped class is subclass of query
				// class)
				if (!pc.componentSubsumes(queryClass, mappedEntityClass)) {
					// TODO We can add the constraint to a log so the user can
					// check why a workflow wasn't retrieved
					return 0f;
				}
			} else {
				// TODO Store the unused component constraints to notify the
				// user or something
			}
		}
		return 1f;
	}

}
