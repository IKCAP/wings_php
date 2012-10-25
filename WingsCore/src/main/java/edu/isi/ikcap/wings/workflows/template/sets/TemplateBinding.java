package edu.isi.ikcap.wings.workflows.template.sets;

import edu.isi.ikcap.wings.workflows.template.Template;

public class TemplateBinding extends Binding {
	private static final long serialVersionUID = 1L;
	transient private Template t;

	public TemplateBinding(Template t) {
		this.t = t;
		super.obj = t.getID();
	}

	public TemplateBinding(TemplateBinding b) {
		super(b);
	}

	public TemplateBinding(Template[] values) {
		for (Template val : values) {
			this.add(new TemplateBinding(val));
		}
	}

	public Template getTemplate() {
		return this.t;
	}
}
