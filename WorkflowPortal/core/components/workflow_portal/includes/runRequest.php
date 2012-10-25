<?php

if($argc != 3) exit(1);

$uid = base64_decode($argv[1]);
$runid = $argv[2];

if(!$runid || !$uid) exit(1);

define("UID", $uid);

$_REQUEST['ctx'] = 'web';

$basePath = dirname( dirname( dirname( dirname( dirname( __FILE__ ) ) ) ) );
require_once $basePath . '/config.core.php';
require_once MODX_CORE_PATH . 'config/' . MODX_CONFIG_KEY . '.inc.php';
require_once MODX_CONNECTORS_PATH . 'index.php';

// Load in Component Configuration
define("UID", $uid);
define('COMPONENT_NAME', 'workflow_portal');
require_once(MODX_CORE_PATH."components/".COMPONENT_NAME."/conf/config.inc.php");

// Load in wings properties
require_once(PORTAL_INCLUDES_PATH."prophelper.inc.php");

$prop_file = USER_HOME_PATH."wings.properties";
$WINGS_PROPS = new PropertiesHelper($prop_file);
if(!$WINGS_PROPS)
	return $modx->lexicon('workflow_portal.no_wings_properties');

// Load the Run Database
require_once(PORTAL_INCLUDES_PATH."rundb.inc.php");

require_once(PORTAL_INCLUDES_PATH."recursive_operations.inc.php");

$dbh = connectRunDB($modx);

$run = getRunDetailsForUser($modx, $uid, $runid);

if($run['results'] && !$run['rows']['start_time']) {
	registerRunStart($modx, $uid, $runid);
	$log = runSeed($modx, $run['rows']['requestid'], $uid, $runid);
	setRunLog($modx, $uid, $runid, $log);
	registerRunEnd($modx, $uid, $runid);
}


function runSeed($modx, $requestid, $uid, $runid) {
	global $CONFIG;
	setEnvironmentVariables();

	ob_start();
	
	echo "Executing Workflow..\n";
	echo "--------------------\n";
	$wings_log = ob_get_contents();
	ob_end_clean();

	$tmpdir = sys_get_temp_dir()."/".uniqid();
	$olddir = getcwd();
	mkdir($tmpdir);
	chdir($tmpdir);

	$file = USER_HOME_PATH.OUTPUT_DIR.$requestid."/$requestid.sh";

	chmod($file, 0755);
	$fh = popen("$file 2>&1", "r");

	$numjobs_total = 0;
	$numjobs_executed = 0;
	$running_job = "";
	$status = "ONGOING";

	$log = "---------------- Result ------------------\n";
	$LOGLINES = 10;
	$numlines = 0;
	while($line = fgets($fh)) {
		$numlines++;
		if(preg_match('/^\*\*Total Jobs: (\d+)/', $line, $m)) {
			$numjobs_total = $m[1];
		}
		else if (preg_match('/^\*\*Job (\d+):\s+(\S+?)\s+/', $line, $m)) {
			$numjobs_executed = $m[1];
			$running_job = $m[2];
		}
		else if (preg_match('/^\*\*Success/', $line)) {
			$running_job = "";
			$status = "SUCCESS";
		}
		else if (preg_match('/^\*\*Failure/', $line)) {
			//$running_job = "";
			$status = "FAILURE";
		}

		$log .= $line;
		$MAXLOG = 40000;
		if(strlen($log) > $MAXLOG) {
			$log = "... truncated ...\n".substr($log, strlen($log)-$MAXLOG, $MAXLOG);
		}

		if($status == "FAILURE") {
			$log .= "Errors in Running Workflow... ABORTING !!\n";
			$log .= "-----------------------------------------\n";
		}
		else if ($status == "SUCCESS") {
			$log .= "Workflow Execution Complete..\n";
			$log .= "-----------------------------\n";
		}
		if($numlines > $LOGLINES) {
			$numlines = 0;
			setRunLog($modx, $uid, $runid, $wings_log.$log);
		}
		setRunStatus($modx, $uid, $runid, $status);
		setRunProgress($modx, $uid, $runid, $numjobs_total, $numjobs_executed, $running_job);
		if($status != "ONGOING") {
			chdir($olddir);
			recursive_unlink($tmpdir);
			return $wings_log.$log;
		}
	}
}

function setEnvironmentVariables() {
	global $CONFIG;
	putenv("JAVA_HOME=".$CONFIG['system']['java_home']);
}

?>
