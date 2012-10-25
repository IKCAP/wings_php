/*
 * Generic Variable Class
 */

var Variable = function(id, text, x, y, type) {
	this.shadow = false;
	this.initialize(id, text, x, y, 1.1, 1.1);
	this.addInputPort(new Port(this.id+'_ip', text+'_ip', this.color, true));
	this.addOutputPort(new Port(this.id+'_op', text+'_op', this.color, true));	

	this.hideBGColor = "rgba(204,224,224,0.4)";
	this.setType(type);
	this.dim = 0;
	this.unknown = false;
	this.isInput = false;
	this.isOutput = false;
	this.autofill = false;
	//this.font = "Bold 14px Tahoma, Arial";
	//this.font = "bold 14px Optimer";
	this.font = "bold 14px tahoma";
};
Variable.prototype = new Shape();
Variable.prototype.KAPPA =  1.4* (Math.sqrt(2) - 1);

Variable.prototype.equals = function(v) {
	if(!v) return false;
	if(this.id != v.id) return false;
	if(this.type != v.type) return false;
	if(this.dim != v.dim) return false;
	return true;
};

Variable.prototype.getInputPort = function() {
	return this.inputPorts[0];
};

Variable.prototype.getOutputPort = function() {
	return this.outputPorts[0];
};

Variable.prototype.setDefaultColors = function() {
	var alpha = 1;
	if(this.autofill)
		alpha = 0.4;

	if(this.unknown) {
		this.color = "rgba(128,5,22,"+alpha+")";
		this.setTextColor("rgba(255,230,230,"+alpha+")");
	}
	else if(this.type == "PARAM") {
		this.color = "rgba(232,164,23,"+alpha+")";
		this.setTextColor("rgba(52,14,3,"+alpha+")");
	}
	else {
		if(this.isInput) {
			this.color = "rgba(51,102,51,"+alpha+")";
			this.setTextColor("rgba(220,255,220,"+alpha+")");
		}
		else {
			this.color = "rgba(0,51,102,"+alpha+")";
			this.setTextColor("rgba(220,220,255,"+alpha+")");
		}
	}
	this.setBackgroundColor(this.color);
	this.getInputPort().color = this.color;
	this.getOutputPort().color = this.color;
};

Variable.prototype.setDimensionality = function(dim) {
	this.dim = dim;
	this.setDefaultColors();
};

Variable.prototype.setIsInput = function(isInput) {
	this.isInput = isInput;
	this.setDefaultColors();
};

Variable.prototype.setIsOutput = function(isOutput) {
	this.isOutput = isOutput;
	this.setDefaultColors();
};

Variable.prototype.setIsUnknown = function(unknown) {
	this.unknown = unknown;
	this.setDefaultColors();
};

Variable.prototype.setAutoFill = function(autofill) {
	this.autofill = autofill;
	this.setDefaultColors();
};

Variable.prototype.setType = function(type) {
	this.type = type;
	this.getInputPort().type = type;
	this.getOutputPort().type = type;
	this.setDefaultColors();
};

Variable.prototype.getDimensionality = function() {
	return this.dim;
};

Variable.prototype.drawShape = function(ctx, x, y, width, height, highlight) {
	//ctx.save();
	ctx.fillStyle = highlight?this.highlightColor:this.getBackgroundColor();
	ctx.strokeStyle = this.getForegroundColor();
	if(this.dim) {
		var tmpstyle = ctx.fillStyle;
		var len = parseInt(this.dim)+1;
		this.enableShadow(ctx);
		ctx.lineWidth = 3;
		for(var i=len; i>=1; i--) {
			ctx.beginPath();
			this.drawEllipse(ctx,x+i*4,y+i*4,width,height);
			ctx.closePath();
			ctx.fillStyle = "rgba(255,255,255,0.7)";
			if(!highlight) ctx.strokeStyle = tmpstyle;
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
	this.drawEllipse(ctx,x,y,width,height);
	ctx.closePath();
	ctx.stroke();
	ctx.fill();
	//ctx.restore();
};

Variable.prototype.drawEllipse = function(ctx, x, y, width, height) {
	//return ctx.rect(x,y,width,height);
	x1 = x;
	x2 = x + width;
	y1 = y;
	y2 = y + height;
	var rx = (x2 - x1) / 2;
	var ry = (y2 - y1) / 2;
	var cx = x1 + rx;
	var cy = y1 + ry;
	
	ctx.moveTo(cx, cy - ry);
	ctx.bezierCurveTo(cx + (this.KAPPA * rx), cy - ry, cx + rx, cy - (this.KAPPA * ry), cx + rx, cy);
	ctx.bezierCurveTo(cx + rx, cy + (this.KAPPA * ry), cx + (this.KAPPA * rx), cy + ry, cx, cy + ry);
	ctx.bezierCurveTo(cx - (this.KAPPA * rx), cy + ry, cx - rx, cy + (this.KAPPA * ry), cx - rx, cy);
	ctx.bezierCurveTo(cx - rx, cy - (this.KAPPA * ry), cx - (this.KAPPA * rx), cy - ry, cx, cy - ry);
};

/**
 * Helper Classes to simplify variable creation
 */

var DraggerVariable = function(id, text) {
	var v = new Variable(id, text, 0, 0);
	v.shadow = false;
	v.isPlaceholder = true;
	v.setTextColor("rgba(255,255,255,1)");
	v.setForegroundColor("rgba(255,255,255,1)");
	v.setBackgroundColor("rgba(0,0,0,0.4)");
	return v;
};
