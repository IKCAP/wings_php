<?php
require_once("properties.inc.php");

Class PropertiesHelper {
   var $conf;
   var $propfile;

   var $ontdir;
   var $dcnsmap;
   var $pcnsmap;

   function PropertiesHelper($properties_file) {
      $this->propfile = $properties_file;
      $this->loadWingsProperties();
   }

   function getProposedLogFileName($id) {
      return $this->getLogDir()."/".$id.".log";      
   }
    
   function loadWingsProperties() {
      if($this->conf) return $this->conf;
      $this->conf = new Properties();
      $fh = fopen($this->propfile,'r');
      $this->conf->load($fh);
      fclose($fh);
   }

   function storeWingsProperties($properties_file) {
      $fh = fopen($properties_file,'w');
      $this->conf->store($fh);
      fclose($fh);
   }

   function getPCFactory() {
      $this->loadWingsProperties();
      return $this->conf->getProperty("pc.factory");
   }
   function getDCFactory() {
      $this->loadWingsProperties();
      return $this->conf->getProperty("dc.factory");
   }
    
   function getPCDomain() {
      $this->loadWingsProperties();
      return $this->conf->getProperty("pc.domain");
   }
   function getDCDomain() {
      $this->loadWingsProperties();
      return $this->conf->getProperty("dc.domain");
   }
   function getTemplateDomain() {
      $this->loadWingsProperties();
      return $this->conf->getProperty("template.domain");
   }
    
   function getGraphvizPath() {
      $this->loadWingsProperties();
      return $this->conf->getProperty("graphviz.path");
   }
    
   function setLogDir($dir) {
      $this->loadWingsProperties();
      $this->conf->setProperty("logs.dir", $dir);
   }
   function getLogDir() {
      $this->loadWingsProperties();
      return $this->conf->getProperty("logs.dir");
   }
    
   function setOutputDir($dir) {
      $this->loadWingsProperties();
      $this->conf->setProperty("output.dir", $dir);
   }
   function getOutputDir() {
      $this->loadWingsProperties();
      return $this->conf->getProperty("output.dir");
   }
    
   function getOutputFormat() {
      $this->loadWingsProperties();
      $oformat = $this->conf->getProperty("output.format");
      if(!$oformat)
         $oformat = "xml";
      return $oformat;
   }

   function setOutputFormat($format) {
      $this->loadWingsProperties();
      $this->conf->setProperty("output.format", $format);
   }
    
   function getResourceDir() {
      $this->loadWingsProperties();
      return $this->conf->getProperty("resource.dir");
   }
    
   function getOntologyDir() {
      $this->loadWingsProperties();
      if(!$this->ontdir)
         $this->ontdir = $this->conf->getProperty("ontology.root.dir");
  
      return $this->ontdir;
   }
   function setOntologyDir($dir) {
      $this->loadWingsProperties();
      $this->ontdir = $dir;
      $this->conf->setProperty("ontology.root.dir", $dir);
   }

   function setProperty($key, $val) {
      $this->loadWingsProperties();
      $this->conf->setProperty($key, $val);
   }
    
   function getOntologyURL() {
      $this->loadWingsProperties();
      return $this->conf->getProperty("ontology.root.url");
   }
    
   function getWorkflowOntologyPath() {
      $this->loadWingsProperties();
      return $this->conf->getProperty("ontology.wflow.path");
   }

   function getProvenanceFlag() {
      $this->loadWingsProperties();
      return $this->conf->getProperty("storeprovenance");
   }
    
   function getTrimmingNumber() {
      $this->loadWingsProperties();
      return $this->conf->getProperty("trimming.numworkflows");
   }
    
   function getKeyValueMap($pref, $suf) {
      $map = array();
      $fac = $this->conf->getProperty($pref.".factory");
      $dom = $this->conf->getProperty($pref.".domain");
      $pattern = "/".$pref."\\.".$fac."\\.([^\\.]+)\\.".$suf."\\.(.+)/";
      $props = $this->conf->propertyNames();
      foreach($props as $key) {
         if(preg_match($pattern,$key,$m)) {
            $mdom = $m[1];
            $mkey = $m[2];
            $value = $this->conf->getProperty($key);
            if($mdom == $dom) $map[$mkey] = $value;
            else if($mdom == "*" || !isset($map[$mkey])) $map[$mkey] = $value;
         }
      }
      return $map;
   }
    
   function getDCPrefixNSMap() {
      $this->loadWingsProperties();
      if(!$this->dcnsmap)
         $this->dcnsmap = $this->getKeyValueMap("dc", "ns");
      return $this->dcnsmap;
   }
    
   function getPCPrefixNSMap() {
      $this->loadWingsProperties();
      if(!$this->pcnsmap)
         $this->pcnsmap = $this->getKeyValueMap("pc", "ns");
      return $this->pcnsmap;
   }
    
   function setPropertyForCurrentDomain($pref, $prop, $val) {
      $this->loadWingsProperties();
      $fac = $this->conf->getProperty($pref.".factory");
      $dom = $this->conf->getProperty($pref.".domain");
      $tmp = $this->conf->setProperty($pref.".".$fac.".".$dom.".".$prop, $val);
   }

   function setDCPropertyForCurrentDomain($prop, $val) {
      return $this->setPropertyForCurrentDomain("dc", $prop, $val);
   }
    
   function setPCPropertyForCurrentDomain($prop, $val) {
      return $this->setPropertyForCurrentDomain("pc", $prop, $val);
   }
    
   function getPropertyForCurrentDomain($pref, $prop) {
      $this->loadWingsProperties();
      $fac = $this->conf->getProperty($pref.".factory");
      $dom = $this->conf->getProperty($pref.".domain");
      $tmp = $this->conf->getProperty($pref.".".$fac.".".$dom.".".$prop);
      if ($tmp == null) $tmp = $this->conf->getProperty($pref.".".$fac.".*.".$prop);
      return $tmp;
   }
    
   function getDCPropertyForCurrentDomain($prop) {
      return $this->getPropertyForCurrentDomain("dc", $prop);
   }
    
   function getPCPropertyForCurrentDomain($prop) {
      return $this->getPropertyForCurrentDomain("pc", $prop);
   }
    
   function getPCNewComponentPrefix() {
      return $this->getPCPropertyForCurrentDomain("componentns");
   }

   function getDCNewDataPrefix() {
      return $this->getDCPropertyForCurrentDomain("datans");
   }
    
   function getQueryNamespace(){
      $this->loadWingsProperties();
      return $this->conf->getProperty("query.ns.wfq");
   }
    
   function getQueryVariablesNamespace(){
      $this->loadWingsProperties();
      return $this->conf->getProperty("query.ns.wfqv");
   }
}
?>
