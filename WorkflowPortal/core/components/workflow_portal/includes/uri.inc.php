<?php

Class URI {
   var $id;
   var $ns;
   var $name;

   function URI($id) {
      $this->setID($id);
   }
   function getId() {
      return $this->id;
   }

   function setId($id) {
      $this->id = $id;
      $arr = parse_url($id);
      $this->name = $arr['fragment'];
      $this->ns = preg_replace('/'.$this->name.'$/','',$this->id);
   }

   function getName() {
      return $this->name;
   }

   function getNamespace() {
      return $this->ns;
   }
}

?>
