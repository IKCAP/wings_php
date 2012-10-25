/**
 * <p>TemplateGraphPanel is an extension of the default Panel. It allows creation of a Template
 * 
 * @author <a href="mailto:varunratnakar@google.com">Varun Ratnakar</a>
 * @class Ext.ux.TemplateGraphPanel
 * @extends Ext.Panel
 * @constructor
 * @component
 * @version 1.0
 * @license GPL
 * 
 */

Ext.Ajax.timeout = 120000;

Ext.namespace("Ext.ux");
Ext.ux.TemplateGraphPanel = Ext.extend(Ext.Panel, { 
	zoomFactor: 1.2,
	editor: null,
	editable: false,

	initComponent: function(config){
		Ext.apply(this, config);
		this.editor = new Ext.ux.TemplateGraph({
			guid:this.guid, 
			store:this.store, 
			cmap:this.cmap,
			url:this.url, 
			template_id:this.template_id, 
			editable: this.editable, 
			editor_mode: this.editor_mode, 
			infoPanel: this.infoPanel,
			gridPanel: this.gridPanel,
			tellMePanel: this.templatePanel.tellMePanel 
		});
		var me = this;

		// Create the toolbar
		var tbar = [];
		if(this.editable) {
			tbar.push({ 
				text:'Add Component',
				iconCls:'addIcon',
				handler:function() {Ext.MessageBox.show({
					title:'Add Component',
					modal:false,
					msg:'To add a component, drag a component from the <b>"Components Tab"</b> on the left to the Graph below',
					icon:Ext.MessageBox.INFO, 
					buttons:Ext.MessageBox.OK
				})}
			});
			tbar.push('-');
			tbar.push({
				text:'Delete Selected',
				iconCls:'delIcon',
				handler:function() {},
				handler:function() {me.editor.deleteSelected()}
			});
		}
		tbar.push({xtype:'tbfill'});
		if(this.editable) {
			if(this.editor_mode == 'tellme') {
				/*tbar.push({text:'TOP', iconCls:'TOPIcon',
					handler:function() {me.editor.findErrors();}
				});*/
			}
		}
		tbar.push({text:'Layout', iconCls:'layoutIcon',
			handler:function() {me.editor.layout();}
		});
		tbar.push({text:'Zoom In', iconCls:'zoomInIcon',
			handler:function() {me.editor.zoom(1.2)}
		});
		tbar.push({text:'Zoom Out',iconCls:'zoomOutIcon',
			handler:function() {me.editor.zoom(1/1.2)}
		});
		if(!Ext.isIE) {
			tbar.push('-');
			tbar.push({text:'Grab Image',iconCls:'snapshotIcon',
				handler:function() {me.editor.saveImage()}
			});
		}

		// Apply toolbar and other styles to the panel
		Ext.applyIf(this, {
			autoScroll: true,
			layout: 'fit',
			//bodyStyle: 'padding:0 10px 10px 0',
			//padding:5,
			bodyCls: (me.editable?'':'disabledCanvas'),
			//monitorResize: true,
			tbar: tbar
		});
		this.items = [this.editor];
		Ext.ux.TemplateGraphPanel.superclass.initComponent.apply(this, arguments);
	},

	reloadGraph: function(store) {
		this.editor.initTemplate(this.template_id, store);
		this.editor.redrawCanvas();
	}
});

Ext.ux.TemplateGraph = Ext.extend(Ext.Component, {
	tb : new Ext.Toolbar(),
	template_layer : null,
	canvas : null,
	canvasDom : null,
	graphPadding : 20,
	template_id : null,
	template: null,
	editable: false,
	manualZoom: false,
	manualMove: false,
	gridSize: 10,
	snapSize: 10,
	snapToGrid: false,
	showGrid: true,
	gridColor: "rgba(230,230,230,1)",
	selBoxColor: "rgba(0,0,0,0.2)",
	selBoxBorderColor: "rgba(0,0,0,0.2)",

	initComponent: function(config){
		Ext.apply(this, config);
		Ext.apply(this, {xtype: "box"});
		this.on('resize', this.onResize, this);
		Ext.ux.TemplateGraph.superclass.initComponent.apply(this, arguments);
	},

	onResize: function(obj, w, h) {
		//console.log('resizing to '+w+","+h);
		Ext.ux.TemplateGraph.superclass.onResize.apply(this, arguments);
		this.panelWidth = w;
		this.panelHeight = h;
		//this.canvas.resetPosition();
		if(!this.template_layer || !this.template_layer.selectedItems.length) {
			var w = this.manualZoom?null:this.panelWidth;
			var h = this.manualZoom?null:this.panelHeight;
			this.redrawCanvas(w,h);
		}
	},

	onRender: function(ct, position) {
		Ext.ux.TemplateGraph.superclass.onRender.call(this, ct, position);
		this.initCanvas();
		this.initTemplate(this.template_id, this.store);
		this.initDropZone(this.canvasDom, this.guid+"_ComponentTree");
	},

	initCanvas : function() {
		this.canvasDom = document.createElement('canvas');
		this.canvasDom.id = '__canvas_'+this.guid+'_'+this.template_id;
		this.el.dom.appendChild(this.canvasDom);
		this.canvas = new Canvas(this.canvasDom);
	},

	initTemplate : function(template_id, store) {
		this.store = store;
		this.template_id = template_id;

		this.template = new Template(this.template_id, this.store, this);
		this.canvas.template = this.template;

		this.initTemplateLayer();
		this.initLayerItems();

		this.template_layer.draw();
	},

	refreshConstraints : function() {
		this.gridPanel.getStore().loadData(cloneObj(this.template.store.constraints));
	},
    
	initTemplateLayer : function() {
		var editor = this;
		if(this.canvas && this.template_layer) 
			this.canvas.LayerManager.removeLayer(this.template_layer.getID());

		this.template_layer = new Canvas.Layer({
			id: "template_"+editor.template_id,
			x:0,
			y:0,
			selectedItems:[],
			oldx:-1,
			oldy:-1,
			on: {
				"mousemove" : function (event, type, ctx, item) {
					var mX = event.mouseX, mY = event.mouseY;
					if(editor.snapToGrid) {
						mX = Math.floor(mX/editor.snapSize)*editor.snapSize;
						mY = Math.floor(mY/editor.snapSize)*editor.snapSize;
					}
					if(mX == this.oldx && mY == this.oldy) return;
					this.oldx = mX;
					this.oldy = mY;

					if(this.dragging) {
						for(var i=0; i<this.selectedItems.length; i++) {
							var item = this.selectedItems[i];
							item.x = parseInt(mX - this.dragging[i].x);
							item.y = parseInt(mY - this.dragging[i].y);
							if(item.x < editor.graphPadding) item.x = editor.graphPadding;
							if(item.y < editor.graphPadding) item.y = editor.graphPadding;
						}
						editor.canvasDom.style.cursor = 'move';
						editor.redrawCanvas();
						editor.manualMove = true;
						/*Automatic scrolling on drag
                  var panel = editor.findParentByType(Ext.ux.TemplateGraphPanel);
						if(panel && panel.view && panel.view.scroller) {
							panel.view.scroller.scrollTo(editor.canvasDom.scrollHeight);
						}*/
					}
					else if (this.newLinkFromPort && editor.editable) {
						this.clear(ctx);
						editor.drawGrid();
						this.draw();

						var port = this.newLinkFromPort;
						var toport = this.newLinkToPort;
						var fromdims = {x:port.x, y:port.y};
						var todims = {x:mX, y:mY};

						if(!port.isVariablePort && (!toport || !toport.isVariablePort)) {
							var varid = editor.template.getFreeVariableId(port.name);
							//if(port.partOfLink) varid = port.partOfLink.variable.id;
							if(!this.newVariable) {
								this.newVariable = new DraggerVariable(varid, varid);
							}
							var dims = this.newVariable.getDimensions(ctx, varid); 
							var x = Math.round(mX - dims.width/2.0);
							var y = Math.round(mY);
							if(port.isInput) y -= dims.height;

							if(toport) {
								x = Math.round(port.x + (toport.x - port.x)/2.0 - dims.width/2.0);
								y = Math.round(port.y + (toport.y - port.y)/2.0 - dims.height/2.0);
								todims.x = Math.round(x+dims.width/2.0);
								todims.y = y + (port.isInput ? dims.height : 0);
							}
							Link.prototype.drawPartialLink(ctx, fromdims, todims, port.isInput);
							this.newVariable.draw(ctx, varid, x, y, false);
							if(toport) {
								fromdims = todims;
								fromdims.y += port.isInput ? -dims.height : dims.height;
								todims = {x:mX, y:mY};
								Link.prototype.drawPartialLink(ctx, fromdims, todims, port.isInput);
							}
						}
						else {
							Link.prototype.drawPartialLink(ctx, fromdims, todims, port.isInput);
						}
						editor.template.markLinkAdditionSideEffects(this.newLinkFromPort, this.newLinkToPort, this.newVariable);
					}
					else if(this.selBoxStart) {
						this.selBoxEnd = {};
						this.selBoxEnd.x = event.mouseX, this.selBoxEnd.y = event.mouseY;
						this.selectedItems = [];
						var x1 = this.selBoxStart.x, x2 = this.selBoxEnd.x;
						var y1 = this.selBoxStart.y, y2 = this.selBoxEnd.y;
						if(x1 > x2) { var tmp = x1; x1 = x2; x2 = tmp; }
						if(y1 > y2) { var tmp = y1; y1 = y2; y2 = tmp; }
						for(var i=0; i<this.items.length; i++) {
							var item = this.items[i];
							if(!item.shape) continue;
							if(item.x > x1 && item.x+item.width < x2 && item.y >  y1 && item.y+item.height < y2) {
								this.selectedItems.push(item.shape);
							}
						}
						this.clear(ctx);
						editor.drawGrid();
						this.draw();
						editor.drawSelectionBox();
					}
					else if (this.needRefresh && editor.editable) {
						this.clear(ctx);
						editor.drawGrid();
						this.draw();
					}
				},
				"mouseup" : function(event, type, ctx, item) {
					editor.template.clearSideEffects();
					editor.template.addLinkInCanvas(this.newLinkFromPort, this.newLinkToPort, this.newVariable);
					editor.updateGridVariables();
					editor.template.markErrors();
					editor.template.forwardSweep();

					if(!this.dragging && !this.selBoxEnd) 
						editor.deselectAllItems();
					else {
						editor.showSelectedItemInfo();
					}
					this.clear(ctx);
					editor.drawGrid();
					this.draw();

					this.newLinkToPort = null;
					this.newLinkFromPort = null;
					this.newVariable = null;
					this.selBoxStart = null;
					this.selBoxEnd = null;
					this.needRefresh = false;
					this.dragging = null;

					editor.canvasDom.style.cursor = 'default';
               if(event.preventDefault) event.preventDefault();
               event.returnValue = false;
				},
				"mousedown" : function(event, type, ctx, item) {
					this.selBoxStart = {};
					this.selBoxStart.x = event.mouseX, this.selBoxStart.y = event.mouseY;

               //if(event.preventDefault) event.preventDefault();
               //event.returnValue = false;
				}
			},
			clear: function(ctx) {
				ctx.clearRect(this.x,this.y,this.width, this.height);
			}
		}, this.canvas.LayerManager);

		this.graphLayout = new Layout(this.template);
	},

	initDropZone : function(item, group, callback) {
		var me = this;
		this.dropZone = new Ext.dd.DropTarget(item, {
			ddGroup : group,
			curNode: null,
			notifyEnter : function(ddSource, e, data) {
				if(data.records.length) {
					var comp = data.records[0].data.component;
					data.ddel.dom.innerHTML = "Drop to add <b>"+comp.id+"</b>";
				}
				return this.overClass;
			},
			notifyOut : function(ddSource, e, data) {
				data.ddel.dom.innerHTML = ddSource.dragText;
			},
			notifyDrop : function(ddSource, e, data) {
				if(data.records.length) {
					var comp = data.records[0].data.component;
					var mouse = me.canvas.translateEventCoordsToCanvasCoords(e.getPageX(), e.getPageY());
					var node = me.template.addNode(comp);
					node.x = mouse.x;
					node.y = mouse.y;
					me.template.markErrors();
					me.redrawCanvas();
					return true;
				}
				return false;
			}
		});
	},

	initLayerItems : function() {
		if(this.template_layer.getCount() > 0) 
			this.template_layer.removeAllItems();
		var items = this.template.getCanvasItems();
		for(var i=0; i<items.length; i++) {
			this.template_layer.addItem(items[i], false);
		}
	},

	zoom : function(value) {
		this.canvas.changeScale(value);
		this.manualZoom = true;
		this.redrawCanvas();
	},

	findErrors : function(rec) {
		var msgTarget = Ext.get(this.getId());
		msgTarget.mask('Connecting to TOP to find issues...', 'x-mask-loading');
		saveTemplateStore(this.template, this.gridPanel);
		var hpTree = this.tellMePanel.history.getComponent('tellmeHistoryTreePanel');
		var hpDetail = this.tellMePanel.history.getComponent('tellmeHistoryDetailPanel');
		runTopAlgorithm(this.template, hpTree, hpDetail, msgTarget, this.redrawCanvas, this, this.url);
		/*msgTarget.mask('Using Graphviz for Layout...', 'x-mask-loading');
		this.graphLayout.layoutDot(msgTarget, this.redrawCanvas, this, this.url, this.template);*/
	},

	layout : function() {
		var msgTarget = Ext.get(this.getId());
		msgTarget.mask('Designing Layout...', 'x-mask-loading');
		this.graphLayout.layoutDot(msgTarget, this.redrawCanvas, this, this.url, this.template);
	},

	saveImage : function() {
		//var scale = this.canvas.getScale();
		var scale = 1;
		var imgdata = this.getImageData(scale, true);
		window.open(imgdata);
	},
		
	getImageData : function(scale, show_constraints) {
		var x = 20;
		var y = this.template_layer.itemHeight+10; 
		var ctx = this.canvas.getCtx();

		var h = 30;
		var consHeight = this.gridPanel ? ((this.gridPanel.store.getCount()+1)*h + 10): 0;
		if(!show_constraints) consHeight = 0;
		this.canvasDom.height = (this.template_layer.itemHeight + consHeight)*scale;
		this.canvasDom.width = (this.template_layer.itemWidth)*scale;

		ctx.scale(scale,scale);
		this.template_layer.clear(ctx);
		this.template_layer.draw();

		if(show_constraints && this.gridPanel && this.gridPanel.store.getCount()) {
			ctx.save();
			ctx.fillStyle = "rgba(40,40,40,1)";
			ctx.textAlign = "left";
			ctx.textBaseline = "middle";
			ctx.font = "bold 17px tahoma";
			var selitem = this.template_layer.selectedItems.length==1 ? this.template_layer.selectedItems[0] : null;
			if(selitem && !selitem.getInputLinks)
				ctx.fillText("Constraints for "+selitem.id+":", x, y);
			else
				ctx.fillText("All Constraints:", x, y);

			ctx.font = "bold 14px tahoma";
			this.gridPanel.store.data.each(function() {
				y+=h;
				ctx.fillText(this.data.s+'  '+this.data.p+'  '+this.data.o, x+10, y);
			});
			ctx.restore();
		}
		var imgdata = this.canvasDom.toDataURL();
		this.redrawCanvas();
		return imgdata;
	},

	getSelectedVariable : function() {
		var items = this.template_layer.selectedItems;
		for(var i=0; i<items.length; i++) {
			var item = this.template.variables[items[i].id];
			if(item) return item;
		}
		return null;
	},

	getSelectedNode : function() {
		var items = this.template_layer.selectedItems;
		for(var i=0; i<items.length; i++) {
			var item = this.template.nodes[items[i].id];
			if(item) return item;
		}
		return null;
	},

	selectItem : function(item, scrollTo) {
		if(typeof(item) == "string") {
			var item = this.template.variables[item];
			if(!item) item = this.template.nodes[item];
		}
		if(item) {
			this.template_layer.selectedItems = [item];
			if(scrollTo) this.scrollTo(item.x, item.y);
			this.redrawCanvas();
			this.showSelectedItemInfo();
		}
	},

	deselectAllItems : function() {
		this.template_layer.selectedItems = [];
		this.clearGridPanelFilters();
		this.collapseInfoPanel();
	},
	
	clearGridPanelFilters : function() {
		// Clear filter
		if(this.gridPanel) {
			this.gridPanel.setTitle('Constraints: All');
			this.gridPanel.store.clearFilter();
		}
	},

	collapseInfoPanel : function() {
		if(this.infoPanel) {
			if(this.editable && this.infoPanel.dataGrid) {
				var gridEditor = this.infoPanel.dataGrid.getPlugin();
				gridEditor.cancelEdit();
			}
			this.infoPanel.collapse();
		}
	},

	deleteSelected : function() {
		var items = this.template_layer.selectedItems;
		if(!items.length) {
			showError('Select an Item to Delete first !');
			return;
		}
		for(var i=0; i<items.length; i++) {
			var item = items[i];
			var isVariable = true;
			if(item.getInputLinks) {
				this.template.removeNode(item);
			}
			else {
				this.template.removeVariable(item);
			}
		}
		this.updateGridVariables();
		this.template.markErrors();
		this.redrawCanvas();
	},

	updateGridVariables: function() {
		if(this.gridPanel && this.gridPanel.variableStore && this.editable) {
			var vars = [];
			for(var id in this.template.variables) vars.push(id);
			vars.sort();
			var values = Ext.Array.map(vars, function(a){return {name:a}});
			//window.console.log(vars);
			this.gridPanel.variableStore.loadData(values);
		}
	},

	showSelectedItemInfo : function() {
		var items = this.template_layer.selectedItems;
		if(items.length==1) {
			//if(window.console) window.console.log(item.id);
			var html = "";
			var isVariable = true;
			var item = items[0];
			if(item.getInputLinks) isVariable = false;
			if(this.infoPanel) {
				if(this.editable) {
					this.infoPanel.collapse(false);
					this.infoPanel.removeAll();
					var title = '';
					var source = {};
					if(isVariable) {
						title = (item.isInput?'Input ':(item.isOutput?'Output ':'Intermediate '))+
								  (item.type=='DATA'?'Data':'Parameter') + ' Variable: '+item.id;
						source['a_varName'] = item.id;
						if(item.isInput && item.type=='DATA') {
							source['b_varColl'] = (item.dim != 0);
						}
						if(item.isInput && item.type=='PARAM') {
							source['c_varAutoFill'] = (item.autofill ? true: false);
						}
					} else {
						title = 'Node: '+item.text;
						source['a_pruleS'] = (item.prule == 'S');
						if(item.getInputPorts().length > 1) 
							source['b_pruleDot'] = (item.pruleOp == 'dot');
						if(!item.isConcrete) source['c_cruleS'] = (item.crule == 'S');
					}
					var dataGrid = new Ext.grid.PropertyGrid({
						title:title, 
						source:source, 
						bodyCls:'smallPropertyGrid multi-line-grid',
						hideHeaders:true, 
						nameColumnWidth:300,
						autoHeight:true, 
						propertyNames: {
							'a_varName':'Variable Name',
							'b_varColl':'Input Data should be a Collection',
							'c_varAutoFill':'Automatically Set Parameter Value (Don\'t ask user)',
							'a_pruleS':'Create Component Collections to handle Input Data Collections',
							'b_pruleDot':'Combine Data Collections Pairwise from multiple inputs (default is Cross Product)',
							'c_cruleS':'Use all Concrete Components of this Abstract Component in the same workflow'
						}
					});
					var me = this;
					var myStore = dataGrid.getStore();
					myStore.on('update', function(){ 
						var mySource = dataGrid.getSource();
						if(isVariable) {
							var name = mySource['a_varName'];
							name = getRDFID(name);
							if(name && name != item.id) {
								if(me.template.variables[name]) {
									Ext.MessageBox.show({msg:'Another variable with name '+name+' already exists', modal:false});
								}
								else {
									delete me.template.variables[item.id];
									item.id = name;
									item.text = name;
									me.template.variables[item.id] = item;
								}
							}
							item.setDimensionality(mySource['b_varColl'] ? 1 : 0);
							item.setAutoFill(mySource['c_varAutoFill'] ? true : false);
						}
						else {
							item.setPortRule((mySource['a_pruleS'] ? 'S' : 'W'), (mySource['b_pruleDot'] ? 'dot' : 'cross'));
							item.setComponentRule(mySource['c_cruleS'] ? 'S' : 'W');
						}
						me.template.forwardSweep();
						me.redrawCanvas();
					});
					this.infoPanel.dataGrid = dataGrid;
					this.infoPanel.add(dataGrid);
					//this.infoPanel.doLayout();
					this.infoPanel.expand();
				}
				else {
					this.infoPanel.collapse(false);
					this.infoPanel.removeAll();
					if(isVariable) {
						html += "<h1>Variable: "+item.id+"</h1>";
						html += "<div><b>" + (item.isInput?"Input ":(item.isOutput?"Output ":"Intermediate "));	
						html += (item.type=="PARAM"?"Parameter":"Data")+"</b></div>";
						if(item.dim) html += "<div><b>Dimensionality:</b> " + item.dim+"-D Collection</div>";
						if(item.autofill) html += "<div><b>Autofill</b> parameter values, don't ask user</div>";
					} else {
						var items = this.cmap[item.component];
						if(items && items.length && (!item.isConcrete || item.binding)) {
							var me = this;
							var copts = {fieldLabel:item.id+' Binding'};
							copts = setComboListOptions(copts, items, item.binding, 'Select Component ..', false, false);
							var formitem = new Ext.form.field.ComboBox(copts);
							this.infoPanel.add(formitem);
							formitem.on('select', function() {
								var val = formitem.getValue();
								if(val) {
									item.setBinding(val);
									me.template.forwardSweep();
									me.redrawCanvas();
								}
							});
						}
						else {
							html += "<h1>Node: "+item.id+"</h1>";
						}
						//if(!item.isConcrete) html += "<div><b>Abstract Component</b></div>";
						if(item.prule=="S") html += "<div><b>Iterate</b> over Data Collection ("+item.pruleOp+" product)</div>";
						html += "<div><b>Inputs: </b>";
						for(var i=0; i<item.inputPorts.length; i++) {
							html += (i?", ":"")+item.inputPorts[i].name;
						}
						html += " <b>Outputs: </b>";
						for(var i=0; i<item.outputPorts.length; i++) {
							html += (i?", ":"")+item.outputPorts[i].name;
						}
						html += "</div>";
					}
					this.infoPanel.collapse();
					this.infoPanel.body.update(html);
					this.infoPanel.expand();
					this.infoPanel.doLayout();
				}
			}

			if(this.gridPanel) {
				this.gridPanel.store.clearFilter();
				if(isVariable) {
					this.gridPanel.store.filter('s', new RegExp('^'+item.id+'$'));
					this.gridPanel.setTitle('Constraints: '+item.id);
				}
				else {
					this.gridPanel.setTitle('Constraints: All');
					this.gridPanel.store.clearFilter();
				}
			}
		}
		else {
			this.collapseInfoPanel();
			this.clearGridPanelFilters();
		}
	},

	scrollTo : function(x, y) {
		if(this.canvasDom.parentNode.parentNode) {
			var cmp = Ext.get(this.canvasDom.parentNode.parentNode.id);
			this.canvasDom.parentNode.parentNode.scrollLeft = this.canvas.scale*x - this.graphPadding;
			cmp.scrollTo('top', this.canvas.scale*y - this.graphPadding, true);
		}
	},

	drawGrid : function() {
		if(!this.showGrid || !this.editable) return;
		var ctx = this.canvas.getCtx();
		ctx.beginPath();
		for(var i=this.gridSize; i<this.template_layer.height; i+= this.gridSize) {
			ctx.moveTo(0.5, i+0.5);
			ctx.lineTo(this.template_layer.width+0.5, i+0.5);
		}
		for(var i=this.gridSize; i<this.template_layer.width; i+= this.gridSize) {
			ctx.moveTo(i+0.5, 0.5);
			ctx.lineTo(i+0.5, this.template_layer.height+0.5);
		}
		ctx.strokeStyle = this.gridColor;
		ctx.lineWidth = 1;
		ctx.stroke();
	},

	drawSelectionBox : function() {
		var ctx = this.canvas.getCtx();
		ctx.beginPath();
		var lyr = this.template_layer;
		ctx.rect(lyr.selBoxStart.x, lyr.selBoxStart.y, lyr.selBoxEnd.x-lyr.selBoxStart.x, lyr.selBoxEnd.y-lyr.selBoxStart.y);
		ctx.strokeStyle = this.selBoxBorderColor;
		ctx.fillStyle = this.selBoxColor;
		ctx.lineWidth = 2;
		ctx.stroke();
		ctx.fill();
	},

	clearCanvas : function() {
		this.template_layer.clear(this.canvas.getCtx());
	},

	redrawCanvas : function(preferredWidth, preferredHeight) {
		var minX = 9999999; var minY = 9999999;
		var maxX = this.graphPadding;
		var maxY = this.graphPadding;

		if(preferredWidth || preferredHeight) 
			this.template_layer.draw();

		// Get required canvas dimensions
		for(var i=0; i<this.template_layer.items.length; i++) {
			var item = this.template_layer.items[i];
			var x = Math.round(item.x + item.width) - 0.5;
			var y = Math.round(item.y + item.height) - 0.5;
			if( x > maxX) maxX = x
			if( y > maxY) maxY = y
			if(item.link) continue;
			if( item.x < minX ) minX = parseInt(item.x);
			if( item.y < minY ) minY = parseInt(item.y);
		}
		if(minX == 9999999 || !preferredWidth) minX=this.graphPadding;
		if(minY == 9999999 || !preferredHeight) minY=this.graphPadding;
		for(var i=0; i<this.template_layer.items.length; i++) {
			var item = this.template_layer.items[i];
			if(item.link || this.manualMove) continue;
			var xitem;
			if(item.port) xitem = item.port;
			else if(item.shape) xitem = item.shape;
			xitem.x -= minX - this.graphPadding;
			xitem.y -= minY - this.graphPadding;
		}

		var w = (maxX+this.graphPadding) - (minX-this.graphPadding);
		var h = (maxY+this.graphPadding) - (minY-this.graphPadding);

		var scale = this.canvas.getScale();
		if(preferredWidth) {
			scale = preferredWidth/w;
			if(scale > 1) scale = 1;
		}
		if(preferredHeight) {
			var hscale = preferredHeight/h;
			if(hscale < scale ) scale = hscale;
		}

      if(Ext.isIE) 
          this.canvas.getCtx().scale(1/this.canvas.scale,1/this.canvas.scale);

		// The following resets the Canvas (setting canvas width, and height)
		this.canvasDom.width = this.panelWidth > scale*w ? this.panelWidth : scale*w;
		this.canvasDom.height = this.panelHeight > scale*h ? this.panelHeight : scale*h;
		//console.log(scale+","+w+","+this.canvasDom.width);

		// Setting some template layer variables
		this.template_layer.width = this.canvasDom.width/scale;
		this.template_layer.height = this.canvasDom.height/scale;

		// The following are used in the TemplateGraph Class only
		this.template_layer.itemWidth = w;
		this.template_layer.itemHeight = h;

		//if(window.console) window.console.log(scale);
		// Resetting the scale to previous value
		this.canvas.scale = 1.0;
		this.canvas.changeScale(scale);

		// Redrawing the canvas
		this.template_layer.clear(this.canvas.getCtx());
		this.drawGrid();
		this.template_layer.draw();
	}
});
