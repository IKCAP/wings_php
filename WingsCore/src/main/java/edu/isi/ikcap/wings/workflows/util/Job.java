package edu.isi.ikcap.wings.workflows.util;

import java.util.ArrayList;
import java.util.HashSet;

import edu.isi.ikcap.wings.workflows.template.sets.Binding;

public class Job {
	public HashSet<Binding> inputFiles;
	public HashSet<Binding> outputFiles;

	ArrayList<String> profileList;

	String name;
	String id;

	String namespace;
	String version;

	ArrayList<String> parentIds;

	String argumentStr;

	public Job(String id, String name, String version, String namespace, String arguments, ArrayList<String> profileList) {
		this.id = id;
		this.name = name;
		this.version = version;
		this.namespace = namespace;
		this.profileList = profileList;
		inputFiles = new HashSet<Binding>();
		outputFiles = new HashSet<Binding>();
		parentIds = new ArrayList<String>();
		argumentStr = arguments;
	}

	/**
	 * Getter for property 'inputFiles'.
	 * 
	 * @return Value for property 'inputFiles'.
	 */
	public HashSet<Binding> getInputFiles() {
		return inputFiles;
	}

	/**
	 * Setter for property 'inputFiles'.
	 * 
	 * @param inputFiles
	 *            Value to set for property 'inputFiles'.
	 */
	public void setInputFiles(HashSet<Binding> inputFiles) {
		this.inputFiles = inputFiles;
	}

	/**
	 * Getter for property 'outputFiles'.
	 * 
	 * @return Value for property 'outputFiles'.
	 */
	public HashSet<Binding> getOutputFiles() {
		return outputFiles;
	}

	/**
	 * Setter for property 'outputFiles'.
	 * 
	 * @param outputFiles
	 *            Value to set for property 'outputFiles'.
	 */
	public void setOutputFiles(HashSet<Binding> outputFiles) {
		this.outputFiles = outputFiles;
	}

	/**
	 * Getter for property 'profileList'.
	 * 
	 * @return Value for property 'profileList'.
	 */
	public ArrayList<String> getProfileList() {
		return profileList;
	}

	/**
	 * Setter for property 'profileList'.
	 * 
	 * @param profileList
	 *            Value to set for property 'profileList'.
	 */
	public void setProfileList(ArrayList<String> profileList) {
		this.profileList = profileList;
	}

	/**
	 * Getter for property 'name'.
	 * 
	 * @return Value for property 'name'.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Setter for property 'name'.
	 * 
	 * @param name
	 *            Value to set for property 'name'.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Getter for property 'id'.
	 * 
	 * @return Value for property 'id'.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Setter for property 'id'.
	 * 
	 * @param id
	 *            Value to set for property 'id'.
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Getter for property 'namespace'.
	 * 
	 * @return Value for property 'namespace'.
	 */
	public String getNamespace() {
		return namespace;
	}

	/**
	 * Setter for property 'namespace'.
	 * 
	 * @param namespace
	 *            Value to set for property 'namespace'.
	 */
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	/**
	 * Getter for property 'version'.
	 * 
	 * @return Value for property 'version'.
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Setter for property 'version'.
	 * 
	 * @param version
	 *            Value to set for property 'version'.
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * Getter for property 'parentIds'.
	 * 
	 * @return Value for property 'parentIds'.
	 */
	public ArrayList<String> getParentIds() {
		return parentIds;
	}

	/**
	 * Setter for property 'parentIds'.
	 * 
	 * @param parentIds
	 *            Value to set for property 'parentIds'.
	 */
	public void setParentIds(ArrayList<String> parentIds) {
		this.parentIds = parentIds;
	}

	/**
	 * Getter for property 'argumentStr'.
	 * 
	 * @return Value for property 'argumentStr'.
	 */
	public String getArgumentStr() {
		return argumentStr;
	}

	/**
	 * Setter for property 'argumentStr'.
	 * 
	 * @param argumentStr
	 *            Value to set for property 'argumentStr'.
	 */
	public void setArgumentStr(String argumentStr) {
		this.argumentStr = argumentStr;
	}
}