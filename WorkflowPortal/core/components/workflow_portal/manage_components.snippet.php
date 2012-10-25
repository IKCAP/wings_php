<?php
/*
 * manage_components.snippet.php
 * - workflow_portal_manageComponents Snippet
 *
 * Arguments
 * - guid               [Required]  Unique ID
 * - load_concrete      [Optional]  Load Concrete Components
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
if(isset($_REQUEST['cid'])) $cid = $_REQUEST['cid'];

// Load in the Component Viewer Class
require_once(PORTAL_INCLUDES_PATH."componentviewer.inc.php");

$opdoc = $modx->getObject('modDocument', array('alias' => "WPComponentOps"));

if(isset($_REQUEST['load_concrete'])) $load_concrete = $_REQUEST['load_concrete'];

$cv = new ComponentViewer($guid, $WINGS_PROPS, $CONFIG, PORTAL_ASSETS_URL, SANDBOX, $modx, $load_concrete);
$cv->setOpsDocId($opdoc->id);

if($op == "getComponentJSON") {
	return $cv->getComponentJSON($cid);
}
else if ($op == "saveComponentJSON") {
	$component_json = $_REQUEST['component_json'];
	if(!SANDBOX)
		return $cv->saveComponentJSON($cid, $component_json);
}
else if ($op == "newComponent") {
	$pid = $_REQUEST['parent_cid'];
	$ptype = $_REQUEST['parent_type'];
	if(!SANDBOX)
		return $cv->newComponent($cid, $pid, $ptype);
}
else if ($op == "newCategory") {
	$ptype = $_REQUEST['parent_type'];
	if(!SANDBOX)
		return $cv->newCategory($cid, $ptype);
}
else if ($op == "setComponentIsConcrete") {
	if(!SANDBOX)
		return $cv->setComponentIsConcrete($cid, true);
}
else if ($op == "setComponentIsAbstract") {
	if(!SANDBOX)
		return $cv->setComponentIsConcrete($cid, false);
}
else if ($op == "moveComponentTo") {
	$frompid = $_REQUEST['from_parent_cid'];
	$topid = $_REQUEST['to_parent_cid'];
	if(!SANDBOX)
		return $cv->moveComponentTo($cid, $frompid, $topid);
}
else if ($op == "delComponent") {
	if(!SANDBOX)
		return $cv->delComponent($cid);
}
else if ($op == "delCategory") {
	if(!SANDBOX)
		return $cv->delCategory($cid);
}
else if ($op == "downloadComponent") {
	if(!SANDBOX)
		return $cv->downloadComponent($cid);
}
else if ($op == "downloadSkeleton") {
	if(!SANDBOX)
		return $cv->downloadComponent("skeleton", $cid);
}
else if ($op == "getAllRules") {
	return $cv->getAllRules();
}
else if ($op == "saveAllRules") {
	$rules = $_REQUEST['rules'];
	if(!SANDBOX)
		return $cv->saveAllRules($rules);
}
else if ($op == "intro") {
	$color = $load_concrete ? "darkblue" : "darkorange";
	$text = $load_concrete ? "Component" : "Component Type";
?>
<div class='x-toolbar x-toolbar-default' style='padding:10px;font-size:1.5em;color:<?=$color?>;border-width:0px 0px 1px 0px'>Manage <?=$text?>s</div>
<div style='padding:5px'>
With this interface, you can:
<ul style='margin-left:25px; list-style-type:square'>
	<li>View/Edit <?=$text?>s by clicking on the links in the tree on the left</li>
	<li>After opening a <?=$text?> you may edit its input and output descriptions:
	<ul style='margin-left:25px; list-style-type:circle'>
		<li>Specifiy a "Role" name for the Input/Output</li>
		<li>If the input isn't a file, it goes under Input Parameters</li>
		<li>Specify a "Prefix" for the Input/Output argument in the argument string (example: -i1 [inputfile1] -p1 [param1]</li>
		<li>Specify the dimensionality of the Input/Output (equal to 1 for a 1-D collection, 0 for a single file)</li>
	</ul>
	</li>
	<li>You may also edit Rules by Editing the "Rules" Tab</li>
</ul>
</div>
<?
}
else {
	$cv->show();
}
?>
