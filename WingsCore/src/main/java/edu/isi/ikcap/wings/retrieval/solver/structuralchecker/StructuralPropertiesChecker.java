/**
 * 
 */
package edu.isi.ikcap.wings.retrieval.solver.structuralchecker;

import edu.isi.ikcap.wings.retrieval.KBTripleList;
import edu.isi.ikcap.wings.retrieval.MappingsList;
import edu.isi.ikcap.wings.workflows.template.Template;

/**
 * @author gonzalo
 * 
 */
public interface StructuralPropertiesChecker {

	float checkStructuralProperties(KBTripleList structuralConstraints, Template workflow, MappingsList mappings);

}
