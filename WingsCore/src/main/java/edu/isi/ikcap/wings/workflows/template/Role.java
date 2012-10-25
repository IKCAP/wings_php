package edu.isi.ikcap.wings.workflows.template;


public class Role extends Entity {
	private static final long serialVersionUID = 1L;
	public static int PARAMETER = 1;
	public static int DATA = 2;

	private int dimensionality = 0;
	int type = DATA;

	public Role(String id) {
		super(id);
	}

	public String toString() {
		return getID();
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getType() {
		return this.type;
	}

	public void setDimensionality(int dim) {
		this.dimensionality = dim;
	}

	public int getDimensionality() {
		return this.dimensionality;
	}
}
