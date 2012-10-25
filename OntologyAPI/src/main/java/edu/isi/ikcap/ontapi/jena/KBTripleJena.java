////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2007, Your Corporation. All Rights Reserved.
////////////////////////////////////////////////////////////////////////////////

package edu.isi.ikcap.ontapi.jena;

import edu.isi.ikcap.ontapi.KBObject;
import edu.isi.ikcap.ontapi.KBTriple;

import java.util.ArrayList;

/**
 * Name: KBTripleJena
 * <p/>
 * Package: edu.isi.ikcap.ontapi.jena
 * <p/>
 * User: moody
 * <p/>
 * Date: Aug 24, 2007
 * <p/>
 * Time: 6:19:52 PM
 */
public class KBTripleJena implements KBTriple {

  /**
   * the subject
   */
  public KBObject subject;

  /**
   * the predicate
   */
  public KBObject predicate;

  /**
   * the object
   */
  public KBObject object;


  /**
   * creates a triple
   * @param subject the subject
   * @param predicate the predicate
   * @param object the object
   */
  public KBTripleJena(KBObject subject, KBObject predicate, KBObject object) {
    this.subject = subject;
    this.predicate = predicate;
    this.object = object;
  }


  /**
   * returns an ArrayList of KBObjects from the triple
   * @return an ArrayList of KBObjects
   */
  public ArrayList<KBObject> toArrayList() {
    ArrayList<KBObject>result = new ArrayList<KBObject>(3);
    result.add(this.getSubject());
    result.add(this.getPredicate());
    result.add(this.getObject());
    return result;
  }


  /**
   * returns true iff the ID of the subject object and predicate
   * are equal to their counterparts in other
   *
   * @param other a KBTriple
   * @return true if the IDs of the elements are (string) equal
   */
  public boolean sameAs(KBTriple other) {
    String subjectString = this.getSubject().getID();
    String predicateString = this.getPredicate().getID();
    String objectString = this.getObject().getID();
    return (subjectString.equals(other.getSubject().getID()) &&
        predicateString.equals(other.getPredicate().getID()) &&
        objectString.equals(other.getObject().getID()));
  }

  
  public String toString() {
    return this.shortForm();
//    return "KBTripleJena{" +
//        "subject=" + subject +
//        ", predicate=" + predicate +
//        ", object=" + object +
//        '}';
  }
  
  public String fullForm() {
	    return "{" +
	        "subject=" + subject +
	        ", predicate=" + predicate +
	        ", object=" + object +
	        '}';
  }

  /** {@inheritDoc} */
  public KBObject getSubject() {
    return subject;
  }

  /** {@inheritDoc} */
  public void setSubject(KBObject subject) {
    this.subject = subject;
  }

  /** {@inheritDoc} */
  public KBObject getPredicate() {
    return predicate;
  }

  /** {@inheritDoc} */
  public void setPredicate(KBObject predicate) {
    this.predicate = predicate;
  }

  /** {@inheritDoc} */
  public KBObject getObject() {
    return object;
  }

  /** {@inheritDoc} */
  public void setObject(KBObject object) {
    this.object = object;
  }

  public String shortForm() {
    StringBuilder result = new StringBuilder();
    String space = " ";
    result.append("(");
    result.append(this.getSubject().shortForm());
    result.append(space);
    result.append(this.getPredicate().shortForm());
    result.append(space);
    result.append(this.getObject().shortForm());
    result.append(")");
    return result.toString();
  }
}
