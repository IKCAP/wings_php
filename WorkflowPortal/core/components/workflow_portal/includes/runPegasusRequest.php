<?php

if($argc != 3) exit(1);

$uid = base64_decode($argv[1]);
$runid = $argv[2];

if(!$runid || !$uid) exit(1);

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
#print_r($run);

if($run['results'] && !$run['rows']['start_time']) {
	registerRunStart($modx, $uid, $runid);
	$log = runSeed($modx, $run['rows']['requestid'], $uid, $runid);
	setRunLog($modx, $uid, $runid, $log);
	registerRunEnd($modx, $uid, $runid);
}

function msg($msg) {
	echo "$msg\n";
	for($i=0; $i<strlen($msg); $i++) echo "-";
	echo "\n";
}

function error($modx, $msg, $uid, $runid, $dir) {
	msg($msg);
	$log = ob_get_contents();
	ob_end_clean();
	setRunStatus($modx,$uid,$runid,'FAILURE');
	//if($dir) system("rm -fr $dir");
	return $log;
}

function runSeed($modx, $requestid, $uid, $runid) {
	global $CONFIG;
	$pool = $CONFIG['grid']['pegasus_pool'];
	setEnvironmentVariables();

	#$pfx = ". \"\$GLOBUS_LOCATION/etc/globus-user-env.sh\"; . \"\$PEGASUS_HOME/setup.sh\"; . \"\$CONDOR_HOME/condor.sh\";";
	$pfx = ". \"\$GLOBUS_LOCATION/etc/globus-user-env.sh\"; \"\$CONDOR_HOME/condor.sh\"; ";
	$pfx .= "export PATH=\$PEGASUS_HOME/bin:\$CONDOR_HOME/bin:\$PATH;";

	ob_start();
	
	msg("Executing Workflows..");

	$rundir = $CONFIG['grid']['pegasus_storage_dir'].USER_DOMAIN."/$runid/";
	$file = USER_HOME_PATH.OUTPUT_DIR.$requestid."/$requestid.dax";

	msg("Pegasus: Planning Workflow");
	exec("$pfx pegasus-plan '-Drun.dir=$rundir' --conf '$rundir/config/properties' --dax '$file' --dir '$rundir/dags' -s $pool -o $pool --nocleanup 2>&1", $output, $ret);
	echo implode("\n",$output)."\n";
	echo "----------------------------\n";
	if($ret) return error($modx, "Pegasus: Error in Planning the Workflow... ABORTING !!",$uid,$runid,$rundir);
	$runcmd = '';
	foreach($output as $line) {
		if(preg_match('/^pegasus-run/',$line)) {
			$runcmd = $line; break;
		}
	}
	if(!$runcmd) return error($modx, "Pegasus: Error in Planning the Workflow... ABORTING !!",$uid,$runid,$rundir);

	$output = array();
	msg("Pegasus: Running Workflow");
	exec("$pfx $runcmd 2>&1", $output, $ret);
	echo implode("\n",$output)."\n";
	echo "----------------------------\n";
	//if($ret) return error($modx, "Pegasus: Error in Running the Workflow... ABORTING !!",$uid,$runid,$rundir);
	$run_status_dir = '';
	foreach($output as $line) {
		//if(preg_match('/^pegasus-status ([^-].+)$/',$line, $res)) 
		if(preg_match('/^cd (.+)$/',$line, $res)) {
			$run_status_dir = $res[1]; break;
		}
	}
	if(!$run_status_dir) return error($modx, "Pegasus: Error in Running the Workflow... ABORTING !!",$uid,$runid,$rundir);

	msg("Monitoring $run_status_dir");

	$log = ob_get_contents();
	setRunLog($modx, $uid, $runid, $log);
	
	$numdid = -1;
	$osleeptime = 5;
	
	$sleeptime = $osleeptime;
	while(1) {
		$done = false;
		$success = false;
		//$tmp = `grep -i -B1 "All Jobs completed" $run_status_dir/*.dagman.out`;
		$tmp = `grep -B2 -A1 '===' '$run_status_dir'/*.dagman.out | tail -4`;
		if($tmp) {
			$arr = preg_split('/\n/',$tmp);
			if(sizeof($arr) > 0) {
				$tot = preg_split('/\s+/',$arr[0]);
				$data = preg_split('/\s+/',$arr[3]);
				$numtotal = $tot[3];
				$numdone = $data[2];
				$numfailed = $data[8];
				$jobs = getRunningJobs($run_status_dir."/jobstate.log");
				$jobstr = "";
				foreach($jobs as $job) {
					if($jobstr) $jobstr .= ",";
					$jobstr .= $job;
				}
				if(file_exists($run_status_dir."/tailstatd.done")) $done = true;
				if(file_exists($run_status_dir."/monitord.done")) $done = true;
				if($numdone > 0 && $numfailed==0) $success = true;
				
				// Set Run Progress
				setRunProgress($modx, $uid, $runid, $numtotal, $numdone + count($jobs), $jobstr);
				
				if($numdid != $numdone) $sleeptime = $osleeptime;
				else $sleeptime = 2*$sleeptime;
				
				if($sleeptime > 120) $sleeptime = 120; // Max sleep for 2 minutes
				
				$numdid = $numdone;

                printJobRunLogs($run_status_dir);
        	    $log = ob_get_contents();
        		setRunLog($modx, $uid, $runid, $log);
			}
		}

		//$success = `grep "finishing, exit with 0" $run_status_dir/tailstatd.log`;
		if($done) {
			if(!$success) {
				// Read the error output from each $job.out.000 files for each job in $jobs (above)
				return error($modx, "Pegasus: Your Workflow failed to finish. Please contact us for details !!",$uid,$runid,$rundir);
			}
			break;
		}
		sleep($sleeptime);
	}

    printJobRunLogs($run_status_dir);

	echo "Workflow Execution Complete..\n";
	echo "-------------------------------\n";
	
	// Get jobs breakdown and log it
	/*$olddir = getcwd();
	chdir($run_status_dir);
	exec("$pfx genstats-breakdown", $output, $ret);
	echo implode("\n",$output)."\n";
	chdir($olddir);*/
	
	$log = ob_get_contents();
	ob_end_clean();
	setRunStatus($modx, $uid, $runid,'SUCCESS');
	
	// Delete the run directory
	recursive_unlink($rundir);
	return $log;
}


function getRunningJobs($file) {
	$curjobs = array();
	$fh = fopen($file, "r");
	while($line = fgets($fh)) {
		$arr = preg_split('/\s+/',$line);
		if($arr[2]=="SUBMIT") {
			$curjobs[$arr[1]]=1;
		}
		if($arr[2]=="POST_SCRIPT_SUCCESS") {
			unset($curjobs[$arr[1]]);
		}
	}
	return array_keys($curjobs);
}

function printJobRunLogs($dir) {
	$done = array();
	$wingsdat = "$dir/wings.dat";
	$jobs = file($wingsdat);
	foreach($jobs as $job) {
		$done[trim($job)] = 1;
	}

	$fh = fopen($wingsdat, "a");
	$dh = opendir($dir);
	while($file = readdir($dh)) {
		if(preg_match('/^(.*Job\d).*\.out\.000$/', $file, $m)) {
			$name = $m[1];
	
			if($done[$name]) continue;
			$done[$name] = 1;
			fwrite($fh, "$name\n");

			print "$name Log:\n-----------------------------\n";
			$fpath = "$dir/$file";
			$datastr = implode("",file($fpath));
			$dataxml = new SimpleXMLElement($datastr);
			foreach ($dataxml->statcall as $statcall) {
				if($statcall['id'] == "stdout" || $statcall['id'] == "stderr") {
					print $statcall->data."\n";
				}
			}
		}
	}
	closedir($dh);
	fclose($fh);
}

function rt($path) {
	return preg_replace('|/$|','',$path);
}

function setEnvironmentVariables() {
	global $CONFIG;
	putenv("JAVA_HOME=".rt($CONFIG['system']['java_home']));
	putenv("WINGS_HOME=".rt($CONFIG['wings']['wings_home']));
	putenv("PEGASUS_HOME=".rt($CONFIG['grid']['pegasus_home']));
	putenv("CONDOR_HOME=".rt($CONFIG['grid']['condor_home']));
	putenv("GLOBUS_LOCATION=".rt($CONFIG['grid']['globus_home']));
}

?>
