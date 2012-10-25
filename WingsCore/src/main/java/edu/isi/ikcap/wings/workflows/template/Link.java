package edu.isi.ikcap.wings.workflows.template;

import edu.isi.ikcap.wings.workflows.template.Port;
import edu.isi.ikcap.wings.workflows.template.variables.Variable;

public class Link extends Entity {
	private static final long serialVersionUID = 1L;

	private Node fromNode;
	private Node toNode;
	private Port fromPort;
	private Port toPort;
	private Variable variable;


	public static short IN = 1;
	public static short INOUT = 2;
	public static short OUT = 3;

	public Link(String id, Node fromNode, Node toNode, Port fromPort, Port toPort) {
		super(id);
		this.fromNode = fromNode;
		this.toNode = toNode;
		this.fromPort = fromPort;
		this.toPort = toPort;
	}

	public Port getDestinationPort() {
		return toPort;
	}

	public Node getDestinationNode() {
		return toNode;
	}

	public Port getOriginPort() {
		return fromPort;
	}

	public Node getOriginNode() {
		return fromNode;
	}

	public Variable getVariable() {
		return this.variable;
	}

	public void setVariable(Variable v) {
		this.variable = v;
	}

	public boolean isInOutLink() {
		if (this.fromNode != null && this.toNode != null) {
			return true;
		}
		return false;
	}

	public boolean isInputLink() {
		// if (linkType == IN) {
		if (this.fromNode == null && this.toNode != null) {
			return true;
		}
		return false;
	}

	public boolean isOutputLink() {
		// if (linkType == OUT) {
		if (this.fromNode != null && this.toNode == null) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return (fromNode == null ? "" : fromNode + " [" + fromPort + "]  ") + " -> " + (toNode == null ? "" : toNode + " [" + toPort + "]") + "\n" + " ("
				+ variable + ")";
	}

	public void setDestinationPort(Port port) {
		this.toPort = port;
	}

	public void setDestinationNode(Node n) {
		this.toNode = n;
	}

	public void setOriginPort(Port port) {
		this.fromPort = port;
	}

	public void setOriginNode(Node n) {
		this.fromNode = n;
	}
}
