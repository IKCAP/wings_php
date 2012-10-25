////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2007, Your Corporation. All Rights Reserved.
////////////////////////////////////////////////////////////////////////////////

package edu.isi.ikcap.wings.catalogs.data.impl.isi;

import edu.isi.ikcap.ontapi.KBAPI;

import java.util.Map;
import java.util.HashMap;

/**
 * Name: FileBackedDCTest
 * <p/>
 * Package: edu.isi.ikcap.wings.catalogs.data
 * <p/>
 * User: moody
 * <p/>
 * Date: Aug 8, 2007
 * <p/>
 * Time: 4:26:02 PM
 */
public class FileBackedDCTest {

	public static void main(String[] args) {

		// String smallLibrary = "library.owl";
		//String testLibrary = "library-dc-test-v1.8.owl";

		FileBackedWekaDC dc = new FileBackedWekaDC("test123");

		KBAPI api = dc.getApi();
		Map foo = api.getPrefixNamespaceMap();
		System.out.println("foo = " + foo);
		HashMap<String, String> bar = api.getNamespacePrefixMap();
		System.out.println("bar = " + bar);
		// HashMap<String, KBObject> cmap = dc.getConceptMap();
		// HashMap<String, KBObject> opmap = dc.getPropertyMap();
		// HashMap<String, KBObject> dtmap = dc.getDatatypeMap();
		//
		// OntFactory ontologyFactory = dc.getOntologyFactory();
		// String rdfNamespace = (String)
		// api.getPrefixNamespaceMap().get("rdf");
		//
		//
		// KBObject rdfType = api.getResource(rdfNamespace + "type");
		//
		//
		// String workflowNamespace =
		// "http://wings-workflows.org/ontology/sr/workflow-ontology.owl#";
		// KBObject dataVariable0 = api.getResource(workflowNamespace +
		// "dataVariable0");
		// KBObject dataVariable1 = api.getResource(workflowNamespace +
		// "dataVariable1");
		// KBObject dataVariable2 = api.getResource(workflowNamespace +
		// "dataVariable2");
		// ArrayList<KBTriple> dods = new ArrayList<KBTriple>();
		//
		// KBTripleJena dod1 = new KBTripleJena(dataVariable0, rdfType,
		// cmap.get("ContinuousInstance"));
		// dods.add(dod1);
		//
		// KBObject westLa = api.getIndividual(dc.getDomainNamespace() +
		// "west-la");
		// KBTripleJena dod2 = new KBTripleJena(dataVariable0,
		// opmap.get("hasArea"), westLa);
		// dods.add(dod2);
		//
		// // not working
		// // KBObject hasNumberOfInstances = ontologyFactory.getDataObject(new
		// Integer(100));
		// // KBTripleJena dod3 = new KBTripleJena(dataVariable0,
		// dtmap.get("hasNumberOfInstances"),
		// // hasNumberOfInstances);
		// // dods.add(dod3);
		//
		// KBObject weatherDomain = api.getIndividual(dc.getDomainNamespace() +
		// "weather");
		// KBTripleJena dod4 = new KBTripleJena(dataVariable0,
		// opmap.get("hasDomain"), weatherDomain);
		// dods.add(dod4);
		//
		//
		// KBTripleJena dod5 = new KBTripleJena(dataVariable1, rdfType,
		// cmap.get("Model"));
		// //KBTripleJena dod5 = new KBTripleJena(dataVariable1,
		// opmap.get("hasDomain"), weatherDomain);
		// dods.add(dod5);
		//
		// //
		// // dod6.add(dataVariable2);
		// // dod6.add(rdfType);
		// // dod6.add(cmap.get("DiscreteInstance"));
		// // dods.add(dod6);
		// //
		// //// dod7.add(dataVariable2);
		// //// dod7.add(opmap.get("hasArea"));
		// //// dod7.add(eastLa);
		// ////
		// //// // added to the dods
		// //// dods.add(dod7);
		// //
		// //
		// // KBObject hasNumberOfInstances = ontologyFactory.getDataObject(new
		// Integer(100));
		// // dod8.add(dataVariable2);
		// // dod8.add(dtmap.get("hasNumberOfInstances"));
		// // dod8.add(hasNumberOfInstances);
		// // dods.add(dod8);
		// //
		// //// KBObject hasMissingValues = ontologyFactory.getDataObject(new
		// Boolean(false));
		// //// dod9.add(dataVariable2);
		// //// dod9.add(dtmap.get("hasMissingValues"));
		// //// dod9.add(hasMissingValues);
		// //// dods.add(dod9);
		// //
		// // KBObject soybeanDomain = api.getIndividual(dc.getDomainNamespace()
		// + "soybean");
		// // dod10.add(dataVariable2);
		// // dod10.add(opmap.get("hasDomain"));
		// // dod10.add(soybeanDomain);
		// // dods.add(dod10);
		//
		// System.out.println("dods = " + dods);

		// ArrayList<ArrayList<DataVariableDataObjectBinding>> results =
		// dc.findDataSources(dods, true);
		// System.out.println("results = " + results);

		// for (ArrayList<ArrayList<KBObject>> result : results) {
		// for (ArrayList<KBObject> kbObjects : result) {
		// KBObject dataObject = kbObjects.get(1);
		// String metrics = dc.findDataMetricsForDataObject(dataObject.getID(),
		// new ArrayList<String>());
		// System.out.println("metrics = " + metrics);
		// }
		// }

		// ArrayList<ArrayList>foo = new ArrayList<ArrayList>();
		// ArrayList<String> as = new ArrayList<String>();
		// as.add("a1");
		// as.add("a2");
		// as.add("a3");
		// as.add("a4");
		// as.add("a5");
		// as.add("a6");
		// as.add("a7");
		// as.add("a8");
		// as.add("a9");
		// as.add("a10");
		// as.add("a11");
		//
		// ArrayList<String> bs = new ArrayList();
		// bs.add("b1");
		// bs.add("b2");
		// bs.add("b3");
		// bs.add("b4");
		// bs.add("b5");
		// bs.add("b6");
		// bs.add("b6");
		// bs.add("b6");
		// bs.add("b6");
		// bs.add("b6");
		// bs.add("b6");
		// bs.add("b6");
		// bs.add("b6");
		// bs.add("b6");
		// bs.add("b6");
		// bs.add("b6");
		// bs.add("b6");
		// bs.add("b6");
		// bs.add("b6");
		//
		// ArrayList<String> cs = new ArrayList();
		// cs.add("c1");
		// cs.add("c2");
		// cs.add("c3");
		// cs.add("c4");
		// cs.add("c5");
		// cs.add("c6");
		// cs.add("c6");
		// cs.add("c6");
		// cs.add("c6");
		// cs.add("c6");
		// cs.add("c6");
		// cs.add("c6");
		// cs.add("c6");
		// cs.add("c6");
		// cs.add("c6");
		// cs.add("c6");
		// cs.add("c6");
		// cs.add("c6");
		// cs.add("c6");
		//
		//
		//
		// foo.add(as);
		// foo.add(bs);
		// foo.add(cs);
		// ArrayList<ArrayList>allcombos = dc.allCombinations(foo);
		// System.out.println("allcombos = " + allcombos);

	}

}
