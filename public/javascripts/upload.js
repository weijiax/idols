var tree = null
function httpGetAsync(rootDir) {
	var xmlHttp = new XMLHttpRequest();
	xmlHttp.onreadystatechange = function() {
		if (xmlHttp.readyState == 4 && xmlHttp.status == 200)
			tree = JSON.parse(this.responseText);
	}
	xmlHttp.open("GET", "/directorytree?rootPath=" + rootDir, true);
	xmlHttp.send();
}

function openTree(index, rootDir) {

	if (!rootDir) {
		document.getElementById('status' + index).className = " status_error";
        document.getElementById('status' + index).innerHTML = "Error: Must select or enter a directory";
	} else {

		httpGetAsync(rootDir);

		setTimeout(
				function() {

					if ($('#container' + index).is(':empty')) {
						// create jstree
						document.getElementById('back' + index).style.display = 'inline-block';
						document.getElementById('forward' + index).style.display = 'inline-block';
						$('#container' + index).jstree({
							'core' : {
								'data' : tree
							}
						});

					} else {
						// jstree already generated, change data and refresh
						$('#container' + index).jstree(true).settings.core.data = tree;
						$('#container' + index).jstree(true).refresh();
						// update root directory
						$('#root' + index).val(rootDir);
					}

					$('#container' + index)
					// listen for event
					.on('changed.jstree', function(e, data) {
						var r = null;
						r = data.instance.get_node(data.selected[0]);

						// keep the absolute path of the directory selected
						$('#directory' + index).val(r.data);
					})
					// create the instance
					.jstree();
				}, 
		500);
	}
}