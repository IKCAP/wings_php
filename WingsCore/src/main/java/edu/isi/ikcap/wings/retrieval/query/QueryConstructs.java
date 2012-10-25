package edu.isi.ikcap.wings.retrieval.query;

import edu.isi.ikcap.ontapi.KBAPI;
import edu.isi.ikcap.ontapi.KBObject;
import edu.isi.ikcap.wings.retrieval.util.NamespaceManager;

public class QueryConstructs {
	static String wfq = NamespaceManager.getNamespace("wfq");
	static String wfqv = NamespaceManager.getNamespace("wfqv");

	// WP
	public static String hasInputDataset = "hasInputDataset";
	public static String hasOutputDataset = "hasOutputDataset";
	public static String hasIntermediateDataset = "hasIntermediateDataset";
	public static String hasDataset = "hasDataset";
	public static String hasComponent = "hasComponent";
	public static String hasInputParameter = "hasInputParameter"; // CP also

	// SP
	public static String datapointPrecedes = "datapointPrecedes";
	public static String datapointImmediatelyPrecedes = "datapointImmediatelyPrecedes";
	public static String hasInputData = "hasInputData";
	public static String precedes = "precedes";

	// CP
	public static String subClassOf = "subClassOf";

	// DP
	public static String hasDataBinding = "hasDataBinding";
	public static String canBeBound = "canBeBound";
	public static String hasType = "hasType";

	public static KBObject getProp(KBAPI api, String prop) {
		return api.getResource(wfq + prop);
	}
}
