package edu.isi.ikcap.wings.retrieval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Results of a query and a set of workflows
 * 
 * @author Gonzalo
 * 
 */
public class Prueba11Results {

	HashMap<String, Prueba10Results> resultList;
	long totalTime;

	public Prueba11Results() {
		resultList = new HashMap<String, Prueba10Results>();
		totalTime = 0;
	}

	public int getResultsSize() {
		return resultList.size();
	}

	public void add(String workflowName, Prueba10Results prueba10) {
		resultList.put(workflowName, prueba10);
		totalTime += prueba10.elapsedTime;
	}

	public long getTotalTime() {
		return totalTime;
	}

	public ArrayList<WorkflowRetrievalResults> getResults() {
		ArrayList<WorkflowRetrievalResults> output = new ArrayList<WorkflowRetrievalResults>();
		for (Prueba10Results results : resultList.values()) {
			output.add(results.result);
		}
		return output;
	}

	public String toString() {
		return toString(true);
	}

	public Set<String> getRetrievedWorkflows() {
		return resultList.keySet();
	}

	public String toString(boolean detail) {
		String output = "";
		long notRetrieved = 0;
		long totalMappings = 0;
		if (detail)
			for (Prueba10Results workflowResult : resultList.values()) {
				output += workflowResult.toString() + "\n";
				if (workflowResult.result == null)
					notRetrieved++;
				else {
					long resultsCount = workflowResult.result.getResultsCount();
					if (resultsCount == 0)
						notRetrieved++;
					else
						totalMappings += resultsCount;
				}
			}
		output += "TOTALS\n" + "\tElapsed time: " + totalTime + "\n" + "\tRetrieved mappings: " + totalMappings + "\n" + "\tTemplates without mappings: "
				+ notRetrieved;
		return output;
	}
}
