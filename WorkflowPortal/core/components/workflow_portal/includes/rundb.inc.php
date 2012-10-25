<?php

require_once("uploader.inc.php");
require_once("zip_operations.inc.php");
require_once("recursive_operations.inc.php");

function connectRunDB($modx) {
	if(defined('PORTAL_MODEL_PATH'))
		$modx->addPackage("workflow_portal", PORTAL_MODEL_PATH);
	else
		die("No Portal Model Path to connect to");
}

function unRegisterRun($modx, $uid, $id) {
	$run = $modx->getObject('workflow_runs', array('id'=>$id, 'uid'=>$uid));
	if($run) $run->remove();
}

function registerRun($modx, $uid, $seedid, $requestid, $templateid, $domain) {
	$run = $modx->newObject('workflow_runs');  
	$run->fromArray(array(  
    	'uid' => $uid,
    	'seedid' => $seedid,
    	'requestid' => $requestid,
    	'templateid' => $templateid,
    	'domain' => $domain
	));
	$run->save(); 
	return $run->get('id');
}

function registerRunStart($modx, $uid, $id) {
	$run = $modx->getObject('workflow_runs', array('id'=>$id, 'uid'=>$uid));
	$run->set('start_time', date('Y-m-d H:i:s'));
	$run->save();
}

function registerRunEnd($modx, $uid, $id) {
	$run = $modx->getObject('workflow_runs', array('id'=>$id, 'uid'=>$uid));
	$run->set('end_time', date('Y-m-d H:i:s'));
	$run->save();
}

function setRunLog($modx, $uid, $id, $log) {
	$run = $modx->getObject('workflow_runs', array('id'=>$id, 'uid'=>$uid));
	$run->set('log', $log);
	$run->save();
}

function setRunStatus($modx, $uid, $id, $status) {
	$run = $modx->getObject('workflow_runs', array('id'=>$id, 'uid'=>$uid));
	$run->set('status', $status);
	$run->save();	
}

function setRunProgress($modx, $uid, $id, $numjobs_total, $numjobs_executed, $running_job) {
	$run = $modx->getObject('workflow_runs', array('id'=>$id, 'uid'=>$uid));
	$run->fromArray(array(
			'numjobs_total' => $numjobs_total,
			'numjobs_executed' => $numjobs_executed,
			'running_job' => $running_job
	));
	$run->save();	
}

function setRunInputs($modx, $uid, $id, $vals) {
	$run = $modx->getObject('workflow_runs', array('id'=>$id, 'uid'=>$uid));
	$run->set('inputs', $vals);
	$run->save();
}

function setRunIntermediates($modx, $uid, $id, $vals) {
	$run = $modx->getObject('workflow_runs', array('id'=>$id, 'uid'=>$uid));
	$run->set('intermediates', $vals);
	$run->save();	
}

function setRunOutputs($modx, $uid, $id, $vals) {
	$run = $modx->getObject('workflow_runs', array('id'=>$id, 'uid'=>$uid));
	$run->set('outputs', $vals);
	$run->save();	
}

function getRunDetailsForUser($modx, $uid, $id, $objOnly) {
  	$runObj = $modx->getObject('workflow_runs', array('id'=>$id, 'uid'=>$uid));
	if($objObly) return $runObj;

	if($runObj) 
		return array("results"=>1, "rows"=>getRunArray($runObj));
	return array("results"=>0, "rows"=>array());
}

function getRunDetailsForUserRDF($modx, $uid, $uname, $id, $image, $lib, $exformat) {
  	$runObj = $modx->getObject('workflow_runs', array('id'=>$id, 'uid'=>$uid));
	$runArr = getRunArray($runObj);
	$imgUrl = saveRunImage($runArr, $image);
	return getRunRDF($runArr, $uid, $uname, $imgUrl, $lib, $exformat);
}

function getRunDetailsForUserHTML($modx, $uid, $uname, $id, $image) {
  	$runObj = $modx->getObject('workflow_runs', array('id'=>$id, 'uid'=>$uid));
	$runArr = getRunArray($runObj);
	$imgUrl = saveRunImage($runArr, $image);
	return getRunHTML($runArr, $uname, $imgUrl);
}

function getAllRunsForUser($modx, $uid, $domain, $objOnly) {
	$runObjs = $modx->getCollection('workflow_runs', array('uid'=>$uid, 'domain'=>$domain));
	if($objObly) return $runObjs;

	$runs = array();
	foreach ($runObjs as $runObj) {
		array_push($runs, getRunArray($runObj, true));
	}
	return array("results"=>sizeof($runs), "rows"=>$runs);
}

function getBindingDetails($binding, $isParam) {
	$arr = array();
	if(is_array($binding)) {
		foreach($binding as $b) {
			array_push($arr, getBindingDetails($b, $isParam));
		}
	}
	else if($isParam) {
		$arr['id'] = $binding;
		$arr['param'] = true;
	}
	else {
		$bid = print_r($binding, true);
		$metrics = null;
		if(is_object($binding)) {
			$binding = get_object_vars($binding);
			$bid = $binding['id'];
			$metrics = $binding['m'];
		}
		$path = USER_HOME_PATH.DATA_DIR.$bid;
		//$url = USER_HOME_URL.DATA_DIR.$bid;
		$size = -1;
		if(file_exists($path)) {
			try { $size = filesize($path); }
			catch (Exception $e) { }
		}
		$arr = array();
		$arr['id'] = $bid;
		$arr['size'] = $size;
		$arr['metrics'] = $metrics;
		//$arr['url'] = $url;
	}
	return $arr;
}

function getTemplateBindingDetails($t) {
	$vars = $t->variables;
	$bindings = array();
	foreach($vars as $v) {
		$bindings[$v->id] = getBindingDetails($v->binding, $v->type=="PARAM");
	}

	$bdetails = array();
	$varsDone = array();
	foreach($t->links as $l) {
		$v = $l->variable;
		if($varsDone[$v]) continue;
		$varsDone[$v] = true;
		$type = "Intermediate";
		if(!$l->fromNode) $type="Input";
		else if(!$l->toNode) $type="Output";
		array_push($bdetails, array("v"=>$v, "b"=>$bindings[$v], "type"=>$type));
	}
	return $bdetails;
}

function getRunArray($run, $short) {
	$r = array();

	$r['id'] = $run->get('id');
	$r['seedid'] = $run->get('seedid');
	$r['requestid'] = $run->get('requestid');
	$r['templateid'] = $run->get('templateid');
	$r['status'] = $run->get('status');
	if(!$r['status']) $r['status'] = "ONGOING";

	$is_started = false; $is_finished = false;
	$is_failure = false; $is_success = false; $is_ongoing = false;
	if($r['status'] == "FAILURE") $is_failure = true;
	else if($r['status'] == "SUCCESS") $is_success = true;
	else $is_ongoing = true;

	$r['start_time'] = strtotime($run->get('start_time'));
	$r['end_time'] = strtotime($run->get('end_time'));
	if($r['start_time']>0) $is_started = true;
	if($r['end_time']>0) $is_finished = true;

	if($r['status'] != "SUCCESS") {
		$r['running_jobs'] = $run->get('running_job');
		$r['numrunning_jobs'] = count(preg_split('/,/',$r['running_jobs']));
		$r['percent_done'] = round(100 * ($run->get('numjobs_executed') - $r['numrunning_jobs']) / $run->get('numjobs_total'));
		$r['percent_doing'] = round(100 * $run->get('numjobs_executed') / $run->get('numjobs_total'));
		if($r['percent_done'] < 0) $r['percent_done'] = 0;
	}

	if($short) 
		return $r;

	//$r['inputs'] = getBindingsFromMapString($run->inputs);
	//$r['intermediates'] = getBindingsFromMapString($run->intermediates);
	//$r['outputs'] = getBindingsFromMapString($run->outputs);

	$r['log'] = $run->get('log');
	$r['template'] = getRequestWorkflow($r['requestid']);
	
	$b = getTemplateBindingDetails($r['template']);
	$r['bindings'] = array("results"=>sizeof($b), "rows"=>$b);

	return $r;
}

function getBindingHTML($binding, $durl) {
	if(is_array($binding) && $binding[0]) {
		$str .= "{<ul>\n";
		foreach($binding as $b) {
			$str .= getBindingHTML($b, $durl);
		}
		$str .= "</ul>}\n";
	}
	else if($binding['param']) {
		$str .= "<li>".$binding['id']."</li>\n";
	}
	else {
		if($binding['size'] > 0) {
			$str .= "<li><a href='$durl".$binding['id']."'>".$binding['id']."</a> <i>(".$binding['size']." bytes)</i></li>\n";
		}
		else {
			$str .= "<li>-Not Available-</li>\n";
		}
	}
	return $str;
}

function getRunHTML($r, $uname, $imgurl) {
	$t = get_object_vars($r['template']);
	$ns = get_object_vars($t['nsmap']);
	$tid = $r['templateid'];
	$svr = ($_SERVER['HTTPS'] ? 'https' : 'http'). '://'.$_SERVER['SERVER_NAME'];
	$turl = $svr.USER_HOME_URL.ONTOLOGY_DIR.USER_DOMAIN.'/'.$r['templateid'].'.owl';
	$durl = $svr.USER_HOME_URL.DATA_DIR;
	$htmlurl = $svr.USER_HOME_URL.RUNS_DIR."run_".$r['id'].".html";
	$vbs = $r['bindings']['rows'];

	// Create Runs directory if it doesn't exist
	if(!file_exists(USER_HOME_PATH.RUNS_DIR)) 
		mkdir(USER_HOME_PATH.RUNS_DIR);

	$str = "<html>";
	$str .= "<body bgcolor='white'>";
	//$str .= "<style>h2{padding-left:10px} h3{padding-left:30px}</style>\n";
	//$str .= "<style>*{font-family:Verdana;font-size:11px;}</style>\n";
	$str .= "<style>h3{margin-bottom:5px;}</style>\n";
	$str .= "<style>ul{margin:0px;}</style>\n";
	$str .= "<style>h2.header{background-color:#ddd;border-bottom:1px solid #999}</style>\n";
	$str .= "<style>.data{padding-left:15px; padding-bottom:15px;}</style>\n";
	$str .= "<style>.data{font-size:12px}</style>\n";
	$str .= "<h1 class='header'>Run ".$r['id']."</h1>\n";
	$str .= "<ul>\n";
	$str .= "<li> - by user $uname</li>\n";
	$str .= "<li> - ran template <a href='$turl'>$tid</a></li>\n";
	$str .= "<li> - with status: ".$r['status']."</li>\n";
	$str .= "<li> - started at: ".date('r', $r['start_time'])."</li>\n";
	$str .= "<li> - ended at: ".date('r', $r['end_time'])."</li>\n";
	$str .= "</ul>\n";

	if($imgurl) {
		$str .= "<a href='$imgurl' target='_blank'><img src='$imgurl' width='400' /></a>\n";
	}

	$inputs = array();
	$inouts = array();
	$outputs = array();
	foreach($vbs as $vb) {
		if($vb['type'] == "Input") array_push($inputs, $vb);
		else if($vb['type'] == "Output") array_push($outputs, $vb);
		else array_push($inouts, $vb);
	}

	$str .= "<h2 class='header'>Inputs</h2>\n";
	$str .= "<div class='data'>\n";
	foreach($inputs as $vb) {
		$str .= "<h3>".$vb['v']."</h3>";
		$str .= getBindingHTML($vb['b'], $durl);
	}
	$str .= "</div>\n";

	$str .= "<h2 class='header'>Outputs</h2>\n";
	$str .= "<div class='data'>\n";
	foreach($outputs as $vb) {
		$str .= "<h3>".$vb['v']."</h3>";
		$str .= getBindingHTML($vb['b'], $durl);
	}
	$str .= "</div>\n";

	$str .= "<h2 class='header'>Intermediate Data</h2>\n";
	$str .= "<div class='data'>\n";
	foreach($inouts as $vb) {
		$str .= "<h3>".$vb['v']."</h3>";
		$str .= getBindingHTML($vb['b'], $durl);
	}
	$str .= "</div>\n";

	$htmlfile = fopen(USER_HOME_PATH.RUNS_DIR."run_".$r['id'].".html", "w");
	fwrite($htmlfile, $str);
	fclose($htmlfile);
	
	return $htmlurl;
}

function saveRunImage($r, $imagedata) {
	// Create Runs directory if it doesn't exist
	if(!file_exists(USER_HOME_PATH.RUNS_DIR)) 
		mkdir(USER_HOME_PATH.RUNS_DIR);

	if(preg_match('/^data:image\/(png|jpeg);base64,(.*)$/', $imagedata, $m)) {
		$extension = $m[1];
		$imgbinary = base64_decode($m[2]);

		$imgurl = $svr.USER_HOME_URL.RUNS_DIR."run_".$r['id'].".$extension";
		$imgpath = USER_HOME_PATH.RUNS_DIR."run_".$r['id'].".$extension";

		file_put_contents($imgpath, $imgbinary);
		return $imgurl;
	}
}

function getValueRDF($o) {
	if(preg_match('/^(true|false)$/i', $o)) $o = "\"$o\"^^xsd:boolean";
	else if(preg_match('/^\-?\d+$/i', $o)) $o = "\"$o\"^^xsd:integer";
	else if(preg_match('/^\-?(\d|E\-|\.)+$/i', $o)) $o = "\"$o\"^^xsd:float";
	else if(preg_match('/^\d{4}\-\d{2}\-\d{2}Z?$/', $o)) $o = "\"$o\"^^xsd:date";
	else if(preg_match('/^http:/', $o)) $o = "\"$o\"";
	else if(!preg_match('/\S+:\S+/', $o)) $o = "\"$o\"^^xsd:string";
	return $o;
}

function getBindingRDF($binding, $durl, $numtabs=1, $uid, $rid, $tmpfolder) {
	$str = "";
	$tab = "";
	for($i=0; $i<$numtabs; $i++) $tab .= "\t";

	if(is_array($binding) && $binding[0]) {
		foreach($binding as $b) {
			$str .= getBindingRDF($b, $durl, $numtabs, $uid, $rid, $tmpfolder);
		}
	}
	else if($binding['param']) {
		$str = "";
	}
	else {
		$str .= $tab."dclib:".$binding['id']."\n";

		$bname = $binding['id'];
    	$tmpfile = "$tmpfolder/$bname";
    	$burl = OPMW_FILE_SERVER."resource/$uid/$rid/$bname";
    	if(!file_exists($tmpfile)) {
			copy(USER_HOME_PATH.DATA_DIR.$bname, $tmpfile);
    	}
		$str .= $tab."\twflow:hasLocation <$burl> ;\n";
		$str .= $tab."\twflow:hasSize \"".$binding['size']."\"^^xsd:integer ;\n";
		foreach($binding['metrics'] as $bm) {
			$o = getValueRDF($bm->o);
			$str .= $tab."\t".$bm->p." $o ;\n";
		}
		$str .= ".\n";
	}
	return $str;
}

function getRecursiveBindingRDF($b, $isparam, $numtabs) {
	$str = "";
	$tab = "";
	for($i=0; $i<$numtabs; $i++) $tab .= "\t";
	if(is_array($b) && sizeof($b)>0) {
		$str .= $tab."(\n";
		foreach($b as $cb) {
			$str .= getRecursiveBindingRDF($cb, $isparam, $numtabs+1);
		}
		$str .= $tab.")\n";
	}
	else {
		$val = $isparam ? getValueRDF($b) : "dclib:$b";
		$str .= $tab."$val\n";
	}
	return $str;
}

function getRecursiveCompBindingRDF($b, $bd, $ior, $iov, $params, $numtabs, $uid, $rid, $lib, $tmpfolder) {
	$str = "";
	$tab = "";
	for($i=0; $i<$numtabs; $i++) $tab .= "\t";
	if(is_array($b) && sizeof($b)>0) {
		$str .= $tab."(\n";
		for($i=0; $i<sizeof($b); $i++) {
			$cbd = $bd ? $bd[$i] : null;
			$str .= getRecursiveCompBindingRDF($b[$i], $cbd, $ior, $iov, $params, $numtabs+1, $uid, $rid, $lib, $tmpfolder); 
		}
		$str .= $tab.")\n";
	}
	else {
		$str .= $tab."[\n";
		$str .= getCompBindingRDF($b, $bd, $ior, $iov, $params, $numtabs+1, $uid, $rid, $lib, $tmpfolder);
		$str .= $tab."]\n";
	}
	return $str;
}

function getArgBindingRDF($r, $v, $bd, $isparam, $numtabs) {
	$tab = "";
	for($i=0; $i<$numtabs; $i++) $tab .= "\t";
	$str = "$tab\tac:hasArgumentID \"$r\" ;\n";
	$str .= "$tab\twflow:hasVariable tmp:$v ;\n";
	if($bd) {
		$prop = $isparam ? "wflow:hasParameterBinding" : "wflow:hasDataBinding";
		if(is_array($bd->$r)) {
			$str .= "$tab\t$prop\n";
			$str .= getRecursiveBindingRDF($bd->$r, $isparam, $numtabs+1);
		}
		else {
			$val = $isparam ? getValueRDF($bd->$r) : "dclib:".$bd->$r;
			$str .= "$tab\t$prop $val ;\n";
		}
	}
	return $str;
}

function getCompBindingRDF($b, $bd, $ior, $iov, $params, $numtabs, $uid, $rid, $lib, $tmpfolder) {
	$str = "";
	$tab = "";

	$zname = "$b.zip";
	$tmpzip = "$tmpfolder/$zname";
	$zipurl = OPMW_FILE_SERVER."resource/$uid/$rid/$zname";
	if(!file_exists($tmpzip)) {
		$codedir = USER_HOME_PATH.CODE_DIR.$lib."/".$b;
		new ZipFolder($tmpzip, $codedir, $b);
	}
	for($i=0; $i<$numtabs; $i++) $tab .= "\t";
	$str .= $tab."wflow:hasComponent acdom:$b ;\n";
	$str .= $tab."wflow:hasLocation <$zipurl> ;\n";

	$i=0;
	foreach($ior['inputs'] as $r) {
		$v = $iov['inputs'][$i];
		$isparam = $params[$v];
		$str .= $tab."wflow:hasInput [\n";
		$str .= getArgBindingRDF($r, $v, $bd, $isparam, $numtabs+1);
		$str .= $tab."] ;\n";
		$i++;
	}
	$i=0;
	foreach($ior['outputs'] as $r) {
		$v = $iov['outputs'][$i];
		$isparam = $params[$v];
		$str .= $tab."wflow:hasOutput [\n";
		$str .= getArgBindingRDF($r, $v, $bd, $isparam, $numtabs+1);
		$str .= $tab."] ;\n";
		$i++;
	}
	return $str;
}

function isAssoc($arr) {
    return array_keys($arr) !== range(0, count($arr) - 1);
}

function deleteBindings($binding) {
	if(is_array($binding) && !isAssoc($binding)) {
		foreach($binding as $b) {
			deleteBindings($b);
		}
	}
	else {
		if(!$binding['param'])
			@unlink(USER_HOME_PATH.DATA_DIR.$binding['id']);
	}
}

function deleteRunFiles($r, $deloutput) {
	$vbs = $r['rows']['bindings']['rows'];
	$inouts = array();
	$outputs = array();
	foreach($vbs as $vb) {
		if($vb['type'] == "Input") {}
		else if($vb['type'] == "Output") array_push($outputs, $vb['b']);
		else array_push($inouts, $vb['b']);
	}

	foreach($inouts as $b) 
		deleteBindings($b);

	if($deloutput) {
		foreach($outputs as $b) 
			deleteBindings($b);
	}

	$rid = $r['rows']['requestid'];
	recursive_unlink(USER_HOME_PATH.OUTPUT_DIR.$rid);
}

function getExecutionEngine($exformat, $numtabs) {
	global $CONFIG;
	$str = "";
	$tab = "";
	for($i=0; $i<$numtabs; $i++) $tab .= "\t";

	$str .= $tab."wflow:hasExecutionEngine [\n";

	if($exformat == "shell") {
		$version = `sh --version`;
		if(preg_match('/version\S*\s+(\S+)\s/ims', $version, $m)) 
			$version = $m[1];
		$str .= "$tab\twflow:usesTool [\n";
		$str .= "$tab\t\twflow:hasID \"Shell\" ;\n";
		$str .= "$tab\t\twflow:hasURL <http://www.gnu.org/s/bash/> ;\n";
		$str .= "$tab\t\twflow:hasVersion \"$version\" ;\n";
		$str .= "$tab\t] ;\n";
	}
	else {
		putenv("JAVA_HOME=".$CONFIG['system']['java_home']);
		$cmd = $CONFIG['grid']['pegasus_home']."/bin/pegasus-plan --version";
		$version = `$cmd`;
		if(preg_match('/version\S*\s+(\S+)\s/ims', $version, $m)) 
			$version = $m[1];
		$str .= "$tab\twflow:usesTool [\n";
		$str .= "$tab\t\twflow:hasID \"Pegasus\" ;\n";
		$str .= "$tab\t\twflow:hasURL <http://pegasus.isi.edu> ;\n";
		$str .= "$tab\t\twflow:hasVersion \"$version\" ;\n";
		$str .= "$tab\t] ;\n";

		$cmd = $CONFIG['grid']['condor_home']."/bin/condor -version";
		$version = `$cmd`;
		if(preg_match('/version\S*\s+(\S+)\s/ims', $version, $m)) 
			$version = $m[1];
		$str .= "$tab\twflow:usesTool [\n";
		$str .= "$tab\t\twflow:hasID \"Condor\" ;\n";
		$str .= "$tab\t\twflow:hasURL <http://www.cs.wisc.edu/condor/> ;\n";
		$str .= "$tab\t\twflow:hasVersion \"$version\" ;\n";
		$str .= "$tab\t] ;\n";
	}
	$str .= $tab."] ;\n";

	return $str;
}


function getRunRDF($r, $uid, $uname, $imgurl, $lib, $exformat) {
	$t = get_object_vars($r['template']);
	$ns = get_object_vars($t['nsmap']);
	$rid = "wflow:".$r['requestid'];
	$tid = 'tmp:'.$r['templateid'];
	$svr = ($_SERVER['HTTPS'] ? 'https' : 'http'). '://'.$_SERVER['SERVER_NAME'];
	$turl = $svr.USER_HOME_URL.ONTOLOGY_DIR.USER_DOMAIN.'/'.$r['templateid'].'.owl';
	$timgurl = $turl.".png";
	$durl = $svr.USER_HOME_URL.DATA_DIR;
	$ttlurl = $svr.USER_HOME_URL.RUNS_DIR."run_".$r['id'].".ttl";
	$vbs = $r['bindings']['rows'];
	$nodes = $t['nodes'];
	$links = $t['links'];
	$vars = $t['variables'];
	$rid = $r['id'];
	$stime = date('Y-m-d\TH:i:sP', $r['start_time']);
	$etime = date('Y-m-d\TH:i:sP', $r['end_time']);

	// Create Runs directory if it doesn't exist
	if(!file_exists(USER_HOME_PATH.RUNS_DIR)) 
		mkdir(USER_HOME_PATH.RUNS_DIR);

	// Create a temporary folder to add files to be uploaded
	$tmpfolder = sys_get_temp_dir()."/wingsrun_".$uid."_".$rid;
	mkdir($tmpfolder, 0755, true);

	$str = "";
	$ontfiles = array();
	foreach($ns as $pfx=>$ns) {
		if($pfx=="_empty_") $pfx = "tmp";
		if($pfx == "dcdom" || $pfx == "acdom") {
			if(preg_match("/.*\/(.+\/.+)\/(.+)$/", $ns, $m)) {
				$nspath = $m[1];
				$ns = OPMW_FILE_SERVER."resource/$uid/$rid/".$m[2];
				$nsfile = preg_replace("/#$/", "", $m[2]);
				copy(USER_HOME_PATH.ONTOLOGY_DIR.$nspath."/$nsfile", $tmpfolder."/$nsfile");
			}
		}
		else {
			//$ns = str_replace("http://www.isi.edu", OPMW_FILE_SERVER."ontology/$uid/$rid", $ns);
		}
		$str .= "@prefix $pfx: <$ns> .\n";
	}
	$str .= "\n";
	$str .= "<$ttlurl>\n";
	$str .= "\twflow:hasRunId \"".$r['id']."\"^^xsd:integer ;\n";
	$str .= "\twflow:hasUser \"$uname\"^^xsd:string ;\n";
	$str .= "\twflow:creationTool \"http://wings.isi.edu\"^^xsd:anyURI ;\n";
	$str .= "\twflow:hasLicense \"http://creativecommons.org/licenses/by-sa/3.0/\"^^xsd:anyURI ;\n";
	//$str .= "\twflow:hasExecutionEngine \"$engine\"^^xsd:string ;\n";
	$str .= getExecutionEngine($exformat, 1);
	$str .= "\twflow:usesTemplate [\n";
	$str .= "\t\twflow:hasID $tid ;\n";
	$str .= "\t\twflow:hasURL <$turl> ;\n"; 
	$str .= "\t\twflow:hasTemplateDiagram <$timgurl>\n"; 
	$str .= "\t] ;\n";
	$str .= "\twflow:hasExecutionDiagram <$svr$imgurl> ;\n"; 
	$str .= "\twflow:hasStatus \"".$r['status']."\"^^xsd:string ;\n";
	$str .= "\twflow:hasStartTime \"$stime\"^^xsd:dateTime ;\n";
	$str .= "\twflow:hasEndTime \"$etime\"^^xsd:dateTime ;\n";

	$params = array();
	foreach($t['variables'] as $v) {
		if($v->type == "PARAM")
			$params[$v->id] = 1;
	}

	$nodeIORole = array();
	$nodeIOVar = array();
	foreach($links as $l) {
		$fn = $l->fromNode;
		$v = $l->variable;
		if($fn) {
			$role = $l->fromPort->role->id;
			if(array_key_exists($fn, $nodeIORole)) {
				array_push($nodeIORole[$fn]["outputs"], $role);
				array_push($nodeIOVar[$fn]["outputs"], $v);
			}
			else {
				$nodeIORole[$fn] = array("inputs"=>array(), "outputs"=>array($role));
				$nodeIOVar[$fn] = array("inputs"=>array(), "outputs"=>array($v));
			}
		}
		$tn = $l->toNode;
		if($tn) {
			$role = $l->toPort->role->id;
			if(array_key_exists($tn, $nodeIORole)) {
				array_push($nodeIORole[$tn]["inputs"], $role);
				array_push($nodeIOVar[$tn]["inputs"], $v);
			}
			else {
				$nodeIORole[$tn] = array("inputs"=>array($role), "outputs"=>array());
				$nodeIOVar[$tn] = array("inputs"=>array($v), "outputs"=>array());
			}
		}
	}

	foreach($nodes as $node) {
		$c = $node->component;
		$ior = $nodeIORole[$node->id];
		$iov = $nodeIOVar[$node->id];
		$cb = $c->binding;
		$cbd = $c->binding_details;
		$arrb = is_array($cb);
		$str .= "\twflow:hasNode [\n";
		$str .= "\t\twflow:hasID tmp:".$node->id." ;\n";
		$str .= "\t\twflow:hasComponentType acdom:".$c->type." ;\n";
		$str .= "\t\twflow:hasComponentBinding\n";
		$str .= getRecursiveCompBindingRDF($cb, $cbd, $ior, $iov, $params, 2, $uid, $rid, $lib, $tmpfolder);
		$str .= "\t] ;\n";
	}
	$str .= "\t .\n";

	foreach($vbs as $vb) {
		$str .= getBindingRDF($vb['b'], $durl, 0, $uid, $rid, $tmpfolder);
	}

	// Write the Run RDF (in TTL format)
	$runfile = USER_HOME_PATH.RUNS_DIR."run_".$r['id'].".ttl";
	$rdffile = fopen($runfile, "w");
	fwrite($rdffile, $str);
	fclose($rdffile);

	// Get all files in $tmpfolder
	$upfiles = array();
	$dh = opendir($tmpfolder);
	while(($file = readdir($dh)) !== false) {
		if($file != "." && $file != "..") {
			array_push($upfiles, "$tmpfolder/$file");
		}
	}
	closedir($dh);

	// Upload all the files
	upload_files_to_server($uid, $rid, $upfiles); 

	// Delete the tmp folder
	recursive_unlink($tmpfolder);

	// Publish OPM of run
	$runfile_tmp = sys_get_temp_dir()."/".$uname."_run_".$r['id']."_opm.ttl";
	convertRunFileToOPMFile($runfile, $runfile_tmp);
	publishToTripleStore($runfile_tmp);

	// Publish OPM of template
	$tfile = USER_HOME_PATH.ONTOLOGY_DIR.USER_DOMAIN.'/'.$r['templateid'].'.owl';
	$tfile_tmp = sys_get_temp_dir()."/".$uname."_".$r['templateid']."_opm.ttl";
	convertTemplateFileToOPMFile($tfile, $tfile_tmp);
	publishToTripleStore($tfile_tmp); 
	
	return $ttlurl;
	#return $str;
}

function getRequestWorkflow($reqid) {
	$json = file(USER_HOME_PATH.OUTPUT_DIR.$reqid."/$reqid.owl");
	return (sizeof($json) ? json_decode($json[0]) : null);
}

function getRunWorkflow($modx, $uid, $id) {
  	$runObj = $modx->getObject('workflow_runs', array('id'=>$id, 'uid'=>$uid));
	return getRequestWorkflow($runObj->get('requestid'));
}

/*
function getUnstartedRunsForUser($modx, $uid) {
	return $modx->getCollection('workflow_runs', array('uid'=>$uid, 'start_time'=>NULL));
}

function getUnfinishedRunsForUser($modx, $uid) {
   return mysql_query("SELECT * FROM wings_runs WHERE uid=$uid AND start_time IS NOT NULL AND end_time IS NULL");
}

function getFinishedRunsForUser($modx, $uid) {
   return mysql_query("SELECT * FROM wings_runs WHERE uid=$uid AND end_time IS NOT NULL");
}

function getAllUnstartedRuns($modx) {
	return $modx->getCollection('workflow_runs', array('start_time'=>NULL));
}

function getAllUnfinishedRuns($modx) {
   return mysql_query("SELECT * FROM wings_runs WHERE start_time IS NOT NULL AND end_time IS NULL");
}
   
function getAllFinishedRuns($modx) {
   return mysql_query("SELECT * FROM wings_runs WHERE end_time IS NOT NULL");
}
*/


?>
