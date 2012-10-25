package edu.isi.ikcap.wings.retrieval.query;

import java.io.PrintStream;
import java.util.ArrayList;

import edu.isi.ikcap.ontapi.KBAPI;
import edu.isi.ikcap.ontapi.KBObject;
import edu.isi.ikcap.ontapi.KBTriple;
import edu.isi.ikcap.ontapi.OntFactory;
import edu.isi.ikcap.ontapi.OntSpec;
import edu.isi.ikcap.wings.retrieval.KBTripleList;
import edu.isi.ikcap.wings.retrieval.Query;
import edu.isi.ikcap.wings.retrieval.RetrievalEngine;
import edu.isi.ikcap.wings.retrieval.WorkflowRetrievalResults;
import edu.isi.ikcap.wings.retrieval.solver.CompoundRetrievalSolver;
import edu.isi.ikcap.wings.retrieval.solver.RetrievalSolver;
import edu.isi.ikcap.wings.retrieval.solver.componentsubsumption.CSCBasicNoLog;
import edu.isi.ikcap.wings.retrieval.solver.constraintclassifier.CCFiltering;
import edu.isi.ikcap.wings.retrieval.solver.datasubsumption.DSCBasic;
import edu.isi.ikcap.wings.retrieval.solver.doublemappingremover.DMRBasic;
import edu.isi.ikcap.wings.retrieval.solver.initialmapper.IMHardCoded;
import edu.isi.ikcap.wings.retrieval.solver.structuralchecker.SPCAdHoc;
import edu.isi.ikcap.wings.workflows.template.Node;
import edu.isi.ikcap.wings.workflows.template.Template;
import edu.isi.ikcap.wings.workflows.template.variables.*;

public class QueryEngine {
	static OntFactory ontfactory = new OntFactory(OntFactory.JENA);

	public static Query createQueryFromTemplate(Template t) {
		ArrayList<KBTriple> triples = new ArrayList<KBTriple>();

		ArrayList queryComponents = new ArrayList();
		queryComponents.add("workflow");
		queryComponents.add("component-1");
		queryComponents.add("component-n");

		KBAPI tapi = ontfactory.getAPI(OntSpec.PLAIN);

		String wfqv = QueryConstructs.wfqv;

		KBObject wft = tapi.getResource(wfqv + "wft");

		KBObject hasComponent = QueryConstructs.getProp(tapi, QueryConstructs.hasComponent);
		KBObject subClassOf = QueryConstructs.getProp(tapi, QueryConstructs.subClassOf);
		KBObject hasInputData = QueryConstructs.getProp(tapi, QueryConstructs.hasInputData);
		KBObject hasInputDataset = QueryConstructs.getProp(tapi, QueryConstructs.hasInputDataset);
		KBObject hasOutputDataset = QueryConstructs.getProp(tapi, QueryConstructs.hasOutputDataset);
		KBObject datapointPrecedes = QueryConstructs.getProp(tapi, QueryConstructs.datapointPrecedes);
		KBObject datapointImmediatelyPrecedes = QueryConstructs.getProp(tapi, QueryConstructs.datapointImmediatelyPrecedes);

		for (Node n : t.getNodes()) {
			KBObject c = tapi.getResource(wfqv + n.getComponentVariable().getName());
			KBObject ctype = tapi.getResource(n.getComponentVariable().getComponentType());

			if (!queryComponents.contains(ctype.getName())) {
				triples.add(tapi.addTriple(wft, hasComponent, c));
				triples.add(tapi.addTriple(c, subClassOf, ctype));
			}
			ArrayList<KBObject> ivs = new ArrayList<KBObject>();
			ArrayList<KBObject> ovs = new ArrayList<KBObject>();

			for (Variable v : t.getInputVariables(n)) {
				String vid = v.getID();
				KBObject vobj = tapi.getResource(wfqv + vid.substring(vid.lastIndexOf("#") + 1));
				ivs.add(vobj);

				if (ctype.getName().equals("workflow")) {
					triples.add(tapi.addTriple(wft, hasInputDataset, vobj));
				} else if (!queryComponents.contains(ctype.getName())) {
					triples.add(tapi.addTriple(c, hasInputData, vobj));
				}
			}

			for (Variable v : t.getOutputVariables(n)) {
				String vid = v.getID();
				KBObject vobj = tapi.getResource(wfqv + vid.substring(vid.lastIndexOf("#") + 1));
				ovs.add(vobj);
				if (ctype.getName().equals("workflow")) {
					triples.add(tapi.addTriple(wft, hasOutputDataset, vobj));
				}
			}

			KBObject dprop = null;
			if (ctype.getName().equals("component-n")) {
				dprop = datapointPrecedes;
			} else if (!ctype.getName().equals("workflow")) {
				dprop = datapointImmediatelyPrecedes;
			}

			if (dprop != null) {
				for (KBObject iv : ivs) {
					for (KBObject ov : ovs) {
						triples.add(tapi.addTriple(iv, dprop, ov));
					}
				}
			}
		}

		ArrayList<String> varids = new ArrayList<String>();
		for (Variable var : t.getVariables())
			varids.add(var.getID());

		for (KBTriple triple : t.getConstraintEngine().getConstraints(varids)) {
			KBObject subj = triple.getSubject();
			KBObject obj = triple.getObject();
			if (subj.getNamespace().equals(t.getNamespace())) {
				subj = tapi.getResource(wfqv + subj.getName());
			}
			if (!obj.isLiteral() && obj.getNamespace().equals(t.getNamespace())) {
				obj = tapi.getResource(wfqv + obj.getName());
			}
			triples.add(tapi.addTriple(subj, triple.getPredicate(), obj));
		}

		Query q = new Query(new KBTripleList(triples), wft.getName());

		return q;
	}

	public static String createQueryStringFromTriples(Query q) {
		if (q == null)
			return "";
		StringBuilder s = new StringBuilder();

		for (KBTriple triple : q.getQueryConstraints()) {
			String ts = triple.toString();
			ts = ts.replaceAll(QueryConstructs.wfq, "wfq:");
			ts = ts.replaceAll(QueryConstructs.wfqv, "?");
			s.append(ts);
			s.append("\n");
		}

		return s.toString();
	}

	public static WorkflowRetrievalResults retreiveTemplates(Query q) {
		if (q == null)
			return null;

		RetrievalEngine re = RetrievalEngine.getEngine();
		RetrievalSolver rs = new CompoundRetrievalSolver(new CCFiltering(), new IMHardCoded(), new DMRBasic(), new CSCBasicNoLog(), new SPCAdHoc(),
				new DSCBasic());
		PrintStream ps = System.out;

		WorkflowRetrievalResults results = re.retrieve(q, rs, true, ps);
		ps.println("QUERY ");
		ps.println(q.toStringDebug());
		ps.println(results.toStringDebug());

		return results;
	}
}
