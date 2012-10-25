<?php

function recursive_copy($src,$dst) {
	$dir = opendir($src);
	@mkdir($dst);
	while(false !== ( $file = readdir($dir)) ) {
		if (( $file != '.' ) && ( $file != '..' ) && ( $file != '.svn' )) {
			if ( is_dir($src . '/' . $file) ) {
				recursive_copy($src . '/' . $file,$dst . '/' . $file);
			}
			else {
				copy($src . '/' . $file,$dst . '/' . $file);
				chmod($dst . '/' . $file, 0755);
			}
		}
	}
	closedir($dir);
}

function recursive_unlink($str){
	if(!is_dir($str)) return @unlink($str);
	$iterator = new RecursiveDirectoryIterator($str);
	foreach (new RecursiveIteratorIterator($iterator, RecursiveIteratorIterator::CHILD_FIRST) as $file) {
		if ($file->isDir()) {
			@rmdir($file->getPathname());
		} else {
			@unlink($file->getPathname());
		}
	}
	@rmdir($str);
}

function recursive_size($directory) { 
	$size = 0; 
	foreach(new RecursiveIteratorIterator(new RecursiveDirectoryIterator($directory)) as $file){ 
		$size+=$file->getSize(); 
	}
	return $size; 
}

function recursive_chmod($directory, $mode) { 
	foreach(new RecursiveIteratorIterator(new RecursiveDirectoryIterator($directory)) as $file){ 
		chmod($file->getPathname(), $mode);
	}
}
