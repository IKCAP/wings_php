<?php
/**
 * @package workflow_portal
 */
require_once (strtr(realpath(dirname(dirname(__FILE__))), '\\', '/') . '/workflow_runs.class.php');
class workflow_runs_mysql extends workflow_runs {
    function workflow_runs_mysql(& $xpdo) {
        $this->__construct($xpdo);
    }
    function __construct(& $xpdo) {
        parent :: __construct($xpdo);
    }
}
?>