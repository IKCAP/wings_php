<?php
/*
 * Beamer Class
 */

Class Beamer {
	var $paraphrases_file;
	var $mappings_file;
	var $paraphrasesJSON;
	var $mappingsJSON;

	function Beamer($paraphrases_file, $mappings_file) {
		$this->paraphrases_file = $paraphrases_file;
		$this->mappings_file = $mappings_file;
		$this->paraphrasesJSON = preg_replace('/\s+/', ' ', implode(file($paraphrases_file), ''));
		$this->mappingsJSON = preg_replace('/\s+/', ' ', implode(file($mappings_file), ''));
	}
	
	function getParaphrasesJSON() {
		return $this->paraphrasesJSON ? $this->paraphrasesJSON : "[]";
	}

	function getMappingsJSON() {
		return $this->mappingsJSON ? $this->mappingsJSON : "[]";
	}
}

