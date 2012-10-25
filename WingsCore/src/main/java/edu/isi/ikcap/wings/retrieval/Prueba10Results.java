/**
 * 
 */
package edu.isi.ikcap.wings.retrieval;

/**
 * Stores the results of retrieval (correct mappings and time) for a query
 * against a workflow
 * 
 * @author Gonzalo
 * 
 */
public class Prueba10Results {
	public String workflowName;
	public int queryNumber;
	public long elapsedTime;
	public WorkflowRetrievalResults result = null;

	public Prueba10Results(String workflowName, int queryNumber, long elapsedTime, WorkflowRetrievalResults result) {
		this.workflowName = workflowName;
		this.queryNumber = queryNumber;
		this.elapsedTime = elapsedTime;
		this.result = result;
	}

	public String toString() {
		String output = "QUERY " + queryNumber + " against WORKFLOW \"" + workflowName + "\"\n";
		output += "RESULTS: ";
		if (result == null)
			output += "[]\n";
		else
			output += result.toString() + "\n";
		output += "ELAPSED TIME: " + elapsedTime;
		return output;
	}
}