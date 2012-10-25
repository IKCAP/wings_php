function openDataTypeEditor(args) {
	//console.log(store);
	var guid = args[0];
	var tab = args[1];
	var store = args[2];
	var id = args[3];
	var op_url = args[4];
	var treePanel = args[5];
	var tabPanel = args[6];
	var advanced_user = args[7];

	// Extract inherited properties and my properties
	var myProperties = [];
	var inhProperties = [];
	Ext.iterate(store, function(prop) {
		if(store[prop].editable) myProperties.push({prop:prop, range:store[prop].range})
		else inhProperties.push({prop:prop, range:store[prop].range})
	});

	// Store for the editable properties
	if(!Ext.ModelManager.isRegistered('dataPropRange'))
		Ext.define('dataPropRange', { extend:'Ext.data.Model', fields:['prop', 'range'] });
   var dataTypeStore = new Ext.data.Store({
		model: 'dataPropRange',
		data: myProperties
   });

	var deletedProperties = {};

	var savebtn;
	if(advanced_user) {
		var savebtn = new Ext.Button({
			text: 'Save Changes',
			iconCls: 'saveIcon',
			disabled: true,
			handler : function(){
				var addedProperties = {};
				var modifiedProperties = {};
				gridPanel.getStore().each(function(rec) {
					if(rec.dirty) {
						var mod = rec.modified;
						if(mod.prop==null && mod.range==null) {
							addedProperties[rec.get('prop')] = rec.data;
						} else {
							modifiedProperties[mod.prop] = rec.data;
						}
					}
				});

				var url = op_url+'&op=saveDataTypeJSON';
				Ext.get(tabPanel.getId()).mask("Saving..");
				Ext.Ajax.request({ 
					url: url,
					params: {
						props_json:Ext.encode({add:addedProperties,del:deletedProperties,mod:modifiedProperties}), 
						force:false,
						data_type:(id=="DataObject" ? null : id)
					},
					success: function(resp) {
						Ext.get(tabPanel.getId()).unmask();

						var response = Ext.decode(resp.responseText);
						if(response.errors.length) {
							Ext.MessageBox.show({ 
								icon:Ext.MessageBox.ERROR, 
								buttons:Ext.MessageBox.OK, 
								msg:"Could not save:<br/>"+response.errors.join('<br/>')
							});
							return;
						}
						//gridPanel.getStore().commitChanges();
						Ext.each(gridPanel.getStore().getUpdatedRecords(), function(rec) { rec.commit(); });
						savebtn.setDisabled(true);
						tab.setTitle(tab.title.replace(/^\*/,''));
						deletedProperties = {};
						refreshInactiveTabs(tabPanel);
						//_console(resp.responseText);
					},
					failure: function(response) {
						Ext.get(tabPanel.getId()).unmask();
						_console(resp.responseText);
					}
				});
			}
		});
	}

	dataTypeStore.on('add', function(){ tab.setTitle("*"+tab.title.replace(/^\*/,'')); savebtn.setDisabled(false) });
	dataTypeStore.on('remove', function(){ tab.setTitle("*"+tab.title.replace(/^\*/,'')); savebtn.setDisabled(false) });
	dataTypeStore.on('update', function(){ tab.setTitle("*"+tab.title.replace(/^\*/,'')); savebtn.setDisabled(false) });

	if(!Ext.ModelManager.isRegistered('dataPropRangeTypes'))
		Ext.define('dataPropRangeTypes', { extend:'Ext.data.Model', fields:['type'] });

   var opts = {style:'font-size:11px;padding-top:0px'}
	var rangeEditor = new Ext.grid.CellEditor({
		field: new Ext.form.ComboBox({
			store: {
				model:'dataPropRangeTypes',
				data:[{type:'string'},{type:'boolean'},{type:'int'},{type:'float'}, {type:'date'}]
			}, 
			displayField: 'type',
			queryMode: 'local',
			listStyle:'font-size:11px',
			forceSelection:true, typeAhead:true, triggerAction:'all'
		})
	});
	var propEditor = new Ext.grid.CellEditor({
		field: new Ext.form.TextField(opts)
	});
	Ext.apply(rangeEditor.field,opts);

	var added_files = [];

	var swfbtn = new Ext.ux.swfbtn({
		id: 'button_'+guid+"_"+id,
		text: 'Upload Files ('+id+')',
		title: id+' Upload Queue',
		iconCls: 'uploadIcon',
		hidden: false,
		minWidth: 10,
		disabled: false,
		filetypes: "*",
		filesizelimit: '2 GB',
		iconpath: ASSETS_URL+'images/',
		postparams: {PHPSESSID: PHPSESSID, datatype: id},
		//customparams: [{title: 'Description', maxwidth: 50, label: 'Image Description:'}, { title: 'fadeimage', label: 'Fade Image', fieldtype: 'checkbox'}],
		uploadurl: ASSETS_URL+'swfupload/upload.php',
		flashurl: ASSETS_URL+'swfupload/swf/swfupload.swf',
		success_callback_fn: function(file, server_file) {
			if(server_file) 
				added_files.push(server_file);
		},
		allcomplete_callback_fn: function(file) {
			var nodes = treePanel.getSelectionModel().getSelection();
			var parentNode = (nodes && nodes.length) ? nodes[0]: null;
			addDataForType(added_files, parentNode, op_url);
		}
	});

	var mainPanel = new Ext.Panel({
		region: 'center',
		border: false,
		autoScroll: true,
		defaults: {
			padding: 4
		},
		tbar: [
			swfbtn,
			{xtype: 'tbfill'},'-',
			{ 
				iconCls: 'reloadIcon',
				text:'Reload Datatype',
				handler: function() {
					tab.getLoader().load();
					savebtn.setDisabled(true);
					tab.setTitle(tab.title.replace(/^\*/,''));
				}
			}
		]
	});

	var editorPlugin = Ext.create('Ext.grid.plugin.CellEditing', { clicksToEdit:1 });

	var tbar = null;
	if(advanced_user) {
		tbar = [{
			text: 'Add Property',iconCls: 'addIcon',
			handler : function(){
				var p =  new dataPropRange();
				var pos = dataTypeStore.getCount();
				editorPlugin.cancelEdit();
				dataTypeStore.insert(pos, p);
				editorPlugin.startEditByPosition({row:pos, column:1});
			}
		},'-',{
			text: 'Delete Property',iconCls: 'delIcon', id:'delProperty', disabled: true,
			handler : function(){
				editorPlugin.cancelEdit();
				var s = mainPanel.gridPanel.getSelectionModel().getSelection();
				for(var i=0, r; r=s[i]; i++) {
					var prop = (r.modified && r.modified.hasOwnProperty('prop'))?r.modified.prop:r.get('prop');
					if(prop==null) {
						// This property was just added, don't mark it as a deletedProperty for the server
					} else {
						deletedProperties[prop] = 1;
					}
					dataTypeStore.remove(r);
				}
			}
		},'-', savebtn];
	}

	var sm = Ext.create('Ext.selection.CheckboxModel', {
		checkOnly:true,
		listeners: {
			selectionchange: function(sm, selections) {
				gridPanel.down('#delProperty').setDisabled(selections.length == 0);
			}
		}
	});

	// Show properties
	var gridPanel = new Ext.grid.GridPanel({
		columnLines: true,
		autoHeight: true,
		plugins: [editorPlugin],
		title: 'Metadata Properties for '+id,
		columns: [ 
			{dataIndex: 'prop', flex:1, header: 'Property', editor: propEditor, editable:advanced_user?true:false, menuDisabled:true},
			{dataIndex: 'range', flex:1, header: 'Range', editor: rangeEditor, editable:advanced_user?true:false, menuDisabled:true}
		],
		selModel: sm,
		clicksToEdit: 1,
		store: dataTypeStore,
		border: true,
		tbar: tbar
	});

	mainPanel.add(gridPanel);
	mainPanel.gridPanel = gridPanel;

	// Show inherited Properties
	if(inhProperties.length) {
		// Store for the inherited properties
   	var inhStore = new Ext.data.Store({
			model: 'dataPropRange',
			data: inhProperties
   	});
		var inhGridPanel = new Ext.grid.GridPanel({
			title: "Inherited Metadata Properties",
			columns: [ 
				{dataIndex:'prop', header: 'Property', flex:1, menuDisabled:true},
				{dataIndex:'range', header: 'Range', flex:1, menuDisabled:true}
			],
			autoHeight: true,
			bodyCls:'inactive-grid',
			columnLines: true,
			border:true,
			store: inhStore
		});
		mainPanel.add(inhGridPanel);
	}
	tab.add(mainPanel);
}

function confirmAndDeleteFile(id, tabPanel, treePanel, op_url) {
	Ext.MessageBox.confirm("Confirm Delete", 
		"Are you sure you want to Delete "+id, 
		function(b) {
			if(b == "yes") {
				var url = op_url+'&op=delData';
				Ext.get(tabPanel.getId()).mask("Deleting..");
				Ext.get(treePanel.getId()).mask("Deleting..");
				Ext.Ajax.request({ 
					url: url,
					params: { data_id:id },
					success: function(response) {
						Ext.get(tabPanel.getId()).unmask();
						Ext.get(treePanel.getId()).unmask();
						if(response.responseText == "OK") {
							var node = treePanel.getStore().getNodeById(id);
							node.parentNode.removeChild(node);
							tabPanel.remove(tabPanel.getActiveTab());
						}
						else {
							if(console) console.log(response.responseText);
						}
					},
					failure: function(response) {
						Ext.get(tabPanel.getId()).unmask();
						Ext.get(treePanel.getId()).unmask();
						if(window.console) window.console.log(response.responseText);
					}
				});
			}
		}
	);
}

function openDataEditor(args) {
	var guid = args[0];
	var tab = args[1];
	var store = args[2];
	var id = args[3];
	var op_url = args[4];
	var treePanel = args[5];
	var tabPanel = args[6];
	var advanced_user = args[7];

	//console.log(store);
   var customEditors = {};
   var opts = {style:'font-size:11px;padding-top:0px;padding-bottom:1px'}
	Ext.iterate(store.props, function(prop) {
		pinfo = store.props[prop];
		var ed;
		if(pinfo.range.match(/int/)) {
			opts['allowDecimals'] = false;
			ed = new Ext.form.NumberField(opts);
		}
		else if(pinfo.range.match(/float/)) {
			opts['decimalPrecision'] = 6;
			ed = new Ext.form.NumberField(opts);
		}
		else if(pinfo.range.match(/date/)) ed = new Ext.form.DateField(opts);
		//else if(pinfo.range.match(/bool/)) ed = new Ext.ux.form.BooleanField(opts);
		else if(pinfo.range.match(/bool/)) ed = new Ext.form.Checkbox(opts);
		else ed = new Ext.form.TextField(opts);
		customEditors[prop] = new Ext.grid.CellEditor({field:ed});
   });

	var savebtn = new Ext.Button({ 
		text: 'Save Metadata',
		iconCls: 'saveIcon',
		disabled: true,
		handler : function() {
			var data = [];
			gridPanel.getStore().each(function(rec) {
				//data.push(rec.data);
 				var name = rec.data.name;
 				var val = rec.data.value;
 				if(val.getMonth) val = val.format("Y-m-d");
 				data.push({name:name, value:val});
			});
			var url = op_url+'&op=saveDataJSON';
			Ext.get(tabPanel.getId()).mask("Saving..");
			Ext.Ajax.request({ 
				url: url,
				params: {
					propvals_json:Ext.encode(data), 
					data_id:id
				},
				success: function(response) {
					Ext.get(tabPanel.getId()).unmask();
					if(response.responseText == "OK") {
						savebtn.setDisabled(true);
						tab.setTitle(tab.title.replace(/^\*/,''));
						refreshInactiveTabs(tabPanel);
					}
					else {
						if(window.console) window.console.log(response.responseText);
					}
				},
				failure: function(response) {
					Ext.get(tabPanel.getId()).unmask();
					if(window.console) window.console.log(response.responseText);
				}
			});
			//console.log(data),
		}
	});

	var delbtn = new Ext.Button({ 
		text: 'Delete File',
		iconCls: 'delIcon',
		handler : function() {
			confirmAndDeleteFile(id, tabPanel, treePanel, op_url);
		}
	});

	for(var key in store.vals) 
		if(store.vals[key] == null) store.vals[key] = '';

	var gridPanel = new Ext.grid.property.Grid({
		autoScroll: true,
		border: true,
		padding: 5,
		source: store.vals,
		columnLines: true,
		nameColumnWidth: 200,
		customEditors: customEditors,
		title: 'Metadata for '+id,
		tbar: [savebtn]
	});
	var dataStore = gridPanel.getStore();
	dataStore.on('add', function(){ tab.setTitle("*"+tab.title.replace(/^\*/,'')); savebtn.setDisabled(false) });
	dataStore.on('remove', function(){ tab.setTitle("*"+tab.title.replace(/^\*/,'')); savebtn.setDisabled(false) });
	dataStore.on('update', function(){ tab.setTitle("*"+tab.title.replace(/^\*/,'')); savebtn.setDisabled(false) });

	var mainPanel = new Ext.Panel({
		region: 'center',
		border: false,
		tbar:[ 
			{ 
				iconCls: 'downloadIcon',
				text:'View File',
				handler: function() {
					window.open(USER_DATA_URL+'/'+id);
				}
			},
			//'-',
			//delbtn,
			{xtype: 'tbfill'},'-',
			{ 
				iconCls: 'reloadIcon',
				text:'Reload Metadata',
				handler: function() {
					tab.getLoader().load();
					savebtn.setDisabled(true);
					tab.setTitle(tab.title.replace(/^\*/,''));
				}
			}
		],
		items:gridPanel
	});
	tab.add(mainPanel);
}

function refreshInactiveTabs(tabPanel) {
	/*var activeTab = tabPanel.getActiveTab();
	var items = tabPanel.items.items;
	for(var i=0; i<items.length; i++) {
		var tab = items[i];
		if(tab && tab.guifn && tab != activeTab) {
			//tab.getLoader().load({url: tab.url});
			tab.getLoader().load();
			//tab.guifn.call(this, tab.args);
		}
	}*/
}


function getDataTreePanel(guid, tabPanel, dataHierarchy, op_url, advanced_user) {
	var tmp = getDataTree(dataHierarchy);

	if(!Ext.ModelManager.isRegistered('dataTreeRecord'))
		Ext.define('dataTreeRecord', {extend:'Ext.data.Model', fields:['text', 'isClass']});
	var treeStore = Ext.create('Ext.data.TreeStore', {
		model: 'dataTreeRecord',
		root: { text: 'DataObject', id:'DataObject', isClass:true, expanded:true, children: tmp },
	});
	var treePanel = new Ext.tree.TreePanel({
		width: '100%',
		border: false,
		autoScroll: true,
		title:'Data',
		iconCls:'dataIcon',
		//bodyCls: 'hrefTree',
		bodyCls: 'x-docked-noborder-top',
		containerScroll: true,
		store:treeStore,
		url: op_url,
		viewConfig: {
			plugins: {
				ptype: 'treeviewdragdrop',
				enableDrag: advanced_user?true:false,
				ddGroup: guid+'_DataTree',
				appendOnly: true,
				dragText: 'Drag Datatype to its new Parent'
			}
		},
	});
	//var ts = new Ext.tree.TreeSorter(treePanel);

	treePanel.on("itemclick", function(view, rec, item, ind, event) {
		var id = rec.data.text;
		var tabName = id;
		var path = getTreePath(rec);

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
		if(!rec.parentNode) url = op_url+'&op=getDataTypeJSON&data_type=';
		else if(rec.data.isClass) url = op_url+'&op=getDataTypeJSON&data_type='+id;
		else url = op_url+'&op=getDataJSON&data_id='+id;

		var tab = openNewIconTab(tabPanel, tabName, (rec.data.isClass?'dtypeIcon':'dataIcon') );
		Ext.apply(tab, {
			path: path,
			guifn: (rec.data.isClass ? openDataTypeEditor : openDataEditor),
			args: [guid, tab, {}, id, op_url, treePanel, tabPanel, advanced_user],
			loader: {
				loadMask: true,
				url: url,
				renderer: function(loader, response, req) {
					//try {
						eval('var store = '+response.responseText);
						if(store) {
							//if(window.console) window.console.log(store);
							tab.removeAll();
							tab.args[2] = store;
							tab.guifn.call(this, tab.args);
							//tab.doLayout(false,true);
						}
					/*}
					catch (e) {
						el.update(e, false, cb);
					}*/
				}
			}
		});
		tabPanel.setActiveTab(tab);
		tab.getLoader().load();
	});

	treePanel.getStore().on('move', function(node, oldp, newp) {
		moveDatatypeTo(node.data.text, !oldp.parentNode?null:oldp.data.text, newp.parentNode?newp.data.text:null, op_url, treePanel);
		//window.console.log(node.data.text+" from "+(oldp?oldp.data.text:'root')+" -> "+(newp?newp.data.text:'root'));
	});

	tabPanel.on('tabchange', function(tp, tab){
		if(tab.path) treePanel.selectPath(tab.path);
	});

	return treePanel;
}

function getDataTree(data) {
	var tmp = [];
	var dids = [];
	var datamap = [];
	for(var i=0; i<data.length; i++) {
		dids[i] = data[i].id;
		datamap[dids[i]] = data[i];
	}
	dids.humanSort();
	for(var i=0; i<dids.length; i++) {
		var did = dids[i];
		var d = datamap[did];
		tmp.push({
			text:d.id,
			id:d.id,
			isClass:(d.type=='class'),
			leaf:(d.type=='class'?false:true),
			iconCls:(d.type=='class'?'dtypeIcon':'dataIcon'),
			expanded:true,
			draggable:(d.type=='class'),
			children:(d.children?getDataTree(d.children):[])
		});
	}
	return tmp;
}

function openNewIconTab(tabPanel, tabname, iconCls) {
	var tab = new Ext.Panel({
		layout: 'fit',
		closable: true,
		iconCls: iconCls,
		title: tabname,
		items: []
	});
	tabPanel.add(tab);
	return tab;
}

function addDataForType(ids, parentNode, op_url) {
	//window.console.log(file);
	var dtype = parentNode?parentNode.data.text:null;
	var url = op_url+'&op=addDataForType';
	Ext.Ajax.request({ 
		url: url,
		params: { data_id: Ext.encode(ids), data_type:dtype },
		success: function(response) {
			if(response.responseText == "OK") {
				for(var i=0; i<ids.length; i++) {
					var tmp = getDataTree([{id:ids[i]}]);
					if(tmp && tmp.length) {
						parentNode.data.leaf = false;
						parentNode.data.expanded = true;
						parentNode.appendChild(tmp[0]);
						//var tn = Ext.ModelManager.create(tmp[0],'dataTreeRecord');
						//parentNode.insertBefore(tn);
					}
				}
			}
			else if(window.console) window.console.log(response.responseText);
		},
		failure: function(response) {
			if(window.console) window.console.log(response.responseText);
		}
	});
}

function addDataType(parentNode, dataTree, op_url) {
	var parentType;
	if(!parentNode || !parentNode.parentNode) {
		parentNode = dataTree.getRootNode();
		parentType = null;
	}
	else {
		parentType = parentNode.data.text;
		parentNode = dataTree.getStore().getNodeById(parentType);
	}

	Ext.Msg.prompt("Add Datatype", "Enter name for the new Datatype:",function(btn, text) {
		if(btn == 'ok' && text) {
			text = getRDFID(text);
			var enode = dataTree.getStore().getNodeById(text);
			if(enode) {	
				showError('Datatype '+text+' already exists');
				return;
			}
			var url = op_url+'&op=newDataType';
			Ext.get(dataTree.getId()).mask('Adding..');
			Ext.Ajax.request({ 
				url: url,
				params: { parent_type: parentType, data_type: text },
				success: function(response) {
					Ext.get(dataTree.getId()).unmask();
					if(response.responseText == "OK") {
						var tmp = getDataTree([{id:text,type:'class'}]);
						if(tmp && tmp.length) {
							parentNode.data.leaf = false;
							parentNode.data.expanded = true;
							parentNode.appendChild(tmp[0]);
							//var tn = Ext.ModelManager.create(tmp[0],'dataTreeRecord');
							//parentNode.insertBefore(tn);
						}
					}
					else {
						if(window.console) window.console.log(response.responseText);
					}
				},
				failure: function(response) {
					Ext.get(dataTree.getId()).unmask();
					if(window.console) window.console.log(response.responseText);
				}
			});
		}
	},window,false);
};


function deleteDataType(treeNode, tabPanel, dataTree, delChildren, op_url) {
	var node = treeNode;
	var url = op_url+'&op=delDataTypes&del_children='+delChildren;
	if(!treeNode.parentNode) return;

	// Get All Child types to be removed as well
	var types = [node.data.text];
	var typesX = {}; typesX[node.data.text] = true;
	var tmp = [node];
	while(tmp.length > 0) {
		var n = tmp.pop();
		n.eachChild(function(t) { 
			typesX[t.data.text] = true; 
			if(t.data.isClass) {
				types.push(t.data.text); 
				tmp.push(t);
			}
		});
	}

	Ext.get(dataTree.getId()).mask('Deleting..');
	Ext.Ajax.request({ 
		url: url,
		params: { data_type: Ext.encode(types) },
		success: function(response) {
			Ext.get(dataTree.getId()).unmask();
			if(response.responseText == "OK") {
				/*var children = [];
				node.eachChild(function(n) { children.push(n) });
				for(var i=0; i<children.length; i++) {
					if(children[i].data.isClass)
						node.parentNode.insertBefore(children[i], node);
				}*/
				node.parentNode.removeChild(node);

				var tabitems = tabPanel.items.items;
				for(var i=0; i<tabitems.length; i++) {
					var tab = tabitems[i];
					if(tab && typesX[tab.title]) tabPanel.remove(tab);
				}
			}
			else {
				if(window.console) window.console.log(response.responseText);
			}
		},
		failure: function(response) {
			Ext.get(dataTree.getId()).unmask();
			if(window.console) window.console.log(response.responseText);
		}
	});
};


function moveDatatypeTo(dtype, fromtype, totype, op_url, treePanel) {
	var url = op_url+'&op=moveDatatypeTo';
	Ext.get(treePanel.getId()).mask('Moving..');
	Ext.Ajax.request({ 
		url: url,
		params: { data_type:dtype, from_parent_type:fromtype, to_parent_type:totype },
		success: function(response) {
			Ext.get(treePanel.getId()).unmask();
			//window.console.log(response.responseText);
		},
		failure: function(response) {
			Ext.get(treePanel.getId()).unmask();
			//window.console.log(response.responseText);
		}
	});
};


// Must be called within Ext.onReady
function initializeDataBrowser(guid, panelid, width, height, store, domain, op_url, advanced_user) {
	// Add the template tabPanel in the center
	var tabPanel = new Ext.TabPanel({
		region: 'center', 
		margins: '5 5 5 0',
		enableTabScroll: true,
		activeTab: 0,
		resizeTabs: true,
		//minTabWidth: 135,
		//tabWidth: 135,
		items: [{ 
			layout: 'fit',
			title: 'DataBrowser',
			autoLoad: { url: op_url+'&op=intro' }
		}]
	});
	var dataTree = getDataTreePanel(guid, tabPanel, store.tree, op_url, advanced_user);

	var tbar = null;
	if(advanced_user) {
		tbar = [{
				text: 'Add Datatype',
				iconCls: 'addIcon',
				handler: function() {
					var nodes = dataTree.getSelectionModel().getSelection();
					var node = (nodes && nodes.length) ? nodes[0]: null;
					addDataType(node, dataTree, op_url);
				}
			},'-',
			{
				text: 'Delete',
				iconCls: 'delIcon',
				handler: function() {
					var nodes = dataTree.getSelectionModel().getSelection();
					if(!nodes || !nodes.length) { return; }
					var node = nodes[0];
					if(!node.parentNode) { return; }
					if(!node.data.isClass) { 
						confirmAndDeleteFile(node.data.text, tabPanel, dataTree, op_url);
					}
					else {
						Ext.MessageBox.confirm("Confirm Delete", 
							"Are you sure you want to Delete "+node.data.text, 
							function(b) {
								if(b == "yes") {
									deleteDataType(node, tabPanel, dataTree, true, op_url);
								}
							}
						);
					}
				}
			}
		];
	}

	// Create an area on the left for the treepanel
	var leftPanel = new Ext.TabPanel({
		region: 'west',
		//collapsible: true,
		//collapseMode: 'mini',
		width:'20%',
		split: true,
		margins: '5 0 5 5',
		cmargins: '5 5 5 5',
		tbar:tbar,
		items: dataTree,
		activeTab: 0
	});

	
	var mainpanel  = new Ext.Viewport({
		layout: 'border', 
		monitorResize: true,
		items: [
			{
				region:'north',
				html:'<b>Data Browser</b>',
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
