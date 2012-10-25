package edu.isi.ikcap.wings.retrieval;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import edu.isi.ikcap.wings.workflows.template.Template;

public class WorkflowRetrievalResults implements Iterable<WorkflowRetrievalResults.RetrievalResult> {

	public class RetrievalResult implements Entry<Template, MappingsList> {

		Template template = null;
		MappingsList mappings = null;

		public RetrievalResult(Template template, MappingsList mappings) {
			this.template = template;
			this.mappings = mappings;
		}

		public Template getKey() {
			return template;
		}

		public MappingsList getValue() {
			return mappings;
		}

		public MappingsList setValue(MappingsList value) {
			MappingsList before = mappings;
			mappings = value;
			return before;
		}

		@Override
		public String toString() {
			return template.getName() + " ==> " + mappings.toStringDebug2();
		}

	}

	ArrayList<RetrievalResult> results;
	HashSet<Template> templates;
	HashSet<String> templateNames;

	public WorkflowRetrievalResults() {
		results = new ArrayList<RetrievalResult>();
		templates = new HashSet<Template>();
		templateNames = new HashSet<String>();
	}

	public boolean add(Template template, MappingsList mappings) {
		templates.add(template);
		templateNames.add(template.getName());
		return results.add(new RetrievalResult(template, mappings));
	}

	public void add(Template workflow, MappingsListsList mappings) {
		for (MappingsList mappingsList : mappings) {
			add(workflow, mappingsList);
		}
	}

	@Override
	public String toString() {
		return results.toString();
	}

	public String toStringDebug() {
		String output = "";
		for (RetrievalResult result : results) {
			output += result.getKey().getName() + ": \n";
			output += result.getValue().toStringDebug2() + "\n";
		}
		return output;
	}

	public void add(WorkflowRetrievalResults result) {
		for (RetrievalResult retrievalResult : result) {
			templates.add(retrievalResult.getKey());
			templateNames.add(retrievalResult.getKey().getName());
			results.add(retrievalResult);
		}
	}

	public HashSet<Template> getTemplateSet() {
		return templates;
	}

	public Iterator<RetrievalResult> iterator() {
		return results.iterator();
	}

	public HashSet<String> getTemplateNames() {
		return templateNames;
	}

	public long getResultsCount() {
		return results.size();
	}

}
