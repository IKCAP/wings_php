<?php

$tomcat = "localhost:8080";
$wingsdir = "Wings";
if(isset($CONFIG)) {
	$tomcat = preg_replace('/^http:\/\//', '', $CONFIG['system']['tomcat_server']);
	$tomcat = preg_replace('/\/$/', '', $tomcat);
	if(preg_match('/(.+?)\/(.*)$/', $tomcat, $m)) {
		$tomcat = $m[1];
		$wingsdir = $m[2];
	}
}

define ("JAVA_HOSTS", $tomcat);
define ("JAVA_SERVLET", "/$wingsdir/servlet.phpjavabridge");
define ("JAVA_PREFER_VALUES", 1);
define ("JAVA_LOG_LEVEL", 0);

require_once(PORTAL_INCLUDES_PATH."java/Java.inc");

function getWorkflowGenerator($itemid, $requestid, $isTemplate, $complibname) {
	$conf = USER_HOME_PATH."wings.properties";
	$wp = "edu.isi.ikcap.wings";
	//flushInit();
	
  	$propHelper = new JavaClass("$wp.workflows.util.PropertiesHelper");
	$propHelper->resetProperties();
	$propHelper->disableLogging();
	//println("Initialized Properties.. $conf");

	$awg = new Java("$wp.AWG", $itemid, $requestid, $conf, $isTemplate);
	//println("Initialized Java Bridge to AWG..");

  	$dc = $awg->initializeDC();
  	$pc = $awg->initializePC($complibname);
  	$awg->initializeWorkflowGenerator();
	//println("Initialized DC/PC..");
	//println("Reading Item..");

  	$awg->initializeItem();
	//println("Finished Reading $itemid..");
	return $awg;
}

function getInferredTemplate($awg) {
 	$swg = $awg->getSWG();
	$swg->addExplanation("INFO: Elaborating Template");

	$t = $awg->getTemplate();
	$it = $swg->getInferredTemplate($t);
	return $it;
}

function getSpecializedTemplates($awg) {
 	$swg = $awg->getSWG();
	$seed = $awg->getSeed();
	$cdts = $swg->findCandidateSeeds($seed);

	$sts = array();
	foreach($cdts as $cdt) {
		$swg->addExplanation("INFO: Elaborating Template");
		$icdt = $swg->getInferredTemplate($cdt);
		if(!$icdt) {
			$swg->addExplanation("ERROR: Cannot Elaborate Template");
			return $sts;
		}
		$swg->addExplanation("INFO: Selecting Components (Specializing Template)");
		$child_sts = $swg->specializeTemplates($icdt);
		foreach($child_sts as $child_st) {
			array_push($sts, $child_st);
		}
		$len = sizeof($sts);
		$swg->addExplanation( ($len?"INFO":"ERROR"). ": Got $len Specialized Templates");
	}
	return $sts;
}

function getBoundTemplates($awg, $sts) {
 	$swg = $awg->getSWG();
	$bts = array();
	$swg->addExplanation("INFO: Selecting Data Objects (Binding Template)");
	foreach($sts as $st) {
		$child_bts = $swg->selectInputDataObjects($st);
		foreach($child_bts as $child_bt) {
			array_push($bts, $child_bt);
		}
	}
	$swg->setDataMetricsForInputDataObjects($bts);
	$len = sizeof($bts);
	$swg->addExplanation( ($len?"INFO":"ERROR"). ": Got $len Bound Templates");
	//showTemplateList($bts, "Bound");
	return $bts;
}

function getConfiguredTemplates($awg, $bts) {
 	$swg = $awg->getSWG();
	$cts = array();
	$swg->addExplanation("INFO: Selecting Parameters (Configuring Template)");
	foreach($bts as $bt) {
		$insts = $swg->configureTemplates($bt);
		foreach($insts as $inst) {
			if($inst->getRules() && $inst->getRules()->getRulesText()) {
				$inst = $inst->applyRules();
				if(!$inst) {
					$swg->addExplanation("ERROR: Template does not comply with Template Rules. Dropping Template.");
					continue;
				}
			}
			array_push($cts, $inst);
		}
	}
	$len = sizeof($cts);
	$swg->addExplanation( ($len?"INFO":"ERROR"). ": Got $len Configured Templates");
	//showTemplateList($cts, "Configured");
	return $cts;
}

function getExecutionScripts($awg, $cts) {
 	$swg = $awg->getSWG();
	$execs = array();
	$i = 1;
	foreach($cts as $ct) {
		$dax = $swg->getTemplateDAX($ct);
		if($dax) {
			$dax->setInstanceId($ct->getID());
			$dax->setIndex($i);
			$dax->fillInputMaps($ct->getInputVariables());
			$dax->fillIntermediateMaps($ct->getIntermediateVariables());
			$dax->fillOutputMaps($ct->getOutputVariables());
			array_push($execs, $dax);
			$i++;
		}
	}
	foreach ($execs as $ex) {
		$ex->setTotalDaxes(sizeof($execs));
	}
	return $execs;
}

function writeExecutionScripts($execs, $requestId) {
	$outputDir = USER_HOME_PATH.OUTPUT_DIR . "/" . $requestId;
	mkdir($outputDir);
	foreach ($execs as $exec) {
		$file = java_values($exec->getFile()->toString());
		$execFile = $outputDir . "/" . $file;

		$execData = $exec->getString();
		$fh = fopen($execFile, "w");
		fwrite($fh, $execData);
		fclose($fh);
		chmod($execFile, 0755);

		$mapData = $exec->getVariableBindingMapString();
		$fh = fopen($execFile.".map", "w");
		fwrite($fh, $mapData);
		fclose($fh);
	}
}

function writeConfiguredTemplates($cts, $requestId) {
	$outputDir = USER_HOME_PATH.OUTPUT_DIR . "/" . $requestId;
	mkdir($outputDir);
	$i = 0;
	foreach ($cts as $ct) {
		$i++;
		$file = $requestId."_".$i.".owl";
		$ctFile = $outputDir . "/" . $file;

		$ctJSON = getWingsTemplateJSON($ct);
		$fh = fopen($ctFile, "w");
		fwrite($fh, $ctJSON);
		fclose($fh);
	}
}

function getDataBindings($bts) {
	$bindings = array();
	$bstrs = array();
	foreach($bts as $bt) {
		$tmpBindings = array();
		foreach ($bt->getInputVariables() as $iv) {
			if ($iv->isDataVariable()) {
				$ivname = java_values($iv->getName()->toString());
				$ivb = array();
				foreach($iv->getBinding() as $b) 
					array_push($ivb, java_values($b->toString()));
				if(!sizeof($ivb))
					array_push($ivb, java_values($iv->getBinding()->getName()));
				$tmpBindings[$ivname] = $ivb;
			}
		}
		ksort($tmpBindings);

		$bstr = "";
		foreach ($tmpBindings as $k=>$v) 
			$bstr .= implode($v, ",")."|";

		if(!array_key_exists($bstr, $bstrs)) {
			array_push($bindings, $tmpBindings);
		}
		$bstrs[$bstr] = 1;
	}
	return $bindings;
}

function getParameterBindings($cts) {
	$bindings_b = array();
	foreach($cts as $ct) {
		$binding_b = array();
		foreach ($ct->getInputVariables() as $iv) {
			if ($iv->isParameterVariable() && $iv->getBinding()) {
				$ivname = java_values($iv->getName()->toString());
				$binding_b[$ivname] = $iv->getBinding();
			}
		}
		array_push($bindings_b, $binding_b);
	}

	$bindings = array();

	// FIXME: Expanding parameter collection into multiple configs.. (cannot handle parameter collections right now) 
	$bstrs = array();
	while(sizeof($bindings_b)) {
		$hasSets = false;
		$binding_b = array_pop($bindings_b);
		$binding = array();
		foreach($binding_b as $v=>$b) {
			if ($b->isSet()) {
				foreach ($b as $cb) {
					$binding_x = array();
					foreach($binding_b as $v1=>$b1) $binding_x[$v1] = $b1;
					$binding_x[$v] = $cb;
					array_push($bindings_b, $binding_x);
				}
				$hasSets = true;
			} else
				$binding[$v] = array(java_values($b->toString()));
		}
		if(!$hasSets) {
			ksort($binding);

			$bstr = "";
			foreach ($binding as $k=>$v) 
				$bstr .= implode($v, ",")."|";

			if(!array_key_exists($bstr, $bstrs)) {
				array_push($bindings, $binding);
			}
			$bstrs[$bstr] = 1;
		}
	}
	return $bindings;
}

function getExplanations($awg) {
	$explanations = array();
	if(!$awg) return $explanations;
 	$swg = $awg->getSWG();
	foreach ($swg->getExplanations() as $exp) {
		$exp = preg_replace('/\^\^[^\s]+\s/',' ', $exp);
		$exp = preg_replace('/\^\^[^\s]+$/',' ', $exp);
		$exp = preg_replace('/\s+[^\s]+#/',' ', $exp);
		$exp = preg_replace('/^.+#/',' ', $exp);
		$exp = preg_replace("/\s+'\s+/",' ', $exp);
		$exp = preg_replace("/'/",'', $exp);
		$exp = preg_replace('/ \{ \} /',' ', $exp);
		$exp = preg_replace('/ \{ \}$/',' ', $exp);
		$exp = preg_replace('/^ERROR :/','ERROR:', $exp);
		$exp = preg_replace('/\s+$/','', $exp);
		if(!in_array($exp, $explanations))
			array_push($explanations, $exp);
	}
	return $explanations;
}

/*function runWingsTest($seedid) {
	try {
		$awg = getWorkflowGenerator($seedid, $seedid, false);
		$sts = getSpecializedTemplates($awg);
		$bts = getBoundTemplates($awg, $sts);
		$cts = getConfiguredTemplates($awg, $bts);
	} catch (JavaException $ex) {
		println("An exception occured: <br>$ex");
	}
}

function getElaboratedTemplateFromWings($tid) {
	try {
		$awg = getWorkflowGenerator($tid, $tid, true);
  		$swg = $awg->getSWG();
		$t = $awg->getTemplate();
		$json = getWingsTemplateJSON($t);
		println($json);
		println("Inferring Elaborated Template..");
		$it = $swg->getInferredTemplate($t);
		$json = getWingsTemplateJSON($it);
		println($json);
	} catch (JavaException $ex) {
		println("An exception occured: <br>$ex");
	}
}*/


function parseWingsMetricsXML($mxml) {
	$cons = array();

	$metric = new SimpleXMLElement($mxml);
	foreach($metric->Metric as $m) {
		$mn = $m['name'];
		$prop = $m->Dimension['name'];
		if($mn == 'rdfMetrics') $prop = "rdf:".$prop;
		else $prop = "dcdom:".$prop;

		foreach($m->Dimension->Value as $val) {
			if($mn != 'dataMetrics') $val = "dcdom:".$val;
			array_push($cons, array('p'=>$prop, 'o'=>(string)$val));
		}
	}
	return $cons;
}

function convertWingsComponentBinding($b) {
	if(!$b) return null;
	if($b->isSet()) {
		$arr = array();
		foreach($b as $cb)
			array_push($arr, convertWingsComponentBinding($cb));
		return $arr;
	}
	else {
		$arr = array();
		$pblist = $b->getData();
		$pb = $pblist->getPortBinding();
		foreach($pb->keySet() as $port) {
			$role = java_values($port->getRole()->getName());
			$binding = $pb->get($port);
			$arr[$role] = convertWingsJavaBinding($binding, true);
		}
		return $arr;
	}
}

function convertWingsJavaBinding($b, $disable_metrics=false) {
	if(!$b) return null;
	if($b->isSet()) {
		$arr = array();
		foreach($b as $cb) {
			array_push($arr, convertWingsJavaBinding($cb, $disable_metrics));
		}
		return $arr;
	}
	else {
		if(!$disable_metrics) {
			$mxml = java_values($b->getMetrics());
			if($mxml) {
				return array("id"=>java_values($b->toString()), "m"=>parseWingsMetricsXML($mxml));
			}
		}
		return java_values($b->toString());
	}
}

function getWingsTemplateJSON($t) {
	$varDims = array();
	if(!$t) return null;
	$ivars = $t->getInputVariables();
	foreach($ivars as $ivar) {
		$ivarid = java_values($ivar->getID());
		$irole = $t->getInputRoleForVariable($ivar);
		if($irole) $varDims[$ivarid] = java_values($irole->getDimensionality());
	}

  	$varids = new Java("java.util.ArrayList");
	$vars = $t->getVariables();
	foreach($vars as $var) {
		$varids->add($var->getID());
	}

	$nodes = array();
	$links = array();
	$variables = array();
	foreach($t->getNodes() as $node) {
		$n = array();

		$n['id'] = java_values($node->getName());
		$cruleObj = $node->getComponentSetRule();
		$pruleObj = $node->getPortSetRule();
		$crule = $cruleObj ? java_values($cruleObj->getType()) : 'WTYPE';
		$prule = $pruleObj ? java_values($pruleObj->getType()) : 'WTYPE';
		$pruleOp = $pruleObj ? java_values($pruleObj->getSetExpression()->getOperator()) : 'cross';
		$c = $node->getComponentVariable();
		$n['component'] = array(
			"name"=>java_values($c->getName()),
			"ex"=>java_values($c->isConcrete()),
			"type"=>java_values($c->getComponentTypeName()),
			"binding"=>convertWingsJavaBinding($c->getBinding()),
			"binding_details"=>convertWingsComponentBinding($c->getBinding())
		);
		$n["crule"]=($crule=='WTYPE' ? 'W':'S');
		$n["prule"]=($prule=='WTYPE' ? 'W':'S');
		$n["pruleOp"]=($pruleOp=='XPRODUCT' ? 'cross':'dot');
		$n['x']=0; $n['y']=0;
		$comment = java_values($node->getComment());
		if(preg_match('/x=([\d\.]+),y=([\d\.]+)/', $comment, $m)) {
			$n['x']=$m[1]; 
			$n['y']=$m[2];
		}
		array_push($nodes, $n);
	}
	foreach($t->getVariables() as $v) {
		$var = array();
		$var['id'] = java_values($v->getName());
		if($v->getBinding()) {
			$var['binding'] = convertWingsJavaBinding($v->getBinding());
		}
		$comment = java_values($v->getComment());
		$var['x']=0; $var['y']=0;
		if(preg_match('/x=([\d\.]+),y=([\d\.]+)/', $comment, $m)) {
			$var['x']=$m[1]; 
			$var['y']=$m[2];
		}
		$var['type'] = "DATA";
		if($v->isParameterVariable()) $var['type'] = "PARAM";

		if($varDims[java_values($v->getID())]) 
			$var['dim'] = $varDims[java_values($v->getID())];
		array_push($variables, $var);
	}
	foreach($t->getLinks() as $link) {
		$l = array();
		$l["variable"] = java_values($link->getVariable()->getName());
		$fromPort = $link->getOriginPort();
		$toPort = $link->getDestinationPort();
		if($fromPort) {
			$l["fromNode"] = java_values($link->getOriginNode()->getName());
			$l["fromPort"] = array(
				"id" => java_values($fromPort->getName()),
				"role" => array(
					"id"=>java_values($fromPort->getRole()->getName()),
					"dim"=>java_values($fromPort->getRole()->getDimensionality()),
				)
			);
		}
		if($toPort) {
			$l["toNode"] = java_values($link->getDestinationNode()->getName());
			$l["toPort"] = array(
				"id" => java_values($toPort->getName()),
				"role" => array(
					"id"=>java_values($toPort->getRole()->getName()),
					"dim"=>java_values($toPort->getRole()->getDimensionality()),
				)
			);
		}
		array_push($links, $l);
	}

	$e = $t->getConstraintEngine();
	$nsmap = java_values($e->getPrefixNSMap());
	$rns = array_flip($nsmap);
	$rns[java_values($t->getNamespace())]='';
	$constraints = java_values($e->getConstraints($varids));
	$cons = array();
	foreach ($constraints as $con) {
		array_push($cons, 
			array("s"=>getPrefixedTerm($con->getSubject(),$rns),
					"p"=>getPrefixedTerm($con->getPredicate(),$rns),
					"o"=>getPrefixedTerm($con->getObject(),$rns)));
	}

	$m = $t->getMetadata();
	$json = json_encode(array(
			"nsmap"=>$nsmap, 
			"nodes"=>$nodes, 
			"variables"=>$variables, 
			"links"=>$links, 
			"constraints"=>$cons, 
			"metadata"=>array(
				"documentation"=>java_values($m->getDocumentation()),
				"lastupdate"=>$m->getLastUpdateTime() ? java_values($m->getLastUpdateTime()->getTime()->toString()) : null,
				"contributor"=>java_values($m->getContributors()->toString()),
			)
	));
	return preg_replace('/\[\[/','[ [',$json); // MODX treats [[ in a special way, so we have to avoid that
}

function getPrefixedTerm($kbobject, $rns) {
	if($kbobject->isLiteral()) {
		$val = java_values($kbobject->getValue());
		$dtype = java_values($kbobject->getDataType());
		$dtype = preg_replace('/^.+#/', 'xsd:', $dtype);
		if($dtype == "xsd:boolean") {
			if($val) $val = "true";
			else $val = "false";
		}
		return "\"$val\"^^$dtype";
	}
	else {
		$pfx = $rns[java_values($kbobject->getNamespace())];
		return $pfx. ($pfx ? ":" : "") . $kbobject->getName();
	}
}

function getTemplateListInfo($ts) {
	$tinfos = array();
	$i=0;
	foreach($ts as $t) {
		$i++;
		$info = getTemplateBindings($t);
		$info["index"] = $i;
		//$info["json"] = getWingsTemplateJSON($t);
		array_push($tinfos, $info);
	}
	return $tinfos;
}

/*function showTemplateList($ts, $title) {
	$len = sizeof($ts);
	println("");
	echo("<h4>$len $title Templates</h4>");
	foreach($ts as $t) {
		println("<b>-------- ".$t->getId()." --------</b>");
		$bindings = getTemplateBindings($t);
		echo("<u><i>Component Bindings:</i></u> <ul>\n$bindings[0]</ul>");
		echo("<u><i>Data Bindings:</i></u> <ul>\n$bindings[1]</ul>");
		echo("<u><i>Param Bindings:</i></u> <ul>\n$bindings[2]</ul>");
		//$json = getWingsTemplateJSON($t);
		//println($json);
		//print $t->deriveInternalRepresentation()."\n";
	}
	flush();
}*/

function getTemplateBindings($t) {
	$compBindings = array();
	$dataBindings = array();
	$paramBindings = array();
	$ivars = $t->getInputVariables();
	foreach ($ivars as $v) {
		$var = java_values($v->getName()->toString());
		if ($v->isDataVariable() && $v->getBinding()) {
			$dataBindings[$var] = preg_replace('/\[\[/','[ [',
				java_values($v->getBinding()->toString()));
		} else if ($v->isParameterVariable() && $v->getBinding()) {
			$paramBindings[$var] = preg_replace('/\[\[/','[ [',
				java_values($v->getBinding()->toString()));
		}
	}
	$nodes = $t->getNodes();
	foreach ($nodes as $n) {
		if ($n->getComponentVariable()) {
			$cname = java_values($n->getComponentVariable()->getComponentTypeName()->toString());
			if ($n->getComponentVariable()->getBinding()) {
				$compBindings[$cname] = preg_replace('/\[\[/','[ [',
					java_values($n->getComponentVariable()->getBinding()->toString()));
			}
		}
	}
	return array("comp"=>$compBindings, "data"=>$dataBindings, "param"=>$paramBindings);
}

function flushInit() {
	echo str_pad('',1024);  // minimum start for Safari
	echo "<br>\n";
}

function println($msg) {
	echo $msg."<br>\n";
	flush();
}

//runWings("testseed", "DMDomain", 1);
//runWings("test", "wordbags", 1);
//getElaboratedTemplateFromWings("English_Words", "wordbags", 1);
//getElaboratedTemplateFromWings("test", "DMDomain", 1);

/* 
	Functions for Daniel's OPM Operations that are now part of the Wings WAR file 
*/
function convertTemplateFileToOPMFile($filepath, $outfilepath) {
	$mapper = new Java("wings_opm_mapper.Mapper");
	$filetype = "RDF/XML"; # Need to detect if the template format is TTL or not
	$mapper->transformWingsElaboratedTemplateToOPM($filepath, $filetype, $outfilepath);
}

function convertRunFileToOPMFile($filepath, $outfilepath) {
	$mapper = new Java("wings_opm_mapper.Mapper");
	$filetype = "TTL";
	$mapper->transformWingsResultsToOPM($filepath, $filetype, $outfilepath);
}

function publishToTripleStore($filepath) {
	publishToTripleStoreVirtuoso($filepath);
	//publishToTripleStoreAllegro($filepath);
}

function publishToTripleStoreVirtuoso($filepath) {
	$server = "jdbc:virtuoso://wind.isi.edu:1111";
	$graph = "http://www.opmw.org/export/provenance";
	$user = "dba";
	$password = "admin123";
	$op = new Java("rdfuploadvirtuoso.UploadLocalRDFFileToVirtuoso");
	$op->loadRDFToRepository($filepath, "TURTLE", $graph, $server, $user, $password);
}

function publishToTripleStoreAllegro($filepath) {
	$server = "http://wind.isi.edu:10035";
	$cat = "java-catalog";
	$repo = "WINGSTemplatesAndResults";
	$user = "admin";
	$pass = "admin123";
	$filetype = "TTL";
	$baseuri = OPMW_EXPORT_SERVER; 
	$op = new Java("allegromodule.AllegroOperations", $server, $cat, $repo, $user, $pass);
	$op->loadRDFToRepositoryAsTurtle($filepath, $baseuri);
}

?>
