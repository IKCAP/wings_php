/**
 * <p>SeedForm is an extension of the default FormPanel. It allows creation of a Seeded Template
 * 
 * @author <a href="mailto:varunratnakar@google.com">Varun Ratnakar</a>
 * @class Ext.ux.SeedForm
 * @extends Ext.FormPanel
 * @constructor
 * @component
 * @version 1.0
 * @license GPL
 * 
 */


function setComboListOptions(copts, data, selection, emptyText, editable, multi) {
	copts.emptyText = emptyText;
	data.humanSort();
	var opts = [];
	for(var x=0; x<data.length; x++) { opts[x] = {"name":data[x]}; }
	copts.store = Ext.create('Ext.data.Store', {fields:['name'], model:'comboList', data:opts});
	if(selection) copts.value = selection;
	if(!editable) copts.forceSelection = true;
	copts.editable = editable;
	copts.queryMode = 'local';
	copts.displayField = 'name';
	copts.valueField = 'name';
	copts.triggerAction = 'all';
	copts.multiSelect = multi;
	return copts;
}

Ext.namespace("Ext.ux");
Ext.ux.form.SeedForm = Ext.extend(Ext.FormPanel, { 
	url: null,
	template_id: null,
	graph: null,

	initComponent: function(config) {
		// Height = padding + height per form item + extra padding
		//var _oldversion = parseFloat(Ext.getVersion()) < 3.2 ? true:false;
		var height = 28*Math.ceil(this.store.length/2.0) + 34; // + (_oldversion ? 16 : 0);

		Ext.apply(this, config);
		Ext.apply(this, {
			//bodyStyle: 'padding-bottom:5px',
			//bodyStyle: 'padding:5px',
			height: height,
			autoHeight: true,
			autoScroll: true,
			border: false,
			layout: 'hbox',
			defaults: {
				border: false, flex: 1,
				layout: 'anchor', padding:4
			},
			fieldDefaults: {
				anchor: '-10', 
				labelAlign: 'right',
				labelWidth: 100,
				labelStyle:'font-size:11px;text-overflow:ellipsis'
			},
			items: []
		}); 

		this.flatItems = [];
		//this.items = [{layout:'column', border:false, height:height, items: []}];

		var tmp = {items: []};
		for(var i=0; i<this.store.length; i++) {
			if(i==Math.ceil(this.store.length/2.0)) {
				this.items.push(tmp);
				tmp = {items: []};
			}
			var item = this.store[i];
			var formitem;
			var copts = {name:item.name, wtype:item.type, fieldLabel:Ext.util.Format.ellipsis(item.name,18)};
			if(item.type=="data") {
				var emptyText = 'Select '+(item.dim ? 'multiple files':'a file')+'...';
				copts = setComboListOptions(copts, item.options, item.binding, emptyText, !item.dim, item.dim);
				var formitem = new Ext.form.field.ComboBox(copts);
			} else if(item.type=="param") {
				copts.emptyText = 'Enter a '+item.dtype+' value...';
				if(item.binding) copts.value = item.binding.label;
				if(item.dtype == "boolean") 
					formitem = new Ext.form.CheckboxField(copts);
				else if(item.dtype == "float") {
					copts['decimalPrecision'] = 6;
					formitem = new Ext.form.NumberField(copts);
				}
				else if(item.dtype == "int") {
					copts.allowDecimals = false;
					formitem = new Ext.form.NumberField(copts);
				}
				else formitem = new Ext.form.TextField(copts);
			}
			if(formitem) {
				formitem.on('focus', function(item) {
					if(this.graph && this.graph.editor) 
						this.graph.editor.selectItem(item.name, true);
				}, this);
				tmp.items.push(formitem);
				this.flatItems.push(formitem);
			}
		}
		this.items.push(tmp);

		var me = this;
		Ext.apply(this, {
		 	tbar: [{
				text: 'Suggest Data',
				iconCls: 'suggestIcon',
				handler: function() {
					op = 'getData';
					if(!me.checkValidity(op,'')) return false;
					var cbindings = me.getComponentBindings();
					me.setMultiValues();
					var title = 'Suggested Data for '+me.template_id;
					//window.console.log({op: op, template_id: me.template_id});
					me.getForm().submit({
						params:{op: op, template_id: me.template_id, cbindings:cbindings},
						waitMsg:'Getting Suggested Data...',
						success: function(form, action) { 
							showWingsBindings(action.result.data, title, me.flatItems, "data"); 
							me.resetMultiValues();
						}, 
						failure: function(form, action) { 
							showWingsError("Wings couldn't find Data for the template: "+me.template_id, title, action.result.data); 
							me.resetMultiValues();
						}
					});
				}
			},
			'-',
			{
				text: 'Suggest Parameters',
				iconCls: 'suggestIcon',
				handler: function() {
					op = 'getParameters';
					if(!me.checkValidity(op,'Please fill in all Data fields before querying for Parameters')) return false;
					var cbindings = me.getComponentBindings();
					me.setMultiValues();
					var title = 'Suggested Parameters for '+me.template_id;
					me.getForm().submit({
						params:{op: op, template_id: me.template_id, cbindings:cbindings},
						waitMsg:'Getting Suggested Parameters...',
						success: function(form, action) { 
							showWingsBindings(action.result.data, title, me.flatItems, "param"); 
							me.resetMultiValues();
						}, 
						failure: function(form, action) { 
							showWingsError("No parameter values are compatible with your datasets for the template: "+me.template_id+" !", title, action.result.data); 
							me.resetMultiValues();
						}
					});
				}
			},
			'-',
			/*{
				text: 'Suggest Components',
				iconCls: 'compIcon',
				handler: function() {
					op = 'getComponents';
					var cbindings = me.getComponentBindings();
					if(!me.checkValidity(op,'Please fill in all fields before querying for Components')) return false;
					var title = 'Suggested Components for '+me.template_id;
					me.getForm().submit({
						params:{op: op, template_id: me.template_id, cbindings:cbindings},
						waitMsg:'Getting Suggested Components...',
						success: function(form, action) { showWingsBindings(action.result.data, title, me.flatItems, "param"); }, 
						failure: function(form, action) { showWingsError("No parameter values are compatible with your datasets for the template: "+me.template_id+" !", title, action.result.data); }
					});
				}
			},
			'-',*/
			{
				text: 'Run Workflow',
				iconCls: 'runIcon',
				handler: function() {
					op = '';
					if(!me.checkValidity(op,'Please fill in all fields before running the Workflow')) return false;
					var cbindings = me.getComponentBindings();
					me.setMultiValues();
					me.getForm().submit({
						params:{op: op, template_id: me.template_id, cbindings:cbindings},
						waitMsg:'Preparing Workflow Execution...',
						success: function(form, action) { 
							var data = action.result.data;
							if(data.alternatives) {
								showWingsAlternatives(me.template_id, data, me.url, me.results_url);
							}
							else {
								showWingsRanMessage(me.template_id, data.run_id, data.request_id, me.results_url);
							}
							me.resetMultiValues();
						}, 
						failure: function(form, action) { 
							showWingsError("Wings couldn't generate any executable workflows for the template "+me.template_id+' based on what was submitted', 
										'No workflow submitted', action.result.data); 
							me.resetMultiValues();
						}
					});
				}
			},{
				xtype:'tbfill'
			},{
				text: 'Clear',
				iconCls: 'clearIcon',
				handler: function() {
					for(var i=0; i<me.flatItems.length; i++) {
		   			var item = me.flatItems[i];
		   			item.setValue('');
		   			item.allowBlank = true;
					}
				}
			},{
				text:'Reload',
				iconCls:'reloadIcon',
				handler: function() {
					var fetchOp = 'getViewerJSON';
					var url = me.op_url+'&op='+fetchOp+'&template_id='+me.template_id;
					me.templatePanel.getLoader().load({url: url});
				}
			}]
		});

		Ext.ux.form.SeedForm.superclass.initComponent.apply(this, config);
	},

	getComponentBindings: function() {
		var compbindings = {};
		var template = this.graph.editor.template;
		template.saveToStore();
		for(var i=0; i<template.store.nodes.length; i++) {
			var n = template.store.nodes[i];
			if(n.component && n.component.binding) 
				compbindings[n.component.name] = n.component.binding;
		}
		return Ext.encode(compbindings);
	},

	checkValidity: function(op, errmsg) {
		for(var i=0; i<this.flatItems.length; i++) {
		   var item = this.flatItems[i];
		   if(op == "") item.allowBlank = false;
		   else if(op == "getData") item.allowBlank = true;
		   else if(op == "getParameters") {
				if(item.wtype == "data") item.allowBlank = false;
				if(item.wtype == "param") item.allowBlank = true;
			}
		}
		if(!this.getForm().isValid()) {
			Ext.Msg.show({title:'Error', msg:errmsg, animEl:this, icon:Ext.Msg.ERROR, buttons:Ext.Msg.OK, modal:false});
			return false;
		}
		return true;
	},

	setMultiValues: function() {
		for(var i=0; i<this.flatItems.length; i++) {
		   var item = this.flatItems[i];
			if(item.multiSelect) {
				var itemvals = "";
				for(var i=0; i<item.value.length; i++) {
					if(i) itemvals += ",";
					itemvals += item.value[i];
				}
				item.tmpvalue = item.value;
				item.value = itemvals;
			}
		}
	},

	resetMultiValues: function() {
		for(var i=0; i<this.flatItems.length; i++) {
		   var item = this.flatItems[i];
			if(item.tmpvalue && item.multiSelect) 
				item.value = item.tmpvalue;
		}
	},
			
	afterRender: function() {
		this.getForm().waitMsgTarget = this.getEl();
		Ext.ux.form.SeedForm.superclass.afterRender.apply(this, arguments);
	}
});

