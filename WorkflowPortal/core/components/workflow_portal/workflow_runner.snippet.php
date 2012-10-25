<?php
/*
 * workflow_runner.snippet.php
 * - workflow_portal_workflowRunner Snippet
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
if(!$template_id)
return $modx->lexicon('workflow_portal.no_template');

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

// Load the Ontology Factory
require_once(PORTAL_INCLUDES_PATH."factory.inc.php");

// Load the Run Database
require_once(PORTAL_INCLUDES_PATH."rundb.inc.php");

// Load Seed Class (used to create a Seed from a Template)
require_once(PORTAL_INCLUDES_PATH."seed.inc.php");

// Load KBAPI
require_once(PORTAL_INCLUDES_PATH."kbapi.inc.php");

// Load Wings Java Interface
require_once(PORTAL_INCLUDES_PATH."wingsapi.inc.php");

function createSeed($tid, $template, $constraints, $domain, $props) {
	$template_url=$template->getNamespace()."/$tid.owl";
	$seedid = uniqid($tid."_Seed_");
	$base_uri = $props->getOntologyURL()."/$domain/seeds/$seedid.owl";
	$seed = new Seed($seedid, $base_uri, $template);
	$seed->addConstraints($constraints);

	$seeddir = USER_HOME_PATH.ONTOLOGY_DIR.$domain."/seeds/";
	if(!is_dir($seeddir)) mkdir($seeddir);
	$seed->saveAs($seeddir."$seedid.owl");
	return $seedid;
}

function RDFResource($const) {
	return new Resource(RDF_NAMESPACE_URI.$const);
}

function getResource($str, $nsmap) {
	$s = preg_split('/:/', $str);
	if(isset($s[1]) && isset($nsmap[$s[0]])) {
		return new Resource($nsmap[$s[0]].$s[1]);
	}
	$d = preg_split('/\^\^/', $str);
	if(isset($d[1]) && isset($d[0])) {
		$x = preg_split('/:/', $d[1]);
		if(isset($x[1]) && isset($nsmap[$x[0]])) {
			$lit = new Literal($d[0]);
			$lit->dtype = $nsmap[$x[0]].$x[1];
			return $lit;
		}
	}
	if(preg_match('/^http:/',$str)) return new Resource($str);
	return new Resource($nsmap['defaultns'].$str);
}

function parseConstraints($consStr, $nsmap) {
	$constraints = array();
	$cons = preg_split('/,/',$consStr);
	for($i=0; $i<sizeof($cons); $i+=3) {
		$subj = getResource($cons[$i], $nsmap);
		$pred = getResource($cons[$i+1], $nsmap);
		$obj = getResource($cons[$i+2], $nsmap);
		$st = new Statement($subj, $pred, $obj);
		array_push($constraints, $st);
	}
	return $constraints;
}

function sanitize($gnode) {
	$gnode = preg_replace('/[^a-zA-Z0-9_\.]/','_',$gnode);
	return preg_replace('/^(\d)/','_$1', $gnode);
}

function getVariableBindings($template, $props, $dc, $pc) {
	$varsdone = array();
	$constraints = array();
	$nsmap = $template->getNSMap();
	$tapi = new KBAPI('', true);

	$varDims = array();
	$iroles = $template->getInputRoles();
	foreach($iroles as $irole) {
		$varobj = $irole->getVariableMapping();
		$varDims[$varobj] = $irole->getDimensionality();
	}

	$cbstr = $_POST['cbindings'];
	$cbindings = $cbstr ? get_object_vars(json_decode($cbstr)) : array();
	$nodes = $template->getNodes();
	foreach($nodes as $node) {
		$comp = $node->getComponent();
		$cname = $comp->getName();
		$value = $cbindings[$cname];
		if($value) {
			$subj = getResource($cname, $nsmap);
			$pred = getResource('wflow:hasComponentBinding', $nsmap);
			$obj = getResource('acdom:'.$value, $nsmap);
			$tapi->addResourceStatement($subj, $pred, $obj);
		}
	}

	$links = $template->getInputLinks();
	foreach($links as $link) {
		$var = $link->getVariable();
		if($varsdone[$var->getId()]) continue;
		$varsdone[$var->getId()] = 1;

		$name = $var->getName();
		$node = $link->getDestinationNode();
		$comp = $node->getComponent();
		$subj = getResource($name, $nsmap);
		if($var->isData()) {
			$roles = $var->getRoles();
			$dim = array_key_exists($var->getId(),$varDims) ? $varDims[$var->getId()] : 0;

			$pred = getResource('wflow:hasDataBinding', $nsmap);
			$values = $_POST[$name];

			if($values && !preg_match('/Select .*\.\.\./', $values)) {
				if($dim) {
					// Handle Multi-Select Inputs
					$instr = "";
					$items = array();
					foreach(preg_split('/\s*,\s*/',$values) as $val) {
						if($instr) $instr .= ", ";
						$instr .= "[$val]";
						$obj = getResource($nsmap['dclib'].$val, $nsmap);
						array_push($items, $obj);
					}
					$list = $tapi->createList($items);
					$tapi->addResourceStatement($subj, $pred, $list);
				} else {
					// Copy over data
					$obj = getResource($nsmap['dclib'].$values, $nsmap);
					$tapi->addResourceStatement($subj, $pred, $obj);
				}
			}
		}
		else if($var->isParam()) {
			$pred = getResource('wflow:hasParameterValue', $nsmap);
			$val = $_POST[$name];
			if($val && !preg_match('/Enter a .* value\.\.\./', $val)) {
				$obj = getResource($val.'^^xsd:string', $nsmap);
				$tapi->addResourceStatement($subj, $pred, $obj);
			}
		}
	}

	//$dc->saveLibrary();
	return $tapi->getTriples(null, null, null);
	//return $constraints;
}

function runRequest($uid, $runid) {
	global $CONFIG;
	exec($CONFIG['system']['php']." ".PORTAL_INCLUDES_PATH."runRequest.php '".base64_encode($uid)."' $runid > /dev/null 2>&1 &");
}

function runPegasusRequest($uid, $runid) {
	global $CONFIG;
	exec($CONFIG['system']['php']." ".PORTAL_INCLUDES_PATH."runPegasusRequest.php '".base64_encode($uid)."' $runid > /dev/null 2>&1 &");
}

function setupPegasusRequest($uid, $runid, $inputstr, $pc, $pcdom, $pclib) {
	global $CONFIG;
	$pool = $CONFIG['grid']['pegasus_pool'];

	$rundir = $CONFIG['grid']['pegasus_storage_dir'].USER_DOMAIN."/$runid/";
	if(file_exists($rundir))
	recursive_unlink($rundir);
	mkdir($rundir, 0700, true);

	mkdir($rundir."config");

	recursive_copy(USER_HOME_PATH.PEGASUS_DIR, $rundir."config");
	symlink(USER_HOME_PATH.DATA_DIR, $rundir."data");

	$inputs = preg_split('/\s*,\s*/',$inputstr);
	$fh = fopen($rundir."config/rc.data", "w");
	foreach($inputs as $input) {
		if(preg_match('/\[([^\[\]]+?)\]/', $input, $res)) {
			$infile = $res[1];
			fwrite($fh, "$infile gsiftp://".$CONFIG['grid']['gsiftp_hostname'].$rundir."data/$infile pool=\"$pool\"\n");
		}
	}
	fclose($fh);

	$ch = $pc->getComponentHierarchy(null, true);
	$comps = array();
	// Flatten Hierarchy
	while(count($ch)) {
		$c = array_pop($ch);
		if($c['concrete']) array_push($comps, $c['id']);
		$ch = array_merge($ch, (array)$c['children']);
	}

	// Write tc.data
	$uid = UID;
	$dom = USER_DOMAIN;
	$nfsdir = $CONFIG['grid']['user_nfs_dir'];
	$fh = fopen($rundir."config/tc.data", "w");
	$tcsuffix = "INSTALLED\tINTEL32::LINUX\tNULL";
	foreach($comps as $cid) {
		fwrite($fh, "$pool\t$pcdom::$cid\t$nfsdir/$uid/$dom/code/$pclib/$cid/run\t$tcsuffix\n");
	}
	fclose($fh);
}


function runWings($op, $seedid, $requestid, $complibname) {
	$bindings = array();
	$explanations = array();
	$error = false;
	$exception = "";
	$alternatives = null;

	try  { $tmp = __javaproxy_Client_getClient(); }
	catch (Exception $ex) {
		return array("error"=>true, "explanations"=>array(), 
		"bindings"=>array(), "output"=>$ex->getMessage());
	}

	try {		
		$awg = getWorkflowGenerator($seedid, $requestid, false, $complibname);
		$sts = getSpecializedTemplates($awg);
		if(!sizeof($sts)) 
			return array("error"=>true, "bindings"=>array(), 
			"explanations"=>getExplanations($awg), "output"=>"");

		$bts = getBoundTemplates($awg, $sts);
		if(!sizeof($bts)) 
			return array("error"=>true, "bindings"=>array(),
			 "explanations"=>getExplanations($awg), "output"=>"");

		if($op == "getData") {
			$bindings = getDataBindings($bts);
			unlink(USER_HOME_PATH.ONTOLOGY_DIR.USER_DOMAIN."/seeds/$seedid.owl");
			return array("error"=>false, "bindings"=>$bindings, 
			 "explanations"=>getExplanations($awg), "output"=>"");
		}

		$cts = getConfiguredTemplates($awg, $bts);
		if(!sizeof($cts)) 
			return array("error"=>true, "bindings"=>array(), 
			 "explanations"=>getExplanations($awg), "output"=>"");

		if($op == "getParameters") {
			$bindings = getParameterBindings($cts);
			unlink(USER_HOME_PATH.ONTOLOGY_DIR.USER_DOMAIN."/seeds/$seedid.owl");
			return array("error"=>false, "bindings"=>$bindings, 
			 "explanations"=>getExplanations($awg), "output"=>"");
		}

		writeConfiguredTemplates($cts, $requestid);
		$execs = getExecutionScripts($awg, $cts);
		writeExecutionScripts($execs, $requestid);

		if(sizeof($execs) > 1)
			$alternatives = getTemplateListInfo($cts);
		else
			selectExecutionIndex($requestid, 1);

	} catch (JavaException $ex) {
		$error = true;
		$exception .= "Exception: ".$ex->toString()."\n";
		foreach($ex->getStackTrace() as $el) {
			$exception .= "|-- at ".$el->getClassName().".".$el->getMethodName();
			$exception .= "(".$el->getFileName().":".$el->getLineNumber().")\n";
		}
	}

	// Remove Temporary Seed (?)
	unlink(USER_HOME_PATH.ONTOLOGY_DIR.USER_DOMAIN."/seeds/$seedid.owl");

	return array("error"=>$error, "bindings"=>array(), 
	 "explanations"=>getExplanations($awg), "output"=>$exception, 
	 "alternatives"=>$alternatives, "request_id"=>$requestid, "seed_id"=>$seedid);
}

function selectExecutionIndex($requestid, $index) {
	$dir = USER_HOME_PATH.OUTPUT_DIR.$requestid;
	$files = array();
	if($handle = opendir($dir)) {
		while(false != ($file = readdir($handle))) {
			if(preg_match("/^$requestid/", $file)) {
				array_push($files, $file);
			}
		}
	}
	$pfx = $requestid."_".$index;
	foreach($files as $file) {
		if(preg_match("/^$pfx(.+)$/",$file, $m)) {
			rename("$dir/$file", "$dir/$requestid".$m[1]);
		} else {
			unlink("$dir/$file");
		}
	}
}

function registerData($modx, $uid, $runid, $requestid) {
	$pfx = USER_HOME_PATH.OUTPUT_DIR . $requestid."/" . $requestid;
	if(file_exists($pfx . ".sh.map"))
		$lines = file($pfx . ".sh.map");
	else
		$lines = file($pfx . ".dax.map");

	$in = preg_replace('/^input:{/','',$lines[0]);
	$in = preg_replace('/}$/','',$in);
	$int = preg_replace('/^intermediate:{/','',$lines[1]);
	$int = preg_replace('/}$/','',$int);
	$out = preg_replace('/^output:{/','',$lines[2]);
	$out = preg_replace('/}$/','',$out);

	setRunInputs($modx, $uid, $runid, $in);
	setRunIntermediates($modx, $uid, $runid, $int);
	setRunOutputs($modx, $uid, $runid, $out);

	return $in;
}

function encodeResults($msg) {
	return json_encode(array("success"=> !$msg["error"], "data"=>$msg));
}


$op = "";
if(isset($_REQUEST['op'])) $op = $_REQUEST['op'];

# Main
{
	$result = array();

	$fac = new Factory($WINGS_PROPS);
	$dc = $fac->getDC();
	$pc = $fac->getPC(true);
	$pcdom = $WINGS_PROPS->getPCDomain();
	$pclib = $fac->getPCLibname();

	if($op == "run_alternative") {
		$seedid = $_REQUEST['seed_id'];
		$requestid = $_REQUEST['request_id'];
		$index = $_REQUEST['index'];

		if($requestid && $index) {
			selectExecutionIndex($requestid, $index);
			$result['error'] = false;
			$result['seed_id'] = $seedid;
			$result['request_id'] = $requestid;
		}
		else {
			return encodeResults(array("error"=>true, 
				"output"=>"No index or request id"));
		}
	}
	else {
		$template = $fac->getTemplate($template_id);

		$domain = $WINGS_PROPS->getTemplateDomain();
		$constraints = getVariableBindings($template, $WINGS_PROPS, $dc, $pc);

		//return encodeResults(array("output"=>$constraints, "error"=>true));

		// Create the seed in seeds/users/uid directory
		$seedid = createSeed($template_id, $template, $constraints, $domain, $WINGS_PROPS);

		// Run the seed with a custom request id, and custom logs/output directory
		$requestid = uniqid($template_id."_Run_");

		// Run Wings with the specified operation
		if($op != "") {
			return encodeResults(runWings($op, $seedid, $requestid, $pclib));
		}
		else {
			$result = runWings('', $seedid, $requestid, $pclib);
			if($result["error"] || $result["alternatives"])
				return encodeResults($result);
		}
	}
	
	connectRunDB($modx);
	
	$runid = registerRun($modx, UID, $seedid, $requestid, $template_id, USER_DOMAIN);
	if(!$runid) 
		return encodeResults(array("error"=>true, "output"=>"Could not register your run. Quitting"));

	$result["run_id"] = $runid;

	$inputstr = registerData($modx, UID, $runid, $requestid);
	
	if($WINGS_PROPS->getOutputFormat() == "shell") {
		runRequest(UID, $runid);
	}
	else {
		setupPegasusRequest(UID, $runid, $inputstr, $pc, $pcdom, $pclib);
		runPegasusRequest(UID, $runid);
	}

	$doc = $modx->getObject('modDocument', array('alias' => "WPAccessResults"));
	$docid = $doc->get('id');

	return encodeResults($result);
}
?>
