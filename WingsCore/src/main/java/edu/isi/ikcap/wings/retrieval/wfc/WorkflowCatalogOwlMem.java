package edu.isi.ikcap.wings.retrieval.wfc;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import edu.isi.ikcap.ontapi.KBObject;
import edu.isi.ikcap.ontapi.OntFactory;
import edu.isi.ikcap.wings.workflows.template.Link;
import edu.isi.ikcap.wings.workflows.template.Node;
import edu.isi.ikcap.wings.workflows.template.Seed;
import edu.isi.ikcap.wings.workflows.template.Template;
import edu.isi.ikcap.wings.workflows.template.impl.SeedOWL;
import edu.isi.ikcap.wings.workflows.template.impl.TemplateOWL;
import edu.isi.ikcap.wings.workflows.template.variables.*;
import edu.isi.ikcap.wings.workflows.util.PropertiesHelper;

public class WorkflowCatalogOwlMem implements WorkflowCatalog {

	ArrayList<Template> templates;
	ArrayList<Seed> seeds;
	
	private static OntFactory ontologyFactory = new OntFactory(OntFactory.JENA);

	/**
	 * Load all the templates from disk into memory.
	 * <p>
	 * Receives a hash table with a list of properties for initialization:
	 * <ul>
	 * <li>domain - Domain of the templates/seeds. If it is not specified the
	 * template domain from PropertiesHelper will be used</li>
	 * </ul>
	 * 
	 * @param properties
	 */
	WorkflowCatalogOwlMem(HashMap<String, String> properties) {
		if (properties == null)
			properties = new HashMap<String, String>();
		templates = new ArrayList<Template>();
		seeds = new ArrayList<Seed>();
		// Get the workflow ontology path
		final String ontdir = PropertiesHelper.getOntologyDir();
		String domain = properties.get("domain");
		if (domain == null) {
			domain = PropertiesHelper.getTemplateDomain();
		}
		// Get seeds
		File dir = new File(ontdir + "/" + domain + "/seeds");
		File[] filesArr = dir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return (pathname.getName().endsWith(".owl"));
			}
		});
		if (filesArr != null) {
			for (int i = 0; i < filesArr.length; i++) {
				// TODO Use a WflowGenFactory to load the seeds and the
				// templates
				try {
					Seed s = new SeedOWL(domain, "seeds/" + filesArr[i].getName());
					seeds.add(s);
					// System.out.println("Remaining: " +
					// String.format("%1$1.2f", (1.0 - (float) i /
					// filesArr.length) * 100)) ;
				} catch (Exception ex) {
					ex.printStackTrace();
					System.err.println("Unable to load SEED: " + filesArr[i].getPath());
				}
			}
		}
		// Get templates
		dir = new File(ontdir + "/" + domain);
		filesArr = dir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return (pathname.getName().endsWith(".owl"));
			}
		});
		for (int i = 0; i < filesArr.length; i++) {
			try {
				templates.add(new TemplateOWL(domain, filesArr[i].getName()));
				// System.out.println("Remaining: " + String.format("%1$1.2f",
				// (1.0 - (float) i / filesArr.length) * 100)) ;
			} catch (Exception ex) {
				ex.printStackTrace();
				System.err.println("Unable to load TEMPLATE: " + filesArr[i].getPath());
			}
		}
	}

	public ArrayList<Template> getAllWorkflows() {
		ArrayList<Template> output = new ArrayList<Template>(templates);
		output.addAll(seeds);
		return output;
	}

	public Template getWorkflow(String name) {
		Template output = getTemplate(name);
		if (output == null)
			return getSeed(name);
		else
			return output;
	}

	public Template getTemplate(String name) {
		for (Template template : templates) {
			if (template.getName().equals(name))
				return template;
		}
		return null;
	}

	public Seed getSeed(String name) {
		for (Seed template : seeds) {
			if (template.getName().equals(name))
				return template;
		}
		return null;
	}

	public ArrayList<Template> getAllTemplates() {
		return new ArrayList<Template>(templates);
	}

	public ArrayList<Seed> getAllSeeds() {
		return new ArrayList<Seed>(seeds);
	}

	public ArrayList<? extends KBObject> getComponents(Template workflow) {
		ArrayList<KBObject> output = new ArrayList<KBObject>();
		String domain = workflow.getNamespace();
		Node[] nodes = workflow.getNodes();
		for (int i = 0; i < nodes.length; i++) {
			output.add(getResourceFromID(domain, nodes[i].getID()));
		}
		return output;
	}

	public ArrayList<? extends KBObject> getDatasets(Template workflow) {
		ArrayList<KBObject> output = new ArrayList<KBObject>();
		String domain = workflow.getNamespace();
		Variable[] variables = workflow.getVariables();
		for (int i = 0; i < variables.length; i++) {
			if (variables[i].isDataVariable())
				output.add(getResourceFromID(domain, variables[i].getID()));
		}
		return output;
	}

	public ArrayList<? extends KBObject> getInputDatasets(Template t) {
		ArrayList<KBObject> output = new ArrayList<KBObject>();
		String domain = t.getNamespace();
		Variable[] variables = t.getInputVariables();
		for (int i = 0; i < variables.length; i++) {
			if (variables[i].isDataVariable())
				output.add(getResourceFromID(domain, variables[i].getID()));
		}
		return output;
	}

	public ArrayList<? extends KBObject> getOutputDatasets(Template t) {
		ArrayList<KBObject> output = new ArrayList<KBObject>();
		String domain = t.getNamespace();
		Variable[] variables = t.getOutputVariables();
		for (int i = 0; i < variables.length; i++) {
			if (variables[i].isDataVariable()) {
				output.add(getResourceFromID(domain, variables[i].getID()));
			}
		}
		return output;
	}

	public ArrayList<? extends KBObject> getParameters(Template t) {
		ArrayList<KBObject> output = new ArrayList<KBObject>();
		String domain = t.getNamespace();
		Variable[] parameters = t.getVariables();
		for (int i = 0; i < parameters.length; i++) {
			if (parameters[i].isParameterVariable())
				output.add(getResourceFromID(domain, parameters[i].getID()));
		}
		return output;
	}

	public ArrayList<? extends KBObject> getIntermediateDatasets(Template t) {
		ArrayList<KBObject> output = new ArrayList<KBObject>();
		String domain = t.getNamespace();
		Variable[] variables = t.getIntermediateVariables();
		for (int i = 0; i < variables.length; i++) {
			if (variables[i].isDataVariable())
				output.add(getResourceFromID(domain, variables[i].getID()));
		}
		return output;
	}

	public ComponentVariable getComponent(Template template, KBObject componentID) {
		return template.getNode(componentID.getID()).getComponentVariable();
	}

	public boolean checkComponentInmediatelyPrecedes(Template workflow, KBObject precedingComponent, KBObject precededComponent) {
		Node precedingNode = workflow.getNode(precedingComponent.getID());
		ArrayList<Node> followingNodes = getFollowingNodes(precedingNode, workflow);
		for (Node node : followingNodes) {
			if (node != null)
				if (node.getID().equals(precededComponent.getID()))
					return true;
		}
		return false;
	}

	public boolean checkComponentPrecedes(Template workflow, KBObject precedingComponent, KBObject precededComponent) {
		Node precedingNode = workflow.getNode(precedingComponent.getID());
		ArrayList<Node> nodesToCheck = getFollowingNodes(precedingNode, workflow);
		while (!nodesToCheck.isEmpty()) {
			Node currentNode = nodesToCheck.remove(0);
			if (currentNode != null) {
				if (currentNode.getID().equals(precededComponent.getID())) {
					return true;
				} else {
					nodesToCheck.addAll(getFollowingNodes(currentNode, workflow));
				}
			}
		}
		return false;
	}

	public boolean checkDatapointInmediatelyPrecedes(Template workflow, KBObject precedingDatapoint, KBObject precededDatapoint) {
		// We go from the preceded datapoint backwards, towards the preceding
		// datapoint
		Variable precededVariable = workflow.getVariable(precededDatapoint.getID());
		if (precededVariable == null)
			return false;
		Link[] currentLink = workflow.getLinks(precededVariable);
		// We are supossing that all the links for a variable begin in the same
		// component
		if (currentLink.length == 0)
			return false;
		ArrayList<Link> precedingLinks = getPrecedingLinks(currentLink[0], workflow);
		for (Link link : precedingLinks) {
			if (linkHasID(link, precedingDatapoint.getID()))
				return true;
		}
		return false;
	}

	public boolean checkDatapointPrecedes(Template workflow, KBObject precedingDatapoint, KBObject precededDatapoint) {
		Variable precededVariable = workflow.getVariable(precededDatapoint.getID());
		if (precededVariable == null)
			return false;
		Link[] currentLink = workflow.getLinks(precededVariable);
		// We are supossing that all the links for a variable begin in the same
		// component
		if (currentLink.length == 0)
			return false;
		ArrayList<Link> precedingLinks = getPrecedingLinks(currentLink[0], workflow);
		while (!precedingLinks.isEmpty()) {
			Link link = precedingLinks.remove(0);
			if (linkHasID(link, precedingDatapoint.getID()))
				return true;
			precedingLinks.addAll(getPrecedingLinks(link, workflow));
		}
		return false;
	}

	public boolean checkInputData(Template workflow, KBObject component, KBObject data) {
		Node node = workflow.getNode(component.getID());
		if (node == null)
			return false;
		Link[] inputLinks = workflow.getInputLinks(node);
		if (inputLinks.length == 0)
			return false;
		for (Link link : inputLinks) {
			if (linkHasID(link, data.getID()))
				return true;
		}
		return false;
	}

	public boolean checkOutputData(Template workflow, KBObject component, KBObject data) {
		Node node = workflow.getNode(component.getID());
		if (node == null)
			return false;
		Link[] inputLinks = workflow.getOutputLinks(node);
		if (inputLinks.length == 0)
			return false;
		for (Link link : inputLinks) {
			if (linkHasID(link, data.getID()))
				return true;
		}
		return false;
	}

	private KBObject getResourceFromID(String defaultDomain, String varID) {
		int pos = varID.indexOf("#");
		if (pos < 0) {
			varID = defaultDomain + "#" + varID;
		}
		return ontologyFactory.getObject(varID);
	}

	private ArrayList<Node> getFollowingNodes(Node origin, Template workflow) {
		HashSet<Node> nodes = new HashSet<Node>();
		Link[] links = workflow.getOutputLinks(origin);
		for (Link link : links) {
			nodes.add(link.getDestinationNode());
		}
		return new ArrayList<Node>(nodes);
	}

	private ArrayList<Link> getPrecedingLinks(Link destination, Template workflow) {
		ArrayList<Link> output = new ArrayList<Link>();
		Node node = destination.getOriginNode();
		if (node == null)
			return output;
		Link[] links = workflow.getInputLinks(node);
		Collections.addAll(output, links);
		return output;
	}

	private boolean linkHasID(Link link, String id) {
		if (link == null)
			return false;
		Variable var = link.getVariable();
		if (var == null)
			return false;
		return var.getID().equals(id);
	}

}
