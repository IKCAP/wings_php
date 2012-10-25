package edu.isi.ikcap.wings.retrieval;

import java.util.ArrayList;

import edu.isi.ikcap.ontapi.KBObject;
import edu.isi.ikcap.ontapi.KBTriple;

public class KBTripleList extends ArrayList<KBTriple> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8903211849760038423L;

	public KBTripleList() {
	}

	public KBTripleList(ArrayList<KBTriple> constraints) {
		super(constraints);
	}

	public String toStringDebug() {
		if (this.size() == 0)
			return "[]";
		String output = "[";
		for (KBTriple triple : this) {
			// output += triple.shortForm() + ", ";
			output += "(" + toStringDebug(triple.getSubject()) + ", " + toStringDebug(triple.getPredicate()) + ", " + toStringDebug(triple.getObject()) + "), ";
		}
		return output.substring(0, output.length() - 2) + "]";
	}

	private String toStringDebug(KBObject object) {
		if (object.isLiteral())
			return object.getValue().toString();
		else
			return object.getName();
	}
}
