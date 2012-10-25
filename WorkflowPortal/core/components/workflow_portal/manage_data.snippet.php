<?php
/*
 * manage_data.snippet.php
 * - workflow_portal_manageData Snippet
 *
 * Arguments
 * - guid               [Required]  Unique ID
 * - op                 [Optional]  Operation (getDataJSON, getDataTypeJSON, etc)
 * - data_type          [Optional]  Required for data type operations
 * - data_id            [Optional]  Required for data id operations
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


if(isset($_REQUEST['op'])) $op = $_REQUEST['op'];
if(isset($_REQUEST['data_type'])) $dtype = $_REQUEST['data_type'];
if(isset($_REQUEST['data_id'])) $did = $_REQUEST['data_id'];

// Load in the Data Viewer Class
require_once(PORTAL_INCLUDES_PATH."dataviewer.inc.php");

// Start the output buffer 
// - so we can just print to stdout and return buffer contents at the end
ob_start();

// Show  Template Selection Panel
// Load Template Form, Graph & other details if template is provided

$opdoc = $modx->getObject('modDocument', array('alias' => "WPDataOps"));

$dv = new DataViewer($guid, $WINGS_PROPS, $CONFIG, PORTAL_ASSETS_URL, SANDBOX, $modx);
$dv->setOpsDocId($opdoc->id);

if($op == "getDataJSON") {
	return $dv->getDataJSON($did);
}
else if ($op == "getDataTypeJSON") {
	return $dv->getDataTypeJSON($dtype);
}
else if ($op == "getDataHierarchyJSON") {
	return $dv->getDataHierarchyJSON();
}
else if ($op == "saveDataJSON") {
	$propvals_json = $_REQUEST['propvals_json'];
	return $dv->saveDataJSON($did, $propvals_json);
}
else if ($op == "saveDataTypeJSON") {
	$props_json = $_REQUEST['props_json'];
	$force = $_REQUEST['force'];
	if(!SANDBOX)
		return $dv->saveDataTypeJSON($dtype, $props_json, $force);
}
else if ($op == "newDataType") {
	$ptype = $_REQUEST['parent_type'];
	if(!SANDBOX)
		return $dv->newDataType($ptype, $dtype);
}
else if ($op == "delDataTypes") {
	$dtypes = json_decode($dtype);
	if(!SANDBOX)
		return $dv->delDataTypes($dtypes);
}
else if ($op == "moveDatatypeTo") {
	$fromtype = $_REQUEST['from_parent_type'];
	$totype = $_REQUEST['to_parent_type'];
	if(!SANDBOX)
		return $dv->moveDatatypeTo($dtype, $fromtype, $totype);
}
else if ($op == "addDataForType") {
	$dids = json_decode($did);
	return $dv->addDataForType($dids, $dtype);
}
else if ($op == "delData") {
	return $dv->delData($did);
}
else if ($op == "intro") {
?>
<div class='x-toolbar x-toolbar-default' style='padding:10px;font-size:1.5em;color:maroon;border-width:0px 0px 1px 0px'>Manage Data</div>
<div style='padding:5px'>
With this interface, you can:
<ul style='margin-left:25px; list-style-type:square'>
	<li>View Data Types (folders) and Data files by clicking on the links in the tree on the left</li>
	<li>After opening a Data Type you may:
	<ul style='margin-left:25px; list-style-type:circle'>
		<li>Add/Remove Metadata Properties for that data type</li>
		<li>Upload files for the data type</li>
		<li>You may also delete the data type and all files associated with it by clicking on the "Delete" button on top of the tree on the left after selecting a data type</li>
	</ul>
	</li>
	<li>After opening a Data File you may:
	<ul style='margin-left:25px; list-style-type:circle'>
		<li>Add Metadata property Values for the data file</li>
		<li>View the file</li>
		<li>Delete the file</li>
	</ul>
	</li>
</ul>
</div>
<?
}
else {
	$dv->show();
}

// Return output buffer contents 
$contents = ob_get_contents();
ob_end_clean();

return $contents;
?>
