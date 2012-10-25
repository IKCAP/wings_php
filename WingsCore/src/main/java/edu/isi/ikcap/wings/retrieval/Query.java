package edu.isi.ikcap.wings.retrieval;

import java.util.HashMap;

public class Query {
	public static final String WORKFLOW_QUERY_NAMESPACE = "wfq";
	public static final String WORKFLOW_NAMESPACE = "wf";
	public static final String COMPONENT_DOMAIN_NAMESPACE = "acdm";
	public static final String DATA_DOMAIN_NAMESPACE = "dcdm";

	KBTripleList queryConstraints;
	String workflowVariable;
	HashMap<String, String> namespaces;

	public Query(KBTripleList queryConstraints, String workflowVariable) {
		this.queryConstraints = queryConstraints;
		this.workflowVariable = workflowVariable;
		namespaces = new HashMap<String, String>();
	}

	/**
	 * @return the query constraints
	 */
	public KBTripleList getQueryConstraints() {
		return queryConstraints;
	}

	/**
	 * @return the workflowVariable
	 */
	public String getWorkflowVariable() {
		return workflowVariable;
	}

	/**
	 * @param workflowVariable
	 *            the workflowVariable to set
	 */
	public void setWorkflowVariable(String workflowVariable) {
		this.workflowVariable = workflowVariable;
	}

	/**
	 * @param queryConstraints
	 *            the queryConstraints to set
	 */
	public void setQueryConstraints(KBTripleList queryConstraints) {
		this.queryConstraints = queryConstraints;
	}

	public String getNamespace(String prefix) {
		return namespaces.get(prefix);
	}

	public void setNamespace(String prefix, String value) {
		namespaces.put(prefix, value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String output = "";
		if (workflowVariable != null)
			output += "VARIABLE: " + workflowVariable + "\n";
		if (queryConstraints != null)
			output += queryConstraints.toString();
		return output;
	}

	public String toStringDebug() {
		String output = "";
		if (workflowVariable != null)
			output += "VARIABLE: " + workflowVariable + "\n";
		if (queryConstraints != null)
			output += queryConstraints.toStringDebug();
		return output;
	}

}
