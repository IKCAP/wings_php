<?php

Class Seed {
   var $id;
   var $template;
   var $base_uri;

   // Create an empty seed from the passed in template
   function Seed($seedid, $base_uri, $template) {
      $this->id = $seedid;
      $this->api = new KBAPI();
      $this->base_uri = $base_uri;
      $this->api->setBaseURI($base_uri);
      $this->createTemplateImport($template);
   }

   function createTemplateImport($template) {
      $this->template = $template;
      $nsmap = $this->template->api->getNamespacePrefixMap();
      $t_base_uri = $this->template->api->getBaseURI();
      $nsmap[$t_base_uri] = '';
      $this->api->addNamespacePrefixMap($nsmap);

      $t_base_uri = preg_replace('/#$/','',$t_base_uri);
      $this->api->addStatement($this->base_uri, RDF_NAMESPACE_URI."type", OWL_URI."Ontology");
      $this->api->addStatement($this->base_uri, OWL_URI."imports", $t_base_uri);
   }

   function addConstraints($constraints) {
      $this->api->addStatements($constraints);
   }

   /*function Seed($seedid) {
      // TODO: Load the Seed
      // load the $template
   }*/

   function saveAs($file) {
      $this->api->saveAs($file);
   }
}

?>
