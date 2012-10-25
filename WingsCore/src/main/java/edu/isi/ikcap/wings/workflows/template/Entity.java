package edu.isi.ikcap.wings.workflows.template;

import java.io.Serializable;
import java.net.URI;

public class Entity implements Serializable {
	private static final long serialVersionUID = 1L;
	private URI uri;
	
	public Entity(String id) {
		setID(id);
	}

	public String getID() {
		if(uri != null)
			return uri.toString();
		else
			return null;
	}

	public void setID(String id) {
		try {
			this.uri = new URI(id);
		} catch (Exception e) {
			System.err.println(id + " Not a URI. Only URIs allowed for IDs");
		}
	}
	
	public String getName() {
		if(uri != null)
			return uri.getFragment();
		else
			return null;
	}
	
	public String getNamespace() {
		if(uri != null)
			return uri.getScheme() + ":" + uri.getSchemeSpecificPart() + "#";
		else
			return null;
	}

	public String toString() {
		return getName();
	}
	
	public int hashCode() {
		return uri.hashCode();
	}
}
