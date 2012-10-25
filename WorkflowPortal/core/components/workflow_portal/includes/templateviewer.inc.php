<?php
/*
 * Template Loader Class
 */

require_once("factory.inc.php");

Class TemplateViewer {
	var $id;              // Unique id identifying this template viewer
	var $dc;              // Data Catalog
	var $pc;              // Process (Component) Catalog
	var $beamer;          // Beamer (used for TellMe)
	var $props;           // Wings Properties
	var $config;          // Portal Config
	var $template;        // Template
	var $template_id;     // Template ID
	var $template_name;   // Template Name
	var $factory;         // Ontology factory
	var $nsmap;           // Namespace to Prefix Mapping
	var $assets_url;      // Location of the Assets directory for our component
	var $run_doc_id;      // The workflowRunner document id
	var $op_doc_id;       // The templateOperations document id
	var $res_doc_d;       // The accessResults document id
	var $editor_mode;     // Flag to indicate that this includes editing operations
	var $graph_only;      // Flag to indicate "graph only", i.e. do not init dc/pc

	var $modx;            // Modx

	function TemplateViewer($tid, $guid, $props, $config, $assets_url, $editor_mode, $modx=null, $graph_only=false) {
		$this->id = $guid;
		$this->props = $props;
		$this->config = $config;
		$this->assets_url = $assets_url;
		$this->modx = $modx;
		
		$this->factory = new Factory($this->props);
		if(!$graph_only || $editor_mode) {
			$this->dc = $this->factory->getDC();
			//FIXME: Now always showing concrete components (even in editor mode)
			//$this->pc = $this->factory->getPC($editor_mode ? false : true);
			$this->pc = $this->factory->getPC(true);
		}
		$this->editor_mode = $editor_mode;
		if($editor_mode == 'tellme') 
			$this->beamer = $this->factory->getBeamer();

		$this->graph_only = $graph_only;

		$this->nsmap = array_flip($props->getDCPrefixNSMap());
		$this->nsmap = array_merge($this->nsmap, array_flip($props->getPCPrefixNSMap()));

		if($tid) $this->loadTemplate($tid);
	}

	function setRunDocId($run_doc_id) {
		$this->run_doc_id = $run_doc_id;
	}

	function setOpsDocId($op_doc_id) {
		$this->op_doc_id = $op_doc_id;
	}

	function setResultsDocId($res_doc_id) {
		$this->res_doc_id = $res_doc_id;
	}

	function loadTemplate($tid) {
		$this->template_id = $tid;
		$this->template_name = preg_replace("/_/"," ",$tid);
		$this->template = $this->factory->getTemplate($tid);
	}

	function show($options) {
		$id = $this->id;
		$this->showHeader($options);
		$optjson = $this->jsonForModx($options);
?>

<div id="panel_<?=$id?>"></div>
<script>

// Template Tab Panel (tabPanel) into which new tabs for each template are opened
var tabPanel_<?=$id?>;
Ext.onReady(function() {
	Ext.QuickTips.init();
	/*if(Ext.isIE) {
		Ext.MessageBox.show({msg:"This site does not work well with Internet Explorer. Please use another browser like Chrome, Safari or Firefox. Sorry for the inconvenience"});
	}*/
	var opts = <?=$optjson?>;
	var height = opts.height ? parseInt(opts.height):getPanelHeight()
	tabPanel_<?=$id?> = initializeTemplateBrowser(
		"<?=$id?>", "panel_<?=$id?>", '100%', height, opts,
		{
			tree: <?=$this->getTemplatesTreeJSON()?>, 
			inputs: <?=$this->getInputsJSON()?>, 
			template: <?=$this->getTemplateJSON()?>, 
			propvals: <?=$this->getConstraintPropertyValuesJSON()?>,
			components: <?=$this->getComponentsJSON()?>,
			data: <?=$this->getDataJSON()?>,
			beamer_paraphrases: <?=$this->getBeamerParaphrasesJSON()?>,
			beamer_mappings: <?=$this->getBeamerMappingsJSON()?>
		},
		'<?=$this->template_id?>', '<?=$this->template_name?>', '<?=USER_DOMAIN?>', 
		'[[~<?=$this->run_doc_id?>]]', '[[~<?=$this->op_doc_id?>]]', '[[~<?=$this->res_doc_id?>]]'
	);

	if(!opts.height) {
		var doResize = function() {
			tabPanel_<?=$id?>.setHeight(getPanelHeight());
		}
		// Resize on demand
		Ext.EventManager.onWindowResize(doResize);
	}
});

function getPanelHeight() {
	var h = Ext.getDoc().getViewSize(false).height-140;
	if(h < 400) h = 400;
	return h;
}

</script>
<?
	}

	function showHeader($options) {
		loadTheme();
		print "<style>body{margin-left:50px;margin-right:50px}</style>\n";

		loadCanvasTextJS();
		loadExtJS();
		loadPortalJS();
		loadTellMeJS();
	}


	/**
	* Get All Templates in the current domain
	*/
	function getTemplatesTreeJSON() {
		$domain = $this->props->getTemplateDomain();
		$onthome = $this->props->getOntologyDir();

		$templatedir = "${onthome}$domain";
		$templates = array();
		if ($handle = opendir($templatedir)) {
			while (false !== ($file = readdir($handle))) {
				if(is_dir($templatedir.'/'.$file)) continue;
				if(preg_match('/(.*)\.owl$/i',$file,$m)) {
					array_push($templates, $m[1]);
				}
			}
		}
		return $this->jsonForModx($templates);
	}


	/**
	 * Private Functions
	 */
	
	private function error($lex) {
		return $this->modx ? $this->modx->lexicon("workflow_portal.".$lex) : $lex;
	}
	
	private function printVariableSelector($template) {
		$variables = $template->getVariables();
		print "<select style='display:none' id='variable_selector_".$this->id."'>";
		foreach($variables as $var) {
			$term = $var->getName();
			print "<option value='$term'>$term</option>\n";
		}
		print "</select>";
	}

   private function jsonForModx($obj) {
		return preg_replace('/\[\[/','[ [',json_encode($obj));
   }

	// Move to Component Operations
	public function getComponentsJSON() {
		if(!$this->pc) return "[]";
		//if(!$this->pc || !$this->editor_mode) return "[]";
		$comps = $this->pc->getComponentHierarchy(null, $this->editor_mode ? false: true);
		return $this->jsonForModx($comps);
	}

	public function getDataJSON() {
		if(!$this->dc || !$this->editor_mode) return "[]";
		$data = $this->dc->getDataHierarchy();
		return $this->jsonForModx($data);
	}

	public function getConstraintPropertyValuesJSON() {
		if(!$this->dc || !$this->editor_mode) return "{}";
		$props = $this->dc->getConstraintProperties(true,true);
		return $this->jsonForModx($props);
	}

	public function getInputsJSON() {
		if(!$this->template || $this->editor_mode || $this->graph_only) return "[]";
		$inputData = array();
		$template = $this->template;

		$nsmap = $template->getNSMap();
		$links = $template->getInputLinks();
		$iroles = $template->getInputRoles();
		$varDims = array();
		foreach($iroles as $irole) {
			$varobj = $irole->getVariableMapping();
			$varDims[$varobj] = $irole->getDimensionality();
		}
		
		$varsdone = array();
		foreach($links as $link) {
			$var = $link->getVariable();

			if($varsdone[$var->getId()]) continue;
			$varsdone[$var->getId()] = 1;
			$roles = $var->getRoles();
			if(!sizeof($roles)) continue;

			$name = $var->getName();
			$comp = $link->getDestinationNode()->getComponent();
			$binding = $var->getBinding();

			$variable = array("name"=>$name);
			if($var->isData()) {
				$variable["type"] = "data";
				$rtype = $this->pc->getRoleType($comp->getType(), $roles[0]->getId());
				$clsids = $this->dc->getDataClassesAndInstances($rtype);
				$dim = array_key_exists($var->getId(),$varDims) ? $varDims[$var->getId()] : $roles[0]->getDimensionality();
		
				$items = array();
				foreach($clsids as $cls=>$ids) {
					foreach($ids as $id) {
						$idr = new Resource($id);
						$idn = $idr->getLocalName();
						array_push($items, $idn);
					}
				}
				$variable["options"] = $items;
				$variable ["dim"] = $dim?1:0;
				//Todo: Get binding name (names for lists)
				if($binding) {
					$b = new Resource($binding);
					$variable["binding"] = $b->getLocalName();
				}
			}
			else if($var->isParam()) {
				$variable["type"] = "param";
				$rtype = $this->pc->getRoleDataType($comp->getType(), $roles[0]->getId());
				$typename = "";
				if($rtype) {
					$typer = new Resource($rtype);
					$typename = $typer->getLocalName();
				}
				$variable["dtype"] = $typename;
				$bindingval = $binding? $binding->label: "";
				$variable["binding"] = $binding;
			}
			if(!$var->getAutoFill())
				array_push($inputData, $variable);
		}
		sort($inputData);
		return $this->jsonForModx($inputData);
	}

	public function getTemplateJSON() {
		if(!$this->template) return "[]";
		return $this->template->getJSON();
	}

	public function getBeamerParaphrasesJSON() {
		if(!$this->beamer) return "[]";
		return $this->beamer->getParaphrasesJSON();
	}
	public function getBeamerMappingsJSON() {
		if(!$this->beamer) return "[]";
		return $this->beamer->getMappingsJSON();
	}
}


