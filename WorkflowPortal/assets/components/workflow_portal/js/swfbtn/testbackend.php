<?php
if (empty($_POST['session_id'])) {
	die("ERROR: session_id required");
}
session_id($_POST['session_id']);

echo "Posted parameters for upload are:<br>";
echo "<PRE>";
print_r($_POST);
echo "</PRE>";
exit();
die('{success: true, , msg: "Backend processor called successfully."}');
?>
