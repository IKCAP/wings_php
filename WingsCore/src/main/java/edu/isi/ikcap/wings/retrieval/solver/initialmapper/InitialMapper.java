package edu.isi.ikcap.wings.retrieval.solver.initialmapper;

import edu.isi.ikcap.wings.retrieval.KBTripleList;
import edu.isi.ikcap.wings.retrieval.MappingsListsList;
import edu.isi.ikcap.wings.workflows.template.Template;

public interface InitialMapper {

	MappingsListsList getInitialMappings(Template currentWorkflow, KBTripleList tripleList);

}
