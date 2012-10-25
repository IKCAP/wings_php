<?php
require "../../../core/config/config.inc.php";

mysql_connect($database_server, $database_user, $database_password);
@mysql_select_db($dbase);

$query = "SELECT name, snippet FROM site_snippets WHERE name LIKE 'workflow_portal%'";
$result = mysql_query($query);
while($row = mysql_fetch_assoc($result)) {
	$file = strtolower(preg_replace("/([A-Z])/", "_$1", $row['name'])).".snippet.php";
	$file = str_replace("workflow_portal_", "", $file);
	print $row['name']." : $file\n";
	$fh = fopen($file, "w");
	fwrite($fh, "<?php\n".$row["snippet"]."\n?>\n");
	fclose($fh);
}
mysql_close();

?>
