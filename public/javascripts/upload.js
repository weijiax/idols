function httpGetAsync(index, rootDir) {
	var xmlHttp = new XMLHttpRequest();
	xmlHttp.onreadystatechange = function() {
		if (xmlHttp.readyState == 4 && xmlHttp.status == 200) {
			tree = JSON.parse(this.responseText);
			console.log("Upload.js TREE:: " + this.responseText)
			setTimeout(showTree(index, tree), 500);
		} else if (this.responseText.startsWith("Error")){
			document.getElementById('status' + index).className = "status_error";
	        document.getElementById('status' + index).innerHTML = this.responseText;
		}
	}
	xmlHttp.open("GET", "/directorytree?rootPath=" + rootDir, true);
	xmlHttp.send();
}

/** Open directory tree
*/

function openTree(index, rootDir) {
	if (!rootDir) {
		document.getElementById('status' + index).className = "status_error";
        document.getElementById('status' + index).innerHTML = "Error: Must select or enter a directory";
	} else {
		if(rootDir.endsWith(".json")){
			httpGetAsyncJSON(index, rootDir);
		}
		else{
			httpGetAsync(index, rootDir);
		}
	}
}

/** Expand a directory structure given a JSON file.
*
*  This is a modification of the method httpGetAsync()
*/
function httpGetAsyncJSON(index, rootDir) {
	var xmlHttp = new XMLHttpRequest();
	xmlHttp.onreadystatechange = function() {
		if (xmlHttp.readyState == 4 && xmlHttp.status == 200) {
			tree = JSON.parse(this.responseText);
			console.log("Upload.js TREE:: " + this.responseText)
			setTimeout(showTree(index, tree), 500);
		} else if (this.responseText.startsWith("Error")){
			document.getElementById('status' + index).className = "status_error";
	        document.getElementById('status' + index).innerHTML = this.responseText;
		}
	}
	xmlHttp.open("GET", "/directorytreeJSON?rootPath=" + rootDir, true);
	xmlHttp.send();
}

/** Open directory tree based on a JSON file
*/
function openTreeFromJSON(index, rootDir) {
	if (!rootDir) {
		document.getElementById('status' + index).className = "status_error";
        document.getElementById('status' + index).innerHTML = "Error: Must select or enter a directory";
	} else {
		httpGetAsyncJSON(index, rootDir);
	}
}

function httpGetAsyncRefresh(index, rootDir) {
	var xmlHttp = new XMLHttpRequest();
	xmlHttp.onreadystatechange = function() {
		if (xmlHttp.readyState == 4 && xmlHttp.status == 200) {
			tree = JSON.parse(this.responseText);
			console.log("Upload.js TREE:: " + this.responseText)
			setTimeout(showTree(index, tree), 500);
		} else if (this.responseText.startsWith("Error")){
			document.getElementById('status' + index).className = "status_error";
	        document.getElementById('status' + index).innerHTML = this.responseText;
		}
	}
	xmlHttp.open("GET", "/directorytreeRefresh?rootPath=" + rootDir, true);
	xmlHttp.send();
}

function openTreeRefresh(index, rootDir) {
	if (!rootDir) {
		document.getElementById('status' + index).className = "status_error";
        document.getElementById('status' + index).innerHTML = "Error: Must select or enter a directory";
	} else {
		httpGetAsyncRefresh(index, rootDir);
	}
}

function showTree(index, tree) {

	if ($('#container' + index).is(':empty')) {
		// create jstree
		document.getElementById('back' + index).style.display = 'inline-block';
		document.getElementById('forward' + index).style.display = 'inline-block';
		$('#container' + index).jstree({
			'core' : {
				'data' : tree
			},
			 'types': {
				 "default" : {
				      "valid_children" : ["default","file"]
				    },
				    "file" : {
				      "icon" : "glyphicon glyphicon-file",
				      "valid_children" : []
				    }
			  },
			  "plugins" : [
				    "types"
				  ]
		});

	} else {
		// jstree already generated, change data and refresh
		$('#container' + index).jstree(true).settings.core.data = tree;
		$('#container' + index).jstree(true).refresh();
//		// update root directory
//		$('#root' + index).val(rootDir);
	}

	$('#container' + index)
	// listen for event
	.on('changed.jstree', function(e, data) {
		var r = null;
		r = data.instance.get_node(data.selected[0]);

		// keep the absolute path of the directory selected
		$('#directory' + index).val(r.data);
		document.getElementById('directory' + index).dispatchEvent(new Event("change"));
		console.log(document.getElementById('root' + index).value)
		var cur = "" + r.data
		var root = "" + document.getElementById('root' + index).value
		document.getElementById('audio_controls' + index).style = "display:none"
		
		if (cur.includes(root) && cur.length > root.length) {
			document.getElementById('back' + index).disabled = false;
		} else {
			document.getElementById('back' + index).disabled = true;
		}
	})
	// create the instance
	.jstree();
}
