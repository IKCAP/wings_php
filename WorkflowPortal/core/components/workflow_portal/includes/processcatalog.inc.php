<?php

Class ProcessCatalog {
   var $api;
	var $wapi;
   var $absapi;
   var $libapi;
   var $nsmap;
   var $absfile;
   var $libfile;
	var $rulefile;
   var $codedir;
   var $topcodedir;
   var $pcThing;

   function ProcessCatalog($absfile, $libfile, $rulefile, $topcodedir, $codedir, $props) {
		$this->absfile = $absfile;
      $this->libfile = $libfile;
      $this->rulefile = $rulefile;
      $this->codedir = $codedir;
      $this->topcodedir = $topcodedir;
		$this->api = new KBAPI();
		$this->nsmap = array();
		if($absfile && file_exists($absfile)) {
      	$this->absapi = new KBAPI($absfile);
			$this->wapi = $this->absapi;
			$this->api->addStatements($this->absapi->getTriples(null, null, null));
      	$this->nsmap = array_merge($this->nsmap, $this->absapi->getPrefixNamespaceMap());
		}
		if($libfile && file_exists($libfile)) {
      	$this->libapi = new KBAPI($libfile);
			$this->wapi = $this->libapi;
			$this->api->addStatements($this->libapi->getTriples(null, null, null));
      	$this->nsmap = array_merge($this->nsmap, $this->libapi->getPrefixNamespaceMap());
		}
		// Defined namespaces
      $this->defnsmap = $props->getPCPrefixNSMap();
      $this->defnsmap = array_merge($this->defnsmap, $props->getDCPrefixNSMap());
		
		// Overwrite the owl namespace prefixes with the defined namespace prefixes
		$this->nsmap = array_flip(array_merge(array_flip($this->nsmap), array_flip($this->defnsmap)));
		unset($this->nsmap['defaultns']);
		$this->pcThing = $this->nsmap['ac']."Component";

		if($this->libapi && !$this->absapi) {
			// Split combined library into abstract and concrete library
			$this->separateAbstractFromConcrete();
			$this->saveLibrary($this->libfile, $this->libapi);
			$this->saveLibrary($this->absfile, $this->absapi);
			$this->moveCodesIntoLibrary();
		}

		foreach($this->nsmap as $pfx=>$ns) {
			$this->api->addNamespace($pfx, $ns);
		}
   }

	function moveCodesIntoLibrary() {
		mkdir($this->codedir);
   	$hier = $this->getComponentHierarchy();
		while(count($hier)) {
			$comp = array_pop($hier);
			$id = $comp['id'];
			if($comp['concrete']) {
				if(file_exists($this->topcodedir."$id/run") && !file_exists($this->codedir."$id/run")) {
					rename($this->topcodedir.$id, $this->codedir.$id);
				}
			}
			if($comp["children"]) {
				$hier = array_merge($hier, $comp["children"]);
			}
		}
	}

	function separateAbstractFromConcrete() {
		$aapi = new KBAPI();
		foreach($this->nsmap as $pfx=>$ns) 
			$aapi->addNamespace($pfx, $ns);

		$capi = $this->libapi;
		$ns = $this->nsmap['acdom'] ? $this->nsmap['acdom'] : $this->nsmap['library'];

   	$hier = $this->getComponentHierarchy();
		while(count($hier)) {
			$comp = array_pop($hier);
			if(!$comp['concrete']) {
				#print "\n".$comp["id"]."\n--------\n";
				#print_r($comp);
				$cid = $ns.$comp['id'];
				$ccls = $ns.$comp['cls'];
				$triples = $capi->getTriples($cid, null, null);
				$triples = array_merge($triples, $capi->getTriples($ccls, null, null));
				foreach($comp["inputs"] as $role) {
					$triples = array_merge($triples, $capi->getTriples($ns.$role['id'], null, null));
				}
				foreach($comp["outputs"] as $role) {
					$triples = array_merge($triples, $capi->getTriples($ns.$role['id'], null, null));
				}
				foreach($triples as $t) {
					$capi->removeStatement($t);
				}

				$triples = array_filter($triples, "ProcessCatalog::TripleFilter");
				$aapi->addStatements($triples);	
			}
			if($comp["children"]) {
				$hier = array_merge($hier, $comp["children"]);
			}
		}
		$this->libapi = $capi;
		$this->absapi = $aapi;
	}

	public static function TripleFilter($t) {
		if($t->getObject()->uri == OWL_NS."Thing") 
			return false; 
		else if(get_class($t->getObject()) == "BlankNode") 
			return false; 
		else return true;
	}

   function saveLibrary($file, $api=null) {
		$ns = $this->nsmap;
		$defaultns = $ns['acdom']?$ns['acdom']:$ns['library'];
		$base = preg_replace('/#(.+)$/','',$defaultns);
		if(!$api) $api=$this->wapi;
		if(!$file) $file=$this->libfile;
		if($api && $file) {
			$api->addNamespace('',$defaultns);
			$api->setBase($base);
      	$api->saveAs($file);
		}
   }

   function getComponentInputs($comp) {
      $ns = $this->nsmap;
      return $this->api->getPropertyValues($comp, $ns['ac']."hasInput");
   }

   function getComponentOutputs($comp) {
      $ns = $this->nsmap;
      return $this->api->getPropertyValues($comp, $ns['ac']."hasOutput");
   }

   function getRoleForArgumentID($comp, $argid) {
      $ns = $this->nsmap;
      $args = $this->getComponentInputs($comp);
      $args = array_merge($args, $this->getComponentOutputs($comp));
      foreach($args as $arg) {
         $argxid = $this->api->getPropertyValue($arg, $ns['ac']."hasArgumentID", true);
         if($argxid && $argid == $argxid->label) return $arg;
      }
      return null;
   }

   function getRoleType($comp, $argid) {
      $argname = preg_replace("/.*#/","",$argid);
      $role = $this->getRoleForArgumentID($comp, $argname);
      $ns = $this->nsmap;
      $rtypes = $this->api->getPropertyValues($role, RDF_NAMESPACE_URI."type");
      foreach($rtypes as $rtype) {
         if(strpos($rtype, $ns['ac'])===0) continue;
         if(strpos($rtype, $ns['owl'])===0) continue;
         return $rtype;
      }
      return null;
   }

   function getRoleDataType($comp, $argid) {
      $argname = preg_replace("/.*#/","",$argid);
      $role = $this->getRoleForArgumentID($comp, $argname);
      $ns = $this->nsmap;
      $vals = $this->api->getPropertyValues($role, $ns['ac']."hasValue", true);
      foreach($vals as $val) {
         if($val->dtype) return $val->dtype;
      }
      return null;
   }

   function localName($resource) {
		$res = new Resource($resource);
      return $res->getLocalName();
   }

	function getAllRules() {
		return implode(file($this->rulefile), "");
	}

	function saveAllRules($rules) {
		$fh = fopen($this->rulefile, "w");
		fwrite($fh, $rules);
		fclose($fh);
	}

	function getComponentRules($cid) {
		return array();
	}

	function getComponentInputDetails($cid) {
		$inputs = array();
		$inputObjs = $this->getComponentInputs($cid);
		foreach($inputObjs as $inputObj) {
			array_push($inputs, $this->fetchArgumentInformation($this->api, $this->nsmap, $inputObj));
		}
		return $inputs;
	}

	function getComponentOutputDetails($cid) {
		$outputs = array();
		$outputObjs = $this->getComponentOutputs($cid);
		foreach($outputObjs as $outputObj) {
			array_push($outputs, $this->fetchArgumentInformation($this->api, $this->nsmap, $outputObj));
		}
		return $outputs;
	}

	// FIXME: Assuming Only 1 Component per Component Class for now
   function getComponentHierarchy($cls, $noDetails) {
      $ns = $this->nsmap;
      if(!$cls) $cls = $ns['ac']."Component";

		$component = array();
		$component['cls'] = $this->localName($cls);

      $compObjs = $this->api->getTriples(null, RDF_NAMESPACE_URI.RDF_TYPE, $cls);
		if(sizeof($compObjs)) {
         $cobj = $compObjs[0]->subj->uri;

			$component['id'] = $this->localName($cobj);
			$isConcrete = $this->api->getPropertyValue($cobj, $ns['ac'].'isConcrete', true);
			if($isConcrete->label == "true") $component['concrete'] = true;
			else $component['concrete'] = false;
			if($component["concrete"]) {
				$component["uploaded"] = file_exists($this->codedir.$component['id']."/run");
			}
			if(!$noDetails) {
				$component['inputs'] = $this->getComponentInputDetails($cobj);
				$component['outputs'] = $this->getComponentOutputDetails($cobj);
			}
      }

      $subs = $this->api->getTriples(null, RDF_SCHEMA_URI."subClassOf", $cls);
      $component['children'] = array();
      foreach($subs as $sub) {
         $subcls = $sub->subj->uri;
         $subcomp = $this->getComponentHierarchy($subcls, $noDetails);
         if($subcomp) array_push($component['children'], $subcomp);
      }
		if($cls == $ns['ac']."Component") return $component['children'];
		//if(sizeof($component['children']) == 1) return $component['children'][0];
		if(!sizeof($component['children'])) unset($component['children']);
		return $component;
   }

	private function fetchArgumentInformation($api, $ns, $obj) {
		$types = array();
		$alltypes = $api->getPropertyValues($obj, $ns['rdf']."type");
		$isParam = null;
		foreach($alltypes as $type) {
			if(strpos($type, $ns['owl'])===false) {
				if($type == $ns['ac']."DataArgument") $isParam = false;
				else if($type == $ns['ac']."ParameterArgument") $isParam = true;
				else array_push($types, $this->localName($type));
			}
		}
		$role = $api->getPropertyValue($obj, $ns['ac']."hasArgumentID",true);
		$dim = $api->getPropertyValue($obj, $ns['ac']."hasDimensionality",true);
		$pfx = $api->getPropertyValue($obj, $ns['ac']."hasArgumentName",true);
		$ind = $api->getPropertyValue($obj, $ns['ac']."hasArgumentIndex",true);
		if($isParam) {
			$val = $api->getPropertyValue($obj, $ns['ac']."hasValue",true);
			if($val && $val->dtype) {
				$arg["default"] = $val->label;
				array_push($types, $this->localName($val->dtype));
			}
		}

		$arg["id"] = $this->localName($obj);
		$arg["role"] = $role->label;
		if(sizeof($types)) $arg["types"] = $types;
		$arg["param"] = $isParam;
		$arg["dim"] = $dim && $dim->label ? $dim->label : 0;
		if($pfx) $arg["pfx"] = $pfx->label;
		if($ind) $arg["ind"] = $ind->label;
		return $arg;
	}

	function getSubComponents($ctype) {
		$subComps = array();
      $subs = $this->api->getTriples(null, RDF_SCHEMA_URI."subClassOf", $ctype);
      foreach($subs as $sub) {
         $subcls = $sub->subj->uri;
			$c = array("cls"=>$subcls);
      	$compObjs = $this->api->getTriples(null, RDF_NAMESPACE_URI.RDF_TYPE, $subcls);
			if(sizeof($compObjs)) 
				$c['id'] = $compObjs[0]->subj->uri;
			array_push($subComps, $c);
		}
		return $subComps;
	}

	function addComponentType($ctype, $ptype) {
		$ns = $this->nsmap;
		if(!$ptype) $ptype = $this->pcThing;
		$this->api->addStatement($ctype, RDF_NAMESPACE_URI.RDF_TYPE, OWL_NS."Class");
		$this->api->addStatement($ctype, RDF_SCHEMA_URI."subClassOf", $ptype);

		$this->wapi->addStatement($ctype, RDF_NAMESPACE_URI.RDF_TYPE, OWL_NS."Class");
		$this->wapi->addStatement($ctype, RDF_SCHEMA_URI."subClassOf", $ptype);
	}

	function addComponentForType($id, $ctype) {
		$this->api->addStatement($id, RDF_NAMESPACE_URI.RDF_TYPE, $ctype);
		$this->wapi->addStatement($id, RDF_NAMESPACE_URI.RDF_TYPE, $ctype);
	}

	function removeComponentType($ctype) {
		$this->api->removeAllStatementsWith($ctype);
		$this->wapi->removeAllStatementsWith($ctype);
	}

	function removeComponent($cid, $unlink) {
		$inputObjs = $this->getComponentInputs($cid);
		$outputObjs = $this->getComponentOutputs($cid);
		foreach($inputObjs as $obj) {
			$this->api->removeAllStatementsWith($obj);
			$this->wapi->removeAllStatementsWith($obj);
		}
		foreach($outputObjs as $obj) {
			$this->api->removeAllStatementsWith($obj);
			$this->wapi->removeAllStatementsWith($obj);
		}

		$this->api->removeAllStatementsWith($cid);
		$this->wapi->removeAllStatementsWith($cid);

		if($unlink) recursive_unlink($this->codedir."/".$cid);
	}

	function getRole($roleid) {
      $ns = $this->nsmap;
      $cin = $this->api->getTriples(null, $ns['ac']."hasInput", $roleid);
      $cout = $this->api->getTriples(null, $ns['ac']."hasOutput", $roleid);
		if(count($cin) || count($cout)) return $roleid;
		return null;
	}

	function checkRoleDimensionality($argid, $dim) {
      $ac = $this->nsmap['ac'];
		$result = $this->api->RDQL("SELECT ?dim WHERE 
												( ?arg  ${ac}hasArgumentID \"$argid\"^^xsd:string ) 
												( ?arg ${ac}hasDimensionality ?dim )", 
					array($ac."hasArgumentID", $ac."hasDimensionality"), true);
		
		foreach($result as $res) {
			if($res["?dim"]->dtype && $res["?dim"]->label != $dim) 
				return false;
		}
		return true;
	}

	function createRole($roleid, $argid, $roletype, $argname, $dim) {
      $ns = $this->nsmap;
		$this->api->addStatement($roleid, RDF_NAMESPACE_URI.RDF_TYPE, $roletype);
		$this->api->setPropertyValue($roleid, $ns['ac']."hasArgumentName", $argname, $ns['xsd']."string");
		$this->api->setPropertyValue($roleid, $ns['ac']."hasArgumentID", $argid, $ns['xsd']."string");
		$this->api->setPropertyValue($roleid, $ns['ac']."hasDimensionality", ($dim?$dim:"0"), $ns['xsd']."int");

		$this->wapi->addStatement($roleid, RDF_NAMESPACE_URI.RDF_TYPE, $roletype);
		$this->wapi->setPropertyValue($roleid, $ns['ac']."hasArgumentName", $argname, $ns['xsd']."string");
		$this->wapi->setPropertyValue($roleid, $ns['ac']."hasArgumentID", $argid, $ns['xsd']."string");
		$this->wapi->setPropertyValue($roleid, $ns['ac']."hasDimensionality", ($dim?$dim:"0"), $ns['xsd']."int");
	}

	function setRoleDefaultValue($roleid, $value, $type) {
      $ns = $this->nsmap;
		if(!$type) $type = "string";
		if(!$value) {
			if($type == "int") $value = "0";
			else if($type == "boolean") $value = "false";
			else if($type == "float") $value = "0.0";
			else if($type == "date") $value = "0-0-0T0:0:0";
		}
		$this->api->setPropertyValue($roleid, $ns['ac']."hasValue", $value, $ns['xsd'].$type);
		$this->wapi->setPropertyValue($roleid, $ns['ac']."hasValue", $value, $ns['xsd'].$type);
	}

	function setComponentIsConcrete($cid, $isConcrete) {
      $ns = $this->nsmap;
		$this->api->removeSubjectProperties($cid, $ns['ac']."isConcrete");
		$this->api->setPropertyValue($cid, $ns['ac']."isConcrete", $isConcrete ? "true":"false", $ns['xsd']."boolean");
		$this->wapi->removeSubjectProperties($cid, $ns['ac']."isConcrete");
		$this->wapi->setPropertyValue($cid, $ns['ac']."isConcrete", $isConcrete ? "true":"false", $ns['xsd']."boolean");
	}

	function addRoleType($roleid, $roletype) {
		$this->api->addStatement($roleid, RDF_NAMESPACE_URI.RDF_TYPE, $roletype);
		$this->wapi->addStatement($roleid, RDF_NAMESPACE_URI.RDF_TYPE, $roletype);
	}

	function addComponentInput($cid, $roleid) {
      $ns = $this->nsmap;
		$this->api->addStatement($cid, $ns['ac']."hasInput", $roleid);
		$this->wapi->addStatement($cid, $ns['ac']."hasInput", $roleid);
	}

	function addComponentOutput($cid, $roleid) {
      $ns = $this->nsmap;
		$this->api->addStatement($cid, $ns['ac']."hasOutput", $roleid);
		$this->wapi->addStatement($cid, $ns['ac']."hasOutput", $roleid);
	}
}
?>
