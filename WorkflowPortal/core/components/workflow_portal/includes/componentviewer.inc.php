<?php
/*
 * Template Loader Class
 */

require_once("factory.inc.php");
require_once("dataviewer.inc.php");
require_once("zip_operations.inc.php");
require_once("recursive_operations.inc.php");

Class ComponentViewer {
	var $id;              // Unique id identifying this component viewer
	var $pc;              // Component Catalog
	var $dv;              // Data Viewer
	var $props;           // Wings Properties
	var $abstract;        // If this is operating on the abstract layer only
	var $config;          // Portal Config
	var $sandbox;         // If the portal is running in sandbox mode
	var $factory;         // Ontology factory
	var $nsmap;           // Namespace to Prefix Mapping
	var $assets_url;      // Location of the Assets directory for our component
	var $op_doc_id;       // The componentOperations document id
	var $load_concrete;	 // Whether we should load the concrete component library or not
	var $libname;         // The concrete component library (if loaded) that we are using

	var $modx;            // Modx

	function ComponentViewer($guid, $props, $config, $assets_url, $sandbox, $modx=null, $load_concrete=false) {
		$this->id = $guid;
		$this->props = $props;
		$this->config = $config;
		$this->assets_url = $assets_url;
		$this->modx = $modx;
		$this->sandbox = $sandbox;
		$this->load_concrete = $load_concrete;
		
		$this->factory = new Factory($this->props);
		$this->pc = $this->factory->getPC($this->load_concrete);
		if($this->load_concrete) {
			$this->libname = $this->factory->getPCLibname();
		}

		$this->dv = new DataViewer($guid, $props, $config, $assets_url, $modx);

		$this->nsmap = array_flip($props->getDCPrefixNSMap());
		$this->nsmap = array_merge($this->nsmap, array_flip($props->getPCPrefixNSMap()));
	}

	function setOpsDocId($op_doc_id) {
		$this->op_doc_id = $op_doc_id;
	}

	function show() {
		$id = $this->id;
		$this->showHeader();
?>

<script>
var PHPSESSID = '<?php echo session_id(); ?>';
var ASSETS_URL = "<?=$this->assets_url?>";
var USER_DATA_URL = "<?=USER_HOME_URL.DATA_DIR?>";
var LOAD_CONCRETE = <?=($this->load_concrete ? "1" : "0")?>;
var LIBNAME = "<?=$this->libname ? $this->libname : "null"?>";
</script>

<div id="comppanel_<?=$id?>"></div>
<script>
// Template Tab Panel (tabPanel) into which new tabs for each template are opened
var tabPanel_<?=$id?>;
Ext.onReady(function() {
	tabPanel_<?=$id?> = initializeComponentBrowser(
		"<?=$id?>", "comppanel_<?=$id?>", '100%', 800, 
		{
			tree: <?=$this->getComponentHierarchyJSON(true)?>, 
			types: <?=$this->dv->getAllDatatypesJSON()?>, 
		},
		'<?=USER_DOMAIN?>', '[[~<?=$this->op_doc_id?>]]', <?=($this->sandbox?"false":"true")?>, <?=($this->abstract?"false":"true")?>);
});
</script>
<?
	}

	function showHeader() {
		loadTheme();
		loadExtJS();
		loadPortalJS();
		loadSWFUploadJS();
		print "<style>.swfupload { position: absolute; z-index: 1; vertical-align: top; text-align: center; } div#content {margin:0px;}</style>\n";
	}

	/**
	 * Private Functions
	 */
	
	private function error($lex) {
		return $this->modx ? $this->modx->lexicon("workflowPortal.".$lex) : $lex;
	}
	
	private function getPrefixedTerm($resource, $nsmap) {
		if(isset($resource->uri)) {
			$ns = $resource->getNamespace();
			if(isset($nsmap[$ns]))
			return ($nsmap[$ns]?$nsmap[$ns].':':'').$resource->getLocalName();
			else
			return $resource->uri;
		}
		else {
			$str = '"'.$resource->label.'"';
			if($resource->dtype) {
				$str .= '^^'.preg_replace('/.*#(.*)$/',"xsd:$1",$resource->dtype);
			}
			return $str;
		}
	}

   private function jsonForModx($obj) {
		return preg_replace('/\[\[/','[ [',json_encode($obj));
   }

	public function getComponentHierarchyJSON($noDetails) {
		if(!$this->pc) return "[]";
		$comps = $this->pc->getComponentHierarchy(null, $noDetails);
		return $this->jsonForModx($comps);
	}

	public function getComponentJSON($cid) {
		if(!$this->pc) return "[]";
		$rns = array_flip($this->nsmap);
		$cid = $cid ? $rns['acdom'].$cid : null;
		$inputs = $this->pc->getComponentInputDetails($cid);
		$outputs = $this->pc->getComponentOutputDetails($cid);
		$rules = $this->pc->getComponentRules($cid);
		return $this->jsonForModx(array("inputs"=>$inputs, "outputs"=>$outputs, "rules"=>$rules));
	}

	private function createRole($cid, $role, $roletype, $rns) {
		$type = $role["types"];
		$default = $role["default"];
		$argid = $role["role"];
		$roleid = $cid."_".$argid;
		$roleobj = $this->pc->getRole($roleid);
		if($roletype == "DataArgument" && !$this->pc->checkRoleDimensionality($argid, $role['dim'])) 
			return null;
		
		if($roleobj) return null;
		$this->pc->createRole($roleid, $argid, $rns['ac'].$roletype, $role['pfx'], $role['dim']);
		if(is_array($type) && sizeof($type)) $type=$type[0];
		if($roletype == "ParameterArgument") {
			$this->pc->setRoleDefaultValue($roleid, $default, $type);
		}
		if($roletype == "DataArgument") {
			if($type != "DataObject") $type = $rns['dcdom'].$type;
			else $type = $rns['dc'].$type;
			$this->pc->addRoleType($roleid, $type);
		}
		return $roleid;
	}

	public function saveComponentJSON($cid, $comp_json) {
		if(!$this->pc) return "[]";
		$rns = array_flip($this->nsmap);
		$cid = $cid ? $rns['acdom'].$cid : null;

		// Remove all existing component assertions (re-assert it's type though)
		$ctype = $cid."Class";
		$this->pc->removeComponent($cid, false);
		$this->pc->addComponentForType($cid, $ctype);

		// Get Component Details
		$comp = json_decode($comp_json);
      $comp = get_object_vars($comp);

		$errors = array();
		foreach($comp["idata"] as $role) {
			$role = get_object_vars($role);
			$roleid = $this->createRole($cid, $role, "DataArgument", $rns);
			if(!$roleid) array_push($errors, "Input data role '".$role['role']."' is a duplicate (or has dimensionality conflicts). Use a different name");
			else $this->pc->addComponentInput($cid, $roleid);
		}
		foreach($comp["iparams"] as $role) {
			$role = get_object_vars($role);
			$roleid = $this->createRole($cid, $role, "ParameterArgument", $rns);
			if(!$roleid) array_push($errors, "Input parameter role '".$role['role']."' is a duplicate (or has dimensionality conflicts). Use a different name");
			else $this->pc->addComponentInput($cid, $roleid);
		}
		foreach($comp["odata"] as $role) {
			$role = get_object_vars($role);
			$roleid = $this->createRole($cid, $role, "DataArgument", $rns);
			if(!$roleid) array_push($errors, "Output data role '".$role['role']."' is a duplicate (or has dimensionality conflicts). Use a different name");
			else $this->pc->addComponentOutput($cid, $roleid);
		}
		$this->pc->setComponentIsConcrete($cid, $comp["concrete"]);
	
		if(count($errors)) return implode($errors, "\n");

		$this->factory->savePC($this->pc);
		return "OK";
	}

	public function newComponent($cid, $pid, $ptype) {
		if(!$this->pc) return "[]";
		$rns = array_flip($this->nsmap);
		$cid = $rns['acdom'].$cid;
		$ctype = $cid."Class";
		$ptype = $ptype ? $rns['acdom'].$ptype : null;
		$this->pc->addComponentType($ctype, $ptype);
		$this->pc->addComponentForType($cid, $ctype);

		// Copy over inputs and outputs from parent
		if($pid) {
			$pid = $rns['acdom'].$pid;
			$inputs = $this->pc->getComponentInputDetails($pid);
			$outputs = $this->pc->getComponentOutputDetails($pid);
			foreach($inputs as $ip) {
				$roleid = $this->createRole($cid, $ip, $ip['param']?"ParameterArgument":"DataArgument", $rns);
				$this->pc->addComponentInput($cid, $roleid);
			}
			foreach($outputs as $op) {
				$roleid = $this->createRole($cid, $op, "DataArgument", $rns);
				$this->pc->addComponentOutput($cid, $roleid);
			}
		}

		// If we are working with a concrete library
		if($this->load_concrete) {
			$this->pc->setComponentIsConcrete($cid, true);
		}

		$this->factory->savePC($this->pc);
		return "OK";
	}

	public function newCategory($ctype, $ptype) {
		if(!$this->pc) return "[]";
		$rns = array_flip($this->nsmap);
		$ctype = $rns['acdom'].$ctype;
		$ptype = $ptype ? $rns['acdom'].$ptype : null;
		$this->pc->addComponentType($ctype, $ptype);
		$this->factory->savePC($this->pc);
		return "OK";
	}

	public function setComponentIsConcrete($cid, $isConcrete) {
		if(!$this->pc) return "[]";
		$rns = array_flip($this->nsmap);
		$cid = $rns['acdom'].$cid;
		$this->pc->setComponentIsConcrete($cid, $isConcrete);
		$this->factory->savePC($this->pc);
		return "OK";
	}

	public function delComponent($cid) {
		$rns = array_flip($this->nsmap);
		$cid = $rns['acdom'].$cid;
		$ctype = $cid."Class";
		$this->delCompHelper($cid, $ctype);
		$this->factory->savePC($this->pc); 
		return "OK";
	}

	public function delCategory($ctype) {
		$rns = array_flip($this->nsmap);
		$ctype = $rns['acdom'].$ctype;
		if($ctype) $this->pc->removeComponentType($ctype);
		$this->factory->savePC($this->pc); 
		return "OK";
    }

	private function delCompHelper($cid, $ctype) {
		$subComps = $this->pc->getSubComponents($ctype);
		foreach($subComps as $subC) {
			$this->delCompHelper($subC['id'], $subC['cls']); 
		}
		if($ctype) $this->pc->removeComponentType($ctype);
		if($cid) $this->pc->removeComponent($cid, true);
	}

	public function downloadComponent($cid, $tocid) {
		if(!$this->load_concrete) {
			return "Can only download concrete components\n";
		}
		$codedir = USER_HOME_PATH.CODE_DIR.($cid=="skeleton"?"":$this->libname."/").$cid;

		$zname = uniqid($cid."_").".zip";
		$tmpzip = sys_get_temp_dir()."/".$zname;
		new ZipFolder($tmpzip, $codedir, $tocid);

		$fp = fopen("$tmpzip","r");
		header("Content-disposition: attachment; filename=$zname");
		header("Content-type: application/zip");
		readfile($tmpzip);
		@unlink($tmpzip);
	}

	public function getAllRules() {
		return $this->pc->getAllRules();
	}

	public function saveAllRules($rules) {
		$this->pc->saveAllRules($rules);
		return "OK";
	}
}


