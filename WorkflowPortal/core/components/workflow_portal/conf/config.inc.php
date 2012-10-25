<?php

// Set Component name
if(!defined('COMPONENT_NAME'))
	define('COMPONENT_NAME', 'workflow_portal');


// Set Core paths
if(!defined('MODX_BASE_PATH'))
	define('MODX_BASE_PATH', dirname( dirname( dirname( dirname( dirname( dirname(__FILE__) ) ) ) ) ).'/');

if(!defined('MODX_CORE_PATH')) {
	require_once(MODX_BASE_PATH . 'config.core.php');
	require_once(MODX_CORE_PATH . 'config/' . MODX_CONFIG_KEY . '.inc.php');
}


// Loading modx if desired
if(defined('CONFIG_LOAD_MODX')) { 
	require_once MODX_CORE_PATH . 'model/modx/modx.class.php';
	$modx= new modX();
	$modx->initialize('web');
}
else if(defined('CONFIG_LOAD_MODX_CONNECTOR')) { 
	require_once MODX_CONNECTORS_PATH . 'index.php';
}


// Set Portal paths
if(!defined('PORTAL_ASSETS_PATH'))
	define('PORTAL_ASSETS_PATH', MODX_ASSETS_PATH.'components/'.COMPONENT_NAME.'/');

if(!defined('PORTAL_ASSETS_URL'))
	define('PORTAL_ASSETS_URL', MODX_ASSETS_URL.'components/'.COMPONENT_NAME.'/');

if(!defined('PORTAL_INCLUDES_PATH'))
	define('PORTAL_INCLUDES_PATH', MODX_CORE_PATH.'components/'.COMPONENT_NAME.'/includes/');

if(!defined('PORTAL_MODEL_PATH'))
	define('PORTAL_MODEL_PATH', MODX_CORE_PATH.'components/'.COMPONENT_NAME.'/model/');

if(!defined('PORTAL_CONF_PATH'))
	define('PORTAL_CONF_PATH', MODX_CORE_PATH.'components/'.COMPONENT_NAME.'/conf/');


// Load properties from config file
if(!defined('DONT_LOAD_PROPERTIES') && !isset($CONFIG)) {
	global $CONFIG;
	$CONFIG = parse_ini_file('workflow_portal.ini', true);
}

// Set the Sandbox flag 
if(!defined('SANDBOX') && $CONFIG && isset($CONFIG['restrictions']['sandbox']))
	define('SANDBOX', ($CONFIG['restrictions']['sandbox']=="true" ? true : false));

// Set the Portal theme
if(!defined('THEME') && $CONFIG && isset($CONFIG['theme']['current_theme']))
	define('THEME', $CONFIG['theme']['current_theme']);

// Load Theme function based on $CONFIG['theme']['current_theme']
function loadTheme() {
	$theme = THEME;
	if($theme == "blue") {
		print '<link rel="stylesheet" href="'.PORTAL_ASSETS_URL.'css/xtheme-blue.css" />';
		print '<link rel="stylesheet" href="'.PORTAL_ASSETS_URL.'lib/extjs/resources/css/ext-all.css" />';
	}
	else if($theme == "gray") {
		print '<link rel="stylesheet" href="'.PORTAL_ASSETS_URL.'lib/extjs/resources/css/ext-all-gray.css" />';
	}
	else if($theme == "neptune") {
		print '<link rel="stylesheet" href="'.PORTAL_ASSETS_URL.'lib/extjs/resources/css/ext-neptune.css" />';
	}
	else if($theme == "minimal") {
		print '<link rel="stylesheet" href="'.PORTAL_ASSETS_URL.'resources/css/minimal-gray.css" />';
	}
	else {
		print '<link rel="stylesheet" href="'.PORTAL_ASSETS_URL.'lib/extjs/resources/css/ext-all-gray.css" />';
	}
}

// Load ExtJS javascript code
function loadExtJS() {
	print "<script src='".PORTAL_ASSETS_URL."lib/extjs/ext-all.js'></script>\n";
}

// Load Portal Javascript code
function loadPortalJS() {
	$dbg=false;
	print "<script src='".PORTAL_ASSETS_URL."/min/index.php?g=portaljs&debug=$dbg'></script>\n";
}

// Load TellMe Javascript code
function loadTellMeJS() {
	$dbg=false;
	print "<script src='".PORTAL_ASSETS_URL."/min/index.php?g=tellmejs&debug=$dbg'></script>\n";
}

// Load SWF Upload Javascript code
function loadSWFUploadJS() {
	print "<script src='".PORTAL_ASSETS_URL."/swfupload/js/swfupload.js'></script>\n";
	print "<script src='".PORTAL_ASSETS_URL."/swfupload/js/plugins/swfupload.queue.js'></script>\n";
	print "<script src='".PORTAL_ASSETS_URL."/js/swfbtn/swfbtn.js'></script>\n";
}

// Load Canvas Text writing Javascript code
function loadCanvasTextJS() {
	// Get Canvas and Canvas Text working on IE, Opera, etc
	print "<!--[if IE]><script type='text/javascript' src='".PORTAL_ASSETS_URL."/js/legacy/excanvas.js'></script><![endif]-->\n";
	print "<script type='text/javascript' src='".PORTAL_ASSETS_URL."/js/legacy/canvas.text.js'></script>\n";
	print "<script type='text/javascript' src='".PORTAL_ASSETS_URL."/js/legacy/faces/optimer-bold-normal.js'></script>\n";
}


define('ONTOLOGY_DIR', 'ontology/');
define('DATA_DIR', 'data/');
define('CODE_DIR', 'code/');
define('LOGS_DIR', 'logs/');
define('RUNS_DIR', 'runs/');
define('OUTPUT_DIR', 'output/');
define('PEGASUS_DIR', 'pegasus-config/');


// Set Current Domain Paths
if(!defined('DOMAINS_TOP_DIR') && defined('PORTAL_ASSETS_PATH'))
	define('DOMAINS_TOP_DIR', PORTAL_ASSETS_PATH.'domains/');

if(!defined('DOMAIN') && $CONFIG && isset($CONFIG['domain']['current_domain']))
	define('DOMAIN', $CONFIG['domain']['current_domain']);


// Set User Paths
if(!defined('USERS_TOP_DIR') && defined('PORTAL_ASSETS_PATH'))
	define('USERS_TOP_DIR', PORTAL_ASSETS_PATH.'users/');

if(!defined('USERS_TOP_URL') && defined('PORTAL_ASSETS_URL'))
	define('USERS_TOP_URL', PORTAL_ASSETS_URL.'users/');

if(!defined('USER_DOMAINS_TOP_DIR') && defined('UID') && defined('USERS_TOP_DIR')) 
	define('USER_DOMAINS_TOP_DIR', USERS_TOP_DIR.UID.'/');

if(!defined('USER_DOMAINS_TOP_URL') && defined('UID') && defined('PORTAL_ASSETS_URL')) 
	define('USER_DOMAINS_TOP_URL', USERS_TOP_URL.UID.'/');


if(!defined('USER_DOMAIN_FILE') && defined('USER_DOMAINS_TOP_DIR')) 
	define('USER_DOMAIN_FILE', USER_DOMAINS_TOP_DIR.'.default');

if(!defined('USER_DOMAIN') && defined('USER_DOMAIN_FILE') && is_file(USER_DOMAIN_FILE))
	define('USER_DOMAIN', trim(implode(file(USER_DOMAIN_FILE),'')));

if(!defined('USER_DOMAIN') && defined('DOMAIN'))
	define('USER_DOMAIN', DOMAIN);


if(!defined('USER_HOME_PATH') && defined('USER_DOMAIN') && defined('USER_DOMAINS_TOP_DIR')) 
	define('USER_HOME_PATH', USER_DOMAINS_TOP_DIR.USER_DOMAIN.'/');
	
if(!defined('USER_HOME_URL') && defined('USER_DOMAIN') && defined('USER_DOMAINS_TOP_URL'))
	define('USER_HOME_URL', USER_DOMAINS_TOP_URL.USER_DOMAIN.'/');


// Set Cache Paths
if(!defined('CACHE_PATH'))
	define('CACHE_PATH', PORTAL_ASSETS_PATH.'cache/');

if(!defined('CACHE_URL'))
	define('CACHE_URL', PORTAL_ASSETS_URL.'cache/');

// Set the Wings File Server 
if(!defined('OPMW_FILE_SERVER') && $CONFIG && isset($CONFIG['system']['opmw_file_server']))
	define('OPMW_FILE_SERVER', $CONFIG['system']['opmw_file_server']);

// Set the OPMW Server 
if(!defined('OPMW_EXPORT_SERVER') && $CONFIG && isset($CONFIG['system']['opmw_export_server']))
	define('OPMW_EXPORT_SERVER', $CONFIG['system']['opmw_export_server']);

