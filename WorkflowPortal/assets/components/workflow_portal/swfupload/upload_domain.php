<?php

if (isset($_POST["PHPSESSID"])) 
   define("UID", base64_decode($_POST["PHPSESSID"]));

if(!defined("UID")) 
   exit(1);

define("MODX_BASE_PATH", dirname(dirname(dirname(dirname(dirname(__FILE__)))))."/");
require_once MODX_BASE_PATH . '/config.core.php';  
require_once MODX_CORE_PATH . 'config/' . MODX_CONFIG_KEY . '.inc.php';  

// Load in Component Configuration
define('COMPONENT_NAME', 'workflow_portal');
require_once(MODX_CORE_PATH."components/".COMPONENT_NAME."/conf/config.inc.php");

foreach($_FILES as $varname=>$file) {
	$dest = PORTAL_ASSETS_PATH."domains/".$file['name'];
    if(!move_uploaded_file($file['tmp_name'], $dest)) {
    	echo "Error: ".print_r($file)."<br/>$dest<br/>";
    }
    else {
    	echo $dest;
    }
}

?>
