<?php
/**
 * @package workflow_portal
 */
$xpdo_meta_map['workflow_runs']= array (
  'package' => 'workflow_portal',
  'table' => 'workflow_portal_runs',
  'fields' => 
  array (
    'uid' => 0,
    'requestid' => NULL,
    'seedid' => NULL,
    'templateid' => NULL,
    'domain' => NULL,
    'start_time' => NULL,
    'end_time' => NULL,
    'status' => NULL,
    'running_job' => NULL,
    'numjobs_total' => NULL,
    'numjobs_executed' => NULL,
    'inputs' => NULL,
    'intermediates' => NULL,
    'outputs' => NULL,
    'log' => NULL,
  ),
  'fieldMeta' => 
  array (
    'uid' => 
    array (
      'dbtype' => 'int',
      'attributes' => 'unsigned',
      'phptype' => 'integer',
      'null' => true,
      'default' => 0,
    ),
    'requestid' => 
    array (
      'dbtype' => 'varchar',
      'precision' => '255',
      'phptype' => 'string',
      'null' => true,
    ),
    'seedid' => 
    array (
      'dbtype' => 'varchar',
      'precision' => '255',
      'phptype' => 'string',
      'null' => true,
    ),
    'templateid' => 
    array (
      'dbtype' => 'varchar',
      'precision' => '255',
      'phptype' => 'string',
      'null' => true,
    ),
    'domain' => 
    array (
      'dbtype' => 'varchar',
      'precision' => '255',
      'phptype' => 'string',
      'null' => true,
    ),
    'start_time' => 
    array (
      'dbtype' => 'datetime',
      'phptype' => 'datetime',
      'null' => true,
    ),
    'end_time' => 
    array (
      'dbtype' => 'datetime',
      'phptype' => 'datetime',
      'null' => true,
    ),
    'status' => 
    array (
      'dbtype' => 'enum',
      'precision' => ' \'SUCCESS\', \'FAILURE\', \'ONGOING\' ',
      'phptype' => 'string',
      'null' => true,
    ),
    'running_job' => 
    array (
      'dbtype' => 'text',
      'phptype' => 'string',
      'null' => true,
    ),
    'numjobs_total' => 
    array (
      'dbtype' => 'int',
      'attributes' => 'unsigned',
      'phptype' => 'integer',
      'null' => true,
    ),
    'numjobs_executed' => 
    array (
      'dbtype' => 'int',
      'attributes' => 'unsigned',
      'phptype' => 'integer',
      'null' => true,
    ),
    'inputs' => 
    array (
      'dbtype' => 'text',
      'phptype' => 'string',
      'null' => true,
    ),
    'intermediates' => 
    array (
      'dbtype' => 'text',
      'phptype' => 'string',
      'null' => true,
    ),
    'outputs' => 
    array (
      'dbtype' => 'text',
      'phptype' => 'string',
      'null' => true,
    ),
    'log' => 
    array (
      'dbtype' => 'text',
      'phptype' => 'string',
      'null' => true,
    ),
  ),
);
