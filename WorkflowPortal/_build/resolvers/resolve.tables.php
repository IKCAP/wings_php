<?php
if ($object->xpdo) {
	switch ($options[xPDOTransport::PACKAGE_ACTION]) {
		case xPDOTransport::ACTION_INSTALL:
			$modelPath = MODX_CORE_PATH.'components/workflow_portal/model/';
			$object->xpdo->addPackage('workflow_portal',$modelPath);
			$m = $object->xpdo->getManager();
			$m->createObjectContainer('workflow_runs');
			break;
		case xPDOTransport::ACTION_UPGRADE:
			break;
	}
}
return true;
?>
