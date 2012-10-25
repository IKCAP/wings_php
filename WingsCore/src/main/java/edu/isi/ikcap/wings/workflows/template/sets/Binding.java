package edu.isi.ikcap.wings.workflows.template.sets;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Calendar;
import java.util.TimeZone;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;


/*
 * A Binding can contain just one URI or a set of URIs
 * 
 */
public class Binding extends WingsSet implements Serializable {
	private static final long serialVersionUID = 1L;

	protected URI uri;
	protected transient Object value;
	protected String metrics;

	protected Object data;

	public Binding() {
	}

	public Binding(String id) {
		setID(id);
		this.obj = this.uri;
	}

	public Binding(Binding b) {
		super(b);
	}

	public Binding(String[] ids) {
		for (String id : ids) {
			this.add(new Binding(id));
		}
	}

	public String getID() {
		if (uri != null)
			return uri.toString();
		return null;
	}

	public void setID(String id) {
		try {
			this.uri = new URI(id);
			this.obj = this.uri;
		} catch (Exception e) {
			System.err.println(id + " Not a URI. Only URIs allowed for Binding IDs");
		}
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
		this.setObject();
	}

	public void setValue(Object[] values) {
		for (Object val : values) {
			Binding b = new Binding();
			b.setValue(val);
			this.add(b);
		}
	}
	
	public String getName() {
		if (uri != null)
			return uri.getFragment();
		return null;
	}

	public String getNamespace() {
		if (uri != null)
			return uri.getScheme() + ":" + uri.getSchemeSpecificPart() + "#";
		return null;
	}

	public String toString() {
		if (isSet())
			return super.toString();
		return getValue() != null ? getValue().toString(): getName();
		//return (getValue() != null ? getValue().toString() : getName()) + (getData() != null ? "(" + getData().toString() + ")" : "");
	}

	public boolean isValueBinding() {
		return (getValue() != null);
	}

	public boolean isURIBinding() {
		return (getID() != null);
	}

	public void setMetrics(String metrics) {
		this.metrics = metrics;
	}

	public String getMetrics() {
		return this.metrics;
	}

	public void setData(Object data) {
		this.data = data;
		super.obj = "" + this.uri + this.data.toString();
	}

	public Object getData() {
		return this.data;
	}
	
	/*public Binding clone() {
		Binding b = (Binding)super.clone();
		if(isSet()) {
			for(int i=0; i<this.size(); i++) {
				b.add(i, ((Binding)this.get(i)).clone());
			}
		}
		return b;
	}*/
	
	/*
	 * XSDDateTime isn't serializable. 
	 * So converting it to Calendar and back on Serialization 
	 */
	protected void setObject() {
		this.obj = this.value;
		if(this.obj.getClass().getSimpleName().equals("XSDDateTime")) {
			this.obj = ((XSDDateTime)this.obj).asCalendar();
			((Calendar)this.obj).setTimeZone(TimeZone.getTimeZone("UTC"));
		}
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		if(this.uri == null) {
			this.value = this.obj;
			if(this.obj != null &&
				this.obj.getClass().getSimpleName().equals("GregorianCalendar")) {
				//((Calendar)this.obj).setTimeZone(TimeZone.getTimeZone("UTC"));
				this.value = new XSDDateTime((Calendar)this.obj);
				((XSDDateTime)this.value).narrowType(XSDDatatype.XSDdate);
			}
		}
	}
}
