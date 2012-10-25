package edu.isi.ikcap.wings.retrieval.solver;

import java.util.HashMap;

import edu.isi.ikcap.wings.catalogs.components.ComponentCatalog;
import edu.isi.ikcap.wings.catalogs.data.DataCatalog;
import edu.isi.ikcap.wings.retrieval.KBTripleList;
import edu.isi.ikcap.wings.retrieval.MappingsList;
import edu.isi.ikcap.wings.retrieval.MappingsListsList;
import edu.isi.ikcap.wings.retrieval.Query;
import edu.isi.ikcap.wings.retrieval.solver.componentsubsumption.ComponentSubsumptionChecker;
import edu.isi.ikcap.wings.retrieval.solver.constraintclassifier.ConstraintClassifier;
import edu.isi.ikcap.wings.retrieval.solver.datasubsumption.DataSubsumptionChecker;
import edu.isi.ikcap.wings.retrieval.solver.doublemappingremover.DoubleMappingRemover;
import edu.isi.ikcap.wings.retrieval.solver.initialmapper.InitialMapper;
import edu.isi.ikcap.wings.retrieval.solver.structuralchecker.StructuralPropertiesChecker;
import edu.isi.ikcap.wings.workflows.template.Template;

public class CompoundRetrievalSolver implements RetrievalSolver {

	ConstraintClassifier constraintClassifier = null;
	InitialMapper initialMapper = null;
	DoubleMappingRemover doubleMappingRemover = null;
	ComponentSubsumptionChecker componentSubsumptionChecker = null;
	StructuralPropertiesChecker structuralPropertiesChecker = null;
	DataSubsumptionChecker dataSubsumptionChecker = null;

	public CompoundRetrievalSolver(ConstraintClassifier constraintClassifier, InitialMapper initialMapper, DoubleMappingRemover doubleMappingRemover,
			ComponentSubsumptionChecker componentSubsumptionChecker, StructuralPropertiesChecker structuralPropertiesChecker,
			DataSubsumptionChecker dataSubsumptionChecker) {
		this.constraintClassifier = constraintClassifier;
		this.initialMapper = initialMapper;
		this.doubleMappingRemover = doubleMappingRemover;
		this.componentSubsumptionChecker = componentSubsumptionChecker;
		this.structuralPropertiesChecker = structuralPropertiesChecker;
		this.dataSubsumptionChecker = dataSubsumptionChecker;
	}

	public HashMap<JenaFiltersEnum, KBTripleList> classifyConstraints(Query query) {
		return constraintClassifier.classifyConstraints(query);
	}

	public MappingsListsList getInitialMappings(Template currentWorkflow, KBTripleList tripleList) {
		return initialMapper.getInitialMappings(currentWorkflow, tripleList);
	}

	public MappingsListsList removeDoubleMappings(MappingsListsList currentWorkflowMappings) {
		return doubleMappingRemover.removeDoubleMappings(currentWorkflowMappings);
	}

	public float checkComponentsSubsumption(Template currentWorkflow, KBTripleList componentConstraints, MappingsList mappings, ComponentCatalog pc) {
		return componentSubsumptionChecker.checkComponentsSubsumption(componentConstraints, currentWorkflow, mappings, pc);
	}

	public float checkStructuralProperties(Template workflow, KBTripleList structuralConstraints, MappingsList mappings) {
		return structuralPropertiesChecker.checkStructuralProperties(structuralConstraints, workflow, mappings);
	}

	public SubsumptionDegree checkDataVariablesSubsumption(Template currentWorkflow, KBTripleList dataConstraints, MappingsList mappings,
			DataCatalog dc) {
		return dataSubsumptionChecker.checkDataSubsumption(currentWorkflow, dataConstraints, mappings, dc);
	}

}
