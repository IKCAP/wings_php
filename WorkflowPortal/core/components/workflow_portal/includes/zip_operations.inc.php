<?php
class ZipFolder {
    protected $zip;
    protected $root;
    protected $ignored_names;
    
    function __construct($file, $folder, $zipfolder, $ignored=null) {
        $this->zip = new ZipArchive();
        $this->ignored_names = is_array($ignored) ? $ignored : $ignored ? array($ignored) : array();
        if ($this->zip->open($file, ZIPARCHIVE::CREATE)!==TRUE) {
            throw new Exception("cannot open <$file>\n");
        }
        $folder = substr($folder, -1) == '/' ? substr($folder, 0, strlen($folder)-1) : $folder;
        if(strstr($folder, '/')) {
            $this->root = substr($folder, 0, strrpos($folder, '/')+1);
            $folder = substr($folder, strrpos($folder, '/')+1);
        }
        $this->zip($folder, $zipfolder);
        $this->zip->close();
    }
    
    function zip($folder, $zipfolder, $parent=null) {
        $full_path = $this->root.$parent.$folder;
        $zip_path = $parent.($zipfolder ? $zipfolder : $folder);
        $this->zip->addEmptyDir($zip_path);
        $dir = new DirectoryIterator($full_path);
        foreach($dir as $file) {
            if(!$file->isDot()) {
                $filename = $file->getFilename();
                if(!in_array($filename, $this->ignored_names)) {
                    if($file->isDir()) {
                        $this->zip($filename, null, $zip_path.'/');
                    }
                    else {
                        $this->zip->addFile($full_path.'/'.$filename, $zip_path.'/'.$filename);
                    }
                }
            }
        }
    }
}

class UnzipFolder {
    protected $zip;
    protected $root;
    protected $ignored_names;
    
    function __construct($file, $folder) {
        $this->zip = new ZipArchive();
        if ($this->zip->open($file)!==TRUE) {
            throw new Exception("cannot open <$file>\n");
        }
        $this->unzip($folder);
        $this->zip->close();
    }
    function unzip($folder) {
        $this->zip->extractTo($folder);
    }
}
		
?>
