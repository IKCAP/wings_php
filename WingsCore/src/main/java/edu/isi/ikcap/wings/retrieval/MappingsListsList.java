package edu.isi.ikcap.wings.retrieval;

import java.util.ArrayList;
import java.util.Iterator;

import edu.isi.ikcap.ontapi.KBObject;

public class MappingsListsList implements Iterable<MappingsList> {
	/**
	 * @return
	 * @see java.util.AbstractList#iterator()
	 */
	public Iterator<MappingsList> iterator() {
		return list.iterator();
	}

	ArrayList<MappingsList> list;

	public MappingsListsList() {
		list = new ArrayList<MappingsList>();
		list.add(new MappingsList());
	}

	public MappingsListsList(MappingsListsList original) {
		list = new ArrayList<MappingsList>(original.list);
	}

	public MappingsList getMappings(int i) throws IndexOutOfBoundsException {
		return list.get(i);
	}

	/**
	 * Adds a mapping to a mappings list. If the list has already a mapping for
	 * that variable, a new mappings list is created with all the values equal
	 * to the original except the new one.
	 * 
	 * @param index
	 * @param variable
	 * @param entity
	 * @return
	 * @throws IndexOutOfBoundsException
	 */
	public void addMapping(int index, KBObject variable, KBObject entity) throws IndexOutOfBoundsException {
		MappingsList ml = list.get(index);
		if (ml.hasMapping(variable)) {
			MappingsList new_ml = new MappingsList(ml);
			new_ml.addMapping(variable, entity);
			list.add(new_ml);
		} else {
			ml.addMapping(variable, entity);
		}
	}

	/**
	 * Creates as many variable mappings as elements in the entities list and
	 * then makes a cartesian product between them and the existing mappings
	 * lists.
	 * <p>
	 * For instance, if the initial mappings are
	 * 
	 * <pre>
	 * [[v1--&gt;o1, v2--&gt;o2], 
	 *  [v1--&gt;o2, v2--&gt;o1]]
	 * </pre>
	 * 
	 * and the inputs to the algorithm are v3 and [o3, o4] the result is:
	 * 
	 * <pre>
	 * [[v1--&gt;o1, v2--&gt;o2, v3--&gt;o3], 
	 *  [v1--&gt;o2, v2--&gt;o1, v3--&gt;o3],
	 *  [v1--&gt;o1, v2--&gt;o2, v3--&gt;o4], 
	 *  [v1--&gt;o2, v2--&gt;o1, v3--&gt;o4]]
	 * </pre>
	 * 
	 * @param variable
	 * @param entities
	 */
	public void addMapping(KBObject variable, ArrayList<? extends KBObject> entities) {
		// ArrayList<MappingsList> newList = new ArrayList<MappingsList>();
		int size = list.size();
		for (KBObject entity : entities) {
			// addMapping(variable, entity);
			for (int i = 0; i < size; i++) {
				addMapping(i, variable, entity);
				// MappingsList mappingsList = list.get(i);
				// MappingsList newMappingsList = new
				// MappingsList(mappingsList);
				// newMappingsList.addMapping(variable, entity);
				// newList.add(newMappingsList);
			}
		}
		// list = newList;
	}

	/**
	 * @return
	 * @see java.util.ArrayList#size()
	 */
	public int size() {
		if (list == null)
			return 0;
		if (list.size() == 0)
			return 0;
		MappingsList ml = list.get(0);
		if (ml.size() == 0)
			return 0;
		return list.size();
	}

	/**
	 * <p>
	 * Adds a mapping to all the existing mappings lists. If there is a mapping
	 * already defined in a list for the variable, a new mappings list is
	 * created, equal to the first in all the mappings except in the one of the
	 * variable.
	 * 
	 * @param variable
	 * @param entity
	 */
	public void addMapping(KBObject variable, KBObject entity) {
		int size = list.size();
		for (int i = 0; i < size; i++) {
			addMapping(i, variable, entity);
		}
	}

	public KBObject removeMapping(int index, KBObject variable) {
		return list.get(index).removeMapping(variable);
	}

	public void removeMappingFromAll(KBObject variable, KBObject entity) {
		for (MappingsList mapping : list) {
			mapping.removeMapping(variable, entity);
		}
	}

	public void merge(MappingsListsList newList) {
		list.addAll(newList.list);
		// TODO Check there aren't two equal mappings
		// Idea: build a tree in which, each level correspondes to one query
		// variable and each node
		// to the entity it is mapped to, and the leaves to the mappings id. If
		// there are 2 leaves in the same node ==> there is a repetition
	}

	@Override
	public String toString() {
		return list.toString();
	}

	public String toStringDebug() {
		String output = "";
		int i = 0;
		for (MappingsList ml : list) {
			output += i + ": " + ml.toStringDebug() + "\n";
			i++;
		}
		return output;
	}

	public String toStringDebug2() {
		String output = "";
		int i = 0;
		for (MappingsList ml : list) {
			output += i + ": " + ml.toStringDebug2() + "\n";
			i++;
		}
		return output;
	}

	public void removeDoubleMappings() {
		for (Iterator<MappingsList> it = list.iterator(); it.hasNext();) {
			MappingsList mpl = (MappingsList) it.next();
			if (mpl.hasDoubleMappings())
				it.remove();
		}
	}

}
