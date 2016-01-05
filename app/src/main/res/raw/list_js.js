console.log("list_js");

var http_server = location.origin;
if (location.protocol=="file:" || location.hostname=="localhost") {
	// for remote debug purpose (e.g. page and scripts on desktop displaying file system content on device) 
	http_server = "http://192.168.0.7:8086";
}

function getParameterByName(name) {
    name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
    var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
        results = regex.exec(location.search);
    return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}

function list_dw(folder, ls) {
    var folder_split = folder.split("/");
    folder_split.pop();
    folder_split.shift();

    document.write('<p>Folder: ');
    for (var i=0; i<folder_split.length; i++) {
        var txt = folder_split[i];
        var href = location.pathname+'?path='+folder_split.slice(0,i+1).join("/")+'/';
        document.write('<a href="'+href+'">'+txt+" /</a>");
    }
    document.write('</p><br/>');

    document.write('<table>');
    for (var i=0; i<ls.length; i++) {
    var f = ls[i].name;
    var is_dir = (f[f.length-1]=="/");

    document.write('<tr><td>');
    if (is_dir)
        document.write('<a href="'+location.pathname+'?path='+folder+f+'">'+f+'</a></td>'
            + '<td>'+ls[i].length+'</td><tr>');
    else
        document.write('<a href="'+http_server+folder+f+'">'+f+'</a></td></td>'
            + '<td>'+ls[i].length+'</td><tr>');;
    document.write('</td></tr>');

    }
    document.write('</table>');
}

function list(folder, ls) {
    var div = document.getElementById('path_explorer');

    div.appendChild(document.createElement("br"));
    div.appendChild(document.createTextNode('Folder: '));

    var folder_split = folder.split("/");
    if (folder_split[folder_split.length-1]=="") folder_split.pop();

    for (var i=0; i<folder_split.length; i++) {
        var txt = folder_split[i];
        var href = location.pathname+'?path='+folder_split.slice(0,i+1).join("/")+'/';
        var a = document.createElement("a");
        a.appendChild(document.createTextNode(txt+"/"));
        a.href = href;
        div.appendChild(a);
        div.appendChild(document.createTextNode(" "));
    }
    div.appendChild(document.createElement("br"));
    div.appendChild(document.createElement("br"));

    var table = document.createElement("table");
    var tr = document.createElement("tr");
    tr.innerHTML = '<th id="name-header">Name</th><th id="size-header">Size</th><th id="last-modified-header">LastModified</th>';
    table.appendChild(tr);

    for (var i=0; i<ls.length; i++) {
        var tr = document.createElement("tr");

        var f = ls[i].name;
        var is_dir = (f[f.length-1]=="/");

        var c1ref, c1txt, c2, c3;
        if (is_dir) {
            c1ref = location.pathname+'?path='+folder+f;
            c1txt = f;
        } else {
            c1ref = http_server+folder+f
            c1txt = f;
        }
        
        var td = document.createElement("td");
        var a = document.createElement("a");
        a.appendChild(document.createTextNode(c1txt));
        a.href = c1ref;
        td.appendChild(a);
        tr.appendChild(td);

        var td = document.createElement("td");
        td.appendChild(document.createTextNode(ls[i].length || "-"));
        tr.appendChild(td);
        
        var td = document.createElement("td");
        td.appendChild(document.createTextNode(ls[i].lastModified ? new Date(ls[i].lastModified).toISOString() : ""));
        tr.appendChild(td);
        
        var td = document.createElement("td");
        td.appendChild(document.createTextNode(ls[i].md5_hash || ""));
        tr.appendChild(td);

        var td = document.createElement("td");
        td.appendChild(document.createTextNode(ls[i].file_handler_hash || ""));
        tr.appendChild(td);

        //+ '<td>'+ls[i].length+'</td><tr>');;
        //document.write('</td></tr>');

        table.appendChild(tr);
    }
    div.appendChild(table);
}

function list_hot(folder, ls) {
	for (var i=0; i<ls.length; i++) {
		if (ls[i].length===undefined)
			ls[i].length = null;
		if (!ls[i].lastModified)
			ls[i].lastModified = null;
		else
			ls[i].lastModified = new Date(ls[i].lastModified).toISOString();
			
		 
	}
	for (var i=0; i<ls.length; i++) {
		var f = ls[i].name;
		var is_dir = (f[f.length-1]=="/");

		if (is_dir)
			ls[i].name = '<a href="'+location.pathname+'?path='+folder+f+'">'+f+'</a><br/>';
		else 
			ls[i].name = '<a href="'+http_server+folder+f+'">'+f+'</a><br/>';

	}
	var hot_container = document.getElementById('path_explorer');
	var hot = new Handsontable(hot_container, {
	  data: ls,
	  //rowHeaders: true,
	  colHeaders: [ "name", "length", "lastModified" ],
	  //colWidths: [45, 200, 110, 280],
	  columnSorting: true,
	  columns: [
		{ data: "name", renderer: "html" },
		{ data: "length" },
		{ data: "lastModified" }
	  ],
	  manualColumnMove: true,
	  manualColumnResize: true,
	  //minSpareRows: 0,
	  //contextMenu: true,
	  //persistentState: true
	});
}

(window.onpopstate = function () {

	var path = getParameterByName("path");
	if (!path) path = "/";

	var url = http_server+path+"?list";
	var xmlhttp = new XMLHttpRequest();
    xmlhttp.onreadystatechange = function() {
        if (xmlhttp.readyState == 4 ) {
           if(xmlhttp.status == 200) {
               var content = xmlhttp.responseText;
               //alert(content);
               //console.log(content);
               list(path, JSON.parse(content));
           } else {
               	var div = document.getElementById('path_explorer');
                div.innerHTML = '<p><b>Check server app is still running.</b></br>' +
                    'XMLHttpRequest status '+xmlhttp.status+' for url '+url+'.</p>';
           }
        }
    }
    xmlhttp.open("GET", url, true);
    xmlhttp.send();
;
})();