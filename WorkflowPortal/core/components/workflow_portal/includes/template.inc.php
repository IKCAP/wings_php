<?php

require_once("kbapi.inc.php");
require_once("component.inc.php");
require_once("node.inc.php");
require_once("port.inc.php");
require_once("role.inc.php");
require_once("link.inc.php");
require_once("variable.inc.php");

Class Template {
   var $api;
   var $file;

   var $ns;
   var $url;
   var $name;
   var $doc;

   var $nodes;
   var $links;
   var $variables;

   var $nsmap;

   var $iroles;
   var $oroles;

   function Template($file, $createNew, $nsmap, $ns) {
      $this->file = $file;
		if($ns) $this->ns = $ns;
		if($createNew) {
			$this->api = new KBAPI();
			if($nsmap) {
				$this->nsmap = $nsmap;
				$rns = array_flip($nsmap);
				$rns[$nsmap['defaultns']] = '';
				$this->api->addNamespacePrefixMap($rns);
				$this->api->setBaseURI(preg_replace('/#/','',$nsmap['defaultns']));
			}
		}
		else if(file_exists($file)) {
      	$this->api = new KBAPI($file);
      	$this->readTemplate($this->api);
		}
   }

   function initArrays() {
      $this->links = array();
      $this->nodes = array();
      $this->variables = array();
      $this->iroles = array();
      $this->oroles = array();
   }

   function readTemplate($api) {
      $this->initArrays();
      $this->nsmap = $api->getPrefixNamespaceMap();
      $ns = $this->nsmap;

      $tobjs = $api->getInstancesOfClass($ns['wflow']."WorkflowTemplate");
      foreach($tobjs as $tobj) {
         // Get all nodes
         $nodeobjs = $api->getPropertyValues($tobj, $ns['wflow']."hasNode");
         foreach ($nodeobjs as $nodeobj) {
            $compobj = $api->getPropertyValue($nodeobj, $ns['wflow']."hasComponent");
            $position = $api->getPropertyValue($nodeobj, $ns['rdfs']."comment", true);
            if($compobj) {
               $comp = new Component($compobj);
               $compclses = $api->getClassesOfInstance($compobj);
               foreach($compclses as $compcls) {
                  if(strpos($compcls, $ns['wflow']) === 0) continue;
                  $comp->setType($compcls);
               }
					$binding =  $api->getPropertyValue($compobj, $ns['wflow']."hasComponentBinding");
					if($binding)
						$comp->setBinding($binding);

            	$concrete = $api->getPropertyValue($compobj, $ns['wflow']."isConcrete", true);
               if($concrete && $concrete->label=="true") $comp->setConcrete(true);
               else $comp->setConcrete(false);

               $node = new WNode($nodeobj, $comp);
					if(preg_match('/x=([\d\.]+),y=([\d\.]+)/', $position->label, $m)) {
						$node->setPosition($m[1], $m[2]);
					}
            	$cruleobj = $api->getPropertyValue($nodeobj, $ns['wflow']."hasComponentSetCreationRule", true);
            	$pruleobj = $api->getPropertyValue($nodeobj, $ns['wflow']."hasPortSetCreationRule", true);
					if($cruleobj) {
						$cs = $api->getPropertyValue($cruleobj, $ns['wflow']."createComponentSets", true);
						if($cs && $cs->label=="true") $node->crule = "S";
						else $node->crule = "W";
					}
					if($pruleobj) {
						$cs = $api->getPropertyValue($pruleobj, $ns['wflow']."createComponentSets", true);
						if($cs && $cs->label=="true") $node->prule = "S";
						else $node->prule = "W";
						$exprobj = $api->getPropertyValue($pruleobj, $ns['wflow']."createSetsOn", true);
						if($exprobj) { 
							$exprtype = $api->getPropertyValue($exprobj, $ns['rdf']."type");
							if($exprtype == $ns['wflow']."NWise")  $node->pruleOp = "dot";
							else if($exprtype == $ns['wflow']."XProduct")  $node->pruleOp = "cross";
						}
					}
               $this->nodes[$nodeobj] = $node;
            }
         }
         $linkobjs = $api->getPropertyValues($tobj, $ns['wflow']."hasLink");
         foreach ($linkobjs as $linkobj) {
            $varobj = $api->getPropertyValue($linkobj, $ns['wflow']."hasVariable");
            $var = isset($this->variables[$varobj])?$this->variables[$varobj]:null;
            if($varobj && !$var) {
               $vartype = $api->isA($varobj, $ns['wflow']."DataVariable") ? "DATA":"PARAM";
               $var = new Variable($varobj, $vartype);
            	$position = $api->getPropertyValue($varobj, $ns['rdfs']."comment", true);
               if($var->isData()) {
                    $binding =  $api->getPropertyValue($varobj, $ns['wflow']."hasDataBinding");
                    // TODO: Check if this is a list
               }
               else if($var->isParam()) {
                    $binding =  $api->getPropertyValue($varobj, $ns['wflow']."hasParameterValue", true);
                    $autofill =  $api->getPropertyValue($varobj, $ns['wflow']."autoFill", true);
                    if($autofill)
              	        $var->setAutoFill($autofill->label=="true");
               }
					if(preg_match('/x=([\d\.]+),y=([\d\.]+)/', $position->label, $m)) {
						$var->setPosition($m[1], $m[2]);
					}
               $this->nodes[$nodeobj] = $node;
               $var->setBinding($binding);
               $this->variables[$varobj] = $var;
            }

            $fromNodeObj = $api->getPropertyValue($linkobj, $ns['wflow']."hasOriginNode");
            $fromNode = $fromNodeObj?$this->nodes[$fromNodeObj]:null;
            $toNodeObj = $api->getPropertyValue($linkobj, $ns['wflow']."hasDestinationNode");
            $toNode = $toNodeObj?$this->nodes[$toNodeObj]:null;

            $fromPort = null;
            $toPort = null;
            $fromPortObj = $api->getPropertyValue($linkobj, $ns['wflow']."hasOriginPort");
            $toPortObj = $api->getPropertyValue($linkobj, $ns['wflow']."hasDestinationPort");

            if($fromPortObj) {
                $fromPort = new Port($fromPortObj);
                $fromRoleObj = $api->getPropertyValue($fromPortObj, $ns['wflow']."satisfiesRole");
                if($fromRoleObj) {
                    $fromRole = new Role($fromRoleObj);
                    $dimObj = $api->getPropertyValue($fromRoleObj, $ns['wflow']."hasDimensionality", true);
                    if($dimObj && $dimObj->label) $fromRole->setDimensionality($dimObj->label);
                    else $fromRole->setDimensionality(0);
                    $var->addRole($fromRole);
                    $fromPort->setRole($fromRole);
                }
            }
            if($toPortObj) {
                $toPort = new Port($toPortObj);
                $toRoleObj = $api->getPropertyValue($toPortObj, $ns['wflow']."satisfiesRole");
                if($toRoleObj) {
                    $toRole = new Role($toRoleObj);
                    $dimObj = $api->getPropertyValue($toRoleObj, $ns['wflow']."hasDimensionality", true);
                    if($dimObj && $dimObj->label) $toRole->setDimensionality($dimObj->label);
                    else $toRole->setDimensionality(0);
                    $var->addRole($toRole);
                    $toPort->setRole($toRole);
                }
            }
            $link = new Link($linkobj, $fromNode, $toNode, $fromPort, $toPort, $var);
            $this->links[$linkobj] = $link;
         }

         // Get Input/Output roles of the Template
         $iroleobjs = $api->getPropertyValues($tobj, $ns['wflow']."hasInputRole");
         foreach ($iroleobjs as $iroleobj) {
            $irole = new Role($iroleobj);
            $dimObj = $api->getPropertyValue($iroleobj, $ns['wflow']."hasDimensionality", true);
            if($dimObj && $dimObj->label) $irole->setDimensionality($dimObj->label);
            $varObj = $api->getPropertyValue($iroleobj, $ns['wflow']."mapsToVariable");
            if($varObj) $irole->setVariableMapping($varObj);
            array_push($this->iroles, $irole);
         }
         $oroleobjs = $api->getPropertyValues($tobj, $ns['wflow']."hasOutputRole");
         foreach ($oroleobjs as $oroleobj) {
            $orole = new Role($oroleobj);
            $dimObj = $api->getPropertyValue($oroleobj, $ns['wflow']."hasDimensionality", true);
            if($dimObj && $dimObj->label) $orole->setDimensionality($dimObj->label);
            $varObj = $api->getPropertyValue($oroleobj, $ns['wflow']."mapsToVariable");
            if($varObj) $orole->setVariableMapping($varObj);
            array_push($this->oroles, $orole);
         }

         if($ns['defaultns']) $this->ns = $ns['defaultns'];

         // FIXME: Only handling one template at a time (old style templates: no nesting)
         break;
      }

      $metaobjs = $api->getInstancesOfClass($ns['wflow']."Metadata");
      if($metaobjs) {
			$metaobj = $metaobjs[0];
         $doc = $api->getPropertyValue($metaobj, $ns['wflow']."hasDocumentation", true);
         if($doc) $this->doc = $doc->label;

         $contrib = $api->getPropertyValue($metaobj, $ns['wflow']."hasContributor", true);
         if($contrib) $this->contributor = $contrib->label;

         $lastupdate = $api->getPropertyValue($metaobj, $ns['wflow']."lastUpdateTime", true);
         if($lastupdate) $this->lastupdate = $lastupdate->label;

			$tellme = $api->getPropertyValue($metaobj, $ns['wflow']."hasTellMeTree", true);
         if($tellme) $this->tellme = $tellme->label;
      }
   }

	function loadTemplateFromJSON($tid, $json, $props) {
		global $default_prefixes;

		$apins = $this->api->getPrefixNamespaceMap();
		$api = $this->api;

		$ns = array();
		foreach($default_prefixes as $pfx=>$uri) $ns[$pfx] = $uri;
      foreach(get_object_vars($json->nsmap) as $pfx=>$uri) $ns[$pfx] = $uri;
		if(!$ns['wflow']) $ns['wflow'] = $apins['wflow'];
		if(!$ns['defaultns']) $ns['defaultns'] = $this->ns;
		if(!$ns['acdom']) $ns['acdom'] = $apins['acdom'];
		if(!$ns['dcdom']) $ns['dcdom'] = $apins['dcdom'];
		if(!$ns['dc']) $ns['dc'] = $apins['dc'];
		if(!$ns['ac']) $ns['ac'] = $apins['ac'];

      $nodes = $json->nodes ? $json->nodes : array();
      $variables = $json->variables ? $json->variables : array();
      $links = $json->links ? $json->links : array();
      $constraints = $json->constraints ? $json->constraints : array();
      $meta = $json->metadata ? get_object_vars($json->metadata) : array();
      $tellme = $json->tellme;

		$defaultns = $ns['defaultns'];
		$base = $defaultns;
		if(preg_match('/^(.+\/)(.*)(\.owl)#(.*)$/',$defaultns,$m)){
			$defaultns = $m[1].$tid.$m[3].'#'.$m[4];
			$base = $m[1].$tid.$m[3];
			$api->setBase($base);
		}

		$nsPrefix = array();
		foreach($ns as $prefix=>$url) 
			if($prefix != '_empty_') $nsPrefix[$url] = $prefix;
		$nsPrefix[$defaultns] = "";
		$api->addNamespacePrefixMap($nsPrefix);

		$workflowURL = $props->getOntologyURL()."/".$props->getWorkflowOntologyPath();
		$api->addImport($base, $workflowURL);

      $tobj = $api->createInstanceOfClass($defaultns.$tid, $ns['wflow']."WorkflowTemplate");
		$api->setPropertyValue($tobj, $ns['wflow']."hasVersion", "2", $ns['xsd']."int");
		// Get all nodes
		foreach ($nodes as $node) {
			$node = get_object_vars($node);
			$node['component'] = get_object_vars($node['component']);
			$nodeobj = $api->createInstanceOfClass($defaultns.$node['id'], $ns['wflow']."Node");

			$compobj = $api->createInstanceOfClass($defaultns.$node['component']['name'], $ns['acdom'].$node['component']['type']);
			if($node['component']['concrete']) $api->setPropertyValue($compobj, $ns['wflow']."isConcrete", "true", $ns['xsd']."boolean");
			else $api->setPropertyValue($compobj, $ns['wflow']."isConcrete", "false", $ns['xsd']."boolean");

			$api->setPropertyValue($nodeobj, $ns['wflow']."hasComponent", $compobj);
			$api->setPropertyValue($nodeobj, $ns['rdfs']."comment", "x=".$node['x'].",y=".$node['y'], $ns['xsd']."string");

			$cruleobj = $api->createAnonymousInstanceOfClass($ns['wflow']."ComponentSetRule");
			$pruleobj = $api->createAnonymousInstanceOfClass($ns['wflow']."PortSetRule");

			if($node['crule'] == "W") $cs = $api->setPropertyValue($cruleobj, $ns['wflow']."createWorkflowSets", "true", $ns['xsd']."boolean");
			else $cs = $api->setPropertyValue($cruleobj, $ns['wflow']."createComponentSets", "true", $ns['xsd']."boolean");

			if($node['prule'] == "W") $cs = $api->setPropertyValue($pruleobj, $ns['wflow']."createWorkflowSets", "true", $ns['xsd']."boolean");
			else $cs = $api->setPropertyValue($pruleobj, $ns['wflow']."createComponentSets", "true", $ns['xsd']."boolean");

			$exprobj;
			if($node['pruleOp'] == "cross")	$exprobj = $api->createAnonymousInstanceOfClass($ns['wflow']."XProduct");
			else $exprobj = $api->createAnonymousInstanceOfClass($ns['wflow']."NWise");
			
			foreach($node['inputPorts'] as $iport) {
				$iport = get_object_vars($iport);
				$iport['role'] = get_object_vars($iport['role']);
				$portobj = $api->createInstanceOfClass($defaultns.$iport['id'], $ns['wflow']."Port");
				$api->setPropertyValue($nodeobj, $ns['wflow']."hasInputPort", $portobj);
				$api->setPropertyValue($portobj, $ns['wflow']."satisfiesRole", $ns['acdom'].$iport['role']['id']);
				$api->setPropertyValue($ns['acdom'].$iport['role']['id'], $ns['wflow']."hasDimensionality", $iport['role']['dim']?$iport['role']['dim']:0, $ns['xsd']."int");
				$api->setPropertyValue($exprobj, $ns['wflow']."hasExpressionArgument", $portobj);
			}
			foreach($node['outputPorts'] as $oport) {
				$oport = get_object_vars($oport);
				$oport['role'] = get_object_vars($oport['role']);
				$portobj = $api->createInstanceOfClass($defaultns.$oport['id'], $ns['wflow']."Port");
				$api->setPropertyValue($nodeobj, $ns['wflow']."hasInputPort", $portobj);
				$api->setPropertyValue($portobj, $ns['wflow']."satisfiesRole", $ns['acdom'].$oport['role']['id']);
				$api->setPropertyValue($ns['acdom'].$oport['role']['id'], $ns['wflow']."hasDimensionality", $oport['role']['dim']?$oport['role']['dim']:0, $ns['xsd']."int");
				/*$oport = get_object_vars($oport);
				$portobj = $api->createInstanceOfClass($defaultns.$oport['id'], $ns['wflow']."Port");
				$api->setPropertyValue($nodeobj, $ns['wflow']."hasOutputPort", $portobj);
				$api->setPropertyValue($portobj, $ns['wflow']."satisfiesRole", $ns['acdom'].$oport['role']);*/
			}

			$api->setPropertyValue($pruleobj, $ns['wflow']."createSetsOn", $exprobj);

			$api->setPropertyValue($nodeobj, $ns['wflow']."hasComponentSetCreationRule", $cruleobj);
			$api->setPropertyValue($nodeobj, $ns['wflow']."hasPortSetCreationRule", $pruleobj);

			$api->setPropertyValue($tobj, $ns['wflow']."hasNode", $nodeobj);
		}
		$varDims = array();
		foreach ($variables as $var) {
			$var = get_object_vars($var);
			$type = "DataVariable";
			if($var['type'] == "PARAM") $type = "ParameterVariable";
			$varobj = $api->createInstanceOfClass($defaultns.$var['id'], $ns['wflow'].$type);
			$api->setPropertyValue($varobj, $ns['rdfs']."comment", "x=".$var['x'].",y=".$var['y'], $ns['xsd']."string");
			if($var['autofill'])
				$api->setPropertyValue($varobj, $ns['wflow']."autoFill", "true", $ns['xsd']."boolean");
			$varDims[$var['id']] = $var['dim'];
			// Not checking bindings here: Handling variable binding via constraints for now
		}
		foreach ($links as $link) {
			$link = get_object_vars($link);
			$type = "InOutLink";
			if(!$link['fromNode']) $type = "InputLink";
			else if(!$link['toNode']) $type = "OutputLink";
			$linkobj = $api->createInstanceOfClass($defaultns.$link['id'], $ns['wflow'].$type);
			$api->setPropertyValue($linkobj, $ns['wflow']."hasVariable", $defaultns.$link['variable']);
			if($link['fromNode']) {
				$api->setPropertyValue($linkobj, $ns['wflow']."hasOriginNode", $defaultns.$link['fromNode']);
				$api->setPropertyValue($linkobj, $ns['wflow']."hasOriginPort", $defaultns.$link['fromPort']);
			}
			if($link['toNode']) {
				$api->setPropertyValue($linkobj, $ns['wflow']."hasDestinationNode", $defaultns.$link['toNode']);
				$api->setPropertyValue($linkobj, $ns['wflow']."hasDestinationPort", $defaultns.$link['toPort']);
			}
			$api->setPropertyValue($tobj, $ns['wflow']."hasLink", $linkobj);

			if($type != "InOutLink") {
				// Set the input Role
				$var = $link['variable'];
				$roleid = $defaultns.$var.($type=="InputLink"?"_irole":"_orole");
				$roleProp = $ns['wflow'].($type=="InputLink"?"hasInputRole":"hasOutputRole");
				$roleobj = $api->createInstanceOfClass($roleid, $ns['wflow']."Role");
				$roleDim = $varDims[$var] ? $varDims[$var]:0;
				$api->setPropertyValue($roleobj, $ns['wflow']."hasDimensionality", $roleDim, $ns['xsd']."int");
				$api->setPropertyValue($roleobj, $ns['wflow']."mapsToVariable", $defaultns.$var);
				$api->setPropertyValue($tobj, $roleProp, $roleobj);
			}
		}

		foreach ($constraints as $cons) {
			$cons = get_object_vars($cons);
			$var = $defaultns.$cons['s'];
			if(preg_match('/(.*?):(.*)/', $cons['p'], $m)) $pred = $ns[$m[1]].$m[2];
			else continue;

			if(preg_match('/"(.*?)"\^\^xsd:(.*)/', $cons['o'], $m)) {
				$api->setPropertyValue($var, $pred, $m[1], $ns['xsd'].$m[2]);
			}
			else if(preg_match('/(.*?):(.*)/', $cons['o'], $m)) {
				$obj = $ns[$m[1]].$m[2];
				$api->setPropertyValue($var, $pred, $obj);
			}
			else {
				$api->setPropertyValue($var, $pred, $defaultns.$cons['o']);
			}
		}

		// Set Documentation and other metadata
		$metaobj = $api->createInstanceOfClass($base, $ns['wflow']."Metadata");
		$api->setPropertyValue($metaobj, $ns['wflow']."hasDocumentation", $meta['documentation'], $ns['xsd']."string");
		$api->setPropertyValue($metaobj, $ns['wflow']."hasContributor", UNAME, $ns['xsd']."string");
		$api->setPropertyValue($metaobj, $ns['wflow']."lastUpdateTime", date("c", time()), $ns['xsd']."dateTime");

		if(!$tellme) $tellme = $meta['tellme'];
		if($tellme)
			$api->setPropertyValue($metaobj, $ns['wflow']."hasTellMeTree", $tellme, $ns['xsd']."string");

		// TODO: Handle Template Input/Output Roles & dimensionality

		$this->readTemplate($api);
		return "OK";
	}

	function getJSON() {
		$iroles = $this->getInputRoles();
		$rns = array_flip($this->nsmap);
		$varDims = array();
		foreach($iroles as $irole) {
			$varobj = $irole->getVariableMapping();
			$varDims[$varobj] = $irole->getDimensionality();
		}
		$nodes = array();
		$links = array();
		$variables = array();
		foreach($this->getNodes() as $node) {
			$n = clone $node;
			$ctype = new Resource($node->component->type);
			$n->component = array(
				"name"=>$node->component->name,
				"ex"=>$node->component->isConcrete,
				"binding"=>$node->component->binding,
				"type"=>$ctype->getLocalName(),
			);
			$n->x = $node->x;
			$n->y = $node->y;
			$n->id = $node->name;
			unset($n->ns);
			unset($n->name);
			array_push($nodes, $n);
		}
		foreach($this->getVariables() as $v) {
			$var = clone $v;
			$var->id = $var->name;
			if(!$var->binding) unset($var->binding);
			unset($var->roles);
			unset($var->ns);
			unset($var->name);
			unset($var->dim);
			if($varDims[$v->id]) 
				$var->dim = $varDims[$v->id];
			array_push($variables, $var);
		}
		foreach($this->getLinks() as $link) {
			$l = array();
			$l["fromNode"] = $link->fromNode->name;
			$l["toNode"] = $link->toNode->name;
			$l["variable"] = $link->variable->name;
			if($link->fromPort) {
				$l["fromPort"] = array(
					"id" => $link->fromPort->name,
					"role" => array(
						"id"=>$link->fromPort->role->name,
						"dim"=>$link->fromPort->role->getDimensionality(),
					)
				);
			}
			if($link->toPort) {
				$l["toPort"] = array(
					"id" => $link->toPort->name,
					"role" => array(
						"id"=>$link->toPort->role->name,
						"dim"=>$link->toPort->role->getDimensionality(),
					)
				);
			}
			array_push($links, $l);
		}

		$nsmap = $this->nsmap;
      $rns = array_flip($nsmap);
		$rns[$this->getNamespace()]='';
		$constraints = $this->getAllVariableConstraints();
		$cons = array();
		foreach ($constraints as $con) {
			array_push($cons, 
				array("s"=>$this->getPrefixedTerm($con->subj,$rns),
						"p"=>$this->getPrefixedTerm($con->pred,$rns),
						"o"=>$this->getPrefixedTerm($con->obj,$rns)));
		}
		$json = json_encode(array(
				"nsmap"=>$nsmap, 
				"nodes"=>$nodes, 
				"variables"=>$variables, 
				"links"=>$links, 
				"constraints"=>$cons, 
				"metadata"=>array(
					"documentation"=>$this->doc,
					"lastupdate"=>$this->lastupdate,
					"contributor"=>$this->contributor,
					"tellme"=>$this->tellme
				)
		));
		return preg_replace('/\[\[/','[ [',$json); // MODX treats [[ in a special way, so we have to avoid that
	}

	private function getPrefixedTerm($resource, $nsmap) {
		if(isset($resource->uri)) {
			$ns = $resource->getNamespace();
			if(isset($nsmap[$ns]))
			return ($nsmap[$ns]?$nsmap[$ns].':':'').$resource->getLocalName();
			else
			return $resource->uri;
		}
		else {
			$str = '"'.$resource->label.'"';
			if($resource->dtype) {
				$str .= '^^'.preg_replace('/.*#(.*)$/',"xsd:$1",$resource->dtype);
			}
			return $str;
		}
	}


	function save() {
		$this->api->saveAs($this->file);
	}

   function getDocumentation() {
      return $this->doc;
   }

   function getLink($id) {
      return isset($this->links[$id])?$this->links[$id]:null;
   }
   function getNode($id) {
      return isset($this->nodes[$id])?$this->nodes[$id]:null;
   }
   function getVariable($id) {
      return isset($this->variables[$id])?$this->variables[$id]:null;
   }

   function getLinks() {
      return array_values($this->links);
   }
   function getInputLinks() {
      $ret = array();
      foreach($this->links as $id=>$obj) 
         if($obj->isInputLink()) array_push($ret, $obj);
      return $ret;
   }
   function getOutputLinks() {
      $ret = array();
      foreach($this->links as $id=>$obj) 
         if($obj->isOutputLink()) array_push($ret, $obj);
      return $ret;
   }
   function getInOutLinks() {
      $ret = array();
      foreach($this->links as $id=>$obj) 
         if($obj->isInOutLink()) array_push($ret, $obj);
      return $ret;
   }
   function getNodeInputLinks($node) {
      $ret = array();
      foreach($this->links as $id=>$obj) 
         if($obj->getDestinationNode() && $obj->getDestinationNode()->getId()==$node->getId()) 
            array_push($ret, $obj);
      return $ret;
   }
   function getNodeOutputLinks($node) {
      $ret = array();
      foreach($this->links as $id=>$obj) 
         if($obj->getOriginNode() && $obj->getOriginNode()->getId()==$node->getId()) 
            array_push($ret, $obj);
      return $ret;
   }
   function getVariableLinks($variable) {
      $ret = array();
      foreach($this->links as $id=>$obj) 
         if($obj->getVariable() && $obj->getVariable()->getId()==$variable->getId()) 
            array_push($ret, $obj);
      return $ret;
   }
   function getNodeConnectionLinks($fromNode, $toNode) {
      $ret = array();
      foreach($this->links as $id=>$obj) 
         if( ($obj->getDestinationNode() && $obj->getDestinationNode()->getId()==$toNode->getId()) &&
             ($obj->getOriginNode() && $obj->getOriginNode()->getId()==$fromNode->getId()) ) 
            array_push($ret, $obj);
      return $ret;
   }

   function getNodePortConnectionLinks($fromNode, $toNode, $fromPort, $toPort) {
      $ret = array();
      foreach($this->links as $id=>$obj) 
         if( ($obj->getDestinationNode() && $obj->getDestinationNode()->getId()==$toNode->getId()) &&
             ($obj->getOriginNode() && $obj->getOriginNode()->getId()==$fromNode->getId()) &&
             ($obj->getDestionationPort() == $toPort) &&
             ($obj->getOriginPort() == $fromPort))
            array_push($ret, $obj);
      return $ret;
   }

   function getNodes() {
      return array_values($this->nodes);
   }

   function getInputRoles() {
      return $this->iroles;
   }

   function getOutputRoles() {
      return $this->oroles;
   }

   function getComponentNodes($comp) {
   }

   function getNamespace() {
      return $this->ns;
   }
   function getAPI() {
      return $this->api;
   }

   function getVariables() {
      return array_values($this->variables);
   }
   function getInputVariables() {
      return $this->getLinkVariables($this->getInputLinks());
   }
   function getOutputVariables() {
      return $this->getLinkVariables($this->getOutputLinks());
   }
   function getInOutVariables() {
      return $this->getLinkVariables($this->getInOutLinks());
   }
   function getNodeInputVariables($node) {
      return $this->getLinkVariables($this->getNodeInputLinks($node));
   }
   function getNodeOutputVariables($node) {
      return $this->getLinkVariables($this->getNodeOutputLinks($node));
   }

   function getLinkVariables($links) {
      $ret = array();
      foreach($links as $link) {
         if($link->getVariable() && !in_array($link->getVariable(),$ret)) 
            array_push($ret, $link->getVariable());
      }
      return $ret;
   }

   function getName() {
   }

   function getAllVariableConstraints() {
      $constraints = array();
      $blacklistns = array($this->nsmap['wflow']);
      $whiteListProps = array(
			$this->nsmap['wflow']."hasParameterValue", 
			$this->nsmap['wflow']."hasDataBinding",
			$this->nsmap['wflow']."hasSameDataAs",
			$this->nsmap['wflow']."hasDifferentDataFrom"
		);
      $blacklistprop = array($this->nsmap['rdfs'].'comment');
      foreach($this->variables as $varid=>$var) {
         $triples = $this->getVariableConstraints($var);
         foreach($triples as $t) {
				if(!in_array($t->pred->uri, $whiteListProps)) {
            	if(in_array($t->subj->getNamespace(), $blacklistns)) continue;
            	if(in_array($t->pred->getNamespace(), $blacklistns)) continue;
            	if(isset($t->obj->uri) && in_array($t->obj->getNamespace(), $blacklistns)) continue;
            	if(in_array($t->pred->getURI(), $blacklistprop)) continue;
				}
            array_push($constraints, $t);
         }
      }
      return $constraints;
   }

   function getVariableConstraints($variable) {
      return $this->api->getTriples($variable->getId(), null, null);
   }

   function getNSMap() {
      return $this->nsmap;
   }
}

?>
