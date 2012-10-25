package edu.isi.ikcap.wings.workflows.util;

import edu.isi.ikcap.ontapi.OntFactory;
import edu.isi.ikcap.ontapi.OntSpec;
import edu.isi.ikcap.wings.catalogs.components.ComponentCatalog;
import edu.isi.ikcap.wings.catalogs.components.impl.isi.ComponentCatalogOWLwithRules;
import edu.isi.ikcap.wings.catalogs.data.DataCatalog;
import edu.isi.ikcap.wings.catalogs.data.impl.isi.FileBackedWekaDC;
import edu.isi.ikcap.wings.workflows.template.Seed;
import edu.isi.ikcap.wings.workflows.template.Template;
import edu.isi.ikcap.wings.workflows.template.impl.SeedOWL;
import edu.isi.ikcap.wings.workflows.template.impl.TemplateOWL;

public class WflowGenFactory {
	public final static int INTERNALOWL = 0;
	static boolean loaded = false;

	int type;

	// Type of Factory (default OWL used for now, can add more here)
	public WflowGenFactory(int type) {
		this.type = type;
		if (type == INTERNALOWL && !loaded) {
			// Load in the workflow ontology once so that the system caches it
			System.out.println("Loading Wings ontology..");
			String wflowuri = PropertiesHelper.getWorkflowOntologyURL();
			OntFactory fac = new OntFactory(OntFactory.JENA);
			setLocalRedirects(fac);
			fac.getAPI(wflowuri, OntSpec.PLAIN);
			loaded = true;
		}
	}

	public ComponentCatalog getPC(String ldid) {
		if (this.type == INTERNALOWL) {
			return new ComponentCatalogOWLwithRules(ldid);
		}
		return null;
	}

	// When a particular library name is also passed in
	public ComponentCatalog getPC(String libname, String ldid) {
		if (this.type == INTERNALOWL) {
			if(libname != null)
				return new ComponentCatalogOWLwithRules(libname, ldid);
			else
				return new ComponentCatalogOWLwithRules(ldid);
		}
		return null;
	}
	
	public DataCatalog getDC(String ldid) {
		if (this.type == INTERNALOWL) {
			return new FileBackedWekaDC(ldid);
		}
		return null;

	}
	
	/* Initializers added below for backward compatibility */
	@Deprecated
	public ComponentCatalog getPC(String dummy1, String dummy2, String id) {
		System.err.println("WflowGenFactory.getPC(dcdom,pcdom,id) function is deprecated. Just use getPC(id) instead. The domains are picked up from the properties file");
		return getPC(id);
	}
	@Deprecated
	public DataCatalog getDC(String dummy1, String id) {
		System.err.println("WflowGenFactory.getDC(dcdom,id) function is deprecated. Just use getDC(id) instead. The domain is picked up from the properties file");
		return getDC(id);
	}

	public Template getTemplate(String domain, String templateFile) {
		return new TemplateOWL(domain, templateFile);
	}

	public Template createTemplate(String domain, String templateFile) {
		return new TemplateOWL(domain, templateFile, true);
	}

	public Seed getSeed(String domain, String seedFile) {
		return new SeedOWL(domain, "seeds/" + seedFile);
	}

	public Seed createSeed(String domain, String seedFile, String templateFile) {
		return new SeedOWL(domain, "seeds/" + seedFile, templateFile);
	}
	
	public static void setLocalRedirects(OntFactory fac) {
		fac.addAltEntry(PropertiesHelper.getWorkflowOntologyURL(), 
				"file:" + PropertiesHelper.getOntologyDir() + "/" + PropertiesHelper.getWorkflowOntologyPath());
		fac.addAltEntry(PropertiesHelper.getDCURL()+"/ontology.owl", "file:" + PropertiesHelper.getDCDir()+"/ontology.owl");
		fac.addAltEntry(PropertiesHelper.getPCURL()+"/ontology.owl", "file:" + PropertiesHelper.getPCDir()+"/ontology.owl");
		fac.addAltPrefix(PropertiesHelper.getDCDomainURL(), "file:" + PropertiesHelper.getDCDomainDir());
		fac.addAltPrefix(PropertiesHelper.getPCDomainURL(), "file:" + PropertiesHelper.getPCDomainDir());
		fac.addAltPrefix(PropertiesHelper.getTemplateURL(), "file:" + PropertiesHelper.getTemplatesDir());
		fac.addAltPrefix(PropertiesHelper.getSeedURL(), "file:" + PropertiesHelper.getSeedsDir());
	}
}
