package edu.isi.ikcap.ontapi;

import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinRegistry;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.LocationMapper;

import edu.isi.ikcap.ontapi.jena.KBAPIJena;
import edu.isi.ikcap.ontapi.jena.KBObjectJena;
import edu.isi.ikcap.ontapi.jena.KBTripleJena;
import edu.isi.ikcap.ontapi.jena.extrules.date.*;
import edu.isi.ikcap.ontapi.jena.extrules.math.*;

import java.io.InputStream;

public class OntFactory {
	public static int JENA = 0;

	public static int SESAME = 1;

	int type;

	// Type of Factory (default JENA used for now, can add more here)
	public OntFactory(int type) {
		this.type = type;
		if(this.type == JENA) {
			OntDocumentManager.getInstance().clearCache();
			OntDocumentManager.getInstance().setCacheModels(false);
			FileManager.get().setModelCaching(false);
			
			// Extra Rules
			Power rule1 = new Power();
			BuiltinRegistry.theRegistry.register(rule1);
			AddDays rule2 = new AddDays();
			BuiltinRegistry.theRegistry.register(rule2);
			SubtractDays rule3 = new SubtractDays();
			BuiltinRegistry.theRegistry.register(rule3);			
		}
	}

    public KBAPI getAPI(String url, OntSpec spec) {
        if (this.type == JENA) {
            return new KBAPIJena(url, spec);
        }
        return null;
	}

	public KBAPI getAPI(String url, OntSpec spec, String ruleUrl) {
		if (this.type == JENA) {
			return new KBAPIJena(url, spec, ruleUrl);
		}
		return null;
	}

	public KBAPI getAPI(InputStream data, String base, OntSpec spec) {
		if (this.type == JENA) {
			return new KBAPIJena(data, base, spec);
		}
		return null;
	}

	public KBAPI getAPI(OntSpec spec) {
		return this.getAPI(null, spec);
	}

	public KBObject getObject(String id) {
		if (this.type == JENA) {
			return new KBObjectJena(id);
		}
		return null;
	}

	public KBObject getDataObject(Object value) {
		if (this.type == JENA) {
			return new KBObjectJena(value, true);
		}
		return null;
	}

	public void addAltEntry(String ns, String file) {
		//System.out.println("Using local ontologies");
		if (this.type == JENA) {
			// Setup alternate URLS for a localized system
			OntDocumentManager.getInstance().getFileManager().getLocationMapper().addAltEntry(ns, file);
			LocationMapper.get().addAltEntry(ns, file);
		} else if (this.type == SESAME) {

		}
	}
	
	public void addAltPrefix(String nsPrefix, String dir) {
		//System.out.println("Using local ontologies");
		if (this.type == JENA) {
			// Setup alternate URLS for a localized system
			OntDocumentManager.getInstance().getFileManager().getLocationMapper().addAltPrefix(nsPrefix, dir);
			LocationMapper.get().addAltPrefix(nsPrefix, dir);
		} else if (this.type == SESAME) {

		}
	}
	
	public KBTriple getTriple(KBObject subj, KBObject pred, KBObject obj) {
		if(this.type == JENA) {
			return new KBTripleJena(subj, pred, obj);
		}
		return null;
	}
}
