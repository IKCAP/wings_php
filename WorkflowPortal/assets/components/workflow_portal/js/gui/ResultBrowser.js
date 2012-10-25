function formatRunListItem(value, meta, record, rowind, colind, store, view) {
	var d = record.data;
	var tid = d.templateid;
	var rstat = d.status;
	var tmpl = "<div class='status_{0}'><div class='runtitle'>{1}</div>";
	tmpl += "<div class='runtime'>Run ID:{2}, Request ID:{3}</div></div>"
	return Ext.String.format(tmpl, rstat, tid, d.id, d.requestid);
}

function formatProgressBar(value, meta, record, rowind, colind, store, view) {
	var d = record.data;
	var tmpl = "<div style='width:{0}px;height:{1}px;background-color:#EEE;border:1px solid #CCC'>";
	tmpl += "<div style='width:{3}px;height:{1}px;background-color:{6}'>";
	tmpl += "<div style='width:{2}px;height:{1}px;background-color:{7}'>&nbsp;</div></div></div>";
	if(d.status == "FAILURE")
		tmpl += "<div class='running'>Last job(s): {5}</div>";
	else if(d.status == "ONGOING")
		tmpl += "<div class='running'>Current job(s): {5}</div>";
	else if(d.status == "SUCCESS")
		tmpl += "<div class='running'>Finished !</div>";

	var w = 190;
	if(d.status == "SUCCESS") d.percent_done=100;
	return Ext.String.format(tmpl, w, 10,
		Math.round(w*d.percent_done/100), 
		Math.round(w*d.percent_doing/100), 
		d.percent_done, d.running_jobs, 
		(d.status=="FAILURE"?"#F66" : (d.status=="ONGOING"?"#999":"#6E6")),
		"#6E6" 
	);
}

function formatBinding(value, meta, record, op_url, runid, tabPanelId) {
	return getBindingItemHTML(record.data.b, record.data.type, op_url, runid, tabPanelId);
}

function getBindingItemHTML(b, type, op_url, runid, tabPanelId) {
	var str = "";
	if(Ext.isArray(b)) {
		str += "{ ";
		Ext.each(b, function(cb, i) {
			if(i) str += ", ";
			str += getBindingItemHTML(cb, type, op_url, runid, tabPanelId);
		});
		str += " }";
	}
	else {
		if(b.param) str += "<b style='color:green'>"+b.id+"</b>";
		else if(b.size != -1) {
			str += "<a href='"+USER_DATA_URL+b.id+"' target='_blank'>"+b.id+"</a> ";
			str += "<i style='color:#888'>("+formatSize(b.size);
			if(type != "Input") {
				str += ", <a class='smSaveIcon' href='#' ";
				str += "onclick=\"return registerData('"+b.id+"','"+op_url+"',"+runid+",'"+tabPanelId+"')\">Save</a>";
			}
			str += ")</i>";
		}
		else str += "<b style='color:red'>Not yet available</b>";
	}
	return str;
}

function formatSize(bytes, precision) {
	if(!precision) precision = 2;
	var units = ['B', 'KB', 'MB', 'GB', 'TB']; 
   
	bytes = Math.max(bytes, 0); 
	var pow = Math.floor((bytes ? Math.log(bytes) : 0) / Math.log(1024)); 
	pow = Math.min(pow, units.length - 1); 
   
	bytes /= Math.pow(1024, pow); 
   
	return Math.round(bytes, precision) + ' ' + units[pow]; 
}

function getIODataGrid(data, op_url, runid, tabPanel) {
	//console.log(data.bindings);
	if(!Ext.ModelManager.isRegistered('workflowRunDetails'))
		Ext.define('workflowRunDetails', {extend:'Ext.data.Model', fields:['v','type','b']});

	var wRunDStore = new Ext.data.Store ({
		model: 'workflowRunDetails',
		proxy: {
			type: 'memory',
			reader: {
				type: 'json',
				root: 'rows',
				totalProperty: 'results'
			}
		},
		sorters: ['v'],
		groupField: 'type',
		data: data.bindings
	});
	tabPanel.bindings = data.bindings.rows;

	var groupingFeature = Ext.create('Ext.grid.feature.Grouping', {
		groupHeaderTpl: '{name} ({rows.length} Item{[values.rows.length > 1 ? "s" : ""]})'
	});

	var grid = new Ext.grid.GridPanel({
		store: wRunDStore,
		columns: [
			Ext.create('Ext.grid.RowNumberer', {width: 30, dataIndex:'x'}),
			{ header:'Variable', dataIndex:'v', width:200, sortable:true, menuDisabled:true },
			{ header:'Bindings', dataIndex:'b', flex:1, menuDisabled:true, 
				renderer: function(v,m,r) { return formatBinding(v,m,r,op_url,runid,tabPanel.getId()) } },
			{ header: 'Data', hidden:true, dataIndex:'type', menuDisabled:true }
		],

		//autoExpandColumn: 'binding',
		monitorResize: true,
		features: [groupingFeature],
		viewConfig: {
			trackOver: false
		},
		bodyCls:'multi-line-grid',
		border: false
	});

	return grid;
}

function getBindingMetrics_helper(bid, b) {
	var metrics = null;
	if(Ext.isArray(b)) {
		Ext.each(b, function(cb) {
			var m = getBindingMetrics_helper(bid, cb); 
			if(m) metrics = m;
		});
	}
	else {
		if(!b.param && b.size >= 0 && b.id == bid) 
			metrics = b.metrics;
	}
	return metrics;
}
function getBindingMetrics(bid, bindings) {
	var metrics = null;
	Ext.each(bindings, function(binding) {
		var m = getBindingMetrics_helper(bid, binding.b);
		if(m) metrics = m;
	});
	return metrics;
}

function registerData(dsid, op_url, runid, tabPanelId) {
	Ext.Msg.prompt("Save data..", 
		"Name this dataset for your Data Catalog:", 
		function(btn, text) {
			if(btn == 'ok' && text) {
				var newid = getRDFID(text);
				var url = op_url+'&op=registerData';
				var tabPanel = Ext.getCmp(tabPanelId);
				var msgTarget = tabPanel.getEl();
				var metrics = getBindingMetrics(dsid, tabPanel.bindings);
				msgTarget.mask('Saving and Registering Data...', 'x-mask-loading');
				Ext.Ajax.request({ 
					url: url,
					params: {dsid:dsid, newid:newid, run_id:runid, metrics:Ext.encode(metrics)},
					success: function(response) {
						msgTarget.unmask();
						if(response.responseText == "OK") {
							// Replace dsid with newid in store
						}
						else if(window.console) 
							window.console.log(response.responseText);
					},
					failure: function(response) {
						if(window.console) window.console.log(response.responseText);
					}
				});
			}
		},
	window, false, dsid);
	return false;
}


function getRunReport(op_url, runid, tabPanel, type, image) {
	var msgTarget = tabPanel.getEl();
	msgTarget.mask('Generating '+type+'...', 'x-mask-loading');
	var url = op_url+'&op=getRun'+type;
	Ext.Ajax.request({ 
		url: url,
		params: {run_id:runid, run_image:image},
		success: function(response) {
			msgTarget.unmask();
			var url = response.responseText;
			var win = new Ext.Window({
				title: type+' Turtle',
				width: 800,
				height: 600,
				plain: true,
				html: "URL: <a target='_blank' href='"+url+"'>"+url+"</a><br/><iframe style='width:770px; height:550px' src='"+url+"'></iframe>"
			});
			win.show();
			//console.log(url);
		},
		failure: function(response) {
			msgTarget.unmask();
			if(window.console) window.console.log(response.responseText);
		}
	});
}

function getRunDetailsPanel(guid, data, op_url, runid) {
	var tid = data.seedid;
	//var tid = data.templateid;
	//console.log(data);
	var tBrowser = initializeTemplateBrowser(guid, null, '100%', '100%', 
							{hide_selector:true, hide_form:true}, 
							data, tid, tid, null, null, null);
	Ext.apply(tBrowser, {border:false});
	var tabPanel = tBrowser.templatePanel.mainTab;
	Ext.apply(tabPanel, {border:false});
	tBrowser.templatePanel.setTitle('Workflow');
	tabPanel.insert(0, new Ext.Panel({
		title:'Data',
		layout:'fit',
		border:false,
		iconCls: 'dataIcon',
		items: getIODataGrid(data, op_url, runid, tabPanel),
		tbar: [
			{
				text: 'Get HTML',
				iconCls: 'docsIcon',
				handler: function() {
					tabPanel.setActiveTab(2);
					var graph = tBrowser.templatePanel.graph.editor;
					var image = graph.getImageData(1, false);
					tabPanel.setActiveTab(0);
					getRunReport(op_url, runid, tabPanel, 'HTML', image);
				}
			},
			{
				text: 'Publish RDF',
				iconCls: 'docsIcon',
				handler: function() {
					Ext.MessageBox.confirm("Confirm", 
        				"This might take a while to publish. Please don't close this browser window while upload is going on. Are you sure you want to Continue ?", 
	        			function(b) {
		        			if(b == "yes") {
								tabPanel.setActiveTab(2);
								var graph = tBrowser.templatePanel.graph.editor;
								var image = graph.getImageData(1, false);
								tabPanel.setActiveTab(0);
								getRunReport(op_url, runid, tabPanel, 'RDF', image);
							}
						}
					);
				}
			}
		]
	}));
	tabPanel.insert(1, new Ext.Panel({
		title:'Run Log',
		layout:'border',
		border:false,
		iconCls: 'runIcon',
		items: {
			region:'center', border:false, 
			xtype: 'textarea', readOnly: true, value:data.log,
			style:'font-size:12px;font-family:monospace;'
		},
		autoScroll:true
	}));
	tabPanel.setActiveTab(0);
	return tBrowser;
}

function refreshOpenRunTabs(grid, guid, tabPanel, op_url, wRunStore) {
	var tab = tabPanel.getActiveTab();
	if(tab && tab.runid) {
		var rec = wRunStore.getById(tab.runid);
		if(rec.data.status != tab.status || rec.data.status=='ONGOING') {
			openRunDetails(guid, tab.runid, tab.title, rec.data.status, tabPanel, op_url, tab);
			tab.status = rec.data.status;
		}
		selectRunInList(grid, tab.runid);
	}
}

function openRunDetails(guid, runid, reqid, status, tabPanel, op_url, tab) {
	var tabName = reqid;

	// If a tab is provided, then explicitly  refresh
	// Else, check for matching tab and return if already open
	if(!tab) {
		// Check if tab is already open
		var items = tabPanel.items.items;
		for(var i=0; i<items.length; i++) {
			var tab = items[i];
			if(tab && tab.title.replace(/^\**/,'')==tabName) {
				tabPanel.setActiveTab(tab);
				tab.status = status;
				return null;
			}
		}
		tab = openNewIconTab(tabPanel, tabName, 'runIcon');
		tab.runid = runid;
		tab.status = status;
		tabPanel.setActiveTab(tab);
	}

	var url = op_url+"&op=getRunDetails&run_id="+runid;
	Ext.apply(tab, {
		loader: {
			loadMask: true,
			url: url,
			renderer: function(loader, response, req) {
				//try {
					eval('var runJson = '+response.responseText);
					if(runJson && runJson.results > 0) {
						tab.removeAll();
						var runDetails = getRunDetailsPanel(guid, runJson.rows, op_url, runid);
						tab.add(runDetails);
						//tab.doLayout(false,true);
					}
					else {
						el.update('No Run Details', false, cb);
					}
				/*}
				catch (e) {
					el.update(e, false, cb);
				}*/
			}
		}
	});
	tab.getLoader().load();
}

function getRunList(guid, tabPanel, runid, op_url) {
	var fields = [ 'id', 'seedid', 'requestid', 'templateid', 'status', 
			{name:'start_time', type:'date', dateFormat:'timestamp'},
			{name:'end_time', type:'date', dateFormat:'timestamp'},
			'running_jobs', 'numrunning_jobs', 'percent_done', 'percent_doing' ];
	var url = op_url+"&op=getRunList";

	if(!Ext.ModelManager.isRegistered('workflowRuns'))
		Ext.define('workflowRuns', {extend:'Ext.data.Model', fields:fields});

	var wRunStore = new Ext.data.Store({
		model: 'workflowRuns',
		proxy: {
			type: 'ajax',
			simpleSortMode: true,
			api: { 
				read: op_url+"&op=getRunList",
				destroy: op_url+"&op=deleteRun" 
			},
			reader: {
				type: 'json',
				idProperty: 'id',
				root: 'rows',
				totalProperty: 'results'
			},
			writer: {
				type: 'json',
				encode: true, 
				root: 'rows',
				writeAllFields: false
			},
		},
		sorters: [{property:'start_time', direction:'DESC'}],
		autoSync: true
		//autoLoad: true
	});

	var grid = new Ext.grid.GridPanel({
		columns: [
			{ header:'Template', dataIndex:'templateid', flex:1, 
				renderer:formatRunListItem, sortable:true, menuDisabled:true },
			{ header:'Progress', dataIndex:'percent_done', sortable: true,
				width:200, renderer:formatProgressBar, menuDisabled:true },
			{ header:'Start Time', dataIndex:'start_time', sortable: true,
				xtype:'datecolumn', format: 'g:i:s a, M d, Y', width:150, menuDisabled:true },
			{ header:'End Time', dataIndex:'end_time', sortable:true,
				xtype:'datecolumn', format: 'g:i:s a, M d, Y', width:150, menuDisabled:true }
		],
		title: 'My Runs',
		//autoExpandColumn: 'title',
		bodyCls:'multi-line-grid custom-highlight-grid',
		border: false,
		split: true,
		tbar: [
			{
				text: 'Delete',
				iconCls: 'delIcon',
				handler: function() {
					var recs = grid.getSelectionModel().getSelection();
					if(recs && recs.length) {
						wRunStore.remove(recs);
						tabPanel.remove(tabPanel.getActiveTab());
					}
				}
			}, '-', {xtype: 'tbfill'}, '-',
			{
				text: 'Reload',
				iconCls: 'reloadIcon',
				handler: function() {
					wRunStore.load();
				}
			}
		],
		store:wRunStore
	});

	grid.getSelectionModel().on("select", function(sm, rec, ind) {
		openRunDetails(guid, rec.data.id, rec.data.requestid, rec.data.status, tabPanel, op_url);
		//Ext.EventManager.stopEvent(event);
	}, this);

	if(runid) {
		var fn = function() { 
			//var rec = wRunStore.getById(runid);
			//openRunDetails(guid, runid, rec.data.requestid, rec.data.status, tabPanel, op_url); 
			selectRunInList(grid, runid);
			wRunStore.un('load', fn);
		};
		wRunStore.on('load', fn );
	}

	wRunStore.on('load', function() {
		refreshOpenRunTabs(grid, guid, tabPanel, op_url, wRunStore); 
	});

	var gridListRefresh = { 
    	run: function() { wRunStore.load(); },
    	interval: 30000 //30 seconds
	}
	Ext.TaskManager.start(gridListRefresh);

	return grid;
}

function selectRunInList(runList, runid) {
	//if(window.console) window.console.log(runid);
	runList.getSelectionModel().deselectAll();
	runList.getStore().each(function(rec) {
		if(rec.data.id == runid) 
			runList.getSelectionModel().select([rec]);
	});
}

function initializeResultBrowser(guid, runid, op_url) {
	// Add the result tabPanel in the center
	var tabPanel = new Ext.TabPanel({
		region: 'center', 
		margins: '0 5 5 5',
		enableTabScroll: true,
		resizeTabs: true,
		//minTabWidth: 175,
		//tabWidth: 175,
		items: [{ 
			layout: 'fit',
			title: 'ResultBrowser',
			autoLoad: { url: op_url+'&op=intro' }
		}]
	});

	var runList = getRunList(guid, tabPanel, runid, op_url);
	Ext.apply(runList, {
		region:'north',
		height: 200,
		split: true
	});

	tabPanel.on('tabchange', function(tp, tab){
		selectRunInList(runList, tab.runid);
	});

	var mainpanel  = new Ext.Viewport({
		layout: 'border', 
		monitorResize: true,
		items: [
			runList, 
			tabPanel
		]
	});
	if(!runid) tabPanel.setActiveTab(0);

	return mainpanel;
}
