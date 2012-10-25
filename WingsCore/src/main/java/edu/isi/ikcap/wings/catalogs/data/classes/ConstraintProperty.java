package edu.isi.ikcap.wings.catalogs.data.classes;

import java.util.ArrayList;

public class ConstraintProperty {
	int type;
	String id;
	ArrayList<String> domainClassIds;
	ArrayList<String> rangeObjectIds;

	public static int DATACONSTRAINT = 1;
	public static int OBJECTCONSTRAINT = 2;

	public ConstraintProperty(String id, int type) {
		this.id = id;
		this.type = type;
		domainClassIds = new ArrayList<String>();
		rangeObjectIds = new ArrayList<String>();
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getType() {
		return this.type;
	}

	public boolean isDataConstraint() {
		if (this.type == DATACONSTRAINT)
			return true;
		return false;
	}

	public boolean isObjectConstraint() {
		if (this.type == OBJECTCONSTRAINT)
			return true;
		return false;
	}

	public ArrayList<String> getDomainClassIds() {
		return this.domainClassIds;
	}

	public ArrayList<String> getRangeObjectIds() {
		return this.rangeObjectIds;
	}

	public void addDomainClassId(String id) {
		this.domainClassIds.add(id);
	}

	public void addRangeObjectId(String id) {
		this.rangeObjectIds.add(id);
	}

	public String toString() {
		String str = "";
		str += "\n" + getName() + "(" + type + ")\nDomain:" + domainClassIds + "\nRange:" + rangeObjectIds + "\n";
		return str;
	}

	public String getName(String id) {
		return id.substring(id.lastIndexOf('#') + 1);
	}

	public String getName() {
		return getName(this.id);
	}

	public String getId() {
		return this.id;
	}
}
