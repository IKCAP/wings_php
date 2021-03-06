package edu.isi.ikcap.wings.workflows.template.event;

public interface TemplateListener {

	public void nodeAdded(TemplateEvent te);

	public void nodeDeleted(TemplateEvent te);

	public void linkAdded(TemplateEvent te);

	public void linkDeleted(TemplateEvent te);

	public void dataVariableBound(TemplateEvent te);

	public void parameterVariableBound(TemplateEvent te);

	public void templateCopied(TemplateEvent te);

	public void rulesApplied(TemplateEvent te);

	public void templateIdSet(TemplateEvent te);

	public void templateParentSet(TemplateEvent te);
	// public void variableAdded (TemplateEvent te);
	// public void variableDeleted (TemplateEvent te);
}
