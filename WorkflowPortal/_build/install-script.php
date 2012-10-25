<?php
/**
 * Workflow Portal Extra Items Installer Script
 *
 * @package workflow_portal
 * @version 0.1
 * @author Varun Ratnakar <varunr@isi.edu>
 */

/* The $modx object is not available here so we have to use
 * $object->xpdo in it's place */

global $xpdo;
$xpdo = $object->xpdo;

function createDocument($title, $alias, $menu_index, $parent, $id, $template) {
	global $xpdo;
	global $sources;
	
	$r = $xpdo->getObject('modDocument', array('alias' => $alias));
	if(!$r) $r = $xpdo->getObject('modDocument', array('id' => $id));
	if(!$r) {
		$xpdo->log(xPDO::LOG_LEVEL_INFO,"Creating resource: $title<br />");
		$r = $xpdo->newObject('modResource');
		if($id) $r->set('id', $id);
	}
	
	$r->set('class_key','modDocument');
	$r->set('context_key','web');
	$r->set('type','document');
	$r->set('contentType','text/html');
	$r->set('pagetitle',$title);
	$r->set('longtitle',$title);
	$r->set('description','');
	$r->set('alias',$alias);
	$r->set('published','1');
	$r->set('parent', $parent);
	$r->set('isfolder','0');
	$r->set('richtext','0');
	$r->set('menuindex',$menu_index);
	$r->set('searchable','1');
	$r->set('cacheable','0');
	$r->set('menutitle',$title);
	$r->set('donthit','0');
	$r->set('hidemenu','0');
	$r->set('template',$template);
	return $r;
}

function deleteDocument($alias) {
	global $xpdo;
	$xpdo->log(xPDO::LOG_LEVEL_INFO,"<br>Deleting resource: $alias<br />");
	$resource = $xpdo->getObject('modDocument', array('alias' => $alias));
	if($resource) $resource->remove();
}

function deleteSnippet($name) {
	global $xpdo;
	$xpdo->log(xPDO::LOG_LEVEL_INFO,"<br>Deleting snippet: $name<br />");
	$resource = $xpdo->getObject('modSnippet', array('name' => $name));
	if($resource) $resource->remove();
}

function deleteTemplate($name) {
	global $xpdo;
	$xpdo->log(xPDO::LOG_LEVEL_INFO,"<br>Deleting template: $name<br />");
	$resource = $xpdo->getObject('modTemplate', array('templatename' => $name));
	if($resource) $resource->remove();
}

$success = false;

switch($options[xPDOTransport::PACKAGE_ACTION]) {
	
	case xPDOTransport::ACTION_UPGRADE:
	case xPDOTransport::ACTION_INSTALL:
		
		//$default = $xpdo->config['default_template'];
		$default = $xpdo->getObject('modTemplate', array('templatename' => 'Andreas'))->get('id');
		$plain = $xpdo->getObject('modTemplate', array('templatename' => 'Plain'))->get('id');
		
		$rhome = createDocument('Home', 'WPHome', '1', '0', '1', $default);
		if(!$rhome->getContent())
			$rhome->setContent("<h2>Workflow Portal Home Page</h2><p>Description about your Portal</p><p>You can edit this page by logging into your modx manager screen</p>");
		$rhome->save();

		$rportal = createDocument('Analysis', 'WPAnalysis', '2', '0', '', $default);
		if(!$rportal->getContent())
			$rportal->setContent("<h2>Workflow Portal</h2>");
		$rportal->save();

		$rportaladv = createDocument('Advanced', 'WPAdvanced', '3', '0', '', $default);
		$rportaladv->save();
		
		$rnews = createDocument("What's New !", "WPWhatsNew", '4', '0', '', $default);
		if(!$rnews->getContent())
			$rnews->setContent("<ul><li>Workflow Portal Installed !</li></ul>");
		$rnews->save();
		
		$rnews = createDocument("Credits", "WPCredits", '5', '0', '', $default);
		if(!$rnews->getContent())
			$rnews->setContent("<ul><li>Yolanda Gil</li><li>Varun Ratnakar</li></ul>");
		$rnews->save();
				
		$pid = $rportal->get('id');
		
		$r1 = createDocument('Description of  Analysis', 'WPAnalysisDescription', '1', $pid, '', $default);
		if(!$r1->getContent())
			$r1->setContent('
<h1>Analysis Types</h1>
<p>General description of types of workflows in the portal.</p>
<ul>
<li>
<h2>Workflow Category</h2>
<p>
Category Description... You can choose from the following workflows:
</p>
<ol>
<li>
<h3><a href="#">Workflow 1</a></h3>
<p>
Workflow 1 Description
</p>
</li>
<li>
<h3><a href="#">Workflow 2</a></h3>
<p>
Workflow 2 Description
</p>
</li>
</ol>
</li>
</ul>');
		$r1->save();
		
		$rx = createDocument('Edit Workflows', 'WPEditWorkflows', '2', $pid, '', $default);
		if(!$rx->getContent())
			$rx->setContent("[[!workflow_portal_workflowBrowser? &guid=`1` &editor_mode=`1`]]");
		$rx->save();
		
		$r2 = createDocument('Run Workflows', 'WPBrowseWorkflows', '3', $pid, '', $default);
		if(!$r2->getContent())
			$r2->setContent("[[!workflow_portal_workflowBrowser? &guid=`1`]]");
		$r2->save();
		
		$r3 = createDocument('Access Results', 'WPAccessResults', '4', $pid, '', $plain);
		if(!$r3->getContent())
			$r3->setContent("[[!workflow_portal_accessResults? &guid=`1`]]");
		$r3->set('link_attributes', 'target="_accessResults"');
		$r3->save();
		
		$r5 = createDocument('Workflow Gallery', 'WPWorkflowGallery', '5', $pid, '', $default);
		if(!$r5->getContent())
			$r5->setContent('
<table class="gallerytable" border="1" cellspacing="0" cellpadding="0">
<tbody>
<tr>
<td class="category" colspan="2">Workflow Category</td>
</tr>
<tr>
<td>
<div class="wrapper">
<div class="txt"><a href="#">Example Workflow 1</a></div>
<div class="img">
<a href="#">
<img src="cache/thsm_Association_Test_Conditional_On_Matching.jpg" alt="" />
</a>
</div>
</div>
<div class="txt">Workflow 1 Description.</div>
</td>
<td>
<div class="wrapper">
<div class="txt"><a href="#">Example Workflow 2</a></div>
<div class="img">
<a href="#">
<img src="cache/thsm_Association_Test_Conditional_On_Matching.jpg" alt="" />
</a>
</div>
</div>
<div class="txt">Workflow 2 Description.</div>
</td>
</tr>
<tr>
<td class="category" colspan="2">More Workflows (coming soon)
</td>
</tr>
</tbody>
</table>');
		$r5->save();
		
		
		$advpid = $rportaladv->get('id');
		
		$rx = createDocument('Manage Data', 'WPManageData', '1', $advpid, '', $plain);
		if(!$rx->getContent())
			$rx->setContent("[[!workflow_portal_manageData? &guid=`1`]]");
		$rx->set('link_attributes', 'target="_manageData"');
		$rx->save();
		
		$rx = createDocument('Manage Component Types', 'WPManageComponentTypes', '2', $advpid, '', $plain);
		if(!$rx->getContent())
			$rx->setContent("[[!workflow_portal_manageComponents? &guid=`1`]]");
		$rx->set('link_attributes', 'target="_manageComponentTypes"');
		$rx->save();

		$rx = createDocument('Manage Components', 'WPManageComponents', '3', $advpid, '', $plain);
		if(!$rx->getContent())
			$rx->setContent("[[!workflow_portal_manageComponents? &guid=`1`&load_concrete=`true`]]");
		$rx->set('link_attributes', 'target="_manageComponents"');
		$rx->save();

		$rx = createDocument('Manage Domain', 'WPManageDomain', '4', $advpid, '', $default);
		if(!$rx->getContent())
			$rx->setContent("[[!workflow_portal_manageDomain? &guid=`1`]]");
		$rx->save();
		
		$rx = createDocument('TellMe', 'WPTellMe', '5', $advpid, '', $default);
		if(!$rx->getContent())
			$rx->setContent("[[!workflow_portal_workflowBrowser? &guid=`1` &editor_mode=`tellme`]]");
		//$rx->set('hidemenu', '1');
		$rx->save();

		
		$ops = createDocument('Operations', 'WPOperations', '98', '0', '', $default);
		$ops->set('hidemenu', '1');
		$ops->save();
		
		$opsid = $ops->get('id');
		
		$rx = createDocument('Run Workflow', 'WPRunWorkflow', '97', $opsid, '', '');
		$rx->setContent("[[!workflow_portal_workflowRunner]]");
		$rx->set('hidemenu', '1');
		$rx->save();
		
		$rx = createDocument('Data Operations', 'WPDataOps', '99', $opsid, '', '');
		$rx->setContent("[[!workflow_portal_manageData? &guid=`1`]]");
		$rx->set('hidemenu', '1');
		$rx->save();

		$rx = createDocument('Component Operations', 'WPComponentOps', '100', $opsid, '', '');
		$rx->setContent("[[!workflow_portal_manageComponents? &guid=`1`]]");
		$rx->set('hidemenu', '1');
		$rx->save();
		
		$rx = createDocument('Template Operations', 'WPTemplateOps', '101', $opsid, '', '');
		$rx->setContent("[[!workflow_portal_templateOperations? &guid=`1`]]");
		$rx->set('hidemenu', '1');
		$rx->save();
		
		$rx = createDocument('Access Result Operations', 'WPAccessResultOps', '102', $opsid, '', '');
		$rx->setContent("[[!workflow_portal_accessResults? &guid=`1`]]");
		$rx->set('hidemenu', '1');
		$rx->save();
		
		$rx = createDocument('Domain Operations', 'WPDomainOps', '103', $opsid, '', '');
		$rx->setContent("[[!workflow_portal_manageDomain? &guid=`1`]]");
		$rx->set('hidemenu', '1');
		$rx->save();

		
		$register = createDocument('Register', 'WPRegister', '104', '0', '', $default);
		$register->set('hidemenu', '1');
		$register->save();
		
		$activateaccount = createDocument('Activate Account', 'WPActivateAccount', '105', $register->get('id'), '', $default);
		$activateaccount->setContent("[[!ConfirmRegister]]<div class='info'>Your Account has been Activated !</div>");
		$activateaccount->save();

		$doneregister = createDocument('Thank you', 'WPRegistrationThanks', '106', $register->get('id'), '', $default);
		$doneregister->setContent("<div class='info'>Thank you for Registering ! Please CHECK YOUR EMAIL to activate the account.</div>");
		$doneregister->save();

		$register->setContent('
<h2>Register</h2>
[[Register?
    &submittedResourceId=`'.$doneregister->get('id').'`
    &activationResourceId=`'.$activateaccount->get('id').'`
    &activationEmailSubject=`Thanks for Registering with the Wings Portal!`
]]

<style>
.register fieldset {padding: 1em; border:1px solid #369}
.register label { float:left; width:20%; margin-right:0.5em;padding-top:0.2em;text-align:right; font-weight:bold;}
.register legend {padding: 0.2em 0.5em;border:1px solid #369;color:#369;text-align:right;}
.register span.error {font-weight:normal;font-size:80%;color:maroon;}
</style>

<div class="register">
    <div class="registerMessage">[[+error.message]]</div>
    <fieldset>
    <legend>Registration Information</legend>
    <form class="form" action="[[~[[*id]]]]" method="post">
        <input type="hidden" name="nospam:blank" value="" />
        
        <label for="username">[[%register.username]]</label>
        <input type="text" name="username:required:minLength=6" id="username" value="[[+username]]" />
        <span class="error">[[+error.username]]</span>
        <br />
        
        <label for="password">[[%register.password]]</label>
        <input type="password" name="password:required:minLength=6" id="password" value="[[+password]]" />
        <span class="error">[[+error.password]]</span>
        <br />
        
        <label for="password_confirm">[[%register.password_confirm]]</label>
        <input type="password" name="password_confirm:password_confirm=`password`" id="password_confirm" value="[[+password_confirm]]" />
        <span class="error">[[+error.password_confirm]]</span>
        <br />
        
        <label for="fullname">[[%register.fullname]]</label>
        <input type="text" name="fullname:required" id="fullname" value="[[+fullname]]" />
        <span class="error">[[+error.fullname]]</span>
        <br />
        
        <label for="email">[[%register.email]]</label>
        <input type="text" name="email:email" id="email" value="[[+email]]" />
        <span class="error">[[+error.email]]</span>
        <br class="clear" />
        
        <div class="form-buttons">
            <input type="submit" name="registerbtn" value="Register" />
        </div>
    </form>
    </fieldset>
</div>');
		$register->save();

		$success = true;
		break;

	case xPDOTransport::ACTION_UNINSTALL:
		
		deleteDocument('WPHome');
		deleteDocument("WPWhatsNew");
		deleteDocument('WPCredits');
		deleteDocument('WPAnalysis');
		deleteDocument('WPAdvanced');
		
		deleteDocument('WPAnalysisDescription');
		deleteDocument('WPBrowseWorkflows');
		deleteDocument('WPEditWorkflows');		
		deleteDocument('WPAccessResults');	
		deleteDocument('WPManageData');
		deleteDocument('WPManageComponents');
		deleteDocument('WPManageComponentTypes');
		deleteDocument('WPManageDomain');
		deleteDocument('WPWorkflowGallery');
		
		deleteDocument('WPOperations');
		deleteDocument('WPRunWorkflow');
		deleteDocument('WPDataOps');
		deleteDocument('WPComponentOps');
		deleteDocument('WPTemplateOps');
		deleteDocument('WPDomainOps');
		deleteDocument('WPAccessResultOps');

		deleteDocument('WPRegister');
		deleteDocument('WPActivateAccount');
		deleteDocument('WPRegistrationThanks');
		
		deleteSnippet('workflow_portal_accessResults');
		deleteSnippet('workflow_portal_workflowBrowser');
		deleteSnippet('workflow_portal_workflowRunner');
		deleteSnippet('workflow_portal_manageData');
		deleteSnippet('workflow_portal_manageDomain');
		deleteSnippet('workflow_portal_manageComponents');
		deleteSnippet('workflow_portal_templateOperations');

		deleteTemplate('Andreas');
		deleteTemplate('Plain');
		
		deleteDocument('WPTellMe');
		
		$success = true;
		break;

}
return $success;
?>
