package edu.isi.ikcap.wings.workflows.template.impl;

import edu.isi.ikcap.ontapi.KBAPI;
import edu.isi.ikcap.ontapi.KBObject;
import edu.isi.ikcap.ontapi.KBTriple;
import edu.isi.ikcap.ontapi.OntFactory;
import edu.isi.ikcap.ontapi.OntSpec;
import edu.isi.ikcap.wings.workflows.template.ConstraintEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ConstraintEngineOWL implements ConstraintEngine {
	KBAPI api;
	Map nsmap; 
	
	ArrayList<String> blacklistns;
	ArrayList<String> whitelistns; // If set, then blacklistns is ignored

	ArrayList<String> blacklistIds;
	ArrayList<String> allowedIds;

	transient protected static OntFactory ontFactory = new OntFactory(OntFactory.JENA);
	
	public ConstraintEngineOWL(KBAPI api, String wns) {
		this.api = ontFactory.getAPI(OntSpec.PLAIN);
		
		nsmap = api.getPrefixNamespaceMap();
		
		initAllowedIds(wns);
		initBannedNamespaces(wns);
		initBannedIds();
		initAPI(api);
	}

	public ConstraintEngineOWL(ConstraintEngineOWL engine) {
		this.api = ontFactory.getAPI(OntSpec.PLAIN);
		
		nsmap = new HashMap<String, String>(engine.nsmap);
		
		if(engine.blacklistns != null) blacklistns = new ArrayList<String>(engine.blacklistns);
		if(engine.whitelistns != null) whitelistns = new ArrayList<String>(engine.whitelistns);
		if(engine.blacklistIds != null) blacklistIds = new ArrayList<String>(engine.blacklistIds);
		if(engine.allowedIds != null) allowedIds = new ArrayList<String>(engine.allowedIds);
		this.api.addTriples(engine.getConstraints());
	}
	
	private void initAPI(KBAPI api) {
		ArrayList<KBTriple> triples = api.getAllTriples();
		ArrayList<KBTriple> newTriples = new ArrayList<KBTriple>();
		for(KBTriple t: triples) {
			if (isBanned(t.getSubject()) || isBanned(t.getPredicate()) || isBanned(t.getObject())) {
				continue;
			}
			newTriples.add(t);
		}
		this.api.addTriples(newTriples);
	}
	
	private void initBannedNamespaces(String wns) {
		blacklistns = new ArrayList<String>();
		blacklistns.add(wns);
		blacklistns.add((String) nsmap.get("rdf"));
		blacklistns.add((String) nsmap.get("owl"));
		blacklistns.add((String) nsmap.get("rdfs"));
		blacklistns.add((String) nsmap.get("xsd"));
		//blacklistns.add((String) nsmap.get(""));
		
		whitelistns = new ArrayList<String>();
	}

	private void initAllowedIds(String wns) {
		allowedIds = new ArrayList<String>();
		allowedIds.add(nsmap.get("rdf") + "type");
		allowedIds.add(wns + "hasDataBinding");
		allowedIds.add(wns + "hasParameterValue");
		allowedIds.add(wns + "hasSameDataAs");
		allowedIds.add(wns + "hasDifferentDataFrom");
	}
	
	private void initBannedIds() {
		blacklistIds = new ArrayList<String>();
		/*blacklistIds.add(nsmap.get("rdfs") + "comment");
		blacklistIds.add(nsmap.get("rdfs") + "range");
		blacklistIds.add(nsmap.get("rdfs") + "domain");
		blacklistIds.add(nsmap.get("owl") + "topDataProperty");
		blacklistIds.add(nsmap.get("owl") + "bottomDataProperty");*/
	}
	
	
	private boolean isBanned(KBObject item) {
		if (item.isLiteral()) return false;
		if (blacklistIds.contains(item.getID())) return true;
		if (allowedIds.contains(item.getID())) return false;
		
		if ((whitelistns != null) && (whitelistns.contains(item.getNamespace()))) return false;
		if ((blacklistns != null) && (blacklistns.contains(item.getNamespace()))) return true;

		return false;
	}
	
	private boolean isRelevant(KBObject item) {
		ArrayList<KBObject> clses = api.getAllClassesOfInstance(item, true);
		if (clses == null) {
			//System.err.println(item + " does not have any class !!");
			return false;
		}
		for (KBObject cls : clses) {
			if (cls.getID() != null && whitelistns != null && whitelistns.contains(cls.getNamespace())) {
				return true;
			} else if (cls.getID() != null && !blacklistns.contains(cls.getNamespace())) {
				return true;
			}
		}
		return false;
	}


	public Map getPrefixNSMap() {
		return this.nsmap;
	}

	private ArrayList<KBTriple> removeUselessConstraints(ArrayList<KBTriple> constraints) {
		// What are useless constraints ?
		// - Duplicates
		// - Constraints which have the same subject and object
		// (like subClassOf, equivalentClass entailments)
		ArrayList<String> strconstraints = new ArrayList<String>();
		ArrayList<KBTriple> newconstraints = new ArrayList<KBTriple>();
		for (KBTriple triple : constraints) {
			ArrayList<KBObject> kbos = triple.toArrayList();
			String str = kbos.toString();
			if (!strconstraints.contains(str)) {
				strconstraints.add(str);
				KBObject subj = triple.getSubject();
				KBObject obj = triple.getObject();
				if (obj.isLiteral() || !obj.getID().equals(subj.getID())) {
					newconstraints.add(triple);
				}
			}
		}
		return newconstraints;
	}

	// filterType = 0 : Filter both object and subject for relevance
	// filterType = 1 : Filter only subject for relevance
	// filterType = 2 : Filter only object for relevance
	private ArrayList<KBTriple> getTriplesFor(KBObject forsubj, KBObject forobj, int filterType) {

		ArrayList<KBTriple> triples = this.api.genericTripleQuery(forsubj, null, forobj);
		ArrayList<KBTriple> relevantTriples = new ArrayList<KBTriple>();
		for (KBTriple triple : triples) {
			// System.out.println(triple);
			KBObject subj = triple.getSubject();
			KBObject pred = triple.getPredicate();
			KBObject obj = triple.getObject();
			if (subj != null && pred != null && obj != null && subj.getID() != null && (obj.getID() != null || obj.isLiteral()) && pred.getID() != null) {
				if (isBanned(subj) || isBanned(pred) || isBanned(obj)) {
					continue;
				}

				relevantTriples.add(triple);
			}
		}
		return removeUselessConstraints(relevantTriples);
	}

	private ArrayList<KBTriple> getConstraintsForId(String id, ArrayList<String> done) {
		KBObject item = this.api.getResource(id);
		if (done.contains(id) || blacklistIds.contains(id)) {
			return new ArrayList<KBTriple>();
		}
		done.add(id);
		if (item == null) {
			return new ArrayList<KBTriple>();
		}

		ArrayList<KBTriple> constraints = new ArrayList<KBTriple>();

		// Get Triples with item as subject and object

		// TODO: Temporarily just choose subject-based constraints
		// to simplify which constraints are chosen !!

		ArrayList<KBTriple> tmp = this.getTriplesFor(item, null, 2);
		// ArrayList<KBTriple> tmp2 = this.getTriplesFor(null, item, 1,
		// blacklistIds);
		constraints.addAll(tmp);
		// constraints.addAll(tmp2);

		// Have to recursively get constraints for all the objects in tmp
		for (KBTriple triple : tmp) {
			KBObject obj = triple.getObject();
			if (obj != null && obj.getID() != null && isRelevant(obj)) {
				constraints.addAll(getConstraintsForId(obj.getID(), done));
			}
		}

		// Have to recursively get constraints for all the subjects in tmp2
		/*
		 * for (KBTriple triple : tmp2) { KBObject subj = triple.getSubject();
		 * if (subj != null && subj.getID() != null) {
		 * constraints.addAll(getConstraintsForId(subj.getID(), done)); } }
		 */

		return this.removeUselessConstraints(constraints);
	}

	public ArrayList<KBTriple> getConstraints() {
		return this.getTriplesFor(null, null, 0);
	}

	public ArrayList<KBTriple> getConstraints(String id) {
		return getConstraintsForId(id, new ArrayList<String>());
	}

	public ArrayList<KBTriple> getConstraints(ArrayList<String> ids) {
		ArrayList<KBTriple> constraints = new ArrayList<KBTriple>();
		for (String id : ids) {
			constraints.addAll(this.getConstraints(id));
		}
		return this.removeUselessConstraints(constraints);
	}

	public void setConstraints(ArrayList<KBTriple> constraints) {
		// Modify the internal api to add statements
		this.api.addTriples(constraints);
	}

	public void addConstraints(ArrayList<KBTriple> constraints) {
		// Modify the internal api to add constraints
		// this.constraints.addAll(constraints);
		this.api.addTriples(constraints);
	}

	public void removeConstraint(KBTriple constraint) {
		this.api.removeTriple(constraint);
	}

	public void removeObjectAndConstraints(KBObject obj) {
		this.api.deleteObject(obj);
	}

	public void addBlacklistedNamespace(String ns) {
		blacklistns.add(ns);
	}

	public void addBlacklistedId(String id) {
		blacklistIds.add(id);
	}

	public void removeBlacklistedId(String id) {
		blacklistIds.remove(id);
	}

	public void addWhitelistedNamespace(String ns) {
		if (whitelistns == null) {
			whitelistns = new ArrayList<String>();
		}
		whitelistns.add(ns);
	}

	public boolean containsConstraint(KBTriple cons) {
		if (this.api.genericTripleQuery(cons.getSubject(), cons.getPredicate(), cons.getObject()) != null)
			return true;
		return false;
	}
	
	public void replaceSubjectInConstraints(KBObject subj, KBObject newSubj) {
		for(KBTriple t : this.api.genericTripleQuery(subj, null, null)) {
			this.api.removeTriple(t);
			t.setSubject(newSubj);
			this.api.addTriple(t);
		}
	}

	public void replaceObjectInConstraints(KBObject obj, KBObject newObj) {
		for(KBTriple t : this.api.genericTripleQuery(null, null, obj)) {
			this.api.removeTriple(t);
			t.setObject(newObj);
			this.api.addTriple(t);
		}
	}
	
	public KBTriple createNewConstraint(String subjID, String predID, String objID) {
		KBObject subjkb = api.getResource(subjID);
		KBObject predkb = api.getProperty(predID);
		KBObject objkb = api.getResource(objID);
		if (subjkb != null && predkb != null && objkb != null) {
			return this.api.addTriple(subjkb, predkb, objkb);
		}
		return null;
	}

	public KBTriple createNewDataConstraint(String subjID, String predID, String obj, String type) {
		KBObject subjkb = api.getResource(subjID);
		KBObject predkb = api.getProperty(predID);
		if (subjkb != null && predkb != null) {
			try {
				KBObject objkb = api.createXSDLiteral(obj, type);
				if (objkb != null) {
					return this.api.addTriple(subjkb, predkb, objkb);
				}
			} catch (Exception e) {
				System.err.println(obj + " is not of type " + type);
			}
		}
		return null;
	}

	public KBObject getResource(String ID) {
		return api.getResource(ID);
	}

	public String toString() {
		// return this.api.toN3();
		return this.api.toRdf(false);
	}

	public void removeBlacklistedNamespace(String ns) {
		blacklistns.remove(ns);
	}

	public void removeWhitelistedNamespace(String ns) {
		whitelistns.remove(ns);
	}
}
