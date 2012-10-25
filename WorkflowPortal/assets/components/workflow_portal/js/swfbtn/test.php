<?php session_start(); ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<link rel="stylesheet" type="text/css" href="extjs/resources/css/ext-all.css"></link>
<script type="text/javascript" src="extjs/adapter/ext/ext-base-debug.js"></script>
<script type="text/javascript" src="extjs/ext-all-debug.js"></script>
<script type="text/javascript" src="SwfUpload/swfupload.js"></script>
<script type="text/javascript" src="SwfUpload/plugins/swfupload.queue.js"></script>
<script type="text/javascript" src="./swfbtn.js"></script>
<script type="text/javascript">

Ext.onReady( function() {
        var b1 = new Ext.ux.swfbtn({
        id: 'button1',
        text: 'Upload file',
        hidden: false,
        disabled: false,
        iconpath: 'extjs/resources/images/icons/',
        isSingle: true,
        postparams: {session_id: '<?php echo session_id(); ?>', Type: 'P'},
        customparams: [{title: 'Description', maxwidth: 50, label: 'Image Description:'}, { title: 'fadeimage', label: 'Fade Image', fieldtype: 'checkbox'}],
        buttonimageurl: './SwfUpload/images/FullyTransparent_65x29.png',
        uploadurl: 'testbackend.php',
        flashurl: './SwfUpload/Flash/swfupload.swf'
    });

    var b2 = new Ext.ux.swfbtn({
        id: 'button2',
         text: 'Upload files',
         hidden: false,
         disabled: false,
         iconpath: 'extjs/resources/images/icons/',
         postparams: { session_id: '<?php echo session_id(); ?>', postparamsFixedVariable: 'somevalue'},
         customparams: [
            {title: 'firstname', maxwidth: '25', label: 'First Name'},
            {title: 'lastname', maxWidth: 50, label: 'Last Name'}
         ],
         buttonimageurl: './SwfUpload/images/FullyTransparent_65x29.png',
         uploadurl: 'testbackend.php',
         flashurl: './SwfUpload/Flash/swfupload.swf'
    });
    var p = new Ext.Window({
        title: 'Test Panel',
        layout: 'fit',
        bodyStyle: 'padding: 10px;',
        buttonAlign: 'center',
        closeable: false,
        autoHeight: false,
        autoWidth: false,
        height: 100,
        width: 200,
        autoScroll: true,
        html: 'Hello world.',
        bbar: [b1, b2]
    });

    p.show();
});
</script>
<style type="text/css">
.swfupload {
        position: absolute;
        z-index: 1;
}
</style>
</head>
<body>
        <div id="button"></div>
</body>
</html>