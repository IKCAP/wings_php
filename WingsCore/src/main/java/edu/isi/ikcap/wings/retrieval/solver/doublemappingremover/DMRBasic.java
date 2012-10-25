/**
 * 
 */
package edu.isi.ikcap.wings.retrieval.solver.doublemappingremover;

import edu.isi.ikcap.wings.retrieval.MappingsListsList;

/**
 * @author gonzalo
 * 
 */
public class DMRBasic implements DoubleMappingRemover {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.isi.ikcap.wings.retrieval.DoubleMappingRemover#removeDoubleMappings
	 * (edu.isi.ikcap.wings.retrieval.MappingsListsList)
	 */
	public MappingsListsList removeDoubleMappings(MappingsListsList currentWorkflowMappings) {
		MappingsListsList cpMappings = new MappingsListsList(currentWorkflowMappings);
		cpMappings.removeDoubleMappings();
		return cpMappings;
	}

}
