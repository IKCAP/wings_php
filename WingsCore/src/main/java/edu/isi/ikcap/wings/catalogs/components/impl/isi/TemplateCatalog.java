package edu.isi.ikcap.wings.catalogs.components.impl.isi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import edu.isi.ikcap.ontapi.KBObject;
import edu.isi.ikcap.ontapi.KBTriple;
import edu.isi.ikcap.ontapi.OntFactory;
import edu.isi.ikcap.wings.catalogs.components.ComponentCatalog;
import edu.isi.ikcap.wings.catalogs.components.classes.ComponentMapsAndRequirements;
import edu.isi.ikcap.wings.catalogs.components.classes.TransformationCharacteristics;
import edu.isi.ikcap.wings.workflows.impl.StandaloneWorkflowGenerator;
import edu.isi.ikcap.wings.workflows.template.Role;
import edu.isi.ikcap.wings.workflows.template.Template;
import edu.isi.ikcap.wings.workflows.template.variables.ComponentVariable;
import edu.isi.ikcap.wings.workflows.template.variables.Variable;

public class TemplateCatalog implements ComponentCatalog {

	StandaloneWorkflowGenerator swg;
	String requestId;
	OntFactory ontfac;

	public TemplateCatalog(StandaloneWorkflowGenerator swg) {
		this.swg = swg;
		setRequestId(requestId);
		this.ontfac = new OntFactory(OntFactory.JENA);
	}

	public void close() {
		// TODO Auto-generated method stub
	}

	public boolean componentSubsumes(String subsumerClassID, String subsumedClassID) {
		// TODO: Maybe can be done later
		return false;
	}

	public boolean connect(Properties properties) {
		return false;
	}

	public List<TransformationCharacteristics> findCandidateInstallations(ComponentVariable c) {
		// TODO: Should return template urls ?
		return null;
	}

	public ComponentMapsAndRequirements findDataRequirements(ComponentMapsAndRequirements mapsAndConstraints) {
		// TODO: Should run a light backward,forward sweep ?
		return null;
	}

	public ComponentMapsAndRequirements findDataTypeRequirements(ComponentMapsAndRequirements mapsAndConstraints) {
		// TODO: Should run a type-only light backward,forward sweep ?
		return null;
	}

	// TODO/FIXME: Ignore Variable constraints for now
	public ArrayList<ComponentMapsAndRequirements> findOutputDataPredictedDescriptions(ComponentMapsAndRequirements cmr) {
		// Do a forward sweep
		ArrayList<ComponentMapsAndRequirements> list = new ArrayList<ComponentMapsAndRequirements>();

		ComponentVariable c = cmr.getComponent();
		HashMap<String, Variable> inputMaps = cmr.getStringIndexedInputMaps();
		HashMap<String, Variable> outputMaps = cmr.getStringIndexedOutputMaps();

		Template t = (Template) c.getBinding().getValue();
		if (t == null)
			return null;

		// NOTE: Role variables current do not equate to Template Variables !!!
		// Transfer bindings
		for (Role r : t.getInputRoles().keySet()) {
			Variable v = inputMaps.get(r.getID());
			if (v != null)
				t.getVariable(t.getInputRoles().get(r).getID()).setBinding(v.getBinding());
		}
		for (Role r : t.getOutputRoles().keySet()) {
			Variable v = inputMaps.get(r.getID());
			if (v != null)
				t.getVariable(t.getOutputRoles().get(r).getID()).setBinding(v.getBinding());
		}

		// Do the forward sweep on the sub-template
		ArrayList<Template> templates = swg.configureTemplates(t);
		int i = 0;
		for (Template ct : templates) {
			ct.setName(ct.getName() + "_" + i, false);

			ComponentVariable cv = new ComponentVariable(ct);
			cv.setComponentType(ct.getNamespace() + ct.getName());

			HashMap<Role, Variable> iMap = new HashMap<Role, Variable>();
			HashMap<Role, Variable> oMap = new HashMap<Role, Variable>();

			// Transfer bindings
			for (Role r : ct.getInputRoles().keySet()) {
				Variable vv = ct.getInputRoles().get(r);
				Variable v = new Variable(inputMaps.get(r.getID()).getID(), vv.getVariableType());
				v.setBinding(vv.getBinding());
				iMap.put(r, v);
			}
			for (Role r : ct.getOutputRoles().keySet()) {
				Variable vv = ct.getOutputRoles().get(r);
				Variable v = new Variable(outputMaps.get(r.getID()).getID(), vv.getVariableType());
				// FIXME: Unsure of this !!!
				v.setBinding(vv.getBinding());
				oMap.put(r, v);
			}
			ComponentMapsAndRequirements cmap = new ComponentMapsAndRequirements(cv, iMap, oMap, cmr.getRequirements());
			list.add(cmap);
			i++;
		}

		return list;
	}

	public ArrayList<ComponentVariable> getAllComponentTypes() {
		// TODO: Return all templates ?
		return null;
	}

	public ArrayList<Role> getComponentInputs(ComponentVariable c) {
		// TODO: Return template's input roles
		return null;
	}

	public ArrayList<Role> getComponentOutputs(ComponentVariable c) {
		// TODO: Return template's output roles
		return null;
	}

	public List<TransformationCharacteristics> getDeploymentRequirements(ComponentVariable c, String site) {
		// TODO: Unsure
		return null;
	}

	public String getInvocationCommand(ComponentMapsAndRequirements mapsAndConstraints) {
		// TODO: Unsure
		return null;
	}

	public List<TransformationCharacteristics> getPredictedPerformance(ComponentMapsAndRequirements mapsAndConstraints, String site, String architecture) {
		// TODO: Find overall performance of the template ?
		return null;
	}

	public void setRequestId(String id) {
		this.requestId = id;
	}

	public ArrayList<ComponentMapsAndRequirements> specializeAndFindDataRequirements(ComponentMapsAndRequirements cmr) {
		// Do a backward sweep

		ArrayList<ComponentMapsAndRequirements> list = new ArrayList<ComponentMapsAndRequirements>();

		ComponentVariable c = cmr.getComponent();
		HashMap<String, Variable> inputMaps = cmr.getStringIndexedInputMaps();
		HashMap<String, Variable> outputMaps = cmr.getStringIndexedOutputMaps();

		HashMap<String, Role> rmaps = new HashMap<String, Role>(cmr.getStringIndexedReverseInputMaps());
		rmaps.putAll(cmr.getStringIndexedReverseOutputMaps());

		Template t = c.getTemplate();
		if (t == null)
			return null;

		HashMap<Role, Variable> tr = new HashMap<Role, Variable>(t.getInputRoles());
		tr.putAll(t.getOutputRoles());

		HashMap<String, String> rvars = new HashMap<String, String>();
		for (Role r : tr.keySet())
			rvars.put(r.getID(), tr.get(r).getID());

		// Transfer constraints to the sub-template
		ArrayList<KBTriple> triplesp = cmr.getRequirements();
		ArrayList<KBTriple> constraints = new ArrayList<KBTriple>();

		for (KBTriple triple : triplesp) {
			KBObject obj = triple.getObject();
			KBObject subj = triple.getSubject();
			if (subj != null && rmaps.containsKey(subj.getID())) {
				subj = ontfac.getObject(rvars.get(rmaps.get(subj.getID()).getID()));
			}
			if (obj != null && rmaps.containsKey(obj.getID())) {
				obj = ontfac.getObject(rvars.get(rmaps.get(obj.getID()).getID()));
			}
			triple.setSubject(subj);
			triple.setObject(obj);
			constraints.add(triple);
		}
		t.getConstraintEngine().addConstraints(constraints);

		// Do the backward sweep on the sub-template
		ArrayList<Template> templates = swg.specializeTemplates(t);
		int i = 0;
		for (Template ct : templates) {
			ct.setName(ct.getName() + "_" + i, false);

			ComponentVariable cv = new ComponentVariable(ct);
			cv.setComponentType(ct.getNamespace() + ct.getName());

			HashMap<Role, Variable> iMap = new HashMap<Role, Variable>();
			HashMap<Role, Variable> oMap = new HashMap<Role, Variable>();
			HashMap<String, String> varMaps = new HashMap<String, String>();
			for (Role r : ct.getInputRoles().keySet()) {
				Variable v = inputMaps.get(r.getID());
				Variable vv = ct.getInputRoles().get(r);
				if (v == null) {
					v = new Variable(t.getNamespace() + vv.getName(), vv.getVariableType());
				}
				varMaps.put(vv.getID(), v.getID());
				iMap.put(r, v);
			}
			for (Role r : ct.getOutputRoles().keySet()) {
				Variable v = outputMaps.get(r.getID());
				Variable vv = ct.getOutputRoles().get(r);
				if (v == null) {
					v = new Variable(t.getNamespace() + vv.getName(), vv.getVariableType());
				}
				varMaps.put(vv.getID(), v.getID());
				oMap.put(r, v);
			}
			ArrayList<String> varids = new ArrayList<String>(varMaps.keySet());
			ArrayList<KBTriple> triples = ct.getConstraintEngine().getConstraints(varids);
			ArrayList<KBTriple> req = new ArrayList<KBTriple>();
			for (KBTriple triple : triples) {
				KBObject obj = triple.getObject();
				KBObject subj = triple.getSubject();
				if (subj != null && varMaps.containsKey(subj.getID())) {
					subj = ontfac.getObject(varMaps.get(subj.getID()));
				}
				if (obj != null && varMaps.containsKey(obj.getID())) {
					obj = ontfac.getObject(varMaps.get(obj.getID()));
				}
				triple.setSubject(subj);
				triple.setObject(obj);
				req.add(triple);
			}
			ComponentMapsAndRequirements cmap = new ComponentMapsAndRequirements(cv, iMap, oMap, req);
			list.add(cmap);
			i++;
		}

		return list;
	}

}
