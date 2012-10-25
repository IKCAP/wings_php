////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2007, Your Corporation. All Rights Reserved.
////////////////////////////////////////////////////////////////////////////////

package edu.isi.ikcap.wings.catalogs.components.classes;

import edu.isi.ikcap.ontapi.KBTriple;
import edu.isi.ikcap.wings.workflows.template.Role;
import edu.isi.ikcap.wings.workflows.template.variables.*;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Name: ComponentMapsAndRequirements
 * <p/>
 * Package: edu.isi.ikcap.wings.catalogs.components
 * <p/>
 * User: moody
 * <p/>
 * Date: Aug 24, 2007
 * <p/>
 * Time: 6:36:59 PM
 * 
 * Stores - Component - inputMaps : Component Input Parameter <=> Template
 * Variable Mappings - outputMaps : Component Output Parameter <=> Template
 * Variable Mappings - requirements : Also known as DOD's (Data Object
 * Descriptions). These are basically a list of KBTriple Objects
 */
public class ComponentMapsAndRequirements {

	/**
	 * a component
	 */
	public ComponentVariable component;

	/**
	 * the input maps
	 */
	public HashMap<Role, Variable> inputMaps;

	/**
	 * the output maps
	 */
	public HashMap<Role, Variable> outputMaps;

	/**
	 * the reverse input maps
	 */
	public HashMap<Variable, Role> reverseInputMaps;

	/**
	 * the reverse output maps
	 */
	public HashMap<Variable, Role> reverseOutputMaps;

	/**
	 * the requirements
	 */
	public ArrayList<KBTriple> requirements;

	/**
	 * the reasoning explaining the contents of this CMR (usually returned from the Catalog)
	 */
	public ArrayList<String> explanations;
	
	/**
	 * the invalid flag that marks this CMR as not to be used (only used for provenance purposes) 
	 */
	public boolean isInvalid;
	
	/**
	 * the output of query 2.1 findInputDataRequirements
	 * 
	 * @param component
	 *            a component
	 * @param inputMaps
	 *            maps from Input Role to Template Variable
	 * @param outputMaps
	 *            maps from Output Role to Template Variable
	 * @param requirements
	 *            a list of KBTriples
	 */
	public ComponentMapsAndRequirements(ComponentVariable component, HashMap<Role, Variable> inputMaps, HashMap<Role, Variable> outputMaps,
			ArrayList<KBTriple> requirements) {
		this.component = component;
		this.inputMaps = inputMaps;
		this.outputMaps = outputMaps;
		this.reverseInputMaps = createReverseMap(inputMaps);
		this.reverseOutputMaps = createReverseMap(outputMaps);
		this.requirements = requirements;
		this.explanations = new ArrayList<String>();
		this.isInvalid = false;
	}

	public HashMap<Variable, Role> createReverseMap(HashMap<Role, Variable> map) {
		HashMap<Variable, Role> rmap = new HashMap<Variable, Role>();
		for (Role cp : map.keySet()) {
			rmap.put(map.get(cp), cp);
		}
		return rmap;
	}

	public HashMap<Role, Variable> createMapFromReverseMap(HashMap<Variable, Role> rmap) {
		HashMap<Role, Variable> map = new HashMap<Role, Variable>();
		for (Variable cp : rmap.keySet()) {
			map.put(rmap.get(cp), cp);
		}
		return map;
	}

	/**
	 * Getter for property 'component'.
	 * 
	 * @return Value for property 'component'.
	 */
	public ComponentVariable getComponent() {
		return component;
	}

	/**
	 * Setter for property 'component'.
	 * 
	 * @param component
	 *            Value to set for property 'component'.
	 */
	public void setComponent(ComponentVariable component) {
		this.component = component;
	}

	/**
	 * Getter for property 'inputMaps'.
	 * 
	 * @return Value for property 'inputMaps'.
	 */
	public HashMap<Role, Variable> getInputMaps() {
		return inputMaps;
	}

	/**
	 * Setter for property 'inputMaps'.
	 * 
	 * @param inputMaps
	 *            Value to set for property 'inputMaps'.
	 */
	public void setInputMaps(HashMap<Role, Variable> inputMaps) {
		this.inputMaps = inputMaps;
		this.reverseInputMaps = createReverseMap(inputMaps);
	}

	/**
	 * Getter for property 'outputMaps'.
	 * 
	 * @return Value for property 'outputMaps'.
	 */
	public HashMap<Role, Variable> getOutputMaps() {
		return outputMaps;
	}

	/**
	 * Setter for property 'outputMaps'.
	 * 
	 * @param outputMaps
	 *            Value to set for property 'outputMaps'.
	 */
	public void setOutputMaps(HashMap<Role, Variable> outputMaps) {
		this.outputMaps = outputMaps;
		this.reverseOutputMaps = createReverseMap(outputMaps);
	}

	/**
	 * Getter for property 'requirements'.
	 * 
	 * @return Value for property 'requirements'.
	 */
	public ArrayList<KBTriple> getRequirements() {
		return requirements;
	}

	/**
	 * Setter for property 'requirements'.
	 * 
	 * @param requirements
	 *            Value to set for property 'requirements'.
	 */
	public void setRequirements(ArrayList<KBTriple> requirements) {
		this.requirements = requirements;
	}

	public String toString() {
		return "ComponentMapsAndRequirements{" + "component=" + component + ", inputMaps=" + inputMaps + ", outputMaps=" + outputMaps + ", requirements="
				+ requirements + '}';
	}

	/**
	 * Get Variable <=> Input Role Mappings
	 * 
	 * @return A HashMap of Variable <=> Role objects
	 */
	public HashMap<Variable, Role> getReverseInputMaps() {
		return reverseInputMaps;
	}

	/**
	 * Get Variable <=> Output Role Mappings
	 * 
	 * @return A HashMap of Variable <=> Role objects
	 */
	public HashMap<Variable, Role> getReverseOutputMaps() {
		return reverseOutputMaps;
	}

	/**
	 * Get Input Role ID <=> Variable Mappings
	 * 
	 * @return The Mapping indexed by Role IDs rather than Role Objects
	 */
	public HashMap<String, Variable> getStringIndexedInputMaps() {
		HashMap<String, Variable> map = new HashMap<String, Variable>();
		for (Role cp : inputMaps.keySet()) {
			map.put(cp.getID(), inputMaps.get(cp));
		}
		return map;
	}

	/**
	 * Get Output Role ID <=> Variable Mappings
	 * 
	 * @return The Mapping indexed by Role IDs rather than Role Objects
	 */
	public HashMap<String, Variable> getStringIndexedOutputMaps() {
		HashMap<String, Variable> map = new HashMap<String, Variable>();
		for (Role cp : outputMaps.keySet()) {
			map.put(cp.getID(), outputMaps.get(cp));
		}
		return map;
	}

	/**
	 * Get VarID <=> Input Role Mappings
	 * 
	 * @return The Reverse Mapping indexed by Variable IDs rather than Variable
	 *         Objects
	 */
	public HashMap<String, Role> getStringIndexedReverseInputMaps() {
		HashMap<String, Role> map = new HashMap<String, Role>();
		for (Variable var : reverseInputMaps.keySet()) {
			map.put(var.getID(), reverseInputMaps.get(var));
		}
		return map;
	}

	/**
	 * Get VarID <=> Output Role Mappings
	 * 
	 * @return The Reverse Mapping indexed by Variable IDs rather than Variable
	 *         Objects
	 */
	public HashMap<String, Role> getStringIndexedReverseOutputMaps() {
		HashMap<String, Role> map = new HashMap<String, Role>();
		for (Variable var : reverseOutputMaps.keySet()) {
			map.put(var.getID(), reverseOutputMaps.get(var));
		}
		return map;
	}
	
	/**
	 * Add Explanations
	 * 
	 * @param explanations
	 * 			The reasoning explaining the contents of this CMR (usually returned from the Catalog)
	 */
	public void addExplanations(ArrayList<String> explanations) {
		this.explanations.addAll(explanations);
	}
	
	public void addExplanations(String explanation) {
		this.explanations.add(explanation);
	}

	/**
	 * Get Explanations
	 * 
	 * @return The reasoning explaining the contents of this CMR 
	 * 			 (usually returned from the Catalog)
	 */
	public ArrayList<String> getExplanations() {
		return this.explanations;
	}
	
	/**
	 * Set Invalid Flag
	 * 
	 * @param isInvalid
	 * 			Mark this CMR as invalid (i.e. not to be used)
	 */
	public void setInvalidFlag(boolean isInvalid) {
		this.isInvalid = isInvalid;
	}

	/**
	 * Get Invalid Flag
	 * 
	 * @return The reasoning explaining the contents of this CMR 
	 * 			 (usually returned from the Catalog)
	 */
	public boolean getInvalidFlag() {
		return this.isInvalid;
	}
}
