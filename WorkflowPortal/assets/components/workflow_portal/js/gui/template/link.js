/*
 * Generic Link Class
 */

var Link = function(fromPort, toPort, variable) {
	this.fromNode = null;
	this.fromPort = null;
	this.toNode = null;
	this.toPort = null;;
	this.setFromPort(fromPort);
	this.setToPort(toPort);
	this.variable = variable;
	this.color = 'rgba(50,50,50,0.9)';
	this.hideLinkColor = 'rgba(0,0,0,0.2)';
	this.layerItem = this.createLayerItem(this.id);
};

Link.prototype.arrowSize = 8;

Link.prototype.equals = function(l) {
	if(!l) return false;

	if(!this.fromNode && l.fromNode) return false;
	if(!this.toNode && l.toNode) return false;
	if(!this.fromPort && l.fromPort) return false;
	if(!this.toPort && l.toPort) return false;
	if(!this.variable && l.variable) return false;

	if(this.fromNode && !this.fromNode.equals(l.fromNode)) return false;
	if(this.toNode && !this.toNode.equals(l.toNode)) return false;
	if(this.fromPort && !this.fromPort.equals(l.fromPort)) return false;
	if(this.toPort && !this.toPort.equals(l.toPort)) return false;
	if(this.variable && !this.variable.equals(l.variable)) return false;

	return true;
};

Link.prototype.getLayerItem = function() {
	return this.layerItem;
};

Link.prototype.setFromPort = function(fromPort) {
	// Alter existing fromPort (if any)
	if(this.fromPort) this.fromNode.removeOutputLink(this);

	this.fromNode = fromPort?fromPort.partOf:null;
	this.fromPort = fromPort;
	if(this.fromPort) this.fromPort.partOfLink = this;
	if(this.fromNode) this.fromNode.addOutputLink(this);

	this.id = (this.fromNode?this.fromPort.id:'ip')+'_'+(this.toNode?this.toPort.id:'op');
};

Link.prototype.setToPort = function(toPort) {
	// Alter existing toPort (if any)
	if(this.toPort) this.toNode.removeInputLink(this);

	this.toNode = toPort?toPort.partOf:null;
	this.toPort = toPort;
	if(this.toPort) this.toPort.partOfLink = this;
	if(this.toNode) this.toNode.addInputLink(this);

	this.id = (this.fromPort?this.fromPort.id:'ip')+'_'+(this.toPort?this.toPort.id:'op');
};

Link.prototype.createLayerItem = function(id) {
	var link = this;
	return {
		id : id,
		link : link,
		clear : function(ctx) {
			// How to clear a link ? 
			// How to figure out intersecting objects underneath
		},
		refresh : function(ctx) {
			this.clear(ctx);
			this.draw(ctx);
		},
		// ctx or context, is passed to the item by the layer
		draw : function(ctx) {
			//ctx.save();
			var canvas = this.getLayer().parent;
			var lyr = canvas.LayerManager.findLayer(this.getLayer().id);
			ctx.lineWidth = 1;
			this.link.draw(ctx, canvas.template.sideEffects[this.link.id], lyr.items);
			//ctx.restore();
		},
		on : {
		}
	};
};

// Link drawing should only happen after the node has been drawn and its ports have been updated !!!
Link.prototype.draw = function(ctx, sideEffects, items) {
	var color = this.color;
	if(sideEffects && sideEffects.op == "remove") this.color = this.hideLinkColor;
	if(this.fromPort) {
		if(sideEffects && sideEffects.op == "changeFromPort") {
			this.color = this.hideLinkColor;
		}
		// Draw Path from fromNode/fromOutputPort -> Variable/inputPort
		this.drawPartialLink(ctx, this.fromPort, this.variable.getInputPort(), false, true, items);
		this.color = color;
	}
	if(this.toPort) {
		if(sideEffects && sideEffects.op == "changeToPort") {
			this.color = this.hideLinkColor;
		}
		this.drawPartialLink(ctx, this.variable.getOutputPort(), this.toPort, false, true, items);
		this.color = color;
	}
	this.color = color;
	//ctx.closePath();
};

Link.prototype.drawPartialLink = function(ctx, port1, port2, inverted, straight, items) {
	if(inverted) {
		var tmp = port1;
		port1 = port2;
		port2 = tmp;
	}
	x1 = port1.x;
	y1 = port1.y + Port.prototype.height + 2;
	x2 = port2.x;
	y2 = port2.y - Port.prototype.height - 3;
	ctx.strokeStyle = ctx.fillStyle = this.color?this.color:"rgba(0,0,0,1)";

	var points = this.getLineSegments(new Point(x1,y1),new Point(x2,y2),items);
	ctx.lineWidth = 1;

	/*var i=0;
	var p0 = points[i++];
	while(i < points.length) {
		var p1 = points[i++];
		var p2 = points[i++];
		if(p0 && p1 && p2) {
			var a = this.getAnchors(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y);
			this.drawBezierArrow(ctx, p0.x, p0.y, a.x1, a.y1, a.x2, a.y2, p2.x, p2.y, (i>=points.length));
		}
		else if(p0 && p1) {
			this.drawArrow(ctx, p0.x, p0.y, p1.x, p1.y, (i>=points.length));
		}
		p0 = p2;
	}*/

	var i=0;
	var p0 = points[i++];
	while(i < points.length) {
		var p1 = points[i++];
		var p2 = points[i++];
		var p3 = points[i++];
		if(p0 && p1 && p2 && p3) {
			this.drawBezierArrow(ctx, p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, (i>=points.length));
		}
		else if(p0 && p1 && p2) {
			this.drawQuadraticArrow(ctx, p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, (i>=points.length));
		}
		else if(p0 && p1) {
			this.drawArrow(ctx, p0.x, p0.y, p1.x, p1.y, (i>=points.length));
		}
		p0 = p3;
	}
	//ctx.stroke();
	/*if(straight) {
		this.drawArrow(ctx, x1, y1, x2, y2, !inverted, inverted);
	}
	else {
		//this.drawQuadraticArrow(ctx, x1, y1, x1+(x2-x1)/2, y1+(y2-y1)/3, x2, y2, !inverted, inverted);
		this.drawBezierArrow(ctx, x1, y1, x1, y1+(y2-y1)/2.0, x1+(x2-x1)/2.0, y1, x2, y2, !inverted, inverted);
	}*/
	
};

Link.prototype.drawQuadraticArrow = function(ctx, x0, y0, cpx, cpy, x1, y1, head, tail, an) {
	var len = this.arrowSize*ctx.lineWidth;
	var an = an?an:0.4;
	
	var dh = Math.sqrt ( (x1-cpx)*(x1-cpx) + (y1-cpy)*(y1-cpy) );
	var dt = Math.sqrt ( (x0-cpx)*(x0-cpx) + (y0-cpy)*(y0-cpy) );
	
	var ah = Math.acos((x1-cpx)/dh); if (y1<cpy) ah = -ah;
	var at = Math.acos((x0-cpx)/dt); if (y0<cpy) at = -at;

	var x1a = x1 - len*Math.cos(ah+an);
	var y1a = y1 - len*Math.sin(ah+an);
	var x1b = x1 - len*Math.cos(ah-an);
	var y1b = y1 - len*Math.sin(ah-an);
	var x0a = x0 - len*Math.cos(at+an);
	var y0a = y0 - len*Math.sin(at+an);
	var x0b = x0 - len*Math.cos(at-an);
	var y0b = y0 - len*Math.sin(at-an);

	var cx0 = tail ? x0a + (x0b - x0a)/2 : x0;
	var cy0 = tail ? y0a + (y0b - y0a)/2 : y0;
	var cx1 = head ? x1a + (x1b - x1a)/2 : x1;
	var cy1 = head ? y1a + (y1b - y1a)/2 : y1;

	ctx.beginPath();
	ctx.moveTo(cx0,cy0);
	ctx.quadraticCurveTo(cpx,cpy,cx1,cy1);
	ctx.stroke();

	if(head) {
		ctx.beginPath();
		ctx.moveTo(x1,y1);
		ctx.lineTo(x1a,y1a);
		ctx.lineTo(x1b,y1b);
		ctx.closePath();
		ctx.fill();
	}
	if (tail) {
		ctx.beginPath();
		ctx.moveTo(x0,y0);
		ctx.lineTo(x0a,y0a);
		ctx.lineTo(x0b,y0b);
		ctx.closePath();
		ctx.fill();
	}
};


Link.prototype.drawBezierArrow = function(ctx, x0, y0, cp1x, cp1y, cp2x, cp2y, x1, y1, head, tail, an) {
	var len = this.arrowSize*ctx.lineWidth;
	var an = an?an:0.4;
	
	var dh = Math.sqrt ( (x1-cp2x)*(x1-cp2x) + (y1-cp2y)*(y1-cp2y) );
	var dt = Math.sqrt ( (x0-cp1x)*(x0-cp1x) + (y0-cp1y)*(y0-cp1y) );
	
	var ah = Math.acos((x1-cp2x)/dh); if (y1<cp2y) ah = -ah;
	var at = Math.acos((x0-cp1x)/dt); if (y0<cp1y) at = -at;

	var x1a = x1 - len*Math.cos(ah+an);
	var y1a = y1 - len*Math.sin(ah+an);
	var x1b = x1 - len*Math.cos(ah-an);
	var y1b = y1 - len*Math.sin(ah-an);
	var x0a = x0 - len*Math.cos(at+an);
	var y0a = y0 - len*Math.sin(at+an);
	var x0b = x0 - len*Math.cos(at-an);
	var y0b = y0 - len*Math.sin(at-an);

	var cx0 = tail ? x0a + (x0b - x0a)/2 : x0;
	var cy0 = tail ? y0a + (y0b - y0a)/2 : y0;
	var cx1 = head ? x1a + (x1b - x1a)/2 : x1;
	var cy1 = head ? y1a + (y1b - y1a)/2 : y1;

	ctx.beginPath();
	ctx.moveTo(cx0,cy0);
	ctx.bezierCurveTo(cp1x,cp1y,cp2x,cp2y,cx1,cy1);
	ctx.stroke();

	if(head) {
		ctx.beginPath();
		ctx.moveTo(x1,y1);
		ctx.lineTo(x1a,y1a);
		ctx.lineTo(x1b,y1b);
		ctx.closePath();
		ctx.fill();
	}
	if (tail) {
		ctx.beginPath();
		ctx.moveTo(x0,y0);
		ctx.lineTo(x0a,y0a);
		ctx.lineTo(x0b,y0b);
		ctx.closePath();
		ctx.fill();
	}
};


Link.prototype.drawArrow = function(ctx, x0, y0, x1, y1, head, tail, an) {
	var len = this.arrowSize*ctx.lineWidth;
	var an = an?an:0.4;
	
	var d = Math.sqrt ( (x1-x0)*(x1-x0) + (y1-y0)*(y1-y0) );
	var ah = Math.acos((x1-x0)/d); if (y1<y0) ah = -ah;
	var at = ah + Math.PI;

	var x1a = x1 - len*Math.cos(ah+an);
	var y1a = y1 - len*Math.sin(ah+an);
	var x1b = x1 - len*Math.cos(ah-an);
	var y1b = y1 - len*Math.sin(ah-an);
	var x0a = x0 - len*Math.cos(at+an);
	var y0a = y0 - len*Math.sin(at+an);
	var x0b = x0 - len*Math.cos(at-an);
	var y0b = y0 - len*Math.sin(at-an);

	var cx0 = tail ? x0a + (x0b - x0a)/2 : x0;
	var cy0 = tail ? y0a + (y0b - y0a)/2 : y0;
	var cx1 = head ? x1a + (x1b - x1a)/2 : x1;
	var cy1 = head ? y1a + (y1b - y1a)/2 : y1;

	ctx.beginPath();
	ctx.moveTo(cx0,cy0);
	ctx.lineTo(cx1,cy1);
	ctx.stroke();
	ctx.closePath();

	if(head) {
		ctx.beginPath();
		ctx.moveTo(x1,y1);
		ctx.lineTo(x1a,y1a);
		ctx.lineTo(x1b,y1b);
		ctx.closePath();
		ctx.stroke();
		ctx.fill();
	}
	if (tail) {
		ctx.beginPath();
		ctx.moveTo(x0,y0);
		ctx.lineTo(x0a,y0a);
		ctx.lineTo(x0b,y0b);
		ctx.closePath();
		ctx.stroke();
		ctx.fill();
	}
};

Link.prototype.getLineSegments = function(pstart,pend,items) {
	if(!items) { return [pstart, pend]; }

	var npoints = [];
	for(var i=0; i<items.length; i++) {
		var n = items[i];
		if(!n.width || !n.height || !n.shape) continue;
		var intersection = this.lineIntersects(pstart.x,pstart.y,pend.x,pend.y,n.x,n.y,n.x+n.width,n.y+n.height);
		if(intersection) {
			var ix = intersection[0];
			var iy = intersection[1];
			var nx = n.x - 10;
			if(ix - n.x > n.x + n.width - ix) 
				nx = n.x + n.width + 10;
			npoints.push(new Point(nx, n.y));
			npoints.push(new Point(nx, n.y+n.height));
			//console.log(this.id+" intersection "+n.shape.id);
		}
	}

	if(npoints.length > 0) {
		var points = [];
		points.push(pstart);
		npoints.sort(function(a,b){return a.y-b.y;});
		if(pstart.y > pend.y) npoints.reverse();
		for(var i=0; i<npoints.length; i++) points.push(npoints[i]);
		points.push(pend);
		return points;
	}
	return [pstart, pend];
};


Link.prototype.lineIntersects = function(x1, y1, x2, y2, xmin, ymin, xmax, ymax) {
   var u1 = 0.0;
	var u2 = 1.0;
   var r;

   var deltaX = (x2 - x1);
   var deltaY = (y2 - y1);

   /*
    * left edge, right edge, bottom edge and top edge checking
    */
   var pPart = [-1 * deltaX, deltaX, -1 * deltaY, deltaY];
   var qPart = [x1 - xmin, xmax - x1, y1 - ymin, ymax - y1];

   var accept = true;

   for( var i = 0; i < 4; i ++ ) {
       p = pPart[ i ];
       q = qPart[ i ];

       if( p == 0 && q < 0 ) {
           accept = false;
           break;
       }

       r = q / p;

       if( p < 0 ) u1 =Math.max(u1, r);
       if( p > 0 ) u2 = Math.min(u2, r);

       if( u1 > u2 ) {
           accept = false;
           break;
       }
   }

   if(accept) {
       if( u2 < 1 ) {
           x2 = parseInt(x1 + u2 * deltaX);
           y2 = parseInt(y1 + u2 * deltaY);
       }
       if( u1 > 0) {
           x1 = parseInt(x1 + u1 * deltaX);
           y1 = parseInt(y1 + u1 * deltaY);
       }
       return[x1, y1, x2, y2];
   }
   else {
       return null;
   }
};

Link.prototype.getAnchors = function (p1x, p1y, p2x, p2y, p3x, p3y) {
	var l1 = (p2x - p1x) / 2,
		l2 = (p3x - p2x) / 2,
		a = Math.atan((p2x - p1x) / Math.abs(p2y - p1y)),
		b = Math.atan((p3x - p2x) / Math.abs(p2y - p3y));
	a = p1y < p2y ? Math.PI - a : a;
	b = p3y < p2y ? Math.PI - b : b;
	var alpha = Math.PI / 2 - ((a + b) % (Math.PI * 2)) / 2,
		dx1 = l1 * Math.sin(alpha + a),
		dy1 = l1 * Math.cos(alpha + a),
		dx2 = l2 * Math.sin(alpha + b),
		dy2 = l2 * Math.cos(alpha + b);
	return {
		x1: p2x - dx1,
		y1: p2y + dy1,
		x2: p2x + dx2,
		y2: p2y + dy2
	};
};


var Point = function(x, y) {
	this.x = x;
	this.y = y;
};


var Line = function(start, end) {
	this.start = start;
	this.end = end;
};


/**
 * Helper Classes to simplify link creation
 */

var InputLink = function(toPort, variable) {
	return new Link(null, toPort, variable);
};

var OutputLink = function(fromPort, variable) {
	return new Link(fromPort, null, variable);
};



