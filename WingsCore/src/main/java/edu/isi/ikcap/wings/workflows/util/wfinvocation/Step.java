package edu.isi.ikcap.wings.workflows.util.wfinvocation;

import java.util.ArrayList;

public class Step {
	String id;
	ArrayList<Variable> inputVariables;
	ArrayList<Variable> outputVariables;
	String invocationLine;
	String customData;
	String codeBinding;
	
	public Step(String id) {
		this.id = id;
		inputVariables = new ArrayList<Variable>();
		outputVariables = new ArrayList<Variable>();
	}
	
	public void addInputVariable(Variable v) {
		inputVariables.add(v);
	}
	
	public void addOutputVariable(Variable v) {
		outputVariables.add(v);
	}
	
	public void addInvocationLine(String s) {
		this.invocationLine = s;
	}
	
	public void addCustomData(String data) {
		this.customData = data;
	}
	
	public void setCodeBinding(String id) {
		this.codeBinding = id;
	}
	
	public String getID() {
		return this.id;
	}
	
	public ArrayList<Variable> getInputVariables() {
		return this.inputVariables;
	}
	
	public ArrayList<Variable> getOutputVariables() {
		return this.outputVariables;
	}
	
	public String getCustomData() {
		return this.customData;
	}
	
	public String getCodeBinding() {
		return this.codeBinding;
	}
	
	public String getInvocationLine() {
		return this.invocationLine;
	}
}
