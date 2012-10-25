<?php

require_once("recursive_operations.inc.php");
require_once("prophelper.inc.php");
		
function createNewUserSpace($domain="", $user_domain="", $domain_comp_lib="library", $user_comp_lib="library") {
	if(!defined('USER_DOMAINS_TOP_DIR') || !defined('DOMAINS_TOP_DIR') || !defined('DOMAIN')
		|| !defined('USER_DOMAIN') || !defined('USER_DOMAIN_FILE'))
		return false;

	if(!$domain) $domain=DOMAIN;
	if(!$user_domain) $user_domain=USER_DOMAIN;

	$domain_path = DOMAINS_TOP_DIR."$domain/";
	$domain_ontology_path = $domain_path.ONTOLOGY_DIR;
	$domain_data_path = $domain_path.DATA_DIR;
	$domain_code_path = $domain_path.CODE_DIR.$domain_comp_lib."/";
	if(!file_exists($domain_code_path)) 
		$domain_code_path = $domain_path.CODE_DIR;
	$domain_logs_path = $domain_path.LOGS_DIR;
	$domain_output_path = $domain_path.OUTPUT_DIR;
	$domain_config_pegasus_path = $domain_path.PEGASUS_DIR;

	$user_home_path = USER_DOMAINS_TOP_DIR."$user_domain/";
	$user_ontology_path = $user_home_path.ONTOLOGY_DIR;
	$user_data_path = $user_home_path.DATA_DIR;
	$user_code_path = $user_home_path.CODE_DIR.$user_comp_lib."/";
	$user_logs_path = $user_home_path.LOGS_DIR;
	$user_output_path = $user_home_path.OUTPUT_DIR;
	$user_config_pegasus_path = $user_home_path.PEGASUS_DIR;

	// If the domain directory doesn't exist, return
	if(!is_dir($domain_path))
		return false;

	if(!is_dir(USER_DOMAINS_TOP_DIR)) 
		mkdir(USER_DOMAINS_TOP_DIR, 0755, true);

	if(!is_file(USER_DOMAIN_FILE)) {
		$fh = fopen(USER_DOMAIN_FILE, "w"); 
		fwrite($fh, $user_domain);
		fclose($fh);
	}

	// If the user domain directory already exists, return
	if(is_dir($user_home_path))
		return true;

	if($domain_path) {
		// Create user space in USER_HOME if it doesn't already exist
		mkdir($user_home_path, 0755, true);
		if(!is_dir($user_home_path)) return false;

		mkdir($user_logs_path);
		mkdir($user_output_path);

		// Copy over the ontology directory
		recursive_copy($domain_ontology_path, $user_ontology_path);
		// Replace domain name in files if this is a new domain
		if($domain != $user_domain) {
			replaceDomainNameInFiles($user_ontology_path, $domain, $user_domain, "/.owl$/");
			rename($user_ontology_path."$domain", $user_ontology_path."$user_domain");
			rename($user_ontology_path."ac/$domain", $user_ontology_path."ac/$user_domain");
			rename($user_ontology_path."dc/$domain", $user_ontology_path."dc/$user_domain");
		}

		// Copy over the data directory
		recursive_copy($domain_data_path, $user_data_path);

		// Copy over the code directory
		mkdir($user_home_path.CODE_DIR);
		recursive_copy($domain_code_path, $user_code_path);
		if(is_dir($user_code_path."skeleton"))
			rename($user_code_path."skeleton", $user_home_path.CODE_DIR."skeleton");

		// Copy over the pegasus config directory if it exists
		if(is_dir($domain_config_pegasus_path)) {
			recursive_copy($domain_config_pegasus_path, $user_config_pegasus_path);
			if($domain != $user_domain)
				replaceDomainNameInFiles($user_ontology_path, $domain, $user_domain, "/^tc\.data$/");
		}

		// Rewrite some of wings properties and store a custom wings properties file
		$props = new PropertiesHelper($domain_path."wings.properties");
		$props->setLogDir($user_logs_path);
		$props->setOutputDir($user_output_path);
		$props->setOntologyDir($user_ontology_path);
		$props->setProperty("dc.internal.*.data.dir", $user_data_path);
		$props->setProperty("pc.internal.*.components.dir", $user_code_path);
		$props->storeWingsProperties($user_home_path."wings.properties");

		if($domain != $user_domain)
			replaceDomainNameInFiles($user_home_path, $domain, $user_domain, "/^wings\.properties$/");

		return true;
	}
	else {
		return false;
	}
}

function createNewComponentLibrary($domain, $user_domain, $domlib="library", $userlib="library") {
	// Create directory in code/ too !!!!
	// Use that to get the Code dir
	if(!defined('USER_DOMAINS_TOP_DIR') || !defined('DOMAINS_TOP_DIR') || !defined('DOMAIN')
		|| !defined('USER_DOMAIN') || !defined('USER_DOMAIN_FILE') || !$domlib || !$userlib)
		return false;

	if(!$domain) $domain=DOMAIN;
	if(!$user_domain) $user_domain=USER_DOMAIN;

	$domain_path = DOMAINS_TOP_DIR."$domain/";
	$domain_code_path = $domain_path.CODE_DIR."$domlib/";
	if(!file_exists($domain_code_path)) 
		$domain_code_path = $domain_path.CODE_DIR;

	$user_home_path = USER_DOMAINS_TOP_DIR."$user_domain/";
	$user_code_path = $user_home_path.CODE_DIR."$userlib/";

	// Create factories and get PC Dirs
	$props = new PropertiesHelper($domain_path."wings.properties");
	$dompcdir = $domain_path.ONTOLOGY_DIR.$props->getPCPropertyForCurrentDomain("directory")."/".$props->getPCDomain()."/";
	$pcfrom = $dompcdir."$domlib.owl";
	$rulesfrom = $dompcdir."$domlib.rules";

	$props = new PropertiesHelper($user_home_path."wings.properties");
	$userpcdir = $user_home_path.ONTOLOGY_DIR.$props->getPCPropertyForCurrentDomain("directory")."/".$props->getPCDomain()."/";
	$pcto = $userpcdir."$userlib.owl";
	$rulesto = $userpcdir."$userlib.rules";

	// Copy over the component library owl file and edit it
	if(file_exists($pcfrom)) {
		copy($pcfrom, $pcto);
		replaceDomainNameInFiles($userpcdir, $domain, $user_domain, "/$userlib.owl$/");
	}
	else {
		return false;
	}

	// Copy over the component library rules file (or create an empty file)
	if(file_exists($rulesfrom)) {
		copy($rulesfrom, $rulesto);
	}
	else {
		$fh = fopen($rulesto, 'w');
		fclose($fh);
	}

	// Copy over the user codes (or just create an empty directory)
	mkdir($user_home_path.CODE_DIR);
	if(is_dir($domain_code_path)) {
		recursive_copy($domain_code_path, $user_code_path);
	}
	else {
		mkdir($user_code_path);
	}
	return true;
}


function replaceDomainNameInFiles($directory, $from, $to, $filepattern) {
	foreach(new RecursiveIteratorIterator(new RecursiveDirectoryIterator($directory)) as $file)
		if(!$file->isDir() && preg_match($filepattern, $file->getBaseName()))
			file_put_contents($file->getPathName(), str_replace($from, $to, file_get_contents($file->getPathName()))); 
}

?>
