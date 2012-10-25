<?php

// Get User Information
$uid = $modx->getLoginUserID();
$uname = $modx->getLoginUserName();
if(!$uid)
	return $modx->lexicon('workflow_portal.please_login');

// Load in Component Configuration
define("UID", $uid);
define('COMPONENT_NAME', 'workflow_portal');
require_once(MODX_CORE_PATH."components/".COMPONENT_NAME."/conf/config.inc.php");

$inifile = PORTAL_CONF_PATH.COMPONENT_NAME.'.ini';

ob_start();

?>
<h2>Workflow Portal Configuration</h2>
<style>
h2,h4 {
	color: #333;
	margin: 1em 0em 0em 1em;
}

div.uploader {
	float: right;
}

fieldset {
	padding: 1em;
	margin: 1em;
	font: 80%/ 1 sans-serif;
	border: 1px solid #999;
	background-color: #FFF;
}

label {
	float: left;
	color: #333;
	width: 25%;
	margin-right: 0.5em;
	padding-top: 0.5em;
	text-align: right;
	font-weight: bold;
}

.error,.borderinput {
	border: 1px solid #BBB;
	padding: 0.5em;
}

.buttoninput,legend {
	padding: 0.2em 0.5em;
	background-color: #777;
	color: #FFF;
	font-size: 110%;
	text-align: left;
}
</style>
<?

function readDomains($domaindir) {
	global $CONFIG;
	$installed_domains = array();
	if($dfn = opendir($domaindir)) {
		while(false !== ($fname = readdir($dfn))) {
			if(preg_match('/^[\._]/',$fname)) continue;
			if(is_dir($domaindir.'/'.$fname)) {
				array_push($installed_domains, $fname);
			}
		}
		closedir($dfn);
	}
	if(count($installed_domains)>0) {
		$CONFIG['domain']['installed_domain'] = $installed_domains;
		if(!$CONFIG['domain']['current_domain'])
		$CONFIG['domain']['current_domain'] = $installed_domains[0];
	}
}


// Save if needed
if(isset($_REQUEST['op']) && $_REQUEST['op'] == 'save') {
	require_once('conf/write_ini.inc.php');
	if(isset($_REQUEST['domain_file'])) {
		$domain = $_REQUEST['domain_file'];
		if(preg_match('/Error: (.+?)/', $domain, $m)) {
			print "<h4 style='color:red'>There were errors while uploading the domain. The domain will not be saved.</h4>\n";
			print "Debug: $domain";
		}
		else {
			$domaindir = PORTAL_ASSETS_PATH."domains";
			exec("unzip -o -d $domaindir $domain");
			unlink($domain);
			if(!$ret) {
				readDomains($domaindir);
			} else {
				print "<pre>Uploaded Zip file could not be unzipped\n</pre>";
			}
		}
	}
	else if(isset($_REQUEST['refresh'])) {
		$domaindir = PORTAL_ASSETS_PATH."domains";
		echo "reloading domains";		
		readDomains($domaindir);
	}

	if(!$_REQUEST['refresh']) {
		foreach($CONFIG as $section=>$values) {
			if($values) {
				foreach ($values as $key=>$value) {
					if(isset($_REQUEST[$key])) {
						$newvalue = $_REQUEST[$key];
						$charold = $value[strlen($value)-1];
						$charnew = $newvalue[strlen($newvalue)-1];
						if(($charold=='/' || $charold=='\\') && $charnew != $charold)
						$newvalue .= $charold;
						$CONFIG[$section][$key] = $newvalue;
					}
				}
			}
		}
	}

	if(write_ini_file($CONFIG, $inifile, true))
		print "<h4 style='color:green'>The portal configuration changes were saved</h4>\n";
	else
		print "<h4 style='color:red'>There were errors while saving the portal configuration</h4>\n";
}

$controller = preg_replace('/\.php$/','', basename(__FILE__));
$thisobj = $modx->getObject('modAction', array('namespace'=>COMPONENT_NAME, 'controller'=>$controller));

// Print form
print "<form enctype='multipart/form-data' id='portal_config_form' action='' method='post'>\n";
print "<input type='hidden' name='a' value='".$thisobj->get('id')."' />\n";
print "<input type='hidden' name='op' value='save' />\n";
foreach($CONFIG as $section=>$values) {
	$seclabel = strtoupper($section);
	print "<fieldset><legend>$seclabel</legend>\n";
	if($section=="domain") {
		// Special handling for Domain
		print "<label for='current_domain'>Domain:</label>\n";
		if(is_array($values['installed_domain'])) {
			print "<select class='borderinput' name='current_domain'>\n";
			print "<option value=''>-- Select --</option>\n";
			foreach ($values['installed_domain'] as $domain) {
				$sel='';
				if($domain == $values['current_domain'])
				$sel="selected";
				print "<option value='$domain' $sel>$domain</option>\n";
			}
			print "</select>";
		} else {
			print "<input type='text' disabled='disabled' class='error' value='No Domains installed !'/>\n";
		}
		print "<a href='javascript:refreshDomains()'>[Reload from Disk]</a>\n";
		//print "<div class='uploader'><b>Install New Domain (Zip File):</b><br/><br/>\n";
		//print "<input class='borderinput' type='text' id='txt_1_domain' name='txt_domain' disabled='disabled' />\n";
		//print "<span id='button_1_domain'></span>\n";
		//print "<div class='flash' id='progress_1_domain'></div></div>\n";
		//print "<input type='hidden' name='domain_file' id='domain_file_1' value='' />\n";
		//print "<input type='hidden' name='refresh' />\n";
	}
	else if($section=="theme") {
		// Special handling for Domain
		print "<label for='current_theme'>Theme:</label>\n";
		if(is_array($values['installed_theme'])) {
			print "<select class='borderinput' name='current_theme'>\n";
			print "<option value=''>-- Select --</option>\n";
			foreach ($values['installed_theme'] as $theme) {
				$sel='';
				if($theme == $values['current_theme'])
				$sel="selected";
				print "<option value='$theme' $sel>$theme</option>\n";
			}
			print "</select>";
		} else {
			print "<input type='text' disabled='disabled' class='error' value='No Themes installed !'/>\n";
		}
	}
	else if($section=="restrictions") {
		// Special handling for Sandbox
		print "<label for='sandbox'>Portal Mode:</label>\n";
		$sboxsel = $values['sandbox']=="true" ? "selected" : "";
		print "<select class='borderinput' name='sandbox'>\n";
		print "<option value='false'>No Restrictions</option>\n";
		print "<option value='true' $sboxsel>Sandboxed</option>\n";
		print "</select>";
		print " <i>(Sandboxed mode doesn't allow creating components and datatypes)</i>";
	}
	else {
		foreach ($values as $key=>$value) {
			$label = ucwords(strtolower(preg_replace('/_/',' ',$key)));
			print "<label for=\"$key\">$label:</label>\n";
			print "<input class='borderinput' type=\"text\" name=\"$key\" size=\"50\" value=\"$value\"/>\n";
			print "<br />\n";
		}
	}
	print "</fieldset>\n";
}
print "<input type='submit' value='Save' id='run_btn_1' class='buttoninput' />\n";
print "</form>\n";

// Create SWF UPload objects
require_once(PORTAL_INCLUDES_PATH."uploader.inc.php");
//createUploads(array('domain'=>'zip'), PORTAL_ASSETS_URL, UID, '1', 'upload_domain.php');

?>
<script>
	function refreshDomains() {
		var myform = document.getElementById('portal_config_form');
		myform.refresh.value=1;
		myform.submit();
	}
	
   // Override existing handlers
	function uploadDone(snipid) {
		try {
        	NUM_REMAINING_UPLOADS[snipid]--;
	    	if(NUM_REMAINING_UPLOADS[snipid] == 0) {
				document.getElementById('portal_config_form').submit();
        	}
		} catch (ex) {
			alert("Fix me: "+ex.message);
		}
	}

	function doSubmit(e, snipid) {
		if (formChecker != null) {
			clearInterval(formChecker);
			formChecker = null;
		}
	
		e = e || window.event;
		if (e.stopPropagation) {
			e.stopPropagation();
		}
		e.cancelBubble = true;

    	if(NUM_REMAINING_UPLOADS[snipid] == 0) {
			document.getElementById('portal_config_form').submit();
        	return;
    	}
	
		try {
        	for(var fu in swfu[snipid]) {
		    	swfu[snipid][fu].startUpload();
        	}
		} catch (ex) {
        	alert(ex.message);
		}
		return false;
	}

</script>
<?

$contents = ob_get_contents();
ob_end_clean();

return $contents;

