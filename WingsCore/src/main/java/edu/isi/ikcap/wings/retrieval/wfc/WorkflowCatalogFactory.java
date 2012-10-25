package edu.isi.ikcap.wings.retrieval.wfc;

import java.util.HashMap;

public class WorkflowCatalogFactory {
	private static WorkflowCatalog catalog = null;
	private static HashMap<String, String> properties = null;

	public WorkflowCatalogFactory() {
		// TODO Auto-generated constructor stub
	}

	public static void setProperties(HashMap<String, String> properties) {
		WorkflowCatalogFactory.properties = properties;
		createNewCatalog();
	}

	public static WorkflowCatalog getCatalog() {
		if (catalog == null)
			createNewCatalog();
		return catalog;
	}

	private static void createNewCatalog() {
		catalog = new WorkflowCatalogOwlMem(properties);
	}
}
