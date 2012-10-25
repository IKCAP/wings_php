<?php
/*
 * access_results.snippet.php
 * - workflow_portal_accessResults Snippet
 *
 * Arguments
 * - guid               [Required]  Unique ID
 * - page_limit         [Optional]  Number of runs to show per page (unimplemented)
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

$prop_file = USER_HOME_PATH."wings.properties";
$WINGS_PROPS = new PropertiesHelper($prop_file);
if(!$WINGS_PROPS)
return $modx->lexicon('workflow_portal.no_wings_properties');

// Load the Run Database
require_once(PORTAL_INCLUDES_PATH."rundb.inc.php");

// Load the User recursive operations
require_once(PORTAL_INCLUDES_PATH."recursive_operations.inc.php");


function jsonForModx($arr) {
	return preg_replace('/\[\[/', '[ [', json_encode($arr));
}

function showResults($guid, $runid, $docid) {
	loadTheme();
	if(!$runid) $runid = "null";
	loadExtJS();
	loadPortalJS();
	?>
<script>
var USER_DATA_URL = "<?=USER_HOME_URL.DATA_DIR?>";
Ext.onReady(function() {
	initializeResultBrowser("<?=$guid?>", <?=$runid?>, '[[~<?=$docid?>]]');
});
</script>
	<?
}


// Get Operation if passed in via the URL
if(isset($_REQUEST['op'])) $op = $_REQUEST['op'];
if(isset($_REQUEST['run_id'])) $runid = $_REQUEST['run_id'];
if(isset($_REQUEST['run_image'])) $runimage = $_REQUEST['run_image'];

// Connect to modX Run DB
connectRunDB($modx);

if($op == "getRunList") {
	$runs = getAllRunsForUser($modx, UID, USER_DOMAIN);
	return jsonForModx($runs);
}
else if ($op == "getRunDetails") {
	$run = getRunDetailsForUser($modx, UID, $runid);
	return jsonForModx($run);
}
else if ($op == "getRunRDF") {
	require_once(PORTAL_INCLUDES_PATH."factory.inc.php");
	$fac = new Factory($WINGS_PROPS);
	$libname = $fac->getPCLibname();
	$exformat = $WINGS_PROPS->getOutputFormat();
	$rdfurl = getRunDetailsForUserRDF($modx, UID, $uname, $runid, $runimage, $libname, $exformat);
	return $rdfurl;
}
else if ($op == "getRunHTML") {
	$htmlurl = getRunDetailsForUserHTML($modx, UID, $uname, $runid, $runimage);
	return $htmlurl;
}
else if ($op == "deleteRun") {
	$rundetails = json_decode($_REQUEST['rows']);
	$rundetails = $rundetails ? get_object_vars($rundetails) : null;
	$runid = $rundetails ? $rundetails["id"] : null;
	$ret = array("success"=>false, "records"=>array("id"=>$runid));
	if($runid) {
		$run = getRunDetailsForUser($modx, UID, $runid);
 		if($run['rows'] && $run['rows']['requestid']) {
			recursive_unlink(USER_HOME_PATH.DATA_DIR.$run['rows']['requestid']);
			unRegisterRun($modx, UID, $runid);
			deleteRunFiles($run, true);
			$ret["success"] = true;
		}
	}
	return jsonForModx($ret);
}
else if ($op == "registerData") {
	if(!$runid) return "Error: No Run Specified";
	$dsid = $_REQUEST['dsid'];
	if(!$dsid) return "Error: No Data Specified for Registration";

	require_once(PORTAL_INCLUDES_PATH."factory.inc.php");
	$fac = new Factory($WINGS_PROPS);
	$dc = $fac->getDC();

	$newid = $_REQUEST['newid'];
	$metricsJson = $_REQUEST['metrics'];
	$dc->registerData($dsid, $newid, $metricsJson);
	$fac->saveDC($dc);

	return "OK";
}
else if ($op == "intro") {
?>
<div class='x-toolbar x-toolbar-default' style='padding:10px;font-size:1.5em;color:darkgreen;border-width:0px 0px 1px 0px'>Access Results</div>
<div style='padding:5px'>
With this interface, you can:
<ul style='margin-left:25px; list-style-type:square'>
	<li>Quickly view all your Runs from the table on top, and see what their current status is (Succeeded, Failed, In Progress).</li>
	<li>View Run Details by clicking on a Run</li>
	<li>The Run Details include:
	<ul style='margin-left:25px; list-style-type:circle'>
		<li>Input, Intermediate, and Output Data/Parameters that have currently been generated for the run</li>
		<li>A commandline output from the Run (Run Log)</li>
		<li>The Workflow that was used to create the run, and the documentation associated with it</li>
	</ul>
	</li>
</ul>
</div>
<?
}
else if (!$op) {
	$doc = $modx->getObject('modDocument', array('alias' => "WPAccessResultOps"));
	$docid = $doc->get('id');
	showResults($guid, $runid, $docid);
}
?>
