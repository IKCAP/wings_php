package edu.isi.ikcap.wings.retrieval.solver;

import java.util.HashMap;

import edu.isi.ikcap.wings.catalogs.components.ComponentCatalog;
import edu.isi.ikcap.wings.catalogs.data.DataCatalog;
import edu.isi.ikcap.wings.retrieval.KBTripleList;
import edu.isi.ikcap.wings.retrieval.MappingsList;
import edu.isi.ikcap.wings.retrieval.MappingsListsList;
import edu.isi.ikcap.wings.retrieval.Query;
import edu.isi.ikcap.wings.workflows.template.Template;

public interface RetrievalSolver {

	public enum SubsumptionDegree {
		EQUIVALENT, SUBSUMES, SUBSUMED, INTERSECTS, DISJOINT;

		public SubsumptionDegree updatedValue(SubsumptionDegree update) {
			if ((this == SUBSUMES && update == SUBSUMED) || (this == SUBSUMED && update == SUBSUMES)) {
				return INTERSECTS;
			} else {
				return min(update);
			}
		}

		public SubsumptionDegree min(SubsumptionDegree check) {
			if (this.compareTo(check) > 0)
				return this;
			else
				return check;
		}

		public SubsumptionDegree max(SubsumptionDegree check) {
			if (this.compareTo(check) < 0)
				return check;
			else
				return this;
		}
	}

	public enum JenaFiltersEnum {
		WORKFLOW_DESIGN_PROPERTIES, STRUCTURAL_PROPERTIES, COMPONENT_PROPERTIES, DATA_PROPERTIES, OTHER_PROPERTIES
	}

	HashMap<JenaFiltersEnum, KBTripleList> classifyConstraints(Query query);

	MappingsListsList getInitialMappings(Template currentWorkflow, KBTripleList constraints);

	MappingsListsList removeDoubleMappings(MappingsListsList currentWorkflowMappings);

	float checkComponentsSubsumption(Template currentWorkflow, KBTripleList componentConstraints, MappingsList mappings, ComponentCatalog pc);

	float checkStructuralProperties(Template workflow, KBTripleList structuralConstraints, MappingsList mappings);

	SubsumptionDegree checkDataVariablesSubsumption(Template currentWorkflow, KBTripleList dataConstraints, MappingsList mappings, DataCatalog dc);

}
