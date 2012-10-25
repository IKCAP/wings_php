////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2007, Your Corporation. All Rights Reserved.
////////////////////////////////////////////////////////////////////////////////

package edu.isi.ikcap.ontapi;

import java.util.ArrayList;

/**
 * Name: KBTriple
 * <p/>
 * Package: edu.isi.ikcap.ontapi
 * <p/>
 * User: moody
 * <p/>
 * Date: Aug 24, 2007
 * <p/>
 * Time: 6:18:24 PM
 */
public interface KBTriple {

  /**
   * returns an ArrayList of KBObjects from the triple
   * @return an ArrayList of KBObjects
   */
  public ArrayList<KBObject> toArrayList();

  /**
   * Getter for property 'subject'.
   *
   * @return Value for property 'subject'.
   */
  public KBObject getSubject();

  /**
   * Setter for property 'subject'.
   *
   * @param subject Value to set for property 'subject'.
   */
  public void setSubject(KBObject subject);

  /**
   * Getter for property 'predicate'.
   *
   * @return Value for property 'predicate'.
   */
  public KBObject getPredicate();

  /**
   * Setter for property 'predicate'.
   *
   * @param predicate Value to set for property 'predicate'.
   */
  public void setPredicate(KBObject predicate);

  /**
   * Getter for property 'object'.
   *
   * @return Value for property 'object'.
   */
  public KBObject getObject();

  /**
   * Setter for property 'object'.
   *
   * @param object Value to set for property 'object'.
   */
  public void setObject(KBObject object);

  /**
   * 
   * @return a short form (prefix:subject prefix:predicate prefix:object)
   */
  public String shortForm();
  
  public String fullForm();
  
}
