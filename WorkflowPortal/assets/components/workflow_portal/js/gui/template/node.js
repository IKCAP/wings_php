/*
 * Generic Node Class
 */

var Node = function(id, component, x, y) {
	this.component = component;
	this.initialize(id, component, x, y, 1, 1);
	this.setBackgroundColor("rgba(255,204,153,1)");
	this.setTextColor("rgba(72,42,3,1)");
	this.inputLinks = new Array();
	this.outputLinks = new Array();
	this.crule = "W";
	this.prule = "W";
	this.pruleOp = "cross";
	this.isConcrete = true;
	this.binding = null;
	//this.font = "Bold 17px Calibri, Sans";
	//this.font = "bold 16px Optimer";
	this.font = "bold 16px tahoma";
	this.dim = 0;
};
Node.prototype = new Shape();

Node.prototype.equals = function(n) {
	if(!n) return false;
	//if(this.id ! n.id) return false;
	if(this.component != n.component) return false;
	if(this.prule != n.prule) return false;
	if(this.crule != n.crule) return false;
	if(this.pruleOp != n.pruleOp) return false;
	return true;
};

Node.prototype.getDimensions = function(ctx, text) {
	var dims = Shape.prototype.getDimensions.call(this, ctx, text);
	
	var psize = Port.prototype.size;
	var pspacing = Port.prototype.spacing;
	var ipwidth = pspacing + (psize+pspacing)*this.inputPorts.length;
	var opwidth = pspacing + (psize+pspacing)*this.outputPorts.length;
	
	dims.width = dims.width > ipwidth? dims.width : ipwidth;
	dims.width = dims.width > opwidth? dims.width : opwidth;
	return dims;
};

Node.prototype.setComponentRule = function(crule) {
	this.crule = crule;
	if(this.crule == "S") this.text = "{"+this.component+"}";
	else this.text = this.component;
};

Node.prototype.setPortRule = function(prule, pruleOp) {
	this.prule = prule;
	this.pruleOp = pruleOp;
	//if(this.prule == 'S') this.dim = 1;
	//else this.dim = 0;
};

Node.prototype.setConcrete = function(isConcrete) {
	this.isConcrete = isConcrete;
	if(!this.isConcrete) {
		this.color = "rgba(204,204,204,1)";
		this.setBackgroundColor(this.color);
		var ports = this.getPorts();
		for(var i=0; i<ports.length; i++) ports[i].color = this.color;
	}
	else {
		this.color = "rgba(255,204,153,1)";
		this.setBackgroundColor(this.color);
		var ports = this.getPorts();
		for(var i=0; i<ports.length; i++) ports[i].color = this.color;
	}
};

Node.prototype.setBinding = function(binding) {
	if(binding) {
		this.setConcrete(true);
		this.dim = this.getBindingDimensionality(binding);
		this.text = binding+'';
		this.binding = binding;
	}
};

Node.prototype.getBindingDimensionality = function(binding) {
	if(typeof(binding) == "string") return 0;
	var max = 0;
	for(var i=0; i<binding.length; i++) {
		var m = this.getBindingDimensionality(binding[i]) + 1;
		if(m > max) max = m;
	}
	if(binding.length == 1) return max-1;
	return max;
};

Node.prototype.drawShape = function(ctx, x, y, width, height, highlight) {
	//ctx.save();
	ctx.fillStyle = highlight?this.highlightColor:this.getBackgroundColor();
	ctx.strokeStyle = this.getForegroundColor();
	if(this.dim) {
		var tmpstyle = ctx.fillStyle;
		var len = 2;
		this.enableShadow(ctx);
		ctx.lineWidth = 3;
		for(var i=len; i>=1; i--) {
			ctx.beginPath();
			this.drawPath(ctx,x+i*4,y+i*4,width,height);
			ctx.closePath();
			//ctx.fillStyle = "rgba(255,255,255,0.7)";
			ctx.stroke();
			ctx.fill();
			this.disableShadow(ctx);
		}
		ctx.fillStyle = tmpstyle;
	}
	else {
		ctx.lineWidth = 3;
		this.enableShadow(ctx);
	}
	ctx.beginPath();
	this.drawPath(ctx,x,y,width,height);
	ctx.closePath();
	ctx.stroke();
	ctx.fill();
	//ctx.restore();
};

Node.prototype.drawPath = function(ctx, x, y, width, height) {
	ctx.beginPath();
	ctx.rect(x, y, width, height);
	/*ctx.moveTo(x, y);
	ctx.lineTo(x, y+height);
	ctx.lineTo(x+width, y+height);
	ctx.lineTo(x+width, y);
	ctx.lineTo(x,y);*/
	ctx.closePath();
};

Node.prototype.addInputLink = function(link) {
	this.inputLinks.push(link);
};

Node.prototype.addOutputLink = function(link) {
	this.outputLinks.push(link);
};

Node.prototype.removeInputLink = function(link) {
	var index = this.inputLinks.indexOf(link);
	if(index >= 0)
		return this.inputLinks.splice(index,1);
};

Node.prototype.removeOutputLink = function(link) {
	var index = this.outputLinks.indexOf(link);
	if(index >= 0)
		return this.outputLinks.splice(index,1);
};

Node.prototype.getInputLinks = function() {
	return this.inputLinks;
};

Node.prototype.getOutputLinks = function() {
	return this.outputLinks;
};

Node.prototype.getLinks = function() {
	return this.inputLinks.concat(this.outputLinks);
};

