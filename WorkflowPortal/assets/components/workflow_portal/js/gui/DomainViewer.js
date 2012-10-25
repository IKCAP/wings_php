function formatSizeField(val, p, record) {
	if(val) return formatSize(val, 2);
	return val;
}
function formatDateField(val, p, record) {
	if(val) return Ext.Date.format(new Date(val*1000), 'g:i:s a, M d, Y');
	return val;
}

function formatExecutionEngineField(val, p, record) {
    if(val == "shell") return "Shell";
    if(val == "xml") return "Pegasus";
	return val;
}

function formatUserDomainField(val, p, record) {
	var cls = "";
	var data = record.data;
	//if(data.user_default) cls = "user_default_domain";
	//else cls = "user_domain";

	var str = "";
	var det = data.details;
	//var str = "<div class='"+cls+"'>";
	if(data.islib && val=="library") 
		val="Default Library";

	str += "<span class='runtitle'>"+val+"</span>";
	if(det) {
		if(det.d) {
			str += "<span class='runtime'>("+det.t+" Template, "+det.d[0]+" DataTypes, "+det.d[1]+" Files)</span>";
		}
		if(det.c) {
			str += "<span class='runtime'>("+det.c[0]+" Concrete Components, "+det.c[1]+" Abstract Components)</span>";
		}
	}
	//str += "</div>";
	return str;
}

function formatSysDomainField(val, p, record) {
	var cls = "";
	if(record.data.sys_default) cls = "default_domain";
	else cls = "domain";

	var str = "<div class='"+cls+"'>";
	str += "<div class='runtitle'>"+val+"</div>";
	str += "</div>";
	return str;
}

function toggleSibling(item) {
	var d = item.nextSibling.style.display;
	item.nextSibling.style.display = d ? '' : 'none';
	item.innerHTML = d ? 'less..' : 'more..'
}

function formatUpdatesField(val, p, record) {
	if(!val || !val.length) return val;
	var str = "Yes ";
	var details = "";
	for(var i=0; i<val.length; i++) {
		var v = val[i];
		details += v.name+" \\n\\t"+ Ext.Date.format(new Date(v.time*1000), 'g:i:s a, M d, Y')+"\\n";
	}
	str += "<a onclick='alert(\""+details+"\")'>more..</a>";
	return str;
}

function updateDomainInUserGrid(json, userGrid) {
	var rec = userGrid.getStore().getNodeById(json.id);
	if(rec) {
		for(var key in json) rec.set(key,json[key]);
		//userGrid.getView().refresh();
	}
	else {
		userGrid.getStore().getRootNode().appendChild(json);
	}
}

function importDomain(sysGrid, userGrid, op_url) {
	//var rec = sysGrid.getSelectionModel().getSelected();
	var recs = sysGrid.getSelectionModel().getSelection();
	if(!recs || !recs.length) return;
	var rec = recs[0];
	var domain = rec.data.domain;
	_importDomainHelper("importDomain", domain, sysGrid, userGrid, op_url);
}

function reimportDomain(userGrid, op_url) {
	//var rec = userGrid.getSelectionModel().getSelected();
	var recs = userGrid.getSelectionModel().getSelection();
	if(!recs || !recs.length) return;
	var rec = recs[0];
	var domain = rec.data.domain;
	Ext.MessageBox.confirm("Confirm Domain Deletion", 
		"Are you sure you want to re-import ? This will overwrite any changes in the domain: "+domain, 
		function(b) {
			if(b == "yes") 
				_importDomainHelper("reimportDomain", domain, null, userGrid, op_url);
		}
	);
}

function _importDomainHelper(op, domain, sysGrid, userGrid, op_url) {
	var url = op_url+'&op='+op;
	Ext.get(userGrid.getId()).mask("Importing..");
	Ext.Ajax.request({ 
		url: url,
		params: { domain: domain },
		success: function(response) {
			Ext.get(userGrid.getId()).unmask();
			if(response.responseText) {
				var rjson = Ext.decode(response.responseText); 
				updateDomainInUserGrid(rjson, userGrid);
				if(sysGrid) sysGrid.getView().refresh();
			}
		},
		failure: function(response) {
			userGrid.unmask();
			_console(response.responseText);
		}
	});
}

function newDomain(sysGrid, userGrid, op_url) {
	Ext.Msg.prompt("New Domain", "Enter name for the new Domain:", function(btn, domain) {
		if(btn == 'ok' && domain) {
			domain = getRDFID(domain);
			var r1 = userGrid.getStore().getNodeById(domain);
			var r2 = sysGrid.getStore().getById(domain);
			if(r1 || r2) {
				return Ext.MessageBox.show({ 
					icon:Ext.MessageBox.ERROR, buttons:Ext.MessageBox.OK, 
					msg:"A domain with the name "+domain+" already exists" 
				});
			}

			Ext.get(userGrid.getId()).mask("Creating new domain..");
			var url = op_url+'&op=newDomain';
			Ext.Ajax.request({ 
				url: url,
				params: { domain: domain },
				success: function(response) {
					Ext.get(userGrid.getId()).unmask();
					if(response.responseText) {
						var rjson = Ext.decode(response.responseText); 
						updateDomainInUserGrid(rjson, userGrid);
					}
				},
				failure: function(response) {
					Ext.get(userGrid.getId()).unmask();
					_console(response.responseText);
				}
			});
		}
	});
}

function newComponentLibrary(sysGrid, userGrid, op_url) {
	var doms = userGrid.getSelectionModel().getSelection();
	if(!doms || !doms.length) {
		showError("Please select a domain to create the Component Library for");
		return;
	}
	var domain = doms[0];
	if(domain.data.islib) {
		domain = domain.parentNode;
	}

	var domainid = domain.data.id;

	Ext.Msg.prompt("New Component Library", "Enter name for the new Component Library:", function(btn, libname) {
		if(btn == 'ok' && libname) {
			libname = getRDFID(libname);
			var r1 = userGrid.getStore().getNodeById(domainid+"_"+libname);
			if(r1) {
				showError("A library with the name "+libname+" already exists") 
				return;
			}

			Ext.get(userGrid.getId()).mask("Creating New Component Library..");
			var url = op_url+'&op=newComponentLibrary';
			Ext.Ajax.request({ 
				url: url,
				params: { domain: domainid, libname: libname },
				success: function(response) {
					Ext.get(userGrid.getId()).unmask();
					if(response.responseText) {
						var rjson = Ext.decode(response.responseText); 
						for(var i=0; i<rjson.children.length; i++) {
							var cjson = rjson.children[i];
							if(cjson.domain == libname)
								domain.appendChild(cjson);
						}
					}
				},
				failure: function(response) {
					Ext.get(userGrid.getId()).unmask();
					_console(response.responseText);
				}
			});
		}
	});
}

function setDefaultDomainOrLibrary(userGrid, op_url) {
	var recs = userGrid.getSelectionModel().getSelection();
	if(!recs || !recs.length) return;
	var rec = recs[0];

	var url = "";
	var params = {};
	if(rec.data.islib) {
		url = op_url+'&op=setDefaultComponentLibrary';
		params = {domain: rec.parentNode.data.domain, libname:rec.data.domain};
	}
	else {
		url = op_url+'&op=setDefaultDomain';
		params = {domain:rec.data.domain};
	}

	Ext.get(userGrid.getId()).mask("Setting Default Domain..");
	Ext.Ajax.request({ 
		url: url,
		params: params,
		success: function(response) {
			Ext.get(userGrid.getId()).unmask();
			if(response.responseText) {
				if(rec.data.islib) {
					rec.parentNode.eachChild( function(node) {
						node.set('selected', false);
						node.set('iconCls', 'undefIcon');
					});
					rec.set('selected', 'true');
					rec.set('iconCls', 'defIcon');
				}
				else {
					var rootNode = userGrid.getStore().getRootNode();
					rootNode.eachChild( function(node) {
						node.set('user_default', false);
						node.set('iconCls', 'undefIcon');
					});
					var rjson = Ext.decode(response.responseText);
					updateDomainInUserGrid(rjson, userGrid);
				}
			}
		},
		failure: function(response) {
			Ext.get(userGrid.getId()).unmask();
			_console(response.responseText);
		}
	});
}

function changeDomainOutputFormat(userGrid, op_url) {
	//var rec = userGrid.getSelectionModel().getSelected();
	var recs = userGrid.getSelectionModel().getSelection();
	if(!recs || !recs.length) return;
	var rec = recs[0];
	var domain = rec.data.domain;
    var opfmt = (rec.data.output_format == "shell") ? "xml" : "shell";

	var url = op_url+'&op=setDomainOutputFormat';
	Ext.get(userGrid.getId()).mask("Changing Execution Engine..");
	Ext.Ajax.request({ 
		url: url,
		params: { domain: domain, format:opfmt },
		success: function(response) {
			Ext.get(userGrid.getId()).unmask();
			if(response.responseText) {
				var rjson = Ext.decode(response.responseText);
				updateDomainInUserGrid(rjson, userGrid);
			}
		},
		failure: function(response) {
			Ext.get(userGrid.getId()).unmask();
			_console(response.responseText);
		}
	});
}

function deleteDomainOrLibrary(sysGrid, userGrid, op_url) {
	var recs = userGrid.getSelectionModel().getSelection();
	if(!recs || !recs.length) return;
	var rec = recs[0];

	var url = "";
	var params = {};
	if(rec.data.islib) {
		if(rec.data.selected) return showError("Cannot delete default component library");
		url = op_url+'&op=deleteComponentLibrary';
		params = {domain: rec.parentNode.data.domain, libname:rec.data.domain};
	}
	else {
		if(rec.data.user_default) return showError("Cannot delete default domain");
		url = op_url+'&op=deleteDomain';
		params = {domain:rec.data.domain};
	}

	Ext.get(userGrid.getId()).mask("Deleting..");
	Ext.Ajax.request({ 
		url: url,
		params: params,
		success: function(response) {
			Ext.get(userGrid.getId()).unmask();
			if(response.responseText == "OK") {
				rec.parentNode.removeChild(rec);
			}
			else _console(response.responseText);
			sysGrid.getView().refresh();
		},
		failure: function(response) {
			Ext.get(userGrid.getId()).unmask();
			_console(response.responseText);
		}
	});
}


function initializeDomainBrowser(guid, panelid, width, height, store, domain, op_url, advanced_user) {
	// Add the template tabPanel in the center
	//if(window.console) window.console.log(store);

	var userdoms = store.domains.user;
	var sysdoms = store.domains.system;

	var userrecs = [];
	var sysrecs = [];
	var userfields = ['domain', 'details', 'size', 'user_default', 'output_format', 'out_of_date', 'install_time', 'islib', 'selected', 'user_defined'];
	var sysfields = ['domain', 'size', 'sys_default', 'install_time'];

	if(!Ext.ModelManager.isRegistered('userRecord'))
		Ext.define('userRecord', {extend:'Ext.data.Model', fields:userfields});
	if(!Ext.ModelManager.isRegistered('sysRecord'))
		Ext.define('sysRecord', {extend:'Ext.data.Model', idProperty:'domain', fields:sysfields});

	var sysGridStore = new Ext.data.Store({model:'sysRecord', reader: {type:'json'}});
	sysGridStore.loadData(sysdoms);

	var userGridStore = new Ext.data.TreeStore({model:'userRecord', reader: {type:'json'}, root:{children:userdoms}});

	var userGrid = new Ext.tree.Panel({
		//autoHeight: true,
		height: '50%',
		store: userGridStore,
		useArrows: true,
		split: true,
		autoScroll: true,
		animate: false,
		region: 'center',
		//collapsible: true,
		rootVisible: false,
		columnLines: true,
		title: 'My Domains',
		selModel: new Ext.selection.RowModel({mode:"SINGLE"}),
		bodyCls:'multi-line-grid custom-highlight-grid',
		viewConfig: {
			stripeRows: true,
			getRowClass: function(rec, rowIndex, rp, ds){ 
				if(rec.data.islib) return "complibCls";
				else if(!sysGridStore.getById(rec.data.domain)) return "myDomainCls";
				return "";
			}
		},
		columns: [{
			header:'Domain', dataIndex:'domain', sortable: true, flex: 1, 
			xtype: 'treecolumn',
			menuDisabled:true,
			renderer: formatUserDomainField
		},
		{ 
			header:'Execution Engine', dataIndex:'output_format', sortable: true, 
			menuDisabled:true,
			renderer:formatExecutionEngineField
		},
		{ 
			header:'Size', dataIndex:'size', sortable: true, 
			menuDisabled:true,
			renderer:formatSizeField
		},
		{ 
			header:'Installed On', dataIndex:'install_time', sortable: true, width:150,
			menuDisabled:true,
			renderer:formatDateField
		},
		{ 
			header:'Updates Available', dataIndex:'out_of_date',
			menuDisabled:true,
			renderer:formatUpdatesField 
		}],

		dockedItems: [
		{
			xtype: 'toolbar',
			dock: 'top',
			itemId: 'userGridToolbar',
			items: [
				{
					text: 'New', iconCls: 'addIcon',
					menu: [{ 
						text:'Domain', iconCls: 'undefIcon',
						handler: function() { newDomain(sysGrid, userGrid, op_url); }
					},
					{ 
						text:'Component Library', iconCls: 'runIcon',
						handler: function() { newComponentLibrary(sysGrid, userGrid, op_url); }
					}]
				}, 
				{
					text: 'Set Default', iconCls: 'defIcon', disabled:true,
					handler: function() { setDefaultDomainOrLibrary(userGrid, op_url); }
				},
				{
					text: 'Delete', iconCls: 'delIcon', disabled:true,
					handler: function() { deleteDomainOrLibrary(sysGrid, userGrid, op_url); }
				}, 
				{
					text: 'Reimport', iconCls: 'importIcon', disabled:true,
					handler: function() { reimportDomain(userGrid, op_url); }
				}, 
				{
					text: 'Change Execution Engine', iconCls: 'runIcon', disabled:true,
					handler: function() { changeDomainOutputFormat(userGrid, op_url); }
				}
			]
		}]
	});
	userGrid.on("afterrender", function() {
		userGrid.getEl().removeCls("x-tree-panel");
		userGrid.getEl().addCls("mytree-grid");
	});

	userGrid.getSelectionModel().on("select", function(sm, rec, ind) {
		var tb = userGrid.getDockedComponent('userGridToolbar');
		if(rec.data.user_default || rec.data.selected) {
			tb.getComponent(1).setDisabled(true);
			tb.getComponent(2).setDisabled(true);
		}
		else {
			tb.getComponent(1).setDisabled(false);
			tb.getComponent(2).setDisabled(false);
		}
		if(rec.data.user_defined || rec.data.islib) {
			tb.getComponent(3).setDisabled(true);
		}
		else {
			tb.getComponent(3).setDisabled(false);
		}
		if(rec.data.islib) {
			tb.getComponent(4).setDisabled(true);
		}
		else {
			tb.getComponent(4).setDisabled(false);
		}
		//console.log(rec.json.complibs);
	});


	var sysGrid = new Ext.grid.GridPanel({
		//autoHeight:true,
		height: '50%',
		region: 'south',
		split: true,
		//frame:true,
		selModel: new Ext.selection.RowModel({mode:"SINGLE"}),
		columns: [
			{ header:'Domain', dataIndex:'domain', sortable: true, flex:1, 
				menuDisabled:true,
				renderer:formatSysDomainField
			},
			{ header:'Size', dataIndex:'size', sortable: true,
				menuDisabled:true,
				renderer:formatSizeField
			},
			{ header:'Installed On', dataIndex:'install_time', sortable: true, width: 150,
				menuDisabled:true,
				renderer:formatDateField
			}
		],
		viewConfig: {
			getRowClass: function(rec, rowIndex, rp, ds){ 
				if(userGrid.getStore().getNodeById(rec.data.domain)) return "installedDomainCls";
				return "";
			}
		},
		columnLines: true,
		title: 'System Installed Domains',
		bodyCls:'multi-line-grid custom-highlight-grid',
		dockedItems: [
		{
			xtype: 'toolbar',
			dock: 'top',
			itemId: 'sysGridToolbar',
			items: [
				{
					text: 'Import', iconCls: 'importIcon', disabled:true, 
					handler: function() { importDomain(sysGrid, userGrid, op_url); this.setDisabled(true); }
				} 
			]
		}],
		store:sysGridStore
	});
	sysGrid.getSelectionModel().on("select", function(sm, rec, ind) {
		var tb = sysGrid.getDockedComponent('sysGridToolbar');
		var rec = userGrid.getStore().getNodeById(rec.data.domain);
		if(rec) tb.getComponent(0).setDisabled(true);
		else tb.getComponent(0).setDisabled(false);
	});

	var mainPanel  = new Ext.Panel({
		//margin: '10 0 0 0',
		layout: 'border',
		width: width, 
		height: height,
		border: false,
		monitorResize:true,
		items: [
			userGrid, 
			sysGrid
		]
	});

	if(panelid)
		mainPanel.render(Ext.get(panelid));

	/*userGrid.on('itemexpand', userGrid.doLayout);
	userGrid.on('itemcollapse', userGrid.doLayout);
	userGrid.on('itemappend', userGrid.doLayout);
	userGrid.on('itemremove', userGrid.doLayout);*/

	return mainPanel;
}
