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

function analyzeTOP($tid) {
	$conf = USER_HOME_PATH."wings.properties";
	$ik = "edu.isi.ikcap";
	$wp = "$ik.wings";
	$tm = "$ik.tellme";
	
  	$propHelper = new JavaClass("$wp.workflows.util.PropertiesHelper");
	$propHelper->resetProperties();
	$propHelper->disableLogging();
	$propHelper->loadWingsProperties($conf);

	$pcFac = $propHelper->getPCFactory();
	$pc = $pcFac->getPC(null);
	$tellMeKb = new Java("$tm.kb.TellMeKnowledgeBase", $pc);
	
	$typeFacClass = new JavaClass("$tm.kb.TellMeTypeFactory");
  	$ttrClass = new JavaClass("$tm.translation.TemplateToRoutine");
  	$rttClass = new JavaClass("$tm.translation.RoutineToTemplate");

	$typeFactory = $typeFacClass->getInstance();
	$assessor = new Java("$ik.routine.algorithm.assessment.RoutineAssessor", $tellMeKb, $tellMeKb, $tellMeKb, $typeFactory);

	$wfac = new Java("$wp.workflows.util.WflowGenFactory", 0);
	$domain = $propHelper->getTemplateDomain();
	$template = $wfac->getTemplate($domain, $tid.".owl");

	$routine = $ttrClass->makeRoutineFromTemplate($template, $tellMeKb, true);

	//return getLinkString($routine);
	$issues1 = $assessor->annotateAndAssessRoutine($routine, true);
	$routine = processRoutine($routine, $tellMeKb);
	$issues2 = $assessor->annotateAndAssessRoutine($routine, true);

	$issues1JSON = preg_replace('/\[\[/', '[ [', json_encode(parseTOPIssues($issues1)));
	$issues2JSON = preg_replace('/\[\[/', '[ [', json_encode(parseTOPIssues($issues2)));

	$template = $rttClass->makeTemplateFromRoutine($routine, $pc);
	return "{issues1:$issues1JSON, issues2:$issues2JSON, template:".getWingsTemplateJSON($template)."}";
}


function getLinkString($routine) {
	$tmp = "";
	$links = $routine->getDataFlowLinks();
	foreach($links as $l) {
		$sn = $l->getSourceNode();
		$dn = $l->getDestinationNode();
		if(!$l->isInputLink()) $tmp .= $sn->getName();
		$tmp .= "_>_";
		if(!$l->isOutputLink()) $tmp .= $dn->getName();
		$v = $l->getDataVariable();
		$tmp .= "(".$v->getInternalName().")";
		$tmp .= "[".($l->isInputLink() ? 'In' : ($l->isOutputLink() ? 'Out' : 'InOut'))."]";
		$tmp .= ", ";
	}
	return $tmp;
}

function fromCamelCase($str) {
	return implode(preg_split('/(?<=\\w)(?=[A-Z])/', $str), " ");
}

function parseTOPIssues($issues) {
	$nIssues = array();
	$i = 1;
	foreach($issues as $issue) {
		array_push($nIssues, $i." : ".fromCamelCase(java_values($issue->getName()->toString())));
		$i++;
	}
	return $nIssues;
}


function processRoutine($routine, $tellMeKb) {
  	$topAlgo = new JavaClass("edu.isi.ikcap.tellme.top.TopAlgorithm");
	$tightener = new Java("edu.isi.ikcap.routine.algorithm.tightening.Tightener", $tellMeKb, $tellMeKb, $tellMeKb);
	$nroutine = $topAlgo->doPostMacroTasks($routine, $tightener, $tellMeKb);
	return $nroutine;
}
?>
