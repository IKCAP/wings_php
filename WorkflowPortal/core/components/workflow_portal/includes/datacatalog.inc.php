<?php

Class DataCatalog {
   var $ontapi;
   var $libapi;
   var $nsmap;
   var $ontfile;
   var $libfile;
   var $datadir;
	var $dcThing;

   function DataCatalog($ontfile, $libfile, $dcontfile, $datadir, $props) {
      $this->ontfile = $ontfile;
      $this->libfile = $libfile;
      $this->datadir = $datadir;

      $this->ontapi = new KBAPI($ontfile);
      //$this->ontapi->loadFile($dcontfile);
      $this->libapi = new KBAPI($libfile);

      $this->nsmap = $this->ontapi->getPrefixNamespaceMap();
      $this->nsmap = array_merge($this->nsmap, $this->libapi->getPrefixNamespaceMap());
      $this->nsmap = array_merge($this->nsmap, $props->getDCPrefixNSMap());
		unset($this->nsmap['defaultns']);
		$this->dcThing = $this->nsmap['dc']."DataObject";
		$this->addDCPrefixesToAPI();
   }

	function addDCPrefixesToAPI() {
		$rns = array_flip($this->nsmap);
		$this->ontapi->addNamespacePrefixMap($rns);
		$this->libapi->addNamespacePrefixMap($rns);
	}

   function getPrefixedDataClassesAndInstances($clsid='') {
      $dc = $this->nsmap['dc'];
      $rns = array_flip($this->nsmap);
      if(!$clsid) $clsid = $dc."DataObject";

      $clsinstances = array();
      $vals = $this->libapi->getTriples(null, RDF_NAMESPACE_URI.RDF_TYPE, $clsid);
      $possibleValues = array();
      foreach($vals as $val) {
         array_push($possibleValues, $this->getPrefixedTerm($val->subj, $rns));
      }
      $clsinstances[$this->getPrefixedTerm($cls, $rns)] = $possibleValues;

      $triples = $this->ontapi->getTriples(null, RDF_SCHEMA_URI."subClassOf", $clsid);
      foreach($triples as $t) {
         $clsid = $t->subj->uri;
         $clsinstances = array_merge($clsinstances, $this->getPrefixedDataClassesAndInstances($clsid));
      }
      return $clsinstances;
   }

   function getDataHierarchy($cls) {
      if(!$cls) $cls = new Resource($this->dcThing);

      $clsinstances = array();
      $clsinstances['id'] = $cls->getLocalName();
      $clsinstances['type'] = 'class';

      $vals = $this->libapi->getTriples(null, RDF_NAMESPACE_URI.RDF_TYPE, $cls->uri);
      $possibleValues = array();
      foreach($vals as $val)
         array_push($possibleValues, array("id"=>$val->subj->getLocalName()));

		if(sizeof($possibleValues)) 
      	$clsinstances['children'] = $possibleValues;
     	else $clsinstances['children'] = array();

      $subs = $this->ontapi->getTriples(null, RDF_SCHEMA_URI."subClassOf", $cls->uri);
      foreach($subs as $sub) {
         $subcls = $sub->subj;
         array_push($clsinstances['children'], $this->getDataHierarchy($subcls));
      }
      return $clsinstances;
   }

	function indexSuperTypes($parentIndex, $type, $ptypes) {
      if(!$type) $type = $this->dcThing;
      if(!$ptypes) $ptypes = array($this->dcThing);
		if(!$parentIndex) $parentIndex = array();

      $subtypes = $this->ontapi->getTriples(null, RDF_SCHEMA_URI."subClassOf", $type);
      foreach($subtypes as $subtype) {
         $stype = $subtype->subj->uri;
			$nptypes = array_merge($ptypes, array($type));
			$parentIndex = $this->indexSuperTypes($parentIndex, $stype, $nptypes);
      }

		if(!$parentIndex[$type]) $parentIndex[$type] = array();
		foreach($ptypes as $ptype) {
			if(!in_array($ptype, $parentIndex[$type]))
				array_push($parentIndex[$type], $ptype);
		}
		return $parentIndex;
	}

   function getDataClassesAndInstances($cls='', $hierarchy=false) {
      if(!$cls) $cls = $this->dcThing;

      $clsinstances = array();
      $vals = $this->libapi->getTriples(null, RDF_NAMESPACE_URI.RDF_TYPE, $cls);
      $possibleValues = array();
      foreach($vals as $val) {
         array_push($possibleValues, $val->subj->uri);
      }
      if($hierarchy) {
         $clsinstances['id'] = $cls;
         $clsinstances['instances'] = $possibleValues;
         $clsinstances['children'] = array();
      }
      else
         $clsinstances[$cls] = $possibleValues;

      $subs = $this->ontapi->getTriples(null, RDF_SCHEMA_URI."subClassOf", $cls);
      foreach($subs as $sub) {
         $subcls = $sub->subj->uri;
         if($hierarchy)
            array_push($clsinstances['children'], $this->getDataClassesAndInstances($subcls, $hierarchy));
         else
            $clsinstances = array_merge($clsinstances, $this->getDataClassesAndInstances($subcls, $hierarchy));
      }
      return $clsinstances;
   }

   function addNewDataObject($data, $type) {
      $sts = $this->libapi->getTriples($data, null, null);
      foreach($sts as $st)
         $this->libapi->removeStatement($st);
      $this->libapi->addStatement($data, RDF_NAMESPACE_URI.RDF_TYPE, $type);
   }

   function saveLibrary() {
		$ns = $this->nsmap;
		$defaultns = $ns['dclib']?$ns['dclib']:$ns['library'];
		$base = preg_replace('/#(.+)$/','',$defaultns);
		$this->libapi->addNamespace('',$defaultns);
		$this->libapi->setBase($base);
      $this->libapi->saveAs($this->libfile);
   }

   function saveOntology() {
		$ns = $this->nsmap;
		$defaultns = $ns['dcdom']?$ns['dcdom']:$ns['ontology'];
		$base = preg_replace('/#(.+)$/','',$defaultns);
		$this->ontapi->addNamespace('',$defaultns);
		$this->ontapi->setBase($base);
      $this->ontapi->saveAs($this->ontfile);
   }

   function getPrefixedTerm($resource, $nsmap, $prefix=true) {
      if(isset($resource->uri)) {
         if(!$prefix) return $resource->uri;
         $ns = $resource->getNamespace();
         if(isset($nsmap[$ns]))
            return ($nsmap[$ns]?$nsmap[$ns].':':'').$resource->getLocalName();
         else
            return $resource->uri;
      }
      else {
         if(!$prefix) return $resource->label;
         $str = '"'.$resource->label.'"';
         if($resource->dtype) {
            $str .= '^^'.preg_replace('/.*#(.*)$/',"xsd:$1",$resource->dtype);
         }
         return $str;
      }
   }

   function getConstraintProperties($extraprops=true, $pref=true) {
      $dc = $this->nsmap['dc'];
      $rns = array_flip($this->nsmap);

      $objprops = $this->ontapi->getTriples(null, RDF_SCHEMA_URI."subPropertyOf", $dc."hasMetrics");
      $dataprops = $this->ontapi->getTriples(null, RDF_SCHEMA_URI."subPropertyOf", $dc."hasDataMetrics");
      $rangeprops = $this->ontapi->getTriples(null, RDF_SCHEMA_URI."range", null);

      $props = array();
      if($extraprops) $props = $this->addHardcodedProperties($props);
      foreach($objprops as $t)
         $props[$this->getPrefixedTerm($t->subj, $rns, $pref)] = array();
      foreach($dataprops as $t)
         $props[$this->getPrefixedTerm($t->subj, $rns, $pref)] = array();

      foreach($rangeprops as $ranget) {
         $propid = $this->getPrefixedTerm($ranget->subj, $rns, $pref);
         if(!isset($props[$propid])) continue;
         $possibleValues = array();
         $range = $ranget->obj;
         if($range->uri && $range->getNamespace() != XML_SCHEMA) {
            $vals = $this->ontapi->getTriples(null, RDF_NAMESPACE_URI.RDF_TYPE, $range->uri);
            $vals = array_merge($vals, $this->libapi->getTriples(null, RDF_NAMESPACE_URI.RDF_TYPE, $range->uri));
            foreach($vals as $val) {
               array_push($possibleValues, $this->getPrefixedTerm($val->subj, $rns, $pref));
            }
         }
         $props[$propid]['range'] = $this->getPrefixedTerm($range, $rns, $pref);
         $props[$propid]['values'] = $possibleValues;
      }
      return $props;
   }

   function addHardcodedProperties($props) {
      $rns = array_flip($this->nsmap);
      $clsinst = $this->getDataClassesAndInstances();
      $clses = array();
      $insts = array();
      foreach($clsinst as $cls=>$inst) {
         array_push($clses, $this->getPrefixedTerm(new Resource($cls), $rns));
         foreach($inst as $in) 
         	array_push($insts, $this->getPrefixedTerm(new Resource($in), $rns));
      }
      $props["rdf:type"] = array();
      $props["rdf:type"]["range"] = "owl:Class";
      $props["rdf:type"]["values"] = $clses;

      $props["wflow:hasDataBinding"] = array();
      $props["wflow:hasDataBinding"]["range"] = "dc:DataObject";
      $props["wflow:hasDataBinding"]["values"] = $insts;

      $props["wflow:hasParameterValue"] = array();
      $props["wflow:hasParameterValue"]["range"] = "xsd:string";
      $props["wflow:hasParameterValue"]["values"] = array();

      $props["wflow:hasSameDataAs"] = array();
      $props["wflow:hasSameDataAs"]["range"] = "wflow:DataVariable";
      $props["wflow:hasSameDataAs"]["values"] = array();

      $props["wflow:hasDifferentDataFrom"] = array();
      $props["wflow:hasDifferentDataFrom"]["range"] = "wflow:DataVariable";
      $props["wflow:hasDifferentDataFrom"]["values"] = array();

      return $props;
   }

	function getDatatype($dsid) {
      $ns = $this->nsmap;
		$clses = $this->libapi->getClassesOfInstance($dsid);
		foreach($clses as $cls) return $cls;
		return null;
	}

	function getAllDatatypes($cls) {
      if(!$cls) $cls = new Resource($this->dcThing);
      $rns = array_flip($this->nsmap);
		$clses = array($cls->getLocalName());
      $subs = $this->ontapi->getTriples(null, RDF_SCHEMA_URI."subClassOf", $cls->uri);
      foreach($subs as $sub) {
			$subcls = $sub->subj;
         $clses = array_merge($clses, $this->getAllDatatypes($subcls));
		}
		return $clses;
	}

	function getAllMetadataProperties() {
      $ns = $this->nsmap;
		return $this->ontapi->getSubPropertiesDetail($ns['dc'].'hasDataMetrics');
	}

	function getDatatypeProperties($dtype) {
      $ns = $this->nsmap;
		$allprops = $this->getAllMetadataProperties();
		$clses = array();
		if($dtype) {
			$clses = array_merge($clses, $this->ontapi->getSuperClasses($dtype));
			if(!in_array($this->dcThing, $clses)) array_push($clses, $this->dcThing);
		} else {
			$dtype = $this->dcThing;
		}
		array_push($clses, $dtype);

		$props = array();
		foreach($allprops as $prop=>$pinfo) {
			$domain = $pinfo['domain'];
			if(!count($domain)) $domain = array($this->dcThing);
			foreach($domain as $dom) {
				if(in_array($dom, $clses)) {
					if($props[$prop] && $dom==$dtype)
						$props[$prop]['editable'] = true;
					else if(!$props[$prop])
						$props[$prop] = array('range'=>$pinfo['range'],'editable'=>($dom==$dtype));
				}
			}
			/*if(!$domain) $domain = $this->dcThing;
			if(in_array($domain, $clses)) {
				$props[$prop] = array('range'=>$pinfo['range'],'editable'=>($domain==$dtype));
			}*/
		}
		return $props;
	}

   function getDatasetPropertyValues($dsid, $props) {
      $triples = $this->libapi->getTriples($dsid, null, null);
      $vals = array();
      foreach($props as $prop) $vals[$prop] = null;
      foreach($triples as $triple) {
			$pred = $triple->pred->uri;
			$val = $triple->obj->label;
			if(in_array($pred, $props)) {
         	if($val != "") $vals[$pred] = $val;
			}
      }
      return $vals;
   }

	function removeAllPropertyValues($dsid, $props) {
		$dsr = new Resource($dsid);
		foreach($props as $prop) {
			$propr = new Resource($prop);
			$vals = $this->libapi->getPropertyValues($dsid, $prop, true);
			foreach($vals as $val) {
				//print("Removing "+$prop);
				$this->libapi->removeStatement(new Statement($dsr, $propr, $val));
			}
		}
	}

	function addDatatypePropertyValue($dsid, $prop, $val, $range) {
      $ns = $this->nsmap;
		$this->libapi->setPropertyValue($dsid, $prop, $val, $range);
	}

	function addMetadataProperty($propid, $domain, $range) {
		$ns = $this->nsmap;
		if(!$domain) $domain = $this->dcThing;
		$this->ontapi->addStatement($propid, RDF_NAMESPACE_URI.RDF_TYPE, OWL_NS."DatatypeProperty");
		$this->ontapi->addStatement($propid, RDF_SCHEMA_URI."subPropertyOf", $ns['dc']."hasDataMetrics");
		$this->ontapi->addStatement($propid, RDF_SCHEMA_URI."range", $range);
		$this->ontapi->addStatement($propid, RDF_SCHEMA_URI."domain", $domain);
	}

	function addDatatype($dtype, $ptype) {
		$ns = $this->nsmap;
		$this->ontapi->addStatement($dtype, RDF_NAMESPACE_URI.RDF_TYPE, OWL_NS."Class");
		$this->ontapi->addStatement($dtype, RDF_SCHEMA_URI."subClassOf", $ptype);
	}

	function removeDatatype($dtype) {
		$ns = $this->nsmap;
		// Remove all properties of the datatype
      $props = $this->ontapi->getTriples(null, RDF_SCHEMA_URI."domain", $dtype);
      foreach($props as $prop) {
			$this->ontapi->removeAllStatementsWith($prop->subj->uri);
      }
		// Remove all Files of that type
      $vals = $this->libapi->getTriples(null, RDF_NAMESPACE_URI.RDF_TYPE, $dtype);
      foreach($vals as $val) {
			$this->removeData($val->subj->uri, $val->subj->getLocalName());
      }
		// Remove the datatype itself
		$this->ontapi->removeAllStatementsWith($dtype);
	}

	function removeData($did, $dname) {
		$this->libapi->removeAllStatementsWith($did);
		unlink($this->datadir."/".$dname);
	}

	function moveDatatypeTo($dtype, $fromtype, $totype) {
		$dobj = new Resource($dtype);
		$fobj = new Resource($fromtype);
		$tobj = new Resource($totype);
		$subcls = new Resource(RDF_SCHEMA_URI."subClassOf");

		// Change superClass
		$st1 = new Statement($dobj, $subcls, $fobj);
		$this->ontapi->removeStatement($st1);
		$this->ontapi->addResourceStatement($dobj, $subcls, $tobj);

		// Get all properties of fromtype that are invalid for totype
		/*$fromProps = $this->getDatatypeProperties($fromtype);
		$toProps = $this->getDatatypeProperties($totype);
		$invalidProps = array();
		foreach($fromProps as $pid=>$pinfo) {
			if(!$toProps[$pid]) array_push($invalidProps, $pid);
		}*/

		// For all instances of dtype and below, remove the invalid properties
	}

	function addDataForType($id, $dtype) {
		$this->libapi->addStatement($id, RDF_NAMESPACE_URI.RDF_TYPE, $dtype);
	}

	function removeMetadataProperty($propid) {
		$this->ontapi->removeAllStatementsWith($propid);
		$this->libapi->removeAllStatementsWith($propid);
	}

	function renameMetadataProperty($oldid, $newid) {
		$this->ontapi->renameAllStatementsWith($oldid, $newid);
		$this->libapi->renameAllStatementsWith($oldid, $newid);
	}

	function registerData($id, $nid, $metricsJson) {
      $ns = $this->nsmap;
		$subj = $ns['dclib'].($nid ? $nid : $id);
		$subjtype = $this->dcThing;

		$dataProps = $this->getAllMetadataProperties();
		$parentIndex = $this->indexSuperTypes();

		// Get Prospective metadata property values for the dataset
		$metrics = json_decode($metricsJson);
		$tmpcons = array();
		foreach($metrics as $metric) {
			$pred = $obj = $objdtype = null;
			$metric = get_object_vars($metric);

			if(preg_match('/(.+):(.+)/',$metric['p'],$m)) {
				if(array_key_exists($m[1], $ns)) 
					$pred = $ns[$m[1]].$m[2];
			}
			if(!$pred) continue;

			if(preg_match('/(.+?):(.+)/',$metric['o'],$m)) {
				if(array_key_exists($m[1], $ns))
					$obj = $ns[$m[1]].$m[2];
			}

			if($metric['p'] == "rdf:type") {
				if($obj && in_array($subjtype, $parentIndex[$obj])) 
					$subjtype = $obj;
				continue;
			}

			if($dataProps[$pred]) {
				$obj = $metric['o'];
				$objdtype = $dataProps[$pred]['range'];
			}
			array_push($tmpcons, array($pred, $obj, $objdtype));
		}

		// Create dataset in the library
   	$this->addNewDataObject($subj, $subjtype);

		// Create dataset property values
		foreach($tmpcons as $c) {
			if($c[2]) $this->libapi->setPropertyValue($subj, $c[0], $c[1], $c[2]);
			else $this->libapi->setPropertyValue($subj, $c[0], $c[1]);
		}

		if($nid) {
			copy($this->datadir."/".$id, $this->datadir."/".$nid);
		}
	}
}
?>
