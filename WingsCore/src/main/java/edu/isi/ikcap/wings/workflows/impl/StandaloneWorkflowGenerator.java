////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2007, Your Corporation. All Rights Reserved.
////////////////////////////////////////////////////////////////////////////////

package edu.isi.ikcap.wings.workflows.impl;

import edu.isi.ikcap.ontapi.KBObject;
import edu.isi.ikcap.ontapi.KBTriple;
import edu.isi.ikcap.wings.catalogs.components.ComponentCatalog;
import edu.isi.ikcap.wings.catalogs.components.classes.ComponentMapsAndRequirements;
import edu.isi.ikcap.wings.catalogs.components.impl.isi.TemplateCatalog;
import edu.isi.ikcap.wings.catalogs.data.DataCatalog;
import edu.isi.ikcap.wings.catalogs.data.classes.DataVariableDataObjectBinding;
import edu.isi.ikcap.wings.util.logging.LogEvent;
import edu.isi.ikcap.wings.workflows.AutomaticWorkflowGenerator;
import edu.isi.ikcap.wings.workflows.template.*;
import edu.isi.ikcap.wings.workflows.template.sets.Binding;
import edu.isi.ikcap.wings.workflows.template.sets.ComponentSetCreationRule;
import edu.isi.ikcap.wings.workflows.template.sets.PortBinding;
import edu.isi.ikcap.wings.workflows.template.sets.PortBindingList;
import edu.isi.ikcap.wings.workflows.template.sets.PortSetCreationRule;
import edu.isi.ikcap.wings.workflows.template.sets.PortSetRuleHandler;
import edu.isi.ikcap.wings.workflows.template.sets.ValueBinding;
import edu.isi.ikcap.wings.workflows.template.sets.WingsSet;
import edu.isi.ikcap.wings.workflows.template.sets.SetCreationRule.SetType;
import edu.isi.ikcap.wings.workflows.template.variables.*;
import edu.isi.ikcap.wings.workflows.util.*;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

/**
 * Name: StandaloneWorkflowGenerator
 * <p/>
 * Package: edu.isi.ikcap.workflows
 * <p/>
 * User: moody
 * <p/>
 * Date: Aug 23, 2007
 * <p/>
 * Time: 2:31:23 PM
 */
public class StandaloneWorkflowGenerator implements AutomaticWorkflowGenerator {
	/**
	 * the info logger
	 */
	private Logger logger;

	/**
	 * the current seed
	 */
	public Seed currentSeed;

	/**
	 * the dc
	 */
	public DataCatalog dc;

	/**
	 * the pc
	 */
	public ComponentCatalog pc;

	/*
	 * The template catalog
	 */
	public ComponentCatalog tc;

	/**
	 * template catalog domain
	 */
	public String templateLibraryDomain;

	/**
	 * explanations
	 */
	public ArrayList<String> explanations;
	
	/*
	 * Access to the WG-PC
	 */
	public WorkflowGenerationProvenanceCatalog wgpc;

	/*
	 * Access to the workflow factory
	 */
	public WflowGenFactory wfac;

	/*
	 * Flag to indicated if provenance information needs to be stored or not
	 */
	public boolean storeProvenance;

	LogEvent curLogEvent;

	/**
	 * base constructor
	 * 
	 * @param uriPrefix
	 *            the uriPrefix
	 * @param dc
	 *            the dc
	 * @param pc
	 *            the pc
	 * @param baseDirectory
	 *            the base directory of the ontologies
	 * @param templateLibraryDomain
	 *            the domain of the template library
	 */

	public String request_id;

	String dataNS;

	public StandaloneWorkflowGenerator(DataCatalog dc, ComponentCatalog pc, String templateLibraryDomain, boolean provenanceFlag, String ldid) {
		this.request_id = ldid;
		this.logger = PropertiesHelper.getLogger(this.getClass().getName(), ldid);

		this.dc = dc;
		this.pc = pc;
		this.tc = new TemplateCatalog(this);

		this.templateLibraryDomain = templateLibraryDomain;
		this.wfac = new WflowGenFactory(WflowGenFactory.INTERNALOWL);
		this.storeProvenance = provenanceFlag;
		if (provenanceFlag)
			this.wgpc = new WorkflowGenerationProvenanceCatalog();

		String data = PropertiesHelper.getDCNewDataPrefix();
		this.dataNS = PropertiesHelper.getDCPrefixNSMap().get(data);
		this.explanations = new ArrayList<String>();
	}

	/**
	 * Step 1
	 * 
	 * @param seedFile
	 *            the actual path to the seed file
	 * @param templateFile
	 *            the template
	 * @return a list of candiate templates
	 */
	public ArrayList<Template> findCandidateTemplatesForSeedFile(String seedFile, String templateID) {
		if (logger.isInfoEnabled()) {
			HashMap<String, Object> argumentMap = new HashMap<String, Object>(2);
			argumentMap.put("seedFile", seedFile);
			argumentMap.put("templateID", templateID);
			String arguments = AWGLoggerHelper.getArgumentString("<findCandidateTemplates> q1.0", argumentMap);
			logger.info(arguments);
		}

		// String seedName = seedFile.substring(0, seedFile.indexOf("."));
		// String seedUuid = UuidGen.generateAUuid(seedName);

		Seed seed = wfac.getSeed(templateLibraryDomain, seedFile);
		// seed.setSeedId(seedUuid);
		seed.setSeedId(this.request_id);

		this.setCurrentSeed(seed);

		if (storeProvenance)
			wgpc.addSeedToProvenanceCatalog(seed);

		// note that the template "file" is actually the seedFile
		Template template = wfac.getTemplate(templateLibraryDomain, seedFile);
		templateID = template.getName();
		template.setID(UuidGen.generateAUuid(templateID));
		template.setName(templateID, false);

		ArrayList<Template> result = new ArrayList<Template>(1);
		result.add(template);

		if (logger.isInfoEnabled()) {
			String returnValue = AWGLoggerHelper.getReturnString("<findCandidateTemplates> q1.0", result);
			logger.info(returnValue);
		}

		return result;
	}

	/**
	 * Step 1
	 * 
	 * @param seedFile
	 *            the seed
	 * @param templateFile
	 *            the template
	 * @return a list of candiate templates
	 */
	public Seed initializeSeed(String seedName) {
		String domain = this.getTemplateLibraryDomain();
		// String seedUuid = UuidGen.generateAUuid(seedName);
		String seedFile = seedName + ".owl";

		Seed seed = wfac.getSeed(domain, seedFile);
		seed.setSeedId(this.request_id);
		// seed.setSeedId(seedUuid);

		this.setCurrentSeed(seed);

		if (storeProvenance)
			wgpc.addSeedToProvenanceCatalog(seed);

		return seed;
	}

	public Template initializeTemplate(String templateName) {
		String domain = this.getTemplateLibraryDomain();
		// String seedUuid = UuidGen.generateAUuid(seedName);
		String templateFile = templateName + ".owl";

		Template template = wfac.getTemplate(domain, templateFile);

		return template;
	}

	public ArrayList<Seed> findCandidateSeeds(Seed seed) {
		// Just copy over the seed for now
		// TODO: for composite seeds: put in all the atomic seeds

		seed.setID(UuidGen.generateAUuid(seed.getName()));

		ArrayList<Seed> result = new ArrayList<Seed>(1);
		result.add(seed);

		return result;
	}

	/**
	 * modifies the template by replacing abstract component (if any) with a
	 * concrete component and adds constraints. Note: For component sets: Union
	 * the variables, and intersect the constraints
	 * 
	 * @param template
	 *            the template to modify
	 * @param node
	 *            the node to modify
	 * @param cmrs
	 *            an array of ComponentMapAndRequirements (the component,
	 *            inputMaps, outputMaps, and constraints)
	 */
	private boolean modifyTemplate(Template template, Node node, ComponentMapsAndRequirements[] cmrs) {

		Node tNode = template.getNode(node.getID());

		HashSet<String> curbindingIds = new HashSet<String>();
		if (tNode.getComponentVariable().getBinding() != null)
			for (WingsSet b : tNode.getComponentVariable().getBinding()) {
				curbindingIds.add(((Binding) b).getID());
			}

		// Combine all cmrs into one cmr:
		// -- union of variables (maps), and intersection of their constraints
		// (redboxes)

		HashMap<String, Variable> uIMap = new HashMap<String, Variable>();
		HashMap<String, Variable> uOMap = new HashMap<String, Variable>();
		ArrayList<KBTriple> uRedbox = new ArrayList<KBTriple>();
		Binding uBinding = null;

		int i = 0;
		for (ComponentMapsAndRequirements cmr : cmrs) {
			ComponentVariable component = cmr.getComponent();

			// FIXME: Handle Template Bindings ?
			if (!curbindingIds.isEmpty() && !curbindingIds.contains(component.getComponentType()))
				continue;
			if (!component.isTemplate()) {
				if (uBinding == null)
					uBinding = new Binding();
				uBinding.add(new Binding(component.getComponentType()));
			} else {
				if (uBinding == null)
					uBinding = new ValueBinding();
				uBinding.add(new ValueBinding(component.getTemplate()));
			}

			HashMap<String, Variable> iMap = cmr.getStringIndexedInputMaps();
			HashMap<String, Variable> oMap = cmr.getStringIndexedOutputMaps();

			if (i > 0) {
				// TODO: Mark variables that are not common
			}

			uIMap.putAll(iMap);
			uOMap.putAll(oMap);

			ArrayList<KBTriple> redbox = cmr.getRequirements();
			ArrayList<String> redboxStr = new ArrayList<String>();
			for (KBTriple kbTriple : redbox) {
				redboxStr.add(kbTriple.fullForm());
			}

			if (i == 0) {
				uRedbox.addAll(redbox);
			} else {
				for (KBTriple con : new ArrayList<KBTriple>(uRedbox)) {
					if (!redboxStr.contains(con.fullForm())) {
						// System.out.println(component.getComponentTypeName()+": Removing constraint: "+con);
						uRedbox.remove(con);
					}
				}
			}
			i++;
		}
		if (uBinding == null)
			return false;

		tNode.getComponentVariable().setBinding(uBinding);

		HashMap<String, String> idChanged = new HashMap<String, String>();
		// Specialize Link ComponentParams, & Create New Links if there are any
		// *New* variables introduced
		HashMap<String, Role> nodeRoles = new HashMap<String, Role>();
		for (Port p : node.getInputPorts())
			nodeRoles.put(p.getRole().getID(), p.getRole());
		for (Port p : node.getOutputPorts())
			nodeRoles.put(p.getRole().getID(), p.getRole());

		for (String iscp : uIMap.keySet()) {
			Variable ivar = uIMap.get(iscp);
			Role ir = nodeRoles.get(iscp);
			if (ir == null) {
				// If new Input argument found, then create a new Input Link
				ir = new Role(iscp);
				nodeRoles.put(iscp, ir);

				// Create a new variable
				String varid = template.getNamespace() + ivar.getName();
				// make sure that the output variable id is unique
				if (template.getVariable(varid) != null) {
					int count = 1;
					while (template.getVariable(varid + count) != null)
						count++;
					varid = varid + count;
				}
				idChanged.put(ivar.getID(), varid);
				ivar.setID(varid);

				int count = 1;
				while (node.findInputPort(node.getNamespace() + "ip" + count) != null)
					count++;
				Port ip = new Port(node.getNamespace() + "ip" + count);
				ip.setRole(ir);

				template.addLink(null, node, null, ip, ivar);
			} else {
				// Copy over data object binding to existing variable
				Variable itvar = template.getVariable(ivar.getID());
				if (itvar != null) {
					itvar.setBinding(ivar.getBinding());
				}
			}
		}

		for (String oscp : uOMap.keySet()) {
			Variable ovar = uOMap.get(oscp);
			Role or = nodeRoles.get(oscp);
			if (or == null) {
				// If new Output argument found, then create a new Output Link
				or = new Role(oscp);
				nodeRoles.put(oscp, or);

				String varid = template.getNamespace() + ovar.getName();
				// make sure that the output variable id is unique
				if (template.getVariable(varid) != null) {
					int count = 1;
					while (template.getVariable(varid + count) != null)
						count++;
					varid = varid + count;
				}
				idChanged.put(ovar.getID(), varid);
				ovar.setID(varid);

				int count = 1;
				while (node.findOutputPort(node.getNamespace() + "op" + count) != null)
					count++;
				Port op = new Port(node.getNamespace() + "op" + count);
				op.setRole(or);

				template.addLink(node, null, op, null, ovar);
			} else {
				// Copy over data object binding to existing variable
				Variable otvar = template.getVariable(ovar.getID());
				if (otvar != null) {
					otvar.setBinding(ovar.getBinding());
				}
			}
		}

		ArrayList<KBTriple> newRedBox = new ArrayList<KBTriple>();
		for (KBTriple kbTriple : uRedbox) {
			if (idChanged.containsKey(kbTriple.getSubject().getID())) {
				String newid = idChanged.get(kbTriple.getSubject().getID());
				kbTriple.setSubject(template.getConstraintEngine().getResource(newid));
			}
			if (idChanged.containsKey(kbTriple.getObject().getID())) {
				String newid = idChanged.get(kbTriple.getObject().getID());
				kbTriple.setObject(template.getConstraintEngine().getResource(newid));
			}
			newRedBox.add(kbTriple);
		}

		template.getConstraintEngine().addConstraints(newRedBox);
		return true;
	}

	private LogEvent getEvent(String evid) {
		return new LogEvent(evid, "Wings", LogEvent.REQUEST_ID, this.request_id);
	}

	/**
	 * Step 2
	 * 
	 * @param template
	 *            a candiate template from step 1.
	 * @return a list of specialized templates customized to seed constraints
	 */
	public ArrayList<Template> specializeTemplates(Template template) {
		LogEvent event = getEvent(LogEvent.EVENT_WG_SPECIALIZE);
		logger.info(event.createStartLogMsg().addWQ(LogEvent.TEMPLATE, "" + template));

		ComponentCatalog pc = this.getPc();

		ArrayList<Template> templates = new ArrayList<Template>();
		ArrayList<Template> processedTemplates = new ArrayList<Template>();
		//ArrayList<Template> rejectedTemplates = new ArrayList<Template>();

		HashMap<Template, ArrayList<String>> done = new HashMap<Template, ArrayList<String>>();

		if(template  == null) return templates;
		
		Template tmp = template.createCopy();
		tmp.setID(UuidGen.generateAUuid(template.getName()));
		templates.add(tmp);

		while (!templates.isEmpty()) {
			logger.info(event.createLogMsg().addList(LogEvent.QUEUED_TEMPLATES, templates));
			logger.info(event.createLogMsg().addList(LogEvent.SPECIALIZED_TEMPLATES_Q, processedTemplates));

			Template currentTemplate = templates.remove(0);

			ArrayList<String> nodesDone = done.get(currentTemplate);
			if (nodesDone == null) {
				nodesDone = new ArrayList<String>();
			}

			ArrayList<Link> links = new ArrayList<Link>();
			Link[] linkArray = currentTemplate.getOutputLinks();
			for (Link link : linkArray) {
				links.add(link);
			}

			while (!links.isEmpty()) {
				HashMap<Role, Variable> inputMaps = new HashMap<Role, Variable>();
				HashMap<Role, Variable> outputMaps = new HashMap<Role, Variable>();

				Link currentLink = links.remove(0);
				if (currentLink.isInputLink()) {
					// continue;
					// no op
				} else {
					outputMaps.put(currentLink.getOriginPort().getRole(), currentLink.getVariable());
					ArrayList<String> variableIds = new ArrayList<String>();
					Node originNode = currentLink.getOriginNode();

					Link[] outputLinks = currentTemplate.getOutputLinks(originNode);
					for (Link outputLink : outputLinks) {
						Variable variable = outputLink.getVariable();
						outputMaps.put(outputLink.getOriginPort().getRole(), variable);
						variableIds.add(variable.getID());
						links.remove(outputLink);
					}

					Link[] inputLinks = currentTemplate.getInputLinks(originNode);
					for (Link inputLink : inputLinks) {
						Variable variable = inputLink.getVariable();
						inputMaps.put(inputLink.getDestinationPort().getRole(), variable);
						variableIds.add(variable.getID());
						links.add(inputLink);
					}

					if (nodesDone.contains(originNode.getID())) {
						// continue;
						// no op
					} else {
						ArrayList<KBTriple> redBox = currentTemplate.getConstraintEngine().getConstraints(variableIds);

						ComponentVariable component = originNode.getComponentVariable();
						if (component.isTemplate())
							pc = this.tc;
						else
							pc = this.pc;

						ComponentMapsAndRequirements sentMapsAndRequirements = new ComponentMapsAndRequirements(component, inputMaps, outputMaps, redBox);

						if (logger.isInfoEnabled()) {
							HashMap<String, Object> args = new HashMap<String, Object>();
							args.put("component", component);
							args.put("inputMaps", inputMaps);
							args.put("outputMaps", outputMaps);
							args.put("redBox", redBox);
							logger.info(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "2.1").addMap(LogEvent.QUERY_ARGUMENTS, args));
						}
						ArrayList<ComponentMapsAndRequirements> allcmrs = pc.specializeAndFindDataRequirements(sentMapsAndRequirements);
						ArrayList<ComponentMapsAndRequirements> componentMapsAndRequirements = new ArrayList<ComponentMapsAndRequirements>();
						for(ComponentMapsAndRequirements cmr : allcmrs) {
							this.addExplanations(cmr.getExplanations());
							if(!cmr.getInvalidFlag())
								componentMapsAndRequirements.add(cmr);
							else {
								//Template t = currentTemplate.createCopy();
								//rejectedTemplates.add(t);
							}
						}
						// add to the workflow generation provenance catalog
						if (this.storeProvenance)
							wgpc.addQuery2point1ToProvenanceCatalog(getCurrentSeed().getSeedId(), sentMapsAndRequirements, componentMapsAndRequirements);

						if (componentMapsAndRequirements.isEmpty()) {
							logger.warn(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "2.1").addWQ(LogEvent.QUERY_RESPONSE, LogEvent.NO_MATCH));
							currentTemplate = null;
							break;
						} else {
							if (logger.isInfoEnabled()) {
								ArrayList<ComponentVariable> components = new ArrayList<ComponentVariable>();
								for (ComponentMapsAndRequirements componentMapsAndRequirement : componentMapsAndRequirements) {
									components.add(componentMapsAndRequirement.getComponent());
								}
								logger.info(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "2.1").addList(LogEvent.QUERY_RESPONSE + ".components",
										components));
							}

							nodesDone.add(originNode.getID());
							done.put(currentTemplate, nodesDone);
							String currentTemplateName = currentTemplate.getName();

							// note this is over the rest of the cmrs
							ComponentSetCreationRule crule = originNode.getComponentSetRule();
							if (crule == null || crule.getType() == SetType.WTYPE) {
								for (int i = 1; i < componentMapsAndRequirements.size(); i++) {
									ComponentMapsAndRequirements cmr = componentMapsAndRequirements.get(i);
									this.addExplanations(cmr.getExplanations());
									Template specializedTemplate = currentTemplate.createCopy();
									specializedTemplate.setID(UuidGen.generateAUuid(currentTemplateName));
									Node specializedNode = specializedTemplate.getNode(originNode.getID());
									boolean ok = this.modifyTemplate(specializedTemplate, specializedNode,
											new ComponentMapsAndRequirements[] { componentMapsAndRequirements.get(i) });
									if (ok) {
										templates.add(specializedTemplate);
									}
									done.put(specializedTemplate, new ArrayList<String>(nodesDone));
								}
								ComponentMapsAndRequirements firstCmr = componentMapsAndRequirements.get(0);
								boolean ok = this.modifyTemplate(currentTemplate, originNode, new ComponentMapsAndRequirements[] { firstCmr });
								if (!ok) {
									currentTemplate = null;
									break;
								}
							} else if (crule != null && crule.getType() == SetType.STYPE) {
								boolean ok = this.modifyTemplate(currentTemplate, originNode, componentMapsAndRequirements
										.toArray(new ComponentMapsAndRequirements[0]));
								if (!ok) {
									currentTemplate = null;
									break;
								}
							}
						}
					}
				}
			}
			if (currentTemplate != null) {
				currentTemplate.autoUpdateRoles(); // If any new input/output
													// variables have been
													// created
				currentTemplate.fillInDefaultSetCreationRules();

				processedTemplates.add(currentTemplate);
			}
		}
		// System.out.println("****** Specialization Rejected "+rejectedTemplates.size());
		logger.info(event.createEndLogMsg().addWQ(LogEvent.TEMPLATE, "" + template));
		return processedTemplates;
	}
	
	
	/**
	 * Helper function to Group variable object bindings by the variable ids passed in
	 * 
	 * @param listOfVariableObjectBindings 
	 * 			the list of variable object bindings received from the Data Catalog
	 * @param groupByVarIds
	 * 			list of variable ids that the bindings should be grouped by
	 * 
	 * @return the list of variable object bindings grouped by groupByIds 
	 */
	
	private ArrayList<ArrayList<DataVariableDataObjectBinding>> groupVariableDataObjectMappings(
			ArrayList<ArrayList<DataVariableDataObjectBinding>>listsOfVariableDataObjectMappings, 
			ArrayList<String> groupByVarIds, ArrayList<String> variableIds) {
		HashMap<String, ArrayList<DataVariableDataObjectBinding>> bindingsByGroupByVarIds = 
			new HashMap<String, ArrayList<DataVariableDataObjectBinding>>();
		
		for (ArrayList<DataVariableDataObjectBinding> mapping : listsOfVariableDataObjectMappings) {
			HashMap<String, HashSet<KBObject>> varBindings = new HashMap<String, HashSet<KBObject>>();
			for (DataVariableDataObjectBinding dvobinding : mapping) {
				KBObject dv = dvobinding.getDataVariable();
				HashSet<KBObject> objs = dvobinding.getDataObjects();
				varBindings.put(dv.getID(), objs);
			}
			String groupKey = "";
			for(String varId: groupByVarIds) {
				groupKey += varBindings.get(varId)+"|";
			}
			
			ArrayList<DataVariableDataObjectBinding> om = bindingsByGroupByVarIds.get(groupKey);
			if(om == null) {
				om = mapping;
			}
			else {
				for (DataVariableDataObjectBinding dvobinding : om) {
					KBObject dv = dvobinding.getDataVariable();
					HashSet<KBObject> obj = dvobinding.getDataObjects();
					if(variableIds.contains(dv.getID()) && !groupByVarIds.contains(dv.getID())) {
						obj.addAll(varBindings.get(dv.getID()));
					}
					varBindings.put(dv.getID(), obj);
					dvobinding.setDataObjects(obj);
				}
			}
			bindingsByGroupByVarIds.put(groupKey, om);
		}
		ArrayList<ArrayList<DataVariableDataObjectBinding>> groupedList = new  ArrayList<ArrayList<DataVariableDataObjectBinding>>();
		for(ArrayList<DataVariableDataObjectBinding> vals : bindingsByGroupByVarIds.values()) {
			groupedList.add(vals);
		}
		return groupedList;
	}

	
	/**
	 * Helper function to Filter variable object bindings by any explicit binding constraint specified in the template
	 * 
	 * @param listOfVariableObjectBindings 
	 * 			the list of variable object bindings received from the Data Catalog
	 * @param groupByVarIds
	 * 			list of variable ids that the bindings should be grouped by
	 * 
	 * @return the list of variable object bindings grouped by groupByIds 
	 */
	
	private ArrayList<ArrayList<DataVariableDataObjectBinding>> filterVariableDataObjectMappings(
			ArrayList<ArrayList<DataVariableDataObjectBinding>>fullList, ArrayList<String> variableIds, 
			HashMap<String, HashSet<String>> userBindings,
			HashMap<String, ArrayList<String>> varEquality,
			HashMap<String, ArrayList<String>> varInequality
			) {
		ArrayList<ArrayList<DataVariableDataObjectBinding>> filteredList = new  ArrayList<ArrayList<DataVariableDataObjectBinding>>();
		
		for (ArrayList<DataVariableDataObjectBinding> mapping : fullList) {
			HashMap<String, String> varBindings = new HashMap<String, String>();
			for (DataVariableDataObjectBinding dvobinding : mapping) {
				KBObject dv = dvobinding.getDataVariable();
				HashSet<KBObject> objs = dvobinding.getDataObjects();
				for(KBObject obj: objs)
					varBindings.put(dv.getID(), obj.getID());
			}
			
			boolean ok = true;
			for (String dvid : variableIds) {
				String dvB = varBindings.get(dvid);

				// Check that the dc provided bindings follow any user defined
				// databinding constraints
				HashSet<String> udvB = userBindings.get(dvid);
				if (udvB != null) {
					if (!udvB.contains(dvB)) {
						ok = false;
						break;
					}
				}

				// Check that the dc provided bindings follow any varEquality
				// and varInequality constraints
				ArrayList<String> eqvars = varEquality.get(dvid);
				if (eqvars != null) {
					for (String eqv : eqvars) {
						if (!dvB.equals(varBindings.get(eqv))) {
							ok = false;
							break;
						}
					}
					if (!ok)
						break;
				}
				ArrayList<String> ineqvars = varInequality.get(dvid);
				if (ineqvars != null) {
					for (String ineqv : ineqvars) {
						if (dvB.equals(varBindings.get(ineqv))) {
							ok = false;
							break;
						}
					}
					if (!ok)
						break;
				}
			}
			if(ok) filteredList.add(mapping);
		}
		return filteredList;
	}
	

	/**
	 * step 3
	 * 
	 * @param specializedTemplate
	 *            a specialized template
	 * @return a list of partially specified template instances - input data
	 *         objects bound
	 */
	public ArrayList<Template> selectInputDataObjects(Template specializedTemplate) {

		LogEvent event = this.curLogEvent;
		if (event == null) {
			event = this.getEvent(LogEvent.EVENT_WG_DATA_SELECTION);
		}
		logger.info(event.createLogMsg().addWQ(LogEvent.TEMPLATE, "" + specializedTemplate));

		DataCatalog dc = this.getDc();
		ArrayList<Template> boundTemplates = new ArrayList<Template>();

		Variable[] variables = specializedTemplate.getVariables();
		ArrayList<String> blacklist = new ArrayList<String>(variables.length);
		for (Variable variable : variables) {
			blacklist.add(variable.getID());
		}
		
		// Data Filtering properties
		String hdbPropId = PropertiesHelper.getWorkflowOntologyURL() + "#hasDataBinding";
		blacklist.add(hdbPropId);
		
		Variable[] inputVariables = specializedTemplate.getInputVariables();
		ArrayList<String> inputVariableIds = new ArrayList<String>();
		ArrayList<String> nonCollectionIds = new ArrayList<String>();
		
		HashMap<String, HashSet<String>> varUserBindings = new HashMap<String, HashSet<String>>();
		HashMap<String, ArrayList<String>> varEquality = new HashMap<String, ArrayList<String>>();
		HashMap<String, ArrayList<String>> varInequality = new HashMap<String, ArrayList<String>>();
		
		for (Variable inputVariable : inputVariables) {
			if (inputVariable.isDataVariable()) {				
				Role r = specializedTemplate.getInputRoleForVariable(inputVariable);
				if(r.getDimensionality()==0) {
					nonCollectionIds.add(inputVariable.getID());
				} else if(r.getDimensionality() > 1) {
					System.err.println("Warning: No Support for "+
							r.getDimensionality()+"-D Template Input roles ("+r.getName()+")");
					continue;
				}
				String variableId = inputVariable.getID();
				inputVariableIds.add(variableId);
				blacklist.remove(variableId);
				
				Binding b = inputVariable.getBinding();
				if(b != null) {
					HashSet<String> userBindings = new HashSet<String>();
					if(b.isSet()) {
						if(b.getMaxDimension() > 1) { 
							System.err.println("Warning: No Support for "+b.getMaxDimension()+"-D Input Data");
							continue;
						}
						for(WingsSet s : b) userBindings.add(((Binding)s).getID());
					} else {
						userBindings.add(b.getID());
					}
					varUserBindings.put(variableId, userBindings);
				}
			}
		}
		Collections.sort(nonCollectionIds);

		ConstraintEngine engine = specializedTemplate.getConstraintEngine();
		for (String id : blacklist)
			engine.addBlacklistedId(id);
		ArrayList<KBTriple> allInputConstraints = engine.getConstraints(inputVariableIds);
		for (String id : blacklist)
			engine.removeBlacklistedId(id);

		ArrayList<KBTriple> inputConstraints = new ArrayList<KBTriple>();
		for(KBTriple t : allInputConstraints) {
			if(t.getPredicate().getID().equals(PropertiesHelper.getWorkflowOntologyURL()+"#hasSameDataAs")) {
				String var1 = t.getObject().getID();
				String var2 = t.getSubject().getID();
				ArrayList<String> equality1 = varEquality.get(var1);
				ArrayList<String> equality2 = varEquality.get(var2);
				if(equality1 == null) equality1 = new ArrayList<String>();
				if(equality2 == null) equality2 = new ArrayList<String>();
				equality1.add(var2); equality2.add(var1);
				varEquality.put(var1, equality1); varEquality.put(var2, equality2);
			}
			else if(t.getPredicate().getID().equals(PropertiesHelper.getWorkflowOntologyURL()+"#hasDifferentDataFrom")) {
				String var1 = t.getObject().getID();
				String var2 = t.getSubject().getID();
				ArrayList<String> inequality1 = varInequality.get(var1);
				ArrayList<String> inequality2 = varInequality.get(var2);
				if(inequality1 == null) inequality1 = new ArrayList<String>();
				if(inequality2 == null) inequality2 = new ArrayList<String>();
				inequality1.add(var2); inequality2.add(var1);
				varInequality.put(var1, inequality1); varInequality.put(var2, inequality2);
			}
			else {
				inputConstraints.add(t);
			}
		}
		
		logger.info(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "3.1").addList(LogEvent.QUERY_ARGUMENTS, inputConstraints));

		// System.out.println(inputConstraints);
		this.addExplanation("Querying the DataCatalog with the following constraints: <br/>"+
				inputConstraints.toString().replaceAll(",", "<br/>"));
		ArrayList<ArrayList<DataVariableDataObjectBinding>> listsOfVariableDataObjectMappings = dc.findDataSources(inputConstraints, false);

		if (this.storeProvenance)
			wgpc.addQuery3point1ToProvenanceCatalog(getCurrentSeed().getSeedId(), inputConstraints, false, listsOfVariableDataObjectMappings);

		if (listsOfVariableDataObjectMappings == null || listsOfVariableDataObjectMappings.isEmpty()) {
			logger.warn(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "3.1").addWQ(LogEvent.QUERY_RESPONSE, LogEvent.NO_MATCH));
			this.addExplanation("ERROR: The DataCatalog did not return any matching datasets");
		} else {
			logger.info(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "3.1").addList(LogEvent.QUERY_RESPONSE, listsOfVariableDataObjectMappings));
			
			// Filter Datasets
			ArrayList<ArrayList<DataVariableDataObjectBinding>> filteredList = 
				filterVariableDataObjectMappings(listsOfVariableDataObjectMappings, inputVariableIds, 
						varUserBindings, varEquality, varInequality);
			
			// Group Datasets
			ArrayList<ArrayList<DataVariableDataObjectBinding>> groupedList = 
				groupVariableDataObjectMappings(filteredList, nonCollectionIds, inputVariableIds);
			//System.out.println(groupedList.size());
			//System.out.println(groupedList);
			
			// New Template for each group
			for (ArrayList<DataVariableDataObjectBinding> mapping : groupedList) {
				HashMap<String, HashSet<String>> variableBindings = new HashMap<String, HashSet<String>>();
				Template t = specializedTemplate.createCopy();
				t.setID(UuidGen.generateAUuid(t.getName()));
				
				for (DataVariableDataObjectBinding dvobinding : mapping) {
					KBObject dv = dvobinding.getDataVariable();
					HashSet<KBObject> objs = dvobinding.getDataObjects();
					
					// Ignore temporary variables (not input data variables)
					if(!inputVariableIds.contains(dv.getID())) {
						// This could be a temporary variable (skolem variable) created by component rules.
						// Remove all constraints for the temporary variable as it has fulfilled it's purpose i.e. data selection)
						t.getConstraintEngine().removeObjectAndConstraints(dv);
						
						// Replace all occurences of it in the constraint engine with the bound value
						/*for (KBObject obj : objs) {
							t.getConstraintEngine().replaceSubjectInConstraints(dv, obj);
							t.getConstraintEngine().replaceObjectInConstraints(dv, obj);
						}*/
						continue;
					}

					Variable v = specializedTemplate.getVariable(dv.getID());
					HashSet<String> tmp = variableBindings.get(v.getID());
					if (tmp == null)
						tmp = new HashSet<String>();
					
					for(KBObject obj: objs)
						tmp.add(obj.getID());

					variableBindings.put(v.getID(), tmp);
				}
				
				Variable[] ivs = t.getInputVariables();
				boolean unboundDataVariableP = false;
				for (Variable iv : ivs) {
					if (iv.isDataVariable()) {
						HashSet<String> bindingIds = variableBindings.get(iv.getID());
						if (bindingIds != null && !bindingIds.isEmpty()) {
							Binding b = new Binding();

							Binding ivb = iv.getBinding();
							if (ivb != null) {
								
								// Check the case where the template already has a binding
								boolean ok = false;
								if (!ivb.isSet()) {
									if (bindingIds.contains(ivb.getID())) {
										b = (Binding) SerializableObjectCloner.clone(ivb);
										ok = true;
									} else {
										engine.addBlacklistedId(hdbPropId);
										this.addExplanation(ivb.getName()+" cannot be bound to "+iv.getID() +
												" because "+iv.getID()+" needs to satisfy the following constraints: " + 
												engine.getConstraints(iv.getID())+"\n");
										engine.removeBlacklistedId(hdbPropId);
									}
								} else {
									for (WingsSet s : ivb) {
										if (bindingIds.contains(((Binding) s).getID())) {
											b.add((Binding) SerializableObjectCloner.clone(s));
											ok = true;
										} else {
											engine.addBlacklistedId(hdbPropId);
											this.addExplanation(ivb.getName()+" cannot be bound to "+iv.getID() +
													" because "+iv.getID()+" needs to satisfy the following constraints: " + 
													engine.getConstraints(iv.getID())+"\n");
											engine.removeBlacklistedId(hdbPropId);
										}
									}
								}
								if (ok)
									iv.setBinding(b);
								else {
									unboundDataVariableP = true;
									iv.setBinding(null);
									break;
								}
							} else {
								for (String bindingId : bindingIds)
									b.add(new Binding(bindingId));
								iv.setBinding(b);
							}
							// System.out.println(iv.getName() + ": " + iv.getBinding());
						} else {
							unboundDataVariableP = true;
							break;
						}
					}
				}
				if (!unboundDataVariableP) {
					boundTemplates.add(t);
				}
			}
		}

		return boundTemplates;
	}

	/**
	 * Step 4: 4.1 sets the data metrics for the partially instantiated
	 * candidate instances
	 * 
	 * @param partialCandidateInstances
	 *            a list of candidate instances with input data variables bound
	 */
	public void setDataMetricsForInputDataObjects(ArrayList<Template> partialCandidateInstances) {
		HashMap<String, String> dataObjectNameToDataMetricsMap = new HashMap<String, String>();
		LogEvent event = curLogEvent;

		for (Template partialCandidateInstance : partialCandidateInstances) {
			Variable[] inputVariables = partialCandidateInstance.getInputVariables();
			for (Variable inputVariable : inputVariables) {
				if (inputVariable.isDataVariable()) {
					Binding binding = inputVariable.getBinding();
					setBindingMetrics(binding, dataObjectNameToDataMetricsMap, event);
				}
			}
		}
	}

	private void setBindingMetrics(Binding binding, HashMap<String, String> metricsMap, LogEvent event) {
		if (binding == null)
			return;

		if (binding.isSet()) {
			for (WingsSet b : binding) {
				setBindingMetrics((Binding) b, metricsMap, event);
			}
			return;
		}

		if (!binding.getID().startsWith(this.dataNS))
			return;

		String dataObjectName = binding.getName();
		String xml = metricsMap.get(dataObjectName);
		if (xml == null) {
			String dataObjectId = binding.getID();
			if (event != null)
				logger.info(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "4.1").addWQ(LogEvent.QUERY_ARGUMENTS, dataObjectId));
			xml = dc.findDataMetricsForDataObject(dataObjectId);
			if (xml != null) {
				metricsMap.put(dataObjectName, xml);
				if (this.storeProvenance)
					wgpc.setDataMetrics(getCurrentSeed().getSeedId(), dataObjectName, xml);
				if (event != null)
					logger.info(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "4.1").addWQ(LogEvent.QUERY_RESPONSE, "<metrics not shown>"));
			} else if (event != null) {
				logger.warn(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "4.1").addWQ(LogEvent.QUERY_RESPONSE, LogEvent.NO_MATCH));
			}
		}
		binding.setMetrics(xml);
	}

	public DAX getTemplateDAX(Template template) {
		LogEvent event = getEvent(LogEvent.EVENT_WG_GET_DAX);
		logger.info(event.createStartLogMsg().addWQ(LogEvent.TEMPLATE, "" + template));

		ComponentCatalog pc = this.getPc();

		// String templateName = template.getName();
		// String daxId = UuidGen.generateAUuid(templateName + "-dax");
		String daxId = this.request_id;
		DAX dax = new DAX(daxId, PropertiesHelper.getOutputFormat());

		int jobCounter = 0;

		ArrayList<Node> nodesDone = new ArrayList<Node>();

		ArrayList<Link> links = new ArrayList<Link>();
		for (Link link : template.getInputLinks()) {
			links.add(link);
		}
		while (!links.isEmpty()) {
			Link currentLink = links.remove(0);
			if (!currentLink.isOutputLink()) {
				Node destNode = currentLink.getDestinationNode();

				if (!nodesDone.contains(destNode)) {
					nodesDone.add(destNode);

					ComponentVariable component = destNode.getComponentVariable();

					JobNode jobNode = new JobNode(destNode.getID());

					// Check component's bindings
					// - For each binding, get it's portBindings
					// - Create a mapsAndRequirements object for each component
					// binding
					// and get it's appropriate argument string

					ArrayList<Binding> cbindings = new ArrayList<Binding>();
					cbindings.add(component.getBinding());

					while (!cbindings.isEmpty()) {
						Binding b = cbindings.remove(0);
						if (b.isSet()) {
							for (WingsSet s : b) {
								cbindings.add((Binding) s);
							}
						} else {
							HashMap<Role, Variable> inputMaps = new HashMap<Role, Variable>();
							HashMap<Role, Variable> outputMaps = new HashMap<Role, Variable>();

							ArrayList<String> variableIds = new ArrayList<String>();
							ArrayList<Binding> inputDataObjects = new ArrayList<Binding>();
							ArrayList<Binding> outputDataObjects = new ArrayList<Binding>();

							PortBindingList pb = (PortBindingList) b.getData();

							Link[] outputLinks = template.getOutputLinks(destNode);
							for (Link outputLink : outputLinks) {
								Variable variable = outputLink.getVariable();
								Variable v = new Variable(variable.getID(), variable.getVariableType());
								Binding xb = getPortBinding(pb.getPortBinding(), outputLink.getOriginPort());
								v.setBinding(xb);
								//variable.setBinding(xb);
								outputMaps.put(outputLink.getOriginPort().getRole(), v);
								variableIds.add(v.getID());
								if (v.isDataVariable()) {
									outputDataObjects.add(xb);
								}
								links.add(outputLink);
							}

							Link[] inputLinks = template.getInputLinks(destNode);
							for (Link inputLink : inputLinks) {
								Variable variable = inputLink.getVariable();
								Variable v = new Variable(variable.getID(), variable.getVariableType());
								Binding xb = getPortBinding(pb.getPortBinding(), inputLink.getDestinationPort());
								v.setBinding(xb);
								//variable.setBinding(xb);
								inputMaps.put(inputLink.getDestinationPort().getRole(), v);
								variableIds.add(v.getID());
								if (v.isDataVariable()) {
									if (inputLink.getOriginNode() != null) {
										String originNodeId = inputLink.getOriginNode().getID();
										if (!jobNode.parentNodeIds.contains(originNodeId))
											jobNode.parentNodeIds.add(originNodeId);
									}
									inputDataObjects.add(xb);
								}
								links.remove(inputLink);
							}

							ArrayList<KBTriple> relevantConstraints = template.getConstraintEngine().getConstraints(variableIds);

							ComponentVariable c = new ComponentVariable(b.getID());
							c.setComponentType(b.getID());

							if (logger.isInfoEnabled()) {
								HashMap<String, Object> args = new HashMap<String, Object>();
								args.put("component", c);
								args.put("inputMaps", inputMaps);
								args.put("outputMaps", outputMaps);
								logger.info(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "4.5").addMap(LogEvent.QUERY_ARGUMENTS, args));
							}

							ComponentMapsAndRequirements mapsAndRequirements = new ComponentMapsAndRequirements(c, inputMaps, outputMaps, relevantConstraints);

							String argumentString = pc.getInvocationCommand(mapsAndRequirements);

							if (argumentString == null) {
								logger.warn(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "4.5").addWQ(LogEvent.QUERY_RESPONSE, LogEvent.NO_MATCH));
								return null;
							}

							logger.info(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "4.5").addWQ(LogEvent.QUERY_RESPONSE, argumentString));

							if (this.storeProvenance)
								wgpc.addQuery4point5ToProvenanceCatalog(getCurrentSeed().getSeedId(), mapsAndRequirements, argumentString);

							String jobName = c.getName();
							//String jobNS = c.getNamespace();
							String jobNS = PropertiesHelper.getPCDomain();

							if (c.getComponentType() != null) {
								ComponentVariable tmpComp = new ComponentVariable(c.getComponentType());
								jobName = tmpComp.getName();
								//jobNS = tmpComp.getNamespace();
								jobNS = PropertiesHelper.getPCDomain();
							}

							String jobId = UuidGen.generateAUuid("Job" + jobCounter);
							Job job = new Job(jobId, jobName, "", jobNS, argumentString, new ArrayList<String>());
							job.getInputFiles().addAll(inputDataObjects);
							job.getOutputFiles().addAll(outputDataObjects);

							logger.info(event.createLogMsg().addWQ(LogEvent.JOB_ID, jobId).addList(LogEvent.JOB_INPUTS, job.getInputFiles()).addList(
									LogEvent.JOB_OUTPUTS, job.getOutputFiles()));

							if (this.storeProvenance)
								wgpc.storeJobInformation(getCurrentSeed().getSeedId(), daxId, jobId, mapsAndRequirements);

							dax.addJob(job);
							jobNode.getJobIds().add(jobId);
							dax.addNode(jobNode);
							jobCounter++;
						}
					}
				}
			}
		}
		dax.fillInParentJobs();

		logger.info(event.createEndLogMsg().addWQ(LogEvent.TEMPLATE, "" + template));
		return dax;
	}

	private Binding getPortBinding(PortBinding pb, Port port) {
		for (Port p : pb.keySet()) {
			if (p.getID().equals(port.getID()))
				return pb.get(p);
		}
		return null;
	}

	private ComponentMapsAndRequirements cloneCMRBindings(ComponentMapsAndRequirements cmr) {
		// TODO: Clone cmr before sending ? We basically need to have separate
		// variable bindings
		HashMap<Role, Variable> inmap = new HashMap<Role, Variable>();
		HashMap<Role, Variable> outmap = new HashMap<Role, Variable>();
		for (Role r : cmr.inputMaps.keySet()) {
			Variable invar = cmr.inputMaps.get(r);
			Variable var = new Variable(invar.getID(), invar.getVariableType());
			if(invar.getBinding() != null)
				var.setBinding((Binding) invar.getBinding().clone());
			inmap.put(r, var);
		}
		for (Role r : cmr.outputMaps.keySet()) {
			Variable outvar = cmr.outputMaps.get(r);
			Variable var = new Variable(outvar.getID(), outvar.getVariableType());
			if(outvar.getBinding() != null)
				var.setBinding((Binding) outvar.getBinding().clone());
			outmap.put(r, var);
		}
		ComponentMapsAndRequirements pcmr = new ComponentMapsAndRequirements(cmr.getComponent(), inmap, outmap, cmr.getRequirements());
		pcmr.addExplanations(cmr.getExplanations());
		pcmr.setInvalidFlag(cmr.getInvalidFlag());
		return pcmr;
	}

	
	private KBObject fetchVariableTypeFromCMR(Variable v, ComponentMapsAndRequirements cmr) {
		KBObject vtype = null;
		for(KBTriple t : cmr.getRequirements()) {
			if(t.getSubject().getID().equals(v.getID()) && t.getPredicate().getName().equals("type")) {
				KBObject tobj = t.getObject();
				if(vtype == null) vtype = tobj;
				else if(dc.subsumes(vtype.toString(), tobj.toString())) {
					vtype = tobj;
				}
			}
		}
		return vtype;
	}
	// TODO: Add Another function for comparison purposes, where the components
	// are evaluated last
	// -- i.e. innermost loop

	/*
	 * Helper function - Takes input portbindinglist - Returns output
	 * portbindinglist
	 * 
	 * Currently ignores variable constraint forward propagation - relies on
	 * binding metrics forward propagation
	 * 
	 * Handles component sets inside destNode TODO: Handle multiple parameters
	 * and parameter SetCreationRules
	 * 
	 * TODO: Handle extra ports for specialized components (whether bindings
	 * exist for them or not)
	 */
	private PortBindingList configureBindings(PortBindingList ipblist, Node origNode, Node newNode, ComponentVariable component, ComponentMapsAndRequirements cmr,
			LogEvent event, HashMap<String, String> prospectiveIds) {
		
		PortBindingList pblist = new PortBindingList();

		// Handle the input port binding list
		if (ipblist.isList()) {
			// For all elements in the port binding list
			// - add a new component binding (component instantiation)
			// - call configureBindings
			// - add the returned output binding list to our own output
			// binding list
			
			Binding listb = new Binding();
			Binding origB = component.getBinding();
			for (PortBindingList ipbl : ipblist) {
				ComponentMapsAndRequirements pcmr = cloneCMRBindings(cmr);
				pcmr.setComponent(component);
				component.setBinding(origB);
				PortBindingList opbl = configureBindings(ipbl, origNode, newNode, component, pcmr, event, prospectiveIds);

				if ((opbl.isList() && !opbl.isEmpty()) || (!opbl.isList() && opbl.getPortBinding() != null)) {
					pblist.add(opbl);
					listb.add(component.getBinding());
					// System.out.println("***"+compBinding.hashCode()+"****"+childb.hashCode());
					// compBinding.add(childb);
					// System.out.println("-----"+compBinding);
				}
			}
			component.setBinding(listb);
		} else {
			// if(component.getBinding() == null) return opblist;

			// For all component bindings
			int len = component.getBinding().size();
			
			Binding allCB = component.getBinding();
			component.setBinding(new Binding());
			
			// System.out.println(component.getBinding() + ":" + ipblist);
			for (WingsSet cbs : allCB) {
				PortBindingList cpblist = new PortBindingList();
				Binding compBinding = (Binding) cbs;
				
				Binding cb;
				if (!component.isTemplate())
					cb = new Binding(compBinding.getID());
				else
					cb = new ValueBinding((Template) compBinding.getValue());

				// TODO: Should we check if cb itself is not a set ?? (Can we
				// have
				// pre-defined 2,3,..-D arrays of components ?
				// if(cb.isSet()) .. ??
				ComponentVariable c;
				if (component.isTemplate())
					c = new ComponentVariable(component.getTemplate());
				else {
					c = new ComponentVariable(component.getID());
					c.setComponentType(compBinding.getID());
				}
				c.setBinding(cb);

				ComponentMapsAndRequirements ccmr = cloneCMRBindings(cmr);
				ccmr.setComponent(c);

				PortBinding pb = ipblist.getPortBinding();

				HashMap<String, Port> newRolePort = new HashMap<String, Port>();
				for (Port p : newNode.getInputPorts())
					newRolePort.put(p.getRole().getID(), p);
				for (Port p : newNode.getOutputPorts())
					newRolePort.put(p.getRole().getID(), p);

				HashMap<String, Port> origRolePort = new HashMap<String, Port>();
				for (Port p : origNode.getInputPorts())
					origRolePort.put(p.getRole().getID(), p);
				for (Port p : origNode.getOutputPorts())
					origRolePort.put(p.getRole().getID(), p);

				// Create/modify the inputMaps, outputMaps
				for (Role r : ccmr.inputMaps.keySet()) {
					ccmr.inputMaps.get(r).setBinding(pb.get(origRolePort.get(r.getID())));
				}
				// for(Role r: cmr.outputMaps.keySet())
				// cmr.outputMaps.get(r).setBinding(pb.get(rolePort.get(r.getID())));

				if (logger.isInfoEnabled()) {
					HashMap<String, Object> args = new HashMap<String, Object>();
					args.put("component", ccmr.component);
					args.put("inputMaps", ccmr.inputMaps);
					args.put("outputMaps", ccmr.outputMaps);
					logger.info(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "4.2").addMap(LogEvent.QUERY_ARGUMENTS, args));
				}

				ComponentCatalog pc = this.getPc();
				if (ccmr.component.isTemplate())
					pc = this.tc;

				ArrayList<ComponentMapsAndRequirements> allcmrs = pc.findOutputDataPredictedDescriptions(ccmr);
				ArrayList<ComponentMapsAndRequirements> rcmr = new ArrayList<ComponentMapsAndRequirements>();
				for (ComponentMapsAndRequirements acmr : allcmrs) {
					this.addExplanations(acmr.getExplanations());
					if (!acmr.getInvalidFlag())
						rcmr.add(acmr);
					else {
						// Do something with the invalid components
					}
				}
				if (rcmr.isEmpty()) {
					logger.warn(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "4.2").addWQ(LogEvent.QUERY_RESPONSE, LogEvent.NO_MATCH));
					continue;
				} else {
					logger.info(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "4.2").addWQ(LogEvent.QUERY_RESPONSE,
							"Returned " + rcmr.size() + " responses"));

					// FIXME: Handle parameter sets properly ! (use rules too !)

					// Handle multiple return values
					for (int i = 0; i < rcmr.size(); i++) {
						ComponentMapsAndRequirements m = rcmr.get(i);

						PortBinding newpb = new PortBinding();
						// PortBinding opb = new PortBinding();
						for (Role r : m.inputMaps.keySet()) {
							Variable v = m.inputMaps.get(r);
							// System.out.println(r+"<="+v);
							newpb.put(newRolePort.get(r.getID()), v.getBinding());
						}

						String sortedInputs = getInputRoleStr(newpb, ccmr.component);
						for (Role r : m.outputMaps.keySet()) {
							Variable v = m.outputMaps.get(r);

							Binding b = v.getBinding();

							newpb.put(newRolePort.get(r.getID()), b);
							// opb.put(newRolePort.get(r.getID()), b);

							// Rename outputs
							ArrayList<Binding> dbs = new ArrayList<Binding>();
							dbs.add(b);
							while (!dbs.isEmpty()) {
								Binding db = dbs.remove(0);
								if (db.isSet()) {
									int ind = 0;
									for (WingsSet s : db) {
										Binding sb = (Binding) s;
										sb.setID(db.getID() + "_" + ind);
										dbs.add(sb);
										ind++;
									}
								} else {
									KBObject vtype = fetchVariableTypeFromCMR(v, ccmr);
									Binding ds = createNewBinding(v, db, vtype, r, sortedInputs, event);
									db.setID(ds.getID());
								}
							}
						}
						if (rcmr.size() == 1 && i == 0) {
							// Return the portbindings
							cpblist = new PortBindingList(newpb);

							// Associate the port binding with this particular
							// component binding
							cb.setData(cpblist);
						} else {
							PortBindingList cbl = (PortBindingList) compBinding.getData();
							if (cbl == null)
								cbl = new PortBindingList();
							PortBindingList tmp = new PortBindingList(newpb);
							cbl.add(tmp);
							cpblist.add(tmp);
							cb.setData(cbl);
						}
						component.getBinding().add(cb);
					}
				}
				if ( len > 1 ) { 
					if ( !cpblist.isEmpty() || (cpblist.getPortBinding() != null) ) 
						pblist.add(cpblist); 
				} 
				else {
					pblist = cpblist;
					
					if(component.getBinding().size() == 1)
						component.setBinding((Binding)component.getBinding().get(0));
				}
			}
		}
		
		return pblist;
	}

	/**
	 * Step 4
	 * 
	 * @param template
	 *            a specialized template
	 * @return configured candidate workflows with all paramter values set
	 */

	public ArrayList<Template> configureTemplates(Template template) {
		LogEvent event = getEvent(LogEvent.EVENT_WG_CONFIGURE);
		logger.info(event.createStartLogMsg().addWQ(LogEvent.TEMPLATE, "" + template));

		ArrayList<Template> templates = new ArrayList<Template>();
		ArrayList<Template> processedTemplates = new ArrayList<Template>();

		HashMap<Template, ArrayList<String>> done = new HashMap<Template, ArrayList<String>>();

		Template t = template.createCopy();
		t.setID(UuidGen.generateAUuid(t.getName()));
		templates.add(t);

		// Configuration Step
		while (!templates.isEmpty()) {
			logger.info(event.createLogMsg().addList(LogEvent.QUEUED_TEMPLATES, templates));
			logger.info(event.createLogMsg().addList(LogEvent.CONFIGURED_TEMPLATES_Q, processedTemplates));

			Template currentTemplate = templates.remove(0);

			ArrayList<String> nodesDone = done.get(currentTemplate);
			if (nodesDone == null) {
				nodesDone = new ArrayList<String>();
			}

			ArrayList<Link> links = new ArrayList<Link>();
			for (Link link : currentTemplate.getInputLinks()) {
				links.add(link);
			}

			while (!links.isEmpty()) {
				if(currentTemplate == null) break;
				
				HashMap<Role, Variable> inputMaps = new HashMap<Role, Variable>();
				HashMap<Role, Variable> outputMaps = new HashMap<Role, Variable>();

				Link currentLink = links.remove(0);

				if (!currentLink.isOutputLink()) {
					inputMaps.put(currentLink.getDestinationPort().getRole(), currentLink.getVariable());
					ArrayList<String> variableIds = new ArrayList<String>();
					Node destNode = currentLink.getDestinationNode();

					// If this node hasn't been processed yet
					if (!nodesDone.contains(destNode.getID())) {

						Link[] inputLinks = currentTemplate.getInputLinks(destNode);
						Link[] outputLinks = currentTemplate.getOutputLinks(destNode);

						// Check that all inputs have data bindings
						boolean comebacklater = false;
						for (Link inputLink : inputLinks) {
							Variable invar = inputLink.getVariable();
							if (invar.isDataVariable() && invar.getBinding() == null) {
								comebacklater = true;
								break;
							}
						}
						if (comebacklater) {
							links.add(currentLink);
							continue;
						}

						HashMap<String, String> prospectiveIds = new HashMap<String, String>();
						HashMap<String, String> portVariableIds = new HashMap<String, String>();
						HashMap<String, String> opPortVariableIds = new HashMap<String, String>();

						// Add all output links to queue
						// Set temporary output dataset id
						for (Link outputLink : outputLinks) {
							Variable variable = outputLink.getVariable();
							if (variable.getBinding() == null) {
								String prospectiveId = dataNS + UuidGen.generateAUuid("");
								variable.setBinding(new Binding(prospectiveId));
								prospectiveIds.put(variable.getID(), prospectiveId);
							}

							outputMaps.put(outputLink.getOriginPort().getRole(), variable);
							variableIds.add(variable.getID());
							links.add(outputLink);
							portVariableIds.put(outputLink.getOriginPort().getID(), variable.getID());
							opPortVariableIds.put(outputLink.getOriginPort().getID(), variable.getID());
						}

						// Remove all input links from queue
						for (Link inputLink : inputLinks) {
							Variable variable = inputLink.getVariable();
							inputMaps.put(inputLink.getDestinationPort().getRole(), variable);
							variableIds.add(variable.getID());
							links.remove(inputLink);
							portVariableIds.put(inputLink.getDestinationPort().getID(), variable.getID());
						}

						ArrayList<KBTriple> redBox = currentTemplate.getConstraintEngine().getConstraints(variableIds);

						ComponentVariable component = destNode.getComponentVariable();

						// TODO: Check the port rules & variable bindings here
						// FIXME: For now, ignore all parameters in port rules
						// (add port-parameter-rules later)
						// TODO: Have to get constraints for bindings with the
						// correct dimensionality here ? (or maybe PC will
						// handle it from the metrics)

						ComponentMapsAndRequirements cmr = new ComponentMapsAndRequirements(component, inputMaps, outputMaps, redBox);

						PortBindingList ipblist = PortSetRuleHandler.normalizePortBindings(destNode, currentTemplate);

						// PortBindingList opblist = configureBindings(pblist);
						// Recursively go through the whole pblist and create
						// input/output maps and get parameter bindings for each
						// portbinding
						// - Maybe check rules at this point too in the future ?

						// constraints for these bindings needed or will the
						// metrics be enough ?
						// - should we translate metrics to rdf constraints ?
						// - should we represent metrics as they are (strings)
						// in the rdf ?

						// Check that opblist is not empty

						PortSetCreationRule prule = destNode.getPortSetRule();
						nodesDone.add(destNode.getID());

						if (ipblist.isEmpty() && ipblist.getPortBinding() == null) {
							currentTemplate = null;
							break;
						}

						if (prule.getType() == SetType.WTYPE) {
							ipblist = PortSetRuleHandler.flattenPortBindingList(ipblist, 0);
							//System.out.println("Flat: "+ipblist);

							String currentTemplateName = currentTemplate.getName();

							for (int i = ipblist.size() - 1; i >= 0; i--) {
								// Todo: Parallelize: have independent threads for each iteration
								
								PortBindingList ipb = ipblist.get(i);

								Template configuredTemplate = currentTemplate;
								ComponentVariable c = component;
								Node n = destNode;
								if (i > 0) {
									configuredTemplate = currentTemplate.createCopy();
									configuredTemplate.setID(UuidGen.generateAUuid(currentTemplateName));
									n = configuredTemplate.getNode(destNode.getID());
									c = n.getComponentVariable();
								}

								// Clone cmr before sending ? We basically need
								// to have separate variable bindings
								ComponentMapsAndRequirements pcmr = cloneCMRBindings(cmr);
								pcmr.setComponent(c);

								//System.out.println("WTYPE: "+c);
								//System.out.println(ipb);
								PortBindingList pblist = configureBindings(ipb, destNode, n, c, pcmr, event, prospectiveIds);
								PortBinding pb = PortSetRuleHandler.deNormalizePortBindings(pblist);
								//System.out.println(pb);

								if (pb == null) {
									if (i == 0)
										currentTemplate = null;
									continue;
								}
								
								this.removeComponentBindingsWithNoData(c);
								
								// CHANGED: (6/6/2011)
								// Extract bindings only for output variables or parameter variables
								// Earlier we were doing this for all variables
								for (Port p : pb.keySet()) {
									Variable cv = configuredTemplate.getVariable(portVariableIds.get(p.getID()));
									if(cv.isParameterVariable() || opPortVariableIds.containsKey(p.getID())) {
										Binding b = pb.get(p);
										cv.setBinding(b);
									}
								}

								if (i > 0) {
									templates.add(configuredTemplate);
									done.put(configuredTemplate, new ArrayList<String>(nodesDone));
								} else {
									logger.info(event.createLogMsg().addWQ(LogEvent.MSG, "Configured Template: " + currentTemplate));
									done.put(currentTemplate, nodesDone);
								}

							}

						} else if (prule.getType() == SetType.STYPE) {
							//System.out.println("STYPE: " + component);
							//System.out.println(ipblist);
							PortBindingList pblist = configureBindings(ipblist, destNode, destNode, component, cmr, event, prospectiveIds);
							PortBinding pb = PortSetRuleHandler.deNormalizePortBindings(pblist);
							//System.out.println(pb);

							if (pb == null) {
								currentTemplate = null;
								break;
							}
							
							this.removeComponentBindingsWithNoData(component);

							// CHANGED: (6/6/2011)
							// Extract bindings only for output variables or parameter variables
							// Earlier we were doing this for all variables
							for (Port p : pb.keySet()) {
								Variable cv = currentTemplate.getVariable(portVariableIds.get(p.getID()));
								if(cv.isParameterVariable() || opPortVariableIds.containsKey(p.getID())) {
									Binding b = pb.get(p);
									cv.setBinding(b);
								}
							}

							logger.info(event.createLogMsg().addWQ(LogEvent.MSG, "Configured Template: " + currentTemplate));
							done.put(currentTemplate, nodesDone);
						}

						// System.exit(0);
					}
					else if (currentTemplate != null) {
						Link[] outputLinks = currentTemplate.getOutputLinks(destNode);
						for(Link ol : outputLinks) {
							links.add(ol);
						}
					}
				}
			}
			if (currentTemplate != null) {
				// TODO: ?? Check that all component bindings in the template
				// have portbindings
				/*
				 * boolean ok = true; for(Node n: currentTemplate.getNodes()) {
				 * if(!hasPortBindings(n.getComponentVariable().getBinding())) {
				 * ok = false; break; } } if(ok)
				 */
				
				// TODO: Check in-out links to see that all the data that is produced, is actually consumed by a component on the other side of the link
				// - If not, then remove the producer component
				
				// **********************FIXME: (6/6/2011) Removing this Check for Now****************
				// if(removeProducersWithNoConsumers(currentTemplate))
					processedTemplates.add(currentTemplate);
			}
		}

		logger.info(event.createEndLogMsg().addWQ(LogEvent.TEMPLATE, "" + template));

		return processedTemplates;
	}
	
	
	private void removeComponentBindingsWithNoData(ComponentVariable c) {
		HashMap<Binding, Binding> parentOf = new HashMap<Binding, Binding>();
		ArrayList<Binding> ocbs = new ArrayList<Binding>();
		ocbs.add(c.getBinding());
		
		while(!ocbs.isEmpty()) {
			Binding ocb = ocbs.remove(0);
			
			if(ocb.isSet()) {
				for(WingsSet os: ocb) {
					Binding osb = (Binding)os;
					ocbs.add(osb);
					parentOf.put(osb, ocb);
				}
			}
			else {
				PortBindingList opblist = (PortBindingList) ocb.getData();
				if(opblist == null) parentOf.get(ocb).remove(ocb);
			}
		}
	}
	
	// Do a multi-dimensional binding dependency deletion (not completely necessary, but useful for efficiency)
	@SuppressWarnings("unused")
	private boolean removeProducersWithNoConsumers(Template template) {
		ArrayList<String> nodesDone = new ArrayList<String>();

		ArrayList<Link> links = new ArrayList<Link>();
		for (Link link : template.getOutputLinks()) {
			links.add(link);
		}

		while (!links.isEmpty()) {
			Link l = links.remove(0);
			if (l.isInputLink()) continue;
			
			Node on = l.getOriginNode();
			if(nodesDone.contains(on.getID())) continue;
			nodesDone.add(on.getID());
			
			if (l.isInOutLink()) {
				Node dn = l.getDestinationNode();
				ComponentVariable oc = on.getComponentVariable();
				ComponentVariable dc = dn.getComponentVariable();
				
				ArrayList<Binding> ocbs = new ArrayList<Binding>();
				ocbs.add(oc.getBinding());
				
				HashMap<Binding, Binding> parentb = new HashMap<Binding, Binding>();
				
				Port op = l.getOriginPort();
				Port dp = l.getDestinationPort();
				
				while(!ocbs.isEmpty()) {
					Binding ocb = ocbs.remove(0);
					
					if(ocb.isSet()) {
						for(WingsSet os: ocb) {
							Binding osb = (Binding)os;
							ocbs.add(osb);
							parentb.put(osb, ocb);
						}
					}
					else {
						boolean ok = false;
						PortBindingList opblist = (PortBindingList) ocb.getData();
						
						if(opblist != null) {
							PortBinding opb = opblist.getPortBinding();
							//System.out.println("Phew.. "+ocb+" has a PortBinding ");
							Binding oxb = opb.getById(op.getID());
							if (oxb == null)
								continue;

							// For each port-binding in dcb, check that there
							// exists a corresponding port-binding in ocb
							// - If not, then that ocb item must be deleted

							ArrayList<Binding> dcbs = new ArrayList<Binding>();
							dcbs.add(dc.getBinding());

							while (!dcbs.isEmpty()) {
								Binding dcb = dcbs.remove(0);
								if (dcb.isSet()) {
									for (WingsSet ds : dcb) {
										Binding dsb = (Binding) ds;
										dcbs.add(dsb);
									}
								} else {
									PortBindingList dpblist = (PortBindingList) dcb.getData();
									PortBinding dpb = dpblist.getPortBinding();
									Binding dxb = dpb.getById(dp.getID());

									// Currently, just assuming that all higher
									// dimension data is consumed
									if (oxb.getMaxDimension() > dxb.getMaxDimension()) {
										ok = true;
										break;
									}

									ArrayList<Binding> dxbs = new ArrayList<Binding>();
									dxbs.add(dxb);

									while (!dxbs.isEmpty()) {
										dxb = dxbs.remove(0);
										if (dxb.isSet() && dxb.getMaxDimension() > oxb.getMaxDimension()) {
											for (WingsSet ds : dxb)
												dxbs.add((Binding) ds);
										} else {
											/*if (oxb.getID() == null || dxb.getID() == null) {
												System.out.println("bzzt");
											}*/

											if (dxb != null && dxb.toString().equals(oxb.toString())) {
												ok = true;
												break;
											}
										}
									}
									if (ok)
										break;
								}
							}
						}
						else {
							System.err.println("WTF.. "+ocb+" has no PortBinding !");
						}
						if(!ok) {
							Binding parent = parentb.get(ocb);
							Binding child = ocb;
							while(parent != null) {
								Binding grandparent = parentb.get(parent);
								parent.remove(child);
								parentb.put(parent, grandparent);
								if(parent.isEmpty()) {
									child = parent;
									parent = grandparent;
								}
								else {
									break;
								}
							}
						}
					}
				}
				if(oc.getBinding().isEmpty()) return false;
			}
			
			for(Link ln : template.getInputLinks(on)) links.add(ln);
		}
		return true;
	}

	public ArrayList<KBTriple> fetchDatasetConstraints(Binding b, Variable v) {
		ArrayList<KBTriple> constraints = new ArrayList<KBTriple>();

		ArrayList<Binding> dbs = new ArrayList<Binding>();
		dbs.add(b);
		int i=0;
		while (!dbs.isEmpty()) {
			Binding db = dbs.remove(0);
			if (db.isSet()) {
				for (WingsSet s : db) {
					Binding sb = (Binding) s;
					dbs.add(sb);
				}
			} else {
				ArrayList<KBTriple> redbox = dc.getDataConstraints(db.getID(), v.getID());
				ArrayList<String> redboxStr = new ArrayList<String>();
				for (KBTriple kbTriple : redbox) {
					redboxStr.add(kbTriple.fullForm());
				}
				if (i == 0) {
					constraints.addAll(redbox);
				} else {
					for (KBTriple con : new ArrayList<KBTriple>(constraints)) {
						if (!redboxStr.contains(con.fullForm())) {
							// System.out.println(component.getComponentTypeName()+": Removing constraint: "+con);
							constraints.remove(con);
						}
					}
				}
				i++;
			}
		}
		return constraints;
	}
	/*
	 * private boolean hasPortBindings(Binding b) { if(b.isSet()) { for(WingsSet
	 * s: b) if(!hasPortBindings((Binding)s)) return false; return true; } else
	 * { if(b.getData() == null) return false; return true; } }
	 */

	public Template getInferredTemplate(Template template) {
		return getInferredTemplate(template, false);
	}

	public Template getInferredTemplate(Template template, boolean infer_types_only) {
		ComponentCatalog pc = this.getPc();

		HashMap<Template, ArrayList<String>> done = new HashMap<Template, ArrayList<String>>();

		Template currentTemplate = template.createCopy();
		currentTemplate.setID(UuidGen.generateAUuid(template.getName()));

		int MAXITERATIONS = 2; // Only 2 max iterations for now to keep it fast
		
		// Keep Sweeping until we reach a stable state (to keep it simple, we're
		// just checking that the number of constraints don't change)
		int numConstraints = 0;
		int iteration = 0;
		while (true) {
			// Get constraints for any bound datasets
			// --------------------------------------
			// Do this only on the initial iteration
			if (iteration == 0) {
				for (Link link : currentTemplate.getInputLinks()) {
					Variable var = link.getVariable();
					if (var.isDataVariable() && var.getBinding() != null) {
						// -- If data bindings are set, then get constraint
						// intersections
						// System.out.println(var.getBinding());
						ConstraintEngine engine = currentTemplate.getConstraintEngine();
						ArrayList<KBTriple> newConstraints = fetchDatasetConstraints(var.getBinding(), var);
						
						ArrayList<KBTriple> curConstraints = engine.getConstraints(var.getID());
						for(KBTriple cons : curConstraints) {
							for(KBTriple ncons : newConstraints) {
								if(cons.getPredicate().getID().equals(ncons.getPredicate().getID())) {
									if(!cons.getObject().isLiteral()) {
										// If this value is already bound to a variable. 
										// - Then replace all occurences of the variable with the value
										this.addExplanation("Setting ?"+cons.getObject().getName()+" "+
												" to "+ncons.getObject().getValue()+
												" because "+cons.getPredicate().getName()+" of "+
												var.getBinding().getName()+" is "+ncons.getObject().getValue());
										engine.replaceObjectInConstraints(cons.getObject(), ncons.getObject());
									}
									else if(!cons.getObject().getValue().equals(ncons.getObject().getValue())){
										// If this is already bound to a value, then check that it is the same
										this.addExplanation("ERROR: Expecting the "+cons.getPredicate().getName()+" of "+
												var.getBinding().getName()+" to be "+cons.getObject().getValue()+
												", but it is "+ncons.getObject().getValue());
										return null;
									}
								}
							}
						}
						engine.addConstraints(newConstraints);
					}
				}
			}
			
			ArrayList<String> nodesDone = new ArrayList<String>();
			ArrayList<Link> links = new ArrayList<Link>();
			
			// Do a light forward sweep 
			// --------------------------------
			ArrayList<Variable> processedVars = new ArrayList<Variable>();
			for (Link link : currentTemplate.getInputLinks()) {
				Variable var = link.getVariable();
				processedVars.add(var);
				links.add(link);
			}
			while (!links.isEmpty()) {
				Link currentLink = links.remove(0);
				if (currentLink.isOutputLink())
					continue;

				Node originNode = currentLink.getDestinationNode();
				Link[] inputLinks = currentTemplate.getInputLinks(originNode);

				// Check that all inputs have been processed
				boolean allinputs_processed = true;
				for (Link inputLink : inputLinks) {
					Variable variable = inputLink.getVariable();
					if (!processedVars.contains(variable)) {
						links.add(currentLink);
						allinputs_processed = false;
						break;
					}
				}
				if (!allinputs_processed)
					continue;

				HashMap<Role, Variable> inputMaps = new HashMap<Role, Variable>();
				HashMap<Role, Variable> outputMaps = new HashMap<Role, Variable>();

				ArrayList<String> variableIds = new ArrayList<String>();

				for (Link inputLink : inputLinks) {
					Variable variable = inputLink.getVariable();
					inputMaps.put(inputLink.getDestinationPort().getRole(), variable);
					variableIds.add(variable.getID());
					links.remove(inputLink);
				}

				Link[] outputLinks = currentTemplate.getOutputLinks(originNode);
				for (Link outputLink : outputLinks) {
					Variable variable = outputLink.getVariable();
					outputMaps.put(outputLink.getOriginPort().getRole(), variable);
					variableIds.add(variable.getID());
					processedVars.add(variable);
					links.add(outputLink);
				}

				if (nodesDone.contains(originNode.getID())) {
					// continue;
					// no op
				} else {
					ArrayList<KBTriple> redBox = currentTemplate.getConstraintEngine().getConstraints(variableIds);

					ComponentVariable component = originNode.getComponentVariable();

					ComponentMapsAndRequirements sentMapsAndRequirements = new ComponentMapsAndRequirements(component, inputMaps, outputMaps, redBox);
					ComponentMapsAndRequirements map;
					if (infer_types_only)
						map = pc.findDataTypeRequirements(sentMapsAndRequirements);
					else
						map = pc.findDataRequirements(sentMapsAndRequirements);
					
					if (map == null) {
						currentTemplate = null;
						break;
					} else {
						this.addExplanations(map.getExplanations());
						if(map.isInvalid) {
							currentTemplate = null;
							break;
						}
						nodesDone.add(originNode.getID());
						done.put(currentTemplate, nodesDone);

						currentTemplate.getConstraintEngine().addConstraints(map.getRequirements());
						//this.modifyTemplate(currentTemplate, originNode, new ComponentMapsAndRequirements[] { map });
					}
				}
			}
			if (currentTemplate == null)
				return null;
			
			
			// Do a light backward sweep 
			// --------------------------------
			nodesDone.clear();
			links.clear();
			
			for (Link link : currentTemplate.getOutputLinks())
				links.add(link);

			while (!links.isEmpty()) {
				HashMap<Role, Variable> inputMaps = new HashMap<Role, Variable>();
				HashMap<Role, Variable> outputMaps = new HashMap<Role, Variable>();
				Link currentLink = links.remove(0);
				if (currentLink.isInputLink()) {
					// continue;
					// no op
				} else {
					outputMaps.put(currentLink.getOriginPort().getRole(), currentLink.getVariable());
					ArrayList<String> variableIds = new ArrayList<String>();
					Node originNode = currentLink.getOriginNode();

					Link[] outputLinks = currentTemplate.getOutputLinks(originNode);
					for (Link outputLink : outputLinks) {
						Variable variable = outputLink.getVariable();
						outputMaps.put(outputLink.getOriginPort().getRole(), variable);
						variableIds.add(variable.getID());
						links.remove(outputLink);
					}

					Link[] inputLinks = currentTemplate.getInputLinks(originNode);
					for (Link inputLink : inputLinks) {
						Variable variable = inputLink.getVariable();
						inputMaps.put(inputLink.getDestinationPort().getRole(), variable);
						variableIds.add(variable.getID());
						links.add(inputLink);
					}

					if (nodesDone.contains(originNode.getID())) {
						// continue;
						// no op
					} else {
						ArrayList<KBTriple> redBox = currentTemplate.getConstraintEngine().getConstraints(variableIds);

						ComponentVariable component = originNode.getComponentVariable();

						ComponentMapsAndRequirements sentMapsAndRequirements = new ComponentMapsAndRequirements(component, inputMaps, outputMaps, redBox);
						ComponentMapsAndRequirements map;
						if (infer_types_only)
							map = pc.findDataTypeRequirements(sentMapsAndRequirements);
						else
							map = pc.findDataRequirements(sentMapsAndRequirements);

						if (map == null) {
							currentTemplate = null;
							break;
						} else {
							this.addExplanations(map.getExplanations());
							if(map.isInvalid) {
								currentTemplate = null;
								break;
							}
							nodesDone.add(originNode.getID());
							done.put(currentTemplate, nodesDone);

							currentTemplate.getConstraintEngine().addConstraints(map.getRequirements());
							//this.modifyTemplate(currentTemplate, originNode, new ComponentMapsAndRequirements[] { map });
						}
					}
				}
			}

			if (currentTemplate == null)
				return null;

			iteration ++;
			
			int newNumConstraints = currentTemplate.getConstraintEngine().getConstraints().size();
			if (newNumConstraints == numConstraints)
				break;
			
			if(iteration == MAXITERATIONS) break;
			numConstraints = newNumConstraints;
		}

		return currentTemplate;
	}

	private Binding createNewBinding(Variable v, Binding db, KBObject vtype, Role param, String prefix, LogEvent event) {
		String key = prefix + param;

		// Q4.3a Here
		logger.info(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "4.3").addWQ(LogEvent.QUERY_ARGUMENTS, key));

		String id = dc.createDataSetIDFromDescription(prefix + db.getName() + param);

		String opid = null;
		if(db != null && vtype != null && db.getMetrics() != null)
			opid = dc.createDataSetIDFromType(id, vtype.getID(), db.getMetrics());
		
		if(opid == null) 
			opid = id;

		logger.info(event.createLogMsg().addWQ(LogEvent.QUERY_NUMBER, "4.3").addWQ(LogEvent.QUERY_RESPONSE, opid));

		return new Binding(dataNS + opid);
	}

	private String getInputRoleStr(PortBinding pb, ComponentVariable c) {
		// Create a hashmap of input (role + ":"+ dsid/param)
		// - to uniquely identify the outputs
		TreeMap<String, String> iRoleVals = new TreeMap<String, String>();
		//System.out.println(pb);

		for (Port p : pb.keySet()) {
			iRoleVals.put(p.getRole().getName(), pb.get(p).toString());
		}
		String sortedInputs = (c.isTemplate() ? c.getID() : c.getComponentTypeName()) + ":";
		for (String irole : iRoleVals.keySet()) {
			sortedInputs += irole + ":" + iRoleVals.get(irole) + ",";
		}
		return sortedInputs;
	}

	/**
	 * Getter for property 'wgpcConnector'.
	 * 
	 * @return Value for property 'wgpcConnector'.
	 */
	public WorkflowGenerationProvenanceCatalog getWgpc() {
		return wgpc;
	}

	/**
	 * Setter for property 'wgpcConnector'.
	 * 
	 * @param wgpcConnector
	 *            Value to set for property 'wgpcConnector'.
	 */
	public void setWgpc(WorkflowGenerationProvenanceCatalog wgpc) {
		this.wgpc = wgpc;
	}

	/**
	 * Getter for property 'currentSeed'.
	 * 
	 * @return Value for property 'currentSeed'.
	 */
	public Seed getCurrentSeed() {
		return currentSeed;
	}

	/**
	 * Setter for property 'currentSeed'.
	 * 
	 * @param currentSeed
	 *            Value to set for property 'currentSeed'.
	 */
	public void setCurrentSeed(Seed currentSeed) {
		this.currentSeed = currentSeed;
	}

	/**
	 * Getter for property 'dc'.
	 * 
	 * @return Value for property 'dc'.
	 */
	public DataCatalog getDc() {
		return dc;
	}

	/**
	 * Setter for property 'dc'.
	 * 
	 * @param dc
	 *            Value to set for property 'dc'.
	 */
	public void setDc(DataCatalog dc) {
		this.dc = dc;
	}

	/**
	 * Getter for property 'pc'.
	 * 
	 * @return Value for property 'pc'.
	 */
	public ComponentCatalog getPc() {
		return pc;
	}

	/**
	 * Setter for property 'pc'.
	 * 
	 * @param pc
	 *            Value to set for property 'pc'.
	 */
	public void setPc(ComponentCatalog pc) {
		this.pc = pc;
	}

	/**
	 * Getter for property 'templateLibraryDomain'.
	 * 
	 * @return Value for property 'templateLibraryDomain'.
	 */
	public String getTemplateLibraryDomain() {
		return templateLibraryDomain;
	}

	/**
	 * Setter for property 'templateLibraryDomain'.
	 * 
	 * @param templateLibraryDomain
	 *            Value to set for property 'templateLibraryDomain'.
	 */
	public void setTemplateLibraryDomain(String templateLibraryDomain) {
		this.templateLibraryDomain = templateLibraryDomain;
	}

	/**
	 * Setter for property 'storeProvenance'
	 */
	public void setProvenanceFlag(boolean flag) {
		this.storeProvenance = flag;
	}

	public void setCurrentLogEvent(LogEvent ev) {
		this.curLogEvent = ev;
	}

	public LogEvent getCurrentLogEvent() {
		return this.curLogEvent;
	}
	
	public ArrayList<String> getExplanations() {
		return this.explanations;
	}
	
	public void addExplanations(ArrayList<String> exp) {
		this.explanations.addAll(exp);
	}
	
	public void addExplanation(String exp) {
		this.explanations.add(exp);
	}
}
