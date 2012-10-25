package edu.isi.ikcap.wings.retrieval.solver.doublemappingremover;

import edu.isi.ikcap.wings.retrieval.MappingsListsList;

public interface DoubleMappingRemover {

	MappingsListsList removeDoubleMappings(MappingsListsList currentWorkflowMappings);

}
