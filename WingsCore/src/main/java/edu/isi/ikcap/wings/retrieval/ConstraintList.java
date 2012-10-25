package edu.isi.ikcap.wings.retrieval;

import java.util.ArrayList;

import edu.isi.ikcap.ontapi.KBTriple;

public class ConstraintList {
	ArrayList<KBTriple> unmarkedConstraints;
	ArrayList<KBTriple> markedConstraints;

	public ConstraintList() {
		unmarkedConstraints = new ArrayList<KBTriple>();
		markedConstraints = new ArrayList<KBTriple>();
	}

	public ConstraintList(ConstraintList orig) {
		unmarkedConstraints = new ArrayList<KBTriple>(orig.unmarkedConstraints);
		markedConstraints = new ArrayList<KBTriple>(orig.markedConstraints);
	}

	public ConstraintList(ArrayList<KBTriple> constraints) {
		unmarkedConstraints = new ArrayList<KBTriple>(constraints);
		markedConstraints = new ArrayList<KBTriple>();
	}

	public boolean existsUnmarkedItems() {
		return unmarkedConstraints.size() > 0;
	}

	public KBTriple getAndMark() {
		if (unmarkedConstraints.size() == 0)
			return null;
		KBTriple item = unmarkedConstraints.remove(0);
		markedConstraints.add(item);
		return item;
	}

	public int unmarkAll() {
		int size = markedConstraints.size();
		unmarkedConstraints.addAll(markedConstraints);
		markedConstraints.clear();
		return size;
	}

	public void add(KBTriple item) {
		unmarkedConstraints.add(item);
	}

}