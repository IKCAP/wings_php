<?php

function get_redirect_url( $url ) { 
	$options = array( 
		CURLOPT_RETURNTRANSFER => true,    // return web page 
		CURLOPT_HEADER         => true,    // return headers 
		CURLOPT_FOLLOWLOCATION => true     // follow redirects 
	); 

	$ch      = curl_init( $url ); 
	curl_setopt_array( $ch, $options ); 
	$content = curl_exec( $ch ); 
	$header  = curl_getinfo( $ch ); 
	curl_close( $ch ); 
	//print_r($header);
	return $header["url"]; 
}

function upload_files_to_server($uid, $runid, $files) {
	$URL = OPMW_FILE_SERVER."upload.php";

	$URL = get_redirect_url($URL);
	$post = array(
		'passcode'=>'_uploadopm_',
		'uid'=>$uid,
		'runid'=>$runid
	);

	foreach($files as $file) {
		if(preg_match('/.*\/(.+)$/', $file, $m)) {
			$post[$m[1]] = "@".$file;
		}
	}

	$ch = curl_init();
	curl_setopt($ch, CURLOPT_HEADER, 0);
	curl_setopt($ch, CURLOPT_VERBOSE, 0);
	curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
	//curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
	//curl_setopt($ch, CURLOPT_POSTREDIR, true);
	curl_setopt($ch, CURLOPT_USERAGENT, "Mozilla/4.0 (compatible;)");
	curl_setopt($ch, CURLOPT_URL, $URL);
	curl_setopt($ch, CURLOPT_POST, true);

	curl_setopt($ch, CURLOPT_POSTFIELDS, $post); 
	$response = curl_exec($ch);
}


/*$tmpfolder = "/tmp/wingsrun_2_77";
// Get all files in $tmpfolder
$upfiles = array();
$dh = opendir($tmpfolder);
while(($file = readdir($dh)) !== false) {
    if($file != "." && $file != "..") {
        array_push($upfiles, "$tmpfolder/$file");
    }
}
$uid = 2;
$runid = 77;
//$upfiles = array("/tmp/run_71_opm.ttl");
upload_files_to_server($uid, $runid, $upfiles);
*/

?>
