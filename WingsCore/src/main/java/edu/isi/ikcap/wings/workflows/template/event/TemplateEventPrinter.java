package edu.isi.ikcap.wings.workflows.template.event;

import edu.isi.ikcap.wings.workflows.template.Template;

public class TemplateEventPrinter extends TemplateAdapter {
	public void dataVariableBound(TemplateEvent te) {
		System.out.println("Data Variable Bound");
		System.out.println("   var: " + te.getVariable());
		System.out.println("   val: " + te.getDataObject());
		System.out.println("   modified template: " + te.getSource());

	}

	public void linkAdded(TemplateEvent te) {
		System.out.println("Link added");
		System.out.println("   link: " + te.getLink());
		System.out.println("   modified template: " + te.getSource());

	}

	public void linkDeleted(TemplateEvent te) {
		System.out.println("Link deleted");
		System.out.println("   link: " + te.getLink());
		System.out.println("   modified template: " + te.getSource());

	}

	public void nodeAdded(TemplateEvent te) {
		System.out.println("Node added");
		System.out.println("   node: " + te.getNode());
		System.out.println("   modified template: " + te.getSource());

	}

	public void nodeDeleted(TemplateEvent te) {
		System.out.println("Node deleted");
		System.out.println("   node: " + te.getNode());
		System.out.println("   modified template: " + ((Template) te.getSource()).getID());
	}

	public void parameterVariableBound(TemplateEvent te) {
		System.out.println("Parameter Variable Bound");
		System.out.println("   var: " + te.getVariable());
		System.out.println("   val: " + te.getParameterValue());
		System.out.println("   modified template: " + te.getSource());
	}

	public void rulesApplied(TemplateEvent te) {
		System.out.println("Rules applied");
		System.out.println("   modified template: " + te.getSource());
	}

	public void templateCopied(TemplateEvent te) {
		System.out.println("Template copied");
		System.out.println("   copied template id: " + te.getAssociatedTemplate().getID());
		System.out.println("   modified template: " + te.getSource());
	}

	public void templateIdSet(TemplateEvent te) {
		System.out.println("Template id set");
		System.out.println("   id: " + te.getTemplateId());
		System.out.println("   modified template: " + te.getSource());
	}

	public void templateParentSet(TemplateEvent te) {
		System.out.println("Template parent set");
		System.out.println("   parent id: " + te.getAssociatedTemplate().getID());
		System.out.println("   modified template: " + ((Template) te.getSource()).getID());
	}

	// public void variableAdded(TemplateEvent te) {
	// System.out.println("Variable added");
	// System.out.println("   var: " + te.getVariable());
	// System.out.println("   val: " + te.getParameterValue());
	// System.out.println("   modified template: " + te.getSource());
	//        
	// }
	//
	// public void variableDeleted(TemplateEvent te) {
	// System.out.println("Variable deleted");
	// System.out.println("   var: " + te.getVariable());
	// System.out.println("   modified template: " + te.getSource());
	// }
}
