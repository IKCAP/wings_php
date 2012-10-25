
function getUploader(filename, isZip, codeid, snippet_id, assets_url, upload_script) {

	return new SWFUpload ({
				// Backend settings
				post_params: {"PHPSESSID" : codeid},
				upload_url: assets_url+"swfupload/"+upload_script,
				file_post_name: filename,

				// Flash file settings
				file_size_limit : "3 GB",
				file_types : isZip?"*.zip":"*.*",			// or you could use something like: "*.doc;*.wpd;*.pdf",
				file_types_description : isZip?"Zip Files":"All Files",
				file_upload_limit : "0",
				file_queue_limit : "1",

				// Event handler settings
				swfupload_loaded_handler : swfUploadLoaded,
				
				file_dialog_start_handler: fileDialogStart,
				file_queued_handler : fileQueued,
				file_queue_error_handler : fileQueueError,
				file_dialog_complete_handler : fileDialogComplete,
				
				upload_start_handler : uploadStart,	
				upload_progress_handler : uploadProgress,
				upload_error_handler : uploadError,
				upload_success_handler : uploadSuccess,
				upload_complete_handler : uploadComplete,

				// Button Settings
				button_image_url : assets_url+"images/uploadbutton.png",
				button_placeholder_id : "button_"+snippet_id+"_"+filename,
				button_width : 61,
				button_height : 22,
				
				// Flash Settings
				flash_url : assets_url+"swfupload/swf/swfupload.swf",

				custom_settings : {
					progress_target : "progress_"+snippet_id+"_"+filename,
					upload_successful : false,
					upload_file_target : "txt_"+snippet_id+"_"+filename,
					snippet_id : snippet_id,
					wrapper_id : "file_"+snippet_id+"_"+filename,
					hidden_field : filename+"_file_"+snippet_id
				},
				
				// Debug settings
				debug: false
		});
}

// Helper function
function BindArgument(fn, arg) {
  return function (e) { return fn(e, arg); };
}


var formChecker = null;
function swfUploadLoaded() {
	var snipid = this.customSettings.snippet_id;
	var btnSubmit = document.getElementById("run_btn_"+snipid);
	btnSubmit.onclick = BindArgument(doSubmit, snipid);
}

function validateForm(snipid) {
	var isValid = true;
	document.getElementById("run_btn_"+snipid).disabled = !isValid;
}

// Called by the submit button to start the upload
function doSubmit(e, snipid) {
	if (formChecker != null) {
		clearInterval(formChecker);
		formChecker = null;
	}
	
	e = e || window.event;
	if (e.stopPropagation) {
		e.stopPropagation();
	}
	e.cancelBubble = true;

   if(!validateTemplate(snipid)) return;

    if(NUM_REMAINING_UPLOADS[snipid] == 0) {
	    runTemplate(snipid);
        return;
    }
	
	try {
        for(var fu in swfu[snipid]) {
		    swfu[snipid][fu].startUpload();
        }
	} catch (ex) {
        alert(ex.message);
	}
	return false;
}

