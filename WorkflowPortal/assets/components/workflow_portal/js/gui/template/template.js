/*
 * Generic Template Class
 */

var Template = function(id, store, editor) {
	this.id = id;
	this.store = store;
	this.nodes = {};
	this.links = {};
	this.ports = {};
	this.variables = {};
	this.sideEffects = {};
	this.errors = {};
	this.canvasItems = [];
	this.editor = editor;
	this.initialize();
	this.DBG = false;
};


Template.prototype.initialize = function() {
	//this.mergeLinks(); // Cleanup

	for(var i in this.store.nodes) {
		var node = this.store.nodes[i];
		if(typeof(node) == "function") continue;
		var n = new Node(node.id, node.component.type, parseInt(node.x)+0.5, parseInt(node.y)+0.5);
		n.setComponentRule(node.crule);
		n.setPortRule(node.prule, node.pruleOp);
		n.setConcrete(node.component.ex);
		n.setBinding(node.component.binding);

		// in case inputPorts/outputPorts are explicitly provided for the nodes
		if(node.inputPorts) {
			for(var j=0; j<node.inputPorts.length; j++) {
				var p = node.inputPorts[j];
				this.ports[p.id] = new Port(p.id, p.role.id);
				this.ports[p.id].dim = parseInt(p.role.dim);
				n.addInputPort(this.ports[p.id]);
			}
		}
		if(node.outputPorts) {
			for(var j=0; j<node.outputPorts.length; j++) {
				var p = node.outputPorts[j];
				this.ports[p.id] = new Port(p.id, p.role.id);
				this.ports[p.id].dim = parseInt(p.role.dim);
				n.addOutputPort(this.ports[p.id]);
			}
		}
		this.nodes[node.id] = n;
	}
	for(var i in this.store.variables) {
		var variable = this.store.variables[i];
		if(typeof(variable) == "function") continue;
		this.variables[variable.id] = new Variable(variable.id, variable.id, parseInt(variable.x)+0.5, parseInt(variable.y)+0.5, variable.type);
		if(variable.dim) this.variables[variable.id].setDimensionality(parseInt(variable.dim));
		if(variable.unknown) this.variables[variable.id].setIsUnknown(variable.unknown);
		if(variable.autofill) this.variables[variable.id].setAutoFill(variable.autofill);
	}
	for(var i in this.store.links) {
		var link = this.store.links[i];
		if(typeof(link) == "function") continue;
		var fromPort = null;
		var toPort = null;
		if(link.fromPort != null && link.fromPort.id) {
			if(!this.ports[link.fromPort.id]) {
				this.ports[link.fromPort.id] = new Port(link.fromPort.id, link.fromPort.role.id);
				this.ports[link.fromPort.id].dim = parseInt(link.fromPort.role.dim);
				this.nodes[link.fromNode].addOutputPort(this.ports[link.fromPort.id]);
			}
			this.ports[link.fromPort.id].type = this.variables[link.variable].type;

			var v = this.variables[link.variable];
			if(link.fromPort.role.dim) v.setDimensionality(parseInt(link.fromPort.role.dim));
			if(!link.toPort) v.setIsOutput(true);

			fromPort = this.ports[link.fromPort.id];
		}
		if(link.toPort != null && link.toPort.id) {
			if(!this.ports[link.toPort.id]) {
				this.ports[link.toPort.id] = new Port(link.toPort.id, link.toPort.role.id);
				this.ports[link.toPort.id].dim = parseInt(link.toPort.role.dim);
				this.nodes[link.toNode].addInputPort(this.ports[link.toPort.id]);
			}
			this.ports[link.toPort.id].type = this.variables[link.variable].type;

			var v = this.variables[link.variable];
			if(link.toPort.role.dim) v.setDimensionality(parseInt(link.toPort.role.dim));
			if(!link.fromPort) v.setIsInput(true);

			toPort = this.ports[link.toPort.id];
		}
		link = new Link(fromPort, toPort, this.variables[link.variable]);
		this.links[link.id] = link;
	}
	for(var i in this.nodes) {
		var node = this.nodes[i];
		if(typeof(node) == "function") continue;
		node.setConcrete(node.isConcrete);
		this.canvasItems = this.canvasItems.concat(this.nodes[node.id].getLayerItems());
	}
	for(var i in this.variables) {
		var variable = this.variables[i];
		if(typeof(variable) == "function") continue;
		this.canvasItems = this.canvasItems.concat(this.variables[variable.id].getLayerItems());
	}
	
	for(var i in this.links) {
		var link = this.links[i];
		if(typeof(link) == "function") continue;
		this.canvasItems.push(this.links[link.id].getLayerItem());
	}
	this.forwardSweep();
};

Template.prototype.saveToStore = function(showFullPorts) {
	this.store.nodes = [];
	this.store.links = [];
	this.store.variables = [];
	var cnt = 1;
	var ports = {};
	for(var i in this.nodes) {
		var n = this.nodes[i];
		var ips = []; var ops = [];
		var iports = n.getInputPorts();
		var oports = n.getOutputPorts();
		for(var j=0; j<iports.length; j++) {
			var ip = {id:iports[j].id, role:{id:iports[j].name,dim:iports[j].dim}};
			ports[ip.id] = ip;
			ips.push(ip);
		}
		for(var j=0; j<oports.length; j++) {
			var op = {id:oports[j].id, role:{id:oports[j].name,dim:oports[j].dim}};
			ports[op.id] = op;
			ops.push(op);
		}
		this.store.nodes.push({
			id:n.id, x:n.x, y:n.y, 
			crule:n.crule, prule:n.prule, pruleOp:n.pruleOp, 
			component:{name:'component'+cnt, type:n.component, concrete:n.isConcrete, ex:n.isConcrete, binding:n.binding},
			inputPorts:ips, outputPorts:ops
		});
		cnt++;
	}
	for(var i in this.variables) {
		var v = this.variables[i];
		this.store.variables.push({id:v.id, x:v.x, y:v.y, dim:v.isInput?v.dim:0, type: v.type, unknown:v.unknown, autofill:v.autofill});
	}
	for(var i in this.links) {
		var l = this.links[i];
		var link = {id:l.id, variable:l.variable.id};
		if(l.fromPort) {
			link.fromNode = l.fromPort.partOf.id;
			link.fromPort = l.fromPort.id;
			if(showFullPorts) link.fromPort = ports[link.fromPort];
		}
		if(l.toPort) {
			link.toNode = l.toPort.partOf.id;
			link.toPort = l.toPort.id;
			if(showFullPorts) link.toPort = ports[link.toPort];
		}
		this.store.links.push(link);
	}

	this.mergeLinks(); // Cleanup
};

Template.prototype.getCanvasItems = function() {
	return this.canvasItems;
};


Template.prototype.getFreeVariableId = function(varid) {
	if(!this.variables[varid]) return varid;
	var i=1;
	while(this.variables[varid+i]) i++;
	return varid+i;
};

Template.prototype.getFreeNodeId = function(nodeid) {
	if(!this.nodes[nodeid]) return nodeid;
	var i=1;
	while(this.nodes[nodeid+i]) i++;
	return nodeid+i;
};

Template.prototype.getFreePortId = function(portid) {
	var pfx = 'port_';
	if(!this.ports[pfx+portid]) return pfx+portid;
	var i=1;
	while(this.ports[pfx+portid+i]) i++;
	return pfx+portid+i;
};

Template.prototype.getNumLinks = function() {
	var num=0;
	for(var lid in this.links) num++;
	return num;
};

// There can be multiple links with the same variable
Template.prototype.getLinksWithVariable = function(variable) {
	var links = [];
	for(var lid in this.links) {
		var l = this.links[lid];
		if(l.variable == variable) {
			links.push(l);
		}
	}
	return links;
};

// We can have multiple links coming out of a port
Template.prototype.getLinksFromPort = function(port) {
	var links = [];
	for(var lid in this.links) {
		var l = this.links[lid];
		if(l.fromPort == port) {
			links.push(l);
		}
	}
	return links;
};

// We can have only 1 link going to a port
Template.prototype.getLinkToPort = function(port) {
	for(var lid in this.links) {
		var l = this.links[lid];
		if(l.toPort == port) {
			return l;
		}
	}
	return null;
};

Template.prototype.getLinksWithVariable = function(variable) {
	var links = [];
	for(var lid in this.links) {
		var l = this.links[lid];
		if(l.variable == variable) {
			links.push(l);
		}
	}
	return links;
};

Template.prototype.getLinksFromPortWithVariable = function(port, variable) {
	var links = [];
	for(var lid in this.links) {
		var l = this.links[lid];
		if(l.variable == variable && l.fromPort == port) {
			links.push(l);
		}
	}
	return links;
};

// We can have only 1 link going to a port
Template.prototype.getLinkToPortWithVariable = function(port, variable) {
	var links = [];
	for(var lid in this.links) {
		var l = this.links[lid];
		if(l.variable == variable && l.toPort == port) {
			return l;
		}
	}
	return null;
};

Template.prototype.getNodesWithComponent = function(compid) {
	var nodes = [];
	for(var nid in this.nodes) {
		var n = this.nodes[nid];
		if(n.component == compid) nodes.push(n);
	}
	return nodes;
};

/**
 * Add Variable
 */
Template.prototype.addVariable = function(varid) {
	varid = this.getFreeVariableId(varid);
	var v = new Variable(varid, varid, 0, 0);
	this.variables[varid] = v;
	this.registerCanvasItem(v);
	return v;
}

/**
 * Add Node
 */
Template.prototype.addNode = function(comp) {
	var nodeid = this.getFreeNodeId(comp.id);
	var node = new Node(nodeid, comp.id, 0, 0);
	for(var i=0; i<comp.inputs.length; i++) {
		var ip = comp.inputs[i];
		var pid = this.getFreePortId(ip.role);
		var port = new Port(pid, ip.role);
		port.dim = parseInt(ip.dim);
		port.type = ip.param ? 'PARAM' : 'DATA';
		this.ports[pid] = port;
		node.addInputPort(port);
	}
	for(var i=0; i<comp.outputs.length; i++) {
		var op = comp.outputs[i];
		var pid = this.getFreePortId(op.role);
		var port = new Port(pid, op.role);
		port.dim = parseInt(op.dim);
		port.type = op.param ? 'PARAM' : 'DATA';
		this.ports[pid] = port;
		node.addOutputPort(port);
	}
	node.setConcrete(comp.concrete);
	this.nodes[node.id] = node;
	this.registerCanvasItem(node);
	return node;
};

/*
 * Fill out unbound Node IO automatically
 * - i.e. add input/output links to ports that are currently un-attached
 */
Template.prototype.bindUnboundNodeIO = function(node, c) {
	var itypes = {}; var otypes = {};
	var iparams = {};
	var iports = node.getInputPorts();
	var oports = node.getOutputPorts();

	for(var i=0; i<c.inputs.length; i++) {
		if(!c.inputs[i].param) itypes[c.inputs[i].role] = c.inputs[i].types[0];
		else iparams[c.inputs[i].role] = true;
	}
	for(var i=0; i<c.outputs.length; i++) 
		otypes[c.outputs[i].role] = c.outputs[i].types[0];

	for(var i=0; i<iports.length; i++) {
		var ip = iports[i];
		if(!ip.partOfLink) {
			var v = this.addVariable(ip.name);
			//v.setType(ip.type);
			v.setType(iparams[ip.name]?'PARAM':'DATA');
			this.addInputLink(ip, v);
			if(itypes[ip.name]) 
				this.setVariableType(v, itypes[ip.name]);
		}
	}
	for(var i=0; i<oports.length; i++) {
		var op = oports[i];
		if(!op.partOfLink) {
			var v = this.addVariable(op.name);
			v.setType(op.type);
			this.addOutputLink(op, v);
			if(otypes[op.name]) 
				this.setVariableType(v, otypes[op.name]);
		}
	}
}

/**
 * Add Link from Component to a Variable
 * -- markOnly just marks the the side-effects and doesn't actually perform the action
 */
Template.prototype.addLinkToVariable = function(fromPort, toPort, markOnly) {
	if(!markOnly && this.DBG && window.console) window.console.log("-- add Link To Variable");
	// Reorder fromPort/toPort if required
	if(fromPort.isInput) {
		var tmp = fromPort; fromPort = toPort; toPort = tmp;
	}
	// One of the ports needs to be a variable port
	// - Get the variable included in the link, and fromNodePort, or toNodePort
	var variable;
	var fromNodePort; var toNodePort;
	if(fromPort.isVariablePort) {
		variable = fromPort.partOf; 
		var ls = this.getLinksWithVariable(variable);
		if(ls.length) fromNodePort = ls[0].fromPort;
		toNodePort = toPort;
	}
	else if(toPort.isVariablePort) {
		variable = toPort.partOf; 
		var ls = this.getLinksWithVariable(variable);
		toNodePort = new Array();
		for(var i=0; i<ls.length; i++) toNodePort.push(ls[i].toPort);
		fromNodePort = fromPort;
	}
	if(!variable) return false;

	if(toNodePort instanceof Array) {
		for(var i=0; i<toNodePort.length; i++) {
			if(!this.addLink(fromNodePort, toNodePort[i], variable, markOnly)) return false;
		}
	}
	else 
		return this.addLink(fromNodePort, toNodePort, variable, markOnly);
};

/**
 * Add Input Link
 * -- markOnly just marks the the side-effects and doesn't actually perform the action
 */
Template.prototype.addInputLink = function(toNodePort, variable, markOnly) {
	if(!markOnly && this.DBG && window.console) window.console.log("-- add Input Link");
	return this.addLink(null, toNodePort, variable, markOnly);
};

/**
 * Add Output Link
 * -- markOnly just marks the the side-effects and doesn't actually perform the action
 */
Template.prototype.addOutputLink = function(fromNodePort, variable, markOnly) {
	if(!markOnly && this.DBG && window.console) window.console.log("-- add Output Link");
	return this.addLink(fromNodePort, null, variable, markOnly);
};


/**
 * Add a link from component to new component
 * -- markOnly just marks the the side-effects and doesn't actually perform the action
 */
Template.prototype.addLink = function(fromNodePort, toNodePort, variable, markOnly) {
	if(!markOnly && this.DBG && window.console) window.console.log("-- add Link from "+(fromNodePort?fromNodePort.id:null)+" -> "+(toNodePort?toNodePort.id:null)+" ["+variable.id+"]");

	if(!markOnly && !variable) return false;
	// Reorder fromPort/toPort if required
	if(fromNodePort && fromNodePort.isInput) {
		var tmp = fromNodePort; fromNodePort = toNodePort; toNodePort = tmp;
	}

	var v = variable;
	if(!markOnly && v.isPlaceholder) {
		v = new Variable(variable.id, variable.text, variable.x, variable.y);
		if(toNodePort && toNodePort.dim) v.setDimensionality(toNodePort.dim);
		if(fromNodePort && fromNodePort.dim) v.setDimensionality(fromNodePort.dim);
		if(toNodePort) v.setType(toNodePort.type);
		else if(fromNodePort) v.setType(fromNodePort.type);
		this.variables[v.id] = v;
		this.registerCanvasItem(v);
	}
	if(!markOnly) {
		v.setIsInput(!fromNodePort);
		v.setIsOutput(!toNodePort);
	}

	// Exit if if the required link already exists
	if(this.duplicateLinkExists(fromNodePort, toNodePort, v))
		return false;

	this.linkAdditionSideEffects(fromNodePort, toNodePort, v, markOnly);
	if(markOnly) {
		this.removeOrphanVariables(markOnly);
		return true;
	}

	var l = new Link(fromNodePort, toNodePort, v);
	this.links[l.id] = l;
	this.registerCanvasItem(l);
	if(!markOnly && this.DBG && window.console) window.console.log("new link id: "+l.id);

	this.removeOrphanVariables();
	return true;
};

/**
 * Make Link consistency checks
 */
// Check for link duplicates
Template.prototype.duplicateLinkExists = function(fromNodePort, toNodePort, variable) {
	var links = this.getLinksWithVariable(variable);
	for(var i=0; i<links.length; i++) 
		if(links[i].fromPort == fromNodePort && links[i].toPort == toNodePort)
			return true;
	return false;
};

// Side effects of adding a new link
Template.prototype.linkAdditionSideEffects = function(fromNodePort, toNodePort, variable, markOnly) {
	// If a link already exists to the port that we are creating a link to
	// - then remove that link's destination
	// - Caveat: if we are creating a link to the same variable, then dont remove the link's destination
	if(toNodePort) {
		var link = this.getLinkToPort(toNodePort);
		if(link && (!markOnly || link.variable != variable)) this.changeLinkDestination(link, null, markOnly); 
	}

	if(fromNodePort) {
		// If links already exists from the port that we are creating a link from
		// - then remove the link's origin if they don't contain the same variable
		var links = this.getLinksFromPort(fromNodePort);
		for(var i=0; i<links.length; i++) {
			if(links[i].variable != variable) this.changeLinkOrigin(links[i], null, markOnly); 
		}
	}

	if(variable) {
		// If the variable we are including in the link is already present in some links as the output of a node
		// - then change the origin of those links to the origin of the link we are creating
		var links = this.getLinksWithVariable(variable);
		for(var i=0; i<links.length; i++) {
			if(links[i].fromPort && !links[i].toPort && links[i].fromPort != fromNodePort) 
				this.changeLinkOrigin(links[i], fromNodePort, markOnly); 
			if(links[i].fromPort && !links[i].toPort && links[i].fromPort == fromNodePort) 
				this.changeLinkOrigin(links[i], null, markOnly); 
		}
	}
};


//
// Cleanup Function to merge pairs of input and output links containing the same variable to an InOutLink
//
Template.prototype.mergeLinks = function() {
	var inputLinks = {};
	var outputLinks = {};
	var inOutLinks = {};
	for(var lid in this.store.links) {
		var l = this.store.links[lid];
		if(typeof(l) == "function") continue;
		if(!l.fromPort) inputLinks[l.variable] = l;
		else if(!l.toPort) outputLinks[l.variable] = l;
		else inOutLinks[l.variable] = l;
	}
	for(var vid in inputLinks) {
		var il = inputLinks[vid];
		var ol = outputLinks[vid];
		var iol = inOutLinks[vid];
		if(ol) {
			il.fromPort = ol.fromPort;
			il.fromNode = ol.fromNode;
			delete this.store.links[ol.id];
		}
		else if(iol) {
			il.fromPort = iol.fromPort;
			il.fromNode = iol.fromNode;
		}
	}
	for(var vid in outputLinks) {
		var il = inputLinks[vid];
		var ol = outputLinks[vid];
		var iol = inOutLinks[vid];
		if(il) {
			ol.toPort = il.toPort;
			ol.toNode = il.toNode;
			delete this.store.links[il.id];
		}
		else if(iol) {
			ol.toPort = iol.toPort;
			ol.toNode = iol.toNode;
		}
	}
};


/**
 * Remove a Link
 */
// Need to set the partOfLink property of the ports appropriately
Template.prototype.removeLink = function(link, markOnly) {
	if(!markOnly && this.DBG && window.console) window.console.log("removing link: "+link.id);
	if(markOnly) {
		this.addSideEffects(link.id, {op:'remove'});
		return;
	}
	if(link.fromNode) {
		link.fromNode.removeOutputLink(link);
		link.fromPort.partOfLink = null;
	}
	else if(link.toNode) {
		link.toNode.removeInputLink(link);
		link.toPort.partOfLink = null;
	}
	delete this.links[link.id];
	this.deRegisterCanvasItem(link);
};

// Need to set the partOfLink property of the ports appropriately
Template.prototype.changeLinkOrigin = function(link, fromPort, markOnly) {
	if(!markOnly && this.DBG && window.console) window.console.log("removing link origin for: "+link.id);
	if(link.toPort == null) return this.removeLink(link, markOnly);
	if(markOnly) {
		this.addSideEffects(link.id, {op:'changeFromPort', fromPort:fromPort});
		return;
	}
	if(!fromPort) link.variable.setIsInput(true);
	if(link.fromNode) link.fromNode.removeOutputLink(link);
	delete this.links[link.id];
	link.setFromPort(fromPort);
	if(!markOnly && this.DBG && window.console) window.console.log("new link id: "+link.id);
	this.links[link.id] = link;
};

Template.prototype.changeLinkDestination = function(link, toPort, markOnly) {
	if(!markOnly && this.DBG && window.console) window.console.log("removing link destination for: "+link.id);
	if(link.fromPort == null) return this.removeLink(link, markOnly);
	if(markOnly) {
		this.addSideEffects(link.id, {op:'changeToPort', toPort:toPort});
		return;
	}
	if(!markOnly && !toPort) link.variable.setIsOutput(true);
	if(link.toNode) link.toNode.removeInputLink(link);
	delete this.links[link.id];
	link.setToPort(toPort);
	if(!markOnly && this.DBG && window.console) window.console.log("new link id: "+link.id);
	this.links[link.id] = link;
};

/**
 * Remove a Variable
 */
Template.prototype.removeVariable = function(variable) {
	delete this.variables[variable.id];
	this.deRegisterCanvasItem(variable);

	// Also Delete all links containing the variable
	var links = this.getLinksWithVariable(variable);
	for(var i=0; i<links.length; i++) {
		this.removeLink(links[i]);
	}
};


/**
 * Remove a Node
 */
Template.prototype.removeNode = function(node) {
	delete this.nodes[node.id];
	this.deRegisterCanvasItem(node);
	
	// Also Delete all input and output links from/to the node
	var iports = node.getInputPorts();
	for(var i=0; i<iports.length; i++) {
		var l = this.getLinkToPort(iports[i]);
		if(l) this.changeLinkDestination(l, null); 
	}
	var oports = node.getOutputPorts();
	for(var i=0; i<oports.length; i++) {
		var ls = this.getLinksFromPort(oports[i]);
		for(var j=0; j<ls.length; j++) {
			this.changeLinkOrigin(ls[j], null);
		}
	}
	this.removeOrphanVariables();
};

// Remove the variable if it is an orphan (not used by any link)
Template.prototype.removeOrphanVariables = function(markOnly) {
	var variables = {};
	for(var lid in this.links) {
		var l = this.links[lid];
		if(markOnly && this.sideEffects[lid] && this.sideEffects[lid].op == "remove")
			continue;
		variables[l.variable.id] = true;
	}
	for(var vid in this.variables) {
		if(!variables[vid]) {
			if(markOnly) this.addSideEffects(vid, {op:"remove"});
			else this.removeVariable(this.variables[vid]);
		}
	}
};

/**
 * Canvas Operations
 */
Template.prototype.markLinkAdditionSideEffects = function(fromPort, toPort, skolemVariable) {
	this.clearSideEffects();
	this.addLinkInCanvas(fromPort, toPort, skolemVariable, true); 
};

Template.prototype.addLinkInCanvas = function(fromPort, toPort, skolemVariable, markOnly) {
	if(fromPort && toPort) {
		if(!fromPort.isVariablePort  && !toPort.isVariablePort && skolemVariable) 
			this.addLink(fromPort, toPort, skolemVariable, markOnly);
		else
			this.addLinkToVariable(fromPort, toPort, markOnly);
	}
	else if(fromPort && !fromPort.isVariablePort) {
		if(fromPort.isInput) 
			this.addInputLink(fromPort, skolemVariable, markOnly);
		else 
			this.addOutputLink(fromPort, skolemVariable, markOnly);
	}
};

Template.prototype.clearSideEffects = function() {
	this.sideEffects = {};
};

Template.prototype.addSideEffects = function(id, effect) {
	if(!this.sideEffects[id])
		this.sideEffects[id] = {};
	for(key in effect) 
		this.sideEffects[id][key] = effect[key];
};


Template.prototype.markErrors = function() {
	this.clearErrors();
	var ports = {};
	for(var lid in this.links) {
		var l = this.links[lid];
		if(l.toPort) ports[l.toPort.id] = true;
		if(l.fromPort) ports[l.fromPort.id] = true;
	}
	for(var nid in this.nodes) {
		var nports = this.nodes[nid].getPorts();
		for(var i=0; i<nports.length; i++) {
			var port = nports[i];
			if(!ports[port.id]) {
				this.addError(nid, {msg:"Missing Links for port "+port.id+" of node "+nid});
				break;
			}
		}
	}
};

Template.prototype.clearErrors = function() {
	this.errors = {};
};

Template.prototype.addError = function(id, error) {
	if(!this.errors[id])
		this.errors[id] = {};
	for(key in error) 
		this.errors[id][key] = error[key];
};

/**
 * Canvas Item registraion/deRegistration
 */
Template.prototype.registerCanvasItem = function(item) {
	var layerItems = [];
	if(item.getLayerItems) 
		layerItems = item.getLayerItems();
	else if(item.getLayerItem) 
		layerItems.push(item.getLayerItem());

	this.canvasItems = this.canvasItems.concat(layerItems);
	for(var i=0; i<layerItems.length; i++) {
		var layerItem = layerItems[i];
		this.editor.template_layer.addItem(layerItem);
	}
};

Template.prototype.deRegisterCanvasItem = function(item) {
	var layerItems = [];
	if(item.getLayerItems) 
		layerItems = item.getLayerItems();
	else if(item.getLayerItem) 
		layerItems.push(item.getLayerItem());

	if(!layerItems.length) return 0;

	// Assuming that the layerItems are all added together into canvasItems (??)
	var index = this.canvasItems.indexOf(layerItems[0]);
	if(index >=0) this.canvasItems.splice(index, layerItems.length);
	for(var i=0; i<layerItems.length; i++) {
		var layerItem = layerItems[i];
		this.editor.template_layer.removeItem(layerItem.id);
	}
};


/**
 * Forward sweep through the template to setup variable dimensionalities
 */

Template.prototype.forwardSweep = function() {
	var links = [];
	var doneLinks = {};
	for(var lid in this.links) {
		if(!this.links[lid].fromNode) links.push(this.links[lid]);
	}
	while(links.length > 0) {
		var l = links.pop();
		if(doneLinks[l.id]) continue;
		doneLinks[l.id] = true;

		l.variable.setIsInput(!l.fromNode);
		l.variable.setIsOutput(!l.toNode);

		var n = l.toNode;
		if(!n) continue;

		var extraDim = 0;
		var ok = true;
		for(var i=0; i<n.inputLinks.length; i++) {
			var il = n.inputLinks[i];
			if(!doneLinks[il.id]) { ok = false; break; }
			var tmpDim = il.variable.dim - il.toPort.dim;
			if(extraDim < tmpDim) extraDim = tmpDim;
		}
		if(!ok) {
			links.push(l);
			continue;
		}

		n.dim = 0;
		for(var i=0; i<n.outputLinks.length; i++) {
			var ol = n.outputLinks[i];
			if(n.prule == 'S') {
				ol.variable.setDimensionality(ol.fromPort.dim + extraDim);
				n.dim = ol.fromPort.dim + extraDim;
			}
			else {
				ol.variable.setDimensionality(ol.fromPort.dim);
				//n.dim = ol.fromPort.dim;
			}
			links.push(ol);
		}
	}
};


Template.prototype.getVariablePropertyValue = function(v, prop) {
	for(var i=0; i<this.store.constraints.length; i++) {
		var cons = this.store.constraints[i];
		if(cons.s == v.id && cons.p == prop) {
			return cons.o;
		}
	}
};
Template.prototype.setVariablePropertyValue = function(v, prop, value) {
	var updated = false;
	for(var i=0; i<this.store.constraints.length; i++) {
		var cons = this.store.constraints[i];
		if(cons.s == v.id && cons.p == prop) {
			cons.o = value;
			updated = true;
		}
		this.store.constraints[i] = cons;
	}
	if(!updated) this.store.constraints.push({s:v.id, p:prop, o:value});
};


Template.prototype.getVariableType = function(v) {
	var type = this.getVariablePropertyValue(v, 'rdf:type');
	if(type) type = type.replace(/^dcdom:/,'');
	return type;
};
Template.prototype.setVariableType = function(v, value) {
	var pfx = ( value=='DataObject' ? 'dc:' : 'dcdom:' );
	this.setVariablePropertyValue(v, 'rdf:type', pfx+value);
};


function cloneObj(o) {
	return Ext.clone(o);
	//return jQuery.extend(true, {}, o);
}

Template.prototype.createCopy = function() {
	this.saveToStore(true);
	var store = cloneObj(this.store);
	return new Template(this.id, store, this.editor);
};

Template.prototype.equals = function(t) {
	if(!t) return false;
	if(this.isSubsetOf(t) && t.isSubsetOf(this))
		return true;
	return false;
};

Template.prototype.isSubsetOf = function(t) {
	if(!t) return false;
	if(!t.store) return false;
	if(!this.checkLinkSubset(this.links, t.links)) 
		return false;
	if(!this.checkConstraintsSubset(this.store.constraints, t.store.constraints))
		return false;
	return true;
};

Template.prototype.checkLinkSubset = function(links1, links2) {
	for(var lid in links1) {
		var link1 = links1[lid];
		var link2 = links2[lid];
		if(!link2) return false;
		if(!link1.equals(link2)) return false;
	}
	return true;
};

Template.prototype.checkConstraintsSubset = function(constraints1, constraints2) {
	for(var i=0; i<constraints1.length; i++) {
		var cons1 = constraints1[i];
		var found = false;
		for(var j=0; j<constraints2.length; j++) {
			var cons2 = constraints2[j];
			if(cons1.s == cons2.s && 
				cons1.p == cons2.p && 
				cons1.o == cons2.o) {
				found = true;
				break;
			}
		}
		if(!found) return false;
	}
	return true;
};

