<?php
require_once("uri.inc.php");

Class WNode extends URI {
	var $x;
	var $y;
   var $component;
   var $crule = "W";
   var $prule = "W";
   var $pruleOp = "cross";

   function WNode($id, $component=null) {
      parent::URI($id);
      $this->component = $component;
   }

   function setComponent($component) {
      $this->component = $component;
   }

   function getComponent() {
      return $this->component;
   }

	function setPosition($x, $y) {
		$this->x = $x;
		$this->y = $y;
	}

	function getPosition() {
		return array($this->x, $this->y);
	}
}

?>
