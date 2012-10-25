////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2007, Your Corporation. All Rights Reserved.
////////////////////////////////////////////////////////////////////////////////

package edu.isi.ikcap.ontapi;

import java.util.HashMap;
import java.util.ArrayList;

/**
 * Name: SparqlQuery
 * <p/>
 * Package: edu.isi.ikcap.workflows
 * <p/>
 * User: moody
 * <p/>
 * Date: Aug 22, 2007
 * <p/>
 * Time: 12:39:03 PM
 */
public class SparqlQuery {

  /**
   * the query in sparql format
   */
  public String query;

  /**
   * a map of variable names (dataVariable) to variable objects (http://wings-workflows.org/ontology/DMDomain/Template1.owl#dataVariable0}
   * variableMap = {dataVariable0=http://wings-workflows.org/ontology/DMDomain/Template1.owl#dataVariable0}
   */
  public HashMap<String, KBObject> variableMap;

  /**
   * variable names (dataVariable0)
   */
  public ArrayList<String> variables;

  /**
   * name space maps
   * {http://www.w3.org/1999/02/22-rdf-syntax-ns#=ns2,
   *  http://wings-workflows.org/ontology/DMDomain/Template1.owl#=ns0,
   *  http://wings-workflows.org/ontology/dc/dm/ontology.owl#=ns1}
   */
  HashMap<String, String> namespacePrefixes;

  /**
   * default constructor - initializes fields
   */
  public SparqlQuery() {
    this.query = null;
    this.variableMap = new HashMap<String, KBObject>();
    this.variables = new ArrayList<String>();
    this.namespacePrefixes = new HashMap<String, String>();
  }


  public String toString() {
    return "SparqlQuery{" +
        "query='" + query + '\'' +
        ", variableMap=" + variableMap +
        ", variables=" + variables +
        ", namespacePrefixes=" + namespacePrefixes +
        '}';
  }

  /**
   * Getter for property 'namespacePrefixes'.
   *
   * @return Value for property 'namespacePrefixes'.
   */
  public HashMap<String, String> getNamespacePrefixes() {
    return namespacePrefixes;
  }

  /**
   * Setter for property 'namespacePrefixes'.
   *
   * @param namespacePrefixes Value to set for property 'namespacePrefixes'.
   */
  public void setNamespacePrefixes(HashMap<String, String> namespacePrefixes) {
    this.namespacePrefixes = namespacePrefixes;
  }

  /**
   * Getter for property 'query'.
   *
   * @return Value for property 'query'.
   */
  public String getQuery() {
    return query;
  }

  /**
   * Setter for property 'query'.
   *
   * @param query Value to set for property 'query'.
   */
  public void setQuery(String query) {
    this.query = query;
  }

  /**
   * Getter for property 'variableMap'.
   *
   * @return Value for property 'variableMap'.
   */
  public HashMap<String, KBObject> getVariableMap() {
    return variableMap;
  }

  /**
   * Setter for property 'variableMap'.
   *
   * @param variableMap Value to set for property 'variableMap'.
   */
  public void setVariableMap(HashMap<String, KBObject> variableMap) {
    this.variableMap = variableMap;
  }

  /**
   * Getter for property 'variables'.
   *
   * @return Value for property 'variables'.
   */
  public ArrayList<String> getVariables() {
    return variables;
  }

  /**
   * Setter for property 'variables'.
   *
   * @param variables Value to set for property 'variables'.
   */
  public void setVariables(ArrayList<String> variables) {
    this.variables = variables;
  }
}
