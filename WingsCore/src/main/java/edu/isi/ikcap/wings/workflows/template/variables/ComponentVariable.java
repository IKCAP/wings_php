package edu.isi.ikcap.wings.workflows.template.variables;

import java.net.URI;

import edu.isi.ikcap.wings.workflows.template.Template;

public class ComponentVariable extends Variable {
	private static final long serialVersionUID = 1L;

	private URI typeuri;
	private boolean isConcrete;
	private Template template;

	public ComponentVariable(String id) {
		super(id, VariableType.COMPONENT);
	}

	public ComponentVariable(Template t) {
		super(t.getID(), VariableType.COMPONENT);
		this.template = t;
	}

	public void setConcrete(boolean isConcrete) {
		this.isConcrete = isConcrete;
	}

	public void setComponentType(String typeid) {
		try {
			this.typeuri = new URI(typeid);
		} catch (Exception e) {
			System.err.println(typeid + " Not a URI. Only URIs allowed for Component Types");
		}
	}

	public String getComponentType() {
		if (typeuri != null)
			return typeuri.toString();
		else
			return null;
	}

	public String getComponentTypeName() {
		if (typeuri != null)
			return typeuri.getFragment();
		else
			return null;
	}

	public boolean isConcrete() {
		return this.isConcrete;
		// return (binding != null)
	}

	public Template getTemplate() {
		return this.template;
	}

	public boolean isTemplate() {
		return (this.template != null);
	}
	
	public String toString() {
		return super.toString() + (typeuri != null ? " (Type: " + typeuri + ")" : "");
	}
}