package edu.isi.ikcap.wings.workflows.util.wfinvocation;

public class Variable {
	String id;
	String dataBinding;
	
	public Variable(String id) {
		this.id = id;
	}
	
	public Variable(String id, String binding) {
		this.id = id;
		this.dataBinding = binding;
	}
	
	public void addDataBinding(String binding) {
		this.dataBinding = binding;
	}
	
	public String getDataBinding() {
		return this.dataBinding;
	}
	
	public String getID() {
		return this.id;
	}
}
