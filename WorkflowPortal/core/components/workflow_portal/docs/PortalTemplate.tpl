<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
  <meta http-equiv="content-type" content="text/html; charset=utf-8" />
  <meta name="description" content="Your description goes here" />
  <meta name="keywords" content="your,keywords,goes,here" />
  <meta name="author" content="Your Name" />
  <base href="[[++site_url]]"></base>
  <link rel="stylesheet" type="text/css" href="[[++site_url]]/assets/components/workflow_portal/min/index.php?g=portalcss" />
  <title>[[*pagetitle]]</title>
</head>

<body>
<div id="wrap">
   <div id="header">
    <div id="header-left">
        <div id="title">
           <h1><a href="[[++site_url]]">[[++site_name]]</a></h1>
           <p style='margin-bottom:5px;font-size:13px'>[ [[*longtitle]] ]</p>
        </div>
    </div>
    <div id="header-right">
        <div id="login">[[!Login? &tpl=`HeaderLogin` &loginhomeid=`[[++site_start]]`!]]</div>
    </div>
   </div>
   <div class="menu">[[Wayfinder?startId=`0`&rowTpl=`row`]]</div>

   <div id="maincontent">
        [[*content]]
   </div>

   <div id="footer">
        [[$footer]]
        &copy; 
        <a href="http://www.isi.edu">USC/ISI</a> | 
        <a href="http://wings.isi.edu">Wings Project</a> |
        <a href="mailto:varunr@isi.edu">Contact</a>
   </div>
</div>
</body>
</html>
