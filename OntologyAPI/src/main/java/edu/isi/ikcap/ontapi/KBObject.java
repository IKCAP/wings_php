package edu.isi.ikcap.ontapi;

// Simple interface to any KBObject
// - Wraps around internal implementation of KBObjects
// *PLEASE* Check KBObjectJena for implementation

public interface KBObject {
	public String getID(); // Concatenation of Namespace and Name, ex:

	// http://www.example.org/owl#Person

	// RDF/OWL based KB's
	public String getNamespace(); // Namespace of object ex:

	// http://www.example.org/owl#

	public String getName(); // LocalName of the object (After the #), ex:

	// Person

	// Literals : KBObject that contains Data values such as numbers or strings
	public Object getValue(); // Example: 4, "hello",..
    public String getDataType(); // Example: http://www.w3.org/2001/XMLSchema#integer
        
	public boolean isLiteral(); // Check if this is a literal or not

	public boolean isList();
	
	// For use by respective KBAPI implementations to manipulate internal
	// structures
	// For example: if using Jena, the internal resource would be a Jena
	// "Resource", etc.
	Object getInternalNode();

	void setInternalNode(Object res);

	// Non-critical functions
	public boolean isThing(); // corresponds to the OWL "Thing" class

	// (subsumes every class)

	public boolean isNothing(); // corresponds to the OWL "Nothing" class (is

	// subsumed by every class)

	public boolean isClassificationProperty(); // The RDF "type" property

  public String shortForm();
  public String shortForm(boolean showLiteralTypes);

}
