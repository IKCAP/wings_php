package edu.isi.ikcap.wings.workflows.template.sets;

import edu.isi.ikcap.wings.workflows.template.variables.ComponentVariable;

public class ComponentSetCreationRule extends SetCreationRule {
	private static final long serialVersionUID = 1L;
	private ComponentVariable component;

	// private SetExpression expression;

	public ComponentSetCreationRule(SetType type, ComponentVariable component) {
		super(type);
		this.component = component;
	}

	public ComponentVariable getComponent() {
		return this.component;
	}

}
