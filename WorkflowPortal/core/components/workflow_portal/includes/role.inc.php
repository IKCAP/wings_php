<?php
require_once("uri.inc.php");

Class Role extends URI {
   var $dimensionality;
   var $variable;

   function Role($id) {
      parent::URI($id);
      $this->dimensionality=0;
   }

   function setDimensionality($dimensionality) {
      $this->dimensionality = $dimensionality;
   }

   function getDimensionality() {
      return $this->dimensionality;
   }

   function setVariableMapping($variable) {
      $this->variable = $variable;
   }

   function getVariableMapping() {
      return $this->variable;
   }
}

?>
