<?php
define(MODX_MANAGER_PATH, '../manager');
require_once(MODX_MANAGER_PATH . '.inc.phpludes/config.inc.php.php');
require_once(MODX_MANAGER_PATH . '.inc.phpludes/protect.inc.php.php');
    
// Setup the MODx API
define('MODX_API_MODE', true);
// initiate a new document parser
include_once(MODX_MANAGER_PATH.'.inc.phpludes/document.parser.class.inc.php.php');
$modx = new DocumentParser;
     
//$modx->db->connect(); // provide the MODx DBAPI
//$modx->getSettings(); // provide the $modx->documentMap and user settings
?>
