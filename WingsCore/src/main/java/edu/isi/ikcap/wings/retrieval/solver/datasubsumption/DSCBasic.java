package edu.isi.ikcap.wings.retrieval.solver.datasubsumption;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import edu.isi.ikcap.ontapi.KBObject;
import edu.isi.ikcap.ontapi.KBTriple;
import edu.isi.ikcap.wings.catalogs.data.DataCatalog;
import edu.isi.ikcap.wings.retrieval.KBTripleList;
import edu.isi.ikcap.wings.retrieval.MappingsList;
import edu.isi.ikcap.wings.retrieval.solver.RetrievalSolver.SubsumptionDegree;
import edu.isi.ikcap.wings.retrieval.util.NamespaceManager;
import edu.isi.ikcap.wings.workflows.template.ConstraintEngine;
import edu.isi.ikcap.wings.workflows.template.Template;
import edu.isi.ikcap.wings.workflows.util.PropertiesHelper;

public class DSCBasic implements DataSubsumptionChecker {

	public SubsumptionDegree checkDataSubsumption(Template workflow, KBTripleList dataConstraints, MappingsList mappings, DataCatalog dc) {

		SubsumptionDegree result = SubsumptionDegree.EQUIVALENT;
		// TODO create a classification property hasType so we could treat the
		// type as the other properties
		// Note: Maybe it's not so good idea, because all properties' subjects
		// are instances except hasType's
		// Separate the constraints for each query variable
		HashMap<KBObject, KBTripleList> queryVariableConstraints = new HashMap<KBObject, KBTripleList>();
		for (KBTriple dataConstraint : dataConstraints) {
			KBTripleList listForDataConstraint;
			if (queryVariableConstraints.containsKey(dataConstraint.getSubject())) {
				listForDataConstraint = queryVariableConstraints.get(dataConstraint.getSubject());
			} else {
				listForDataConstraint = new KBTripleList();
			}
			// Check if the constraint is "canBeBound"
			if (dataConstraint.getPredicate().getID().equals(NamespaceManager.getNamespace("wflow") + "canBeBound")) {
				// If so, expand it to the properties of the dataset
				Collection<KBTriple> expandedConstraints = dc.getDatasetClassificationProperties(dataConstraint);
				listForDataConstraint.addAll(expandedConstraints);
			} else {
				listForDataConstraint.add(dataConstraint);
			}
			queryVariableConstraints.put(dataConstraint.getSubject(), listForDataConstraint);
		}
		ConstraintEngine cnsEng = workflow.getConstraintEngine();
		ArrayList<KBTriple> constraints = cnsEng.getConstraints();
		// For each query variable with data constraints
		for (Entry<KBObject, KBTripleList> entry : queryVariableConstraints.entrySet()) {
			// Get mapped variable
			KBObject mappedVariable = mappings.getEntity(entry.getKey());
			if (mappedVariable == null) {
				// TODO notify the user that the mapping is missing
				continue;
			}
			// Variable var = workflow.getVariable(mappedVariable.getID());

			// Get Classification properties from the data variable
			// TODO the method getCostraints(String) didn't work as I expected
			// so I am doing it manually
			// ArrayList<KBTriple> varConstraints =
			// cnsEng.getConstraints(var.getID());
			ArrayList<KBTriple> varConstraints = new ArrayList<KBTriple>();
			for (KBTriple triple : constraints) {
				if (triple.getSubject().getID().equals(mappedVariable.getID()))
					varConstraints.add(triple);
			}

			HashMap<String, ArrayList<KBTriple>> varClassificationProperties = filterClassificationProperties(dc, varConstraints);

			// Get Classification properties from the query variable
			KBTripleList queryConstraints = entry.getValue();
			HashMap<String, ArrayList<KBTriple>> queryClassificationProperties = filterClassificationProperties(dc, queryConstraints);

			// SubsumptionDegree currentResult = SubsumptionDegree.EQUIVALENT;
			// // For each property in the query
			// for (Entry<String, KBTriple> queryEntry :
			// queryClassificationProperties.entrySet()) {
			// String propertyID = queryEntry.getKey();
			// if (varClassificationProperties.containsKey(propertyID)) {
			// KBObject queryValue = queryEntry.getValue().getObject();
			// KBObject varValue =
			// varClassificationProperties.get(propertyID).getObject();
			// if (queryValue.isLiteral()) {
			// if (!queryValue.equals(varValue))
			// return SubsumptionDegree.DISJOINT;
			// } else {
			// SubsumptionDegree sd = dc.checkSubsumtion(propertyID,
			// queryValue.getID(), varValue.getID());
			// currentResult = currentResult.updatedValue(sd);
			// }
			// } else {
			// currentResult =
			// currentResult.updatedValue(SubsumptionDegree.INTERSECTS);
			// }
			// }
			// result = result.updatedValue(currentResult);
			// }
			// return result;
			// }
			// The individual with less properties is the subsumer
			HashMap<String, ArrayList<KBTriple>> subsumerProperties;
			HashMap<String, ArrayList<KBTriple>> subsumedProperties;
			boolean invert;
			if (queryClassificationProperties.size() < varClassificationProperties.size()) {
				subsumerProperties = queryClassificationProperties;
				subsumedProperties = varClassificationProperties;
				invert = false;
			} else {
				subsumerProperties = varClassificationProperties;
				subsumedProperties = queryClassificationProperties;
				invert = true;
			}
			SubsumptionDegree currentResult = SubsumptionDegree.EQUIVALENT;
			// For each property in the subsumer
			for (Entry<String, ArrayList<KBTriple>> subsumerEntry : subsumerProperties.entrySet()) {
				String propertyID = subsumerEntry.getKey();
				// Check if the property exists in the subsumed
				if (subsumedProperties.containsKey(propertyID)) {
					ArrayList<KBTriple> subsumerValues = subsumerEntry.getValue();
					ArrayList<KBTriple> subsumedValues = subsumedProperties.get(propertyID);
					// dc:Obtain the subsumption result for the values of the
					// property
					// KBObject subsumerValue =
					// subsumerEntry.getValue().getObject();
					// KBObject subsumedValue =
					// subsumedProperties.get(propertyID).getObject();
					for (KBTriple subsumedTriple : subsumedValues) {
						for (KBTriple subsumerTriple : subsumerValues) {
							KBObject subsumerValue = subsumerTriple.getObject();
							KBObject subsumedValue = subsumedTriple.getObject();
							if (subsumerValue.isLiteral()) {
								// Two literals are equivalent if they have the
								// same values, otherwise are disjoint
								if (!subsumerValue.equals(subsumedValue))
									return SubsumptionDegree.DISJOINT;
							} else {
								// TODO We have to distinguish between plain
								// values and values from an ontology
								// We need some mechanism to distinguish between
								// a property for which the values are plain or
								// from an ontology
								// Ad hoc solution: if the relation is hasType
								// then it's ontology, otherwise are plain
								// values
								if (propertyID.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#" + "type")
										|| (propertyID.equals(NamespaceManager.getNamespace("wfq") + "hasType"))) {
									currentResult = currentResult.updatedValue(dc.checkSubsumtion(propertyID, subsumerValue.getID(), subsumedValue.getID()));
								} else {
									// TODO We are only comparing the IDs, for
									// other implementations may be necessary to
									// check equivalence in the dc
									String subsumerID = subsumerValue.getID();
									if (subsumerID == null)
										return SubsumptionDegree.DISJOINT;
									String subsumedID = subsumedValue.getID();
									if (!subsumerID.equals(subsumedID))
										return SubsumptionDegree.DISJOINT;
								}
							}
						}
					}
				} else {
					currentResult = currentResult.updatedValue(SubsumptionDegree.INTERSECTS);
				}
			}
			if (currentResult == SubsumptionDegree.EQUIVALENT && queryClassificationProperties.size() != varClassificationProperties.size())
				currentResult = SubsumptionDegree.SUBSUMES;
			if (invert) {
				if (currentResult == SubsumptionDegree.SUBSUMES)
					currentResult = SubsumptionDegree.SUBSUMED;
				else if (currentResult == SubsumptionDegree.SUBSUMED)
					currentResult = SubsumptionDegree.SUBSUMES;
			}
			result = result.updatedValue(currentResult);
		}
		return result;
	}

	private HashMap<String, ArrayList<KBTriple>> filterClassificationProperties(DataCatalog dc, ArrayList<KBTriple> constraints) {
		HashMap<String, ArrayList<KBTriple>> output = new HashMap<String, ArrayList<KBTriple>>();
		for (Iterator<KBTriple> iterator = constraints.iterator(); iterator.hasNext();) {
			KBTriple triple = iterator.next();
			KBObject property = triple.getPredicate();
			if (dc.isClassificationProperty(triple)) {
				String index = property.getID();
				if (triple.getPredicate().getID().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"))
					index = PropertiesHelper.getQueryNamespace() + "hasType";
				ArrayList<KBTriple> tripleList = output.get(index);
				if (tripleList == null)
					tripleList = new ArrayList<KBTriple>();
				// Before we add it, we check the triples. If there is any
				// superclass of it, it is removed. If there is any subclass, it
				// is not added
				boolean addIt = true;
				for (Iterator iterator2 = tripleList.iterator(); iterator2.hasNext();) {
					KBTriple formerTriple = (KBTriple) iterator2.next();
					SubsumptionDegree sd = dc.checkSubsumtion(index, triple.getObject().getID(), formerTriple.getObject().getID());
					// EQUIVALENT or SUBSUMES ==> Don't add it
					if (sd == SubsumptionDegree.EQUIVALENT || sd == SubsumptionDegree.SUBSUMES)
						addIt = false;
					// SUBSUMED ==> Add it and remove formerTriple
					else if (sd == SubsumptionDegree.SUBSUMED)
						iterator2.remove();
					// DISJOINT ==> Add it
				}
				if (addIt)
					tripleList.add(triple);
				output.put(index, tripleList);
			}
		}
		return output;
	}
}
