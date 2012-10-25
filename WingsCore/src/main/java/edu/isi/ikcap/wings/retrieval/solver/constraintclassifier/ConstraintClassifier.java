package edu.isi.ikcap.wings.retrieval.solver.constraintclassifier;

import java.util.HashMap;

import edu.isi.ikcap.wings.retrieval.KBTripleList;
import edu.isi.ikcap.wings.retrieval.Query;
import edu.isi.ikcap.wings.retrieval.solver.RetrievalSolver.JenaFiltersEnum;

public interface ConstraintClassifier {

	HashMap<JenaFiltersEnum, KBTripleList> classifyConstraints(Query query);

}
