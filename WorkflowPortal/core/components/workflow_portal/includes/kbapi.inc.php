<?php
/*
 This implementation of KBAPI uses RAP (RDF API for PHP)
 */

define("RDFAPI_INCLUDE_DIR", PORTAL_INCLUDES_PATH."rdfapi-php/api/");
include(RDFAPI_INCLUDE_DIR . "RdfAPI.php");
include(RDFAPI_INCLUDE_DIR . "resModel/ResModelP.php");
include(RDFAPI_INCLUDE_DIR . "ontModel/OntModelP.php");

Class KBAPI {
	var $file;
	var $model;

	var $nsPrefix;
	var $prefixNS;

	var $is_resmodel;
	var $is_ontmodel;
	var $isn3 = false;

	function KBAPI($file='', $use_resmodel=false, $use_ontmodel=false) {
		if($use_resmodel) $this->model = ModelFactory::getResModel();
		else if($use_ontmodel) $this->model = ModelFactory::getOntModel();
		else $this->model = ModelFactory::getDefaultModel();
		$this->is_resmodel = $use_resmodel;
		$this->is_ontmodel = $use_ontmodel;
		if($file) $this->loadFile($file);
	}
	function loadFile($file) {
		$this->file = $file;
		$data = file($file);
		foreach($data as $line) {
			if(preg_match('/^@prefix/', $line)) {
				$this->isn3 = true;
				break;
			}
		}
		if($this->isn3) 
			$this->model->load($file, "n3");
		else 
			$this->model->load($file);

		$this->initNamespaces();
	}

	function initNamespaces() {
		$this->nsPrefix = $this->model->getParsedNamespaces();
		$this->nsPrefix[$this->model->baseURI] = 'defaultns';
		$this->prefixNS = array_flip($this->nsPrefix);
	}

	function getNamespacePrefixMap() {
		return $this->nsPrefix;
	}
	function getPrefixNamespaceMap() {
		return $this->prefixNS;
	}
	function getDefaultNamespace() {
		return $this->model->baseURI;
	}

	function addImport($base, $import) {
		$this->addStatement($base, RDF_NAMESPACE_URI.RDF_TYPE, OWL_NS."Ontology");
		$this->addStatement($base, OWL_NS."imports", $import);
	}

	function addNamespace($prefix, $namespace) {
		$this->model->addNamespace($prefix, $namespace);
		$this->nsPrefix[$namespace]=$prefix;
		$this->prefixNS[$prefix] = $namespace;
	}

	function addNamespacePrefixMap($nsPrefixMap) {
		$this->model->addParsedNamespaces($nsPrefixMap);
		$this->initNamespaces();
		/*foreach($prefixNSMap as $prefix=>$ns) {
		 $this->addNamespace($prefix,$ns);
		 }*/
	}

	function setBaseURI($uri) {
		$this->model->setBaseURI($uri);
	}

	function getBaseURI() {
		return $this->model->getBaseURI();
	}

	function setBase($uri) {
		$this->model->setBaseURI($uri);
	}

	function saveAs($file) {
		if($this->isn3) $this->model->saveAs($file, 'n3');
		else $this->model->saveAs($file);
	}

	function addStatement($subj, $pred, $obj) {
		$st = new Statement(new Resource($subj), new Resource($pred), new Resource($obj));
		if($this->is_resmodel) $result = $this->model->model->add($st);
		else $result = $this->model->add($st);
	}

	function addResourceStatement($subj, $pred, $obj) {
		$st = new Statement($subj, $pred, $obj);
		if($this->is_resmodel) $this->model->model->add($st);
		else $this->model->add($st);
	}

	function addStatements($sts) {
		foreach($sts as $st) {
			if($this->is_resmodel) $result = $this->model->model->add($st);
			else $result = $this->model->add($st);
		}
	}

	function setPropertyDataValue($s, $p, $o) {
		if(!is_object($s)) $s=new Resource($s);
		if(!is_object($p)) $p=new Resource($p);
		$obj = new Literal($o);
		$this->addResourceStatement($s, $p, $obj);
	}
	function setPropertyValue($s, $p, $o, $dtype) {
		if(!is_object($s)) $s=new Resource($s);
		if(!is_object($p)) $p=new Resource($p);
		if(!$dtype) {
			if(!is_object($o)) $o=new Resource($o);
			$this->addResourceStatement($s, $p, $o);
		} 
		else {
			$obj = new Literal($o, NULL, $dtype);
			$this->addResourceStatement($s, $p, $obj);
		}
	}
	
	function createInstanceOfClass($id, $cls) {
		$this->addStatement($id, RDF_NAMESPACE_URI.RDF_TYPE, $cls);
		return $id;
	}

	function createAnonymousInstanceOfClass($cls) {
    	$bnode = new BlankNode($this->model);
		$this->addResourceStatement($bnode, 
			new Resource(RDF_NAMESPACE_URI.RDF_TYPE), 
			new Resource($cls));
		return $bnode;
	}

	function removeAllStatementsWith($id) {
		$obj = new Resource($id);
		$sts = $this->getResourceTriples($obj, null, null);
		$sts = array_merge($sts, $this->getResourceTriples(null, $obj, null));
		$sts = array_merge($sts, $this->getResourceTriples(null, null, $obj));
		foreach($sts as $st) {
			$this->removeStatement($st);
		}
	}

	function removeSubjectProperties($subj, $pred) {
		$subj = new Resource($subj);
		$pred = new Resource($pred);
		$sts = $this->getResourceTriples($subj, $pred, null);
		foreach($sts as $st) {
			$this->removeStatement($st);
		}
	}

	function renameAllStatementsWith($id, $newid) {
		$obj = new Resource($id);
		$newobj = new Resource($newid);

		$newsts = array();
		$sts1 = $this->getResourceTriples($obj, null, null);
		foreach($sts1 as $st) array_push($newsts, new Statement($newobj, $st->pred, $st->obj));

		$sts2 = $this->getResourceTriples(null, $obj, null);
		foreach($sts2 as $st) array_push($newsts, new Statement($st->subj, $newobj, $st->obj));

		$sts3 = $this->getResourceTriples(null, null, $obj);
		foreach($sts3 as $st) array_push($newsts, new Statement($st->subj, $st->pred, $newobj));

		$sts = array_merge($sts1, $sts2, $sts3);
		foreach($sts as $st) {
			$this->removeStatement($st);
		}
		$this->addStatements($newsts);
	}

	function removeStatement($st) {
		if($this->is_resmodel) $result = $this->model->model->remove($st);
		else $result = $this->model->remove($st);
	}

	function createRDQLQuery($query, $objs) {
		$clause = "";
		foreach($objs as $obj) {
			foreach($this->prefixNS as $prefix=>$ns) {
				if(strpos($obj,$ns) === 0) {
					$prefix = "x_".$prefix;
					$clause .= "\n$prefix FOR <$ns>";
					$query = str_replace($ns,"$prefix:",$query);
					break;
				}
			}
		}
		if($clause) { $query .= " USING ".$clause; }
		return $query;
	}

	function RDQL($query, $items, $return_asis=false) {
		if(!is_array($items)) $items = array($items);
		$query = $this->createRDQLQuery($query, $items);
		$result = $this->model->rdqlQuery($query);

		#print_r($result);
		if($return_asis) return $result;

		$ret = array();
		foreach($result as $res) {
			if($res["?x"]->uri) 	
				array_push($ret,$res["?x"]->uri);
		}
		return $ret;
	}

	function getInstancesOfClass($cls) {
		return $this->RDQL("SELECT ?x WHERE ( ?x rdf:type $cls )", $cls);
	}

	function getClassesOfInstance($item) {
		return $this->RDQL("SELECT ?x WHERE ( $item rdf:type ?x )", $item);
	}

	function isA($item, $cls) {
		$props = $this->RDQL("SELECT ?x WHERE ( $item ?x $cls )", array($item, $cls));
		foreach($props as $prop) {
			if($prop == RDF_NAMESPACE_URI."type") return true;
		}
		return false;
	}

	function getSuperClasses($cls) {
		$clses = array();
		while($cls) {
			$superclses = $this->RDQL("SELECT ?x WHERE ( $cls rdfs:subClassOf ?x )", $cls);
			if($superclses) {
				$cls = $superclses[0];
				array_push($clses, $cls);
			}
			else break;
		}
		return $clses;
		//return $this->RDQL("SELECT ?x WHERE ( $cls rdfs:subClassOf ?x )", $cls);
	}

	function getSubProperties($prop) {
		return $this->RDQL("SELECT ?x WHERE ( ?x rdfs:subPropertyOf $prop )", $prop);
	}

	function getClassProperties($cls) {
		return $this->RDQL("SELECT ?x WHERE ( ?x rdfs:domain $cls )", $cls);
	}

	function getSubPropertiesDetail($prop) {
		$ret = array();
		$result = $this->RDQL("SELECT ?x WHERE ( ?x rdfs:subPropertyOf $prop ) ", $prop, true);
		foreach($result as $res) {
			$subprop = $res["?x"]->uri;
			if($res["?x"]->uri) {
				$prop = array();
				$prop['range'] = $this->getPropertyValue($subprop, RDF_SCHEMA_URI."range");
				$prop['domain'] = $this->getPropertyValues($subprop, RDF_SCHEMA_URI."domain");
				$ret[$subprop] = $prop;
			}
		}
		return $ret;
	}

	function getClassPropertiesAndRanges($cls) {
		$ret = array();
		$result = $this->RDQL("SELECT ?x, ?y WHERE ( ?x rdfs:domain $cls ) ( ?x rdfs:range ?y ) ", $cls, true);
		foreach($result as $res) {
			if($res["?x"]->uri && $res["?y"]->uri)
				$ret[$res["?x"]->uri] = $res["?y"]->uri;
		}
		return $ret;
	}

	function getPropertyValues($item, $property, $resource_return=false) {
		$ret = array();
		/*list($item, $using1) = $this->getRDQLUsingClause($item);
		list($property, $using2) = $this->getRDQLUsingClause($property);
		$query = "SELECT ?x WHERE ( $item, $property, ?x ) USING ".$using1.$using2;
		$result = $this->model->rdqlQuery($query);
		#print_r($result);
		foreach($result as $res) {
			if($res["?x"]) {
				if($resource_return) array_push($ret,$res["?x"]);
				else array_push($ret,$res["?x"]->uri);
			}
		}*/
		if(is_string($item)) $item = new Resource($item);
		if(is_string($property)) $property = new Resource($property);
		$triples = $this->getResourceTriples($item, $property, null);
		foreach($triples as $triple) {
			$res = $triple->obj;
			if($resource_return) array_push($ret,$res);
			else array_push($ret,$res->uri);
		}
		return $ret;
	}

	function getPropertyValue($item, $property, $resource_return=false) {
		$vals = $this->getPropertyValues($item, $property, $resource_return);
		if(sizeof($vals) > 0) return $vals[0];
		else return null;
	}

	function getTriples($subj, $pred, $obj) {
		if($subj) $subj = new Resource($subj);
		if($pred) $pred = new Resource($pred);
		if($obj) $obj = new Resource($obj);
		if($this->is_resmodel) $result = $this->model->model->find($subj, $pred, $obj);
		else $result = $this->model->find($subj, $pred, $obj);
		return $result->triples;
	}

	function getResourceTriples($subj, $pred, $obj) {
		if($this->is_resmodel) $result = $this->model->model->find($subj, $pred, $obj);
		else $result = $this->model->find($subj, $pred, $obj);
		return $result->triples;
	}

	function createList($items) {
		if(!$this->is_resmodel) return null;

		$list = $this->model->createList();
		foreach($items as $item) {
			$list->add($this->model->_node2ResNode($item));
		}
		return $this->model->_resNode2Node($list);
	}
	
}

?>
