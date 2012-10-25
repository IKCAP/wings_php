package edu.isi.ikcap.wings.workflows.template.event;

import java.util.EventObject;

//import edu.isi.ikcap.wings.workflows.template.Component;
import edu.isi.ikcap.wings.workflows.template.Link;
import edu.isi.ikcap.wings.workflows.template.Node;
import edu.isi.ikcap.wings.workflows.template.Template;
import edu.isi.ikcap.wings.workflows.template.sets.Binding;
import edu.isi.ikcap.wings.workflows.template.variables.Variable;

public class TemplateEvent extends EventObject {
	private static final long serialVersionUID = 1L;

	public static final int NODE_ADDED = 1;
	public static final int NODE_DELETED = 2;
	public static final int LINK_ADDED = 3;
	public static final int LINK_DELETED = 4;
	public static final int DATA_OBJECT_BOUND = 5;
	public static final int PARAM_VALUE_ADDED = 6;
	public static final int COPY_CREATED = 7;
	public static final int RULES_APPLED = 8;
	public static final int TEMPLATE_ID_SET = 9;
	public static final int PARENT_TEMPLATE_SET = 10;
	// public static final int VARIABLE_ADDED = 11;
	// public static final int VARIABLE_DELETED = 12;

	private Template at = null;
	private String tpId = null;
	private Variable v = null;
	private Binding dobj = null;
	private Object pv = null;
	private Link link = null;
	private Node node = null;

	private int evtId;

	/*
	 * Template Editing Functions supported by this event - Node
	 * addNode(Component c); - void deleteNode(Node n); - Link addLink(Node
	 * fromN, Node toN, Role fromParam, Role toParam, Variable var); - void
	 * deleteLink(Link l); - void addDataObjectBinding(Variable v, DataObject
	 * d); - void addParameterValue(Variable v, Object value); - Template
	 * createCopy(); - Template applyRules(); - void setTemplateId(String
	 * templateId); - void setParent(Template parent);
	 */

	public TemplateEvent(Template source, int id) {
		super(source);
		evtId = id;
	}

	public TemplateEvent(Template source, int id, Node n) {
		super(source);
		node = n;
		evtId = id;
	}

	public TemplateEvent(Template source, int id, Link l) {
		super(source);
		link = l;
		evtId = id;
	}

	public TemplateEvent(Template source, int id, Variable var) {
		super(source);
		v = var;
		evtId = id;
	}

	public TemplateEvent(Template source, int id, Variable parameterVar, Object parameterVal) {
		super(source);
		v = parameterVar;
		pv = parameterVal;
		evtId = id;
	}

	public TemplateEvent(Template source, int id, Variable dataVar, Binding dataObj) {
		super(source);
		v = dataVar;
		dobj = dataObj;
		evtId = id;
	}

	public TemplateEvent(Template source, int id, String templateId) {
		super(source);
		tpId = templateId;
		evtId = id;
	}

	public TemplateEvent(Template source, int id, Template template) {
		super(source);
		at = template;
		evtId = id;
	}

	public Template getAssociatedTemplate() {
		return at;
	}

	public Link getLink() {
		return link;
	}

	public Node getNode() {
		return node;
	}

	public Variable getVariable() {
		return v;
	}

	public Binding getDataObject() {
		return dobj;
	}

	public Object getParameterValue() {
		return pv;
	}

	public String getTemplateId() {
		return tpId;
	}

	public int getEventId() {
		return evtId;
	}
}
