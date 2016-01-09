console.log("upload_js");

function drag_init() {
    //console.log("drag_init");
    function handleDragOver(evt) {
        //document.getElementById('drop_zone').style.visibility = "visible";

        evt.stopPropagation();
        evt.preventDefault();
        evt.dataTransfer.dropEffect = 'copy'; // Explicitly show this is a copy.
    }
    function handleFileSelect(evt) {
        //document.getElementById('drop_zone').style.visibility = "collapse";

        evt.stopPropagation();
        evt.preventDefault();
        var files = evt.dataTransfer.files; // FileList object.

        var path = getParameterByName("path");
        if (!path) path = "/";

        for (var i=0; i<files.length; i++) 
            http_put_file(http_server+path+files[i].name, files[i]);
    }
    var dropZone = document.getElementById('drop_zone');
    document.body.addEventListener('dragover', handleDragOver, false);
    document.body.addEventListener('drop', handleFileSelect, false);

    var upload_queue = 0;
    var upload_bytes_left = 0;
    var upload_update_div = document.getElementById('upload_update');
    function upload_update() {
        while (upload_update_div.firstChild) 
            upload_update_div.removeChild(upload_update_div.firstChild);
        //if (upload_queue==0)
        //    return ;
        upload_update_div.appendChild(document.createTextNode(
            "Upload queue: "+upload_queue+" files. "+Math.round(upload_bytes_left/1024)+" kBytes left."));
    }
    function http_put_file(url, file) {
        upload_queue++;
        upload_bytes_left+=file.size;
        upload_update();
        
        var http_req = new XMLHttpRequest();
        http_req.open('PUT', url, true);
        http_req.setRequestHeader('Content-Type', file.type);
        http_req.setRequestHeader('LastModifiedDate', file.lastModifiedDate);

        http_req.onreadystatechange = function() {

            if (http_req.readyState == 4) {
                upload_queue--;
                upload_bytes_left-=file.size;
                upload_update();
                
                if (!http_req.status || http_req.status>300)
                    alert("Error with upload:\n" + url + "\n" + JSON.stringify(http_req, null, 4));
               
                //http_req.responseText;
            }
        };
        var blob = file; 
        http_req.send(blob);
    }
}
