////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2007, Your Corporation. All Rights Reserved.
////////////////////////////////////////////////////////////////////////////////

package edu.isi.ikcap.wings.workflows.template.impl;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.isi.ikcap.ontapi.KBAPI;
import edu.isi.ikcap.ontapi.KBObject;
import edu.isi.ikcap.ontapi.KBTriple;
import edu.isi.ikcap.ontapi.OntSpec;

import edu.isi.ikcap.wings.workflows.template.ConstraintEngine;
import edu.isi.ikcap.wings.workflows.template.Link;
import edu.isi.ikcap.wings.workflows.template.Metadata;
import edu.isi.ikcap.wings.workflows.template.Node;
import edu.isi.ikcap.wings.workflows.template.RuleSet;
import edu.isi.ikcap.wings.workflows.template.Seed;
import edu.isi.ikcap.wings.workflows.template.variables.ComponentVariable;
import edu.isi.ikcap.wings.workflows.template.variables.Variable;
import edu.isi.ikcap.wings.workflows.util.PropertiesHelper;



/**
 * Name: RequestOWL
 * <p/>
 * Package: edu.isi.ikcap.workflows
 * <p/>
 * User: moody
 * <p/>
 * Date: Aug 24, 2007
 * <p/>
 * Time: 2:32:12 AM
 */
public class SeedOWL extends TemplateOWL implements Seed {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * request id
	 */
	public String seedId;

	transient protected KBAPI r_api;
	transient protected KBAPI t_api;

	transient protected ConstraintEngine r_constraintEngine;
	transient protected ConstraintEngine t_constraintEngine;

	String seedFile;
	String templateFile;
	String templateURL;

	protected Metadata seedMeta;
	protected RuleSet seedRules;

	// Loading an existing seed
	public SeedOWL(String domain, String seedFile) {
		super.initializeAPI(domain, seedFile);

		this.seedFile = seedFile;
		this.templateFile = findTemplateFile();
		this.templateURL = PropertiesHelper.getOntologyURL() + "/" + domain + "/" + templateFile;

		if (modifiedTemplates.contains(templateURL)) {
			api.refreshCacheForURL(templateURL);
			modifiedTemplates.remove(templateURL);
			super.initializeAPI(domain, seedFile);
		}
		// Read in the template
		super.readTemplate();

		// Initialize the local seed apis
		initializeAPIs();
		initializeEngines();

		String templateName = templateFile.substring(0, templateFile.lastIndexOf(".owl"));
		super.setName(templateName, false);

		this.seedMeta = readMetadata(r_api, this.url);
		this.meta = readMetadata(api, templateURL);

		this.seedRules = readRules(r_api, this.url);
		this.rules = readRules(api, templateURL);
	}

	// Creation of a blank seed
	public SeedOWL(String domain, String seedFile, String templateFile) {
		super(domain, seedFile, true);
		r_api = ontFactory.getAPI(OntSpec.PLAIN);
		r_api.setNamespacePrefixMap(api.getNamespacePrefixMap());
		t_api = ontFactory.getAPI(OntSpec.PLAIN);
		t_api.setNamespacePrefixMap(api.getNamespacePrefixMap());
		initializeEngines();
		this.setTemplateFile(templateFile);
		this.seedFile = seedFile;

		this.seedMeta = new Metadata();
		this.seedRules = new RuleSet();
	}

	private void initializeAPIs() {
		r_api = ontFactory.getAPI(this.url, OntSpec.PLAIN);
		r_api.useRawModel();
		t_api = ontFactory.getAPI(this.url, OntSpec.PLAIN);
		t_api.useBaseModel();
	}

	private void initializeEngines() {
		this.r_constraintEngine = new ConstraintEngineOWL(r_api, this.wns);
		this.r_constraintEngine.addBlacklistedNamespace(uriPrefix + "/" + domain + "/seeds/");
		this.r_constraintEngine.addBlacklistedNamespace(uriPrefix + "/" + domain + "/");
		this.t_constraintEngine = new ConstraintEngineOWL(t_api, this.wns);
	}

	/** {@inheritDoc} */
	public String getSeedId() {
		return seedId;
	}

	/** {@inheritDoc} */
	public void setSeedId(String seedId) {
		this.seedId = seedId;
	}

	public ConstraintEngine getSeedConstraintEngine() {
		return this.r_constraintEngine;
	}

	public ConstraintEngine getTemplateConstraintEngine() {
		return this.t_constraintEngine;
	}

	public String getName() {
		return seedFile.substring(seedFile.lastIndexOf('/') + 1, seedFile.lastIndexOf(".owl"));
	}

	public void setTemplateFile(String templateFile) {
		String importURL = PropertiesHelper.getOntologyURL() + "/" + this.domain + "/" + templateFile;
		r_api.refreshCacheForURL(importURL);

		for (String importurl : r_api.getImports(url)) {
			r_api.removeImport(url, importurl);
		}
		r_api.addImport(url, importURL);

		changeConstraintNamespace(importURL + "#");
		this.templateFile = templateFile;
	}

	public String getTemplateFile() {
		return this.templateFile;
	}

	private String findTemplateFile() {
		Pattern pat = Pattern.compile(uriPrefix + "/" + domain + "/(.+\\.owl)");
		for (String importurl : api.getImports(url)) {
			Matcher m = pat.matcher(importurl);
			if (m.find()) {
				return m.group(1);
			}
		}
		return null;
	}

	private void changeConstraintNamespace(String newNS) {
		String oldNS = this.ns;
		r_api.setPrefixNamespace("", newNS);

		ArrayList<KBTriple> constraints = new ArrayList<KBTriple>();
		ArrayList<String> varids = new ArrayList<String>();
		for (Variable v : getVariables())
			varids.add(v.getID());
		for (KBTriple t : this.r_constraintEngine.getConstraints(varids)) {
			KBObject obj = t.getObject();
			KBObject subj = t.getSubject();
			if (oldNS.equals(subj.getNamespace())) {
				subj = r_api.getResource(newNS + subj.getName());
			}
			if (oldNS.equals(obj.getNamespace())) {
				obj = r_api.getResource(newNS + obj.getName());
			}
			r_constraintEngine.removeConstraint(t);
			t.setObject(obj);
			t.setSubject(subj);
			constraints.add(t);
		}
		for (Variable v : getVariables())
			v.setID(v.getID().replace(this.ns, newNS));
		for (Link l : getLinks())
			l.setID(l.getID().replace(this.ns, newNS));
		for (Node n : getNodes()) {
			n.setID(n.getID().replace(this.ns, newNS));
			ComponentVariable c = n.getComponentVariable();
			if (c.getNamespace().equals(this.ns))
				c.setID(newNS + c.getName());
		}

		r_constraintEngine.setConstraints(constraints);
		this.ns = newNS;
	}

	public String getInternalRepresentation() {
		// return r_api.toN3(this.url);
		return r_api.toAbbrevRdf(false, this.url);
	}

	public String deriveInternalRepresentation() {
		// Create a plain new KBAPI
		KBAPI tapi = ontFactory.getAPI(OntSpec.PLAIN);

		// Add template import
		String turl = uriPrefix + "/" + domain + "/" + templateFile;
		tapi.addImport("", turl);

		// Copy over the namespace mappings (change default namespace first)
		Map<String, String> nsmap = api.getPrefixNamespaceMap();
		nsmap.put("", turl + "#");
		tapi.setPrefixNamespaceMap(nsmap);

		ArrayList<String> varids = new ArrayList<String>();
		for (Variable v : getVariables())
			varids.add(v.getID());
		for (KBTriple t : this.r_constraintEngine.getConstraints(varids)) {
			tapi.addTriple(t);
		}
		writeMetadataDescription(tapi, seedMeta);
		writeRules(tapi, seedRules);

		// Return RDF representation
		// return tapi.toN3(this.url);
		return tapi.toAbbrevRdf(false, this.url);
	}

	public String deriveTemplateRepresentation() {
		KBAPI tapi = api;
		api = t_api;
		String rdf = super.deriveInternalRepresentation();
		api = tapi;
		return rdf;
	}

	public Metadata getSeedMetadata() {
		return this.seedMeta;
	}

	public RuleSet getSeedRules() {
		return this.seedRules;
	}

	public void setName(String name, boolean internalChange) {
		this.name = name;
		if (internalChange) {
			String origURL = this.url;

			this.file = name + ".owl";
			this.url = uriPrefix + "/" + domain + "/seeds/" + name + ".owl";
			this.ns = this.url + "#";
			for (String importurl : r_api.getImports(origURL)) {
				r_api.removeImport(origURL, importurl);
				r_api.addImport(this.url, importurl);
			}
		}
	}

	public void clearImportCache() {
		for (String importurl : api.getImports(url)) {
			api.refreshCacheForURL(importurl);
		}
	}

	public void close() {
		r_api.close();
		t_api.close();
		api.close();
	}
}
