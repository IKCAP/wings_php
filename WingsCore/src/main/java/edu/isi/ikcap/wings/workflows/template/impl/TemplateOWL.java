package edu.isi.ikcap.wings.workflows.template.impl;

import edu.isi.ikcap.ontapi.KBAPI;
import edu.isi.ikcap.ontapi.KBObject;
import edu.isi.ikcap.ontapi.KBTriple;
import edu.isi.ikcap.ontapi.OntFactory;
import edu.isi.ikcap.ontapi.OntSpec;
import edu.isi.ikcap.wings.util.LayoutHelper;
import edu.isi.ikcap.wings.workflows.template.ConstraintEngine;
import edu.isi.ikcap.wings.workflows.template.Link;
import edu.isi.ikcap.wings.workflows.template.Metadata;
import edu.isi.ikcap.wings.workflows.template.Node;
import edu.isi.ikcap.wings.workflows.template.Port;
import edu.isi.ikcap.wings.workflows.template.Role;
import edu.isi.ikcap.wings.workflows.template.RuleSet;
import edu.isi.ikcap.wings.workflows.template.Template;
import edu.isi.ikcap.wings.workflows.template.event.TemplateEvent;
import edu.isi.ikcap.wings.workflows.template.event.TemplateListener;
import edu.isi.ikcap.wings.workflows.template.sets.Binding;
import edu.isi.ikcap.wings.workflows.template.sets.ComponentSetCreationRule;
import edu.isi.ikcap.wings.workflows.template.sets.PortSetCreationRule;
import edu.isi.ikcap.wings.workflows.template.sets.SetExpression;
import edu.isi.ikcap.wings.workflows.template.sets.ValueBinding;
import edu.isi.ikcap.wings.workflows.template.sets.WingsSet;
import edu.isi.ikcap.wings.workflows.template.sets.SetCreationRule.SetType;
import edu.isi.ikcap.wings.workflows.template.sets.SetExpression.SetOperator;
import edu.isi.ikcap.wings.workflows.template.variables.ComponentVariable;
import edu.isi.ikcap.wings.workflows.template.variables.DataVariable;
import edu.isi.ikcap.wings.workflows.template.variables.ParameterVariable;
import edu.isi.ikcap.wings.workflows.template.variables.Variable;
import edu.isi.ikcap.wings.workflows.util.PropertiesHelper;
import edu.isi.ikcap.wings.workflows.util.SerializableObjectCloner;
import edu.isi.ikcap.wings.workflows.util.UuidGen;
import edu.isi.ikcap.wings.workflows.util.WflowGenFactory;


import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;

import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.vocabulary.RDF;

public class TemplateOWL implements Template {
	private static final long serialVersionUID = 1L;
	private static final int latestVersion = 2;

	int version = 0;

	transient protected KBAPI api;

	protected String url, uriPrefix, dirBase;
	protected String domain, file, name;
	protected String wns; // base workflow namespace
	protected String ns; // namespace of the template

	private Link[] Links = new Link[0];

	private Variable[] Variables = new Variable[0];

	private Node[] Nodes = new Node[0];

	private HashMap<Role, Variable> inputRoles = new HashMap<Role, Variable>();

	private HashMap<Role, Variable> outputRoles = new HashMap<Role, Variable>();

	transient private HashMap<String, KBObject> propertyObjMap;
	transient private HashMap<String, KBObject> conceptObjMap;
	transient protected ConstraintEngine constraintEngine;

	boolean blankCanvas = false;

	private HashMap<String, Template> subtemplates = new HashMap<String, Template>();

	transient private Template createdFrom;
	transient private Template parent;

	private Vector<TemplateListener> evtListeners = new Vector<TemplateListener>();

	protected Metadata meta;
	protected RuleSet rules;

	static protected ArrayList<String> modifiedTemplates = new ArrayList<String>();

	private String[] properties = {
	// Workflow
			"hasNode", "hasLink", "hasInputRole", "hasOutputRole", "hasVersion",

			// Link
			"hasOriginNode", "hasDestinationNode", "hasVariable", "hasOriginPort", "hasDestinationPort",

			// Legacy
			"hasOriginParameter", "hasDestinationParameter",

			// Variable
			"hasDataBinding", "hasParameterValue", "hasComponentBinding", "isConcrete",

			// Node
			"hasComponent", "hasWorkflow", "hasInputPort", "hasOutputPort", "hasPortSetCreationRule", "hasComponentSetCreationRule",

			// SetCreationRule & ExpressionArgument
			"createWorkflowSets", "createComponentSets", "createSetsOn", "hasExpressionArgument",

			// Port
			"satisfiesRole",

			// Role
			"mapsToVariable", "hasDimensionality",

			// Metadata
			"hasContributor", "hasDocumentation", "lastUpdateTime", "createdFrom",

			// RuleSet
			"hasRules" };

	private String[] concepts = { "InputLink", "OutputLink", "InOutLink", "Link", "Node", "Port", 
			"SetCreationExpression", "XProduct", "NWise", "Shift", "IncreaseDimensionality", "ReduceDimensionality",
			"SetCreationRule", "ComponentSetRule", "PortSetRule", 
			"Variable", "DataVariable", "ParameterVariable", "ComponentVariable", 
			"WorkflowTemplate", "Workflow", 
			"Metadata", "RuleSet", "Role" };

	transient protected static OntFactory ontFactory = new OntFactory(OntFactory.JENA);

	/**
	 * the template id (a uuid)
	 */
	public String templateId;

	/**
	 * return a string representation of the template in rdf required for
	 * logging
	 * 
	 * @return a string
	 */
	public String getInternalRepresentation() {
		// return this.api.toN3();
		return this.api.toRdf(true);
	}

	/** {@inheritDoc} */
	public String getID() {
		return templateId;
	}

	/** {@inheritDoc} */
	public String getName() {
		return name;
	}

	public String getDomain() {
		return domain;
	}

	public String getNamespace() {
		return ns;
	}

	/**
	 * Setter for property 'name'.
	 * 
	 * @param name
	 *            Value to set for property 'name'.
	 */

	public void setName(String name, boolean changeInternalStructures) {
		setName(name, changeInternalStructures, false);
		this.name = name;
	}

	protected void setName(String name, boolean changeInternalStructures, boolean subtemplate) {
		if (changeInternalStructures) {
			String oldURI = this.url;
			String newFile = name + ".owl";
			String newURI = PropertiesHelper.getOntologyURL() + "/" + this.domain + "/" + newFile;
			String oldNS = this.ns;
			String newNS = newURI + "#";

			if (!subtemplate) {
				// Change constraint URI's in the engine
				ConstraintEngine engine = this.getConstraintEngine();

				ArrayList<String> varids = new ArrayList<String>();
				for (Variable v : Variables)
					varids.add(v.getID());
				ArrayList<KBTriple> constraints = new ArrayList<KBTriple>();

				for (KBTriple t : engine.getConstraints(varids)) {
					KBObject obj = t.getObject();
					KBObject subj = t.getSubject();
					if (oldNS.equals(subj.getNamespace())) {
						subj = api.getResource(newNS + subj.getName());
					}
					if (oldNS.equals(obj.getNamespace())) {
						obj = api.getResource(newNS + obj.getName());
					}
					if (oldURI.equals(subj.getID())) {
						subj = api.getResource(newURI);
					}
					if (oldURI.equals(obj.getID())) {
						obj = api.getResource(newURI);
					}
					engine.removeConstraint(t);
					t.setObject(obj);
					t.setSubject(subj);
					constraints.add(t);
				}
				engine.setConstraints(constraints);
			}

			// Change variable/node/link URI's
			for (Variable v : Variables)
				v.setID(newNS + v.getName());
			for (Link l : Links)
				l.setID(l.getID().replace(this.ns, newNS));
			for (Node n : Nodes) {
				n.setID(newNS + n.getName());

				ComponentVariable c = n.getComponentVariable();

				if (c != null && c.getNamespace().equals(this.ns)) {
					if (c.isTemplate()) {
						// FIXME: Should the subtemplate's namespace be changed
						// too ? Maybe not !
						// - Currently changing the sub's namespace only if it
						// currently has the same one
						((TemplateOWL) c.getTemplate()).setName(name, true, true);
					} else
						c.setID(newNS + c.getName());
				}

				for (Port p : n.getInputPorts())
					p.setID(newNS + p.getName());
				for (Port p : n.getOutputPorts())
					p.setID(newNS + p.getName());
			}
			for (Role r : inputRoles.keySet()) {
				r.setID(newNS + r.getName());
			}
			for (Role r : outputRoles.keySet()) {
				r.setID(newNS + r.getName());
			}

			this.ns = newNS;

			if (!subtemplate) {
				// Change default namespace
				api.setPrefixNamespace("", newNS);
				this.name = name;
				this.url = newURI;
				this.file = newFile;
			}
		}
	}

	/**
	 * Setter for property 'templateId'.
	 * 
	 * @param templateId
	 *            Value to set for property 'templateId'.
	 */
	public void setID(String templateId) {
		this.templateId = templateId;
	}

	private void cacheConceptsAndProperties() {
		this.propertyObjMap = new HashMap<String, KBObject>();
		this.conceptObjMap = new HashMap<String, KBObject>();
		for (String element : properties) {
			propertyObjMap.put(element, api.getProperty(wns + element));
		}
		for (String element : concepts) {
			conceptObjMap.put(element, api.getConcept(wns + element));
		}
	}

	private Node createNode(KBObject obj) {
		if (obj == null)
			return null;

		KBObject compObj = api.getPropertyValue(obj, propertyObjMap.get("hasComponent"));

		KBObject wObj = api.getPropertyValue(obj, propertyObjMap.get("hasWorkflow"));

		Node n = new Node(obj.getID());

		if (compObj != null) {
			ComponentVariable comp = new ComponentVariable(compObj.getID());

			KBObject cval = api.getDatatypePropertyValue(compObj, propertyObjMap.get("isConcrete"));
			if (cval != null && cval.getValue() != null && (Boolean) cval.getValue())
				comp.setConcrete(true);

			//KBObject compCls = api.getClassOfInstance(compObj);
			ArrayList<KBObject> compClses = api.getPropertyValues(compObj, api.getProperty(RDF.type.getURI()));
			//ArrayList<KBObject> compClses = api.getAllClassesOfInstance(compObj, true);
			for(KBObject compCls: compClses) {
				if(!compCls.getNamespace().equals(this.wns))
					comp.setComponentType(compCls.getID());
			}

			//if (this.version > 1) {
				KBObject cbinding = api.getPropertyValue(compObj, propertyObjMap.get("hasComponentBinding"));
				if (cbinding != null) {
					comp.setBinding(readBindingObjectFromKB(api, cbinding));
					// comp.setBinding(new Binding(cbinding.getID()));
				}
			//}
			n.setComponentVariable(comp);
		}

		else if (wObj != null) {
			Template t = readTemplate(wObj);
			ComponentVariable comp = new ComponentVariable(t);
			comp.setComponentType(conceptObjMap.get("WorkflowTemplate").getID());
			n.setComponentVariable(comp);
		}

		return n;
	}

	private Node getNode(KBObject obj, Node[] nodes) {
		if (obj == null) {
			return null;
		}
		for (Node element : nodes) {
			if (element.getID().equals(obj.getID())) {
				return element;
			}
		}
		return null;
	}

	protected void readTemplate() {
		cacheConceptsAndProperties();

		ArrayList<KBObject> tobjs = api.getInstancesOfClass(conceptObjMap.get("WorkflowTemplate"));

		// Read all the templates into subtemplates
		for (KBObject tobj : tobjs) {
			readTemplate(tobj);
		}

		// Get the root subtemplate (no parent). Copy over its links, nodes, and
		// variables
		String rootid = null;
		for (String id : subtemplates.keySet()) {
			Template t = subtemplates.get(id);
			if (t.getParent() == null) {
				Links = t.getLinks();
				Variables = t.getVariables();
				Nodes = t.getNodes();
				meta = t.getMetadata();
				rules = t.getRules();
				inputRoles = t.getInputRoles();
				outputRoles = t.getOutputRoles();
				ns = t.getNamespace();
				rootid = id;
				break;
			}
		}

		// Remove the root subtemplate
		subtemplates.remove(rootid);
	}

	private Template readTemplate(KBObject templateObj) {
		if (subtemplates.containsKey(templateObj.getID()))
			return subtemplates.get(templateObj.getID());

		TemplateOWL t = new TemplateOWL();
		t.copyBookkeepingInfo(this);
		t.name = templateObj.getName();
		t.setID(templateObj.getID());

		t.ns = templateObj.getNamespace();

		// Create a new constraint engine
		t.constraintEngine = new ConstraintEngineOWL(api, wns);
		
		subtemplates.put(templateObj.getID(), t);

		HashMap<String, KBObject> pmap = propertyObjMap;
		HashMap<String, KBObject> cmap = conceptObjMap;

		KBObject verobj = api.getPropertyValue(templateObj, pmap.get("hasVersion"));
		if (verobj != null) {
			t.version = (Integer) verobj.getValue();
		}
		
		// ns = (String) api.getPrefixNamespaceMap().get("");

		ArrayList<KBObject> linkObjs = api.getPropertyValues(templateObj, pmap.get("hasLink"));
		ArrayList<KBObject> nodeObjs = api.getPropertyValues(templateObj, pmap.get("hasNode"));

		Node[] nodes = new Node[nodeObjs.size()];
		Link[] links = new Link[linkObjs.size()];

		HashMap<String, Variable> varMap = new HashMap<String, Variable>();
		// Variables = new Variable[links.size()];

		int i = 0;
		for (KBObject nodeObj : nodeObjs) {
			Node node = createNode(nodeObj);
			if (node.getComponentVariable().getTemplate() != null) {
				node.getComponentVariable().getTemplate().setParent(t);
			}
			nodes[i] = node;
			i++;
			String comment = api.getComment(nodeObj);
			if (comment != null)
				node.setComment(comment);

			/* New Version Queries */
			if (t.version > 0) {
				ArrayList<KBObject> ports = api.getPropertyValues(nodeObj, pmap.get("hasInputPort"));
				for (KBObject port : ports) {
					node.addInputPort(getPortFromKB(port));
				}
				ports = api.getPropertyValues(nodeObj, pmap.get("hasOutputPort"));
				for (KBObject port : ports) {
					node.addOutputPort(getPortFromKB(port));
				}

				KBObject cruleobj = api.getPropertyValue(nodeObj, pmap.get("hasComponentSetCreationRule"));
				KBObject pruleobj = api.getPropertyValue(nodeObj, pmap.get("hasPortSetCreationRule"));

				ComponentSetCreationRule crule = null;
				PortSetCreationRule prule = null;

				if (cruleobj != null) {
					// KBObject compobj = api.getPropertyValue(pruleobj,
					// pmap.get("createSetsOn"));

					KBObject stype = api.getPropertyValue(cruleobj, pmap.get("createComponentSets"));
					KBObject wtype = api.getPropertyValue(cruleobj, pmap.get("createWorkflowSets"));
					if (stype != null && (Boolean) stype.getValue()) {
						crule = new ComponentSetCreationRule(SetType.STYPE, node.getComponentVariable());
					}
					if (wtype != null && (Boolean) wtype.getValue()) {
						crule = new ComponentSetCreationRule(SetType.WTYPE, node.getComponentVariable());
					}
				}
				if (pruleobj != null) {
					KBObject exprobj = api.getPropertyValue(pruleobj, pmap.get("createSetsOn"));
					SetExpression expr = getSetExpressionFromKB(api, exprobj, node);

					KBObject stype = api.getPropertyValue(pruleobj, pmap.get("createComponentSets"));
					KBObject wtype = api.getPropertyValue(pruleobj, pmap.get("createWorkflowSets"));
					if (stype != null && (Boolean) stype.getValue()) {
						prule = new PortSetCreationRule(SetType.STYPE, expr);
					}
					if (wtype != null && (Boolean) wtype.getValue()) {
						prule = new PortSetCreationRule(SetType.WTYPE, expr);
					}
				}
				if (crule != null)
					node.addComponentSetRule(crule);
				if (prule != null)
					node.addPortSetRule(prule);
			} else {
				// Default WType for component sets
				node.addComponentSetRule(new ComponentSetCreationRule(SetType.WTYPE, node.getComponentVariable()));

				// Default SType for data sets
				SetExpression expr = new SetExpression(SetOperator.XPRODUCT);
				node.addPortSetRule(new PortSetCreationRule(SetType.WTYPE, expr));
			}
		}

		i = 0;
		for (KBObject linkObj : linkObjs) {
			Node fromNode = getNode(api.getPropertyValue(linkObj, pmap.get("hasOriginNode")), nodes);
			Node toNode = getNode(api.getPropertyValue(linkObj, pmap.get("hasDestinationNode")), nodes);

			Port fromPort = null;
			Port toPort = null;

			if (t.version > 0) {
				KBObject fromPortObj = api.getPropertyValue(linkObj, pmap.get("hasOriginPort"));
				KBObject toPortObj = api.getPropertyValue(linkObj, pmap.get("hasDestinationPort"));

				if (fromNode != null && fromPortObj != null) {
					fromPort = fromNode.findOutputPort(fromPortObj.getID());
					if (fromPort == null) {
						fromPort = getPortFromKB(fromPortObj);
						fromNode.addOutputPort(fromPort);
					}
				}

				if (toNode != null && toPortObj != null) {
					toPort = toNode.findInputPort(toPortObj.getID());
					if (toPort == null) {
						toPort = getPortFromKB(toPortObj);
						toNode.addInputPort(toPort);
					}
				}
			} else {
				KBObject fromParamObj = api.getPropertyValue(linkObj, pmap.get("hasOriginParameter"));
				KBObject toParamObj = api.getPropertyValue(linkObj, pmap.get("hasDestinationParameter"));

				if (fromNode != null && fromParamObj != null) {
					int suff = fromNode.getOutputPorts().size();
					fromPort = new Port(linkObj.getNamespace() + "op" + suff);
					fromPort.setRole(new Role(fromParamObj.getID()));
					fromNode.addOutputPort(fromPort);
				}

				if (toNode != null && toParamObj != null) {
					int suff = toNode.getInputPorts().size();
					toPort = new Port(linkObj.getNamespace() + "ip" + suff);
					toPort.setRole(new Role(toParamObj.getID()));
					toNode.addInputPort(toPort);

					toNode.getPortSetRule().getSetExpression().add(new SetExpression(SetOperator.XPRODUCT, toPort));
				}
			}

			links[i] = new Link(linkObj.getID(), fromNode, toNode, fromPort, toPort);

			KBObject varObj = api.getPropertyValue(linkObj, pmap.get("hasVariable"));

			Variable var = varMap.get(varObj.getID());
			if (var == null) {
				if (api.isA(varObj, cmap.get("DataVariable"))) {
					var = new DataVariable(varObj.getID());
					KBObject dsBinding = api.getPropertyValue(varObj, pmap.get("hasDataBinding"));
					if (dsBinding != null) {
						var.setBinding(readBindingObjectFromKB(api, dsBinding));
					}
					varMap.put(varObj.getID(), var);
				} else if (api.isA(varObj, cmap.get("ParameterVariable"))) {
					var = new ParameterVariable(varObj.getID());
					KBObject paramValue = api.getDatatypePropertyValue(varObj, pmap.get("hasParameterValue"));
					if (paramValue != null && paramValue.getValue() != null) {
						var.setBinding(readValueBindingObjectFromKB(api, paramValue));
					}
					varMap.put(varObj.getID(), var);
				}
			}
			if (var != null) {
				links[i].setVariable(var);
				String comment = api.getComment(varObj);
				if (comment != null)
					var.setComment(comment);
			}
			i++;
		}

		t.Links = links;
		t.Nodes = nodes;
		t.Variables = varMap.values().toArray(new Variable[0]);

		readRolesFromKB(this.api, t, templateObj);
		t.autoUpdateRoles();

		t.fillInDefaultSetCreationRules();

		t.meta = readMetadata(this.api, this.url);
		t.rules = readRules(this.api, this.url);

		return t;
	}

	private void readRolesFromKB(KBAPI api, Template t, KBObject templateObj) {
		ArrayList<KBObject> iroleObjs = api.getPropertyValues(templateObj, propertyObjMap.get("hasInputRole"));
		ArrayList<KBObject> oroleObjs = api.getPropertyValues(templateObj, propertyObjMap.get("hasOutputRole"));
		HashSet<Variable> rolevars = new HashSet<Variable>();

		for (KBObject iroleObj : iroleObjs) {
			Role r = new Role(iroleObj.getID());
			KBObject varObj = api.getPropertyValue(iroleObj, propertyObjMap.get("mapsToVariable"));
			KBObject dimObj = api.getDatatypePropertyValue(iroleObj, propertyObjMap.get("hasDimensionality"));
			if (varObj == null) {
				System.err.println("Warning: Role " + iroleObj + " not mapped to any variable");
				continue;
			}
			if (dimObj != null && dimObj.getValue() != null) {
				r.setDimensionality((Integer) dimObj.getValue());
			}
			Variable v = t.getVariable(varObj.getID());
			t.addInputRole(r, v);
			rolevars.add(v);
		}
		for (KBObject oroleObj : oroleObjs) {
			Role r = new Role(oroleObj.getID());
			KBObject varObj = api.getPropertyValue(oroleObj, propertyObjMap.get("mapsToVariable"));
			KBObject dimObj = api.getDatatypePropertyValue(oroleObj, propertyObjMap.get("hasDimensionality"));
			if (varObj == null) {
				System.err.println("Warning: Role " + oroleObj + " not mapped to any variable");
				continue;
			}
			if (dimObj != null && dimObj.getValue() != null) {
				r.setDimensionality((Integer) dimObj.getValue());
			}
			Variable v = t.getVariable(varObj.getID());
			t.addOutputRole(r, v);
			rolevars.add(v);
		}
	}

	// TODO: On Deletion of a variable:
	// - Delete the Port too
	// - Delete the port from the PortSetExpression
	// - If it is an input/output variable, then delete the template
	// Input/Output role too

	public void autoUpdateRoles() {
		// Check if there are any input and output variables that dont have
		// roles.
		// Assign them roles if they dont exist
		
		// Also check if there are any incorrect roles that have been assigned

		HashSet<String> rolevars = new HashSet<String>();
		ArrayList<String> ivars = new ArrayList<String>();
		ArrayList<String> ovars = new ArrayList<String>();
		for (Variable v : getInputVariables()) ivars.add(v.getID());
		for (Variable v : getOutputVariables()) ovars.add(v.getID());
		
		ArrayList<Role> iroles = new ArrayList(inputRoles.keySet());
		ArrayList<Role> oroles = new ArrayList(outputRoles.keySet());
		
		for (Role r : iroles) {
			Variable v = inputRoles.get(r);
			if(v==null || !ivars.contains(v.getID())) {
				inputRoles.remove(r);
				continue;
			}
			if (v != null)
				rolevars.add(v.getID());
		}
		for (Role r : oroles) {
			Variable v = outputRoles.get(r);
			if(v==null || !ovars.contains(v.getID())) {
				outputRoles.remove(r);
				continue;
			}
			if (v != null)
				rolevars.add(v.getID());
		}
		
		for (Variable v : getInputVariables()) {
			if (!rolevars.contains(v.getID())) {
				Role r = new Role(v.getID() + "_irole");
				addInputRole(r, v);
			}
		}
		for (Variable v : getOutputVariables()) {
			if (!rolevars.contains(v.getID())) {
				Role r = new Role(v.getID() + "_orole");
				addOutputRole(r, v);
			}
		}
	}

	// - In addition to checking whether the rule doesn't exist.. check that all
	// ports exist
	// - if a port doesn't exist in the set expression, then add it

	public void fillInDefaultSetCreationRules() {
		// Add default set creation rules if they are not present
		for (Node n : Nodes) {
			this.addDefaultSetCreationRulesForNode(n);
		}
	}

	private void addDefaultSetCreationRulesForNode(Node n) {
		ComponentSetCreationRule crule = n.getComponentSetRule();
		if (crule == null) {
			// Default WType for component sets
			n.addComponentSetRule(new ComponentSetCreationRule(SetType.WTYPE, n.getComponentVariable()));
		}

		PortSetCreationRule prule = n.getPortSetRule();
		if (prule == null) {
			// Default SType for data sets
			SetExpression expr = new SetExpression(SetOperator.XPRODUCT);
			prule = new PortSetCreationRule(SetType.WTYPE, expr);
		}

		HashSet<Port> ruleports = new HashSet<Port>();
		ArrayList<SetExpression> exprs = new ArrayList<SetExpression>();
		exprs.add(prule.getSetExpression());
		while (!exprs.isEmpty()) {
			SetExpression ex = exprs.remove(0);
			if (ex.isSet())
				exprs.addAll(ex);
			else
				ruleports.add(ex.getPort());
		}

		// Default X-Product of all ports that are not defined in the prule
		// FIXME: I wonder if this should be for datavariable ports only ?
		Link[] links = this.getInputLinks(n);
		for (Link l : links) {
			Port p = l.getDestinationPort();
			if (l.getVariable() != null && !ruleports.contains(p))
				prule.getSetExpression().add(new SetExpression(SetOperator.XPRODUCT, p));
		}

		n.addPortSetRule(prule);
	}

	public Role getInputRoleForVariable(Variable v) {
		for(Role r : this.inputRoles.keySet()) {
			if(this.inputRoles.get(r).equals(v)) {
				return r;
			}
		}
		return null;
	}
	
	public Role getOutputRoleForVariable(Variable v) {
		for(Role r : this.outputRoles.keySet()) {
			if(this.outputRoles.get(r).equals(v)) {
				return r;
			}
		}
		return null;
	}
	
	protected Metadata readMetadata(KBAPI api, String metaurl) {
		this.cacheConceptsAndProperties();

		Metadata m = new Metadata();

		KBObject metaClass = conceptObjMap.get("Metadata");
		if (metaClass == null)
			return m;

		KBObject mobj = null;
		ArrayList<KBObject> mobjs = api.getInstancesOfClass(metaClass);
		for (KBObject mob : mobjs) {
			if (mob.getID().equals(metaurl))
				mobj = mob;
		}
		if (mobj == null)
			return m;

		// Fetch metadata
		KBObject val = api.getPropertyValue(mobj, propertyObjMap.get("lastUpdateTime"));
		if (val != null)
			m.lastUpdateTime = (XSDDateTime) val.getValue();

		val = api.getPropertyValue(mobj, propertyObjMap.get("hasDocumentation"));
		if (val != null)
			m.documentation = (String) val.getValue();

		ArrayList<KBObject> crs = api.getPropertyValues(mobj, propertyObjMap.get("hasContributor"));
		for (KBObject cr : crs) {
			if (!m.contributors.contains(cr.getValue()))
				m.contributors.add((String) cr.getValue());
		}

		ArrayList<KBObject> templates = api.getPropertyValues(mobj, propertyObjMap.get("createdFrom"));
		for (KBObject tmp : templates) {
			if (!m.createdFrom.contains(tmp.getValue()))
				m.createdFrom.add((String) tmp.getValue());
		}
		return m;
	}

	protected RuleSet readRules(KBAPI api, String ruleurl) {
		this.cacheConceptsAndProperties();

		RuleSet r = new RuleSet();

		KBObject ruleClass = conceptObjMap.get("RuleSet");
		if (ruleClass == null)
			return r;

		KBObject robj = null;
		ArrayList<KBObject> robjs = api.getInstancesOfClass(ruleClass);
		for (KBObject rob : robjs) {
			if (rob.getID().equals(ruleurl + "#Rules"))
				robj = rob;
		}
		if (robj == null)
			return r;

		// Fetch rules
		KBObject val = api.getPropertyValue(robj, propertyObjMap.get("hasRules"));
		if (val != null)
			r.setRulesText((String) val.getValue());

		return r;
	}

	private Port getPortFromKB(KBObject port) {
		Port p = new Port(port.getID());
		KBObject role = api.getPropertyValue(port, propertyObjMap.get("satisfiesRole"));
		if (role != null) {
			Role r = new Role(role.getID());
			KBObject dim = api.getDatatypePropertyValue(role, propertyObjMap.get("hasDimensionality"));
			if(dim != null && dim.getValue() != null)
				r.setDimensionality((Integer)dim.getValue());
			p.setRole(r);
		}
		return p;
	}

	private Variable[] getLinkVariables(Link[] links) {
		HashSet<Variable> varAr = new HashSet<Variable>();
		for (int i = 0; i < links.length; i++) {
			Variable var = links[i].getVariable();
			varAr.add(var);
		}
		return varAr.toArray(new Variable[varAr.size()]);
	}

	protected void initVariables(String domain, String templateFile, String templateName) {
		this.uriPrefix = PropertiesHelper.getOntologyURL();
		this.dirBase = "file:" + PropertiesHelper.getOntologyDir();
		this.domain = domain;
		this.file = templateFile;
		this.name = templateName;

		this.wns = PropertiesHelper.getWorkflowOntologyURL() + "#";
		this.url = uriPrefix + "/" + domain + "/" + templateFile;
		this.ns = this.url + "#";

		WflowGenFactory.setLocalRedirects(ontFactory);
	}

	protected void initializeAPI(String d, String f) {
		String templateName = f.substring(0, f.lastIndexOf("."));
		initVariables(d, f, templateName);

		// Initialize a PELLET inference kb api
		// -- Changed to PLAIN (no inference) kb api as we don't need much inference on the workflow system side
		api = ontFactory.getAPI(this.url, OntSpec.PLAIN);
		this.ns = api.getPrefixNamespaceMap().get("");
		this.constraintEngine = new ConstraintEngineOWL(api, this.wns);
	}

	public TemplateOWL() {
	}

	// This constructor loads in a template
	public TemplateOWL(String domain, String templateFile) {
		initializeAPI(domain, templateFile);
		readTemplate();
	}

	// This constructor creates a new blank plain template
	public TemplateOWL(String domain, String templateFile, boolean createNew) {
		String templateName = templateFile.substring(0, templateFile.lastIndexOf("."));
		initVariables(domain, templateFile, templateName);

		api = ontFactory.getAPI(OntSpec.PLAIN);
		// KBAPI wapi =
		// ontFactory.getAPI(uriPrefix+"/"+PropertiesHelper.getWorkflowOntologyPath(),
		// OntSpec.PELLET);
		// api.importFrom(wapi);
		api.addImport(this.url, PropertiesHelper.getWorkflowOntologyURL());

		HashMap<String, String> nsmap = new HashMap<String, String>();
		nsmap.putAll(PropertiesHelper.getDCPrefixNSMap());
		nsmap.putAll(PropertiesHelper.getPCPrefixNSMap());
		nsmap.put("", this.ns);
		nsmap.put("wflow", this.wns);
		api.setPrefixNamespaceMap(nsmap);

		// Create a template
		// api.createObjectOfClass(ns+name,
		// api.getConcept(this.wns+"WorkflowTemplate"));
		api.addTriple(api.getResource(ns + name), api.getResource(api.getPrefixNamespaceMap().get("rdf") + "type"), api.getResource(wns + "WorkflowTemplate"));

		api = ontFactory.getAPI(new ByteArrayInputStream(api.toAbbrevRdf(false, this.url).getBytes()), this.url, OntSpec.PLAIN);
		this.constraintEngine = new ConstraintEngineOWL(api, this.wns);

		// this.constraintEngine = new ConstraintEngineOWL(api, this.wns);
		blankCanvas = true;
		this.meta = new Metadata();
		this.rules = new RuleSet();
		// readTemplate(name);
	}

	private void copyBookkeepingInfo(TemplateOWL t) {
		this.uriPrefix = t.uriPrefix;
		this.ns = t.ns;
		this.url = t.url;
		this.wns = t.wns;
		this.name = t.name;
		this.file = t.file;
		this.domain = t.domain;
	}

	public TemplateOWL(TemplateOWL t) {
		Variables = new Variable[0];
		Links = new Link[0];
		Nodes = new Node[0];
		copyBookkeepingInfo(t);

		// copy rules
		this.rules = new RuleSet();
		if (t.rules != null)
			this.rules.setRulesText(t.rules.getRulesText());

		// copy metadata
		this.meta = new Metadata();
		if (t.meta != null) {
			this.meta.documentation = t.meta.documentation;
			this.meta.contributors = new ArrayList<String>(t.meta.contributors);
		}

		//api = ontFactory.getAPI(new ByteArrayInputStream(t.deriveInternalRepresentation().getBytes()), this.url, OntSpec.PLAIN);
		api = t.api;
		this.constraintEngine = new ConstraintEngineOWL((ConstraintEngineOWL)t.getConstraintEngine());
	}

	public Link[] getInputLinks() {
		ArrayList<Link> links = new ArrayList<Link>();
		for (Link l : Links) {
			if (l.isInputLink()) {
				links.add(l);
			}
		}
		return links.toArray(new Link[0]);
	}

	public Link[] getInputLinks(Node n) {
		ArrayList<Link> links = new ArrayList<Link>();
		for (Link l : Links) {
			if (l.getDestinationNode() != null && l.getDestinationNode().equals(n)) {
				links.add(l);
			}
		}
		return links.toArray(new Link[0]);
	}

	public Variable[] getInputVariables() {
		return getLinkVariables(getInputLinks());
	}

	public Variable[] getInputVariables(Node n) {
		return getLinkVariables(getInputLinks(n));
	}

	public Link[] getIntermediateLinks() {
		ArrayList<Link> links = new ArrayList<Link>();
		for (Link l : Links) {
			if (l.isInOutLink()) {
				links.add(l);
			}
		}
		return links.toArray(new Link[0]);
	}

	public Variable[] getIntermediateVariables() {
		return getLinkVariables(getIntermediateLinks());
	}

	public Link getLink(Node fromN, Node toN, Port fromPort, Port toPort) {
		for (Link l : Links) {
			if (l.getOriginNode().equals(fromN) && l.getDestinationNode().equals(toN) && l.getOriginPort().equals(fromPort)
					&& l.getDestinationPort().equals(toPort)) {
				return l;
			}
		}
		return null;
	}

	public Link[] getLinks() {
		return Links;
	}

	public Link[] getLinks(Node fromN, Node toN) {
		ArrayList<Link> links = new ArrayList<Link>();
		for (Link l : Links) {
			if (l.getOriginNode().equals(fromN) && l.getDestinationNode().equals(toN)) {
				links.add(l);
			}
		}
		return links.toArray(new Link[0]);
	}

	public Link[] getLinks(Variable v) {
		ArrayList<Link> links = new ArrayList<Link>();
		for (Link l : Links) {
			if (l.getVariable().equals(v)) {
				links.add(l);
			}
		}
		return links.toArray(new Link[0]);
	}

	public Node[] getNodes() {
		return Nodes;
	}

	public Node[] getNodes(ComponentVariable c) {
		ArrayList<Node> nodes = new ArrayList<Node>();
		for (Node n : Nodes) {
			if (n.getComponentVariable().equals(c)) {
				nodes.add(n);
			}
		}
		return nodes.toArray(new Node[0]);
	}

	public Node getNode(String id) {
		for (Node n : Nodes) {
			if (n.getID().equals(id)) {
				return n;
			}
		}
		return null;
	}

	public Link getLink(String id) {
		for (Link l : Links) {
			if (l.getID().equals(id)) {
				return l;
			}
		}
		return null;
	}

	public Variable getVariable(String id) {
		for (Variable v : Variables) {
			if (v.getID().equals(id)) {
				return v;
			}
		}
		return null;
	}

	public Link[] getOutputLinks() {
		ArrayList<Link> links = new ArrayList<Link>();
		for (Link l : Links) {
			if (l.isOutputLink()) {
				links.add(l);
			}
		}
		return links.toArray(new Link[0]);
	}

	public Link[] getOutputLinks(Node n) {
		ArrayList<Link> links = new ArrayList<Link>();
		for (Link l : Links) {
			if (l.getOriginNode() != null && l.getOriginNode().equals(n)) {
				links.add(l);
			}
		}
		return links.toArray(new Link[0]);
	}

	public Variable[] getOutputVariables() {
		return getLinkVariables(getOutputLinks());
	}

	public Variable[] getOutputVariables(Node n) {
		return getLinkVariables(getOutputLinks(n));
	}

	public Variable[] getVariables() {
		return Variables;
	}

	public Link addLink(Node fromN, Node toN, Port fromPort, Port toPort, Variable var) {
		String lid = ns;
		if (fromPort != null)
			lid += fromPort.getName() + "_To";
		else
			lid += "Input_To";

		if (toPort != null)
			lid += "_" + toPort.getName();
		else
			lid += "_Output";

		int i = 1;
		while (getLink(lid) != null) {
			lid += "_" + i;
		}

		Link l = new Link(lid, fromN, toN, fromPort, toPort);
		if (toN != null && toN.findInputPort(toPort.getID()) == null)
			toN.addInputPort(toPort);

		if (fromN != null && fromN.findInputPort(fromPort.getID()) == null)
			fromN.addOutputPort(fromPort);

		if (var != null) {
			if (getVariable(var.getID()) == null)
				addVariable(var);
			l.setVariable(var);
		}

		Link[] links = new Link[Links.length + 1];
		for (i = 0; i < Links.length; i++) {
			links[i] = Links[i];
		}
		links[Links.length] = l;
		Links = links;

		TemplateEvent te = new TemplateEvent(this, TemplateEvent.LINK_ADDED, l);
		fireTemplateEvent(te);

		return l;
	}

	public void deleteLink(Link l) {
		Link[] links = new Link[Links.length - 1];
		int j = 0;
		for (Link el : Links) {
			if (!el.equals(l)) {
				links[j] = el;
				j++;
			}
		}
		Links = links;

		if (this.getLinks(l.getVariable()).length == 0) {
			deleteVariable(l.getVariable());
		}
		if (l.getDestinationNode() != null)
			l.getDestinationNode().deleteInputPort(l.getDestinationPort());
		if (l.getOriginNode() != null)
			l.getOriginNode().deleteOutputPort(l.getOriginPort());

		TemplateEvent te = new TemplateEvent(this, TemplateEvent.LINK_DELETED, l);
		fireTemplateEvent(te);
	}

	void addVariable(Variable var) {
		Variable[] variables = new Variable[Variables.length + 1];
		for (int i = 0; i < Variables.length; i++) {
			variables[i] = Variables[i];
		}
		variables[Variables.length] = var;
		Variables = variables;

	}

	public void deleteVariable(Variable v) {
		if (Variables.length == 0)
			return;
		Variable[] variables = new Variable[Variables.length - 1];
		int j = 0;
		for (Variable el : Variables) {
			if (!el.equals(v)) {
				variables[j] = el;
				j++;
			}
		}
		Variables = variables;

	}

	public Node addNode(ComponentVariable c) {
		String cid = c.getName();
		if (c.getComponentType() != null)
			cid = c.getComponentTypeName();
		String nid = this.ns + cid + "Node";
		int i = 1;
		while (getNode(nid) != null) {
			nid += "_" + i;
		}
		Node n = new Node(nid);
		n.setComponentVariable(c);

		Node[] nodes = new Node[Nodes.length + 1];
		for (i = 0; i < Nodes.length; i++) {
			nodes[i] = Nodes[i];
		}
		nodes[Nodes.length] = n;
		Nodes = nodes;

		// this.addDefaultSetCreationRulesForNode(n);

		TemplateEvent te = new TemplateEvent(this, TemplateEvent.NODE_ADDED, n);
		fireTemplateEvent(te);

		return n;
	}

	public void deleteNode(Node n) {
		Node[] nodes = new Node[Nodes.length - 1];
		int j = 0;
		for (Node element : Nodes) {
			if (!element.equals(n)) {
				nodes[j] = element;
				j++;
			}
		}
		Nodes = nodes;

		// Delete or Modify input/output links to/from the node
		for (Link l : getInputLinks(n)) {
			if (l.isInputLink()) {
				deleteLink(l);
			} else {
				l.setDestinationNode(null);
				l.setDestinationPort(null);
			}
		}
		for (Link l : getOutputLinks(n)) {
			if (l.isOutputLink()) {
				deleteLink(l);
			} else {
				l.setOriginNode(null);
				l.setOriginPort(null);
			}
		}

		TemplateEvent te = new TemplateEvent(this, TemplateEvent.NODE_DELETED, n);
		fireTemplateEvent(te);
	}

	public void addVariableBinding(Variable v, Binding b) {
		ArrayList<KBTriple> cons = new ArrayList<KBTriple>();
		if (v.isParameterVariable()) {
			KBTriple triple = ontFactory.getTriple(api.getIndividual(v.getID()), propertyObjMap.get("hasParameterValue"), ontFactory
					.getDataObject(b.getValue()));
			cons.add(triple);
		} else {
			KBTriple triple = ontFactory.getTriple(api.getIndividual(v.getID()), propertyObjMap.get("hasDataBinding"), ontFactory.getObject(b.getID()));
			cons.add(triple);
		}
		this.constraintEngine.addConstraints(cons);
		v.setBinding(b);

		if (v.isParameterVariable()) {
			TemplateEvent te = new TemplateEvent(this, TemplateEvent.PARAM_VALUE_ADDED, v, b.getValue());
			fireTemplateEvent(te);
		} else {
			TemplateEvent te = new TemplateEvent(this, TemplateEvent.DATA_OBJECT_BOUND, v, b);
			fireTemplateEvent(te);
		}
	}

	public Template createCopy1() {
		TemplateOWL t = (TemplateOWL) SerializableObjectCloner.clone(this);
		// Create a plain new KBAPI
		t.api = ontFactory.getAPI(OntSpec.PLAIN);
		// t.api.setNamespacePrefixMap(api.getNamespacePrefixMap());
		t.api.copyFrom(api);
		// t.api = fillKBWithDerivedRepresentation(t.api, false);
		t.constraintEngine = new ConstraintEngineOWL(t.api, t.wns);
		return t;
	}

	private ValueBinding copyTemplateBindings(ValueBinding b) {
		if (b.isSet()) {
			ValueBinding rb = new ValueBinding();
			for (WingsSet bb : b)
				rb.add(copyTemplateBindings((ValueBinding) bb));
			return rb;
		} else
			return new ValueBinding(((Template) b.getValue()).createCopy());
	}

	public Template createCopy() {
		TemplateOWL t = new TemplateOWL(this);
		t.getMetadata().createdFrom.add(this.getID());
		
		t.setID(templateId);
		HashMap<Node, Node> map = new HashMap<Node, Node>();

		for (Node e : Nodes) {
			// Add New Nodes
			ComponentVariable ev = e.getComponentVariable();
			ComponentVariable cv;
			if (ev.isTemplate()) {
				cv = new ComponentVariable(ev.getTemplate());
			} else {
				cv = new ComponentVariable(ev.getID());
			}
			cv.setComponentType(ev.getComponentType());
			cv.setConcrete(ev.isConcrete());
			
			if (ev.getBinding() != null) {
				if (ev.isTemplate()) {
					cv.setBinding(copyTemplateBindings((ValueBinding) ev.getBinding()));
					// cv.setBinding(new
					// ValueBinding(((Template)ev.getBinding()).createCopy()));
				} else
					cv.setBinding((Binding) SerializableObjectCloner.clone(ev.getBinding()));
				// System.out.println("******binding******"+cv.getBinding());
			}
			// System.out.println(ev + "," + cv);

			Node n = t.addNode(cv);
			n.setID(e.getID());
			n.setComment(e.getComment());

			// Copy over ports
			for (Port p : e.getInputPorts()) {
				Port np = new Port(p.getID());
				np.setRole(p.getRole());
				n.addInputPort(np);
			}
			for (Port p : e.getOutputPorts()) {
				Port np = new Port(p.getID());
				np.setRole(p.getRole());
				n.addOutputPort(np);
			}

			// Copy over setrules
			ComponentSetCreationRule crule = e.getComponentSetRule();
			PortSetCreationRule prule = e.getPortSetRule();
			if (crule != null) {
				ComponentSetCreationRule ncrule = new ComponentSetCreationRule(crule.getType(), crule.getComponent());
				n.addComponentSetRule(ncrule);
			}
			if (prule != null) {
				SetExpression expr = prule.getSetExpression();
				SetExpression nexpr = copySetExpression(n, expr);

				PortSetCreationRule nprule = new PortSetCreationRule(prule.getType(), nexpr);
				n.addPortSetRule(nprule);
			}

			map.put(e, n);
		}

		ArrayList<String> varids = new ArrayList<String>();

		for (int i = 0; i < Links.length; i++) {
			Link l = Links[i];
			Node fromNode = null, toNode = null;
			Port fromPort = null, toPort = null;
			if (l.getOriginNode() != null) {
				fromNode = map.get(l.getOriginNode());
				fromPort = fromNode.findOutputPort(l.getOriginPort().getID());
			}
			if (l.getDestinationNode() != null) {
				toNode = map.get(l.getDestinationNode());
				toPort = toNode.findInputPort(l.getDestinationPort().getID());
			}

			// Create New Variable
			Variable v = l.getVariable();
			Variable vv = t.getVariable(v.getID());
			if (vv == null) {
				if (v.isDataVariable()) {
					vv = new DataVariable(v.getID());
					// System.out.println("Copying Data Variable : "+v);
					if (v.getBinding() != null) {
						// System.out.println("Copying data binding----");
						vv.setBinding((Binding) SerializableObjectCloner.clone(v.getBinding()));
					}
				} else if (v.isParameterVariable()) {
					vv = new ParameterVariable(v.getID());
					if (v.getBinding() != null) {
						vv.setBinding((Binding) SerializableObjectCloner.clone(v.getBinding()));
					}
				}
				vv.setComment(v.getComment());
			}
			if (vv != null) {
				// Add New Links
				Link ll = t.addLink(fromNode, toNode, fromPort, toPort, vv);
				ll.setID(Links[i].getID());

				varids.add(vv.getID());
			}
		}

		for (Role r : inputRoles.keySet()) {
			Role nr = new Role(r.getID());
			nr.setDimensionality(r.getDimensionality());
			t.addInputRole(nr, t.getVariable(inputRoles.get(r).getID()));
		}
		for (Role r : outputRoles.keySet()) {
			Role nr = new Role(r.getID());
			nr.setDimensionality(r.getDimensionality());
			t.addOutputRole(nr, t.getVariable(outputRoles.get(r).getID()));
		}

		// Copy over Variable Constraints
		t.getConstraintEngine().addConstraints(this.constraintEngine.getConstraints(varids));

		t.cacheConceptsAndProperties();

		TemplateEvent te = new TemplateEvent(this, TemplateEvent.COPY_CREATED, t);
		fireTemplateEvent(te);

		return t;
	}

	public ConstraintEngine getConstraintEngine() {
		return this.constraintEngine;
	}

	public String toString() {
		String hyphen = "-";
		String space = " ";
		String comma = ",";
		String equals = "=";
		String openParen = "(";
		String closeParen = ")";
		StringBuilder componentDescription = new StringBuilder();

		int size = Nodes.length;
		int counter = 0;
		for (Node node : Nodes) {
			// String cname = this.getURIName(node.getComponent().getID());
			String cname = node.getComponentVariable().toString();
			componentDescription.append(cname);
			if (++counter != size) {
				componentDescription.append(hyphen);
			}
		}

		StringBuilder dataVariables = new StringBuilder();
		StringBuilder parameters = new StringBuilder();
		ArrayList<Variable> parameterVariablesWithBindings = new ArrayList<Variable>();
		ArrayList<Variable> dataVariablesWithDataBindings = new ArrayList<Variable>();
		Variable[] inputVariables = this.getInputVariables();
		for (Variable inputVariable : inputVariables) {
			if (inputVariable.isDataVariable()) {
				Binding binding = inputVariable.getBinding();
				if (binding != null) {
					dataVariablesWithDataBindings.add(inputVariable);
				}
			} else {
				Binding binding = inputVariable.getBinding();
				if (binding != null) {
					parameterVariablesWithBindings.add(inputVariable);
				}
			}
		}

		parameters.append(openParen);
		if (parameterVariablesWithBindings.isEmpty()) {
			parameters.append("unbound parameters");
		} else {
			size = parameterVariablesWithBindings.size();
			counter = 0;
			for (Variable parameterVariable : parameterVariablesWithBindings) {
				String uriName = this.getURIName(parameterVariable.getID());
				Object value = parameterVariable.getBinding();
				parameters.append(uriName);
				parameters.append(equals);
				parameters.append(value);
				if (++counter != size) {
					parameters.append(comma);
				}
			}
		}
		parameters.append(closeParen);

		dataVariables.append(openParen);
		if (dataVariablesWithDataBindings.isEmpty()) {
			dataVariables.append("unbound data variables");
		} else {
			size = dataVariablesWithDataBindings.size();
			counter = 0;
			for (Variable dataVariable : dataVariablesWithDataBindings) {
				String uriName = this.getURIName(dataVariable.getID());
				Binding binding = dataVariable.getBinding();
				String dataObjectName;
				if (binding == null) {
					dataObjectName = "null";
				} else {
					dataObjectName = binding.getName();
				}
				dataVariables.append(uriName);
				dataVariables.append(equals);
				dataVariables.append(dataObjectName);
				if (++counter != size) {
					dataVariables.append(comma);
				}
			}
		}
		dataVariables.append(closeParen);

		StringBuilder result = new StringBuilder();
		// result.append("Template");
		// result.append(this.getTemplateId());
		result.append(openParen);
		result.append(componentDescription.toString());
		result.append(space);
		result.append(dataVariables.toString());
		result.append(space);
		result.append(parameters.toString());
		result.append(closeParen);
		return result.toString();

	}

	public String getURIName(String url) {
		return url.substring(url.indexOf('#') + 1);
	}

	public KBAPI getKBAPICopy(boolean includeDataConstraints) {
		// Create a new temporary api
		KBAPI tapi = ontFactory.getAPI(new ByteArrayInputStream(getInternalRepresentation().getBytes()), this.ns, OntSpec.MICRO);

		if (includeDataConstraints) {
			ArrayList<String> varids = new ArrayList<String>();
			for (Variable v : Variables)
				varids.add(v.getID());
			tapi.addTriples(this.constraintEngine.getConstraints(varids));
		}

		tapi.setPrefixNamespaceMap(api.getPrefixNamespaceMap());

		return tapi;
	}

	public String deriveInternalRepresentation() {
		// Create a plain new KBAPI
		KBAPI tapi = ontFactory.getAPI(OntSpec.PLAIN);

		tapi = fillKBWithDerivedRepresentation(tapi, false);

		// Return RDF representation
		// return tapi.toN3(this.url);
		return tapi.toAbbrevRdf(false, this.url);
	}

	private Binding readBindingObjectFromKB(KBAPI tapi, KBObject bobj) {
		if (bobj == null)
			return null;
		Binding b = new Binding(UuidGen.generateAUuid(""));
		if (bobj.isList()) {
			ArrayList<KBObject> items = tapi.getListItems(bobj);
			for (KBObject item : items) {
				b.add(readBindingObjectFromKB(tapi, item));
			}
		} else if (bobj.getID() != null) {
			b.setID(bobj.getID());
		}
		return b;
	}

	private ValueBinding readValueBindingObjectFromKB(KBAPI tapi, KBObject bobj) {
		ValueBinding b = new ValueBinding(UuidGen.generateAUuid(""));
		if (bobj.isList()) {
			ArrayList<KBObject> items = tapi.getListItems(bobj);
			for (KBObject item : items) {
				b.add(readValueBindingObjectFromKB(tapi, item));
			}
		} else {
			b.setValue(bobj.getValue());
		}
		return b;
	}

	protected KBObject writeBindingObjectToKB(KBAPI tapi, Binding b) {
		if (!b.isSet())
			return tapi.getResource(b.getID());

		ArrayList<KBObject> listItems = new ArrayList<KBObject>();
		for (WingsSet a : b) {
			listItems.add(writeBindingObjectToKB(tapi, (Binding) a));
		}
		return tapi.createList(listItems);
	}

	protected KBObject writeValueBindingObjectToKB(KBAPI tapi, ValueBinding b) {
		if (!b.isSet())
			return ontFactory.getDataObject(b.getValue());

		ArrayList<KBObject> listItems = new ArrayList<KBObject>();
		for (WingsSet a : b) {
			listItems.add(writeValueBindingObjectToKB(tapi, (ValueBinding) a));
		}
		return tapi.createList(listItems);
	}

	protected KBAPI fillKBWithDerivedRepresentation(KBAPI tapi, boolean subtemplate) {
		cacheConceptsAndProperties();
		autoUpdateRoles();

		HashMap<String, KBObject> pmap = propertyObjMap;
		HashMap<String, KBObject> cmap = conceptObjMap;

		// Copy over the namespace mappings
		// TODO: Add just the subtemplate import if this is an external
		// subtemplate
		if (!subtemplate) {
			Map<String, String> nsmap = api.getPrefixNamespaceMap();
			tapi.setPrefixNamespaceMap(nsmap);

			// Create the ontology object
			tapi.addImport(ns + file, PropertiesHelper.getWorkflowOntologyURL());
		}

		// Create a template
		KBObject tobj = tapi.createObjectOfClass(ns + name, cmap.get("WorkflowTemplate"));

		// Create the Nodes
		for (Node n : Nodes) {
			KBObject nobj = tapi.createObjectOfClass(n.getID(), cmap.get("Node"));
			tapi.addPropertyValue(tobj, pmap.get("hasNode"), nobj);

			ComponentVariable c = n.getComponentVariable();
			if (c != null && !c.isTemplate()) {
				KBObject cobj;
				if (c.getComponentType() != null) {
					cobj = tapi.createObjectOfClass(c.getID(), tapi.getResource(c.getComponentType()));
				} else {
					cobj = tapi.getResource(c.getID());
				}
				tapi.addClassForInstance(cobj, cmap.get("ComponentVariable"));

				if (cobj != null) {
					tapi.addPropertyValue(nobj, pmap.get("hasComponent"), cobj);
					if (c.isConcrete())
						tapi.addPropertyValue(cobj, pmap.get("isConcrete"), ontFactory.getDataObject(true));
					if (c.getBinding() != null) {
						tapi.addPropertyValue(cobj, pmap.get("hasComponentBinding"), writeBindingObjectToKB(tapi, c.getBinding()));
					}
				}
			} else if (c != null && c.isTemplate()) {
				TemplateOWL subt = (TemplateOWL) c.getTemplate();
				tapi.addPropertyValue(nobj, pmap.get("hasWorkflow"), tapi.getResource(subt.getNamespace() + subt.getName()));

				// RECURSE here
				String suburl = subt.ns.substring(0, subt.ns.lastIndexOf('#'));
				if (api.getImports(ns + file).contains(suburl)) {
					tapi.addImport(ns + file, suburl);
				} else {
					tapi = subt.fillKBWithDerivedRepresentation(tapi, true);
				}
			}

			if (nobj != null && n.getComment() != null) {
				double[] pos = LayoutHelper.parseCommentString(n.getComment());
				if (pos != null)
					tapi.setComment(nobj, LayoutHelper.createCommentString(pos[0], pos[1]));
			}

			// Add Port information
			for (Port p : n.getInputPorts()) {
				KBObject pobj = tapi.createObjectOfClass(p.getID(), cmap.get("Port"));
				if (p.getRole() != null) {
					Role r = p.getRole();
					KBObject roleobj = tapi.getResource(r.getID());
					tapi.setPropertyValue(roleobj, propertyObjMap.get("hasDimensionality"), ontFactory.getDataObject(r.getDimensionality()));
					tapi.addPropertyValue(pobj, pmap.get("satisfiesRole"), roleobj); 
				}
				tapi.addPropertyValue(nobj, pmap.get("hasInputPort"), pobj);
			}

			for (Port p : n.getOutputPorts()) {
				KBObject pobj = tapi.createObjectOfClass(p.getID(), cmap.get("Port"));
				if (p.getRole() != null) {
					Role r = p.getRole();
					KBObject roleobj = tapi.getResource(r.getID());
					tapi.setPropertyValue(roleobj, propertyObjMap.get("hasDimensionality"), ontFactory.getDataObject(r.getDimensionality()));
					tapi.addPropertyValue(pobj, pmap.get("satisfiesRole"), roleobj); 
				}
				tapi.addPropertyValue(nobj, pmap.get("hasOutputPort"), pobj);
			}

			// Add Set Creation Rules information
			ComponentSetCreationRule crule = n.getComponentSetRule();
			PortSetCreationRule prule = n.getPortSetRule();

			if (crule != null) {
				KBObject cruleobj = tapi.createObjectOfClass(null, cmap.get("ComponentSetRule"));
				if (crule.getType() == SetType.STYPE) {
					tapi.addPropertyValue(cruleobj, pmap.get("createComponentSets"), ontFactory.getDataObject(true));
				} else if (crule.getType() == SetType.WTYPE) {
					tapi.addPropertyValue(cruleobj, pmap.get("createWorkflowSets"), ontFactory.getDataObject(true));
				}

				if (crule.getComponent() != null) {
					tapi.addPropertyValue(cruleobj, pmap.get("createSetsOn"), tapi.getResource(crule.getComponent().getID()));
				}
				tapi.addPropertyValue(nobj, pmap.get("hasComponentSetCreationRule"), cruleobj);
			}
			if (prule != null) {
				KBObject pruleobj = tapi.createObjectOfClass(null, cmap.get("PortSetRule"));
				if (prule.getType() == SetType.STYPE) {
					tapi.addPropertyValue(pruleobj, pmap.get("createComponentSets"), ontFactory.getDataObject(true));
				} else if (prule.getType() == SetType.WTYPE) {
					tapi.addPropertyValue(pruleobj, pmap.get("createWorkflowSets"), ontFactory.getDataObject(true));
				}

				SetExpression expr = prule.getSetExpression();
				if (expr != null) {
					KBObject exprobj = createSetExpressionInKB(tapi, expr);
					tapi.addPropertyValue(pruleobj, pmap.get("createSetsOn"), exprobj);
				}
				tapi.addPropertyValue(nobj, pmap.get("hasPortSetCreationRule"), pruleobj);
			}
		}

		// Create Variables
		for (Variable v : Variables) {
			KBObject vobj = null;
			if (v.isDataVariable()) {
				vobj = tapi.createObjectOfClass(v.getID(), cmap.get("DataVariable"));
				if (v.getBinding() != null) {
					tapi.addPropertyValue(vobj, pmap.get("hasDataBinding"), writeBindingObjectToKB(tapi, v.getBinding()));
				}
			} else if (v.isParameterVariable()) {
				vobj = tapi.createObjectOfClass(v.getID(), cmap.get("ParameterVariable"));
				if (v.getBinding() != null) {
					tapi.addPropertyValue(vobj, pmap.get("hasParameterValue"), writeBindingObjectToKB(tapi, v.getBinding()));
				}
			}
			if (vobj != null && v.getComment() != null) {
				double[] pos = LayoutHelper.parseCommentString(v.getComment());
				if (pos != null)
					tapi.setComment(vobj, LayoutHelper.createCommentString(pos[0], pos[1]));
			}
		}

		// Create Links
		for (Link l : Links) {
			KBObject lc = null;
			if (l.isInputLink())
				lc = cmap.get("InputLink");
			else if (l.isInOutLink())
				lc = cmap.get("InOutLink");
			else if (l.isOutputLink())
				lc = cmap.get("OutputLink");
			if (lc != null) {
				KBObject lobj = tapi.createObjectOfClass(l.getID(), lc);
				tapi.addPropertyValue(tobj, pmap.get("hasLink"), lobj);

				Node on = l.getOriginNode();
				Node dn = l.getDestinationNode();
				Port ocp = l.getOriginPort();
				Port dcp = l.getDestinationPort();
				Variable v = l.getVariable();

				if (on != null)
					tapi.addPropertyValue(lobj, pmap.get("hasOriginNode"), tapi.getResource(on.getID()));
				if (ocp != null)
					tapi.addPropertyValue(lobj, pmap.get("hasOriginPort"), tapi.getResource(ocp.getID()));
				if (dn != null)
					tapi.addPropertyValue(lobj, pmap.get("hasDestinationNode"), tapi.getResource(dn.getID()));
				if (dcp != null)
					tapi.addPropertyValue(lobj, pmap.get("hasDestinationPort"), tapi.getResource(dcp.getID()));
				if (v != null)
					tapi.addPropertyValue(lobj, pmap.get("hasVariable"), tapi.getResource(v.getID()));
			}
		}

		writeRolesInKB(tapi, tobj);

		if (!subtemplate) {
			// Add variable constraints
			ArrayList<String> varids = new ArrayList<String>();
			for (Variable v : getVariables())
				varids.add(v.getID());
			tapi.addTriples(getConstraintEngine().getConstraints(varids));

			this.version = latestVersion;
			
			tapi.addPropertyValue(tobj, pmap.get("hasVersion"), ontFactory.getDataObject(this.version));

			writeMetadataDescription(tapi, meta);
			writeRules(tapi, rules);

			this.url = uriPrefix + "/" + domain + "/" + name + ".owl";
		}

		return tapi;
	}

	private KBObject createSetExpressionInKB(KBAPI tapi, SetExpression expr) {
		KBObject exprobj = null;

		if (expr.isSet()) {
			if (expr.getOperator() == SetOperator.XPRODUCT) {
				exprobj = tapi.createObjectOfClass(UuidGen.generateAUuid("_xprod"), conceptObjMap.get("XProduct"));
			} else if (expr.getOperator() == SetOperator.NWISE) {
				exprobj = tapi.createObjectOfClass(UuidGen.generateAUuid("_nwise"), conceptObjMap.get("NWise"));
			} else if (expr.getOperator() == SetOperator.INCREASEDIM) {
				exprobj = tapi.createObjectOfClass(UuidGen.generateAUuid("_dim"), conceptObjMap.get("IncreaseDimensionality"));
			} else if (expr.getOperator() == SetOperator.REDUCEDIM) {
				exprobj = tapi.createObjectOfClass(UuidGen.generateAUuid("_rdim"), conceptObjMap.get("ReduceDimensionality"));
			} else if (expr.getOperator() == SetOperator.SHIFT) {
				exprobj = tapi.createObjectOfClass(UuidGen.generateAUuid("_shift"), conceptObjMap.get("Shift"));
			}
			
			for (SetExpression cexpr : expr) {
				KBObject cexprobj = createSetExpressionInKB(tapi, cexpr);
				tapi.addPropertyValue(exprobj, propertyObjMap.get("hasExpressionArgument"), cexprobj);
			}
		} else if(expr.getPort() != null) {
			exprobj = tapi.getResource(expr.getPort().getID());
		}
		return exprobj;
	}

	private void writeRolesInKB(KBAPI tapi, KBObject templateObj) {
		for (Role r : inputRoles.keySet()) {
			Variable v = inputRoles.get(r);
			if (v == null)
				continue;
			KBObject roleobj = tapi.createObjectOfClass(r.getID(), conceptObjMap.get("Role"));
			tapi.addPropertyValue(templateObj, propertyObjMap.get("hasInputRole"), roleobj);
			tapi.setPropertyValue(roleobj, propertyObjMap.get("mapsToVariable"), tapi.getResource(v.getID()));
			tapi.setPropertyValue(roleobj, propertyObjMap.get("hasDimensionality"), ontFactory.getDataObject(r.getDimensionality()));
		}
		for (Role r : outputRoles.keySet()) {
			Variable v = outputRoles.get(r);
			if (v == null)
				continue;
			KBObject roleobj = tapi.createObjectOfClass(r.getID(), conceptObjMap.get("Role"));
			tapi.addPropertyValue(templateObj, propertyObjMap.get("hasOutputRole"), roleobj);
			tapi.setPropertyValue(roleobj, propertyObjMap.get("mapsToVariable"), tapi.getResource(v.getID()));
			tapi.setPropertyValue(roleobj, propertyObjMap.get("hasDimensionality"), ontFactory.getDataObject(r.getDimensionality()));
		}
	}

	private SetExpression getSetExpressionFromKB(KBAPI tapi, KBObject exprobj, Node n) {
		SetExpression expr = null;

		KBObject exprcls = tapi.getClassOfInstance(exprobj);

		boolean isleaf = false;
		if (exprcls.getID().equals(conceptObjMap.get("XProduct").getID())) {
			expr = new SetExpression(SetOperator.XPRODUCT);
		} else if (exprcls.getID().equals(conceptObjMap.get("NWise").getID())) {
			expr = new SetExpression(SetOperator.NWISE);
		} else if (exprcls.getID().equals(conceptObjMap.get("IncreaseDimensionality").getID())) {
			expr = new SetExpression(SetOperator.INCREASEDIM);
		} else if (exprcls.getID().equals(conceptObjMap.get("ReduceDimensionality").getID())) {
			expr = new SetExpression(SetOperator.REDUCEDIM);
		} else if (exprcls.getID().equals(conceptObjMap.get("Shift").getID())) {
			expr = new SetExpression(SetOperator.SHIFT);
		} else if (exprcls.getID().equals(conceptObjMap.get("Port").getID())) {
			Port p = n.findInputPort(exprobj.getID());
			if (p == null)
				return null;

			expr = new SetExpression(SetOperator.XPRODUCT, p);
			isleaf = true;
		}

		if (!isleaf) {
			ArrayList<KBObject> argobjs = tapi.getPropertyValues(exprobj, propertyObjMap.get("hasExpressionArgument"));
			if (argobjs != null) {
				for (KBObject argobj : argobjs) {
					SetExpression cexpr = getSetExpressionFromKB(tapi, argobj, n);
					if (cexpr != null)
						expr.add(cexpr);
				}
			}
		}
		return expr;
	}

	private SetExpression copySetExpression(Node n, SetExpression expr) {
		if (expr.isSet()) {
			SetExpression nexpr = new SetExpression(expr.getOperator());
			for (SetExpression cexpr : expr)
				nexpr.add(copySetExpression(n, cexpr));
			return nexpr;
		} else {
			return new SetExpression(expr.getOperator(), n.findInputPort(expr.getPort().getID()));
		}
	}

	protected void writeMetadataDescription(KBAPI tapi, Metadata m) {
		// Add metadata
		this.cacheConceptsAndProperties();
		KBObject mobj = tapi.createObjectOfClass(url, conceptObjMap.get("Metadata"));

		if (m.lastUpdateTime != null)
			tapi.setPropertyValue(mobj, propertyObjMap.get("lastUpdateTime"), ontFactory.getDataObject(m.lastUpdateTime));
		if (m.documentation != null)
			tapi.setPropertyValue(mobj, propertyObjMap.get("hasDocumentation"), ontFactory.getDataObject(m.documentation));

		for (String tmp : m.createdFrom)
			if(tmp != null)
				tapi.addPropertyValue(mobj, propertyObjMap.get("createdFrom"), ontFactory.getDataObject(tmp));
		for (String tmp : m.contributors)
			if(tmp != null)
				tapi.addPropertyValue(mobj, propertyObjMap.get("hasContributor"), ontFactory.getDataObject(tmp));
	}

	protected void writeRules(KBAPI tapi, RuleSet rules) {
		// Add rules
		this.cacheConceptsAndProperties();
		KBObject robj = tapi.createObjectOfClass(url + "#Rules", conceptObjMap.get("RuleSet"));
		if (rules.getRulesText() != null)
			tapi.setPropertyValue(robj, propertyObjMap.get("hasRules"), ontFactory.getDataObject(rules.getRulesText()));
	}

	public void clearKBCache(String url) {
		api.refreshCacheForURL(url);
		modifiedTemplates.add(url);
	}

	public void close() {
		api.close();
	}

	public Template getCreatedFrom() {
		return this.createdFrom;
	}

	public void setCreatedFrom(Template createdFrom) {
		this.createdFrom = createdFrom;

		TemplateEvent te = new TemplateEvent(this, TemplateEvent.PARENT_TEMPLATE_SET, createdFrom);
		fireTemplateEvent(te);
	}

	public Template getParent() {
		return this.parent;
	}

	public void setParent(Template parent) {
		this.parent = parent;
	}

	public Metadata getMetadata() {
		return this.meta;
	}

	public String getUrl() {
		return this.url;
	}

	public RuleSet getRules() {
		return this.rules;
	}

	public Template applyRules() {
		KBAPI tapi = getKBAPICopy(true);

		String ns = (String) tapi.getPrefixNamespaceMap().get("wflow");
		ArrayList<KBObject> templates = tapi.getInstancesOfClass(tapi.getConcept(ns + "WorkflowTemplate"));
		if (templates == null || templates.size() == 0)
			return null;

		KBObject template = templates.get(0);
		HashMap<String, String> tmpMap = new HashMap<String, String>(tapi.getPrefixNamespaceMap());

		tapi.setRulesPrefixNamespaceMap(tmpMap);
		// System.out.println(instance.getRules().getRulesText());
		String ruleText = getRules().getRulesText();
		ruleText = ruleText.replaceAll("#.*\\n", "");
		tapi.applyRulesFromString(ruleText);

		KBObject invalidProp = tapi.getProperty(ns + "isInvalid");
		KBObject isInvalid = tapi.getPropertyValue(template, invalidProp);

		if (isInvalid != null && (Boolean) isInvalid.getValue()) {
			return null;
		}

		TemplateEvent te = new TemplateEvent(this, TemplateEvent.RULES_APPLED);
		fireTemplateEvent(te);

		return this;
	}

	public void addTemplateListener(TemplateListener listener) {
		evtListeners.add(listener);
	}

	public void removeTemplateListener(TemplateListener listener) {
		evtListeners.remove(listener);
	}

	protected void fireTemplateEvent(TemplateEvent te) {
		for (TemplateListener l : evtListeners) {
			int id = te.getEventId();
			switch (id) {
			case TemplateEvent.NODE_ADDED:
				l.nodeAdded(te);
				break;
			case TemplateEvent.NODE_DELETED:
				l.nodeDeleted(te);
				break;
			case TemplateEvent.LINK_ADDED:
				l.linkAdded(te);
				break;
			case TemplateEvent.LINK_DELETED:
				l.linkDeleted(te);
				break;
			case TemplateEvent.DATA_OBJECT_BOUND:
				l.dataVariableBound(te);
				break;
			case TemplateEvent.PARAM_VALUE_ADDED:
				l.parameterVariableBound(te);
				break;
			case TemplateEvent.COPY_CREATED:
				l.templateCopied(te);
				break;
			case TemplateEvent.RULES_APPLED:
				l.rulesApplied(te);
				break;
			case TemplateEvent.TEMPLATE_ID_SET:
				l.templateIdSet(te);
				break;
			case TemplateEvent.PARENT_TEMPLATE_SET:
				l.templateParentSet(te);
				break;

			}

		}
	}

	public void addInputRole(Role r, Variable v) {
		inputRoles.put(r, v);
	}

	public void addOutputRole(Role r, Variable v) {
		outputRoles.put(r, v);
	}

	public void deleteInputRole(Role r) {
		inputRoles.remove(r);
	}

	public void deleteOutputRole(Role r) {
		outputRoles.remove(r);
	}

	public HashMap<Role, Variable> getInputRoles() {
		return inputRoles;
	}

	public HashMap<Role, Variable> getOutputRoles() {
		return outputRoles;
	}

}
