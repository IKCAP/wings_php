LOAD_CONCRETE=0;

function getComponentTree(comp) {
	var tmp = [];
	for(var i=0; i<comp.length; i++) {
		var c = comp[i];
		var cnode = getComponentTreeNode(c, true);
		cnode.children = (c.children?getComponentTree(c.children):null)
		tmp.push(cnode);
	}
	return tmp;
}

function getComponentTreePanel(children, title, iconCls, guid, op_url, enableDrag) {
	if(!Ext.ModelManager.isRegistered('compTreeRecord'))
		Ext.define('compTreeRecord', {extend:'Ext.data.Model', fields:['text', 'component']});

	var treeStore = Ext.create('Ext.data.TreeStore', {
		model: 'compTreeRecord',
		root: { text: 'Components', expanded:true, children: children },
	});
	var treePanel = new Ext.tree.TreePanel({
		width: '100%',
		border: false,
		autoScroll: true,
		hideHeaders: true,
		//rootVisible: false,
		viewConfig: {
			plugins: {
				ptype: 'treeviewdragdrop',
				enableDrag: enableDrag,
				ddGroup: guid+'_ComponentTree',
				enableDrop: false,
				dragText: 'Drag the Component to the Canvas'
			}
		},
		iconCls: iconCls,
		bodyCls: 'x-docked-noborder-top',
		//bodyCls: 'nonBoldTree',
		title: title,
		store: treeStore,
		url: op_url
	});
	//var ts = new Ext.tree.TreeSorter(treePanel);
	return treePanel;
}

function getComponentTreeNode(c, setid) {
	var tooltip = null;
	if(c.id) {
		tooltip = '<h4>Component: '+c.id+'</h4>';
		if(c.inputs && c.outputs) {
			tooltip += "<br/><b>Inputs</b><br/>";
			for(var i=0; i<c.inputs.length; i++) 
				tooltip += " - "+c.inputs[i].role+" ("+(c.inputs[i].param?"Parameter":c.inputs[i].types)+')<br/>';
			tooltip += "<br/><b>Outputs</b><br/>";
			for(var i=0; i<c.outputs.length; i++) 
				tooltip += " - "+c.outputs[i].role+" ("+(c.outputs[i].param?"Parameter":c.outputs[i].types)+')<br/>';
			tooltip += '<br/>Drag this to the template graph on the right to add to the template';
		}
	}

	return {
		text:(c.id?c.id:c.cls),
		id:(setid ? (c.id?c.id:c.cls) : null),
		leaf:(c.children?false:true),
		iconCls:(c.id ? (c.concrete ? (c.uploaded ? 'compIcon': 'noCompIcon'): 'absCompIcon') : 'ctypeIcon'),
		component:{id:c.id, cls:c.cls, inputs:c.inputs, outputs:c.outputs, concrete:c.concrete, uploaded:c.uploaded},
		draggable:(c.id?true:false),
		qtip: tooltip,
		expanded:true,
	};
}


function getComponentIO(components, inputsOnly, outputsOnly) {
	var inputs = [];
	var outputs = [];
	var imaps = {};
	var omaps = {};
	var tmp = [];
	for(var i=0; i<components.length; i++) tmp.push(components[i]);

	while(tmp.length > 0) {
		var c = tmp.pop();
		if(c.children) for(var i=0; i<c.children.length; i++) tmp.push(c.children[i]);
		if(!c.id) continue;

		if(!outputsOnly) {
			var doneTypes = {};
			for(var i=0; i<c.inputs.length; i++) {
				var cin = c.inputs[i];
				if(cin.param) continue;
				if(!cin.types) continue;
				for(var j=0; j<cin.types.length; j++) {
					var itype = cin.types[j];
					if(doneTypes[itype]) continue;
					doneTypes[itype] = true;
					var inode = imaps[itype];
					if(!inode) {
						inode = {text:itype, leaf:false, iconCls:'dtypeIcon', expanded:true, draggable: false, children:[]}
						inputs.push(inode);
					}
					imaps[itype] = inode;
					var cnode = getComponentTreeNode(c, false);
					cnode.leaf = true;
					inode.children.push(cnode);
				}
			}
		}
		if(!inputsOnly) {
			var doneTypes = {};
			for(var i=0; i<c.outputs.length; i++) {
				var cout = c.outputs[i];
				for(var j=0; j<cout.types.length; j++) {
					var otype = cout.types[j];
					if(doneTypes[otype]) continue;
					doneTypes[otype] = 1;
					var onode = omaps[otype];
					if(!onode) {
						onode = {text:otype, leaf:false, iconCls:'dtypeIcon', expanded:true, draggable: false, children:[]}
						outputs.push(onode);
					}
					omaps[otype] = onode;
					var cnode = getComponentTreeNode(c, false);
					cnode.leaf = true;
					onode.children.push(cnode);
				}
			}
		}
	}
	if(inputsOnly) return inputs;
	if(outputsOnly) return outputs;
	return [{
		text:'Grouped By Inputs', iconCls:'dtypeIcon', draggable: false, expanded:true, children:inputs
	}, {
		text:'Grouped By Outputs', iconCls:'dtypeIcon', draggable: false, expanded:true, children:outputs
	}];
}

function getComponentListTree(guid, tabPanel, componentHierarchy, op_url, enableDrag) {
	var tmp = getComponentTree(componentHierarchy);
	return getComponentTreePanel(tmp, 'Tree', (LOAD_CONCRETE?'compIcon':'absCompIcon'), guid, op_url, enableDrag);
}

function getComponentInputsTree(guid, tabPanel, componentHierarchy, op_url, enableDrag) {
	var tmp = getComponentIO(componentHierarchy, true, false);
	return getComponentTreePanel(tmp, 'Inputs', 'inputIcon', guid, op_url, enableDrag);
}

function getComponentOutputsTree(guid, tabPanel, componentHierarchy, op_url, enableDrag) {
	var tmp = getComponentIO(componentHierarchy, false, true);
	return getComponentTreePanel(tmp, 'Outputs', 'outputIcon', guid, op_url, enableDrag);
}


function getIOListEditor(c, iostore, types, tab, savebtn, editable) {
	var mainPanel = new Ext.Panel({
		region: 'center',
		border: false,
		defaults : {
			border: true,
			padding: 4
		},   
		autoScroll: true
	});

	var inputs = []; var params = [];
	for(var i=0; i<iostore.inputs.length; i++) {
		var ip = iostore.inputs[i];
		if(ip.param) params.push(ip);
		else inputs.push(ip);
	}

	if(!Ext.ModelManager.isRegistered('DataRole'))
		Ext.define('DataRole', {extend:'Ext.data.Model', fields:['role','types','pfx','dim']});
	if(!Ext.ModelManager.isRegistered('ParamRole'))
		Ext.define('ParamRole', {extend:'Ext.data.Model', fields:['role','types','pfx','default']});

	var ipStore = new Ext.data.Store({model:'DataRole', data:inputs});
	var paramStore = new Ext.data.Store({model:'ParamRole', data:params});
	var opStore = new Ext.data.Store({model:'DataRole', data:iostore.outputs});

	var opts = {typeAhead:true, forceSelection:true, allowBlank:false};

	var iDataGrid, iParamGrid, oDataGrid;

	if(!Ext.ModelManager.isRegistered('dataPropRangeTypes'))
		Ext.define('dataPropRangeTypes', { extend:'Ext.data.Model', fields:['type'] });

	for(var i=0; i<3; i++) {
		var typeEditor = new Ext.grid.CellEditor({
			field: new Ext.form.ComboBox({
				store:{
					model:'dataPropRangeTypes',
					data: Ext.Array.map(types, function(a){return {type:a}})
				},
				displayField: 'type',
				queryMode: 'local'
			})
		});
		var pTypeEditor = new Ext.grid.CellEditor({
			field: new Ext.form.ComboBox({
				store:{
					model:'dataPropRangeTypes', 
					data:[{type:'string'},{type:'boolean'},{type:'int'},{type:'float'}, {type:'date'}]
				},
				displayField: 'type',
				queryMode: 'local'
			})
		});
		var txtEditor = new Ext.grid.CellEditor({field:new Ext.form.field.Text({allowBlank:false})});
		var numEditor = new Ext.grid.CellEditor({field:new Ext.form.field.Number({allowDecimals:false})});
		var boolEditor = new Ext.grid.CellEditor({field:new Ext.form.field.Checkbox()});

		Ext.apply(typeEditor.field,opts);
		Ext.apply(pTypeEditor.field,opts);

		var columns = [ 
			{dataIndex: 'role', header: 'Name', flex:1, editor:txtEditor, menuDisabled:true},
			{dataIndex: 'types', header: 'Type', flex:1, editor:(i==1?pTypeEditor:typeEditor), menuDisabled:true},
			{dataIndex: 'pfx', header: 'Prefix', flex:1, width:40, editor:txtEditor, menuDisabled:true},
		];
		if(i!=1) columns.push({dataIndex: 'dim', flex:1, header: 'Dimensionality', width:80, editor:numEditor, menuDisabled:true});
		else columns.push({dataIndex: 'default', flex:1, header: 'Default Value', width:80, editor:txtEditor, menuDisabled:true});

		var sm = editable ? Ext.create('Ext.selection.CheckboxModel', { checkOnly:true, }) : Ext.create('Ext.selection.RowModel');
		var editorPlugin = Ext.create('Ext.grid.plugin.CellEditing', { clicksToEdit:1 });
		var plugins = editable ? [editorPlugin] : [];
		var bodycls = editable ? '' : 'inactive-grid';

		var gridStore = (i==0?ipStore:(i==1?paramStore:opStore));
		var tbar = null;
		if(editable) {
			tbar = [
				{
					text: 'Add',
					iconCls: 'addIcon',
					roletype: i,
					handler: function() {
						var role;
						var i = this.roletype;
						var panel = (i==0 ? iDataGrid : (i==1 ? iParamGrid : oDataGrid));
						var gridStore = panel.getStore();
						var pos = gridStore.getCount();
						var sm = panel.getSelectionModel();
						var pfx = (i==0 ? '-i': (i==1 ? '-p': '-o')) + (pos+1);

						if(i!=1) role =  new DataRole({pfx:pfx, dim:0});
						else role =  new ParamRole({pfx:pfx});
						panel.editorPlugin.cancelEdit();
						gridStore.insert(pos, role);
						//sm.selectRange(0);
						//panel.editorPlugin.startEditByPosition({row:pos, column:1});
					}
				},
				{
					iconCls: 'delIcon',
					text: 'Delete',
					roletype: i,
					handler: function() {
						var i = this.roletype;
						var panel = (i==0 ? iDataGrid : (i==1 ? iParamGrid : oDataGrid));
						var gridStore = panel.getStore();
						panel.editorPlugin.cancelEdit();
						var s = panel.getSelectionModel().getSelection();
						for(var i=0, r; r=s[i]; i++) {
							gridStore.remove(r);
						}
						//mainPanel.doLayout();
					}
				}
			];
		}

		var gridPanel = new Ext.grid.GridPanel({
			columnLines: true,
			autoHeight: true,
			//forceFit: true,
			title: (i==0?'Input Data':(i==1?'Input Parameters':'Output Data')),
			iconCls: (i==0?'inputIcon':(i==1?'paramIcon':'outputIcon')),
			columns: columns,
			selModel: sm,
			selType: 'cellmodel',
			plugins: plugins,
			bodyCls: bodycls,
			store: gridStore,
			tbar: tbar
		});
		gridPanel.editorPlugin = editorPlugin;

		if(i==0) iDataGrid=gridPanel;
		if(i==1) iParamGrid=gridPanel;
		if(i==2) oDataGrid=gridPanel;
		gridStore.on('add', function(){ tab.setTitle("*"+tab.title.replace(/^\*/,'')); savebtn.setDisabled(false) });
		gridStore.on('remove', function(){ tab.setTitle("*"+tab.title.replace(/^\*/,'')); savebtn.setDisabled(false) });
		gridStore.on('update', function(){ tab.setTitle("*"+tab.title.replace(/^\*/,'')); savebtn.setDisabled(false) });
	}
	/*var colPanel = new Ext.Panel({
		layout:'column',
		autoHeight: true,
		border: false,
		items: [iDataGrid, iParamGrid]
	});
	mainPanel.add(colPanel);*/

	mainPanel.add(iDataGrid);
	mainPanel.add(iParamGrid);
	mainPanel.add(oDataGrid);

	mainPanel.iDataGrid = iDataGrid;
	mainPanel.iParamGrid = iParamGrid;
	mainPanel.oDataGrid = oDataGrid;
	return mainPanel;
}

function addComponent(parentNode, cTree, op_url) {
	var parentId;
	var parentType;
	var parentIsConcrete = false;
	if(!parentNode || !parentNode.parentNode) {
		parentNode = cTree.getRootNode();
		parentId = null;
		parentType = null;
	}
	else {
		var pc = parentNode.data.component;
		parentId = pc.id;
		parentType = pc.cls;
		parentIsConcrete = pc.concrete;
	}
	// New: Can only add concrete components to existing component types
	if(LOAD_CONCRETE && (!parentId || parentIsConcrete)) {
		showError('Please select a Component Type first');
		return;
	}

	Ext.Msg.prompt("Add Component", "Enter name for the new Component:", function(btn, text) {
		if(btn == 'ok' && text) {
			text = getRDFID(text);
			var enode = cTree.getStore().getNodeById(text);
			if(enode) {	
				showError('Component '+text+' already exists');
				return;
			}
			var url = op_url+'&op=newComponent';
			Ext.get(cTree.getId()).mask("Creating..");
			Ext.Ajax.request({ 
				url: url,
				params: { parent_cid: parentId, parent_type: parentType, cid: text, load_concrete:LOAD_CONCRETE },
				success: function(response) {
					Ext.get(cTree.getId()).unmask();
					if(response.responseText == "OK") {
						var tmp = getComponentTree([{id:text, cls:text+'Class', concrete:(LOAD_CONCRETE?true:false)}]);
						if(tmp && tmp.length) {
							parentNode.data.leaf = false;
							parentNode.data.expanded = true;
							parentNode.appendChild(tmp[0]);
						}
					}
					else {
						if(window.console) window.console.log(response.responseText);
					}
				},
				failure: function(response) {
					Ext.get(cTree.getId()).unmask();
					if(window.console) window.console.log(response.responseText);
				}
			});
		}
	},window,false);
}

function addCategory(parentNode, cTree, op_url) {
	var parentType;
	if(!parentNode || !parentNode.parentNode) {
		parentNode = cTree.getRootNode();
		parentType = null;
	}
	else {
		var pc = parentNode.data.component;
		parentType = pc.cls;
	}

	Ext.Msg.prompt("Add Category", "Enter name for the new Category:", function(btn, text) {
		if(btn == 'ok' && text) {
			text = getRDFID(text);
			var enode = cTree.getStore().getNodeById(text);
			if(enode) {	
				showError('Category '+text+' already exists');
				return;
			}
			var url = op_url+'&op=newCategory';
			Ext.get(cTree.getId()).mask("Creating..");
			Ext.Ajax.request({ 
				url: url,
				params: { parent_type: parentType, cid: text, load_concrete:LOAD_CONCRETE },
				success: function(response) {
					Ext.get(cTree.getId()).unmask();
					if(response.responseText == "OK") {
						var tmp = getComponentTree([{cls:text}]);
						if(tmp && tmp.length)
							parentNode.data.leaf = false;
							parentNode.data.expanded = true;
							parentNode.appendChild(tmp[0]);
					}
					else {
						if(window.console) window.console.log(response.responseText);
					}
				},
				failure: function(response) {
					Ext.get(cTree.getId()).unmask();
					if(window.console) window.console.log(response.responseText);
				}
			});
		}
	},window,false);
}

function setComponentIsUploaded(cid, tab, cTree) {
	var node = cTree.getStore().getNodeById(cid);
	tab.setIconCls('compIcon');
	node.set('iconCls', 'compIcon');
}

function setComponentIsConcrete(cid, tab, cTree, op_url) {
	var url = op_url+'&op=setComponentIsConcrete';
	Ext.get(cTree.getId()).mask("Marking as Concrete..");
	Ext.Ajax.request({ 
		url: url,
		params: { cid: cid, load_concrete:LOAD_CONCRETE },
		success: function(response) {
			Ext.get(cTree.getId()).unmask();
			if(response.responseText == "OK") {
				var node = cTree.getStore().getNodeById(cid);
				tab.setIconClass('compIcon');
				var icon = Ext.get(node.getUI().getIconEl());
				icon.removeClass('absCompIcon');
				icon.addClass('compIcon');
			}
			else {
				if(window.console) window.console.log(response.responseText);
			}
		},
		failure: function(response) {
			Ext.get(cTree.getId()).unmask();
			if(window.console) window.console.log(response.responseText);
		}
	});
}

function openComponentEditor(args) {
	var tab = args[0];
	var c = args[1];
	var store = args[2];
	var op_url = args[3];
	var treePanel = args[4];
	var tabPanel = args[5];
	var advanced_user = args[6];
	var types = args[7];
	var mainPanel;

	var savebtn = new Ext.Button({ 
		text: 'Save',
		iconCls: 'saveIcon',
		disabled: true,
		handler : function() {
			var mainPanel = tab.ioEditor;
			var comp = {idata:[], iparams:[], odata:[], rules:[], concrete:c.concrete};
			var notok = false;
			mainPanel.iDataGrid.getStore().each(function(rec) {
				if(!rec.data.role || !rec.data.types || !rec.data.pfx) notok = true;
				comp.idata.push(rec.data);
			});
			mainPanel.iParamGrid.getStore().each(function(rec) {
				if(!rec.data.role || !rec.data.types || !rec.data.pfx) notok = true;
				comp.iparams.push(rec.data);
			});
			mainPanel.oDataGrid.getStore().each(function(rec) {
				if(!rec.data.role || !rec.data.types || !rec.data.pfx) notok = true;
				comp.odata.push(rec.data);
			});
			if(notok) {
				Ext.MessageBox.show({ 
					icon:Ext.MessageBox.ERROR, 
					buttons:Ext.MessageBox.OK, 
					msg:"Please fill out all fields before saving" 
				});
				return;
			}
			var url = op_url+'&op=saveComponentJSON';
			Ext.get(treePanel.getId()).mask("Saving..");
			Ext.Ajax.request({ 
				url: url,
				params: {
					component_json:Ext.encode(comp), 
					load_concrete:LOAD_CONCRETE,
					cid:c.id
				},
				success: function(response) {
					Ext.get(treePanel.getId()).unmask();
					if(response.responseText == "OK") {
						Ext.each(mainPanel.iDataGrid.getStore().getUpdatedRecords(), function(rec) { rec.commit(); });
						Ext.each(mainPanel.iParamGrid.getStore().getUpdatedRecords(), function(rec) { rec.commit(); });
						Ext.each(mainPanel.oDataGrid.getStore().getUpdatedRecords(), function(rec) { rec.commit(); });
						//mainPanel.iDataGrid.getStore().commitChanges();
						//mainPanel.iParamGrid.getStore().commitChanges();
						//mainPanel.oDataGrid.getStore().commitChanges();
						savebtn.setDisabled(true);
						tab.setTitle(tab.title.replace(/^\*/,''));
						refreshInactiveTabs(tabPanel);
					}
					else {
						Ext.MessageBox.show({ 
							icon:Ext.MessageBox.ERROR, 
							buttons:Ext.MessageBox.OK, 
							msg:"Could not save:<br/>"+response.responseText.replace(/\n/, '<br/>')
						});
						//if(window.console) window.console.log(response.responseText);
					}
				},
				failure: function(response) {
					Ext.get(treePanel.getId()).unmask();
					if(window.console) window.console.log(response.responseText);
				}
			});
		}
	});

	tab.ioEditor = getIOListEditor(c, store, types, tab, savebtn, (advanced_user && c.concrete==LOAD_CONCRETE));

	var swfbtn = new Ext.ux.swfbtn({
		id: 'c_button_'+c.id,
		iconCls: 'uploadIcon',
		text:'Upload '+(c.uploaded?'New Version':'Component'),
		title: c.id+' Upload',
		hidden: false,
		isSingle: true,
		filetypes: "*.zip",
		minWidth: 10,
		disabled: false,
		filesizelimit: '3 GB',
		iconpath: ASSETS_URL+'images/',
		postparams: {PHPSESSID: PHPSESSID, cid: c.id},
		uploadurl: ASSETS_URL+'swfupload/upload.php',
		flashurl: ASSETS_URL+'swfupload/swf/swfupload.swf',
		success_callback_fn: function(file, server_file) {
			if(server_file) {
				//setComponentIsConcrete(c.id, tab, treePanel, op_url);
				setComponentIsUploaded(c.id, tab, treePanel);
			}
		}
	});

	var tbar = null;
	if(advanced_user) {
		tbar = [];
		if(c.concrete == LOAD_CONCRETE) {
			tbar.push(savebtn);
			tbar.push('-');
		}
		tbar.push({ 
			iconCls: 'reloadIcon',
			text:'Reload',
			handler: function() {
				tab.getLoader().load();
				savebtn.setDisabled(true);
				tab.setTitle(tab.title.replace(/^\*/,''));
			}
		});
		if(c.concrete && LOAD_CONCRETE) {
			tbar.push({ xtype: 'tbfill' }),
			tbar.push('-');
			tbar.push(swfbtn);
			tbar.push({ 
				text: 'Download', 
				menu: [{ 
					iconCls: 'downloadIcon',
					disabled: !c.uploaded,
					text:'Download Current Version',
					handler: function() {
						window.open(op_url+'&op=downloadComponent&cid='+c.id+"&load_concrete="+LOAD_CONCRETE);
					}
				},
				{ 
					iconCls: 'downloadIcon',
					text:'Download Skeleton Component',
					handler: function() {
						window.open(op_url+'&op=downloadSkeleton&cid='+c.id+"&load_concrete="+LOAD_CONCRETE);
					}
				},
				]
			});
		}
	}

	var mainPanel = new Ext.Panel({
		region: 'center',
		border: false,
		layout: 'fit',
		tbar: tbar,
		items:tab.ioEditor
	});
	tab.add(mainPanel);
}


function initComponentTreePanelEvents(treePanel, tabPanel, op_url, advanced_user, types) {
	treePanel.on("itemclick", function(view, rec, item, ind, event) {
		if(!rec.parentNode) return false;
		var id = rec.data.text;
		var path = getTreePath(rec);
		var tabName = id;
		var c = rec.data.component;
		if(!c.id) return;

		// Check if tab is already open
		var items = tabPanel.items.items;
		for(var i=0; i<items.length; i++) {
			var tab = items[i];
			if(tab && tab.title.replace(/^\**/,'')==tabName) {
				tabPanel.setActiveTab(tab);
				return null;
			}
		}

		// Fetch Store via Ajax
		var url = op_url+'&op=getComponentJSON&cid='+c.id+'&load_concrete='+LOAD_CONCRETE;

		var tab = openNewIconTab(tabPanel, tabName, (c.concrete ? (c.uploaded ? 'compIcon': 'noCompIcon'): 'absCompIcon'));
		Ext.apply(tab, {
			path: path, 
			guifn: openComponentEditor,
			args: [tab, c, {}, op_url, treePanel, tabPanel, advanced_user, types]
		});
		tabPanel.setActiveTab(tab);

		Ext.apply(tab, {
			loader: {
				loadMask: true,
				url: url,
				renderer: function(loader, response, req) {
					//try {
						var store = Ext.decode(response.responseText);
						if(store) {
							//if(window.console) window.console.log(store);
							tab.removeAll();
							tab.args[2] = store;
							tab.guifn.call(this, tab.args);
							//tab.doLayout(false,true);
						}
					/*}
					catch (e) {
						console.log(e);
						//el.update(e, false, cb);
					}*/
				}
			}
		});
		tab.getLoader().load();
	});

	/*treePanel.getStore().on('move', function(node, oldp, newp) {
		moveComponentTo(node.id, !oldp.parentNode?null:oldp.id, !newp.parentNode?null:newp.id, op_url);
		//window.console.log(node.id+" from "+oldp.id+" -> "+newp.id);
	});*/

	tabPanel.on('tabchange', function(tp, tab){
		if(tab.path) treePanel.selectPath(tab.path);
	});

	return treePanel;
}

function getRulesTab(op_url, editable) {
	var rulesArea = new Ext.form.TextArea({enableKeyEvents:editable, disabled:!editable});
	var tmp = {};

	tmp.savebtn = new Ext.Button({
		text: 'Save',
		iconCls: 'saveIcon',
		disabled: true,
		handler: function() {
			var rules = rulesArea.getValue();
			var url = op_url+'&op=saveAllRules';
			var msgTarget = Ext.get(tmp.rulesTab.getId());
			msgTarget.mask('Saving...', 'x-mask-loading');
			Ext.Ajax.request({ 
				url: url,
				params: { rules: rules, load_concrete:LOAD_CONCRETE },
				success: function(response) {
					msgTarget.unmask();
					if(response.responseText == "OK") {
						tmp.rulesTab.setTitle(tmp.rulesTab.title.replace(/^\*/,'')); 
						tmp.savebtn.setDisabled(true);
						rulesArea.on('keyup', tmp.keyfn );
					} 
					else {
						alert("Error: Cannot save the rules..\n"+response.responseText);
						if(window.console) window.console.log(response.responseText);
					}
				},
				failure: function(response) {
					msgTarget.unmask();
					if(window.console) window.console.log(response.responseText);
				}
			});
		}
	});

	tmp.keyfn = function(obj, e) { 
		var key = e.getKey();
		if(key >= 33 && key <= 40) return;
		if(key >= 16 && key <= 18) return;
		if(key >= 112 && key <= 123) return;
		if(key >= 90 && key <=91) return;
		if(key == 27) return;
		tmp.rulesTab.setTitle("*"+tmp.rulesTab.title.replace(/^\*/,'')); 
		tmp.savebtn.setDisabled(false) 
		rulesArea.un('keyup', tmp.keyfn);
	};

	var tbar = null;
	if(editable) {
		tbar = [
			tmp.savebtn,
			{xtype: 'tbfill'},
			{
				text: 'Reload',
				iconCls: 'reloadIcon',
				handler: function() {
					tmp.rulesTab.getLoader().load();
				}
			}
		];
	}
	tmp.rulesTab = new Ext.Panel({
		tbar: tbar,
		title: 'Rules',
		iconCls: 'inferIcon', 
		layout: 'fit',
		items: rulesArea,
		loader: {
			loadMask: true,
			url: op_url+'&op=getAllRules'+"&load_concrete="+LOAD_CONCRETE,
			renderer: function(loader, response, req) {
				tmp.rulesTab.removeAll(false);
				tmp.rulesTab.add(rulesArea);
				rulesArea.setValue(response.responseText);
				tmp.rulesTab.setTitle(tmp.rulesTab.title.replace(/^\*/,'')); 
				tmp.savebtn.setDisabled(true);
				//tmp.rulesTab.doLayout(false,true);
				rulesArea.on('keyup', tmp.keyfn );
			}
		}
	});
	tmp.rulesTab.getLoader().load();
	return tmp.rulesTab;
}

function initializeComponentBrowser(guid, panelid, width, height, store, domain, op_url, advanced_user) {
	// Add the template tabPanel in the center

	var tabPanel = new Ext.TabPanel({
		region: 'center', 
		margins: '5 5 5 0',
		enableTabScroll: true,
		activeTab: 0,
		resizeTabs: true,
		//resizeTabs: true,
		//minTabWidth: 50,
		//tabWidth: 135,
		items: [{ 
			layout: 'fit',
			title: 'Intro',
			autoLoad: { url: op_url+'&op=intro&load_concrete='+LOAD_CONCRETE }
		}]
	});
	tabPanel.add(getRulesTab(op_url, advanced_user)); 

	var componentTree = getComponentListTree(guid, tabPanel, store.tree, op_url);
	//var cInputsTree = getComponentInputsTree(guid, tabPanel, store.tree, op_url);
	//var cOutputsTree = getComponentOutputsTree(guid, tabPanel, store.tree, op_url);

	var delbtn = new Ext.Button({ 
		text: 'Delete',
		iconCls: 'delIcon',
		handler : function() {
			var nodes = componentTree.getSelectionModel().getSelection();
			if(!nodes || !nodes.length) return;
			var node = nodes[0];
			var c = node.data.component;
			if(!c.concrete && LOAD_CONCRETE) {
				Ext.MessageBox.show({
					title: 'Cannot delete', 
					msg: 'Cannot delete component types from this interface. Go to "Manage Component Types" instead',
					buttons: Ext.Msg.OK
				});
				return;
			}
			Ext.MessageBox.confirm("Confirm Delete", 
				"Are you sure you want to Delete "+(c.id ? c.id: c.cls), 
				function(b) {
					if(b == "yes") {
						var url = op_url+'&op='+(c.id ? 'delComponent' : 'delCategory');
						Ext.get(componentTree.getId()).mask("Deleting..");
						Ext.Ajax.request({ 
							url: url,
							params: { cid:c.id?c.id:c.cls, load_concrete:LOAD_CONCRETE },
							success: function(response) {
								Ext.get(componentTree.getId()).unmask();
								if(response.responseText == "OK") {
									node.parentNode.removeChild(node);
									if(c.id) tabPanel.remove(tabPanel.getActiveTab());
								}
								else {
									if(window.console) window.console.log(response.responseText);
								}
							},
							failure: function(response) {
								Ext.get(componentTree.getId()).unmask();
								if(window.console) window.console.log(response.responseText);
							}
						});
					}
				}
			);
		}
	});


	var tbar = null;
	if(advanced_user) {
		tbar = [{
				text: 'Add '+(LOAD_CONCRETE?'Component':'Type'), iconCls: 'addIcon',
				handler: function() {
					var nodes = componentTree.getSelectionModel().getSelection();
					var parentNode = (nodes && nodes.length) ? nodes[0]: null;
					addComponent(parentNode, componentTree, op_url);
				}
			}
		];
		if(!LOAD_CONCRETE) {
			tbar.push({
				text: 'Add Category', iconCls: 'dtypeIcon',
				handler: function() {
					var nodes = componentTree.getSelectionModel().getSelection();
					var parentNode = (nodes && nodes.length) ? nodes[0]: null;
					addCategory(parentNode, componentTree, op_url);
				}
			});
		}
		tbar.push(delbtn);
	}

	var leftPanel = new Ext.TabPanel({
		region: 'west',
		width:300,
		split: true,
		margins: '5 0 5 5',
		activeTab: 0,
		tbar: tbar
	});
	var libname = (LIBNAME=="library" ? "Default": LIBNAME);
	Ext.apply(componentTree, {title:'Component'+(!LOAD_CONCRETE? ' Types':'s: '+libname)});
	//Ext.apply(componentTree, {bodyCls:'hrefTree', title:'Component'+(!LOAD_CONCRETE? ' Types':'s: '+libname)});
	//Ext.apply(cInputsTree, {bodyCls:'hrefTree'});
	//Ext.apply(cOutputsTree, {bodyCls:'hrefTree'});

	store.types.sort();
	initComponentTreePanelEvents(componentTree, tabPanel, op_url, advanced_user, store.types);

	leftPanel.add(componentTree);
	leftPanel.setActiveTab(0);
	//leftPanel.add(cInputsTree);
	//leftPanel.add(cOutputsTree);
	
	var mainpanel  = new Ext.Viewport({
		layout: 'border', 
		monitorResize: true,
		items: [
			{
				region:'north',
				html:'<b>Component Browser</b>',
				bodyStyle: 'background-color:#333;color:orange;padding:8px;font-weight:bold',
				autoHeight: true
			},
			leftPanel, 
			tabPanel
		]
	});

	//mainpanel.render(Ext.get(panelid));

	return mainpanel;
}
