////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2007, Your Corporation. All Rights Reserved.
////////////////////////////////////////////////////////////////////////////////

package edu.isi.ikcap.ontapi;

/**
 * Name: SparqlQuerySolution
 * <p/>
 * Package: edu.isi.ikcap.ontapi
 * <p/>
 * User: moody
 * <p/>
 * Date: Aug 26, 2007
 * <p/>
 * Time: 9:29:31 AM
 */
public class SparqlQuerySolution {

  /**
   * the variable
   */
  public String variable;

  /**
   * the kbobject
   */
  public KBObject object;

  /**
   * a new sparql query solution
   * @param variable a variable
   * @param object a kbobject
   */
  public SparqlQuerySolution(String variable, KBObject object) {
    this.variable = variable;
    this.object = object;
  }

  /**
   * Getter for property 'variable'.
   *
   * @return Value for property 'variable'.
   */
  public String getVariable() {
    return variable;
  }

  /**
   * Setter for property 'variable'.
   *
   * @param variable Value to set for property 'variable'.
   */
  public void setVariable(String variable) {
    this.variable = variable;
  }

  /**
   * Getter for property 'object'.
   *
   * @return Value for property 'object'.
   */
  public KBObject getObject() {
    return object;
  }

  /**
   * Setter for property 'object'.
   *
   * @param object Value to set for property 'object'.
   */
  public void setObject(KBObject object) {
    this.object = object;
  }

  @Override
  public String toString() {
    StringBuilder string = new StringBuilder();
    string.append("( ");
    string.append(variable);
    string.append(" = ");
    string.append(object.getID());
    string.append(" )");
    return string.toString();
  }
}
