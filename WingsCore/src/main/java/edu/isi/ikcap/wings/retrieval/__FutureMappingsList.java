package edu.isi.ikcap.wings.retrieval;

import java.util.ArrayList;
import java.util.HashMap;

import edu.isi.ikcap.ontapi.KBObject;

/**
 * A mappings list is a list of lists of mappings from 1 query variable to a
 * list of possible workflow elements.
 * <p>
 * A mappings list has the form:
 * <p>
 * 
 * <pre>
 * [[var1 --&gt; [obj1, obj2], var2 --&gt; [obj2, obj3, obj4]],
 *  [var1 --&gt; [obj1, obj2, obj3], var2 --&gt; [obj4]]]
 * </pre>
 * <p>
 * meaning that ((var1 can be mapped to obj1 OR obj2) AND (var2 can be mapped to
 * obj2 OR obj3 OR obj4)) OR ((var1 can be mapped to obj1 OR obj2 OR obj3) AND
 * (var2 can be mapped to obj4)).
 * 
 * @author Gonzalo Florez
 * 
 */
public class __FutureMappingsList {
	@SuppressWarnings("unused")
	private ArrayList<HashMap<KBObject, ArrayList<KBObject>>> map;

	// public void addMapping(KBObject variable);
}
