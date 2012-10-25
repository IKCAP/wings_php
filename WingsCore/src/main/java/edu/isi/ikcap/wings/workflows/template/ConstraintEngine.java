package edu.isi.ikcap.wings.workflows.template;

import edu.isi.ikcap.ontapi.KBObject;
import edu.isi.ikcap.ontapi.KBTriple;

import java.util.ArrayList;
import java.util.Map;

public interface ConstraintEngine {
	public void setConstraints(ArrayList<KBTriple> constraints);

	public void addConstraints(ArrayList<KBTriple> constraints);

	public void removeConstraint(KBTriple constraint);

	public void removeObjectAndConstraints(KBObject obj);

	public KBTriple createNewConstraint(String subjID, String predID, String objID);

	public KBTriple createNewDataConstraint(String subjID, String predID, String obj, String type);

	public KBObject getResource(String ID);

	public boolean containsConstraint(KBTriple constraint);

	public ArrayList<KBTriple> getConstraints();

	public ArrayList<KBTriple> getConstraints(String id);

	public ArrayList<KBTriple> getConstraints(ArrayList<String> ids);

	public void addBlacklistedId(String id);

	public void removeBlacklistedId(String id);

	public void addBlacklistedNamespace(String ns);

	public void removeBlacklistedNamespace(String ns);

	public void addWhitelistedNamespace(String ns);

	public void removeWhitelistedNamespace(String ns);

	public Map getPrefixNSMap();
	
	public void replaceSubjectInConstraints(KBObject subj, KBObject newSubj);

	public void replaceObjectInConstraints(KBObject obj, KBObject newObj);

}
