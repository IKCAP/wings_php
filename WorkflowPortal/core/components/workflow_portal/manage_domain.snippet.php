<?php
/*
 * manage_domain.snippet.php
 * - workflow_portal_manageDomain Snippet
 *
 * Arguments
 * - guid               [Required]  Unique ID
 *
 * @author: Varun Ratnakar
 */

// Initialize the Lexicon
$modx->getService('lexicon','modLexicon');
$modx->lexicon->load('workflow_portal:default');

// Check for Snippet id
if(!$guid)
return $modx->lexicon('workflow_portal.snippet_id_missing');
$guid = preg_replace("/[^\w\d]+/","_",$guid);

// Get User Information
$uid = $modx->getLoginUserID();
$uname = $modx->getLoginUserName();
if(!$uid)
return $modx->lexicon('workflow_portal.please_login');

// Load in Component Configuration
define("UID", $uid);
define('COMPONENT_NAME', 'workflow_portal');
require_once(MODX_CORE_PATH."components/".COMPONENT_NAME."/conf/config.inc.php");

// Check for Domain
if(!defined('USER_DOMAIN'))
return $modx->lexicon('workflow_portal.no_domain_selected');

// Create User Space if this is a new user
require_once(PORTAL_INCLUDES_PATH."newuser.inc.php");
if(!createNewUserSpace())
return $modx->lexicon('workflow_portal.cannot_create_user_space');

// Load the User wings.properties file
require_once(PORTAL_INCLUDES_PATH."prophelper.inc.php");

require_once(PORTAL_INCLUDES_PATH."factory.inc.php");

$prop_file = USER_HOME_PATH."wings.properties";
$WINGS_PROPS = new PropertiesHelper($prop_file);
if(!$WINGS_PROPS)
return $modx->lexicon('workflow_portal.no_wings_properties');

$op = "";
if(isset($_REQUEST['op'])) $op = $_REQUEST['op'];

// Load in the Component Viewer Class
require_once(PORTAL_INCLUDES_PATH."domainviewer.inc.php");

$opdoc = $modx->getObject('modDocument', array('alias' => "WPDomainOps"));

$domv = new DomainViewer($guid, $WINGS_PROPS, $CONFIG, PORTAL_ASSETS_URL, $modx);
$domv->setOpsDocId($opdoc->id);

$domain = isset($_REQUEST['domain']) ? $_REQUEST['domain'] : null;
$libname = isset($_REQUEST['libname']) ? $_REQUEST['libname'] : null;

if($op == "importDomain" && $domain)
	return $domv->importDomain($domain);

else if($op == "reimportDomain" && $domain)
	return $domv->importDomain($domain, true);

else if($op == "newDomain" && $domain)
	return $domv->newDomain($domain);

else if($op == "newComponentLibrary" && $domain && $libname)
	return $domv->newComponentLibrary($domain, $libname);

else if($op == "setDefaultDomain" && $domain)
	return $domv->setDefaultDomain($domain);

else if($op == "setDefaultComponentLibrary" && $domain && $libname)
	return $domv->setDefaultComponentLibrary($domain, $libname);

else if($op == "getDefaultDomain")
	return USER_DOMAIN;

else if($op == "setDomainOutputFormat" && $domain && $_REQUEST['format'])
	return $domv->setDomainOutputFormat($domain, $_REQUEST['format']);

else if($op == "deleteDomain" && $domain)
	return $domv->deleteDomain($domain);

else if($op == "deleteComponentLibrary" && $domain && $libname)
	return $domv->deleteComponentLibrary($domain, $libname);

else $domv->show();
?>
