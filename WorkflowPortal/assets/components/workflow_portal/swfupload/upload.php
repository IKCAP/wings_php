<?
// Get Session cookie
if(!isset($_COOKIE['PHPSESSID'])) {
	$_COOKIE['PHPSESSID'] = $_REQUEST['PHPSESSID'];
}

$base_path = dirname(dirname(dirname(dirname(dirname(__FILE__)))))."/";
require_once $base_path . '/config.core.php';
require_once MODX_CORE_PATH . 'config/' . MODX_CONFIG_KEY . '.inc.php';
include_once(MODX_CORE_PATH . 'model/modx/modx.class.php');

// Initialize modx in the "web" context
$modx= new modX('', array());
$modx->initialize("web");

// Get Userid
$uid = $modx->getLoginUserID();
if(!$uid) 
	die("ERROR: Unauthorized user");
define( "UID", $uid );

// Load in Portal Configuration
define('COMPONENT_NAME', 'workflow_portal');
require_once(MODX_CORE_PATH."components/".COMPONENT_NAME."/conf/config.inc.php");

// Load in Required Libraries
require_once(PORTAL_INCLUDES_PATH."zip_operations.inc.php");
require_once(PORTAL_INCLUDES_PATH."recursive_operations.inc.php");
require_once(PORTAL_INCLUDES_PATH."prophelper.inc.php");
require_once(PORTAL_INCLUDES_PATH."factory.inc.php");

/*echo "Posted parameters for upload are:<br>";
print_r($_POST);
print "user id is ".UID."\n";
exit();*/
//die('success: true}');

foreach($_FILES as $varname=>$file) {
	// If this is a Component Upload
	if(array_key_exists('cid', $_REQUEST)) {
		if(SANDBOX) return '';
		$cid = $_REQUEST['cid'];

		$fac = new Factory(USER_HOME_PATH, true);
		$libname = $fac->getPCLibname();
		if(!$libname) return null;

		$codedir = USER_HOME_PATH.CODE_DIR."$libname/";
		$cfolder = $codedir. $cid;

		recursive_unlink($cfolder);
		$toFile = $codedir . $file['name'];
		move_uploaded_file($file['tmp_name'], $toFile);
		new UnzipFolder($toFile, $codedir);
		recursive_chmod($cfolder, 0755);
		//chmod("$cfolder/run", 0755);
		recursive_unlink($codedir . "__MACOSX");
		@unlink($toFile);
		echo $file['name'];
	} 
	// If this is a Domain Upload
	else if(array_key_exists('domainid', $_REQUEST)) {
		if(SANDBOX) return '';
		$domainid = $_REQUEST['domainid'];
		if(!$domainid) return '';
		$zipFile = $domaindir.$file['name'];
		move_uploaded_file($file['tmp_name'], $zipFile);
		installDomain($zipFile, $domainid);
		@unlink($zipFile);
		echo $domainid;
	} 
	// If this is a normal Data Upload
	else {
		$name = preg_replace('/[^a-zA-Z0-9_]/','_',$file['name']);
		$toFile = USER_HOME_PATH.DATA_DIR . $name;
		move_uploaded_file($file['tmp_name'], $toFile);
		echo $name;
	}
}


function installDomain($zipfile, $domain) {
	$domain_path = USER_DOMAINS_TOP_DIR."$domain/";
	// Clean any previous domain
	recursive_unlink($domain_path);
	// Extract the domain
	new UnzipFolder($zipfile, USER_DOMAINS_TOP_DIR);
	// Set execute permission on the codes
	recursive_chmod($domain_path.CODE_DIR, 0755);

	$domain_ontology_path = $domain_path.ONTOLOGY_DIR;
	$domain_data_path = $domain_path.DATA_DIR;
	$domain_code_path = $domain_path.CODE_DIR;
	$domain_logs_path = $domain_path.LOGS_DIR;
	$domain_output_path = $domain_path.OUTPUT_DIR;
	$domain_config_pegasus_path = $domain_path.PEGASUS_DIR;

	$props = new PropertiesHelper($domain_path."wings.properties");
	$props->setLogDir($domain_logs_path);
	$props->setOutputDir($domain_output_path);
	$props->setOntologyDir($domain_ontology_path);
	$props->setProperty("dc.internal.*.data.dir", $domain_data_path);
	$props->setProperty("pc.internal.*.components.dir", $domain_code_path);
	$props->storeWingsProperties($domain_path."wings.properties");
}

function getUploadedCodePath() {
    // Read the wings.properties file
    // Check if output is "xml" type, (i.e. pegasus needs to be invoked)
    // If yes:
    // -    Set the cfolder to <nfs_software_location>/<user_domain>/default/$cid
    // If no:
    // -    Set the colder to <user_code_path>/$cid
}

?>
