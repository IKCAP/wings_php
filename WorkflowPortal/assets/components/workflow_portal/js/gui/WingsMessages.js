function getWingsDebugOutput(data) {
	if(data.output) {
		return "<pre><b>Debug Output:</b> <br/>\n" + data.output + "</pre>";
	}
	return "";
}

function _console(msg) {
	if(window.console) window.console.log(msg);
}

function getWingsExplanation(data) {
	var exps = Ext.Array.map(data.explanations, function(a) { return {text:a}; });
	var exp = new Ext.grid.GridPanel({
		columns: [{dataIndex:'text', menuDisabled:true}],
		hideHeaders: true,
		region: 'south',
		title: 'Explanation',
		collapsible: true,
		animCollapse: false,
		hideCollapseTool: true,
		collapseMode: 'mini',
		collapsed: true,
		forceFit:true,
		viewConfig: {
			getRowClass: function(record, rowIndex, rp, ds){ 
				if(record.data.text.match(/ERROR/i)) {
					return "errorCls";
				}
				if(record.data.text.match(/INFO/i)) {
					return "infoCls";
				}
				if(record.data.text.match(/Suggest/i)) {
					return "suggestCls";
				}
			}
		},
		bodyCls:'multi-line-grid',
		height: 200,
		border: false,
		split: true,
		autoScroll: true,
		store: new Ext.data.Store( {fields:['text'], data:exps} ),
	});
	return exp;
}

function showWingsError(msg, title, data) {
	if(window.console) window.console.log(data.output);
	var win = new Ext.Window({
		layout:'border',
		title:title?title:"Error in Wings..",
		autoScroll:true,
		constrain:true,
		maximizable:true,
		width:600, height:450, 
		items: {
			region: 'north',
			autoHeight: true,
			padding: 5,
			border: false,
			bodyStyle: 'color:white;background-color:'+(data.error?'red':'darkorange'),
			html: msg
		}
	});

	if(data.explanations && data.explanations.length) {
		var explanationGrid = getWingsExplanation(data);
		Ext.apply(explanationGrid, {region:'center', collapsed:false});
		win.add(explanationGrid);
	}
	else {
		win.add({region:'center', xtype: 'textarea', border:false, readOnly: true, value:data.output});
	}

	win.show();
}

function formatDataBindings(value, meta, record, rowind, colind, store, view) {
	var s = "";
	for(var i=0; i<value.length; i++) {
		s += "<div>"+value[i]+"</div>";
	}
	return s;
}

function formatAlternatives(value, meta, record, rowind, colind, store) {
	var s = "";
	for(var key in value) {
		s += "<div>"+key+"="+value[key]+"</div>";
	}
	return s;
}

function showWingsBindings(data, title, formItems, type) {
	//if(window.console) window.console.log(data);
	var bindings = data.bindings;
	if(!bindings || bindings.length == 0) {
   	return showWingsError("Wings couldn't find Data for the template", title, data);
	}

	// Get Binding Data
	var cols = [];
	var fields = [];
	for(var i=0; i<formItems.length; i++) {
	   var item = formItems[i];
		//if(item.wtype == type && !item.getValue())
		if(item.wtype == type) {
			var name = item.name;
			cols.push({dataIndex:name, header:name, renderer: formatDataBindings, sortable:true, menuDisabled:true});
			fields.push(name);
		}
	}
	var myStore = new Ext.data.Store({
		reader: {type: 'json'},
		fields: fields,
		data: bindings
	});

	var win = new Ext.Window({
		layout:'border',
		title:title,
		constrain:true,
		maximizable:true,
		autoScroll:true,
		//autoWidth:true, 
		width:600,
		height:450 
	});

	// Binding Grid Panel
	var bindingsGrid = new Ext.grid.GridPanel({
		columns: cols,
		region: 'center',
		forceFit:true,
		border: false,
		columnLines: true,
		autoScroll: true,
		bodyCls:'multi-line-grid',
		store:myStore,
		sm: new Ext.selection.RowModel({singleSelect:true}),
		tbar:[{
			text:'Use Selected '+type,
			iconCls:'selectIcon',
			handler: function() {
				var recs = bindingsGrid.getSelectionModel().getSelection();
				if(!recs || !recs.length) {
					Ext.Msg.show({msg:'Select some '+type+' combination from below and then press Select', modal:false});
					return;
				}
				var myRec = recs[0];
				for(var i=0; i<formItems.length; i++) {
					var item = formItems[i];
					var val = myRec.get(item.name);
					if(!val) continue;
					//valstr = val.join(',');
					item.setValue(val);
				}
				win.close();
			}
		}]
	});

	var explanationsGrid = getWingsExplanation(data);
	explanationsGrid.on("expand", function() {
		explanationsGrid.determineScrollbars();
	});

	win.add({
		region:'center',
		layout:'border',
		items:[ 
			bindingsGrid,
			{
				xtype:'button', region:'south', text: 'Show/Hide Explanations', handler: function() {
					if(explanationsGrid.isVisible()) { explanationsGrid.collapse(); }
					else { explanationsGrid.expand(); }
				}
			}
		]
	});
	win.add(explanationsGrid);
	win.show();
}

function showWingsRanMessage(tid, runid, requestid, results_url) {
	var msg = "Workflow: "+tid+" has been submitted for execution [Run id: "+runid+"]!<br/><br/>"+
		"You can monitor Workflow Execution from the 'Access Results' page in the Analysis Menu";

	var win = new Ext.Window({
		layout:'border',
		constrain:true,
		maximizable:true,
		title:"Workflow Submitted",
		autoScroll:true,
		width:400, 
		height:150,
		items: [{
			region: 'center',
			padding: 5,
			border: false,
			html: msg
		},
		{
			xtype: 'button',
			region: 'south',
			text: 'CLICK HERE to Monitor Execution',
			iconCls: 'status_SUCCESS',
			handler: function() {
				var w = window.open(results_url+'&run_id='+runid, '_accessResults');
				win.close();
			}
		}]
	});
	win.show();
}

function showWingsAlternatives(tid, data, run_url, results_url) {
	var alternatives = data.alternatives;
	//if(window.console) window.console.log(alternatives);

	var title = "Select an Executable Template to Run..";
	if(!alternatives || alternatives.length == 0) {
   	return showWingsError("Wings couldn't find any Executable Templates", title);
	}
	for(var i=0; i<alternatives.length; i++) {
		var alt = alternatives[i];
		if(alt.param && alt.param.length == 0) alt.param = null;
	}
	Ext.define('wingsAlternatives', {
		extend:'Ext.data.Model', 
		fields: [
			{name:'index', mapping:'index'},
			{name:'comp', mapping:'comp'},
			{name:'data', mapping:'data'},
			{name:'param', mapping:'param'}
		]
	});
	var myStore = new Ext.data.Store({
		model: 'wingsAlternatives',
		reader: {type:'json'}
	});
	myStore.loadData(alternatives);

	var win = new Ext.Window({
		layout:'border',
		title:title,
		constrain:true,
		maximizable:true,
		autoScroll:true,
		//autoWidth:true, 
		width:600,
		height:450 
	});

	// Alternatives Grid Panel
	var alternativesGrid = new Ext.grid.GridPanel({
		columns: [
			{dataIndex:'comp', header:'Component Bindings', renderer: formatAlternatives, menuDisabled:true},
			{dataIndex:'data', header:'Data Bindings', renderer: formatAlternatives, menuDisabled:true},
			{dataIndex:'param', header:'Parameter Bindings', renderer: formatAlternatives, menuDisabled:true}
		],
		region: 'center',
		forceFit:true,
		border: false,
		columnLines: true,
		autoScroll: true,
		bodyCls:'multi-line-grid',
		store:myStore,
		sm: new Ext.selection.RowModel({singleSelect:true}),
		tbar:[{
			text:'Run Selected Executable Template',
			iconCls:'runIcon',
			handler: function() {
				var recs = alternativesGrid.getSelectionModel().getSelection();
				if(!recs || !recs.length) {
					Ext.Msg.show({msg:'Select a Template from below and then press Run', modal:false});
					return;
				}
				var myRec = recs[0];
				var url = run_url+'&op=run_alternative';
				var msgTarget = Ext.get(win.getId());
				msgTarget.mask('Running...', 'x-mask-loading');
				Ext.Ajax.request({ 
					url: url,
					params: {template_id: tid, seed_id: data.seed_id, request_id: data.request_id, index:myRec.data.index},
					success: function(response) {
						msgTarget.unmask();
						try {
							eval('var ret = '+response.responseText);
							if(ret.success) {
								win.close();
								showWingsRanMessage(tid, ret.data.run_id, ret.data.request_id, results_url);
							}
							else if(window.console) 
								window.console.log(response.responseText);
						}
						catch (e) {
							if(window.console) window.console.log(e);
							msgTarget.unmask();
						}
					},
					failure: function(response) {
						if(window.console) window.console.log(response.responseText);
					}
				});
			}
		}]
	});

	var explanationsGrid = getWingsExplanation(data);
	win.add({
		region:'center',
		layout:'border',
		items:[ 
			alternativesGrid,
			{
				xtype:'button', region:'south', text: 'Show/Hide Explanations', handler: function() {
					if(explanationsGrid.isVisible()) { explanationsGrid.collapse(); }
					else { explanationsGrid.expand(); }
				}
			}
		]
	});
	win.add(explanationsGrid);
	win.show();
}

function showWingsMessage(msg, title, data) {
	var win = new Ext.Window({
		layout:'border',
		constrain:true,
		maximizable:true,
		title:title,
		autoScroll:true,
		width:600, height:450 
	});

	if(data) {
		var explanationsGrid = getWingsExplanation(data);
		Ext.apply(explanationsGrid, {region:'south'});
		win.add(explanationsGrid);
		win.add({
			region:'center',
			layout:'border',
			items:[ 
				{ 
					region: 'center',
					autoHeight: true,
					padding: 5,
					border: false,
					html: msg
				},
				{
					xtype:'button', region:'south', text: 'Show/Hide Explanations', handler: function() {
						if(explanationsGrid.isVisible()) { explanationsGrid.collapse(); }
						else { explanationsGrid.expand(); }
					}
				}
			]
		});
		win.add(explanationsGrid);
	}
	else {
		win.add({
			region: 'center',
			autoHeight: true,
			padding: 5,
			border: false,
			html: msg
		});
	}
	win.show();
}
