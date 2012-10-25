<?php
/**
 * Workflow Portal Build Script
 *
 * @package workflow_portal
 * @version 0.1
 * @release alpha
 * @author Varun Ratnakar <varunr@isi.edu>
 */
$mtime = microtime();
$mtime = explode(" ", $mtime);
$mtime = $mtime[1] + $mtime[0];
$tstart = $mtime;
/* Get rid of time limit. */
set_time_limit(0);

/* set package defines */
define('PKG_ABBR','workflow_portal');
define('PKG_NAME', 'workflow_portal');
define('PKG_VERSION','tellme_0.3.1');
define('PKG_RELEASE','');

/* Set directories for source files. */
$root = dirname(dirname(__FILE__)) . '/';
$sources= array (
    'root' => $root,
    'build' => $root . '_build/',
    'lexicon' => $root . '_build/lexicon/',
	'data' => $root . '_build/data/',
	'resolvers' => $root . '_build/resolvers/',
    'core' => $root . 'core/components/'.PKG_ABBR,
	'assets' => $root . 'assets/components/'.PKG_ABBR,
    'docs' => $root . 'core/components/'.PKG_ABBR.'/docs/'
);
unset($root);

/* Include Build Config */
require_once $sources['build'].'build.config.php';


/* Initialize ModX */
require_once MODX_CORE_PATH . 'model/modx/modx.class.php';

$modx= new modX();
$modx->initialize('mgr');
$modx->setLogLevel(MODX_LOG_LEVEL_INFO);
$modx->setLogTarget(XPDO_CLI_MODE ? 'ECHO' : 'HTML');


/* Load the Package Builder and create the package */
$modx->loadClass('transport.modPackageBuilder','',false, true);
$builder = new modPackageBuilder($modx);
$builder->createPackage(PKG_ABBR,PKG_VERSION,PKG_RELEASE);
$builder->registerNamespace(PKG_ABBR,false,true,'{core_path}components/'.PKG_ABBR.'/');


/* Create Snippets */
$c1= $modx->newObject('modSnippet');
$c1->set('name', PKG_ABBR.'_accessResults');
$c1->set('description', '<strong>0.1</strong> This provides a snippet to access Wings and Pegasus execution results');
$c1->set('snippet', file_get_contents($sources['core'] . '/access_results.snippet.php'));

$c2= $modx->newObject('modSnippet');
$c2->set('name', PKG_ABBR.'_workflowBrowser');
$c2->set('description', '<strong>0.1</strong> This provides a snippet to browse Wings workflows');
$c2->set('snippet', file_get_contents($sources['core'] . '/workflow_browser.snippet.php'));

$c3= $modx->newObject('modSnippet');
$c3->set('name', PKG_ABBR.'_manageData');
$c3->set('description', '<strong>0.1</strong> This provides a snippet to manage Wings data');
$c3->set('snippet', file_get_contents($sources['core'] . '/manage_data.snippet.php'));

$c4= $modx->newObject('modSnippet');
$c4->set('name', PKG_ABBR.'_workflowRunner');
$c4->set('description', '<strong>0.1</strong> This provides a snippet to run Wings workflows');
$c4->set('snippet', file_get_contents($sources['core'] . '/workflow_runner.snippet.php'));

$c5= $modx->newObject('modSnippet');
$c5->set('name', PKG_ABBR.'_manageComponents');
$c5->set('description', '<strong>0.1</strong> This provides a snippet to manage Wings components');
$c5->set('snippet', file_get_contents($sources['core'] . '/manage_components.snippet.php'));

$c6= $modx->newObject('modSnippet');
$c6->set('name', PKG_ABBR.'_templateOperations');
$c6->set('description', '<strong>0.1</strong> This provides a snippet to do some template operations');
$c6->set('snippet', file_get_contents($sources['core'] . '/template_operations.snippet.php'));

$c7= $modx->newObject('modSnippet');
$c7->set('name', PKG_ABBR.'_manageDomain');
$c7->set('description', '<strong>0.1</strong> This provides a snippet to do manage Wings domains');
$c7->set('snippet', file_get_contents($sources['core'] . '/manage_domain.snippet.php'));

/* Create the Templates */
$t= $modx->newObject('modTemplate');
$t->set('templatename', 'Andreas');
$t->set('description', '<strong>0.1</strong> This provides the default Workflow Portal Template');
$t->set('content', file_get_contents($sources['docs'] . '/PortalTemplate.tpl'));

$t2= $modx->newObject('modTemplate');
$t2->set('templatename', 'Plain');
$t2->set('description', '<strong>0.1</strong> This provides a plain Template without the header and footer');
$t2->set('content', file_get_contents($sources['docs'] . '/PlainTemplate.tpl'));

/* Create a category */
$category= $modx->newObject('modCategory');
$category->set('id','1');
$category->set('category',PKG_ABBR);

$category->addMany($c1);
$category->addMany($c2);
$category->addMany($c3);
$category->addMany($c4);
$category->addMany($c5);
$category->addMany($c6);
$category->addMany($c7);
$category->addMany($t);
$category->addMany($t2);

/* create category vehicle */
$attr = array(
    xPDOTransport::UNIQUE_KEY  => 'category',
    xPDOTransport::PRESERVE_KEYS => false,
    xPDOTransport::UPDATE_OBJECT => true,
    xPDOTransport::RELATED_OBJECTS => true,  
    xPDOTransport::RELATED_OBJECT_ATTRIBUTES => array (  
        'modSnippet' => array(  
            xPDOTransport::PRESERVE_KEYS => true,  
            xPDOTransport::UPDATE_OBJECT => true,  
            xPDOTransport::UNIQUE_KEY => 'name',  
        ),
        'modTemplate' => array(  
            xPDOTransport::PRESERVE_KEYS => true,  
            xPDOTransport::UPDATE_OBJECT => true,  
            xPDOTransport::UNIQUE_KEY => 'templatename',  
        ),
        'modeMenu' => array(  
            xPDOTransport::PRESERVE_KEYS => true,  
            xPDOTransport::UPDATE_OBJECT => true,  
            xPDOTransport::UNIQUE_KEY => 'text',  
        ),
        'modAction' => array(  
            xPDOTransport::PRESERVE_KEYS => true,  
            xPDOTransport::UPDATE_OBJECT => true,  
            xPDOTransport::UNIQUE_KEY => array ('namespace','controller'),  
        ),
    )  
);
$vehicle = $builder->createVehicle($category,$attr);

/* Copy files over */
$vehicle->resolve('file',array(
    'source' => $sources['core'],
    'target' => "return MODX_CORE_PATH . 'components/';",
));

$vehicle->resolve('file',array(
    'source' => $sources['assets'],
    'target' => "return MODX_ASSETS_PATH . 'components/';",
));

$vehicle->resolve('php',array(
     'source' => $sources['build']. 'install-script.php'
));

$vehicle->resolve('php',array(
     'source' => $sources['resolvers']. 'resolve.tables.php'
));

/* Add Vehicle to Package */
$builder->putVehicle($vehicle);
unset($vehicle);


/* Create the Menu and its Vehicle */

$menu= $modx->newObject('modMenu');
$menu->fromArray(array(
    'parent' => 'components', 'text' => 'workflow_portal',
    'description' => 'workflow_portal_desc'
),'',true,true);

$action= $modx->newObject('modAction');
$action->fromArray(array(
    'namespace' => PKG_ABBR,
    'parent' => 0, 'controller' => 'configure',
    'lang_topics' => PKG_ABBR.':default',
),'',true,true);

$menu->addOne($action);

$vehicle= $builder->createVehicle($menu,array (
    xPDOTransport::PRESERVE_KEYS => true,
    xPDOTransport::UPDATE_OBJECT => true,
    xPDOTransport::UNIQUE_KEY => 'text',
    xPDOTransport::RELATED_OBJECTS => true,
    xPDOTransport::RELATED_OBJECT_ATTRIBUTES => array (
        'Action' => array (
            xPDOTransport::PRESERVE_KEYS => true,
            xPDOTransport::UPDATE_OBJECT => true,
            xPDOTransport::UNIQUE_KEY => array ('namespace','controller'),
        ),
    ),
));

/* Add the menu vehicle to package */
$builder->putVehicle($vehicle);
unset($vehicle,$action);


/* Load lexicon strings */
//- Dont automatically now
//$builder->buildLexicon($sources['lexicon']);

/* Include readme, license, and/or an html file that interacts with the user during the install.
 * Each array member is optional but you should always include a readme.txt file.
 */
$builder->setPackageAttributes(array(
    'readme' => file_get_contents($sources['docs'] . 'readme.txt'),
    'license' => file_get_contents($sources['docs'] . 'license.txt')
    //'setup-options' => file_get_contents($sources['build'] . 'user-input.html')
));

/*  zip up the package */
$builder->pack();

$mtime= microtime();
$mtime= explode(" ", $mtime);
$mtime= $mtime[1] + $mtime[0];
$tend= $mtime;
$totalTime= ($tend - $tstart);
$totalTime= sprintf("%2.4f s", $totalTime);

$modx->log(MODX_LOG_LEVEL_INFO, "Package Built Successfully.");
$modx->log(MODX_LOG_LEVEL_INFO, "Execution time: {$totalTime}");
exit();
