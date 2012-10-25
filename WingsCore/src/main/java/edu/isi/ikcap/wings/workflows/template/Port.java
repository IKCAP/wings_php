package edu.isi.ikcap.wings.workflows.template;

public class Port extends Entity {
	private static final long serialVersionUID = 1L;

	private Role role;

	public Port(String id) {
		super(id);
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}
}
