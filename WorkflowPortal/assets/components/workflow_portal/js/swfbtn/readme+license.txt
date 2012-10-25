Unzip the archive into the directory that contains both extjs and SwfUpload directories or adjust 
parameters in test.php to match your layout

License: MIT - feel free to use, improve, mangle or whatever gets you through the night.

Idea for attaching to the swfupload to the button render event borrowed from another contributor
who's name I cannot locate at this time so I cannot give the earned credit deserved.  Sorry dude.

This extension supports a background store that will be reloaded on success if it is configured.
Also supports supplemental image information via either a configured form or through a dynamically
generated form defined by the customparams config option.  Form will be processed for each file 
uploaded and resulting data will be appended to configured postparams before upload so back end can
process the data as desired.

Tested and works with Ext-3.0, SwfUpload-2.2.0 and Flash 10.