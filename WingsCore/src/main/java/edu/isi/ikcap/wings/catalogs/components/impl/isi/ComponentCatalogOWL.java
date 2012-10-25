package edu.isi.ikcap.wings.catalogs.components.impl.isi;

import edu.isi.ikcap.ontapi.*;
import edu.isi.ikcap.wings.catalogs.components.ComponentCatalog;
import edu.isi.ikcap.wings.catalogs.components.classes.ComponentMapsAndRequirements;
import edu.isi.ikcap.wings.catalogs.components.classes.TransformationCharacteristics;
import edu.isi.ikcap.wings.workflows.template.Role;
import edu.isi.ikcap.wings.workflows.template.sets.Binding;
import edu.isi.ikcap.wings.workflows.template.sets.WingsSet;
import edu.isi.ikcap.wings.workflows.template.variables.ComponentVariable;
import edu.isi.ikcap.wings.workflows.template.variables.Variable;
import edu.isi.ikcap.wings.workflows.util.MetricsHelper;
import edu.isi.ikcap.wings.workflows.util.PropertiesHelper;
import edu.isi.ikcap.wings.workflows.util.WflowGenFactory;
import org.apache.log4j.Logger;

import java.util.*;

public class ComponentCatalogOWL implements ComponentCatalog {

	protected KBAPI api;

	protected String dcns;
	protected String pcns;

	protected String pcdomns;
	protected String dcdomns;

	protected String srns;

	protected Logger logger;

	protected HashMap<String, KBObject> objPropMap;
	protected HashMap<String, KBObject> dataPropMap;
	protected HashMap<String, KBObject> conceptMap;

	private ArrayList<KBObject> owlObjectProperties;
	private ArrayList<KBObject> owlDatatypeProperties;
	private ArrayList<KBObject> owlConcepts;

	protected OntFactory ontologyFactory;

	public ComponentCatalogOWL() {
	}

	public ComponentCatalogOWL(String ldid) {
		HashMap<String, String> nsmap = PropertiesHelper.getDCPrefixNSMap();
		nsmap.putAll(PropertiesHelper.getPCPrefixNSMap());
		this.dcns = nsmap.get("dc");
		this.pcns = nsmap.get("ac");
		this.dcdomns = nsmap.get("dcdom");
		this.pcdomns = nsmap.get("acdom");
		this.srns = PropertiesHelper.getWorkflowOntologyURL() + "#";

		this.ontologyFactory = new OntFactory(OntFactory.JENA);
		WflowGenFactory.setLocalRedirects(this.ontologyFactory);

		this.api = this.ontologyFactory.getAPI(PropertiesHelper.getPCDomainURL() + "/library.owl", OntSpec.PELLET);
		this.api.importFrom(this.ontologyFactory.getAPI(PropertiesHelper.getDCDomainURL() + "/ontology.owl", OntSpec.PELLET));
		this.api.importFrom(this.ontologyFactory.getAPI(PropertiesHelper.getPCURL() + "/ontology.owl", OntSpec.PELLET));
		// this.api.importFrom(ontologyFactory.getAPI(dcUrl, OntSpec.PELLET));

		initializeMaps();

		this.setRequestId(ldid);
	}

	// When an explicit library name is passed in, use  abstract.owl and the libname.owl
	public ComponentCatalogOWL(String libname, String ldid) {
		HashMap<String, String> nsmap = PropertiesHelper.getDCPrefixNSMap();
		nsmap.putAll(PropertiesHelper.getPCPrefixNSMap());
		this.dcns = nsmap.get("dc");
		this.pcns = nsmap.get("ac");
		this.dcdomns = nsmap.get("dcdom");
		this.pcdomns = nsmap.get("acdom");
		this.srns = PropertiesHelper.getWorkflowOntologyURL() + "#";

		this.ontologyFactory = new OntFactory(OntFactory.JENA);
		WflowGenFactory.setLocalRedirects(this.ontologyFactory);

		// Load Abstract Components
		this.api = this.ontologyFactory.getAPI(PropertiesHelper.getPCDomainURL() + "/abstract.owl", OntSpec.PELLET);
		
		// Load Concrete Components from the Library passed in to the constructor
		this.api.importFrom(this.ontologyFactory.getAPI(PropertiesHelper.getPCDomainURL() + "/"+libname+".owl", OntSpec.PELLET));

		// Load DC library and ontology
		this.api.importFrom(this.ontologyFactory.getAPI(PropertiesHelper.getDCDomainURL() + "/library.owl", OntSpec.PELLET));
		this.api.importFrom(this.ontologyFactory.getAPI(PropertiesHelper.getDCDomainURL() + "/ontology.owl", OntSpec.PELLET));
		
		// Load PC Ontology
		this.api.importFrom(this.ontologyFactory.getAPI(PropertiesHelper.getPCURL() + "/ontology.owl", OntSpec.PELLET));
		// this.api.importFrom(ontologyFactory.getAPI(dcUrl, OntSpec.PELLET));

		initializeMaps();

		this.setRequestId(ldid);
	}

	protected void initializeMaps() {
		this.objPropMap = new HashMap<String, KBObject>();
		this.dataPropMap = new HashMap<String, KBObject>();
		this.conceptMap = new HashMap<String, KBObject>();

		this.owlObjectProperties = new ArrayList<KBObject>();
		this.owlDatatypeProperties = new ArrayList<KBObject>();
		this.owlConcepts = new ArrayList<KBObject>();

		this.owlObjectProperties = this.api.getAllObjectProperties();
		this.owlDatatypeProperties = this.api.getAllDatatypeProperties();
		this.owlConcepts = this.api.getAllClasses();

		for (KBObject prop : owlObjectProperties) {
			this.objPropMap.put(prop.getName(), prop);
		}
		for (KBObject con : owlConcepts) {
			this.conceptMap.put(con.getName(), con);
		}
		for (KBObject odp : owlDatatypeProperties) {
			this.dataPropMap.put(odp.getName(), odp);
		}
	}

	/**
	 * <b>Query 2.1</b><br/>
	 * Get a list of Concrete Components with their IO Data Requirements given a
	 * component (maybe abstract) and it's IO Data Requirements
	 * 
	 * @param mapsAndConstraints
	 *            A ComponentMapsAndRequirements Object which contains:
	 *            <ul>
	 *            <li>component,
	 *            <li>maps of component input arguments to template variables,
	 *            <li>maps of component output arguments to template variables,
	 *            <li>template variable descriptions (dods) - list of triples
	 *            </ul>
	 * 
	 * @return list of ComponentMapsAndRequirements Objects for each specialized
	 *         component
	 */
	public ArrayList<ComponentMapsAndRequirements> specializeAndFindDataRequirements(ComponentMapsAndRequirements mapsAndConstraints) {

		ArrayList<ComponentMapsAndRequirements> listOfCmrs;
		listOfCmrs = new ArrayList<ComponentMapsAndRequirements>();

		HashMap<String, KBObject> omap = this.objPropMap;
		HashMap<String, KBObject> dmap = this.dataPropMap;

		ArrayList<KBObject> metricProps = this.api.getSubPropertiesOf(omap.get("hasMetrics"));
		metricProps.addAll(this.api.getSubPropertiesOf(dmap.get("hasDataMetrics")));

		// Extract info from mapsAndConstraints object
		ComponentVariable c = mapsAndConstraints.getComponent();
		HashMap<String, Variable> inputMaps = mapsAndConstraints.getStringIndexedInputMaps();
		HashMap<String, Variable> outputMaps = mapsAndConstraints.getStringIndexedOutputMaps();
		ArrayList<KBTriple> constraints = mapsAndConstraints.getRequirements();

		// Get Component object
		KBObject comp = this.api.getResource(c.getID());

		// Get a mapping of I/O Var's to ArgID's (with property hasArgumentID)
		// for the Passed Component
		ArrayList<KBObject> inputArgs = this.api.getPropertyValues(comp, omap.get("hasInput"));
		ArrayList<KBObject> outputArgs = this.api.getPropertyValues(comp, omap.get("hasOutput"));
		HashMap<String, Variable> inputVarMaps = new HashMap<String, Variable>();
		HashMap<String, Variable> outputVarMaps = new HashMap<String, Variable>();

		for (KBObject iarg : inputArgs) {
			KBObject argid = this.api.getDatatypePropertyValue(iarg, dmap.get("hasArgumentID"));
			inputVarMaps.put((String) argid.getValue(), inputMaps.get(iarg.getID()));
		}
		for (KBObject oarg : outputArgs) {
			KBObject argid = this.api.getDatatypePropertyValue(oarg, dmap.get("hasArgumentID"));
			outputVarMaps.put((String) argid.getValue(), outputMaps.get(oarg.getID()));
		}

		// Get List of all concrete components
		ArrayList<KBObject> ccomps = new ArrayList<KBObject>();
		KBObject isConcrete = this.api.getDatatypePropertyValue(comp, dmap.get("isConcrete"));
		if (isConcrete != null && ((Boolean) isConcrete.getValue()).booleanValue()) {
			ccomps.add(comp);
		} else {
			KBObject cls = this.api.getClassOfInstance(comp);
			if (cls == null) {
				return listOfCmrs;
			}
			ArrayList<KBObject> insts = this.api.getInstancesOfClass(cls);
			for (KBObject inst : insts) {
				isConcrete = this.api.getDatatypePropertyValue(inst, dmap.get("isConcrete"));
				if (isConcrete != null && ((Boolean) isConcrete.getValue()).booleanValue()) {
					ccomps.add(inst);
				}
			}
		}

		// For All concrete components :
		// - Get mapping of specialized arguments to variables
		// - Transfer "relevant" output variable properties to input variables
		// - Pass back the specialized component + specialized mappings +
		// modified red-box
		for (KBObject ccomp : ccomps) {
			inputArgs = this.api.getPropertyValues(ccomp, omap.get("hasInput"));
			outputArgs = this.api.getPropertyValues(ccomp, omap.get("hasOutput"));
			ArrayList<Variable> inputVars = new ArrayList<Variable>();
			ArrayList<Variable> outputVars = new ArrayList<Variable>();

			HashMap<Role, Variable> sInputMaps = new HashMap<Role, Variable>();
			HashMap<Role, Variable> sOutputMaps = new HashMap<Role, Variable>();

			for (KBObject iarg : inputArgs) {
				KBObject argid = this.api.getDatatypePropertyValue(iarg, dmap.get("hasArgumentID"));
				Variable var = inputVarMaps.get((String) argid.getValue());
				sInputMaps.put(new Role(iarg.getID()), var);
				inputVars.add(var);
			}
			for (KBObject oarg : outputArgs) {
				KBObject argid = this.api.getDatatypePropertyValue(oarg, dmap.get("hasArgumentID"));
				Variable var = outputVarMaps.get((String) argid.getValue());
				sOutputMaps.put(new Role(oarg.getID()), var);
				outputVars.add(var);
			}

			// This implementation does propagation it in a very simplistic way
			// using property domains

			HashMap<Variable, Boolean> classified = new HashMap<Variable, Boolean>();

			// Create an api to access the passed redbox
			KBAPI tmpapi = this.ontologyFactory.getAPI(OntSpec.PLAIN);
			tmpapi.addTriples(constraints);
			for (Variable ovar : outputVars) {
				KBObject ov = tmpapi.getResource(ovar.getID());
				String oargid = sOutputMaps.get(ovar.getID()).getID();
				KBObject oargrs = this.api.getResource(oargid);
				ArrayList<KBObject> oargclses = this.api.getAllClassesOfInstance(oargrs, true);
				for (KBObject oargcls : oargclses) {
					// System.out.println(oargcls);
					if (!oargcls.getNamespace().equals(this.pcns)) {
						tmpapi.addClassForInstance(ov, oargcls);
					}
				}

				for (KBObject metricProp : metricProps) {
					ArrayList<KBObject> vals = tmpapi.getPropertyValues(ov, metricProp);
					if (vals != null && vals.size() > 0) {
						KBObject propDomain = this.api.getPropertyDomain(metricProp);
						// System.out.println(propDomain);
						for (KBObject val : vals) {
							for (Variable ivar : inputVars) {
								KBObject iv = tmpapi.getResource(ivar.getID());
								String iargid = sInputMaps.get(ivar.getID()).getID();
								KBObject iargrs = this.api.getResource(iargid);
								if (this.api.isA(iargrs, propDomain)) {
									if (val.isLiteral()) {
										tmpapi.setPropertyValue(iv, metricProp, val);
									} else {
										tmpapi.addPropertyValue(iv, metricProp, val);
									}
								}
								if (classified.get(ivar) == null || !classified.get(ivar)) {
									classified.put(ivar, true);
									ArrayList<KBObject> iargclses = this.api.getAllClassesOfInstance(iargrs, true);
									for (KBObject iargcls : iargclses) {
										// System.out.println(iargcls);
										if (!iargcls.getNamespace().equals(this.pcns)) {
											tmpapi.addClassForInstance(iv, iargcls);
										}
									}
								}
							}
						}
					}
				}
			}
			ComponentVariable concreteComponent = new ComponentVariable(ccomp.getID());
			ArrayList<KBTriple> newContraints = tmpapi.genericTripleQuery(null, null, null);
			ComponentMapsAndRequirements cmr;
			cmr = new ComponentMapsAndRequirements(concreteComponent, sInputMaps, sOutputMaps, newContraints);
			listOfCmrs.add(cmr);

			/*
			 * System.out.println("Specialized Component: "+ccomp);
			 * tmpapi.writeRDF(System.out);
			 * //System.out.println(tmpapi.genericTripleQuery(null, null,
			 * null)); System.out.println(sInputMaps);
			 * System.out.println(sOutputMaps);
			 * System.out.println("Redbox: \n");
			 */
		}
		return listOfCmrs;
	}

	/**
	 * <b>Query 4.2</b><br/>
	 * This function is supposed to <b>SET</b> the DataSet Metrics, or Parameter
	 * Values for the Variables that are passed in via the input/output maps as
	 * part of mapsAndConstraints.<br/>
	 * Variables will already be bound to dataObjects, so the function will have
	 * to do something like the following :
	 * <p/>
	 * 
	 * <pre>
	 * If Variable.isParameterVariable() Variable.setParameterValue(value)
	 * If Variable.isDataVariable() Variable.getDataObjectBinding().setDataMetrics(xml)
	 * </pre>
	 * 
	 * @param mapsAndConstraints
	 *            A ComponentMapsAndRequirements Object which contains:
	 *            <ul>
	 *            <li>component, <li>maps of component input arguments to
	 *            template variables, <li>maps of component output arguments to
	 *            template variables, <li>template variable descriptions (dods)
	 *            - list of triples
	 *            </ul>
	 * 
	 * @return List of extra template variable descriptions (will mostly be
	 *         empty in Q4.2 though)
	 */
	public ArrayList<ComponentMapsAndRequirements> findOutputDataPredictedDescriptions(ComponentMapsAndRequirements mapsAndConstraints) {

		ArrayList list = new ArrayList<ComponentMapsAndRequirements>();

		HashMap<String, KBObject> omap = this.objPropMap;
		HashMap<String, KBObject> dmap = this.dataPropMap;

		// Extract info from mapsAndConstraints object
		HashMap<Role, Variable> inputMaps = mapsAndConstraints.getInputMaps();
		HashMap<Role, Variable> outputMaps = mapsAndConstraints.getOutputMaps();

		HashMap<Variable, ArrayList> outputMetrics = new HashMap<Variable, ArrayList>();
		for (Role iargparam : inputMaps.keySet()) {
			Variable ivar = inputMaps.get(iargparam);
			if (ivar.isParameterVariable()) {
				continue;
			}
			String metrics = ivar.getBinding().getMetrics();
			HashMap<String, ArrayList> propValMap = MetricsHelper.parseMetricsXML(metrics);
			for (String propid : propValMap.keySet()) {
				ArrayList tmp = propValMap.get(propid);
				KBObject metricProp = omap.get(propid);
				if (metricProp == null) {
					metricProp = dmap.get(propid);
				}
				if (metricProp == null) {
					System.err.println("No Such Metrics Property Known to AC : " + propid);
					continue;
				}
				KBObject propDomain = this.api.getPropertyDomain(metricProp);
				for (Role oargparam : outputMaps.keySet()) {
					Variable ovar = outputMaps.get(oargparam);
					KBObject iargrs = this.api.getResource(oargparam.getID());
					if (this.api.isA(iargrs, propDomain)) {
						ArrayList tmp2 = outputMetrics.get(ovar);
						if (tmp2 == null) {
							tmp2 = new ArrayList();
						}
						ArrayList tmp3 = new ArrayList(tmp);
						tmp3.add(0, propid);
						tmp2.add(tmp3);
						outputMetrics.put(ovar, tmp2);
					}
				}
			}
		}

		// Create an XML for vars with some new metrics
		// - And set metrics for the DataObject that's bound to that variable
		for (Variable var : outputMetrics.keySet()) {
			ArrayList<ArrayList> l = outputMetrics.get(var);
			String metric = MetricsHelper.HEADER;
			for (ArrayList tmp : l) {
				String propName = (String) tmp.get(0);
				Object value = tmp.get(1);
				String metricName = (String) tmp.get(2);
				metric += MetricsHelper.getMetricXML(propName, value, metricName, value.getClass());
			}
			metric += MetricsHelper.FOOTER;
			// System.out.println("Binding "+var.getDataObjectBinding()+" to : \n"+metric);
			var.getBinding().setMetrics(metric);
		}
		// Set any Parameter Bindings here .. None for now, so returning an
		// empty list
		list.add(mapsAndConstraints);
		return list;
	}

	/**
	 * <b>Query 4.5</b><br/>
	 * Given a component, it's mapping of arguments to template variables.
	 * Assumption is that the all Parameter Variables have values, and all Data
	 * Variables have DataObject Bindings with metrics (if any)
	 * 
	 * @param mapsAndConstraints
	 *            A ComponentMapsAndRequirements Object which contains:
	 *            <ul>
	 *            <li>component,
	 *            <li>maps of component input arguments to template variables,
	 *            <li>maps of component output arguments to template variables,
	 *            <li>template variable descriptions (dods) - list of triples
	 *            </ul>
	 * 
	 * @return String The Invocation String that is used in the DAX by pegasus
	 */
	public String getInvocationCommand(ComponentMapsAndRequirements mapsAndConstraints) {
		HashMap<String, KBObject> omap = this.objPropMap;
		HashMap<String, KBObject> dmap = this.dataPropMap;

		// Extract info from mapsAndConstraints object
		ComponentVariable c = mapsAndConstraints.getComponent();
		HashMap<String, Variable> inputMaps = mapsAndConstraints.getStringIndexedInputMaps();
		HashMap<String, Variable> outputMaps = mapsAndConstraints.getStringIndexedOutputMaps();

		// Get Component
		KBObject comp = this.api.getResource(c.getComponentType());
		ArrayList<KBObject> args = this.api.getPropertyValues(comp, omap.get("hasArgument"));

		TreeMap<Integer, KBObject> indexedArguments = new TreeMap<Integer, KBObject>();
		HashMap<String, KBObject> namedArguments = new HashMap<String, KBObject>();

		ArrayList<String> argstrs = new ArrayList<String>();

		for (KBObject arg : args) {
			KBObject indexobj = this.api.getPropertyValue(arg, dmap.get("hasArgumentIndex"));
			KBObject prefixobj = this.api.getPropertyValue(arg, dmap.get("hasArgumentName"));

			if (indexobj != null && indexobj.getValue() != null) {
				indexedArguments.put((Integer) indexobj.getValue(), arg);
			} else if (prefixobj != null && prefixobj.getValue() != null) {
				namedArguments.put((String) prefixobj.getValue(), arg);
			} else {
				System.err.println("Component " + c.getName() + " has an invalid Argument: " + arg.getName());
			}
		}

		for (KBObject arg : indexedArguments.values()) {
			argstrs.addAll(getArgumentStringsForArgumentObject(arg, inputMaps, outputMaps));
		}

		for (String prefix : namedArguments.keySet()) {
			KBObject arg = namedArguments.get(prefix);
			argstrs.addAll(getArgumentStringsForArgumentObject(arg, inputMaps, outputMaps));
		}

		int i = 0;
		String argstr = "";
		for (String argument : argstrs) {
			if (i > 0) {
				argstr += " ";
			}
			argstr += argument;
			i++;
		}
		return argstr;
	}

	private ArrayList<String> getArgumentStringsForArgumentObject(KBObject arg, HashMap<String, Variable> inputMaps, HashMap<String, Variable> outputMaps) {

		ArrayList<String> argstrs = new ArrayList<String>();
		HashMap<String, KBObject> dmap = this.dataPropMap;

		KBObject defaultValue = this.api.getPropertyValue(arg, dmap.get("hasValue"));

		// Get the argument ID for the specialized argument
		KBObject argid = this.api.getDatatypePropertyValue(arg, dmap.get("hasArgumentID"));

		String role = this.pcdomns + argid.getValue();
		Variable var = inputMaps.get(role);
		if (var == null) {
			var = outputMaps.get(role);
		}
		// If the argument does not correspond to either an input or an output
		if (var == null) {
			return argstrs;
		}

		String prefix = null;
		KBObject prefixobj = this.api.getPropertyValue(arg, dmap.get("hasArgumentName"));

		if (prefixobj != null && prefixobj.getValue() != null) {
			prefix = (String) prefixobj.getValue();
		}

		String pstr = prefix != null ? prefix + " " : "";

		ArrayList<Binding> bindings = new ArrayList<Binding>();
		bindings.add(var.getBinding());
		while (!bindings.isEmpty()) {
			Binding b = bindings.remove(0);
			// System.out.println(var.getName() + ":" + b);
			if (b.isSet()) {
				for (WingsSet s : b) {
					bindings.add((Binding) s);
				}
			} else {
				if (var.isDataVariable()) {
					argstrs.add(pstr + "<filename file=\"" + b.getName() + "\"/>");
				} else if (var.isParameterVariable()) {
					if (var.getBinding() != null) {
						argstrs.add(pstr + var.getBinding());
					} else if (defaultValue != null && defaultValue.getValue() != null) {
						argstrs.add(pstr + defaultValue.getValue());
					}
				}
			}
		}
		return argstrs;
	}

	public boolean connect(Properties properties) {
		// TODO Auto-generated method stub
		return false;
	}

	public void close() {
	}

	public List<TransformationCharacteristics> findCandidateInstallations(ComponentVariable c) {
		List result = new ArrayList();
		TransformationCharacteristics t = new TransformationCharacteristics();
		t.setCharacteristic(TransformationCharacteristics.SITE_HANDLE, "isi_viz");
		t.setCharacteristic(TransformationCharacteristics.COMPILATION_METHOD, "INSTALLED");
		result.add(t);

		// TODO Auto-generated method stub
		return result;
	}

	public List<TransformationCharacteristics> getDeploymentRequirements(ComponentVariable c, String site) {

		List result = new ArrayList();
		TransformationCharacteristics t = new TransformationCharacteristics();
		t.setCharacteristic(TransformationCharacteristics.CODE_LOCATION, "/nfs/asd2/" + c.getID());
		t.setCharacteristic(TransformationCharacteristics.SITE_HANDLE, "isi_viz");
		t.setCharacteristic(TransformationCharacteristics.COMPILATION_METHOD, "INSTALLED");
		result.add(t);

		// TODO Auto-generated method stub
		return result;
	}

	public List<TransformationCharacteristics> getPredictedPerformance(ComponentMapsAndRequirements mapsAndConstraints, String site, String architecture) {
		// TODO Auto-generated method stub
		return null;
	}

	public ArrayList<ComponentVariable> getAllComponentTypes() {
		ArrayList<ComponentVariable> comps = new ArrayList<ComponentVariable>();
		KBObject cprop = api.getProperty(pcns + "isConcrete");
		if (api.getConcept(pcns + "Component") == null) {
			return comps;
		}
		for (KBObject obj : api.getInstancesOfClass(api.getConcept(pcns + "Component"))) {
			if (obj.getID() == null)
				continue;
			KBObject cval = api.getDatatypePropertyValue(obj, cprop);
			ComponentVariable c = new ComponentVariable(obj.getID());
			c.setComponentType(obj.getID());
			if (cval != null && cval.getValue() != null && (Boolean) cval.getValue()) {
				c.setConcrete(true);
			}
			comps.add(c);
		}
		return comps;
	}

	public ArrayList<Role> getComponentInputs(ComponentVariable c) {
		ArrayList<Role> cps = new ArrayList<Role>();
		KBObject comp = api.getIndividual(c.getComponentType());

		if (comp == null) {
			return null;
		}

		for (KBObject obj : api.getPropertyValues(comp, api.getProperty(pcns + "hasInput"))) {

			KBObject argid = this.api.getDatatypePropertyValue(obj, dataPropMap.get("hasArgumentID"));

			Role cp = new Role(this.pcdomns + argid.getValue());
			if (api.isA(obj, api.getConcept(pcns + "ParameterArgument"))) {
				cp.setType(Role.PARAMETER);
			}
			KBObject dim = this.api.getDatatypePropertyValue(obj, dataPropMap.get("hasDimensionality"));
			if (dim != null && dim.getValue() != null) {
				cp.setDimensionality((Integer) dim.getValue());
			}
			cps.add(cp);
		}
		return cps;
	}

	public ArrayList<Role> getComponentOutputs(ComponentVariable c) {
		ArrayList<Role> cps = new ArrayList<Role>();
		KBObject comp = api.getIndividual(c.getComponentType());
		if (comp == null) {
			return null;
		}
		for (KBObject obj : api.getPropertyValues(comp, api.getProperty(pcns + "hasOutput"))) {
			KBObject argid = this.api.getDatatypePropertyValue(obj, dataPropMap.get("hasArgumentID"));
			Role cp = new Role(this.pcdomns + argid.getValue());
			if (api.isA(obj, api.getConcept(pcns + "ParameterArgument"))) {
				cp.setType(Role.PARAMETER);
			}
			KBObject dim = this.api.getDatatypePropertyValue(obj, dataPropMap.get("hasDimensionality"));
			if (dim != null && dim.getValue() != null) {
				cp.setDimensionality((Integer) dim.getValue());
			}
			cps.add(cp);
		}
		return cps;
	}

	public void setRequestId(String id) {
		this.logger = PropertiesHelper.getLogger(this.getClass().getName(), id);
	}

	public ComponentMapsAndRequirements findDataRequirements(ComponentMapsAndRequirements mapsAndConstraints) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * <p/>
	 * Returns <code>true</code> if the first argument is the identifier of a
	 * component class that subsumes the component class identified by the
	 * second argument.
	 * <p/>
	 * Note that both identifiers refer to the Skolem classes, and not to the
	 * acdm classes.
	 * 
	 * @param subsumerClassID
	 * @param subsumedClassID
	 * 
	 * @return <code>true</code> if the first class subsumes the second,
	 *         <code>false</code> in any other case
	 */
	public boolean componentSubsumes(String subsumerClassID, String subsumedClassID) {
		KBObject subsumerIndividual = api.getIndividual(subsumerClassID);
		if (subsumerIndividual == null) {
			return false;
		}
		KBObject subsumerClass = api.getClassOfInstance(subsumerIndividual);
		KBObject subsumedIndividual = api.getIndividual(subsumedClassID);
		if (subsumedIndividual == null) {
			return false;
		}
		KBObject subsumedClass = api.getClassOfInstance(subsumedIndividual);
		boolean output = api.hasSubClass(subsumerClass, subsumedClass);
		return output;
	}

	public ComponentMapsAndRequirements findDataTypeRequirements(ComponentMapsAndRequirements mapsAndConstraints) {
		// TODO Auto-generated method stub
		return null;
	}

}
