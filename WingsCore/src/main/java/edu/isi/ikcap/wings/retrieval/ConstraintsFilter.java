package edu.isi.ikcap.wings.retrieval;

import java.util.ArrayList;
import java.util.HashMap;

import edu.isi.ikcap.ontapi.KBObject;
import edu.isi.ikcap.ontapi.KBTriple;

public class ConstraintsFilter {

	public static final String PASSED = "PASSED";
	public static final String NOT_PASSED = "NOT_PASSED";

	/**
	 * This wildcard is used when we want to add a filter that matches ANY
	 * entity in a concrete namespace.
	 */
	// TODO this approach is not the best, but it works... we should think some
	// other way.
	// The problem is that, when we create a ResourceImpl in which we define a
	// namespace but the name is the null string,
	// the created object is assigned the name "null". Using a character like
	// "?" or "*" doesn't work neither, because
	// Jena adds the caracter to the namespace.
	public static final String WILDCARD = "null";

	/**
	 * <p>
	 * Takes a list of constraints and divides it into two lists, one with the
	 * constraints that satisfy any of the filters in filters and other with the
	 * ones that satisfy none.
	 * <p>
	 * Each of the filters is a triple. The filtering process consist in
	 * checking each constraint against each filter and "letting pass" only
	 * those that have the same subject, predicate and object.
	 * <p>
	 * A null value on any of the parts of a filter is considered as a wildcard.
	 * 
	 * @param constraints
	 * @param filters
	 * @return
	 */
	public static HashMap<String, KBTripleList> divideConstraintsAny(KBTripleList constraints, KBTripleList filters) {
		KBTripleList passed = new KBTripleList();
		KBTripleList notPassed = new KBTripleList();
		for (KBTriple constraint : constraints) {
			if (passesFilterAny(constraint, filters)) {
				passed.add(constraint);
			} else {
				notPassed.add(constraint);
			}
		}
		HashMap<String, KBTripleList> output = new HashMap<String, KBTripleList>();
		output.put(PASSED, passed);
		output.put(NOT_PASSED, notPassed);
		return output;
	}

	/**
	 * <p>
	 * Takes a list of constraints and divides it into several lists, one for
	 * each of the filter groups. In each list are stored the constraints that
	 * satisfy any of the filters in the corresponding filter group.
	 * <p>
	 * If the constraint doesn't satisfy any filter, it is stored in the last
	 * group.
	 * <p>
	 * If the constraint could fit in several groups it is only stored in the
	 * one corresponding to the first filter that is satisfied.
	 * 
	 * @param constraints
	 * @param filters
	 * @return
	 */
	public static ArrayList<KBTripleList> filterConstraintsAny(KBTripleList constraints, ArrayList<KBTripleList> filters) {
		ArrayList<KBTripleList> output = new ArrayList<KBTripleList>();
		for (int i = 0; i <= filters.size(); i++) {
			output.add(new KBTripleList());
		}
		for (KBTriple constraint : constraints) {
			current_constraint: {
				int i = 0;
				for (i = 0; i < filters.size(); i++) { // TODO use the enum
					KBTripleList filterGroup = filters.get(i);
					if (passesFilterAny(constraint, filterGroup)) {
						output.get(i).add(constraint);
						break current_constraint;
					}
				}
				output.get(i).add(constraint);
			}
		}
		return output;
	}

	// public static ArrayList<ArrayList<KBTriple>> filterConstraints(
	// ArrayList<KBTriple> constraints, ArrayList<KBTriple> filters) {
	// ArrayList<ArrayList<KBTriple>> output = new
	// ArrayList<ArrayList<KBTriple>>();
	// for (int i = 0; i <= filters.size(); i++) {
	// output.add(new ArrayList<KBTriple>());
	// }
	// for (KBTriple constraint : constraints) {
	// outer_for_loop: {
	// int numFilter = 0;
	// for (KBTriple triple : filters) {
	// if (passesFilter(constraint, triple)) {
	// output.get(numFilter).add(constraint);
	// break outer_for_loop;
	// }
	// numFilter++;
	// }
	// output.get(numFilter).add(constraint);
	// }
	// }
	// return output;
	// }

	private static boolean passesFilterAny(KBTriple constraint, ArrayList<KBTriple> filterGroup) {
		for (KBTriple filter : filterGroup) {
			if (passesFilter(constraint, filter))
				return true;
		}
		return false;
	}

	private static boolean passesFilter(KBTriple constraint, KBTriple filter) {
		if (!equalConstraintToFilter(constraint.getSubject(), filter.getSubject()))
			return false;
		if (!equalConstraintToFilter(constraint.getPredicate(), filter.getPredicate()))
			return false;
		return equalConstraintToFilter(constraint.getObject(), filter.getObject());
	}

	private static boolean equalConstraintToFilter(KBObject constraintPart, KBObject filterPart) {
		if (filterPart == null)
			return true;
		if (constraintPart == null)
			return false;
		String filterNamespace = filterPart.getNamespace();
		if (filterNamespace != null && !filterNamespace.equals(WILDCARD)) {
			if (!filterNamespace.equals(constraintPart.getNamespace()))
				return false;
		}
		String filterName = filterPart.getName();
		if (filterName != null && !filterName.equals(WILDCARD)) {
			if (!filterName.equals(constraintPart.getName()))
				return false;
		}
		return true;
	}
}
