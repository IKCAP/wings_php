package edu.isi.ikcap.ontapi.jena;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.TripleCache;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
//import com.hp.hpl.jena.reasoner.Derivation;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.PrintUtil;
import com.hp.hpl.jena.vocabulary.RDFS;
import edu.isi.ikcap.ontapi.*;

import org.mindswap.pellet.jena.PelletReasonerFactory;

import java.io.InputStream;
import java.io.PrintStream;
//import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public class KBAPIJena implements KBAPI {

  // Function results cached: isA, getClassOfInstance, getAllClassesOfInstance
  boolean useCache = false;

  HashMap<String, Object> cache = new HashMap<String, Object>();

  HashMap<String, String> namespacePrefixMap;

  // The ontology model
  OntModel model;
  OntModelSpec rulesModelSpec;
  OntModelSpec modelSpec;
  OntSpec spec;

  Reasoner reasoner;

  String url;

  InputStream inputstream;

  String base;

  private String LANG = "RDF/XML";
  

  public KBAPIJena(String url, OntSpec spec) {
    this.url = url;
    this.spec = spec;
    initialize(spec);
  }

  public KBAPIJena(InputStream data, String base, OntSpec spec) {
    this.inputstream = data;
    this.base = base;
    this.spec = spec;
    initialize(spec);
  }

  public KBAPIJena(OntSpec spec) {
    this.spec = spec;
    initialize(spec);
  }

  public void useRawModel() {
    OntModel tmodel = ModelFactory.createOntologyModel(getOntSpec(spec));
    tmodel.add(model.getRawModel());
    tmodel.setNsPrefixes(model.getNsPrefixMap());
    model = tmodel;
  }

  public void useBaseModel() {
    model.removeAll();
  }

  public void readModel(String loc) {
    if (model != null) {
      model.read(loc);
      this.url = loc;
      initNamespacePrefixMap();
    }
  }

  private OntModelSpec getOntSpec(OntSpec spec) {
    OntModelSpec modelspec = null;
    if (spec == OntSpec.PLAIN) {
      modelspec = OntModelSpec.OWL_MEM;
    } else if (spec == OntSpec.MINI) {
      modelspec = OntModelSpec.OWL_MEM_MINI_RULE_INF;
    } else if (spec == OntSpec.MICRO) {
      modelspec = OntModelSpec.OWL_MEM_MICRO_RULE_INF;
    } else if (spec == OntSpec.DL) {
      modelspec = OntModelSpec.OWL_DL_MEM_RULE_INF;
    } else if (spec == OntSpec.FULL) {
      modelspec = OntModelSpec.OWL_MEM_RULE_INF;
    } else if (spec == OntSpec.TRANS) {
      modelspec = OntModelSpec.OWL_MEM_TRANS_INF;
    } else if (spec == OntSpec.PELLET) {
      cleanPelletSpec(PelletReasonerFactory.THE_SPEC);
      modelspec = PelletReasonerFactory.THE_SPEC;
    }
    return modelspec;
  }
  
  private void cleanPelletSpec(OntModelSpec spec) {
      ArrayList<String> models = new ArrayList<String>();
      for(Iterator<String> miter = spec.getImportModelMaker().listModels(); miter.hasNext();)
    	  models.add(miter.next());
      for(String m : models) spec.getImportModelMaker().removeModel(m);
      
      ArrayList<String> graphs = new ArrayList<String>();
      for(Iterator<String> giter = spec.getImportModelMaker().getGraphMaker().listGraphs(); giter.hasNext();)
    	  graphs.add(giter.next());
      for(String g : graphs) spec.getImportModelMaker().getGraphMaker().removeGraph(g);
  }

  private void readModel() {
    if (model != null) {
      if (this.url != null) {
        //System.out.println("Reading "+url);
        try {
          model.read(this.url);
        }
        catch (Exception e) {
          //System.out.println("File doesn't seem to be an owl file. Trying to read it as TTL");
          model.read(this.url, "TTL");
        }
      } else if (this.inputstream != null) {
        model.read(this.inputstream, this.base);
      }
      initNamespacePrefixMap();
    }
  }

  private void initialize(OntSpec spec) {
    modelSpec = getOntSpec(spec);
    if (modelSpec == null) {
      return;
    }
    // Some specifications to avoid memory leaks while running as in a J2EE Server
    Node.cache(false);
    TripleCache.SIZE = 1;
    
    model = ModelFactory.createOntologyModel(modelSpec);
    readModel();
  }

  private void initNamespacePrefixMap() {
    Map<String, String> prefixNamespaceMap = this.getPrefixNamespaceMap();
    Set<String> keys = prefixNamespaceMap.keySet();
    HashMap<String, String> map = new HashMap<String, String>();
    for (Object key : keys) {
      if (key instanceof String) {
        String prefix = (String) key;
        Object namespace = prefixNamespaceMap.get(key);
        String namespaceString = namespace.toString();
        map.put(namespaceString, prefix);
      }
    }
    this.setNamespacePrefixMap(map);
  }


  public void applyRules(String ruleUrl) {
    if (ruleUrl == null) {
      return;
    }
    List<Rule> rules = Rule.rulesFromURL(ruleUrl);
    applyRulesHelper(rules);
  }

  public void applyRulesFromString(String ruleText) {
    if (ruleText == null || ruleText.equals("")) {
      return;
    }
    List<Rule> rules = Rule.parseRules(ruleText);
    applyRulesHelper(rules);
    rules.clear();
  }

  private void applyRulesHelper(List<Rule> rules) {
    if (rulesModelSpec == null) {
      rulesModelSpec = new OntModelSpec(getOntSpec(OntSpec.PLAIN));
      GenericRuleReasoner reasoner = new GenericRuleReasoner(rules,
        rulesModelSpec.getReasonerFactory());
      reasoner.setOWLTranslation(true);
      reasoner.setTransitiveClosureCaching(true);
      //reasoner.setDerivationLogging(true);
      rulesModelSpec.setReasoner(reasoner);
    }

    // Create a temporary inference model and create new entailments
    OntModel newmodel = ModelFactory.createOntologyModel(rulesModelSpec, null);
    // newmodel = ModelFactory.createOntologyModel(getOntSpec(OntSpec.PLAIN), newmodel);
    newmodel.setNsPrefixes(model.getNsPrefixMap());
    newmodel.add(model.getBaseModel());
    
    for(Iterator<Statement> itst = newmodel.getDeductionsModel().listStatements(); itst.hasNext(); ) {
    	Statement st = itst.next();
    	/*for(Iterator <Derivation> dstit = newmodel.getDerivation(st); dstit.hasNext(); ) {
    		Derivation dst = dstit.next();
    		dst.printTrace(new PrintWriter(System.out, true), true);
    	}*/
    	this.model.add(st);
    }

    // Copy over the inferences to the existing model
    //this.model.add(newmodel.listStatements());
    // this.model.add(newmodel);
  }

  public KBAPIJena(String url, OntSpec spec, String ruleUrl) {
    this.url = url;
    this.spec = spec;
    Model data = FileManager.get().loadModel(url);

    Map<String, String> m = data.getNsPrefixMap();
    for (String prefix : m.keySet()) {
      String nsurl = m.get(prefix);
      PrintUtil.registerPrefix(prefix, nsurl);
    }

    initialize(spec);
    applyRules(ruleUrl);
  }

  // Simple resource, etc queries
  public KBObject getResource(String id) {
    KBObject res = null;
    String key = "gR " + id;
    if (useCache && cache.containsKey(key)) {
      return (KBObject) cache.get(key);
    }
    Resource r = model.getResource(id);
    if (r != null) {
      res = new KBObjectJena(r);
    }
    if (useCache) {
      cache.put(key, res);
    }
    return res;
  }

  public KBObject getConcept(String id) {
    KBObject cls = null;
    String key = "gC " + id;
    if (useCache && cache.containsKey(key)) {
      return (KBObject) cache.get(key);
    }
    OntClass cl = model.getOntClass(id);
    if (cl != null) {
      cls = new KBObjectJena(cl);
    }
    modelSpec = getOntSpec(spec);
    if (useCache) {
      cache.put(key, cls);
    }
    return cls;
  }

  public KBObject getIndividual(String id) {
    KBObject indobj = null;
    String key = "gI " + id;
    if (useCache && cache.containsKey(key)) {
      return (KBObject) cache.get(key);
    }
    Individual ind = model.getIndividual(id);
    if (ind != null) {
      indobj = new KBObjectJena(ind);
    }
    if (useCache) {
      cache.put(key, indobj);
    }
    return indobj;
  }

  public KBObject getProperty(String id) {
    KBObject propobj = null;
    String key = "gP " + id;
    if (useCache && cache.containsKey(key)) {
      return (KBObject) cache.get(key);
    }
    Property prop = model.getProperty(id);
    if (prop != null) {
      propobj = new KBObjectJena(prop);
    }
    if (useCache) {
      cache.put(key, propobj);
    }
    return propobj;
  }

  // Membership queries
  public KBObject getClassOfInstance(KBObject obj) {
    OntClass cl = null;
    String key = "gCOI " + obj.getID();
    if (useCache && cache.containsKey(key)) {
      return (KBObject) cache.get(key);
    }
    Individual ind = getIndividual((Resource) obj.getInternalNode());
    Resource node = ind.getRDFType(true);
    if (node.canAs(OntClass.class)) {
      cl = (OntClass) node.as(OntClass.class);
    }
    KBObject cls = new KBObjectJena(cl);
    if (useCache) {
      cache.put(key, cls);
    }
    return cls;
  }

  @SuppressWarnings("unchecked")
  public ArrayList<KBObject> getAllClassesOfInstance(KBObject obj,
                                                     boolean direct) {
    ArrayList<KBObject> list = new ArrayList<KBObject>();
    String key = "gACOI " + obj.getID();
    if (useCache && cache.containsKey(key)) {
      return (ArrayList<KBObject>) cache.get(key);
    }
    Individual ind = getIndividual((Resource) obj.getInternalNode());
    if (ind == null) {
      ind = model.getIndividual(obj.getID());
    }
    if (ind == null) {
      return list;
    }
    for (Iterator<Resource> i = ind.listRDFTypes(direct); i.hasNext();) {
      Resource node = (Resource) i.next();
      if (node.canAs(OntClass.class)) {
        OntClass cl = (OntClass) node.as(OntClass.class);
        list.add(new KBObjectJena(cl));
      }
    }
    if (useCache) {
      cache.put(key, list);
    }
    return list;
  }

  public ArrayList<KBObject> getInstancesOfClass(KBObject cls) {
    ArrayList<KBObject> list = new ArrayList<KBObject>();

    OntClass cl = (OntClass) cls.getInternalNode();
    for (Iterator<? extends OntResource> it = cl.listInstances(); it.hasNext();) {
      list.add(new KBObjectJena((RDFNode) it.next()));
    }
    return list;
  }

  // Property queries
  public ArrayList<KBObject> getPropertyValues(KBObject obj, KBObject prop) {
    ArrayList<KBObject> v = new ArrayList<KBObject>();
    // System.out.println(obj+" : "+prop);
    Individual ind = getIndividual((Resource) obj.getInternalNode());
    for (NodeIterator it = ind.listPropertyValues((Property) prop
      .getInternalNode()); it.hasNext();) {
      RDFNode node = (RDFNode) it.next();
      if (node != null) {
        KBObjectJena vobj = new KBObjectJena(node);
        v.add(vobj);
      }
    }
    return v;
  }

  public KBObject getPropertyValue(KBObject obj, KBObject prop) {
    ArrayList<KBObject> list = getPropertyValues(obj, prop);
    if (list.size() > 0) {
      return (KBObject) list.get(0);
    }
    return null;
  }

  public ArrayList<KBObject> getDatatypePropertyValues(KBObject obj,
                                                       KBObject prop) {
    DatatypeProperty p = model.getDatatypeProperty(prop.getID());
    ArrayList<KBObject> v = new ArrayList<KBObject>();
    if (p == null) {
      return v;
    }
    Individual ind = getIndividual((Resource) obj.getInternalNode());
    for (NodeIterator it = ind.listPropertyValues(p); it.hasNext();) {
      RDFNode node = (RDFNode) it.next();
      if (node != null) {
        v.add(new KBObjectJena(node));
      }
    }
    return v;
  }

  public KBObject getDatatypePropertyValue(KBObject obj, KBObject prop) {
    ArrayList<KBObject> list = getDatatypePropertyValues(obj, prop);
    if (list.size() == 0) {
      return new KBObjectJena(null, true);
      // return null;
    }
    return list.get(0);
  }

  public ArrayList<KBObject> getSubPropertiesOf(KBObject prop) {
    ArrayList<KBObject> subProps = new ArrayList<KBObject>();
    OntProperty p = model.getOntProperty(prop.getID());
    if (p == null) {
      return subProps;
    }
    for (Iterator<? extends OntProperty> it = p.listSubProperties(); it.hasNext();) {
      Resource subprop = (Resource) it.next();
      if (!subprop.getURI().equals(prop.getID())) {
        subProps.add(new KBObjectJena(subprop));
      }
    }
    return subProps;
  }

  public void setPropertyValue(KBObject obj, KBObject prop, KBObject value) {
    Individual ind = getIndividual((Resource) obj.getInternalNode());
    Property p = (Property) prop.getInternalNode();
    if (value.isLiteral() && value.getInternalNode() == null) {
      ind.setPropertyValue(p, model.createTypedLiteral(value.getValue()));
    } else {
      ind.setPropertyValue(p, (RDFNode) value.getInternalNode());
    }
  }

  public void addPropertyValue(KBObject obj, KBObject prop, KBObject value) {
    Individual ind = getIndividual((Resource) obj.getInternalNode());
    Property p = (Property) prop.getInternalNode();
    if (value.isLiteral() && value.getInternalNode() == null) {
      ind.addProperty(p, model.createTypedLiteral(value.getValue()));
    } else {
      ind.addProperty(p, (RDFNode) value.getInternalNode());
    }
  }

  public KBObject createLiteral(Object literal) {
    return new KBObjectJena(model.createTypedLiteral(literal), true);
  }

  public KBObject createXSDLiteral(String literal, String xsdtype) {
    if (xsdtype == null) {
      return new KBObjectJena(model.createLiteral(literal));
    } else {
      return new KBObjectJena(model.createTypedLiteral(literal, xsdtype));
    }
  }

  // public ArrayList<KBObject> genericTripleQuery(KBObject subj, KBObject
  // pred,
  // KBObject obj) {
  // Individual s = subj != null ? getIndividual((Resource) subj
  // .getInternalResource()) : null;
  // Property p = pred != null ? (Property) pred.getInternalResource()
  // : null;
  // Individual o = obj != null ? getIndividual((Resource) obj
  // .getInternalResource()) : null;
  // ArrayList<KBObject> list = new ArrayList<KBObject>();
  // for (StmtIterator sts = model.listStatements(s, p, o); sts.hasNext();) {
  // ArrayList<KBObject> tmp = new ArrayList<KBObject>();
  // Statement st = (Statement) sts.next();
  // if (s == null) {
  // tmp.add(new KBObjectJena(st.getSubject()));
  // }
  // if (p == null) {
  // tmp.add(new KBObjectJena(st.getPredicate()));
  // }
  // if (o == null) {
  // if (st.getObject().isLiteral()) {
  // tmp.add(new KBObjectJena(st.getObject().asNode()
  // .getLiteralValue(), true));
  // } else {
  // tmp.add(new KBObjectJena((Resource) st.getObject()));
  // }
  // }
  // list.add(tmp);
  // }
  // return list;
  // }

  public ArrayList<KBTriple> genericTripleQuery(KBObject subj, KBObject pred,
                                                KBObject obj) {
    Individual s = subj != null ? getIndividual((Resource) subj
      .getInternalNode()) : null;
    Property p = pred != null ? (Property) pred.getInternalNode() : null;
    RDFNode o = null;
    Model posit = null;
    if (obj != null && obj.isLiteral()) {
      posit = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
      o = posit.createTypedLiteral(obj.getValue());
    } else if (obj != null) {
      o = getIndividual((Resource) obj.getInternalNode());
    }
    ArrayList<KBTriple> list = new ArrayList<KBTriple>();

    StmtIterator sts = null;
    if (posit == null) {
      sts = model.listStatements(s, p, o);
    } else {
      sts = model.listStatements(s, p, o, posit);
    }
    for (; sts.hasNext();) {
      Statement st = (Statement) sts.next();
      KBObject newSubject = new KBObjectJena(st.getSubject());
      KBObject newPredicate = new KBObjectJena(st.getPredicate());
      KBObject newObject = new KBObjectJena(st.getObject());
      list.add(new KBTripleJena(newSubject, newPredicate, newObject));
    }
    return list;
  }

  public ArrayList<ArrayList<SparqlQuerySolution>> sparqlQuery(
    String queryString) {
    ArrayList<ArrayList<SparqlQuerySolution>> list = new ArrayList<ArrayList<SparqlQuerySolution>>();
    Query query = QueryFactory.create(queryString);
    ArrayList<String> vars = new ArrayList<String>(query.getResultVars());
    QueryExecution qexec = QueryExecutionFactory.create(query, model);
    try {
      ResultSet results = qexec.execSelect();
      for (; results.hasNext();) {
        QuerySolution soln = results.nextSolution();
        ArrayList<SparqlQuerySolution> inner = new ArrayList<SparqlQuerySolution>();
        for (String variableName : vars) {
          RDFNode x = soln.get(variableName);
          KBObject item = null;
          if (x.isLiteral()) {
            Literal l = soln.getLiteral(variableName);
            item = new KBObjectJena(l.getValue(), true);
          } else {
            Resource r = soln.getResource(variableName);
            item = new KBObjectJena(r);
          }
          SparqlQuerySolution sqs = new SparqlQuerySolution(
            variableName, item);
          inner.add(sqs);
        }
        list.add(inner);
      }

    } finally {
      qexec.close();
    }
    return list;
  }

  // public ArrayList<ArrayList<KBObject>> sparqlQuery(String queryString) {
  // ArrayList<ArrayList<KBObject>> list = new
  // ArrayList<ArrayList<KBObject>>();
  // Query query = QueryFactory.create(queryString);
  // ArrayList<String> vars = new ArrayList<String>(query.getResultVars());
  // QueryExecution qexec = QueryExecutionFactory.create(query, model) ;
  // try {
  // ResultSet results = qexec.execSelect();
  // for ( ; results.hasNext() ; )
  // {
  // QuerySolution soln = results.nextSolution() ;
  // ArrayList<KBObject> res = new ArrayList<KBObject>();
  // for(int i=0; i<vars.size(); i++) {
  // RDFNode x = soln.get(vars.get(i));
  // KBObject item = null;
  // if(x.isLiteral()) {
  // Literal l = soln.getLiteral(vars.get(i));
  // item = new KBObjectJena(l.getValue(), true);
  // } else {
  // Resource r = soln.getResource(vars.get(i));
  // item = new KBObjectJena(r);
  // }
  // res.add(item);
  // }
  // list.add(res);
  // }
  // } finally { qexec.close() ; }
  // return list;
  // }

  public ArrayList<KBObject> getSubClasses(KBObject cls, boolean direct_only) {

    ArrayList<KBObject> list = new ArrayList<KBObject>();
    OntClass cl = (OntClass) cls.getInternalNode();
    for (Iterator<OntClass> it = cl.listSubClasses(direct_only); it.hasNext();) {
      list.add(new KBObjectJena((Resource) it.next()));
    }

    return list;
  }

  public ArrayList<KBObject> getSuperClasses(KBObject cls, boolean direct_only) {
    ArrayList<KBObject> list = new ArrayList<KBObject>();
    OntClass cl = (OntClass) cls.getInternalNode();
    for (Iterator<OntClass> it = cl.listSuperClasses(direct_only); it.hasNext();) {
      list.add(new KBObjectJena((Resource) it.next()));
    }
    return list;
  }

  public ArrayList<KBObject> getListItems(KBObject list) {
    RDFNode listNode = (RDFNode) list.getInternalNode();

    ArrayList<KBObject> items = new ArrayList<KBObject>();
    if (listNode != null && listNode.canAs(RDFList.class)) {
      RDFList rdfitems = (RDFList) listNode.as(RDFList.class);
      if (rdfitems != null && rdfitems.size() > 0) {
        for (Iterator<RDFNode> it = rdfitems.iterator(); it.hasNext();) {
          items.add(new KBObjectJena(it.next()));
        }
      }
    }
    return items;
  }

  public boolean isA(KBObject obj, KBObject cls) {
    String key = "ISA " + obj.getID() + " " + cls.getID();
    if (useCache && cache.containsKey(key)) {
      return (Boolean) cache.get(key);
    }
    boolean val = false;
    Individual ind = getIndividual((Resource) obj.getInternalNode());
    if (ind == null) {
      return false;
    }
    for (Iterator<Resource> i = ind.listRDFTypes(false); i.hasNext();) {
      if (i.next().toString().equals(cls.getID())) {
        val = true;
        break;
      }
    }
    if (useCache) {
      cache.put(key, new Boolean(val));
    }
    return val;
  }

  public boolean hasSubClass(KBObject cls1, KBObject cls2) {
    if (cls1 == null || cls2 == null) {
      return false;
    }
    String key = "hSC " + cls1.getID() + " " + cls2.getID();
    if (useCache && cache.containsKey(key)) {
      return (Boolean) cache.get(key);
    }
    boolean val = false;
    OntClass cl1 = (OntClass) cls1.getInternalNode();
    OntClass cl2 = (OntClass) cls2.getInternalNode();
    if (cl1 != null && cl2 != null) {
      if (cls1.getID().equals(cls2.getID())) {
        val = true;
      } else if (cl1.hasSubClass(cl2)) {
        val = true;
      }
    }
    if (useCache) {
      cache.put(key, new Boolean(val));
    }
    return val;
  }

  public boolean hasSuperClass(KBObject cls1, KBObject cls2) {
    if (cls1 == null || cls2 == null) {
      return false;
    }
    String key = "hSC " + cls1.getID() + " " + cls2.getID();
    if (useCache && cache.containsKey(key)) {
      return (Boolean) cache.get(key);
    }
    boolean val = false;
    OntClass cl1 = (OntClass) cls1.getInternalNode();
    OntClass cl2 = (OntClass) cls2.getInternalNode();
    if (cl1 != null && cl2 != null) {
      if (cls1.getID().equals(cls2.getID())) {
        val = true;
      } else if (cl1.hasSuperClass(cl2)) {
        val = true;
      }
    }
    if (useCache) {
      cache.put(key, new Boolean(val));
    }
    return val;
  }

  public KBObject createObjectOfClass(String id, KBObject cls) {
    Individual ind = model.createIndividual(id, (Resource) cls
      .getInternalNode());
    return new KBObjectJena(ind);
  }

  public void deleteObject(KBObject obj) {
    // First only delete the object and its owlObjectProperties
    deleteObjectOnly(obj);
    // Delete all owlObjectProperties that have this object as the value
    model.removeAll(null, null, (RDFNode) obj.getInternalNode());
  }

  public void deleteObjectOnly(KBObject obj) {
    // Get all owlObjectProperties of this object, and delete them
    model.removeAll((Resource) obj.getInternalNode(), null, null);
  }

  public KBObject createList(ArrayList<KBObject> items) {
    RDFNode[] nodes = new RDFNode[items.size()];
    int i = 0;
    for (KBObject item : items) {
      nodes[i] = (RDFNode) item.getInternalNode();
      i++;
    }
    RDFList list = model.createList(nodes);
    return new KBObjectJena(list);
  }

  public KBObject createParsetypeLiteral(String xml) {
    Literal lit = model.createLiteral(xml, true);
    return new KBObjectJena(lit);
  }

  public void setPrefixNamespace(String prefix, String namespace) {
    // return model.getNsPrefixURI(prefix);
    model.setNsPrefix(prefix, namespace);
  }

  public Map<String, String> getPrefixNamespaceMap() {
    // return model.getNsPrefixURI(prefix);
    return model.getNsPrefixMap();
  }

  public HashMap<String, String> getNamespacePrefixMap() {
    return this.namespacePrefixMap;
  }

  public void setNamespacePrefixMap(HashMap<String, String> map) {
    this.namespacePrefixMap = map;
  }

  public String getPrefixForNamespace(String namespace) {
    return this.namespacePrefixMap.get(namespace);
  }

  public void setPrefixNamespaceMap(Map<String, String> m) {
    model.setNsPrefixes(m);
  }

  public void importFrom(KBAPI api) {
    // Assume that the passed in api is a jena api
    // Add the submodel
    KBAPIJena japi = (KBAPIJena) api;
    model.addSubModel(japi.model);

    // Add an ontology tag, to define the imports explicitly
    Ontology ont = model.createOntology(model.getNsPrefixURI(""));
    // Add the passed model in the import tag
    ont.addImport(model.createOntResource(japi.model.getNsPrefixURI("")));
    // Add imported ontologies too in the import tag
    for (Object o : japi.model.listImportedOntologyURIs()) {
      String submodelURL = (String) o;
      ont.addImport(model.createOntResource(submodelURL));
    }
  }

  public void copyFrom(KBAPI api) {
    KBAPIJena japi = (KBAPIJena) api;
    model.setNsPrefixes(japi.model.getNsPrefixMap());
    model.addSubModel(japi.model, true);
  }

  public void writeRDF(PrintStream ostr) {
    RDFWriter rdfWriter = model.getWriter("RDF/XML-ABBREV");
    rdfWriter.setProperty("showXmlDeclaration", "true");
    rdfWriter.setProperty("tab", "6");
    rdfWriter.setProperty("xmlbase", this.url);
    rdfWriter.write(model.getBaseModel(), ostr, this.url);
  }

  public String toRdf(boolean showheader, String base) {
    return toRdf(showheader, this.url);
  }

  public String toRdf(boolean showheader) {
    StringWriter out = new StringWriter();
    RDFWriter rdfWriter = model.getWriter("RDF/XML");
    rdfWriter.setProperty("showXmlDeclaration", showheader);
    rdfWriter.setProperty("tab", "6");
    rdfWriter.setProperty("xmlbase", this.url);
    rdfWriter.write(model.getBaseModel(), out, this.url);
    return out.toString();
  }

  public String toAbbrevRdf(boolean showheader) {
    return toAbbrevRdf(showheader, this.url);
  }

  public String toAbbrevRdf(boolean showheader, String base) {
    StringWriter out = new StringWriter();
    RDFWriter rdfWriter = model.getWriter("RDF/XML-ABBREV");
    rdfWriter.setProperty("showXmlDeclaration", showheader);
    rdfWriter.setProperty("tab", "6");
    rdfWriter.setProperty("xmlbase", base);
    rdfWriter.write(model.getBaseModel(), out, this.url);
    return out.toString();
  }

  public void reset() {
//		model.removeAll();
//		model.reset();
    cache.clear();
  }

  public void close() {
    reset();
    model.close();
  }

  Individual getIndividual(Resource node) {
    if (node != null && node.canAs(Individual.class)) {
      return (Individual) node.as(Individual.class);
    }
    return null;
  }

  public void addTriples(ArrayList<KBTriple> triples) {
    if (triples == null) {
      return;
    }
    for (KBTriple triple : triples) {
      addTriple(triple);
    }
  }

  public KBTriple addTriple(KBTriple triple) {
    Statement ontst = this.getOntStatementFromTriple(this.model, triple);
    if (ontst != null) {
      model.add(ontst);
      return triple;
    }
    return null;
  }

  /**
   * returns a KBTriple from an array list of KBObjects
   *
   * @param kbObjects a list of kbObjects
   *
   * @return a KBTriple
   */
  public KBTriple tripleFromArrayList(ArrayList<KBObject> kbObjects) {
    return new KBTripleJena(kbObjects.get(0), kbObjects.get(1), kbObjects
      .get(2));
  }

  /**
   * returns an array list of KBTriples from a list of lists of KBObjects
   *
   * @param lists list of lists of kbobjects
   *
   * @return a list of KBTriples
   */
  public ArrayList<KBTriple> triplesFromArrayLists(
    ArrayList<ArrayList<KBObject>> lists) {
    ArrayList<KBTriple> result = new ArrayList<KBTriple>(lists.size());
    for (ArrayList<KBObject> listOfListsOfKbObject : lists) {
      result.add(this.tripleFromArrayList(listOfListsOfKbObject));
    }
    return result;
  }

  public KBObject getPropertyDomain(KBObject prop) {
    OntProperty p = model.getOntProperty(prop.getID());
    if (p != null) {
      return new KBObjectJena(p.getDomain());
    }
    return null;
  }

  public KBObject getPropertyRange(KBObject prop) {
    OntProperty p = model.getOntProperty(prop.getID());
    if (p != null) {
      return new KBObjectJena(p.getRange());
    }
    return null;
  }

  public void addClassForInstance(KBObject obj, KBObject cls) {
    Individual ind = model.getIndividual(obj.getID());
    OntClass clsobj = (OntClass) cls.getInternalNode();
    if (ind == null || clsobj == null) {
      return;
    }
    ind.addRDFType(clsobj);
  }

  public KBObject createClass(String id) {
    OntClass clsobj = model.createClass(id);
    return new KBObjectJena(clsobj);
  }

  public ArrayList<KBObject> getAllClasses() {
    ArrayList<KBObject> list = new ArrayList<KBObject>();
    for (Iterator<OntClass> i = model.listClasses(); i.hasNext();) {
      list.add(new KBObjectJena((RDFNode) i.next()));
    }
    return list;
  }

  public ArrayList<KBObject> getAllDatatypeProperties() {
    ArrayList<KBObject> list = new ArrayList<KBObject>();
    for (Iterator<DatatypeProperty> i = model.listDatatypeProperties(); i.hasNext();) {
      list.add(new KBObjectJena((RDFNode) i.next()));
    }
    return list;
  }

  public ArrayList<KBObject> getAllObjectProperties() {
    ArrayList<KBObject> list = new ArrayList<KBObject>();
    for (Iterator<ObjectProperty> i = model.listObjectProperties(); i.hasNext();) {
      list.add(new KBObjectJena((RDFNode) i.next()));
    }
    return list;
  }

  public ArrayList<KBObject> getAllProperties() {
    ArrayList<KBObject> list = new ArrayList<KBObject>();
    for (Iterator<OntProperty> i = model.listOntProperties(); i.hasNext();) {
      list.add(new KBObjectJena((RDFNode) i.next()));
    }
    return list;
  }

  public ArrayList<KBObject> getPropertiesOfClass(KBObject cls) {
    ArrayList<KBObject> list = new ArrayList<KBObject>();
    OntClass cl = (OntClass) cls.getInternalNode();
    if (cl == null) {
      cl = model.getOntClass(cls.getID());
    }
    if (cl != null) {
      for (Iterator<OntProperty> i = cl.listDeclaredProperties(); i.hasNext();) {
        list.add(new KBObjectJena((RDFNode) i.next()));
      }
    }
    return list;
  }

  public void setRulesPrefixNamespaceMap(HashMap<String, String> map) {
    for (String prefix : map.keySet()) {
      PrintUtil.registerPrefix(prefix, map.get(prefix));
    }
  }

  public void removeTriple(KBTriple triple) {
    Statement ontst = this.getOntStatementFromTriple(this.model, triple);
    if (ontst != null) {
      this.model.remove(ontst);
    }
    for(Graph subg : this.model.getSubGraphs()) {
    	subg.delete(ontst.asTriple());
    }
  }

  private Statement getOntStatementFromTriple(Model model, KBTriple triple) {
    KBObject subj = triple.getSubject();
    KBObject pred = triple.getPredicate();
    KBObject obj = triple.getObject();
    if (subj != null && pred != null && obj != null) {
      Statement ontst;
      Property predobj = model.getProperty(pred.getID());
      Resource subobj = model.getResource(subj.getID());
      if (obj.isLiteral()) {
        Literal lit = model.createTypedLiteral(obj.getValue());
        ontst = model.createStatement(subobj, predobj, (RDFNode) lit);
      } else {
        Resource obobj = model.getResource(obj.getID());
        ontst = model.createStatement(subobj, predobj, obobj);
      }
      return ontst;
    }
    return null;
  }

  public KBTriple addTriple(KBObject subj, KBObject pred, KBObject obj) {
    return this.addTriple(new KBTripleJena(subj, pred, obj));
  }

  public void removeTriple(KBObject subj, KBObject pred, KBObject obj) {
    this.removeTriple(new KBTripleJena(subj, pred, obj));
  }

  public void setLocalCacheEnabled(boolean cacheflag) {
    this.useCache = cacheflag;
  }

  public void addImport(String ontid, String importurl) {
    Ontology ont = model.getOntology(ontid);
    Resource imp = model.getResource(importurl);
    if (ont == null) {
      ont = model.createOntology(ontid);
    }
    if (imp == null) {
      imp = model.createResource(importurl);
    }
    if (ont != null && imp != null) {
      ont.addImport(imp);
    }
  }

  public ArrayList<String> getImports(String ontid) {
    ArrayList<String> imports = new ArrayList<String>();
    Ontology ont = model.getOntology(ontid);
    if (ont != null) {
      for (Iterator<OntResource> i = ont.listImports(); i.hasNext();) {
        imports.add(i.next().toString());
      }
    }
    return imports;
  }

  public void removeImport(String ontid, String importurl) {
    Ontology ont = model.getOntology(ontid);
    Resource imp = model.getResource(importurl);
    if (ont != null && imp != null && ont.imports(imp)) {
      ont.removeImport(imp);
      if (getImports(ontid).size() == 0) {
        this.deleteObject(new KBObjectJena(ont));
      }
    }
  }

  public void refreshCacheForURL(String url) {
    createNewCacheModel(url, false);
  }

  private OntModel createNewCacheModel(String url, boolean closeOld) {
    FileManager fm = model.getDocumentManager().getFileManager();
    Model oldmodel = fm.getFromCache(url);
    if (oldmodel != null) {
      fm.removeCacheModel(url);
      OntModel impmodel = ModelFactory.createOntologyModel(this.modelSpec);
      try {
        impmodel.read(url);
        fm.addCacheModel(url, impmodel);
        if (closeOld) {
          oldmodel.removeAll();
        }
      }
      catch (Exception e) {
        // File probably has been deleted.. ignore
      }
      return impmodel;
    }
    return null;
  }

  public void setLocal(String uriPrefix, String localDirectory) {
    model.getDocumentManager().addAltEntry(uriPrefix, localDirectory);
  }

  public String getComment(KBObject obj) {
    Individual ind = getIndividual((Resource) obj.getInternalNode());
    if (ind == null) {
      return null;
    }

    RDFNode n = ind.getPropertyValue(RDFS.comment);
    if (n != null && n.isLiteral()) {
      return (String) n.asNode().getLiteralValue();
    }
    return null;
  }

  public void setComment(KBObject obj, String comment) {
    Individual ind = getIndividual((Resource) obj.getInternalNode());
    if (ind == null) {
      return;
    }
    if (comment == null) {
      return;
    }
    ind.setPropertyValue(RDFS.comment, model.createLiteral(comment));
  }

  public ArrayList<String> getAllComments(KBObject obj) {
    Individual ind = getIndividual((Resource) obj.getInternalNode());
    if (ind == null) {
      return null;
    }

    ArrayList<String> comments = new ArrayList<String>();
    for (NodeIterator it = ind.listPropertyValues(RDFS.comment); it.hasNext();) {
      RDFNode n = (RDFNode) it.next();
      if (n != null && n.isLiteral()) {
        comments.add((String) n.asNode().getLiteralValue());
      }
    }
    return comments;
  }

  public boolean hasSubProperty(KBObject prop1, KBObject prop2) {
    if (prop1 == null || prop2 == null) {
      return false;
    }
    String key = "hSP " + prop1.getID() + " " + prop2.getID();
    if (useCache && cache.containsKey(key)) {
      return (Boolean) cache.get(key);
    }
    boolean val = false;
    OntProperty ontProperty1 = (OntProperty) prop1.getInternalNode();
    OntProperty ontProperty2 = (OntProperty) prop2.getInternalNode();
    if (ontProperty1 != null && ontProperty2 != null) {
      if (prop1.getID().equals(prop2.getID())) {
        val = true;
      } else if (ontProperty2.hasSuperProperty(ontProperty1, false)) {
        val = true;
      }
    }
    if (useCache) {
      cache.put(key, new Boolean(val));
    }
    return val;
  }

  public void writeN3(PrintStream ostr) {
    RDFWriter n3Writer = model.getWriter("N3-PP");
    n3Writer.write(model.getBaseModel(), ostr, this.url);
  }

  public String toN3() {
    return toN3(this.url);
  }

  public String toN3(String base) {
    StringWriter out = new StringWriter();
    RDFWriter rdfWriter = model.getWriter("N3-PP");
    rdfWriter.write(model.getBaseModel(), out, base);
    return out.toString();
  }

  public String getDocLanguage() {
    return this.LANG;
  }

  public void setDocLanguage(String lang) {
    this.LANG = lang;
  }

	public ArrayList<KBTriple> getAllTriples() {
		ArrayList<KBTriple> list = new ArrayList<KBTriple>();
		for (Iterator<Statement> sts = this.model.listStatements(); sts.hasNext();) {
			Statement st = (Statement) sts.next();
			KBObject newSubject = new KBObjectJena(st.getSubject());
			KBObject newPredicate = new KBObjectJena(st.getPredicate());
			KBObject newObject = new KBObjectJena(st.getObject());
			list.add(new KBTripleJena(newSubject, newPredicate, newObject));
		}
		return list;
	}
}
