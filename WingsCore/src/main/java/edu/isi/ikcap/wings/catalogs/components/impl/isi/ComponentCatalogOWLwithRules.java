////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2007, Your Corporation. All Rights Reserved.
////////////////////////////////////////////////////////////////////////////////

package edu.isi.ikcap.wings.catalogs.components.impl.isi;

import edu.isi.ikcap.ontapi.KBAPI;
import edu.isi.ikcap.ontapi.KBObject;
import edu.isi.ikcap.ontapi.KBTriple;
import edu.isi.ikcap.ontapi.OntSpec;

import edu.isi.ikcap.wings.catalogs.components.classes.ComponentMapsAndRequirements;
import edu.isi.ikcap.wings.catalogs.components.classes.EnvironmentVariable;
import edu.isi.ikcap.wings.catalogs.components.classes.TransformationCharacteristics;
import edu.isi.ikcap.wings.workflows.template.ConstraintEngine;
import edu.isi.ikcap.wings.workflows.template.Role;
import edu.isi.ikcap.wings.workflows.template.impl.ConstraintEngineOWL;
import edu.isi.ikcap.wings.workflows.template.sets.Binding;
import edu.isi.ikcap.wings.workflows.template.sets.ValueBinding;
import edu.isi.ikcap.wings.workflows.template.sets.WingsSet;
import edu.isi.ikcap.wings.workflows.template.variables.*;
import edu.isi.ikcap.wings.workflows.util.MetricsHelper;
import edu.isi.ikcap.wings.workflows.util.PropertiesHelper;
import edu.isi.ikcap.wings.workflows.util.UuidGen;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

/**
 * <p>
 * Title:
 * </p>
 * 
 * <p>
 * Description:
 * </p>
 * 
 * <p>
 * Copyright: Copyright (c) 2005
 * </p>
 * 
 * <p>
 * Company:
 * </p>
 * 
 * @author not attributable
 * @version 1.0
 */
public class ComponentCatalogOWLwithRules extends ComponentCatalogOWL {
	String rules;
	
	ArrayList<KBTriple> domainKnowledge = new ArrayList<KBTriple>();
	String RDF;
	String RDFS;

	public ComponentCatalogOWLwithRules(String ldid) {
		super(ldid);
		this.initCC(ldid);
	}
	
	public ComponentCatalogOWLwithRules(String libname, String ldid) {
		super(libname, ldid);
		this.initCC(ldid);
		String absrules = readRulesFromFile(PropertiesHelper.getPCDomainDir()+"/abstract.rules");
		rules = absrules + rules;
	}
	
	private String readRulesFromFile(String path) {
		String str = "";
		try {
			Scanner sc = new Scanner(new File(path));
			while(sc.hasNextLine()){
				String line = ignoreComments(sc.nextLine());
				if(line != null) str += line;             
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return str;
	}

	private String ignoreComments(String line) {
		String result_line=null;
		int upto=line.indexOf('#');
		if(upto!=0 && upto>0){
			result_line=line.substring(0, upto);
		} else if(upto < 0){
			result_line=line;
		}
		return result_line;
	}
	
	private void initCC(String ldid) {
		rules = readRulesFromFile(PropertiesHelper.getPCDomainDir()+"/library.rules");

		// Create general domain knowledge data
		RDFS = (String) this.api.getPrefixNamespaceMap().get("rdfs");
		RDF = (String) this.api.getPrefixNamespaceMap().get("rdf");

		// KBObject rdfsSubClass = this.api.getProperty(rdfs+"subClassOf");
		// for(KBTriple triple: api.genericTripleQuery(null, rdfsSubClass,
		// null)) {
		// if(triple.getSubject().getNamespace().equals(this.dcdomns))
		// domainKnowledge.add(triple);
		// }

		KBObject rdfsSubProp = this.api.getProperty(RDFS + "subPropertyOf");
		KBObject dcMetricsProp = this.api.getProperty(this.dcns + "hasMetrics");
		KBObject dcDataMetricsProp = this.api.getProperty(this.dcns + "hasDataMetrics");
		domainKnowledge.addAll(api.genericTripleQuery(null, rdfsSubProp, dcMetricsProp));
		domainKnowledge.addAll(api.genericTripleQuery(null, rdfsSubProp, dcDataMetricsProp));

		setRuleMappings();
	}

	protected KBObject copyObjectIntoAPI(String id, KBObject obj, KBAPI tapi, String includeNS, String excludeNS, boolean direct) {
		// Add component to the temporary KBAPI (add all its classes explicitly)
		KBObject tobj = tapi.createObjectOfClass(id, this.api.getClassOfInstance(obj));
		copyObjectClassesIntoAPI(id, obj, tapi, includeNS, excludeNS, direct);
		return tobj;
	}

	protected KBObject copyObjectClassesIntoAPI(String id, KBObject obj, KBAPI tapi, String includeNS, String excludeNS, boolean direct) {
		// Add component to the temporary KBAPI (add all its classes explicitly)
		KBObject tobj = tapi.getResource(id);
		ArrayList<KBObject> objclses;
		if (direct)
			objclses = api.getAllClassesOfInstance(obj, direct);
		else {
			objclses = new ArrayList<KBObject>();
			for (KBTriple t : api.genericTripleQuery(obj, api.getProperty(RDF + "type"), null))
				objclses.add(t.getObject());
		}
		for (KBObject objcls : objclses) {
			if ((includeNS != null && objcls.getNamespace().equals(includeNS)) || (excludeNS != null && !objcls.getNamespace().equals(excludeNS))) {
				tapi.addTriple(tobj, tapi.getProperty(RDF + "type"), objcls);
				// System.out.println("-- adding class: "+objcls.getID()+" to "+tobj.getID());
				// tapi.addClassForInstance(tobj, objcls);
			}
		}
		return tobj;
	}

	protected void setRuleMappings() {
		// Set Rule Prefix-Namespace Mappings
		// TODO: Set sr: as the workflow namespace
		// TODO: Set template: as the template namespace
		HashMap<String, String> rulePrefixNSMap = new HashMap<String, String>();
		Map<String,String> m = this.api.getPrefixNamespaceMap();
		for(String prefix: m.keySet()) {
			rulePrefixNSMap.put(prefix, m.get(prefix));
		}
		rulePrefixNSMap.put("dcdom", this.dcdomns);
		rulePrefixNSMap.put("dc", this.dcns);
		rulePrefixNSMap.put("pcdom", this.pcdomns);
		rulePrefixNSMap.put("pc", this.pcns);
		rulePrefixNSMap.put("wflow", PropertiesHelper.getWorkflowOntologyURL() + "#");
		//System.out.println(rulePrefixNSMap);
		this.api.setRulesPrefixNamespaceMap(rulePrefixNSMap);
	}

	protected boolean checkTypeCompatibility(KBAPI tapi, String varid, String argid) {
		ArrayList<KBObject> varclses = tapi.getAllClassesOfInstance(tapi.getResource(varid), true);
		ArrayList<KBObject> argclses = api.getAllClassesOfInstance(api.getResource(argid), true);
		for (KBObject argcls : argclses) {
			if (argcls.getNamespace().equals(this.dcdomns)) {
				for (KBObject varcls : varclses) {
					KBObject varcl = api.getConcept(varcls.getID());
					if (varcl.getNamespace().equals(this.dcdomns)) {
						if (!api.hasSubClass(argcls, varcl) && !api.hasSubClass(varcl, argcls))
							return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * <b>Query 2.1</b><br/>
	 * Get a list of Specialized Components with their IO Data Requirements
	 * given a component (maybe abstract) and it's IO Data Requirements
	 * 
	 * @param mapsAndConstraints
	 *            A ComponentMapsAndRequirements Object which contains:
	 *            <ul>
	 *            <li>component,
	 *            <li>maps of component input arguments to template variables,
	 *            <li>maps of component output arguments to template variables,
	 *            <li>template variable descriptions (dods) - list of triples
	 *            </ul>
	 * @return list of ComponentMapsAndRequirements Objects for each specialized
	 *         component
	 */

	public ArrayList<ComponentMapsAndRequirements> specializeAndFindDataRequirements(ComponentMapsAndRequirements mapsAndConstraints) {
		return findDataRequirements(mapsAndConstraints, true, true);
	}

	/**
	 * <b>Query 2.1b</b><br/>
	 * Get inferred IO Data Requirements given a component (maybe abstract) and
	 * it's given IO Data Requirements
	 * 
	 * @param mapsAndConstraints
	 *            A ComponentMapsAndRequirements Object which contains:
	 *            <ul>
	 *            <li>component,
	 *            <li>maps of component input arguments to template variables,
	 *            <li>maps of component output arguments to template variables,
	 *            <li>template variable descriptions (dods) - list of triples
	 *            </ul>
	 * @return ComponentMapsAndRequirements Object
	 */
	public ComponentMapsAndRequirements findDataRequirements(ComponentMapsAndRequirements mapsAndConstraints) {
		ArrayList<ComponentMapsAndRequirements> list = findDataRequirements(mapsAndConstraints, false, true);
		if (list.size() > 0)
			return list.get(0);

		return null;
	}

	/**
	 * <b>Query 2.1c</b><br/>
	 * Get inferred IO Data Requirements (Types only) given a component (maybe
	 * abstract) and it's given IO Data Requirements
	 * 
	 * @param mapsAndConstraints
	 *            A ComponentMapsAndRequirements Object which contains:
	 *            <ul>
	 *            <li>component,
	 *            <li>maps of component input arguments to template variables,
	 *            <li>maps of component output arguments to template variables,
	 *            <li>template variable descriptions (dods) - list of triples
	 *            </ul>
	 * @return ComponentMapsAndRequirements Object
	 */
	public ComponentMapsAndRequirements findDataTypeRequirements(ComponentMapsAndRequirements mapsAndConstraints) {
		ArrayList<ComponentMapsAndRequirements> list = findDataRequirements(mapsAndConstraints, false, false);
		if (list.size() > 0)
			return list.get(0);

		return null;
	}

	public ArrayList<ComponentMapsAndRequirements> findDataRequirements(ComponentMapsAndRequirements mapsAndConstraints, boolean specialize, boolean useRules) {
		ArrayList<ComponentMapsAndRequirements> list = new ArrayList<ComponentMapsAndRequirements>();

		HashMap<String, KBObject> omap = this.objPropMap;
		HashMap<String, KBObject> dmap = this.dataPropMap;
		HashMap<String, KBObject> cmap = this.conceptMap;

		// Extract info from mapsAndConstraints object
		ComponentVariable c = mapsAndConstraints.getComponent();
		HashMap<String, Variable> inputMaps = mapsAndConstraints.getStringIndexedInputMaps();
		HashMap<String, Variable> outputMaps = mapsAndConstraints.getStringIndexedOutputMaps();
		ArrayList<KBTriple> redbox = mapsAndConstraints.getRequirements();

		// Get Component
		String incompid = c.getID();
		KBObject comp = this.api.getIndividual((c.getBinding() != null) ? c.getBinding().getID() : c.getComponentType());
		if (comp == null) {
			logger.debug(comp + " is not a valid component");
			mapsAndConstraints.addExplanations(comp + " is not a valid component");
			mapsAndConstraints.setInvalidFlag(true);
			list.add(mapsAndConstraints);
			return list;
		}

		// Get a mapping of I/O Var's to ArgID's (with property hasArgumentID)
		// for the Passed Component
		ArrayList<KBObject> inputArgs = this.api.getPropertyValues(comp, omap.get("hasInput"));
		ArrayList<KBObject> outputArgs = this.api.getPropertyValues(comp, omap.get("hasOutput"));

		// Get List of all concrete components
		ArrayList<KBObject> ccomps = new ArrayList<KBObject>();
		KBObject isConcrete = this.api.getDatatypePropertyValue(comp, dmap.get("isConcrete"));
		if (!specialize || (isConcrete != null && isConcrete.getValue() != null && ((Boolean) isConcrete.getValue()).booleanValue())) {
			ccomps.add(comp);
		} else {
			KBObject cls = this.api.getClassOfInstance(comp);
			if (cls == null) {
				return list;
			}
			ArrayList<KBObject> insts = this.api.getInstancesOfClass(cls);
			for (KBObject inst : insts) {
				isConcrete = this.api.getDatatypePropertyValue(inst, dmap.get("isConcrete"));
				if (isConcrete != null && isConcrete.getValue() != null && ((Boolean) isConcrete.getValue()).booleanValue()) {
					ccomps.add(inst);
				}
			}
		}

		logger.debug("Available components to check validity: " + ccomps);

		// For All concrete components :
		// - Get mapping of specialized arguments to variables
		// - Transfer "relevant" output variable properties to input variables
		// - Pass back the specialized component + specialized mappings +
		// modified red-box
		// - Handle *NEW* Arguments as well -> Create *NEW* DataVariables

		// Get Metrics property hierarchy triples for adding into the temporary
		// api

		for (KBObject ccomp : ccomps) {
			// Get input and output arguments of the specialized component
			inputArgs = this.api.getPropertyValues(ccomp, omap.get("hasInput"));
			outputArgs = this.api.getPropertyValues(ccomp, omap.get("hasOutput"));

			HashMap<Role, Variable> sInputMaps = new HashMap<Role, Variable>();
			HashMap<Role, Variable> sOutputMaps = new HashMap<Role, Variable>();

			ArrayList varids = new ArrayList<String>();

			// Create a new temporary api
			KBAPI tapi = this.ontologyFactory.getAPI(OntSpec.MICRO);

			// Add the redbox (i.e. datavariable constraints) to the temporary
			// api
			tapi.addTriples(redbox);
			tapi.addTriples(domainKnowledge);

			// Create a copy of the specialized component in the temporary api
			KBObject tcomp = this.copyObjectIntoAPI(incompid, ccomp, tapi, this.pcdomns, null, false);

			boolean typesOk = true;

			// For all arguments of the specialized component :
			ArrayList<KBObject> allArgs = new ArrayList<KBObject>(inputArgs);
			allArgs.addAll(outputArgs);

			ArrayList<String> explanations = new ArrayList<String>();
			ComponentMapsAndRequirements cmr;
			ComponentVariable concreteComponent = new ComponentVariable(incompid);
			concreteComponent.setComponentType(ccomp.getID());
			if (specialize) concreteComponent.setConcrete(true);
			else concreteComponent.setConcrete(c.isConcrete());
			
			for (KBObject arg : allArgs) {
				// Get the argument ID for the specialized argument
				KBObject argid = this.api.getDatatypePropertyValue(arg, dmap.get("hasArgumentID"));

				String varid = null;

				String role = this.pcdomns + argid.getValue();
				// Get the corresponding Variable from the Map
				Variable var = inputMaps.get(role);
				if (var == null)
					var = outputMaps.get(role);

				// If no Variable found for this argument, then create a new
				// data/param variable in the temp. api
				if (var == null) {
					// varid = arg.getID()+"_"+ccomp.getName()+"_Variable";
					varid = arg.getID() + "_var"; // disambiguate inside wings ?
					short type = 0;
					if (this.api.isA(arg, cmap.get("ParameterArgument"))) {
						// tapi.createObjectOfClass(varid,
						// this.api.createClass(this.srns+"ParameterVariable"));
						type = VariableType.PARAM;
					} else if (this.api.isA(arg, cmap.get("DataArgument"))) {
						type = VariableType.DATA;
						// tapi.createObjectOfClass(varid,
						// this.api.createClass(this.srns+"DataVariable"));
					} else {
						System.err.println("Cannot get argument type for " + arg);
					}
					var = new Variable(varid, type);
				} else {
					varid = var.getID();
					// Make sure that the variable has a type that is subsumed
					// by the argument type
					// or that the argument type is subsumed by the variable
					// type
					if (!checkTypeCompatibility(tapi, varid, arg.getID())) {
						logger.debug(arg.getID()+" is not type compatible with variable: " + varid);
						explanations.add(tcomp+" is not selectable because "+ arg.getID()+" is not type compatible with variable: " + varid);
						typesOk = false;
						break;
					}
				}

				// Copy over the argument's classes to the variable
				KBObject varobj = this.copyObjectClassesIntoAPI(varid, arg, tapi, this.dcdomns, null, false);

				// create hasArgumentID property for the variable
				tapi.addTriple(varobj, dmap.get("hasArgumentID"), argid);

				Role r = new Role(role);
				KBObject dim = this.api.getDatatypePropertyValue(arg, dmap.get("hasDimensionality"));
				if (dim != null && dim.getValue() != null) {
					r.setDimensionality((Integer) dim.getValue());
				}
				
				if(var.isDataVariable() && var.getBinding() != null && var.getBinding().getName() != null) {
					tapi.addTriple(varobj, dmap.get("hasBindingID"), tapi.createLiteral(var.getBinding().getName()));
				} else {
					tapi.addTriple(varobj, dmap.get("hasBindingID"), tapi.createLiteral(" "));
				}

				// assign this variable as an input or output to the component
				if (inputArgs.contains(arg)) {
					tapi.addTriple(tcomp, omap.get("hasInput"), varobj);
					sInputMaps.put(r, var);
				} else if (outputArgs.contains(arg)) {
					tapi.addTriple(tcomp, omap.get("hasOutput"), varobj);
					sOutputMaps.put(r, var);
				}
				varids.add(var.getID());
			}

			// Create a Return CMR
			
			
			if (!typesOk) {
				logger.debug(tcomp + " is not selectable ");
				cmr = new ComponentMapsAndRequirements(concreteComponent, sInputMaps, sOutputMaps, new ArrayList<KBTriple>());
				cmr.addExplanations(explanations);
				cmr.setInvalidFlag(true);
				list.add(cmr);
				continue;
			}


			if (useRules) {
				ByteArrayOutputStream bost = new ByteArrayOutputStream();
				PrintStream oldout = System.out;
				System.setOut(new PrintStream(bost, true));
				
				// Run propagation rules on the temporary model
				// logger.debug("*****BEFORE*****\n"+tapi.toAbbrevRdf(true));
				tapi.applyRulesFromString(rules);
				// logger.debug("*****AFTER*****\n"+tapi.toAbbrevRdf(true));
				
				System.setOut(oldout);
				if(!bost.toString().equals("")) {
					for(String exp : bost.toString().split("\\n")) {
						explanations.add(exp);
					}
				}
			}

			
			// Checking for invalidity
			KBObject invalidProp = tapi.getProperty(this.pcns + "isInvalid");
			KBObject isInvalid = tapi.getPropertyValue(tcomp, invalidProp);
			if (isInvalid != null && (Boolean) isInvalid.getValue()) {
				logger.debug(tcomp + " is not selectable ");
				cmr = new ComponentMapsAndRequirements(concreteComponent, sInputMaps, sOutputMaps, new ArrayList<KBTriple>());
				cmr.addExplanations(explanations);
				cmr.setInvalidFlag(true);
				list.add(cmr);
				continue;
			}

			// Set parameter values
			for (Variable var : inputMaps.values()) {
				if (var.isParameterVariable() && var.getBinding() == null) {
					KBObject varobj = tapi.getResource(var.getID());
					KBObject val = tapi.getPropertyValue(varobj, dmap.get("hasValue"));
					if (val != null && val.getValue() != null) {
						tapi.addTriple(varobj, tapi.getResource(this.srns + "hasParameterValue"), val);
						var.setBinding(new ValueBinding(val.getValue()));
					}
				}
			}

			// Create a constraint engine and get Relevant Constraints here
			ConstraintEngine cons = new ConstraintEngineOWL(tapi, "");
			cons.addWhitelistedNamespace(this.dcdomns);
			cons.addWhitelistedNamespace(this.dcns);
			cons.addWhitelistedNamespace(this.srns);
			ArrayList<String> blacklistedIds = new ArrayList<String>();
			blacklistedIds.add(dmap.get("hasArgumentID").getID());
			blacklistedIds.add(dmap.get("hasBindingID").getID());
			blacklistedIds.add(this.dcns + "hasMetrics");
			blacklistedIds.add(this.dcns + "hasDataMetrics");
			blacklistedIds.add(this.pcns + "hasValue");

			for (String id : blacklistedIds)
				cons.addBlacklistedId(id);
			ArrayList<KBTriple> constraints = cons.getConstraints(varids);
			for (String id : blacklistedIds)
				cons.removeBlacklistedId(id);

			cmr = new ComponentMapsAndRequirements(concreteComponent, sInputMaps, sOutputMaps, constraints);
			cmr.addExplanations(explanations);
			list.add(cmr);
		}
		return list;
	}

	/**
	 * <b>Query 4.2</b><br/>
	 * This function is supposed to <b>SET</b> the DataSet Metrics, or Parameter
	 * Values for the Variables that are passed in via the input/output maps as
	 * part of mapsAndConstraints.<br/>
	 * Variables will already be bound to dataObjects, so the function will have
	 * to do something like the following :
	 * 
	 * <pre>
	 * If Variable.isParameterVariable() Variable.setParameterValue(value)
	 * If Variable.isDataVariable() Variable.getDataObjectBinding().setDataMetrics(xml)
	 * </pre>
	 * 
	 * @param mapsAndConstraints
	 *            A ComponentMapsAndRequirements Object which contains:
	 *            <ul>
	 *            <li>component,
	 *            <li>maps of component input arguments to template variables,
	 *            <li>maps of component output arguments to template variables,
	 *            <li>template variable descriptions (dods) - list of triples
	 *            </ul>
	 * @return List of extra template variable descriptions (will mostly be
	 *         empty in Q4.2 though)
	 */
	public ArrayList<ComponentMapsAndRequirements> findOutputDataPredictedDescriptions(ComponentMapsAndRequirements mapsAndConstraints) {

		ArrayList list = new ArrayList<ComponentMapsAndRequirements>();

		HashMap<String, KBObject> omap = this.objPropMap;
		HashMap<String, KBObject> dmap = this.dataPropMap;

		// Extract info from mapsAndConstraints object
		ComponentVariable c = mapsAndConstraints.getComponent();
		HashMap<String, Variable> inputMaps = mapsAndConstraints.getStringIndexedInputMaps();
		HashMap<String, Variable> outputMaps = mapsAndConstraints.getStringIndexedOutputMaps();

		ArrayList<KBTriple> redbox = mapsAndConstraints.getRequirements();

		// Get Component
		// KBObject comp = this.api.getIndividual(c.getComponentType());
		// System.out.println(c.getBinding());
		KBObject comp = this.api.getIndividual(c.getBinding().getID());
		if (comp == null) {
			logger.debug(comp + " is not a valid component");
			mapsAndConstraints.addExplanations(comp + " is not a valid component");
			mapsAndConstraints.setInvalidFlag(true);
			list.add(mapsAndConstraints);
			return list;
		}

		// Get a mapping of ArgID's (with property hasArgumentID) to arg for the
		// Passed Component
		ArrayList<KBObject> inputArgs = this.api.getPropertyValues(comp, omap.get("hasInput"));
		ArrayList<KBObject> outputArgs = this.api.getPropertyValues(comp, omap.get("hasOutput"));

		HashMap<String, KBObject> argMaps = new HashMap<String, KBObject>();
		for (KBObject iarg : inputArgs) {
			KBObject argid = this.api.getDatatypePropertyValue(iarg, dmap.get("hasArgumentID"));
			String argidstr = this.pcdomns + (String) argid.getValue();
			argMaps.put(argidstr, iarg);
		}
		for (KBObject oarg : outputArgs) {
			KBObject argid = this.api.getDatatypePropertyValue(oarg, dmap.get("hasArgumentID"));
			String argidstr = this.pcdomns + (String) argid.getValue();
			argMaps.put(argidstr, oarg);
		}

		// Create a new temporary KBAPI
		KBAPI tapi = this.ontologyFactory.getAPI(OntSpec.MICRO);

		// Add component to the temporary KBAPI (add all its classes explicitly)
		KBObject tcomp = this.copyObjectIntoAPI(comp.getID(), comp, tapi, this.pcdomns, null, false);

		// Keep a map of the metric property id to metric type
		// (datacharacterization/datametric)
		HashMap<String, String> metricPropDCTypeMap = new HashMap<String, String>();

		// Convert metrics to Property assertions in the Temporary KBAPI
		HashMap<String, Variable> map = new HashMap<String, Variable>(inputMaps);
		map.putAll(outputMaps);
		
		HashMap<Variable, String> variableNameMap = new HashMap<Variable, String>();
		System.out.println("--"+comp.getID());
		for (String argidstr : map.keySet()) {
			Variable var = map.get(argidstr);
			KBObject arg = argMaps.get(argidstr);
			System.out.println(argidstr);
			KBObject arg_paramid = this.api.getPropertyValue(arg, dmap.get("hasArgumentID"));
			String variableName = var.getID() + "_" + arg_paramid.getValue();
			variableNameMap.put(var, variableName);
			
			KBObject varobj = tapi.getResource(variableName);
			if (var.isDataVariable()) {
				if (var.getBinding() != null) {
					
					// Convert Metrics to PC properties in order to run rules on them
					
					String metrics = var.getBinding().getMetrics();
					HashMap<String, ArrayList> propValMap = MetricsHelper.parseMetricsXML(metrics);
					for (String propid : propValMap.keySet()) {
						ArrayList<String> tmp = propValMap.get(propid);
						Object val = tmp.get(0);
						String type = tmp.get(1);
						if(type.equals("rdfMetrics")) {
							KBObject rdfProp = this.api.getResource(RDF + propid);
							KBObject valobj = this.api.getResource(this.dcdomns + val);
							if(valobj != null) tapi.addTriple(varobj, rdfProp, valobj);
							continue;
						}
						metricPropDCTypeMap.put(propid, type);
						KBObject metricProp = omap.get(propid);
						if (metricProp != null) {
							// Object property
							// Get obj from dcdomns, or dcns
							KBObject obj = this.api.getResource(this.dcdomns + val);
							if (obj == null)
								obj = this.api.getResource(this.dcns + val);
							if (obj == null) {
								System.err.println("Cannot Recognize Metrics Value " + val);
								continue;
							}
							//System.out.println(propid + " : " +obj);
							KBObject tobj = this.copyObjectIntoAPI(obj.getID(), obj, tapi, null, null, true);
							tapi.addTriple(varobj, metricProp, tobj);
						} else {
							metricProp = dmap.get(propid);
							if (metricProp != null) {
								// Data Property
								KBObject tobj = this.ontologyFactory.getDataObject(val);
								tapi.addTriple(varobj, metricProp, tobj);
							} else {
								System.err.println("No Such Metrics Property Known to AC : " + propid);
								continue;
							}
						}
					}
					
					// -- Create other PC properties on varobj
					
					if(var.getBinding().isSet()) {
						String dimensionSizes = "";
						ArrayList<Binding> vbs = new ArrayList<Binding>();
						vbs.add(var.getBinding());
						while(!vbs.isEmpty()) {
							Binding vb = vbs.remove(0);
							if(vb.isSet()) {
								for(WingsSet vs: vb) {
									vbs.add((Binding)vs);
								}
								if(!dimensionSizes.equals("")) dimensionSizes += ",";
								dimensionSizes += vb.getSize();
							}
						}
						tapi.setPropertyValue(varobj, dmap.get("hasDimensionSizes"), this.ontologyFactory.getDataObject(dimensionSizes));
					}
					
					if(var.getBinding().getID() != null)
						tapi.addTriple(varobj, dmap.get("hasBindingID"), this.ontologyFactory.getDataObject(var.getBinding().getName()));
					else
						tapi.addTriple(varobj, dmap.get("hasBindingID"), this.ontologyFactory.getDataObject(" "));
				}
			} else {
				KBObject arg_value = this.api.getPropertyValue(arg, dmap.get("hasValue"));
				// If the user has set a parameter value, use it here
				if(var.getBinding() != null) {
					arg_value = this.ontologyFactory.getDataObject(var.getBinding().getValue());
				}
				if(arg_value != null) {
					tapi.setPropertyValue(varobj, dmap.get("hasValue"), arg_value);
				}
				tapi.addTriple(varobj, dmap.get("hasBindingID"), this.ontologyFactory.getDataObject("Param"+arg.getName()));
			}
			this.copyObjectClassesIntoAPI(varobj.getID(), arg, tapi, null, null, true);
			tapi.addTriple(varobj, dmap.get("hasArgumentID"), arg_paramid);
			
			if (inputMaps.containsKey(argidstr)) {
				tapi.addTriple(tcomp, omap.get("hasInput"), varobj);
			} else if (outputMaps.containsKey(argidstr)) {
				tapi.addTriple(tcomp, omap.get("hasOutput"), varobj);
			}
		}

		// Add Metrics property hierarchy triples into the temporary api
		String rdfs = (String) this.api.getPrefixNamespaceMap().get("rdfs");
		KBObject rdfsProp = this.api.getProperty(rdfs + "subPropertyOf");
		KBObject dcProp = this.api.getProperty(this.dcns + "hasMetrics");
		KBObject dcPropD = this.api.getProperty(this.dcns + "hasDataMetrics");
		ArrayList<KBTriple> metricTriples = this.api.genericTripleQuery(null, rdfsProp, dcProp);
		metricTriples.addAll(this.api.genericTripleQuery(null, rdfsProp, dcPropD));
		tapi.addTriples(metricTriples);

		// Cache varid to varobj
		HashMap<String, KBObject> varIDObjMap = new HashMap<String, KBObject>();
		for (Variable var : inputMaps.values()) {
			KBObject varobj = tapi.getResource(variableNameMap.get(var));
			varIDObjMap.put(var.getID(), varobj);
		}
		for (Variable var : outputMaps.values()) {
			KBObject varobj = tapi.getResource(variableNameMap.get(var));
			varIDObjMap.put(var.getID(), varobj);
		}
		// Add the information from redbox to the temporary KBAPI
		for(KBTriple t: redbox) {
			KBObject subj = varIDObjMap.get(t.getSubject().getID());
			KBObject obj = varIDObjMap.get(t.getObject().getID());
			if(subj == null) subj = t.getSubject();
			if(obj == null) obj = t.getObject();
			tapi.addTriple(subj, t.getPredicate(), obj);
		}
		
		ArrayList<String> explanations = new ArrayList<String>();
		
		ByteArrayOutputStream bost = new ByteArrayOutputStream();
		PrintStream oldout = System.out;
		System.setOut(new PrintStream(bost, true));

		// Run propagation rules on the temporary model
		// logger.debug("*****BEFORE*****\n"+tapi.toAbbrevRdf(true));
		tapi.applyRulesFromString(rules);
		// logger.debug("*****AFTER*****\n"+tapi.toAbbrevRdf(true));

		System.setOut(oldout);
		if (!bost.toString().equals("")) {
			for(String exp : bost.toString().split("\\n")) {
				explanations.add(exp);
			}
		}

		KBObject invalidProp = tapi.getProperty(this.pcns + "isInvalid");
		KBObject isInvalid = tapi.getPropertyValue(tcomp, invalidProp);
		if (isInvalid != null && (Boolean) isInvalid.getValue()) {
			logger.debug(tcomp + " is not valid for its inputs");
			mapsAndConstraints.addExplanations(explanations);
			mapsAndConstraints.setInvalidFlag(true);
			list.add(mapsAndConstraints);
			return list;
		}
		// Go through each input parameter variable, and set it's value to one
		// from the
		// temporary api (if the input variable doesn't already have a
		// predefined value)

		// TODO: ASSUMPTION: No more Variables will be introduced in Q4.2. Is
		// this valid ?
		for (Variable var : inputMaps.values()) {
			if (var.isParameterVariable() && 
					(var.getBinding() == null || var.getBinding().getValue() == null)) {
				KBObject varobj = tapi.getResource(variableNameMap.get(var));
				KBObject origvarobj = tapi.getResource(var.getID());
				KBObject val = tapi.getPropertyValue(varobj, dmap.get("hasValue"));
				if (val != null && val.getValue() != null) {
					tapi.addTriple(origvarobj, tapi.getResource(this.srns + "hasParameterValue"), val);
					var.setBinding(new ValueBinding(val.getValue()));
				}
			}
		}

		// To create the output Variable metrics, we go through the metrics
		// property of the
		// output data variables and get their values

		// TODO: Check that the SubpropertiesOf function returns *ALL*
		// subproperties
		ArrayList<KBObject> metricProps = this.api.getSubPropertiesOf(omap.get("hasMetrics"));
		metricProps.addAll(this.api.getSubPropertiesOf(dmap.get("hasDataMetrics")));

		// TODO: ************FIXME******************
		//
		// ---- CHECK DIMENSIONALITY !!!! ----------
		//
		// TODO: ***********************************
		
		for (Variable var : outputMaps.values()) {
			if (var.isDataVariable()) {
				StringBuilder metrics = new StringBuilder();

				KBObject varobj = tapi.getResource(variableNameMap.get(var));
				
				// Create Metrics from PC Properties
				for (KBObject metricProp : metricProps) {
					KBObject val = tapi.getPropertyValue(varobj, metricProp);
					// If no value set, then continue onto the next property
					if (val == null)
						continue;
					Object valobj = "";
					Class valcls = null;
					
					if (val.isLiteral()) {
						valobj = val.getValue();
						valcls = val.getValue().getClass();
					}
					else
						valobj = val.getName();
					
					String metricPropType = metricPropDCTypeMap.get(metricProp.getName());
					metrics.append(MetricsHelper.getMetricXML(metricProp.getName(), valobj, metricPropType, valcls));
				}
				ArrayList<KBObject> clses = tapi.getAllClassesOfInstance(varobj, true);
				for(KBObject cls : clses)
					metrics.append(MetricsHelper.getMetricXML("type", cls.getName(), "rdfMetrics", null));

				if (var.getBinding() != null)
					var.getBinding().setMetrics(MetricsHelper.HEADER + metrics.toString() + MetricsHelper.FOOTER);
				
				
				// -- Use other PC Properties --
				
				// Use hasDimensionSizes to fill-in some Output Bindings
				int dim=0;
				int [] dimSizes = new int[10]; // not more than 10 dimensions
				KBObject dimSizesObj = tapi.getPropertyValue(varobj, dmap.get("hasDimensionSizes"));
				
				String [] dimIndexProps = new String[10]; // not more than 10 dimensions
				KBObject dimIndexPropsObj = tapi.getPropertyValue(varobj, dmap.get("hasDimensionIndexProperties"));
				
				if(dimSizesObj != null && dimSizesObj.getValue() != null) {
					if(dimSizesObj.getValue().getClass().getName().equals("java.lang.Integer")) {
						dimSizes[0] = (Integer)dimSizesObj.getValue();
						dim = 1;
					}
					else {
						String dimSizesStr = (String) dimSizesObj.getValue();
						for(String dimSize:  dimSizesStr.split(",")) {
							try {
								int size = Integer.parseInt(dimSize);
								dimSizes[dim] = size;
								dim++;
							}
							catch (Exception e) { }
						}
					}
				}
				if(dimIndexPropsObj != null && dimIndexPropsObj.getValue() != null) {
					int xdim = 0;
					String dimIndexPropsStr = (String) dimIndexPropsObj.getValue();
					for(String dimIndexProp:  dimIndexPropsStr.split(",")) {
						try {
							dimIndexProps[xdim] = dimIndexProp;
							xdim++;
						}
						catch (Exception e) { }
					}
				}
				
				if(dim > 0) {
					int [] dimCounters = new int[dim];
					dimCounters[0] = 1;
					for(int k=1; k<dim; k++) {
						int perms = 1;
						for(int l=k-1; l>=0; l--) perms *= dimSizes[l];
						dimCounters[k] = dimCounters[k-1] + perms;
					}

					Binding b = var.getBinding();
					ArrayList<Binding> vbs = new ArrayList<Binding>();
					vbs.add(b);
					int counter=0;
					while(!vbs.isEmpty()) {
						Binding vb = vbs.remove(0);
						StringBuilder vMetric = new StringBuilder();
						if(vb.getMetrics() == null) continue;
						vMetric.append(vb.getMetrics().replace(MetricsHelper.FOOTER, ""));
						int vdim = 0;
						for(vdim=0; vdim < dim; vdim++) {
							if(counter < dimCounters[vdim])
								break;
						}
						if(vdim < dim) {
							for(int i=0; i<dimSizes[vdim]; i++) {
								Binding cvb = new Binding(b.getNamespace()+UuidGen.generateAUuid(""+i));
								String prop = dimIndexProps[vdim];
								if(prop != null && !prop.equals("")) {
									StringBuilder tmpMetrics = new StringBuilder();
									tmpMetrics.append(MetricsHelper.getMetricXML(prop, i, "dataMetrics", ((Object)i).getClass()));
									cvb.setMetrics(vMetric.toString() + tmpMetrics.toString() + MetricsHelper.FOOTER);
								}
								vb.add(cvb);
								vbs.add(cvb);
							}
						}
						counter++;
					}
				}
			}
		}
		// FIXME: Handle multiple configurations
		list.add(mapsAndConstraints);
		return list;
	}

	public boolean connect(Properties properties) {
		// TODO Auto-generated method stub
		return false;
	}

	public void close() {
	}

	// TODO : Read this information from the Ontology
	public List<TransformationCharacteristics> findCandidateInstallations(ComponentVariable c) {
		List result = new ArrayList();
		TransformationCharacteristics t = new TransformationCharacteristics();
		t.setCharacteristic(TransformationCharacteristics.SITE_HANDLE, "isi_viz");
		t.setCharacteristic(TransformationCharacteristics.COMPILATION_METHOD, "INSTALLED");
		t.setCharacteristic(TransformationCharacteristics.NAME, c.getID());
		result.add(t);

		return result;
	}

	// TODO : Read this information from the Ontology
	public List<TransformationCharacteristics> getDeploymentRequirements(ComponentVariable c, String site) {

		List result = new ArrayList();
		// System.out.println( "Component is " + c.getURIName() );
		// sanity check for pegasus executables
		if (c.getName().trim().equals("transfernull") || c.getName().trim().equals("dirmanagernull") || c.getName().trim().equals("cleanupnull")
				|| c.getName().trim().equals("rc-clientnull")) {
			return result;
		}

		TransformationCharacteristics t = new TransformationCharacteristics();

		String ctype = c.getComponentType();
		String ctypename = ctype.substring(ctype.lastIndexOf('#') + 1);

		t.setCharacteristic(TransformationCharacteristics.CODE_LOCATION, "/nfs/software/anchor/sr12/linux86/weka/" + ctypename);
		// because of the way the weka execs are distributed.
		// t.setCharacteristic( t.CODE_LOCATION,
		// "/nfs/shared-scratch/vahi/tangram/linux86/weka/" + c.getURIName() +
		// "/" + c.getURIName().toLowerCase() );
		t.setCharacteristic(TransformationCharacteristics.SITE_HANDLE, "isi_viz");
		t.setCharacteristic(TransformationCharacteristics.COMPILATION_METHOD, "INSTALLED");
		t.setCharacteristic(TransformationCharacteristics.NAME, c.getID());

		// add the weka specific environment variables
		t.setCharacteristic(TransformationCharacteristics.ENVIRONMENT_VARIABLE, new EnvironmentVariable("WEKA_HOME", "/nfs/software/anchor/sr12/weka"));
		t
				.setCharacteristic(TransformationCharacteristics.ENVIRONMENT_VARIABLE, new EnvironmentVariable("CLASSPATH",
						"/nfs/software/anchor/sr12/weka/weka.jar"));
		t.setCharacteristic(TransformationCharacteristics.ENVIRONMENT_VARIABLE, new EnvironmentVariable("PATH", "/nfs/software/java/default/bin"));

		result.add(t);
		return result;
	}

	// TODO : Add Jena rules to infer performance numbers
	public List<TransformationCharacteristics> getPredictedPerformance(ComponentMapsAndRequirements mapsAndConstraints, String site, String architecture) {
		List result = new ArrayList();
		int runtime = 60;

		// Extract info from mapsAndConstraints object
		ComponentVariable c = mapsAndConstraints.getComponent();

		String ctype = c.getComponentType();
		String cid = ctype.substring(ctype.lastIndexOf('#') + 1).toLowerCase();

		HashMap<String, Variable> inputMaps = mapsAndConstraints.getStringIndexedInputMaps();
		if (cid.matches(".*j48.*")) {
			runtime = 10;
		} else if (cid.matches(".*id3.*")) {
			runtime = 20;
		} else if (cid.matches(".*naivebayes.*")) {
			runtime = 30;
		} else if (cid.matches(".*lmt.*")) {
			runtime = 40;
		} else if (cid.matches(".*hnb.*")) {
			runtime = 50;
		}
		int maxInputSize = 10000;
		for (Variable ivar : inputMaps.values()) {
			if (ivar.isDataVariable()) {
				String metrics = ivar.getBinding().getMetrics();
				HashMap<String, ArrayList> propValMap = MetricsHelper.parseMetricsXML(metrics);
				if (propValMap.get("hasSizeInKB") != null) {
					Integer sizeInKB = (Integer) propValMap.get("hasSizeInKB").get(0);
					// System.out.println(sizeInKB);
					if (maxInputSize < sizeInKB.intValue()) {
						maxInputSize = sizeInKB.intValue();
					}
				}
			}
		}
		runtime = (runtime * maxInputSize) / 100;
		// System.out.println("Runtime : "+runtime);

		TransformationCharacteristics t = new TransformationCharacteristics();
		t.setCharacteristic(TransformationCharacteristics.SITE_HANDLE, site);
		t.setCharacteristic(TransformationCharacteristics.COMPILATION_METHOD, "INSTALLED");
		t.setCharacteristic(TransformationCharacteristics.ARCHITECTURE, architecture);
		t.setCharacteristic(TransformationCharacteristics.EXPECTED_RUNTIME, new Integer(runtime));

		result.add(t);

		return result;
	}

}
