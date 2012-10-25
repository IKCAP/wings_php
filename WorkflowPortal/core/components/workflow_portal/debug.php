<?php
ini_set('display_errors', '1');

require_once dirname(dirname(dirname(dirname(__FILE__)))) . '/_build/build.config.php';
include_once MODX_CORE_PATH . 'model/modx/modx.class.php';
$modx= new modX();
$modx->initialize('web');