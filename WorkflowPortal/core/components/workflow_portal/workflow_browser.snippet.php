<?php
/*
 * workflow_browser.snippet.php
 * - workflow_portal_workflowBrowser Snippet
 * 
 * Arguments
 * - guid               [Required]  Unique ID
 * - template_id        [Optional]  The template to show
 * - editor_mode        [Optional]  In Template Editor mode
 * - hide_selector      [Optional]  Don't show the workflow selection drop-down
 * - hide_form          [Optional]  Don't show the workflow run form
 * - hide_graph         [Optional]  Don't show the workflow run graph
 * - hide_constraints   [Optional]  Hide the template constraints
 * - hide_documentation [Optional]  Hide the template documentation
 * - hide_resize        [Optional]  Hide the resizer
 * - height             [Optional]  Height of the panel
 * - hide_title         [Optional]  Hide the title bar
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

// Get Template id if passed in via the URL
if(isset($_REQUEST['template_id'])) 
	$template_id = $_REQUEST['template_id'];

// Get Operation if passed in via the URL
if(isset($_REQUEST['op'])) 
	$op = $_REQUEST['op'];

if($op == "intro") {
?>
<h1>Hi <?=$uname?>, this is the Template Browser</h1>
With this interface, you can:
<ul>
<li>View Workflow Templates by double-clicking a template name from the tree on the left side</li>
<li>Set inputs to those templates by entering values in the Input form that appears after opening a template. Then you may run the workflow</li> 
<li>Edit Templates by editing the Template Graph</li>
<li>Edit Template Constraints by adding/removing values from the Template Grid
</ul>
<?
}

// Load in the Template Viewer Class
require_once(PORTAL_INCLUDES_PATH."templateviewer.inc.php");

// Start the output buffer 
// - so we can just print to stdout and return buffer contents at the end
ob_start();


// Get Parameters if passed in via the URL instead of via the snippet
if(isset($_REQUEST['editor_mode'])) $editor_mode = $_REQUEST['editor_mode'];
if(isset($_REQUEST['hide_selector'])) $hide_selector = $_REQUEST['hide_selector'];
if(isset($_REQUEST['hide_form'])) $hide_form = $_REQUEST['hide_form'];
if(isset($_REQUEST['hide_graph'])) $hide_graph = $_REQUEST['hide_graph'];
if(isset($_REQUEST['hide_documentation'])) $hide_documentation = $_REQUEST['hide_documentation'];
if(isset($_REQUEST['hide_constraints'])) $hide_constraints = $_REQUEST['hide_constraints'];

// Show  Template Selection Panel
// Load Template Form, Graph & other details if template is provided

$rdoc = $modx->getObject('modDocument', array('alias' => "WPRunWorkflow"));
$opdoc = $modx->getObject('modDocument', array('alias' => "WPTemplateOps"));
$resdoc = $modx->getObject('modDocument', array('alias' => "WPAccessResults"));

if($editor_mode == 'tellme' && !$template_id) 
	$template_id = 'TellMeTemplate';//.(time()%10000);
	
$tv = new TemplateViewer($template_id, $guid, $WINGS_PROPS, $CONFIG, PORTAL_ASSETS_URL, $editor_mode, $modx, $graph_only);
$tv->setRunDocId($rdoc->id);
$tv->setOpsDocId($opdoc->id);
$tv->setResultsDocId($resdoc->id);
	
$tv->show(array(
	"editor_mode"=>$editor_mode,
	"hide_selector"=>$hide_selector,
	"hide_form"=>$hide_form,
	"hide_graph"=>$hide_graph, 
	"hide_documentation"=>$hide_documentation,
	"hide_constraints"=>$hide_constraints,
	"hide_title"=>$hide_title,
	"height"=>$height,
	"hide_resize"=>$hide_resize,
));

// Return output buffer contents 
$contents = ob_get_contents();
ob_end_clean();

return $contents;
?>
