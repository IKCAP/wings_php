package edu.isi.ikcap.wings.retrieval.solver.datasubsumption;

import edu.isi.ikcap.wings.catalogs.data.DataCatalog;
import edu.isi.ikcap.wings.retrieval.KBTripleList;
import edu.isi.ikcap.wings.retrieval.MappingsList;
import edu.isi.ikcap.wings.retrieval.solver.RetrievalSolver.SubsumptionDegree;
import edu.isi.ikcap.wings.workflows.template.Template;

public interface DataSubsumptionChecker {

	SubsumptionDegree checkDataSubsumption(Template currentWorkflow, KBTripleList dataConstraints, MappingsList mappings, DataCatalog dc);

}
