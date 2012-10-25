package edu.isi.ikcap.wings.workflows.template.sets;

public class ValueBinding extends Binding {
	private static final long serialVersionUID = 1L;

	public ValueBinding() {
	}

	public ValueBinding(Object value) {
		this.setValue(value);
	}

	public ValueBinding(ValueBinding b) {
		super(b);
	}

	public ValueBinding(Object[] values) {
		for (Object val : values) {
			this.add(new ValueBinding(val));
		}
	}
}
