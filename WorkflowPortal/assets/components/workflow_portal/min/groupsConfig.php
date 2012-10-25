<?php
/**
 * Groups configuration for default Minify implementation
 * @package Minify
 */

/** 
 * You may wish to use the Minify URI Builder app to suggest
 * changes. http:/yourdomain/min/builder/
 **/

return array(
	'portaljs' => array('../js/util/Ext.ux.util.js', '../js/util/humanSort.js', '../js/gui/SeedForm.js', '../js/gui/template/Canvas.js', '../js/gui/template/port.js', '../js/gui/template/shape.js', '../js/gui/template/link.js', '../js/gui/template/node.js', '../js/gui/template/variable.js', '../js/gui/template/template.js', '../js/gui/template/layout.js', '../js/gui/TemplateBrowser.js', '../js/gui/TemplateGraph.js', '../js/gui/DataViewer.js', '../js/gui/ComponentViewer.js', '../js/gui/WingsMessages.js', '../js/gui/ResultBrowser.js', '../js/gui/DomainViewer.js'),
   'tellmejs' => array('../js/gui/tellme/tellme.js', '../js/gui/tellme/tellme_history.js', '../js/beamer/ControlList.js', '../js/beamer/MatchedTask.js', '../js/beamer/Paraphrase.js', '../js/beamer/Task.js', '../js/beamer/TodoItemMatches.js', '../js/beamer/TodoListParser.js', '../js/beamer/Token.js', '../js/beamer/Trie.js'),
	//'tellmejs' => array('../js/util/Ext.ux.util.js'),
	'portalcss' => array('../css/menu_style.css', '../css/andreas00.css', '../css/workflow_gallery.css' )
);
