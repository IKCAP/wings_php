<?php
/**
 * @package workflow_portal
 */
class workflow_runs extends xPDOSimpleObject {
    function workflow_runs(& $xpdo) {
        $this->__construct($xpdo);
    }
    function __construct(& $xpdo) {
        parent :: __construct($xpdo);
    }
}
?>