<?php
require_once("uri.inc.php");

Class Port extends URI {
   var $role;

   function Port($id) {
      parent::URI($id);
   }

   function setRole($role) {
      $this->role = $role;
   }

   function getRole() {
      return $this->role;
   }
}

?>
