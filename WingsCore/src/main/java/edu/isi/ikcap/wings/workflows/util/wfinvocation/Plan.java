package edu.isi.ikcap.wings.workflows.util.wfinvocation;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;

import edu.isi.ikcap.ontapi.KBAPI;
import edu.isi.ikcap.ontapi.KBObject;
import edu.isi.ikcap.ontapi.OntFactory;
import edu.isi.ikcap.ontapi.OntSpec;
import edu.isi.ikcap.wings.workflows.util.PropertiesHelper;

public class Plan {
	String id;
	int index;
	HashMap<String, Step> steps;
	HashMap<String, Variable> variables;
	
	String wfinst = "http://purl.org/net/wf-invocation";
	String pplan = "http://purl.org/net/p-plan";
	
	public Plan(String id) {
		this.id = id;
		this.index = 0;
		steps = new HashMap<String, Step>();
		variables = new HashMap<String, Variable>();
	}
	
	public void setIndex(int i) {
		this.index = i;
	}
	
	public String getID() {
		return this.id+"_"+this.index;
	}
	
	public Step getStep(String id) {
		if(steps.containsKey(id))
			return steps.get(id);
		Step s = new Step(id);
		steps.put(id, s);
		return s;
	}
	
	public Variable getVariable(String id, String binding) {
		if(variables.containsKey(id))
			return variables.get(id);
		Variable v = new Variable(id, binding);
		variables.put(id,  v);
		return v;
	}
	
	public String toRDF() {
		OntFactory fac = new OntFactory(OntFactory.JENA);
		KBAPI api = fac.getAPI(OntSpec.MICRO);
		KBAPI wfapi = fac.getAPI(this.wfinst, OntSpec.PLAIN);
		KBAPI papi = fac.getAPI(this.pplan, OntSpec.PLAIN);
		
		String base = PropertiesHelper.getSeedURL()+"/"+this.getFile();
		String ns = base+"#";

		api.setPrefixNamespace("wf-instance", wfinst+"#");
		api.setPrefixNamespace("p-plan", pplan+"#");
		api.setPrefixNamespace("", ns);
		
		KBObject stepcls = wfapi.getConcept(wfinst+"#Step");
		KBObject varcls = wfapi.getConcept(wfinst+"#Variable");
		KBObject plancls = papi.getConcept(pplan+"#Plan");
		
		KBObject isstepofplanprop = papi.getProperty(pplan+"#isStepOfPlan");
		KBObject isvarofplanprop = papi.getProperty(pplan+"#isVariableOfPlan");
		KBObject invarprop = papi.getProperty(pplan+"#hasInputVar");
		KBObject invlineprop = papi.getProperty(pplan+"#hasInvocationLine");
		KBObject outvarprop = papi.getProperty(pplan+"#hasOutputVar");
		KBObject cbindingprop = wfapi.getProperty(wfinst+"#hasCodeBinding");
		KBObject dbindingprop = wfapi.getProperty(wfinst+"#hasDataBinding");
		//KBObject outvarprop = papi.getProperty(pplan+"#isOutputVarOf");
		
		KBObject planobj = api.createObjectOfClass(ns+this.id, plancls);
		
		HashMap<String, KBObject> varmap = new HashMap<String, KBObject>();
		for(Variable v: variables.values()) {
			KBObject varobj = api.createObjectOfClass(ns+v.getID(), varcls);
			api.addPropertyValue(varobj, isvarofplanprop, planobj);
			api.addPropertyValue(varobj, dbindingprop, 
					fac.getDataObject(v.getDataBinding()));
			varmap.put(v.getID(), varobj);
		}
		
		for(Step s: steps.values()) {
			KBObject stepobj = api.createObjectOfClass(ns+s.getID(), stepcls);
			api.addPropertyValue(stepobj, isstepofplanprop, planobj);
			api.addPropertyValue(stepobj, cbindingprop, 
					fac.getDataObject(s.getCodeBinding()));
			api.addPropertyValue(stepobj, invlineprop, 
					fac.getDataObject(s.getInvocationLine()));

			for(Variable v: s.getInputVariables()) {
				KBObject varobj = varmap.get(v.getID());
				api.addPropertyValue(stepobj, invarprop, varobj);
			}
			for(Variable v: s.getOutputVariables()) {
				KBObject varobj = varmap.get(v.getID());
				api.addPropertyValue(stepobj, outvarprop, varobj);
			}
		}
		
		return api.toN3();
	}
	
	public String getFile() {
		return getID() + ".pplan.ttl";
	}
	
	public void write(String filePath) {
		try {
			PrintStream out = new PrintStream(new FileOutputStream(filePath));
			out.println(this.toRDF());
			out.close();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
}
