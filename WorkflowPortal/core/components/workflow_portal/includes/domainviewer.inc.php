<?php
/*
 * Template Loader Class
 */

require_once("factory.inc.php");
require_once("recursive_operations.inc.php");

Class DomainViewer {
	var $id;              // Unique id identifying this component viewer
	var $pc;              // Component Catalog
	var $dv;              // Data Viewer
	var $props;           // Wings Properties
	var $config;          // Portal Config
	var $factory;         // Ontology factory
	var $nsmap;           // Namespace to Prefix Mapping
	var $assets_url;      // Location of the Assets directory for our component
	var $op_doc_id;       // The componentOperations document id

	var $modx;            // Modx

	function DomainViewer($guid, $props, $config, $assets_url, $modx=null) {
		$this->id = $guid;
		$this->props = $props;
		$this->config = $config;
		$this->assets_url = $assets_url;
		$this->modx = $modx;
		
		$this->factory = new Factory($this->props);
		$this->pc = $this->factory->getPC(true);
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
var ASSETS_URL = "<?=$this->assets_url?>";
</script>

<div id="domainpanel_<?=$id?>"></div>
<script>
// Template Tab Panel (tabPanel) into which new tabs for each template are opened
var tabPanel_<?=$id?>;
Ext.onReady(function() {
	var height = getPanelHeight()
	tabPanel_<?=$id?> = initializeDomainBrowser(
		"<?=$id?>", "domainpanel_<?=$id?>", '100%', height, 
		{
			domains: <?=$this->getAvailableDomainsJSON()?>

		},
		'<?=USER_DOMAIN?>', '[[~<?=$this->op_doc_id?>]]', true
	);

	var doResize = function() {
		tabPanel_<?=$id?>.setHeight(getPanelHeight());
	}
	// Resize on demand
	Ext.EventManager.onWindowResize(doResize);
});

function getPanelHeight() {
	var h = Ext.getDoc().getViewSize(false).height-140;
	if(h < 400) h = 400;
	return h;
}

</script>
<?
	}

	function showHeader() {
		loadTheme();
		loadExtJS();
		loadPortalJS();
		print "<style>body{margin-left:50px;margin-right:50px}</style>\n";
	}

	function getTemplatesTree() {
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
		return $templates;
	}

	function getTemplatesTreeJSON() {
		return $this->jsonForModx($this->getTemplatesTree());
	}


	function getSystemDomainInfo($name) {
		$sdir = DOMAINS_TOP_DIR.$name;
		$dom = array("domain"=>$name, "size"=>recursive_size($sdir), "install_time"=>filemtime($sdir));
		//$dom = array("domain"=>$name, "install_time"=>filemtime($sdir));
		if($name == DOMAIN) $dom["sys_default"] = true;
		return $dom;
	}

	function countComponents($ctree) {
		$ccount=0; $acount=0;
		if(!is_array($ctree)) $ctree = array($ctree);
		for($i=0; $i<count($ctree); $i++) {
			$c = $ctree[$i];
			if($c['id']) {
				if($c['concrete']) $ccount++;
				else $acount++;
			}
			if($c['children']) {
				$counts = $this->countComponents($c['children']);
				$ccount+= $counts[0];
				$acount+= $counts[1];
			}
		}
		return array($ccount, $acount);
	}

	function countData($dtree) {
		$dcount=0; $fcount=0;
		foreach($dtree as $dtype=>$children) {
			$dcount++;
			if($children) {
				$fcount += count($children);
			}
		}
		return array($dcount, $fcount);
	}

	function getUserDomainInfo($name) {
		$udir = USER_DOMAINS_TOP_DIR.$name;
		if(!is_dir($udir)) return null;

		$dom = array("domain"=>$name, "size"=>recursive_size($udir), "install_time"=>filemtime($udir));
		//$dom = array("domain"=>$name, "install_time"=>filemtime($udir));
		if(is_dir(DOMAINS_TOP_DIR.$name)) {
			$dom["out_of_date"] = $this->getChangedFiles(DOMAINS_TOP_DIR.$name, '', filemtime($udir));
		}
		else {
			$dom["user_defined"] = true;
		}
		$this->factory = new Factory(USER_DOMAINS_TOP_DIR.$name."/", true);
		$this->props = $this->factory->props;

		$dom["id"] = $name;
		$dom["output_format"] = $this->props->getOutputFormat();
		$dom["iconCls"] = "undefIcon";

		// Read default component library (set default to library.owl if there is no current default)
		$acdir = USER_DOMAINS_TOP_DIR.$name."/".ONTOLOGY_DIR."ac/".$this->props->getPCDomain()."/";
		$libname = $this->factory->getPCLibname();

		// Set components dir if it isn't already set properly
		$code_dir = "$udir/".CODE_DIR."$libname/";
		if($this->props->getPCPropertyForCurrentDomain("components.dir") != $code_dir) {
   		$this->props->setPCPropertyForCurrentDomain("components.dir", $code_dir);
   		$this->props->storeWingsProperties("$udir/wings.properties");
		}

		// Get all available concrete component libraries
		// $dom["expanded"] = true;
		$dom["children"] = array();
		if ($handle = opendir($acdir)) {
			while (false !== ($file = readdir($handle))) {
				if(is_dir($acdir.'/'.$file)) continue;
				if(preg_match('/(.*)\.owl$/i',$file,$m)) {
					if($m[1] != "abstract") {
						$lib = $m[1];
						$libinfo = array("domain"=>$lib, "details"=>array());
						$this->pc = $this->factory->getPC(true, $lib);
						$libinfo["details"]["c"] = $this->countComponents($this->pc->getComponentHierarchy());
						$libinfo["leaf"] = true;
						$libinfo["islib"] = true;
						$libinfo["iconCls"] = "undefIcon";
						$libinfo["id"] = $name."_".$lib;
						if($lib == $libname) {
							$libinfo["selected"] = true;
							$libinfo["iconCls"] = "defIcon";
						}
						array_push($dom["children"], $libinfo);
					}
				}
			}
		}
		$this->dc = $this->factory->getDC();
		$numtemps = count($this->getTemplatesTree());
		$numdata = $this->countData($this->dc->getDataClassesAndInstances());
		$dom["details"] = array("t"=>$numtemps, "d"=>$numdata);
		return $dom;
	}

	function getAvailableDomainsJSON() {
		$system_domains = array();
		$user_domains = array();

		$dtd = DOMAINS_TOP_DIR;
		if ($handle = opendir($dtd)) {
			while (false !== ($file = readdir($handle))) {
				if(preg_match('/^\./',$file)) continue;
				if(is_dir($dtd.$file)) {
					$dom = $this->getSystemDomainInfo($file);
					array_push($system_domains, $dom);
				}
			}
		}

		$utd = USER_DOMAINS_TOP_DIR;
		if ($handle = opendir($utd)) {
			while (false !== ($file = readdir($handle))) {
				if(preg_match('/^\./',$file)) continue;
				if(is_dir($utd.$file)) {
					$dom = $this->getUserDomainInfo($file);
					if($file == USER_DOMAIN) {
						$dom["user_default"] = true;
						$dom["iconCls"] = "defIcon";
					}
					else $dom["iconCls"] = "undefIcon";
					array_push($user_domains, $dom);
				}
			}
		}
		return $this->jsonForModx(array("system"=>$system_domains, "user"=>$user_domains));
	}

	public function importDomain($domain, $clean_current) {
		if($clean_current) 
			$this->deleteDomain($domain);

		if(createNewUserSpace($domain, $domain)) {
			$dom = $this->getUserDomainInfo($domain);
			return $this->jsonForModx($dom);
		}
		return "Error";
	}

	public function setDefaultDomain($domain) {
		$dom = $this->getUserDomainInfo($domain);
		if($dom == null) return null;

		$fh = fopen(USER_DOMAIN_FILE, "w"); 
		fwrite($fh, $domain);
		fclose($fh);

		$dom["user_default"] = true;
		$dom["iconCls"] = 'defIcon';
		return $this->jsonForModx($dom);
	}

	public function setDefaultComponentLibrary($domain, $libname) {
      $domdir = USER_DOMAINS_TOP_DIR."$domain/";
      $fac = new Factory($domdir, true);
		if($fac->setPCLibname($libname)) {
   		$fac->props->setPCPropertyForCurrentDomain("components.dir", $domdir.CODE_DIR."$libname/");
   		$fac->props->storeWingsProperties($domdir."wings.properties");
		}
		$dom = $this->getUserDomainInfo($domain);
		return $this->jsonForModx($dom);
	}

	public function setDomainOutputFormat($domain, $format) {
		$dom = $this->getUserDomainInfo($domain);
      $this->props->setOutputFormat($format);
      $user_home_path = USER_DOMAINS_TOP_DIR."$domain/";
		$this->props->storeWingsProperties($user_home_path."wings.properties");
		$dom["output_format"] = $format;
		return $this->jsonForModx($dom);
	}

	public function deleteDomain($domain) {
		recursive_unlink(USER_DOMAINS_TOP_DIR.$domain);
		return "OK";
	}

	public function deleteComponentLibrary($domain, $libname) {
      $fac = new Factory(USER_DOMAINS_TOP_DIR."$domain/", true);
		$pcdir = $fac->getPCOntologyDir();

		@unlink("$pcdir/$libname.owl");
		@unlink("$pcdir/$libname.rules");

      $user_code_path = USER_DOMAINS_TOP_DIR."$domain/".CODE_DIR.$libname;
		recursive_unlink($user_code_path);
		return "OK";
	}

	public function newDomain($domain) {
		if(createNewUserSpace("blank", $domain)) {
			$dom = $this->getUserDomainInfo($domain);
			return $this->jsonForModx($dom);
		}
		return "Error";
	}

	public function newComponentLibrary($domain, $libname) {
		if(createNewComponentLibrary("blank", $domain, "library", $libname)) {
			$dom = $this->getUserDomainInfo($domain);
			return $this->jsonForModx($dom);
		}
		return "Error";
	}

	/**
	 * Private Functions
	 */
    private function jsonForModx($obj) {
        return preg_replace('/\[\[/','[ [',json_encode($obj));
    }

	private function getChangedFiles($dirstr, $subdir, $time) {
		@$dir = opendir($dirstr."/".$subdir); 
		if(!$dir){ return 0; }

		$mods = array();
		while($file = readdir($dir)){
			$filepath="$dirstr/$subdir/$file";
			if(is_dir($filepath)) {
				if(!preg_match('/^\./', $file))
					$mods = array_merge($mods, $this->getChangedFiles($dirstr, "$subdir/$file", $time));
			}
			else {
				if(is_file($filepath)) {
					$last_modified = filemtime($filepath);
					if($time < $last_modified) {
						array_push($mods, array("name"=>"$subdir/$file", "time"=>$last_modified));
					}
				}
			}
		} 
		return $mods;
	}
}


