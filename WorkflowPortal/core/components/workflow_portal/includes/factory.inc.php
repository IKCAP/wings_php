<?php
require_once("processcatalog.inc.php");
require_once("datacatalog.inc.php");
require_once("template.inc.php");
require_once("prophelper.inc.php");

Class Factory {
   var $dc;
   var $pc;
   var $props;
	var $dir;

   function Factory($arg, $is_dir=null) {
		if(!$is_dir) {
      	$this->props = $arg;
			$this->dir = USER_HOME_PATH;
		}
		else {
			$this->props = new PropertiesHelper($arg."/wings.properties");
			$this->dir = $arg;
		}
   }

	function serialize($obj, $file) {
		$fh = fopen($file, "w");
		fwrite($fh, serialize($obj));
		fclose($fh);
	}

	function getPCOntologyDir() {
      $onthome = $this->props->getOntologyDir();
      $pchome = $onthome.$this->props->getPCPropertyForCurrentDomain("directory")."/".$this->props->getPCDomain()."/";
		return $pchome;
	}

	function getDCOntologyDir() {
      $onthome = $this->props->getOntologyDir();
      $dchome = $onthome.$this->props->getDCPropertyForCurrentDomain("directory")."/".$this->props->getDCDomain()."/";
		return $dchome;
	}

	function getPCLibname() {
		$pcdir = $this->getPCOntologyDir();
		if(!file_exists($pcdir.".default")) {
			$fh = fopen($pcdir.".default", "w");
			fwrite($fh, "library");
		}
		$libname = file_get_contents($pcdir.".default");
		return $libname;
	}

	function setPCLibname($libname) {
		$pcdir = $this->getPCOntologyDir();
		if(!file_exists("$pcdir/$libname.owl")) 
			return false;
		$fh = fopen($pcdir.".default", "w");
		fwrite($fh, $libname);
		return true;
	}

   function getPC($load_concrete=false, $libname=null) {
      $pcfactory = $this->props->getPCFactory();
      $pcdomain = $this->props->getPCDomain();
		$libname = $load_concrete ? ($libname ? $libname : $this->getPCLibname()) : null;
		$pchome = $this->getPCOntologyDir();
		$abslibfile = $pchome."abstract.owl";
		$pclibfile = $load_concrete ? $pchome."$libname.owl" : null;
      $pcrulesfile = $load_concrete ? $pchome."$libname.rules" : $pchome."abstract.rules";
		$serfile = $load_concrete ? "$pclibfile.ser" : "$abslibfile.ser";

		$top_code_dir = $this->dir.CODE_DIR;
		$lib_code_dir = $top_code_dir.($load_concrete ? $libname."/" : "");

      $pc = null;
      if($pcfactory == "internal") {
			//if(file_exists($serfile)) {
			//	$pc = unserialize(file_get_contents($serfile));
			//} else {
         	$pc = new ProcessCatalog($abslibfile, $pclibfile, $pcrulesfile, $top_code_dir, $lib_code_dir, $this->props);
			//	$this->serialize($pc, $serfile);
			//}
		}
      return $pc;
   }

   function getDC() {
      $onthome = $this->props->getOntologyDir();
      $dcfactory = $this->props->getDCFactory();
      $dcdomain = $this->props->getDCDomain();
      $dchome = "$onthome/".$this->props->getDCPropertyForCurrentDomain("directory")."/".$this->props->getDCDomain();
      $dcontfile = "$dchome/ontology.owl";
      $dclibfile = "$dchome/library.owl";
      $dctopontfile = "$onthome/".$this->props->getDCPropertyForCurrentDomain("directory")."/ontology.owl";
      $dc = null;
      if($dcfactory == "internal") {
      	$serfile = $dclibfile.".ser";
			if(file_exists($serfile)) {
				$dc = unserialize(file_get_contents($serfile));
			} else {
         	$dc = new DataCatalog($dcontfile, $dclibfile, $dctopontfile, $this->dir.DATA_DIR, $this->props);
				$this->serialize($dc, $serfile);
			}
		}
      return $dc;
   }

	function getBeamer() {
		require_once("beamer.inc.php");
      $onthome = $this->props->getOntologyDir();
      $pcdomain = $this->props->getPCDomain();
      $pchome = "$onthome/".$this->props->getPCPropertyForCurrentDomain("directory")."/".$this->props->getPCDomain();
      $paraphrases_file = "$onthome/beamer/paraphrases.json";
      $mappings_file = "$onthome/beamer/mappings.json";
		$beamer = new Beamer($paraphrases_file, $mappings_file);
		return $beamer;
	}

   function getTemplate($template_id, $createNew, $sendNSMap) {
      $domain = $this->props->getTemplateDomain();
      $onthome = $this->props->getOntologyDir();
  		$onturl = $this->props->getOntologyURL();
      $template_file="$onthome/$domain/$template_id.owl";
  		$template_ns = "$onturl/$domain/$template_id.owl#";

		$template = null;
     	$serfile = $template_file.".ser";
		if(!$createNew && file_exists($serfile)) {
			$template = unserialize(file_get_contents($serfile));
		} else {
			$nsmap = null;
			if($sendNSMap) {
      		$nsmap = $this->props->getPCPrefixNSMap();
      		$nsmap = array_merge($nsmap, $this->props->getDCPrefixNSMap());
				$nsmap['defaultns'] = $template_ns;
				$nsmap['wflow'] = $onturl."/".$this->props->getWorkflowOntologyPath()."#";
			}
      	$template = new Template($template_file, $createNew, $nsmap, $template_ns);
			$this->serialize($template, $serfile);
		}
      return $template;
   }

   function getTemplateFromFile($template_file) {
     	$template = new Template($template_file, false, null);
		return $template;
	}

   function deleteTemplate($template_id) {
      $domain = $this->props->getTemplateDomain();
      $onthome = $this->props->getOntologyDir();
      $template_file="$onthome/$domain/$template_id.owl";
		unlink($template_file);
		unlink($template_file.".ser");
		@unlink($template_file.".png");
		@unlink($template_file.".jpeg");
	}

	function saveDC($dc) {
		$dc->saveLibrary();
		$dc->saveOntology();
		$this->serialize($dc, $dc->libfile.".ser");
	}

	function savePC($pc) {
		if($pc->libfile) {
			$pc->saveLibrary($pc->libfile);
			//$this->serialize($pc, $pc->libfile.".ser");
		}
		else if($pc->absfile) {
			$pc->saveLibrary($pc->absfile);
			//$this->serialize($pc, $pc->absfile.".ser");
		}
	}

	function saveTemplate($template, $imagedata) {
		$template->save();
		$this->serialize($template, $template->file.".ser");
		if($imagedata) 
			$this->saveTemplateImage($imagedata, $template->file);
	}

	function saveTemplateImage($imagedata, $tfile) {
		if(preg_match('/^data:image\/(png|jpeg);base64,(.*)$/', $imagedata, $m)) {
			$extension = $m[1];
			$imgbinary = base64_decode($m[2]);
			$imgpath = $tfile.".$extension";
			file_put_contents($imgpath, $imgbinary);
		}
	}
}

?>
