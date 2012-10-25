// TabPanel Strip Rendering bug

function getTemplateGraph(guid, template_id, store, templatePanel, infoPanel, gridPanel, op_url, editable, editor_mode) {
	return new Ext.ux.TemplateGraphPanel({
		width: '100%',
		guid: guid,
		store: store.template,
		cmap: templatePanel.cmap,
		infoPanel: infoPanel,
		gridPanel: gridPanel,
		border: false,
		editable: editable,
		editor_mode: editor_mode,
		templatePanel: templatePanel,
		url: op_url,
		template_id: template_id});
}

function getSeedForm(template_id, formdata, templatePanel, run_url, op_url, results_url) {
	return new Ext.ux.form.SeedForm({
		store: formdata,
		//margins:'5 5 0 5', 
		//border:true, 
		split:true, 
		preventHeader:true, 
		collapsible:true, 
		collapseMode:'mini', 
		autoHeight:true, 
		url: run_url,
		op_url: op_url,
		results_url: results_url,
		templatePanel: templatePanel,
		template_id: template_id
	});
}


function getChildComponentsMap (comps) {
	var cmaps = getConcreteComponents({id:'Component', children:comps}, {});
	return cmaps;
}

function getConcreteComponents(c, map) {
	if(c.children) {
		map[c.id] = [];
		for(var i=0; i<c.children.length; i++) {
			var child = c.children[i];
			if(child.concrete) map[c.id].push(child.id);
			var cmap = getConcreteComponents(child, map);
			if(cmap[child.id])
				map[c.id] = map[c.id].concat(cmap[child.id]);
		}
	}
	return map;
}

function getRDFID(templateText) {
	templateText = templateText.replace(/[^a-zA-Z0-9_]/g, '_');
	if(templateText.match(/^[0-9]/))
		templateText = 'N'+templateText;
	return templateText;
}

function getTemplateText(templateID) {
	return templateID.replace(/_/g,' '); 
}

function createNewTemplate(guid, opts, tabPanel, treePanel, op_url, run_url, results_url, tellMePanel) {
	Ext.Msg.prompt("New Template..", "Enter name for the template:",function(btn, text) {
		if(btn == 'ok' && text) {
			var tname = text;
			var tid = getRDFID(tname);
			var url = op_url+'&op=newTemplate&template_id='+tid;
			var msgTarget = Ext.get(treePanel.getId());
			msgTarget.mask('Creating...', 'x-mask-loading');
			Ext.Ajax.request({ 
				url: url,
				success: function(response) {
					msgTarget.unmask();
					if(window.console) window.console.log(response.responseText);
					if(response.responseText == "OK") {
						var tn = {id:tid,text:tname,iconCls:'wflowIcon',leaf:true};
						treePanel.getRootNode().appendChild(tn);
						var path = getTreePath(treePanel.getStore().getNodeById(tn.id));
						openTemplate(tid, tname, opts, guid, tabPanel, treePanel, op_url, run_url, results_url, path, tellMePanel); 
					}
				},
				failure: function(response) {
					if(window.console) window.console.log(response.responseText);
				}
			});
		}
	},window,false);
}


function deleteTemplate(tid, tname, tabPanel, treePanel, op_url) {
	var url = op_url+'&op=deleteTemplate&template_id='+tid;
	var msgTarget = Ext.get(treePanel.getId());
	msgTarget.mask('Creating...', 'x-mask-loading');
	Ext.Ajax.request({ 
		url: url,
		success: function(response) {
			msgTarget.unmask();
			if(window.console) window.console.log(response.responseText);
			if(response.responseText == "OK") {
				var tn = treePanel.getStore().getNodeById(tid);
				tn.parentNode.removeChild(tn);

				var items = tabPanel.items.items;
				for(var i=0; i<items.length; i++) {
					var tab = items[i];
					if(tab && tab.title==tname) {
						tabPanel.remove(tab);
					}
				}
			}
		},
		failure: function(response) {
			if(window.console) window.console.log(response.responseText);
		}
	});
}


function openTemplateUI(guid, tab, opts, store, tid, tname, run_url, op_url, results_url) {
	if(!opts.editor_mode)
		openFormTemplateInPanel(guid, tab, opts, store, tid, tname, run_url, op_url, results_url);
	else
		openTemplateEditorInPanel(guid, tab, opts, store, tid, tname, run_url, op_url);
}

function setupTemplateRenderer(guid, tab, opts, tid, tname, run_url, op_url, results_url) {
	Ext.apply(tab, {
		loader: {
			loadMask: true,
			renderer: function(loader, response, req) {
				//try {
					eval('var store = '+response.responseText);
					if(store) {
						tab.removeAll();
						openTemplateUI(guid, tab, opts, store, tid, tname, run_url, op_url, results_url);
						//tab.doLayout(false, true);
					}
				/*}
				catch (e) {
					console.log(e);
					//el.update(e, false, cb);
				}*/
			}
		}
	});
}

function openTemplate(tid, tname, opts, guid, tabPanel, treePanel, op_url, run_url, results_url, path, tellMePanel) {
	// Check if tab is already open
	var items = tabPanel.items.items;
	for(var i=0; i<items.length; i++) {
		var tab = items[i];
		if(tab && tab.title==tname) {
			tabPanel.setActiveTab(tab);
			return null;
		}
	}

	var tab = getTemplatePanel(tabPanel, treePanel, opts, tid, tname, run_url, op_url, tellMePanel);
	Ext.apply(tab.mainTab, {path:path});
	tab.cmap = treePanel.cmap;

	tabPanel.setActiveTab(tab.mainTab);
	setupTemplateRenderer(guid, tab, opts, tid, tname, run_url, op_url, results_url);

	// Fetch Template via Ajax
	var fetchOp = opts.editor_mode?'getEditorJSON':'getViewerJSON';
	var url = op_url+'&op='+fetchOp+'&template_id='+tid;
	tab.getLoader().load({url: url});
}


function getTreePath(node, field, separator) {
   field = field || node.idProperty;
   separator = separator || '/';

   var path = [node.get(field)],
   parent = node.parentNode;

   while (parent) {
       path.unshift(parent.get(field));
       parent = parent.parentNode;
   }
   return separator + path.join(separator);
}

function getTemplatesListTree(guid, tabPanel, opts, templateList, run_url, op_url, results_url, tellMePanel) {
	var tmp = [];
	for(var i=0; i<templateList.length; i++) {
		tmp.push({
			id:templateList[i],
			text:getTemplateText(templateList[i]),
			iconCls: 'wflowIcon',
			leaf:true
		});
	}
	if(!Ext.ModelManager.isRegistered('treeRecord'))
		Ext.define('treeRecord', {extend:'Ext.data.Model', fields:['text']});

	var treeStore = Ext.create('Ext.data.TreeStore', {
		model: 'treeRecord',
		root: { text:'Templates', expanded: true, children: tmp }
	});
	
	var tbar = null;
	var treeScope = {};
	if(opts.editor_mode) {
		tbar = [{text: 'New Template',iconCls: 'addIcon',
			handler: function() {
				createNewTemplate(guid, opts, tabPanel, treePanel, op_url, run_url, results_url, tellMePanel);
			}
		},'-',
		{text: 'Delete Template',iconCls: 'delIcon',
			handler: function() {
				var nodes = treeScope.tree.getSelectionModel().getSelection();
				if(!nodes || !nodes.length) { return; }
				var node = nodes[0];
				if(!node) { return; }
				var tname = node.data.text;
				var tid = getRDFID(tname);
				Ext.MessageBox.confirm("Confirm Delete", 
					"Are you sure you want to Delete "+tname, 
					function(b) {
						if(b == "yes") {
							deleteTemplate(tid, tname, tabPanel, treePanel, op_url);
						}
					}
				);
			}
		}];
	}

	var treePanel = new Ext.tree.TreePanel({
		width: '100%',
		border: false,
		rootVisible: false,
		defaults: { bodyStyle: 'margin-left:0px;padding-left:0px' },
		autoScroll: true,
		//bodyCls: 'hrefTree',
		bodyCls: 'x-docked-noborder-top',
		iconCls: 'wflowIcon',
		containerScroll: true,
		store: treeStore,
		url: op_url,
		tbar: tbar
	});
	treeScope.tree = treePanel;

	//var ts = new Ext.tree.TreeSorter(treePanel);
	treePanel.on("itemclick", function(view, rec, item, ind, event) { 
		if(!rec.parentNode) return false;
		//if(!node.leaf) { return false;}
		var tname = rec.data.text;
		var tid = rec.data.id;
		//var path = node.getPath();
		var path = getTreePath(rec);
		openTemplate(tid, tname, opts, guid, tabPanel, treePanel, op_url, run_url, results_url, path, tellMePanel);
	});

	tabPanel.on('tabchange', function(tp, tab){
		if(tab.path) treePanel.selectPath(tab.path);
		if(tellMePanel) {
			var layout = tellMePanel.histories.getLayout();
			if(tab.tellMeHistory)
				layout.setActiveItem(tab.tellMeHistory.getId());
		}
	});

	tabPanel.on('remove', function(tp, tab){
		if(tellMePanel && tab.tellMeHistory) {
			tellMePanel.histories.remove(tab.tellMeHistory);
		}
	});

	/*tabPanel.on('beforetabchange', function(tp, tab, oldtab){
		if(opts.editor_mode=="tellme" && tellMePanel) {
			if(!tellMePanel) return true;
			if(!oldtab) return true;
			if(oldtab.graphPanel) {
				oldtab.root = getTellMeRoot(tellMePanel);
			}
			if(!tab.root) return true;
			setTellMeRoot(tellMePanel, tab.root)
		}
	});*/

	return treePanel;
}

function openFormTemplateInPanel(guid, templatePanel, opts, store, tid, tname, run_url, op_url, results_url, extraHeight) {
	templatePanel.removeAll();
	var graph;
	var seedForm;
	var gridPanel;

	var mainPanel = new Ext.Panel({layout:'border', border:false});

	if(!opts.hide_constraints) {
		gridPanel = getConstraintsTableViewer(store);
		Ext.apply(gridPanel, {region:'north', bodyCls:'inactive-grid'});
	}
	if(!opts.hide_form) {
		seedForm = getSeedForm(tid, store.inputs, templatePanel, run_url, op_url, results_url);
		var region = opts.hide_graph ? 'center' : 'north';
		Ext.apply(seedForm, {region:region});
		mainPanel.add(seedForm);
	}
	if(!opts.hide_graph) {
		var infoPanel = new Ext.Panel({
			region: 'south',
			//layout: 'fit',
			border: false,
			//height: 200,
			autoHeight: true,
			bodyStyle: 'padding:5px;padding-top:0px',
			bodyCls:'transparent-panel',
			split: true,
			preventHeader: true,
			animCollapse: false,
			collapsible: true,
			collapsed: true,
			collapseMode: 'mini'
		});

		graph = getTemplateGraph(guid, tid, store, templatePanel, infoPanel, gridPanel, op_url, false, null);
		Ext.apply(graph, {region:'center'});

		//var margin = '0 5 5 5';
		//if(opts.hide_form) margin='5 5 5 5';

		var graphPanel = new Ext.Panel({
    		//margins: margin,
			title: (opts.hide_form && !opts.hide_title)?'Workflow: '+tname:'',
			border: false,
			region: 'center',
			layout: 'border'
		});
		if(gridPanel) graphPanel.add(gridPanel);
		if(graph) graphPanel.add(graph);
		if(infoPanel) graphPanel.add(infoPanel);

		mainPanel.add(graphPanel);
	}
	if(graph) {
		templatePanel.graph = graph;
	    if(seedForm)
		    seedForm.graph = graph;
	}


	if(!opts.hide_documentation) {
		var meta = store.template.metadata;
		var docViewer = new Ext.Panel();
		var docid = guid+"_"+tid+"_doc";
		templatePanel.mainTab.remove(docid);

		var docViewerPanel = new Ext.Panel({
			title: 'Documentation',
			layout: 'border',
			id: docid,
			iconCls: 'docsIcon',
			items: [
				{
					region: 'north',
					margins: '5 5 5 5',
					bodyStyle: 'background:transparent; padding:5px',
					autoHeight: true,
					html: ("<b>Author:</b> "+meta.contributor+"<br/><b>Last Updated:</b> "+parseXSDDateString(meta.lastupdate))
				},
				{
					region: 'center',
					margins: '5 5 5 5',
					autoScroll: true,
					bodyCls: 'docsDiv',
					html: meta.documentation
				}
			]
		});
		templatePanel.mainTab.add(docViewerPanel);
		templatePanel.add(mainPanel);
	}
}

function parseXSDDateString(dateString) {
	if(!dateString) return null;
	var re = /(-)?(\d{4})-(\d{2})-(\d{2})(T(\d{2}):(\d{2}):(\d{2})(\.\d+)?)?(([\+-])((\d{2}):(\d{2})))?/;
	return eval(dateString.replace(re,'new Date(new Date(\'$3/$4/$2 $6:$7:$8 GMT$11$13$14\').getTime() + (1000 * 0$9))'));
}


function getConstraintsTableViewer(store) {
	var varMaps = [];
	for(var i=0;i<store.template.variables.length;i++) {
		varMaps[store.template.variables[i].id] = 1;
	}
	store.template.constraints = replaceConstraintObjects(store.template.constraints, varMaps, true);

	if(!Ext.ModelManager.isRegistered('Triple'))
		Ext.define('Triple', {extend: 'Ext.data.Model', fields: ['s', 'p', 'o']});

	var viewerTripleStore = new Ext.data.Store({
		model:'Triple',
		reader: {type:'array'},
		sorters: [{field:'s', direction:'ASC'}]
	});
	viewerTripleStore.loadData(store.template.constraints);

	/*var cm = new Ext.grid.ColumnModel({
		defaults: { sortable: true },
	});*/
	var height = 0;
	var border = 0;
	if(store.template.constraints.length) {
		height = (store.template.constraints.length+2)*20+10;
		border = 1;
		if(height > 120) height=120;
	}

	var gridPanel = new Ext.grid.Panel({
		store: viewerTripleStore,
		columns: [ 
			{dataIndex: 's', header: 'Variable', menuDisabled:true},
			{dataIndex: 'p', header: 'Constraint', menuDisabled:true},
			{dataIndex: 'o', header: 'Value', menuDisabled:true}
		],
		//colModel: cm, 
		forceFit: true,
		autoScroll: true,
		border: false,
		split: border?true:false,
		collapsible: true,
		//header: height?true:false,
		title: 'Constraints: All',
		collapseMode: 'mini',
		bodyStyle:"border-width:0 0 "+border+"px 0",
		columnLines: true,
		height: height
	});
	return gridPanel;
}

function replaceConstraintObjects(constraints, varMaps, isViewer) {
	for(var i=0;i<constraints.length;i++) {
		var cons = constraints[i];
		if(cons.o.match(/"+(.+?)"+\^+.+/)) {
			cons.o = cons.o.replace(/"+(.+?)"+\^+.+/, "$1");
		} else if(!varMaps[cons.o] && !cons.o.match(/:.+/)) {
			cons.o = "?"+cons.o;
		}

		if(!varMaps[cons.s] && !cons.o.match(/:.+/)) {
			cons.s = "?"+cons.s;
		}

		if(isViewer) {
			cons.s = cons.s.replace(/.+:/,'');
			cons.p = cons.p.replace(/.+:/,'');
			cons.o = cons.o.replace(/.+:/,'');
		}
		constraints[i] = cons;
	}
	return constraints;
}

function getConstraintsTable(store) {
	var propvals = store.propvals;
	var proprange = [];
	var props = [];
	var vars = [];
	for(var prop in propvals) {
		props.push(prop);
		proprange[prop] = propvals[prop].range;
	}
	var varMaps = [];
	for(var i=0;i<store.template.variables.length;i++) {
		varMaps[store.template.variables[i].id] = 1;
		vars.push(store.template.variables[i].id);
	}

	store.template.constraints = replaceConstraintObjects(store.template.constraints, varMaps);

	vars.sort();
	props.sort();

	if(!Ext.ModelManager.isRegistered('Triple'))
		Ext.define('Triple', {extend: 'Ext.data.Model', fields: ['s', 'p', 'o']});

	var editorTripleStore = new Ext.data.Store({
		model:'Triple'
	});
	editorTripleStore.loadData(store.template.constraints);

	var opts = {triggerAction:'all', typeAhead:true, lazyRender:true, lazyInit:true,
		//listStyle:'font-size:11px', 
		//style:'font-size:11px'+(Ext.isGecko ? '': ';padding-top:0px;'), 
		forceSelection:true, allowBlank:false};

	var valsStore = Ext.create('Ext.data.Store', {fields: ['text']});
	var varEditor = new Ext.grid.CellEditor({field: new Ext.form.ComboBox({store:vars})});
	var propsEditor = new Ext.grid.CellEditor({field: new Ext.form.ComboBox({store:props})});
	var valsEditor = new Ext.grid.CellEditor({field: new Ext.form.ComboBox({store:valsStore, displayField:'name', valueField:'name', queryMode:'local'})});

	var txtEditor = new Ext.grid.CellEditor({field: new Ext.form.TextField({style:'font-size:11px;padding-top:0px',allowBlank:false})});
	var numEditor = new Ext.grid.CellEditor({field: new Ext.form.NumberField({allowDecimals:false,style:'font-size:11px;padding-top:0px',allowBlank:false})});
	var floatEditor = new Ext.grid.CellEditor({field: new Ext.form.NumberField({style:'font-size:11px;padding-top:0px',allowBlank:false,decimalPrecision:6})});
	var boolEditor = new Ext.grid.CellEditor({field: new Ext.form.field.Checkbox({style:'font-size:11px;padding-top:0px',allowBlank:false})});

	Ext.apply(varEditor.field,opts);
	Ext.apply(propsEditor.field,opts);
	Ext.apply(valsEditor.field,opts);

	var mapfn = function(a) {
		return {name:a};
	};
	var valueEditorFn = function(rec) {
		var pred = rec.get('p');
		if(pred) {
			var val = propvals[pred];
			if(val && val.range) {
				var range = val.range;
				if(range.match(/^xsd:/)) {
					if(range == "xsd:float" || range == "xsd:double") return floatEditor;
					if(range == "xsd:int" || range == "xsd:integer" || range == "xsd:number") return numEditor;
					if(range == "xsd:boolean" || range == "xsd:bool") return boolEditor;
					//editorTripleStore.getAt(rowIndex).set('o', null);
					return txtEditor;
				} else if(range == "wflow:DataVariable") {
					return varEditor;
				} else {
					val.values.sort();
					var values = Ext.Array.map(val.values, mapfn);
					valsStore.loadData(values);
					return valsEditor;
				}
			}
		}
		return txtEditor;
	};

	var selectedRow;
	var selModel = Ext.create('Ext.selection.CheckboxModel', {
		checkOnly:true,
		listeners: {
			selectionchange: function(sm, selections) {
				gridPanel.down('#delConstraint').setDisabled(selections.length == 0);
			}
		}
	});
	var editorPlugin = Ext.create('Ext.grid.plugin.CellEditing', { clicksToEdit:1 });

	var gridPanel = Ext.create('Ext.grid.Panel', {
		store: editorTripleStore,
		selModel: selModel,
		columns: [ 
			{dataIndex: 's', header: 'Variable', flex:1, editor:varEditor, menuDisabled:true},
			{dataIndex: 'p', header: 'Constraint', flex:1, editor:propsEditor, menuDisabled:true},
			{dataIndex: 'o', header: 'Value', editable:true, flex:1, getEditor:valueEditorFn, menuDisabled:true}
		],
		split: true,
		border: false,
		autoScroll: true,
		columnLines: true,
		collapsible: true,
		title: 'Constraints: All',
		//collapsed: true,
		collapseMode: 'mini',
		height: 150,
		proprange: proprange,
		plugins: [ editorPlugin ],
		tbar: [
		{
			text: 'Add Constraint',
			iconCls:'addIcon',
			handler : function(){
				//	access the Record constructor through the grid's store
				var p =  new Triple({s:'',p:'',o:''});
				var pos = editorTripleStore.getCount();
				editorPlugin.cancelEdit();
				editorTripleStore.insert(pos, p);
				//selModel.selectRow(0);
				editorPlugin.startEditByPosition({row:pos, column:1});
			}
		},'-',{
			text: 'Delete Constraint',
			itemId: 'delConstraint',
			iconCls: 'delIcon',
			disabled: true,
			handler : function(){
				//	access the Record constructor through the grid's store
				var p =  new Triple({s:'',p:'',o:''});
				editorPlugin.cancelEdit();
				var s = gridPanel.getSelectionModel().getSelection();
				for(var i=0, r; r=s[i]; i++) editorTripleStore.remove(r);
			}
		}]
	});

	/*gridPanel.on('afterrender', function() {
		gridPanel.determineScrollbars();
	});*/

	gridPanel.variableStore = varEditor.field.store;
	return gridPanel;
}


function openTemplateEditorInPanel(guid, templatePanel, opts, store, tid, tname, run_url, op_url) {
	templatePanel.removeAll();
	var graph;
	var gridPanel;

	var graphPanel = new Ext.Panel({
		title: opts.hide_constraints?'Workflow: '+tname:'', 
		region: 'center',
		layout: 'border',
		border: false
	});

	if(!opts.hide_constraints) {
		gridPanel = getConstraintsTable(store);
		Ext.apply(gridPanel, {region:(opts.hide_graph?'center':'north')});
		graphPanel.add(gridPanel);
	}

	if(!opts.hide_graph) {
		//var infoPanel = null;
		var infoPanel = new Ext.Panel({
			region: 'south',
			//layout: 'fit',
			preventHeader: true,
			border: false,
			animCollapse: false,
			autoHeight: true,
			collapsible: true,
			collapsed: true,
			collapseMode: 'mini'
		});

		graph = getTemplateGraph(guid, tid, store, templatePanel, infoPanel, gridPanel, op_url, true, opts.editor_mode);
		Ext.apply(graph, {region:'center'});

		if(graph) graphPanel.add(graph);
		if(infoPanel) graphPanel.add(infoPanel);

		templatePanel.mainTab.graphPanel = graph;
		templatePanel.mainTab.constraintsTable = gridPanel;
	}
	templatePanel.add(graphPanel);

	if(!opts.hide_documentation) {
		var docEditor = new Ext.form.HtmlEditor({
			region: 'center',
			margins: '5 5 5 5',
			xtype: 'htmleditor',
			enableFont: false,
			bodyCls: 'docsDiv',
			value: store.template.metadata.documentation
		});

		var docid = guid+"_"+tid+"_doc_ed";
		templatePanel.mainTab.remove(docid);

		var docEditorPanel = new Ext.Panel({
			title: 'Documentation',
			id: docid,
			layout: 'border',
			iconCls: 'docsIcon',
			items: docEditor
		});
		templatePanel.mainTab.add(docEditorPanel);
		templatePanel.mainTab.doc = docEditor;
	}

	if(opts.editor_mode=="tellme") {
		var tellme = store.template.metadata.tellme;
		var tellMePanel = templatePanel.tellMePanel;
		if(!tellMePanel) return;
		tellMePanel.leftTabPanel.setActiveTab(tellMePanel);

		var layout = tellMePanel.histories.getLayout();
		if(typeof(layout) == "string") return;
		var history = new TellMe.HistoryPanel({region:'center', border:false, tid:tid, tname:tname});
		history.templatePanel = templatePanel;

		tellMePanel.histories.add(history);
		layout.setActiveItem(history.getId());

		templatePanel.mainTab.tellMeHistory = history;

		if(tellme && tellMePanel) {
			var tree = Ext.decode(tellme);
			loadTellMeHistory(tellMePanel, tree);
		}
	}
}

function showError(msg) {
	Ext.Msg.show({
		icon:Ext.MessageBox.ERROR, 
		buttons:Ext.MessageBox.OK, 
		msg:msg
	});
}

function saveActiveTemplateAs(tabPanel, treePanel, tellMePanel, op_url) {
	var tab = tabPanel.getActiveTab();
	var tname = tab.title;
	Ext.Msg.prompt("Save As..", "Enter name for the template:",function(btn, text) {
			if(btn == 'ok') {
				if(text) 
					saveActiveTemplate(tabPanel, treePanel, tellMePanel, op_url, text); 
				else
					Ext.Msg.show({msg:"Give a name please"});
			}
		},window,false);
}

function saveTemplateStore(template, consTable) {
	template.saveToStore();
	var constraints = [];
	consTable.store.clearFilter();
	consTable.store.data.each(function() { 
		var data = {s:this.data.s, p:this.data.p, o:this.data.o};
		if(data.o && data.s && data.p) {
			var range = consTable.proprange[data.p];
			if(data.o.match(/^\?/)) data.o = data.o.replace(/^\?/,'');
			else if(range.match(/^xsd:/)) data.o = '"'+data.o+'"^^'+range;
			constraints.push(data); 
		}
	});
	template.store.constraints = constraints;
}

function getTemplateStoreForTab (tab) {
	var template = tab.graphPanel.editor.template;
	if(!template) return null;

	template = template.createCopy();
	var consTable = tab.constraintsTable;
	var numErrors = 0;
	for ( err in template.errors ) numErrors++;
	if(numErrors) {
		showError("There are errors in the current template that are marked in red. Please fix them before continuing");
		_console(template.errors);
		return null;
	}
	saveTemplateStore(template, consTable);
	template.store.metadata = {};
	if(tab.doc) {
		template.store.metadata.documentation = tab.doc.getValue();
	}
	return template.store;
}

function saveActiveTemplate(tabPanel, treePanel, tellMePanel, op_url, tname) {
	var tab = tabPanel.getActiveTab();
	if(!tname) { tname = tab.title; }
	tid = getRDFID(tname);
	if(tname == 'TellMeTemplate') {
		showError("Cannot overwrite 'TellMeTemplate'. Try and <b>Save As</b> another template");
		return false;
	}
	// window.console.log("Saving as "+tid);

	var store = getTemplateStoreForTab(tab);
	if(!store) return;

	if(tellMePanel) {
		if(store.metadata) store.metadata.tellme = getTellMeHistory(tellMePanel);
		else store.tellme = getTellMeHistory(tellMePanel);
	}

	var imagedata = tab.graphPanel.editor.getImageData(1, false);

	var url = op_url+'&op=saveTemplateJSON&template_id='+tid;
	var msgTarget = Ext.get(tab.getId());
	msgTarget.mask('Saving...', 'x-mask-loading');
	Ext.Ajax.request({ 
		url: url,
		params: {json: Ext.encode(store), imagedata:imagedata},
		success: function(response) {
			msgTarget.unmask();
			if(response.responseText != "OK") {
				showError(response.responseText);
			} else {
				if(tname == tab.title) {
					Ext.each(tab.constraintsTable.getStore().getUpdatedRecords(), function(rec) { rec.commit(); });
				}
				var node = treePanel.getStore().getNodeById(tid);
				if(!node) {
					var ptab = treePanel.findParentByType('tabpanel');
					ptab.setActiveTab(treePanel);
					var node = {id:tid,text:tname,iconCls:'wflowIcon',leaf:true};
					treePanel.getRootNode().appendChild(node);
				}
			}
		},
		failure: function(response) {
			msgTarget.unmask();
			showError(response.responseText);
		}
	});
}


function inferElaboratedTemplate(tabPanel, op_url, store) {
	var tab = tabPanel.tabBar ? tabPanel.getActiveTab() : tabPanel.templatePanel.mainTab;
	var tname = tab.title;
	var tid = getRDFID(tname);

	if(!store) store = getTemplateStoreForTab(tab);
	if(!store) return;

	//window.console.log(Ext.encode(template.store));
	var url = op_url+'&op=elaborateTemplateJSON&template_id='+tid;
	var msgTarget = Ext.get(tab.getId());
	msgTarget.mask('Elaborating...', 'x-mask-loading');
	Ext.Ajax.request({ 
		url: url,
		params: {json: Ext.encode(store)},
		success: function(response) {
			msgTarget.unmask();
			try {
				eval('var store = '+response.responseText);
				if(!store) {
					if(window.console) window.console.log(response.responseText);
				} else if(store.error) {
					if(window.console) window.console.log(store);
                    showWingsError("Wings couldn't elaborate the template: "+tname, "Error in "+tname, store);
				} else {
					//if(window.console) window.console.log(store);
					var varMaps = [];
					for(var i=0;i<store.template.variables.length;i++) {
						varMaps[store.template.variables[i].id] = 1;
					}
					store.template.constraints = replaceConstraintObjects(store.template.constraints, varMaps);
					tab.constraintsTable.store.loadData(store.template.constraints);
					tab.graphPanel.reloadGraph(store.template);
					showWingsMessage("Wings has elaborated your template. Please Save to make your changes persistent", tname, store); 
				}
			}
			catch (e) {
				if(window.console) window.console.log(response.responseText);
				showWingsError(response.responseText, "Error in "+tname, {explanations:[]}); 
			}
		},
		failure: function(response) {
			msgTarget.unmask();
			showError(response.responseText);
		}
	});
}

function getTemplatePanel(mainPanel, treePanel, opts, tid, tabname, run_url, op_url, tellMePanel) {
	var toolbar = null;
	var tab = new Ext.Panel({
		region: 'center',
		layout: 'fit'
	});

	if(opts.editor_mode) {
		toolbar = [];
		//if(opts.editor_mode != 'tellme') {
			toolbar.push({
				text:'Save',
				iconCls:'saveIcon',
				handler: function() { saveActiveTemplate(mainPanel, treePanel, tellMePanel, op_url); }
			});
		//}

		toolbar = toolbar.concat([{
			text:'Save As',
			iconCls:'saveAsIcon',
			handler: function() { saveActiveTemplateAs(mainPanel, treePanel, tellMePanel, op_url); }
		},'-',{
			text:'Elaborate Template',
			iconCls:'inferIcon',
			handler: function() { inferElaboratedTemplate(mainPanel, op_url); }
		},'-', {xtype:'tbfill'},
		{
			iconCls:'reloadIcon',
			text:'Reload',
			handler: function() {
				var fetchOp = opts.editor_mode?'getEditorJSON':'getViewerJSON';
				var url = op_url+'&op='+fetchOp+'&template_id='+tid;
				tab.getLoader().load({url: url});
				if(tellMePanel) tellMePanel.clear();
			}
		}]);
	}

	if(!opts.hide_documentation) {
		var lowerTabs = new Ext.TabPanel({
			iconCls: 'wflowIcon',
			closable: !opts.hide_selector,
			region: 'center',
			title: tabname,
			//margins: opts.hide_selector?'5 5 5 5':'5 5 5 0',
			tabBar: { bodyCls: 'no-border-tabs' },
			tabPosition: 'top',
			activeTab: 0,
			tbar: toolbar,
			items: [tab]
		});
		lowerTabs.setActiveTab(tab);
		tab.setTitle("Template");
		mainPanel.add(lowerTabs);
		tab.mainTab = lowerTabs;
	}
	else {
		tab = new Ext.Panel({
			layout: 'border',
			region: 'center',
			title: tabname,
			tbar: toolbar,
			iconCls: 'wflowIcon',
			closable: !opts.hide_selector
		});
		mainPanel.add(tab);
		tab.mainTab = tab;
	}

	if(tellMePanel) {
		tab.tellMePanel = tellMePanel;
	}

	return tab;
}

// Must be called within Ext.onReady
function initializeTemplateBrowser(guid, panelid, width, height, opts, store, tid, tname, domain, run_url, op_url, results_url) {
	var mainPanel  = new Ext.Panel({
		layout: 'border', 
		width:width, height:height,
		monitorResize:true,
		items: []
	});
	if(Ext.isIE) {
		mainPanel.add({
				region:'north', 
            autoHeight:true,
            html:'<span style="color:#666;font-size:10px"><b style="color:maroon">Performance Warning:</b> '+
                 'You are using Internet Explorer. There are known '+
                 'performance issues when using IE to access our portal (Specifically the Template Graphing tool). '+
                 'Please use the latest version of any one of the following browsers instead: '+
                 'Firefox, Chrome, Safari, or Opera. Apologies for any inconvenience</span>',
            padding:5
		});
	}

	var leftPanel;
	var tellMePanel;

	var cmap = getChildComponentsMap(store.components);

	if(!opts.hide_selector) {
		// Create an area on the left for the treepanel
		leftPanel = new Ext.TabPanel({
			region: 'west',
			collapsible: true,
			collapseMode: 'mini',
			preventHeader:true, 
			width: (opts.editor_mode == 'tellme' ? 400 : 240),
			split: true,
			margins: '5 0 5 5',
			//cmargins: '5 5 5 5',
			//title:'Domain - '+domain,
			//title:'All Workflows',
			activeTab: 0
		});

		// Add the template tabPanel in the center
		var tabPanel = new Ext.TabPanel({
			region: 'center', 
			margins: '5 5 5 0',
			resizeTabs: true,
			enableTabScroll: true,
			//minTabWidth: 120,
			//maxTabWidth: 200,
			//tabWidth: 120,
			activeTab: 0,
			items: [{ 
				//layout: 'fit',
				title: opts.editor_mode?'Template Editor':'Template Browser',
				autoLoad: { url: op_url+'&op='+(opts.editor_mode?'editor_intro':'browser_intro') }
			}]
		});

		// Add the TellMe Panel
		if(opts.editor_mode == 'tellme') {
			tellMePanel = getTellMePanel(guid, tid, tname, tabPanel, mainPanel, opts, store, run_url, op_url);
			tellMePanel.leftTabPanel = leftPanel;
			leftPanel.add(tellMePanel);
		}

		// Add the template tree list to the leftPanel
		var templateTree = getTemplatesListTree(guid, tabPanel, opts, store.tree, run_url, op_url, results_url, tellMePanel);
		templateTree.setTitle('Templates');
		templateTree.cmap = cmap;

		leftPanel.add(templateTree);
		leftPanel.setActiveTab(0);

		if(opts.editor_mode) {
			var cPanel = new Ext.TabPanel ({
				iconCls:'compIcon', 
				title:'Components', 
				activeTab:0, 
				tabBar: { bodyCls: 'no-border-tabs' },
				tabPosition: 'top'
			});
			cPanel.add(getComponentListTree(guid, tabPanel, store.components, op_url, true));
			cPanel.add(getComponentInputsTree(guid, tabPanel, store.components, op_url, true));
			cPanel.add(getComponentOutputsTree(guid, tabPanel, store.components, op_url, true));
			cPanel.setActiveTab(0);
			leftPanel.add(cPanel);
		}

		mainPanel.add(leftPanel);
		mainPanel.add(tabPanel);
		
	}
	else if(tid && opts.hide_selector) {
		var templatePanel = getTemplatePanel(mainPanel, null, opts, tid, tname, run_url, op_url, tellMePanel);
		templatePanel.title = tid;
		templatePanel.cmap = cmap;
		if(store.template) 
			openTemplateUI(guid, templatePanel, opts, store, tid, tname, run_url, op_url, results_url);

		setupTemplateRenderer(guid, templatePanel, opts, tid, tname, run_url, op_url, results_url);
		mainPanel.templatePanel = templatePanel;
	}

	mainPanel.on("afterrender", function() {
		//if(leftPanel) leftPanel.doLayout(true,true);
		if(tellMePanel) tellMePanel.tellme.focus();

		if(tid && !opts.hide_selector) {
			var path = null;
			if(templateTree && templateTree.getStore().getNodeById(tid)) {
				path = getTreePath(templateTree.getStore().getNodeById(tid));
				templateTree.selectPath(path);
			}
			openTemplate(tid, tname, opts, guid, tabPanel, templateTree, op_url, run_url, results_url, path, tellMePanel);
		}
	});

	if(panelid)
		mainPanel.render(Ext.get(panelid));

	//mainPanel.doLayout();
	return mainPanel;
}
