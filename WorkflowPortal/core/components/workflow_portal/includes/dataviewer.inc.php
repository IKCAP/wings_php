<?php
/*
 * Template Loader Class
 */

require_once("factory.inc.php");

Class DataViewer {
	var $id;              // Unique id identifying this data viewer
	var $dc;              // Data Catalog
	var $props;           // Wings Properties
	var $config;          // Portal Config
	var $factory;         // Ontology factory
	var $sandbox;         // If the portal is running in sandbox mode
	var $nsmap;           // Namespace to Prefix Mapping
	var $assets_url;      // Location of the Assets directory for our component
	var $op_doc_id;       // The dataOperations document id

	var $modx;            // Modx

	function DataViewer($guid, $props, $config, $assets_url, $sandbox, $modx=null) {
		$this->id = $guid;
		$this->props = $props;
		$this->config = $config;
		$this->assets_url = $assets_url;
		$this->modx = $modx;
		$this->sandbox = $sandbox;

		$this->factory = new Factory($this->props);
		$this->dc = $this->factory->getDC();

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
</script>

<div id="datapanel_<?=$id?>"></div>
<script>
// Template Tab Panel (tabPanel) into which new tabs for each template are opened
var tabPanel_<?=$id?>;
Ext.onReady(function() {
	tabPanel_<?=$id?> = initializeDataBrowser(
		"<?=$id?>", "datapanel_<?=$id?>", '100%', 800, 
		{
			tree: <?=$this->getDataHierarchyJSON()?>
		},
		'<?=USER_DOMAIN?>', '[[~<?=$this->op_doc_id?>]]', <?=($this->sandbox?"false":"true")?>);
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
		return $this->modx ? $this->modx->lexicon("workflow_portal.".$lex) : $lex;
	}

	private function getLocalTerm($resource, $nsmap) {
		if(isset($resource->uri)) {
			return $resource->getLocalName();
		}
		else {
			$str = '"'.$resource->label.'"';
			if($resource->dtype) {
				$str .= '^^'.preg_replace('/.*#(.*)$/',"$1",$resource->dtype);
			}
			return $str;
		}
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

	public function getDataHierarchyJSON() {
		if(!$this->dc) return "[]";
		$data = $this->dc->getDataHierarchy();
		return $this->jsonForModx($data["children"]);
	}

	public function getAllDataTypesJSON() {
		if(!$this->dc) return "[]";
		return $this->jsonForModx($this->dc->getAllDatatypes(null, true));
	}

	public function getDataJSON($dataid) {
		if(!$this->dc) return "[]";
		$ns = $this->nsmap;
		$rns = array_flip($ns);
		$dataurl = USER_HOME_URL.DATA_DIR.$dataid;
		$dataid = $rns['dclib'].$dataid;
		$dtype = $this->dc->getDatatype($dataid);
		$props = $this->dc->getDatatypeProperties($dtype);
		$vals = $this->dc->getDatasetPropertyValues($dataid, array_keys($props));

		$prefixedProps = array();
		foreach($props as $prop=>$pinfo) {
			$prop = $this->getLocalTerm(new Resource($prop), $ns);
			$prefixedProps[$prop] = array(
				"range"=>$this->getLocalTerm(new Resource($pinfo['range']), $ns), 
				"editable"=>$pinfo['editable']
			);
		}
		$dtypeshort = $this->getLocalTerm(new Resource($dtype), $ns); 

		$prefixedVals = array();
		foreach($vals as $prop=>$val) {
			$prefixedVals[$this->getLocalTerm(new Resource($prop), $ns)] = $val;
		}

		return $this->jsonForModx(array("dtype"=>$dtypeshort, "url"=>$dataurl,
													"props"=>$prefixedProps, "vals"=>$prefixedVals));
	}

	public function getDataTypeJSON($dtype) {
		if(!$this->dc) return "[]";
		$rns = array_flip($this->nsmap);
		$props = $this->dc->getDatatypeProperties( $dtype ? $rns['dcdom'].$dtype: null );

		$prefixedProps = array();
		foreach($props as $prop=>$pinfo) {
			$prop = $this->getLocalTerm(new Resource($prop), $ns);
			$prefixedProps[$prop] = array(
				"range"=>$this->getLocalTerm(new Resource($pinfo['range']), $ns), 
				"editable"=>$pinfo['editable']
			);
		}
		return $this->jsonForModx($prefixedProps);
	}

	public function saveDataJSON($dataid, $json) {
		if(!$this->dc) return "[]";
		$ns = $this->nsmap;
		$rns = array_flip($ns);

		$propVals = json_decode($json);
		$dataid = $rns['dclib'].$dataid;

		$dtype = $this->dc->getDatatype($dataid);
		$props = $this->dc->getDatatypeProperties($dtype);

		$this->dc->removeAllPropertyValues($dataid, array_keys($props));
		foreach($propVals as $propVal) {
			$propVal = get_object_vars($propVal);
			$prop = $rns['dcdom'].$propVal['name'];
			$pinfo = $props[$prop];
			$value = $propVal['value']."";
			if($pinfo && $value != "") {
				$range = $pinfo['range'];
				$this->dc->addDatatypePropertyValue($dataid, $prop, $value, $range);
			}
		}
		$this->factory->saveDC($this->dc);
		return "OK";
	}

	private function sanitize($name) {
		$name = preg_replace('/^[0-9]/','',$name);
		$name = preg_replace('/[^a-zA-Z0-9_]/','_',$name);
		return $name;
	}

	public function saveDataTypeJSON($dtype, $json, $force) {
		if(!$this->dc) return "[]";
		$ns = array_merge($this->nsmap, $this->dc->nsmap);
	
		$dtype = $dtype ? $ns['dcdom'].$dtype : null;
		$allprops = $this->dc->getAllMetadataProperties(); 

		$errors = array();
		$rename = array();
		$ops = get_object_vars(json_decode($json));
		foreach($ops['add'] as $propid=>$prop) {
			$prop = get_object_vars($prop);
			if(!$prop['range']) {
				array_push($errors, "No range specified for property $propid");
				continue;
			}
			
			$spropid = $this->sanitize($propid);
			if(!$spropid) {
				array_push($errors, "Invalid name: $propid");
				continue;
			}
			if($spropid != $propid) {
				$rename[$propid] = $spropid;
			}

			$eprop = $allprops[$ns['dcdom'].$spropid];
			if($eprop) {
				array_push($errors, "A property with name $spropid already exists");
				continue;
			}

			$this->dc->addMetadataProperty($ns['dcdom'].$spropid, $dtype, $ns['xsd'].$prop['range']);
		}

		foreach($ops['del'] as $propid=>$dummy) {
			$this->dc->removeMetadataProperty($ns['dcdom'].$propid);
		}

		foreach($ops['mod'] as $propid=>$prop) {
			$prop = get_object_vars($prop);
			if(!$prop['range']) {
				array_push($errors, "No range specified for property $propid");
				continue;
			}
			$npropid = $prop['prop'];
			$spropid = $this->sanitize($npropid);
			if(!$spropid) {
				array_push($errors, "Invalid name: $propid");
				continue;
			}
			if($spropid != $npropid) {
				$rename[$npropid] = $spropid;
			}
			$spropid = $ns['dcdom'].$spropid;
			$propid = $ns['dcdom'].$propid;
			$srange = $ns['xsd'].$prop['range'];

			$eprop = $allprops[$propid];
			if($eprop['range'] != $srange) {
				$this->dc->removeMetadataProperty($propid);
				$this->dc->addMetadataProperty($spropid, $dtype, $srange);
			}
			else if($propid != $spropid) {
				$this->dc->renameMetadataProperty($propid, $spropid);
			}
		}

		$this->factory->saveDC($this->dc);
		return $this->jsonForModx(array("errors"=>$errors, "rename"=>$rename));
	}

	public function newDataType($ptype, $dtype) {
		if(!$this->dc) return "";
		$ns = array_merge($this->nsmap, $this->dc->nsmap);
		if(!$ptype) $ptype = $ns['dc']."DataObject";
		else $ptype = $ns['dcdom'].$ptype;
		$dtype = $ns['dcdom'].$dtype;
		$this->dc->addDatatype($dtype, $ptype);
		$this->factory->saveDC($this->dc);
		return "OK";
	}

	public function delDataTypes($dtypes) {
		if(!$this->dc) return "";
		$ns = array_merge($this->nsmap, $this->dc->nsmap);
		if(!$dtypes) return "";
		foreach($dtypes as $dtype) {
			$dtype = $ns['dcdom'].$dtype;
			$this->dc->removeDatatype($dtype);
		}
		$this->factory->saveDC($this->dc);
		return "OK";
	}

	public function delData($did) {
		if(!$this->dc) return "";
		$ns = array_merge($this->nsmap, $this->dc->nsmap);
		$this->dc->removeData($ns['dclib'].$did, $did);
		$this->factory->saveDC($this->dc);
		return "OK";
	}

	public function moveDatatypeTo($dtype, $fromtype, $totype) {
		if(!$this->dc) return "";
		$ns = array_merge($this->nsmap, $this->dc->nsmap);
		if(!$dtype) return "";
		$dtype = $ns['dcdom'].$dtype;
		$fromtype = $fromtype ? $ns['dcdom'].$fromtype : $this->dc->dcThing;
		$totype = $totype ? $ns['dcdom'].$totype : $this->dc->dcThing;
		$this->dc->moveDatatypeTo($dtype, $fromtype, $totype);
		$this->factory->saveDC($this->dc);
		return "OK";
	}

	public function addDataForType($ids, $dtype) {
		if(!$this->dc) return "";
		$ns = $this->dc->nsmap;
		$dtype = $dtype ? $ns['dcdom'].$dtype : $this->dc->dcThing;
		foreach($ids as $id) {
			$this->dc->addDataForType($ns['dclib'].$id, $dtype);
		}
		$this->factory->saveDC($this->dc);
		return "OK";
	}
}


