<?php

function getDotLayout($dotstr, $dot) {
	$tmpdir = sys_get_temp_dir();
	$dotstr = str_replace("\\", '', $dotstr);
	$tmpfile = $tmpdir."/".uniqid().".dot";
	$fh = fopen($tmpfile,"w");
	fwrite($fh, $dotstr);
	fclose($fh);

	// exec("export HOME=$tmpdir; $dot -Tgv $tmpfile 2>&1", $output, $ret);
	exec("export HOME=$tmpdir; $dot $tmpfile 2>&1", $output, $ret);
	if($ret) {
		echo implode($output,"\n");
		return;
	}

	$w; $h;
	$curline = "";
	$idpos = array();
	foreach($output as $line) {
		if($line[strlen($line)-1] == '\\') {
			$curline .= substr($line, 0, strlen($line)-2);
			continue;
		}
		if($curline) $curline .= $line;
		else $curline = $line;
		if(preg_match('/^\s+(.+?)\s.+pos="(\d+),(\d+)"/', $curline, $m)) {
			$idpos[$m[1]] = array($m[2],$m[3]);
		}
		$curline = "";
	}

	$maxY = 0;
	foreach($idpos as $id=>$pos) 
		if($maxY < $pos[1]) $maxY = $pos[1];

	unlink($tmpfile);

	$retval = "";
	foreach($idpos as $id=>$pos) {
		$retval .= $id.":".$pos[0].",".(40+$maxY-$pos[1])."\n";	
	}
	return $retval;
}

?>
