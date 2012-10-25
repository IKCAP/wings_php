package edu.isi.ikcap.ontapi;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author varunr
 *         <p/>
 *         KBAPI Interface : Needs to be implemented (see KBAPIJena for an example) Uses
 *         KBObject interface (see KBObjectJena for an example)
 *         <p/>
 *         Also, check OntFactory/OntSpec to see the list of constants used
 */
public interface KBAPI {
  public void useRawModel();

  public void useBaseModel();

  public void readModel(String location);

  // Query for Ontology elements
  public KBObject getResource(String id);

  public KBObject getConcept(String id);

  public KBObject getIndividual(String id);

  public KBObject getProperty(String id);

  public ArrayList<KBObject> getAllClasses();

  public ArrayList<KBObject> getAllDatatypeProperties();

  public ArrayList<KBObject> getAllObjectProperties();

  public ArrayList<KBObject> getAllProperties();

  // Classificiation
  public KBObject getClassOfInstance(KBObject obj);

  public ArrayList<KBObject> getAllClassesOfInstance(KBObject obj, boolean direct);

  public ArrayList<KBObject> getInstancesOfClass(KBObject cls);

  public ArrayList<KBObject> getPropertiesOfClass(KBObject cls);

  public void addClassForInstance(KBObject obj, KBObject cls);

  // Property Queries/Setting
  public KBObject getPropertyValue(KBObject obj, KBObject prop);

  public ArrayList<KBObject> getPropertyValues(KBObject obj, KBObject prop);

  public KBObject getDatatypePropertyValue(KBObject obj, KBObject prop);

  public ArrayList<KBObject> getDatatypePropertyValues(KBObject obj, KBObject prop);

  public KBObject getPropertyDomain(KBObject prop);

  public KBObject getPropertyRange(KBObject prop);

  public String getComment(KBObject obj);

  public void setComment(KBObject obj, String comment);

  public ArrayList<String> getAllComments(KBObject obj);

  public void setPropertyValue(KBObject obj, KBObject prop, KBObject value);

  public void addPropertyValue(KBObject obj, KBObject prop, KBObject value);

  // Generic Triple query (return all possible values of input KBObjects which
  // are null)
  // -- All inputs null : No need to handle
  // -- Returns an ArrayList of KBObject[] Arrays

  public ArrayList<KBTriple> genericTripleQuery(KBObject subj, KBObject pred, KBObject obj);

  public ArrayList<KBTriple> getAllTriples();
  
  public ArrayList<ArrayList<SparqlQuerySolution>> sparqlQuery(String queryString);

  public void addTriples(ArrayList<KBTriple> statements);

  public KBTriple addTriple(KBTriple triple);

  public KBTriple addTriple(KBObject subj, KBObject pred, KBObject obj);

  public void removeTriple(KBTriple triple);

  public void removeTriple(KBObject subj, KBObject pred, KBObject obj);

  // Returns a KBTriple from an array list of KBObjects
  public KBTriple tripleFromArrayList(ArrayList<KBObject> kbObjects);

  public ArrayList<KBTriple> triplesFromArrayLists(ArrayList<ArrayList<KBObject>> lists);

  // isA, subClasses, subProperties
  public boolean isA(KBObject obj, KBObject cls);

  public boolean hasSubClass(KBObject cls1, KBObject cls2);

  public boolean hasSuperClass(KBObject cls1, KBObject cls2);

  public boolean hasSubProperty(KBObject prop1, KBObject prop2);

  public ArrayList<KBObject> getSubClasses(KBObject cls, boolean direct_only);

  public ArrayList<KBObject> getSuperClasses(KBObject cls, boolean direct_only);

  public ArrayList<KBObject> getSubPropertiesOf(KBObject prop);

  // Creation/Deletion
  public KBObject createClass(String id);

  public KBObject createObjectOfClass(String id, KBObject cls);

  public KBObject createLiteral(Object literal);

  public KBObject createXSDLiteral(String literal, String xsdtype);

  public KBObject createParsetypeLiteral(String xml);

  // The following deletes the object, all it's owlObjectProperties
  // It also deletes all owlObjectProperties that have obj as the value
  public void deleteObject(KBObject obj);

  // The following function only deletes the object and it's
  // owlObjectProperties
  public void deleteObjectOnly(KBObject obj);

  // Lists
  public KBObject createList(ArrayList<KBObject> items);

  public ArrayList<KBObject> getListItems(KBObject list);

  // RDF/OWL specific
  public Map<String, String> getPrefixNamespaceMap(); // Returns a hashmap of prefix:namespace

  // These are the ns:prefix maps for reading the ontology files
  public void setPrefixNamespaceMap(Map<String, String> map);

  public void setPrefixNamespace(String prefix, String namespace);

  // These are the ns:prefix maps used for reading rule files
  public void setRulesPrefixNamespaceMap(HashMap<String, String> map);

  public void applyRules(String ruleUrl);

  public void applyRulesFromString(String ruleText);

  public HashMap<String, String> getNamespacePrefixMap();

  public void setNamespacePrefixMap(HashMap<String, String> map);

  public String getPrefixForNamespace(String namespace);

  public String toRdf(boolean showheader);

  public String toRdf(boolean showheader, String base);

  public String toAbbrevRdf(boolean showheader);

  public String toAbbrevRdf(boolean showheader, String base);

  public void writeRDF(PrintStream ostr);

  public void writeN3(PrintStream ostr);

  public String toN3();

  public String toN3(String base);

  public void setDocLanguage(String lang);

  public String getDocLanguage();

  public void setLocalCacheEnabled(boolean cacheflag);

  public void addImport(String ontid, String importurl);

  public void removeImport(String ontid, String importurl);

  public ArrayList<String> getImports(String ontid);

  public void refreshCacheForURL(String importurl);

  // Import from the model referred to by the passed api
  public void importFrom(KBAPI api);

  // Copy over the model referred to by the passed api
  public void copyFrom(KBAPI api);

  // Reset/Delete internal data
  public void reset();

  public void close();

  public void setLocal(String uriPrefix, String localDirectory);
}
