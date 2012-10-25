package edu.isi.ikcap.wings.workflows.template;

import java.io.Serializable;
import java.util.HashMap;

import edu.isi.ikcap.wings.workflows.template.event.TemplateListener;
import edu.isi.ikcap.wings.workflows.template.sets.Binding;
import edu.isi.ikcap.wings.workflows.template.variables.ComponentVariable;
import edu.isi.ikcap.wings.workflows.template.variables.Variable;

public interface Template extends Serializable {

	// Link Queries
	Link[] getLinks();

	Link[] getInputLinks();

	Link[] getOutputLinks();

	Link[] getIntermediateLinks();

	Link getLink(Node fromN, Node toN, Port fromPort, Port toPort);

	Link[] getLinks(Node fromN, Node toN);

	Link[] getLinks(Variable v);

	Link[] getInputLinks(Node n);

	Link[] getOutputLinks(Node n);

	Link getLink(String id);

	// Node Queries
	Node[] getNodes();

	Node[] getNodes(ComponentVariable c);

	Node getNode(String id);

	// Variable Queries
	Variable[] getVariables();

	Variable[] getInputVariables();

	Variable[] getOutputVariables();

	Variable[] getIntermediateVariables();

	Variable[] getInputVariables(Node n);

	Variable[] getOutputVariables(Node n);

	Variable getVariable(String id);

	void deleteVariable(Variable v);

	Role getInputRoleForVariable(Variable v);
	
	Role getOutputRoleForVariable(Variable v);
	
	// Input Output roles of the template itself
	HashMap<Role, Variable> getInputRoles();

	HashMap<Role, Variable> getOutputRoles();

	void addInputRole(Role r, Variable v);

	void addOutputRole(Role r, Variable v);

	void deleteInputRole(Role r);

	void deleteOutputRole(Role r);

	// Automatically add roles based on input/output variables
	void autoUpdateRoles();

	// Automatically add set creation rules
	// (component/port set rules for expanding components during bw/fw sweeping)
	void fillInDefaultSetCreationRules();

	// Constraint Queries
	ConstraintEngine getConstraintEngine();

	// Template Editing Functions
	Node addNode(ComponentVariable c);

	void deleteNode(Node n);

	Link addLink(Node fromN, Node toN, Port fromPort, Port toPort, Variable var);

	void deleteLink(Link l);

	void addVariableBinding(Variable v, Binding b);

	Template createCopy();

	String getInternalRepresentation();

	String deriveInternalRepresentation();

	String getID();

	void setID(String templateId);

	void setCreatedFrom(Template createdFrom);

	Template getCreatedFrom();

	void setParent(Template parent);

	Template getParent();

	String getDomain();

	String getName();

	String getNamespace();

	String getUrl();

	Metadata getMetadata();

	RuleSet getRules();

	Template applyRules();

	void setName(String name, boolean changeInternalStructures);

	void clearKBCache(String url);

	void close();

	// Event Listeners

	void addTemplateListener(TemplateListener listener);

	void removeTemplateListener(TemplateListener listener);
}
