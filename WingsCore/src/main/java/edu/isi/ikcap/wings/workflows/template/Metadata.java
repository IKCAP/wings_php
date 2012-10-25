package edu.isi.ikcap.wings.workflows.template;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;

import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;

public class Metadata implements Serializable {
	private static final long serialVersionUID = 1L;
	transient public XSDDateTime lastUpdateTime;
	public String documentation;
	public ArrayList<String> contributors = new ArrayList<String>();
	public ArrayList<String> createdFrom = new ArrayList<String>();

	public Metadata() {
	}

	/*
	 * Metadata Properties
	 */
	public void addContributor(String username) {
		if (username != null && !this.contributors.contains(username))
			this.contributors.add(username);
	}

	public ArrayList<String> getContributors() {
		return this.contributors;
	}

	public void setLastUpdateTime() {
		this.lastUpdateTime = new XSDDateTime(Calendar.getInstance());
	}

	public Calendar getLastUpdateTime() {
		if (this.lastUpdateTime != null)
			return this.lastUpdateTime.asCalendar();
		return null;
	}

	public void setDocumentation(String doc) {
		this.documentation = doc;
	}

	public String getDocumentation() {
		return this.documentation;
	}

	public void addCreationSource(String name) {
		if (!this.createdFrom.contains(name))
			this.createdFrom.add(name);
	}

	public ArrayList<String> getCreationSources() {
		return this.createdFrom;
	}
}
