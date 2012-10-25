package edu.isi.ikcap.wings.retrieval.util;

import java.util.HashMap;

import edu.isi.ikcap.wings.workflows.util.PropertiesHelper;

public class NamespaceManager {

	private static HashMap<String, String> namespaces = null;

	private static void initialize() {
		if (namespaces == null) {
			namespaces = new HashMap<String, String>();
			namespaces.putAll(PropertiesHelper.getDCPrefixNSMap());
			namespaces.putAll(PropertiesHelper.getPCPrefixNSMap());
			namespaces.put("wflow", PropertiesHelper.getOntologyURL() + "/" + PropertiesHelper.getWorkflowOntologyPath() + "#");
			namespaces.put("wfq", PropertiesHelper.getQueryNamespace());
			namespaces.put("wfqv", PropertiesHelper.getQueryVariablesNamespace());
		}
	}

	public static String getNamespace(String prefix) {
		initialize();
		return namespaces.get(prefix);
	}

	public static void addNamespace(String prefix, String namespace) {
		initialize();
		namespaces.put(prefix, namespace);
	}

	public static void removeNamespace(String prefix) {
		initialize();
		namespaces.remove(prefix);
	}

}
