<?php
require_once("uri.inc.php");

Class Variable extends URI {
   var $type;
   var $roles;
   var $binding;
   var $dim;
	var $autofill;
   var $x;
   var $y;

   function Variable($id, $type="DATA") {
      parent::URI($id);
      $this->type = $type;
      $this->roles = array();
      $this->autofill = false;
   }

   function getId() {
      return $this->id;
   }

   function getType() {
      return $this->type;
   }

   function setType($type) {
      $this->type = $type;
   }

   function getBinding() {
      return $this->binding;
   }

   function setBinding($binding) {
      $this->binding = $binding;
   }

   function isData() {
      return ($this->type === "DATA");
   }

   function isParam() {
      return ($this->type === "PARAM");
   }

   function getRoles() {
      return $this->roles;
   }

   function addRole($role) {
      array_push($this->roles, $role);
   }

	function setPosition($x, $y) {
		$this->x = $x;
		$this->y = $y;
	}

	function getPosition() {
		return array($this->x, $this->y);
	}

	function setAutoFill($autofill) {
		$this->autofill = $autofill;
	}

	function getAutoFill() {
		return $this->autofill;
	}
}

?>
