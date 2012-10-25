<?php
require_once("uri.inc.php");

Class Link extends URI {
   var $fromNode;
   var $toNode;
   var $fromPort;
   var $toPort;
   var $variable;

   function Link($id, $fromNode=null, $toNode=null, $fromPort=null, $toPort=null, $variable=null) {
      parent::URI($id);
      $this->id=$id;
      $this->fromNode = $fromNode;
      $this->fromPort = $fromPort;
      $this->toNode = $toNode;
      $this->toPort = $toPort;
      $this->variable = $variable;
   }

   function getVariable() {
      return $this->variable;
   }

   function setVariable($var) {
      $this->variable = $var;
   }

   function getOriginNode() {
      return $this->fromNode;
   }

   function getDestinationNode() {
      return $this->toNode;
   }

   function getOriginPort() {
      return $this->fromPort;
   }

   function getDestinationPort() {
      return $this->toPort;
   }

   function isInputLink() {
      return ($this->fromNode===null);
   }

   function isOutputLink() {
      return ($this->toNode===null);
   }

   function isInOutLink() {
      return ($this->fromNode && $this->toNode);
   }
}

?>
