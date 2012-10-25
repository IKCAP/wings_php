package edu.isi.ikcap.wings.retrieval;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import edu.isi.ikcap.ontapi.KBObject;

/**
 * 
 * @author gonzalo
 * 
 */
public class MappingsList {

	HashMap<KBObject, KBObject> mappings;

	public MappingsList() {
		mappings = new HashMap<KBObject, KBObject>();
	}

	public MappingsList(MappingsList original) {
		mappings = new HashMap<KBObject, KBObject>(original.mappings);
	}

	/**
	 * Adds a mapping to the current mappings.
	 * <p>
	 * If the mapping exists it is substituted by the new one. Otherwise, a new
	 * mapping is created.
	 * 
	 * @param variable
	 * @param entity
	 * @return the entity with wich the variable had a mapping or null if it is
	 *         a new mapping.
	 */
	public KBObject addMapping(KBObject variable, KBObject entity) {
		return mappings.put(variable, entity);
	}

	public Iterator<Entry<KBObject, KBObject>> iterator() {
		return mappings.entrySet().iterator();
	}

	public KBObject getEntity(KBObject variable) {
		return mappings.get(variable);
	}

	public KBObject removeMapping(KBObject variable) {
		return mappings.remove(variable);
	}

	public boolean removeMapping(KBObject variable, KBObject entity) {
		if (hasMapping(variable, entity)) {
			mappings.remove(variable);
			return true;
		} else {
			return false;
		}
	}

	public boolean hasMapping(KBObject variable) {
		return mappings.containsKey(variable);
	}

	public boolean hasMapping(KBObject variable, KBObject entity) {
		KBObject mappedEntity = mappings.get(variable);
		if (mappedEntity == null) {
			return entity == null;
		} else {
			// TODO check that equals is implemented and works the way we want
			return mappedEntity.equals(entity);
		}
	}

	@Override
	public String toString() {
		return mappings.toString();
	}

	public int size() {
		return mappings.size();
	}

	public String toStringDebug() {
		if (mappings.size() == 0)
			return "[]";
		String output = "[";
		for (Entry<KBObject, KBObject> entry : mappings.entrySet()) {
			output += entry.getKey().toString() + " --> " + entry.getValue().toString() + ", ";
		}
		return output.substring(0, output.length() - 2) + "]";
	}

	public String toStringDebug2() {
		if (mappings.size() == 0)
			return "[]";
		String output = "[";
		for (Entry<KBObject, KBObject> entry : mappings.entrySet()) {
			output += entry.getKey().getName().toString() + " --> " + entry.getValue().getName().toString() + ", ";
		}
		return output.substring(0, output.length() - 2) + "]";
	}

	public boolean hasDoubleMappings() {
		HashMap<KBObject, KBObject> inverseMappings = new HashMap<KBObject, KBObject>();
		for (Entry<KBObject, KBObject> mapping : mappings.entrySet()) {
			if (inverseMappings.containsKey(mapping.getValue()))
				return true;
			inverseMappings.put(mapping.getValue(), mapping.getKey());
		}
		return false;
	}
}
