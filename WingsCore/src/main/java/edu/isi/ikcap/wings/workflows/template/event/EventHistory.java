package edu.isi.ikcap.wings.workflows.template.event;

import java.util.LinkedList;

import edu.isi.ikcap.wings.workflows.template.Link;
import edu.isi.ikcap.wings.workflows.template.Node;
import edu.isi.ikcap.wings.workflows.template.Template;

public class EventHistory implements TemplateListener {
	private LinkedList<TemplateEvent> events = new LinkedList<TemplateEvent>();

	private boolean recordEvents = true;

	private int historyIndex = -1;

	public void undo() {
		// System.out.println("Current index " + historyIndex);

		try {
			recordEvents = false;
			if (events.size() > historyIndex && historyIndex >= 0) {
				TemplateEvent evt = events.get(historyIndex);

				int evtId = evt.getEventId();

				Template t = (Template) evt.getSource();

				TemplateEvent nevt;

				switch (evtId) {
				case TemplateEvent.NODE_ADDED:
					t.deleteNode(evt.getNode());
					break;
				case TemplateEvent.NODE_DELETED:
					Node n = t.addNode(evt.getNode().getComponentVariable());
					n.setComment(evt.getNode().getComment());
					nevt = new TemplateEvent(t, TemplateEvent.NODE_ADDED, n);
					events.set(historyIndex, nevt);
					break;
				case TemplateEvent.LINK_ADDED:
					t.deleteLink(evt.getLink());
					break;
				case TemplateEvent.LINK_DELETED:
					Link l = evt.getLink();
					Link l2 = t.addLink(l.getOriginNode(), l.getDestinationNode(), l.getOriginPort(), l.getDestinationPort(), l.getVariable());
					nevt = new TemplateEvent(t, TemplateEvent.LINK_ADDED, l2);
					l2.getVariable().setComment(l.getVariable().getComment());
					events.set(historyIndex, nevt);
					break;

				}
			}
			recordEvents = true;
		} catch (Exception e) {
			System.out.println("Unable to undo...");
			e.printStackTrace();
		}

		historyIndex--;
		if (historyIndex < 0)
			historyIndex = -1;

	}

	public void redo() {
		// System.out.println("Current index " + historyIndex);
		if (historyIndex < events.size() - 1) {
			historyIndex++;
			try {
				recordEvents = false;
				if (events.size() > historyIndex && historyIndex >= 0) {
					TemplateEvent evt = events.get(historyIndex);

					int evtId = evt.getEventId();

					Template t = (Template) evt.getSource();

					TemplateEvent nevt;

					switch (evtId) {
					case TemplateEvent.NODE_ADDED:
						Node n = t.addNode(evt.getNode().getComponentVariable());
						n.setComment(evt.getNode().getComment());
						nevt = new TemplateEvent(t, TemplateEvent.NODE_ADDED, n);
						events.set(historyIndex, nevt);
						break;
					case TemplateEvent.NODE_DELETED:
						t.deleteNode(evt.getNode());
						break;
					case TemplateEvent.LINK_ADDED:
						Link l = evt.getLink();
						Link l2 = t.addLink(l.getOriginNode(), l.getDestinationNode(), l.getOriginPort(), l.getDestinationPort(), l.getVariable());
						nevt = new TemplateEvent(t, TemplateEvent.LINK_ADDED, l2);
						l2.getVariable().setComment(l.getVariable().getComment());
						events.set(historyIndex, nevt);
						break;
					case TemplateEvent.LINK_DELETED:
						t.deleteLink(evt.getLink());
						break;

					}
				}
				recordEvents = true;
			} catch (Exception e) {
				System.out.println("Unable to undo...");
				e.printStackTrace();
			}
		}
	}

	public void updateHistory(TemplateEvent te) {
		int len = events.size();
		// System.out.println("History Index: "+historyIndex);
		for (int i = historyIndex + 1; i < len; i++) {
			events.remove(historyIndex + 1);
		}
		events.add(te);
		historyIndex++;
		/*
		 * System.out.println("-----------------"); for (TemplateEvent x :
		 * events) { System.out.println(x); }
		 */
	}

	public void dataVariableBound(TemplateEvent te) {
		System.out.println("Data Variable Bound");
		System.out.println("   var: " + te.getVariable());
		System.out.println("   val: " + te.getDataObject());
		System.out.println("   modified template: " + te.getSource());

	}

	public void linkAdded(TemplateEvent te) {
		if (recordEvents)

			updateHistory(te);
	}

	public void linkDeleted(TemplateEvent te) {
		if (recordEvents)
			updateHistory(te);

	}

	public void nodeAdded(TemplateEvent te) {
		if (recordEvents)
			updateHistory(te);
	}

	public void nodeDeleted(TemplateEvent te) {
		if (recordEvents)
			updateHistory(te);
	}

	public void parameterVariableBound(TemplateEvent te) {
		System.out.println("Parameter Variable Bound");
		System.out.println("   var: " + te.getVariable());
		System.out.println("   val: " + te.getParameterValue());
		System.out.println("   modified template: " + te.getSource());
	}

	public void rulesApplied(TemplateEvent te) {
		System.out.println("Rules applied");
		System.out.println("   modified template: " + te.getSource());
	}

	public void templateCopied(TemplateEvent te) {
		System.out.println("Template copied");
		System.out.println("   copied template id: " + te.getAssociatedTemplate().getID());
		System.out.println("   modified template: " + te.getSource());
	}

	public void templateIdSet(TemplateEvent te) {
		System.out.println("Template id set");
		System.out.println("   id: " + te.getTemplateId());
		System.out.println("   modified template: " + te.getSource());
	}

	public void templateParentSet(TemplateEvent te) {
		System.out.println("Template parent set");
		System.out.println("   parent id: " + te.getAssociatedTemplate().getID());
		System.out.println("   modified template: " + ((Template) te.getSource()).getID());
	}

}
