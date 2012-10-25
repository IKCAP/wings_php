<?php
require_once("uri.inc.php");

Class Component extends URI {
   var $type;
	var $binding;
   var $isConcrete;

   function Component($id, $type=null) {
      parent::URI($id);
      $this->type = $type;
   }

   function setConcrete($isConcrete) {
		$this->isConcrete = $isConcrete;
	}

	function isConcrete() {
		return $this->isConcrete;
	}

	function setBinding($binding) {
		$this->binding = $binding;
	}

	function getBinding() {
		return $this->binding;
	}

   function setType($type) {
      $this->type = $type;
   }

   function getType() {
      return $this->type;
   }
}

?>
