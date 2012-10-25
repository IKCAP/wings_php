<?php
/*
 * template_operations.snippet.php
 * - workflow_portal_templateOperations Snippet
 * 
 * Arguments
 * - op                 [Optional]  The operation to perform ('editor_intro', 'browser_intro', 'getJSON', 'saveFromJSON')
 * - template_id        [Optional]  The template to perform operations on
 * 
 * @author: Varun Ratnakar
 */

//include_once(dirname(__FILE__).'/debug.php');
$modx->getService('lexicon','modLexicon');
$modx->lexicon->load('workflow_portal:default');

// Get User Information
$uid = $modx->getLoginUserID();
$uname = $modx->getLoginUserName();
if(!$uid)
return $modx->lexicon('workflow_portal.please_login');

// Set the Template id
$template_id = $_REQUEST['template_id'];

// Load in Component Configuration
define("UID", $uid);
define("UNAME", $uname);
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

$prop_file = USER_HOME_PATH."wings.properties";
$WINGS_PROPS = new PropertiesHelper($prop_file);
if(!$WINGS_PROPS)
return $modx->lexicon('workflow_portal.no_wings_properties');

// Load the Ontology Factory
require_once(PORTAL_INCLUDES_PATH."factory.inc.php");

// Load the Wings Java API
require_once(PORTAL_INCLUDES_PATH."wingsapi.inc.php");

function elaborateTemplateByWings($templateid, $pclib) {
	$bindings = array();
	$explanations = array();
	$error = false;
	$exception = "";

	try  { $tmp = __javaproxy_Client_getClient(); }
	catch (Exception $ex) {
		return array("error"=>true, "explanations"=>array(), 
		"bindings"=>array(), "output"=>$ex->getMessage());
	}

	$requestid = uniqid($templateid."_Run_");
	$itJSON = "{}";
	try {
		$awg = getWorkflowGenerator($templateid, $requestid, true, $pclib);
		$it = getInferredTemplate($awg);
		if($it) {
			$itJSON = getWingsTemplateJSON($it);
		}
		else {
			$error = true;
		}
	} catch (JavaException $ex) {
		$error = true;
		$exception .= "Exception: ".$ex->toString()."\n";
		foreach($ex->getStackTrace() as $el) {
			$exception .= " at ".$el->getClassName().".".$el->getMethodName();
			$exception .= "(".$el->getFileName().":".$el->getLineNumber().")\n";
		}
	}
	return array("error"=>$error, 
		"template"=>$itJSON, 
		"explanations"=>getExplanations($awg),
		"output"=>$exception);
}

// Get Operation if passed in via the URL
if(isset($_REQUEST['op'])) $op = $_REQUEST['op'];

if($op == "browser_intro") { ?>
<div class='x-toolbar x-toolbar-default' style='padding:10px;font-size:1.5em;border-width:0px 0px 1px 0px;'>Template Browser</div>
<div style='padding:5px'>
With this interface, you can:
<ul style='margin-left:25px; list-style-type:square'>
	<li>View Workflow Templates by clicking a template name from the tree on the left side</li>
	<li>After opening a Template you may click 'Suggest Data', and subsequently 'Suggest Parameters' to ask the system for help in setting inputs to the template in the Inputs form. Alternatively, you may select the data and set the paramters yourself</li> 
</ul>
</div>
<? }
else if($op == "editor_intro") { ?>
<div class='x-toolbar x-toolbar-default' style='padding:10px;font-size:1.5em;color:maroon;border-width:0px 0px 1px 0px'>Template Editor</div>
<div style='padding:5px'>
With this interface, you can:
<ul style='margin-left:25px; list-style-type:square'>
	<li>View Workflow Templates by clicking a template name from the tree on the left side</li>
	<li>After opening a Template you may:
	<ul style='margin-left:25px; list-style-type:circle'>
		<li>Edit Templates by editing the Template Graph, or by Dragging components from the Component tree on the left</li>
		<li>Edit Template Constraints by adding/removing values from the Constraints Table</li>
	</ul>
	</li>
</ul>
</div>
<? }
else if ($op == "getViewerJSON") {
	// Include the Template Viewer
	require_once(PORTAL_INCLUDES_PATH."templateviewer.inc.php");
	$tv = new TemplateViewer($template_id, 1, $WINGS_PROPS, $CONFIG, PORTAL_ASSETS_URL, false, $modx);
	return '{inputs:'.$tv->getInputsJSON().
		',template:'.$tv->getTemplateJSON().
		'}';
}
else if ($op == "getEditorJSON") {
	// Include the Template Viewer
	require_once(PORTAL_INCLUDES_PATH."templateviewer.inc.php");
	$tv = new TemplateViewer($template_id, 1, $WINGS_PROPS, $CONFIG, PORTAL_ASSETS_URL, true, $modx);
	return '{template:'.$tv->getTemplateJSON().
		',propvals:'.$tv->getConstraintPropertyValuesJSON().
		'}';
}
else if ($op == "saveTemplateJSON") {
	$tid = $_REQUEST['template_id'];
	$tjson = json_decode($_REQUEST['json']);
	$xtjson = get_object_vars($tjson);
	$fac = new Factory($WINGS_PROPS);
	$template = $fac->getTemplate($tid, true, !($xtjson['nsmap']));
	$ret = $template->loadTemplateFromJSON($tid, $tjson, $WINGS_PROPS);
	if($ret != "OK") return $ret;

	$imagedata = $_REQUEST['imagedata'];
	$fac->saveTemplate($template, $imagedata);
	//return print_r($template,true);
	return "OK";
}
else if ($op == "newTemplate") {
	$tid = $_REQUEST['template_id'];
	$fac = new Factory($WINGS_PROPS);
	$template = $fac->getTemplate($tid, true, true);
	$template->loadTemplateFromJSON($tid, array(), $WINGS_PROPS);
	$fac->saveTemplate($template);
	return "OK";
}
else if ($op == "deleteTemplate") {
	$tid = $_REQUEST['template_id'];
	$fac = new Factory($WINGS_PROPS);
	$fac->deleteTemplate($tid);
	return "OK";
}
else if ($op == "elaborateTemplateJSON") {
	$tid = $_REQUEST['template_id'];

	// Save temporary template
	$tmpid = uniqid($tid."_");
	$tjson = json_decode($_REQUEST['json']);
	$xtjson = get_object_vars($tjson);
	$fac = new Factory($WINGS_PROPS);
	$pclib = $fac->getPCLibname();

	$template = $fac->getTemplate($tmpid, true, !($xtjson['nsmap']));

	$template->loadTemplateFromJSON($tmpid, $tjson, $WINGS_PROPS);
	$fac->saveTemplate($template);

	// Run elaboration on the temporary template and then delete it
	$ret = elaborateTemplateByWings($tmpid, $pclib);
	$fac->deleteTemplate($tmpid);

	if($ret['error']) return json_encode($ret);
	$templateJSON = $ret['template'];

	return '{error:0'.
		',template:'.$templateJSON.
		',explanations:'.json_encode($ret['explanations']).
		',output:'.json_encode($ret['output']).
		'}';
}
else if ($op == "dotLayout") {
	require_once(PORTAL_INCLUDES_PATH."dot.inc.php");

	$dotstr = $_REQUEST["dotstr"];
	if($dotstr) {
		return getDotLayout($dotstr, $CONFIG['system']['dot']);
	}
	return "";
}
else if ($op == "runTopAlgorithm") {
	require_once(PORTAL_INCLUDES_PATH."topapi.inc.php");

	$tid = $_REQUEST['template_id'];

	// Save temporary template
	$tmpid = uniqid($tid."_");
	$tjson = json_decode($_REQUEST['json']);
	$xtjson = get_object_vars($tjson);
	$fac = new Factory($WINGS_PROPS);
	$template = $fac->getTemplate($tmpid, true, !($xtjson['nsmap']));

	$tmp = $template->loadTemplateFromJSON($tmpid, $tjson, $WINGS_PROPS);
	$fac->saveTemplate($template);

	// Run TOP on the temporary template and then delete it
	$data = analyzeTOP($tmpid);

	$fac->deleteTemplate($tmpid);

	return $data;
}
?>
