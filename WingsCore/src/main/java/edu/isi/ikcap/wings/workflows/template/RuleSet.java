package edu.isi.ikcap.wings.workflows.template;

import java.io.Serializable;

public class RuleSet implements Serializable {
	private static final long serialVersionUID = 1L;
	String rules;

	public RuleSet() {
	}

	public RuleSet(String rules) {
		this.rules = rules;
	}

	public void setRulesText(String rules) {
		this.rules = rules;
	}

	public String getRulesText() {
		return this.rules;
	}

	public void addRules(RuleSet ruleset) {
		if (this.rules != null && ruleset.getRulesText() != null)
			this.rules += "\n" + ruleset.getRulesText();
	}

}
