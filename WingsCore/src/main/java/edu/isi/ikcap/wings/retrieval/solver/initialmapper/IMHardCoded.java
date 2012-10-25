package edu.isi.ikcap.wings.retrieval.solver.initialmapper;

import edu.isi.ikcap.ontapi.KBObject;
import edu.isi.ikcap.ontapi.KBTriple;
import edu.isi.ikcap.wings.retrieval.KBTripleList;
import edu.isi.ikcap.wings.retrieval.MappingsListsList;
import edu.isi.ikcap.wings.retrieval.util.NamespaceManager;
import edu.isi.ikcap.wings.retrieval.wfc.WorkflowCatalog;
import edu.isi.ikcap.wings.retrieval.wfc.WorkflowCatalogFactory;
import edu.isi.ikcap.wings.workflows.template.Template;

public class IMHardCoded implements InitialMapper {

	public MappingsListsList getInitialMappings(Template workflow, KBTripleList constraints) {
		WorkflowCatalog wc = WorkflowCatalogFactory.getCatalog();
		MappingsListsList output = new MappingsListsList();
		for (KBTriple constraint : constraints) {
			KBObject relation = constraint.getPredicate();
			KBObject variable = constraint.getObject();
			if (relation.getNamespace().equals(NamespaceManager.getNamespace("wfq"))) {
				if (relation.getName().equals("hasComponent")) {
					output.addMapping(variable, wc.getComponents(workflow));
				} else if (relation.getName().equals("hasInputDataset")) {
					output.addMapping(variable, wc.getInputDatasets(workflow));
				} else if (relation.getName().equals("hasOutputDataset")) {
					output.addMapping(variable, wc.getOutputDatasets(workflow));
				} else if (relation.getName().equals("hasIntermediateDataset")) {
					output.addMapping(variable, wc.getIntermediateDatasets(workflow));
				} else if (relation.getName().equals("hasDataset")) {
					output.addMapping(variable, wc.getDatasets(workflow));
				} else if (relation.getName().equals("hasInputParameter")) {
					output.addMapping(variable, wc.getParameters(workflow));
				}
			}
		}
		return output;
	}

}
