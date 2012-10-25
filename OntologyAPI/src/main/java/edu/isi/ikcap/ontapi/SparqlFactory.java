////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2007, Your Corporation. All Rights Reserved.
////////////////////////////////////////////////////////////////////////////////

package edu.isi.ikcap.ontapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Name: SparqlFactory
  * Package: edu.isi.ikcap.workflows.dc
 * User: moody
 * Date: Aug 16, 2007
 * Time: 11:57:40 PM
 */
public class SparqlFactory {

  /**
   * the namespace of the base ontology - required by isVariable method
   * http://wings-workflows.org/ontology/dc/ontology.owl#
   */
  String baseNamespace;

  /**
   * the namespace of the domain - required by isVariable method
   * http://wings-workflows.org/ontology/dc/dm/ontology.owl#
   */
  String domainNamespace;

  /**
   * the namespace of the library - required by isVariable method
   * http://wings-workflows.org/ontology/dc/dm/library-dc-test-v1.8.owl
   */
  String libraryUrl;

  /**
   * the only constructor
   * @param baseNamespace the namespace of the base ontology
   * @param domainNamespace the namespace of the domain ontology
   * @param libraryUrl the namepsace of the library
   */
  public SparqlFactory(String baseNamespace, String domainNamespace, String libraryUrl) {
    this.baseNamespace = baseNamespace;
    this.domainNamespace = domainNamespace;
    this.libraryUrl = libraryUrl;
  }

  /**
   * returns true iff item is a variable (from a template)
   * @param item a KBObject from a dod
   * @return {true | false}
   */
  private boolean isVariable(KBObject item) {
    return !(item.isLiteral() || (item.getNamespace().equals(this.getBaseNamespace()))
        || (item.getNamespace().equals(this.getDomainNamespace()))
        || (item.getNamespace().equals(this.getLibraryUrl() + "#")));
  }

  public HashMap<String, ArrayList<KBTriple>> dodsForDataVariables(ArrayList<KBTriple> dods) {
    HashMap<String, ArrayList<KBTriple>> result = new HashMap<String, ArrayList<KBTriple>>();
    for (KBTriple triple : dods) {
      KBObject subject = triple.getSubject();
      String subjectName = subject.getName();
      if (this.isVariable(subject)) {
        ArrayList<KBTriple> tmp = result.get(subjectName);
        if (tmp == null) {
          tmp = new ArrayList<KBTriple>();
          result.put(subjectName, tmp);
        }
        tmp.add(triple);
      }
    }
    return result;
  }

  /**
   * constructs the where clause of the query and populates the nameSpacePrefixes,
   * the variableMap, and variables collections
   *      ?dataVariable0 ns1:hasDomain ns1:weather .
	 *      ?dataVariable0 ns2:type ns1:DiscreteInstance .
	 *      ?dataVariable0 ns2:type ns1:Instance .
   * @param dod a data object description
   * @param sq a SparqlQuery object
   * @return a String representing one line of the where clause
   */
  private String makeWhereClauseLineFromDod(ArrayList<KBObject> dod, SparqlQuery sq) {
    StringBuilder whereClause = new StringBuilder();
    String tabChar = "\t";
    String colon = ":";
    String space = " ";
    String end = " ." + System.getProperty("line.separator");

    HashMap<String, String> namespacePrefixes = sq.getNamespacePrefixes();
    HashMap<String, KBObject> variableMap = sq.getVariableMap();
    ArrayList<String> variables = sq.getVariables();

    KBObject subject = dod.get(0);
    KBObject predicate = dod.get(1);
    KBObject object = dod.get(2);

    if (this.isVariable(subject)) {
      String variableName =  subject.getName();
      String sparqlVariableName = "?" + variableName;
      if (!variables.contains(variableName)) {
        variables.add(variableName);
        variableMap.put(variableName, subject);
      }
      whereClause.append(tabChar);
      whereClause.append(sparqlVariableName);
    } else {
      whereClause.append(tabChar);
      whereClause.append(namespacePrefixes.get(subject.getNamespace()));
      whereClause.append(colon);
      whereClause.append(subject.getName());
    }
    whereClause.append(space);
    whereClause.append(namespacePrefixes.get(predicate.getNamespace()));
    whereClause.append(colon);
    whereClause.append(predicate.getName());

    if (this.isVariable(object)) {
      String variableName = object.getName();
      String sparqlVariableName = "?" + variableName;
      if (!variables.contains(variableName)) {
        variables.add(variableName);
        variableMap.put(variableName, object);
      }
      whereClause.append(space);
      whereClause.append(sparqlVariableName);
    } else {
      whereClause.append(space);
      if(object.isLiteral()) {
        if(object.getDataType() != null) {
          whereClause.append("\"");
          whereClause.append(object.getValue());
          whereClause.append("\"^^<");
          whereClause.append(object.getDataType());
          whereClause.append(">");
        } else {
          whereClause.append(object.getValue());
        }
      } else {
        whereClause.append(namespacePrefixes.get(object.getNamespace()));
        whereClause.append(colon);
        whereClause.append(object.getName());
      }
    }
    whereClause.append(end);

    return whereClause.toString();
  }

  /**
   * make the prefix section of the query
   * PREFIX ns2: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
   * PREFIX ns0: <http://wings-workflows.org/ontology/DMDomain/Template1.owl#>
   * PREFIX ns1: <http://wings-workflows.org/ontology/dc/dm/ontology.owl#>
   * @param namespacePrefixes a map from url to (constructed) namespace (e.g. ns0, ns1, ..., ns0)
   * @return a String representing the prefix section of the query
   */
  public String makePrefixLines(HashMap<String, String> namespacePrefixes) {
    StringBuilder prefixes = new StringBuilder();
    Set<String> namespacePrefixesKeys = namespacePrefixes.keySet();
    for (String namespacePrefixesKey : namespacePrefixesKeys) {
      prefixes.append("PREFIX ");
      prefixes.append(namespacePrefixes.get(namespacePrefixesKey));
      prefixes.append(": <");
      prefixes.append(namespacePrefixesKey);
      prefixes.append(">\n");
    }
    return prefixes.toString();
  }

  /**
   * makes the selection line of the query
   * SELECT ?dataVariable0 WHERE {
   * @param variables the dataVariables of the query
   * @return a String representation of the select line
   */
  private String makeSelectLine(ArrayList<String> variables) {
    StringBuilder selectLine = new StringBuilder();
    StringBuilder variablePortion = new StringBuilder();
    for (String variable : variables) {
      variablePortion.append(" ?");
      variablePortion.append(variable);
    }
    selectLine.append("SELECT DISTINCT");
    selectLine.append(variablePortion.toString());
    selectLine.append(" WHERE {\n");
    return selectLine.toString();
  }


  /**
   * constructs a SparqlQuery object from a list of dods
   * a SparqlQuery contains the query, a variable list, a variable map, and a prefix map
   * @param dods a list of data object descriptions
   * @return a SparqlQuery object
   */
  public SparqlQuery makeSparqlQueryFromDataObjectDescriptions(ArrayList<KBTriple> dods) {
    StringBuilder queryBuilder = new StringBuilder();

    SparqlQuery sq = new SparqlQuery();
    int numberOfNamespaces = 0;
    HashMap<String, String> namespacePrefixes = sq.getNamespacePrefixes();
    ArrayList<String> variables = sq.getVariables();

    StringBuilder whereClause = new StringBuilder();
    for (KBTriple triple : dods) {
      // Add any unknown namespace prefixes
      ArrayList<KBObject> dod = triple.toArrayList();
      for (KBObject kbObject : dod) {
        if(!kbObject.isLiteral()) {
          String namespacePrefix = namespacePrefixes.get(kbObject.getNamespace());
          if (namespacePrefix == null) {
            namespacePrefix = "ns" + numberOfNamespaces++;
            namespacePrefixes.put(kbObject.getNamespace(), namespacePrefix);
          }
        }
      }
      // populates the variables and variableMap while building the where clauses
      whereClause.append(this.makeWhereClauseLineFromDod(dod, sq));
    }

    String prefixLines = this.makePrefixLines(namespacePrefixes);
    String selectLine = this.makeSelectLine(variables);

    queryBuilder.append(prefixLines);
    queryBuilder.append(selectLine);
    queryBuilder.append(whereClause.toString());
    queryBuilder.append("\n}\n");
    //System.out.println(queryBuilder);
    sq.setQuery(queryBuilder.toString());

    return sq;
  }


  /**
   * Getter for property 'baseNamespace'.
   *
   * @return Value for property 'baseNamespace'.
   */
  public String getBaseNamespace() {
    return baseNamespace;
  }

  /**
   * Setter for property 'baseNamespace'.
   *
   * @param baseNamespace Value to set for property 'baseNamespace'.
   */
  public void setBaseNamespace(String baseNamespace) {
    this.baseNamespace = baseNamespace;
  }

  /**
   * Getter for property 'domainNamespace'.
   *
   * @return Value for property 'domainNamespace'.
   */
  public String getDomainNamespace() {
    return domainNamespace;
  }

  /**
   * Setter for property 'domainNamespace'.
   *
   * @param domainNamespace Value to set for property 'domainNamespace'.
   */
  public void setDomainNamespace(String domainNamespace) {
    this.domainNamespace = domainNamespace;
  }

  /**
   * Getter for property 'libraryUrl'.
   *
   * @return Value for property 'libraryUrl'.
   */
  public String getLibraryUrl() {
    return libraryUrl;
  }

  /**
   * Setter for property 'libraryUrl'.
   *
   * @param libraryUrl Value to set for property 'libraryUrl'.
   */
  public void setLibraryUrl(String libraryUrl) {
    this.libraryUrl = libraryUrl;
  }
}
