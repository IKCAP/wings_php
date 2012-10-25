package edu.isi.ikcap.wings.workflows.util;

import java.io.FileWriter;

public class NewDomainStubsWriter {
	public static boolean writeDataOntologyStubOWL(String domain, String file) {
		String dcns = PropertiesHelper.getDCURL()+"/ontology.owl#";
		String dcdomns = PropertiesHelper.getDCURL()+"/"+domain+"/ontology.owl#";
		String stub = "<?xml version=\"1.0\"?>\n"+
					"\t<!DOCTYPE rdf:RDF [\n"+
					getStandardOntologyEntitiesOWL()+
					"\t\t<!ENTITY dc \""+dcns+"\" >\n"+
					"\t\t<!ENTITY ontology \""+dcdomns+"\" >\n"+
					"\t]>\n\n"+
					"<rdf:RDF xmlns=\""+dcdomns+"\"\n"+
					getStandardOntologyNamespacesOWL()+
					"\t\txml:base=\""+dcdomns+"\"\n"+
					"\t\txmlns:dc=\""+dcns+"\"\n"+
					"\t\txmlns:ontology=\""+dcdomns+"\">\n\n"+
					
					"\t<owl:Ontology rdf:about=\"\">\n"+
					"\t\t<owl:imports rdf:resource=\""+dcns+"\"/>\n"+
					"\t</owl:Ontology>\n\n\n"+
					"\t<!-- Create Your Data Ontology Here -->\n\n\n"+
					"</rdf:RDF>\n";		
		return writeStringToFile(stub, file);
	}
	
	public static boolean writeDataLibraryStubOWL(String domain, String file) {
		String dcns = PropertiesHelper.getDCURL()+"/ontology.owl#";
		String dcdomns = PropertiesHelper.getDCURL()+"/"+domain+"/ontology.owl#";
		String dclibns = PropertiesHelper.getDCURL()+"/"+domain+"/library.owl#";
		String stub = "<?xml version=\"1.0\"?>\n"+
					"\t<!DOCTYPE rdf:RDF [\n"+
					getStandardOntologyEntitiesOWL()+
					"\t\t<!ENTITY dc \""+dcns+"\" >\n"+
					"\t\t<!ENTITY dcdom \""+dcdomns+"\" >\n"+
					"\t\t<!ENTITY library \""+dclibns+"\" >\n"+
					"\t]>\n\n"+
					"<rdf:RDF xmlns=\""+dclibns+"\"\n"+
					getStandardOntologyNamespacesOWL()+
					"\t\txml:base=\""+dclibns+"\"\n"+
					"\t\txmlns:dc=\""+dcns+"\"\n"+
					"\t\txmlns:dcdom=\""+dcdomns+"\"\n"+
					"\t\txmlns:library=\""+dclibns+"\">\n\n"+
					
					"\t<owl:Ontology rdf:about=\"\">\n"+
					"\t\t<owl:imports rdf:resource=\""+dcdomns+"\"/>\n"+
					"\t</owl:Ontology>\n\n\n"+
					"\t<!-- Create Your Data Library Here -->\n\n\n"+
					"</rdf:RDF>\n";		
		return writeStringToFile(stub, file);
	}
	
	
	public static boolean writeComponentLibraryStubOWL(String domain, String file) {
		String pcns = PropertiesHelper.getPCURL()+"/ontology.owl#";
		String dcns = PropertiesHelper.getDCURL()+"/ontology.owl#";
		String pcdomns = PropertiesHelper.getPCURL()+"/"+domain+"/library.owl#";
		String dcdomns = PropertiesHelper.getDCURL()+"/"+domain+"/ontology.owl#";
		String stub = "<?xml version=\"1.0\"?>\n"+
					"\t<!DOCTYPE rdf:RDF [\n"+
					getStandardOntologyEntitiesOWL()+
					"\t\t<!ENTITY dc \""+dcns+"\" >\n"+
					"\t\t<!ENTITY dcdom \""+dcdomns+"\" >\n"+
					"\t\t<!ENTITY ac \""+pcns+"\" >\n"+
					"\t\t<!ENTITY library \""+pcdomns+"\" >\n"+
					"\t]>\n\n"+
					"<rdf:RDF xmlns=\""+pcdomns+"\"\n"+
					getStandardOntologyNamespacesOWL()+
					"\t\txml:base=\""+pcdomns+"\"\n"+
					"\t\txmlns:dc=\""+dcns+"\"\n"+
					"\t\txmlns:dcdom=\""+dcdomns+"\"\n"+
					"\t\txmlns:ac=\""+pcns+"\"\n"+
					"\t\txmlns:library=\""+pcdomns+"\">\n\n"+
					
					"\t<owl:Ontology rdf:about=\"\">\n"+
					"\t\t<owl:imports rdf:resource=\""+dcdomns+"\"/>\n"+
					"\t\t<owl:imports rdf:resource=\""+pcns+"\"/>\n"+
					"\t</owl:Ontology>\n\n\n"+
					"\t<!-- Create Your Component Library Here -->\n\n\n"+
					"</rdf:RDF>\n";		
		return writeStringToFile(stub, file);
	}
	
	
	public static boolean writeDataOntologyStubTTL(String domain, String file) {
		String dcns = PropertiesHelper.getDCURL()+"/ontology.owl#";
		String dcurl = PropertiesHelper.getDCURL()+"/ontology.owl";
		String dcdomns = PropertiesHelper.getDCURL()+"/"+domain+"/ontology.owl#";
		String dcdomurl = PropertiesHelper.getDCURL()+"/"+domain+"/ontology.owl";
		String stub = getStandardOntologyNamespacesTTL()+
					"@base <"+dcdomurl+"> .\n"+
					"@prefix : <"+dcdomns+"> .\n"+
					"@prefix dc: <"+dcns+"> .\n"+
					"@prefix ontology: <"+dcdomns+"> .\n\n"+
					
					"<"+dcdomurl+"> rdf:type owl:Ontology;\n"+
					"\towl:imports <"+dcurl+"> .\n\n"+
					"# Paste Your Data Ontology Items Here\n\n";	
		return writeStringToFile(stub, file);
	}
	
	public static boolean writeDataLibraryStubTTL(String domain, String file) {
		String dcns = PropertiesHelper.getDCURL()+"/ontology.owl#";
		String dcdomns = PropertiesHelper.getDCURL()+"/"+domain+"/ontology.owl#";
		String dcdomurl = PropertiesHelper.getDCURL()+"/"+domain+"/ontology.owl";
		String dclibns = PropertiesHelper.getDCURL()+"/"+domain+"/library.owl#";
		String dcliburl = PropertiesHelper.getDCURL()+"/"+domain+"/library.owl";
		String stub = getStandardOntologyNamespacesTTL()+
					"@base <"+dcliburl+"> .\n"+
					"@prefix : <"+dclibns+"> .\n"+
					"@prefix dc: <"+dcns+"> .\n"+
					"@prefix dcdom: <"+dcdomns+"> .\n"+
					"@prefix library: <"+dclibns+"> .\n\n"+
					
					"<"+dcliburl+"> rdf:type owl:Ontology;\n"+
					"\towl:imports <"+dcdomurl+"> .\n\n"+
					"# Paste Your Data Library Items Here\n\n";
		return writeStringToFile(stub, file);
	}
	
	
	public static boolean writeComponentLibraryStubTTL(String domain, String file) {
		String pcns = PropertiesHelper.getPCURL()+"/ontology.owl#";
		String pcurl = PropertiesHelper.getPCURL()+"/ontology.owl";
		String dcns = PropertiesHelper.getDCURL()+"/ontology.owl#";
		String pcdomns = PropertiesHelper.getPCURL()+"/"+domain+"/library.owl#";
		String pcdomurl = PropertiesHelper.getPCURL()+"/"+domain+"/library.owl";
		String dcdomns = PropertiesHelper.getDCURL()+"/"+domain+"/ontology.owl#";
		String dcdomurl = PropertiesHelper.getDCURL()+"/"+domain+"/ontology.owl";
		String stub = getStandardOntologyNamespacesTTL()+
					"@base <"+pcdomurl+"> .\n"+
					"@prefix : <"+pcdomns+"> .\n"+
					"@prefix dc: <"+dcns+"> .\n"+
					"@prefix ac: <"+pcns+"> .\n"+
					"@prefix dcdom: <"+dcdomns+"> .\n"+
					"@prefix library: <"+pcdomns+"> .\n\n"+
					
					"<"+pcdomurl+"> rdf:type owl:Ontology;\n"+
					"\towl:imports <"+dcdomurl+">, <"+pcurl+"> .\n\n"+
					"# Paste Your Component Library Items Here\n\n";
		return writeStringToFile(stub, file);
	}
	
	public static boolean writeComponentRulesStub(String domain, String file) {
		String stub = "# You can use the following prefixes in your rules:\n" +
					"# - pc: Refers to the top level Component/Process Catalog Ontology\n"+
					"# - pcdom: Refers to the domain's Component/Process Catalog Ontology/Library\n"+
					"# - dc: Refers to the top level Data Catalog Ontology\n"+
					"# - dcdom: Refers to the domain's Data Catalog Ontology\n"+
					"#[ExampleRule:\n"+
					"#\t(?c rdf:type pcdom:ComponentClass)\n"+
					"#\t(?c pc:hasInput ?idv) (?idv pc:hasArgumentID \"InputRole\")\n"+
					"#\t(?c pc:hasOutput ?odv1) (?odv1 pc:hasArgumentID \"OutputRole\")\n"+
					"#\t(?idv dcdom:hasSomeDataMetricsProperty ?prop)\n"+
					"#\t-> (?odv1 dcdom:hasSomeOtherDataMetricsProperty ?prop)\n";
		return writeStringToFile(stub, file);
	}
	
	private static boolean writeStringToFile(String stub, String file) {
		try {
			FileWriter out = new FileWriter(file);
			out.write(stub);
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	
	private static String getStandardOntologyEntitiesOWL() {
		return "\t\t<!ENTITY owl \"http://www.w3.org/2002/07/owl#\" >\n"+
		"\t\t<!ENTITY xsd \"http://www.w3.org/2001/XMLSchema#\" >\n"+
		"\t\t<!ENTITY rdfs \"http://www.w3.org/2000/01/rdf-schema#\" >\n"+
		"\t\t<!ENTITY rdf \"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" >\n";
	}
	
	private static String getStandardOntologyNamespacesOWL() {
		return 	"\t\txmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n"+
		"\t\txmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n"+
		"\t\txmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"\n"+
		"\t\txmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n";
	}
	
	private static String getStandardOntologyNamespacesTTL() {
		return 	"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"+
		"@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"+
		"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"+
		"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n";
	}
}
