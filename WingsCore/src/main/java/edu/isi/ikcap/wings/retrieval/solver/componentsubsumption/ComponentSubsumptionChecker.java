package edu.isi.ikcap.wings.retrieval.solver.componentsubsumption;

import edu.isi.ikcap.wings.catalogs.components.ComponentCatalog;
import edu.isi.ikcap.wings.retrieval.KBTripleList;
import edu.isi.ikcap.wings.retrieval.MappingsList;
import edu.isi.ikcap.wings.workflows.template.Template;

public interface ComponentSubsumptionChecker {

	float checkComponentsSubsumption(KBTripleList tripleList, Template currentWorkflow, MappingsList mappings, ComponentCatalog pc);

}
