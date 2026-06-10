let queryDataTable; //*Used by dataTables to store table object, can be use to render data efficiently
const subscriptionDataTables = {};
var openSubscriptions = new Map();
var myJson = {
	namespaces: {}
};
var tabIndex = 0;
var headers = {};
let ids = 0;
let subEditor;
let queryEditor;
let updateEditor;

let emptyMarker = {
	clear: () => { },
}

// Bootstrap 5 helpers (Bootstrap 4 jQuery plugins are gone)
function showTabById(tabLinkId) {
	const el = document.getElementById(tabLinkId);
	if (!el) return;
	const tab = bootstrap.Tab.getOrCreateInstance(el);
	tab.show();
}
function showLastSubscriptionTab() {
	const last = document.querySelector("#pills-tab-subscriptions .nav-link:last-child");
	if (!last) return;
	bootstrap.Tab.getOrCreateInstance(last).show();
}

function onInit() {
	console.log("### SEPA DASHBOARD ###")
	console.log("loading editors...")
	loadEditors()
	initJsapFileInput()
	let type = getQueryVariable("mode");
	switch (type) {
		case "local":
			$("#host").val("localhost");
			break;
		case "playground":
			$("#configPanel").hide()
			break;
		default:
			break;
	}
	defaultNamespaces();

	//LOAD DEFAULT JSAP
	console.log("loading environment variables...")
	const env = getEnvVariables();
	console.log("Host: " + env.HOST);
	console.log("Jsap path: " + env.JSAP_PATH)
	if (env.DEFAULT_JSAP != null && env.DEFAULT_JSAP != undefined && env.DEFAULT_JSAP != "") {
		console.log("loading default jsap")
		myJson = env.DEFAULT_JSAP;
		injectMyJsonIntoEditor();
	} else {
		console.log("skipping load default jsap")
	}
	//LOAD ENV
	if (env["HOST"] != null && env["HOST"] != "") $("#host").val(env["HOST"]);
	if (env["HTTP_PORT"] != null && env["HTTP_PORT"] != "") $("#sparql11port").val(env["HTTP_PORT"]);
	if (env["HTTP_PROTOCOL"] != null && env["HTTP_PROTOCOL"] != "") $("#sparql11protocol").val(env["HTTP_PROTOCOL"]);
	if (env["WS_PORT"] != null && env["WS_PORT"] != "") $("#sparql11seport").val(env["WS_PORT"]);
	if (env["WS_PROTOCOL"] != null && env["WS_PROTOCOL"] != "") $("#sparql11seprotocol").val(env["WS_PROTOCOL"]);
	if (env["UPDATE_PATH"] != null && env["UPDATE_PATH"] != "") $("#updatePath").val(env["UPDATE_PATH"]);
	if (env["QUERY_PATH"] != null && env["QUERY_PATH"] != "") $("#queryPath").val(env["QUERY_PATH"]);
	if (env["SUBSCRIBE_PATH"] != null && env["SUBSCRIBE_PATH"] != "") $("#subscribePath").val(env["SUBSCRIBE_PATH"]);

	//Initializing tree
	// $('#tree').treeview({ data: getTree() });
}

function initJsapFileInput() {
	const input = document.getElementById("loadJsap");
	const fileName = document.getElementById("jsapFileName");
	if (!input || !fileName) return;

	input.addEventListener("change", function () {
		fileName.textContent = input.files && input.files[0]
			? input.files[0].name
			: "Please select a JSAP file";
	});
}

function getTree() {
	// Some logic to retrieve, or generate tree structure
	return [
		{
			text: "Sensor",
			nodes: [
				{
					text: "Child 1",
					nodes: [
						{
							text: "Grandchild 1"
						},
						{
							text: "Grandchild 2"
						}
					]
				},
				{
					text: "Child 2"
				}
			]
		},
		{
			text: "Parent 2"
		},
		{
			text: "Parent 3"
		},
		{
			text: "Parent 4"
		},
		{
			text: "Parent 5"
		}
	];
}

function refreshEditor(editor) {
  editor.refresh();
  // forzo anche la size: aiuta molto quando il container cambia layout
  if (editor.setSize) editor.setSize("100%", null);

  requestAnimationFrame(() => {
    editor.refresh();
    if (editor.setSize) editor.setSize("100%", null);
    editor.scrollIntoView({ line: 0, ch: 0 });
  });
}

function loadEditors() {
  YASQE.defaults.persistent = null;

  subEditor = YASQE.fromTextArea(document.getElementById('subscribeTextInput'));
  queryEditor = YASQE.fromTextArea(document.getElementById('queryTextInput'));
  updateEditor = YASQE.fromTextArea(document.getElementById('updateTextInput'));

  subEditor.prefixMarker = emptyMarker;
  queryEditor.prefixMarker = emptyMarker;
  updateEditor.prefixMarker = emptyMarker;

  // setValue: ok
  subEditor.setValue(subEditor.getTextArea().value);
  queryEditor.setValue(queryEditor.getTextArea().value);
  updateEditor.setValue(updateEditor.getTextArea().value);

  // ✅ refresh SOLO dell’editor che è visibile al load
  // Se per esempio al load è attivo "Query", usa queryEditor; altrimenti cambia qui.
  setTimeout(() => refreshEditor(queryEditor), 0);

  // ✅ handler tab: refresh dopo che Bootstrap ha finito di mostrare il pane
  document.addEventListener('shown.bs.tab', (e) => {
    const href = e.target.getAttribute('href') || e.target.getAttribute('data-bs-target');

    if (href === '#pills-subscribe') {
      unFixGlobalNamespaces(subEditor);
      setTimeout(() => {
        refreshEditor(subEditor);
        YASQE.doAutoFormat(subEditor);
        fixGlobalNamespaces(subEditor);
      }, 0);
    }

    if (href === '#pills-query') {
      unFixGlobalNamespaces(queryEditor);
      setTimeout(() => {
        refreshEditor(queryEditor);
        YASQE.doAutoFormat(queryEditor);
        fixGlobalNamespaces(queryEditor);
      }, 0);
    }

    if (href === '#pills-update') {
      unFixGlobalNamespaces(updateEditor);
      setTimeout(() => {
        refreshEditor(updateEditor);
        YASQE.doAutoFormat(updateEditor);
        fixGlobalNamespaces(updateEditor);
      }, 0);
    }
  });
}



function getTimestamp() {
	date = new Date();
	return date.toLocaleDateString() + " " + date.toLocaleTimeString();
};

function deleteNamespace(pr, ns) {
	document.getElementById(pr).remove();
	delete myJson.namespaces[pr]
	let pref = {}
	pref[pr] = ns

	try { subEditor.removePrefixes(pref) } catch (e) { console.log(e) }
	try { updateEditor.removePrefixes(pref) } catch (e) { console.log(e) }
	try { queryEditor.removePrefixes(pref) } catch (e) { console.log(e) }

};
function addNamespaceToAll(pr, ns) {
	let pref = {}
	pref[pr] = ns

	subEditor.addPrefixes(pref)
	updateEditor.addPrefixes(pref)
	queryEditor.addPrefixes(pref)

	// if an old element with the same prefix exists,
	// then remove it
	el = document.getElementById(pr);
	if ((el !== undefined) && (el !== null)) {
		el.remove();
	}

	// get the table and add the namespace
	table = document.getElementById("namespacesTable");
	newRow = table.insertRow(-1);

	// prefix cell
	newCell = newRow.insertCell(0);
	newCell.innerHTML = pr;

	// namespace cell
	newCell = newRow.insertCell(1);
	newCell.innerHTML = ns;

	// bind the prefix to the row
	newRow.id = pr;

	// actions cell
	newCell = newRow.insertCell(2);
	newCell.innerHTML = "<button action='button' class='btn btn-danger btn-sm' onclick='javascript:deleteNamespace("
		+ '"'
		+ pr
		+ '","' + ns + '"'
		+ ");'><small><span class='glyphicon glyphicon-trash' aria-hidden='true''><i class='fas fa-trash-alt'></i>&nbsp;</span>Delete</small></button>";

}

function addNamespace() {
	// get the prefix
	pr = document.getElementById("prefixField").value;

	// get the namespace
	ns = document.getElementById("namespaceField").value;
	myJson.namespaces[pr] = ns
	addNamespaceToAll(pr, ns)
};

function defaultNamespaces() {
	addNamespaceToAll("xsd", "http://www.w3.org/2001/XMLSchema#");
	addNamespaceToAll("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
	addNamespaceToAll("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
	addNamespaceToAll("time", "http://www.w3.org/2006/time#");
	addNamespaceToAll("owl", "http://www.w3.org/2002/07/owl#");
}

function fixGlobalNamespaces(editor) {
	let startPrefixes = { line: 0, ch: 0 }
	let endPrefixes = { line: Object.keys(myJson.namespaces).length, ch: 0 }
	let marker = editor.markText(startPrefixes, endPrefixes, { readOnly: true, className: "non-editable" })
	editor.prefixMarker = marker
}
function unFixGlobalNamespaces(editor) {
	editor.prefixMarker.clear()
}

function loadJsap() {
	// check if file reader is supported
	if (!window.FileReader) {
		console.log("[ERROR] FileReader API is not supported by your browser.");
		return false;
	}

	// load data
	var $i = $('#loadJsap');
	input = $i[0];
	if (input.files && input.files[0]) {
		file = input.files[0];

		// create a mew instance of the file reader
		fr = new FileReader();
		fr.onload = function () {
			// read the content of the file
			var decodedData = fr.result;
			// parse the JSON file
			myJson = JSON.parse(decodedData);
			injectMyJsonIntoEditor();
		};
		fr.readAsText(file);
	}
};

function injectMyJsonIntoEditor() {
	//myJson = JSON.parse(decodedData);
	// get the namespaces table
	table = document.getElementById("namespacesTable");

	// retrieve namespaces
	for (pr in myJson["namespaces"]) {
		addNamespaceToAll(pr, myJson.namespaces[pr])
	}

	fixGlobalNamespaces(queryEditor)
	fixGlobalNamespaces(updateEditor)
	fixGlobalNamespaces(subEditor)


	// retrieve the URLs
	$("#host").val(myJson["host"]);

	$("#sparql11protocol").val(myJson["sparql11protocol"]["protocol"]);
	$("#sparql11port").val(myJson["sparql11protocol"]["port"]);
	$("#updatePath").val(myJson["sparql11protocol"]["update"]["path"]);
	$("#queryPath").val(myJson["sparql11protocol"]["query"]["path"]);

	ws = myJson["sparql11seprotocol"]["protocol"];

	$("#sparql11seprotocol").val(ws);
	$("#sparql11seport").val(myJson["sparql11seprotocol"]["availableProtocols"][ws]["port"]);
	$("#subscribePath").val(myJson["sparql11seprotocol"]["availableProtocols"][ws]["path"]);

	// load queries
	ul = document.getElementById("queryDropdown");
	for (q in myJson["queries"]) {
		li = document.createElement("li");
		li.setAttribute("id", q);
		li.innerHTML = q;
		li.setAttribute("onclick",
			"javascript:loadUQS(\"Q\", '" + q + "');");
		li.setAttribute("data-bs-toggle", "modal");
		li.setAttribute("data-bs-target", "#basicModal");
		li.classList.add("dropdown-item");
		li.classList.add("small");
		ul.appendChild(li);
	};

	// load subscribes
	ul = document.getElementById("subscribeDropdown");
	for (q in myJson["queries"]) {
		li = document.createElement("li");
		li.setAttribute("id", q);
		li.innerHTML = q;
		li.setAttribute("onclick",
			"javascript:loadUQS(\"S\", '" + q + "');");
		li.setAttribute("data-bs-toggle", "modal");
		li.setAttribute("data-bs-target", "#basicModal");
		li.classList.add("dropdown-item");
		li.classList.add("small");
		ul.appendChild(li);
	};

	// load updates
	ul = document.getElementById("updateDropdown");
	for (q in myJson["updates"]) {
		li = document.createElement("li");
		li.setAttribute("id", q);
		li.innerHTML = q;
		li.setAttribute("onclick", "javascript:loadUQS(\"U\", '"
			+ q + "');");
		li.setAttribute("data-bs-toggle", "modal");
		li.setAttribute("data-bs-target", "#basicModal");
		li.classList.add("dropdown-item");
		li.classList.add("small");
		ul.appendChild(li);
	}
}

function getForcedBindings(u) {
	let fb = ""
	switch (u) {
		case "U":
			fb = "#updateForcedBindings";
			break;
		case "Q":
			fb = "#queryForcedBindings";
			break;
		case "S":
			fb = "#subscribeForcedBindings";
			break;
		default:
			throw "Cannot find forced binding type"
	}

	let result = {}
	$(fb).each(function () {
		$(this).find(':input').each(function () {
			let binding = $(this).attr("id");
			tempType = $(this).attr("type");
			tempType = (tempType == "uri") ? tempType : (tempType == "bnode") ? tempType : "literal";

			result[binding] = {
				type: tempType,
				value: $(this).val(),
				datatype: ($(this).attr("type") != tempType) ? $(this).attr("type") : null
			}
		});
	});

	return result;
}

function loadForcedBindings(u, id) {
	if (u == "U") {
		key = "updates";
		forcedBindings = document.getElementById("updateForcedBindings");
		$("#updateForcedBindings").empty();
	} else if (u == "Q") {
		key = "queries";
		forcedBindings = document.getElementById("queryForcedBindings");
		$("#queryForcedBindings").empty();
	}
	else {
		key = "queries";
		forcedBindings = document.getElementById("subscribeForcedBindings");
		$("#subscribeForcedBindings").empty();
	}

	if ("forcedBindings" in myJson[key][id]) {
		console.log(Object.keys(myJson[key][id]["forcedBindings"]).length);
		if (Object.keys(myJson[key][id]["forcedBindings"]).length > 0) {
			label = document.createElement("label");
			label.setAttribute("class", "col-form-label-sm");
			label.innerHTML = "<b>Forced bindings</b>";

			forcedBindings.appendChild(label);

			for (fb in myJson[key][id]["forcedBindings"]) {
				span = document.createElement("span");
				span.setAttribute("class", "input-group-text");
				span.innerHTML = fb;

				div = document.createElement("div");
				div.setAttribute("class", "input-group mb-3");
				div.appendChild(span);

				if (myJson[key][id]["forcedBindings"][fb]["datatype"] != null)
					type = myJson[key][id]["forcedBindings"][fb]["datatype"];
				else
					type = myJson[key][id]["forcedBindings"][fb]["type"];

				input = document.createElement("input");
				input.type = "text";
				input.setAttribute("class", "form-control");
				input.setAttribute("aria-label", "Specify the binding value");
				input.setAttribute("id", fb);
				input.setAttribute("type", type);

				div.appendChild(input);

				span2 = document.createElement("span");
				span2.setAttribute("class", "input-group-text");
				span2.innerHTML = type;

				div.appendChild(span2);

				if (myJson[key][id]["forcedBindings"][fb]["lang"] != null)
					lang = myJson[key][id]["forcedBindings"][fb]["lang"];
				else
					lang = null;

				if (lang != null) {
					input2 = document.createElement("input");
					input2.type = "text";
					input2.setAttribute("class", "form-control");
					input2.setAttribute("aria-label", "Specify the binding language");
					input2.setAttribute("id", fb + "@" + lang);
					input2.setAttribute("lang", lang);

					div.appendChild(input2);

					span3 = document.createElement("span");
					span3.setAttribute("class", "input-group-text");
					span3.innerHTML = lang;

					div.appendChild(span3);
				}
				forcedBindings.appendChild(div);
			}
		}
	}
}

function loadUQS(usq, uqname) {
	if (usq == "U") {
		document.getElementById("updateTextInput").value = myJson["updates"][uqname]["sparql"];
		let tempNS = updateEditor.getPrefixesFromQuery()
		updateEditor.setValue(myJson["updates"][uqname]["sparql"])
		updateEditor.addPrefixes(tempNS)
		unFixGlobalNamespaces(updateEditor)
		YASQE.doAutoFormat(updateEditor)
		fixGlobalNamespaces(updateEditor)
		document.getElementById("updateLabel").innerHTML = "<b>" + uqname + "</b>";
	} else if (usq == "Q") {
		document.getElementById("queryTextInput").value = myJson["queries"][uqname]["sparql"];
		let tempNS = queryEditor.getPrefixesFromQuery()
		queryEditor.setValue(myJson["queries"][uqname]["sparql"])
		queryEditor.addPrefixes(tempNS)
		unFixGlobalNamespaces(queryEditor)
		YASQE.doAutoFormat(queryEditor)
		fixGlobalNamespaces(queryEditor)
		document.getElementById("queryLabel").innerHTML = "<b>" + uqname + "</b>";
	}
	else {
		document.getElementById("subscribeTextInput").value = myJson["queries"][uqname]["sparql"];
		let tempNS = subEditor.getPrefixesFromQuery()
		subEditor.setValue(myJson["queries"][uqname]["sparql"])
		subEditor.addPrefixes(tempNS)
		unFixGlobalNamespaces(subEditor)
		YASQE.doAutoFormat(subEditor)
		fixGlobalNamespaces(subEditor)
		document.getElementById("subscribeLabel").innerHTML = "<b>" + uqname + "</b>";
		document.getElementById("subscriptionAlias").value = uqname;
	}

	loadForcedBindings(usq, uqname);
}

function query() {
	let queryText = queryEditor.getValue();
	let bench = new Sepajs.bench();
	queryText = bench.sparql(queryText, getForcedBindings("Q"));

	const sepa = Sepajs.client;

	config = { host: $("#host").val(), sparql11protocol: { protocol: $("#sparql11protocol").val(), port: $("#sparql11port").val(), query: { "path": $("#queryPath").val() } } };

	start = Date.now();
	sepa.query(queryText, config).then((data) => {
		stop = Date.now();

		$("#queryResultLabel").html("[" + getTimestamp() + "] " + data["results"]["bindings"].length + " results in " + (stop - start) + " ms")

		renderQueryResultsWithDataTable(data);

	}).catch((err) => {
		stop = Date.now();
		$("#queryResultLabel").html("[" + getTimestamp() + "] " + err + " *** Query FAILED in " + (stop - start) + " ms ***");
	});
}

function renderQueryResultsWithDataTable(queryResponse, tableRef, tableId) {
	console.log("Rendering query results with dataTable...")
	if (queryDataTable != null) {
		queryDataTable.destroy();
		queryDataTable = null;
	}
	$("#queryTable").remove();
	$("#queryTableContainer").empty();
	$("#queryTableContainer").append('<table id="queryTable" class="table table-striped table-bordered table-sm mt-3"><thead><tr></tr></thead><tbody></tbody></table>');
	queryDataTable = $("#queryTable").DataTable({
		responsive: true,
		scrollX: true,
		order: [],
		dom: `<"top d-flex justify-content-between"
				<"left"l><"right d-flex align-items-center"fB>
			  >
			  	rt
			  <"bottom"ip>`,
		buttons: [
			"copy",
			"csv",
			"colvis"
		],
		columns: queryResponse["head"]["vars"].map((colName) => {
			return { title: colName }
		}),
		data: queryResponse["results"]["bindings"].map((binding) => {
			return queryResponse["head"]["vars"].map((colName) => {
				let value = binding[colName]?.value || null;
				const type = binding[colName]?.type || null;
				if (type === "typed-literal" && value != null) value = value + "^^" + binding[colName]["datatype"]
				return value;
			})
		}),
		createdRow: function (row, data, dataIndex) {
			for (let i = 0; i < data.length; i++) {
				if (data[i] == null) {
					$('td', row).eq(i).addClass('table-danger')
				} else {
					const binding = queryResponse["results"]["bindings"][dataIndex];
					const colName = queryResponse["head"]["vars"][i];
					const type = binding[colName]["type"];
					if (type === "literal" || type === "typed-literal")
						$('td', row).eq(i).addClass('table-primary')
					else $('td', row).eq(i).addClass('table-success')
				}
			}
		},
		initComplete: function () {
			$('.btn', '.dt-buttons').removeClass('btn-secondary').addClass('btn-primary');
		}
	})
}

function clearQueryResults() {
	$("#queryTable thead tr").empty();
	$("#queryTable tbody").empty();
}

function update() {
	let updateText = updateEditor.getValue()
	let bench = new Sepajs.bench()
	updateText = bench.sparql(updateText, getForcedBindings("U"));

	config = { host: $("#host").val(), sparql11protocol: { protocol: $("#sparql11protocol").val(), "port": $("#sparql11port").val(), update: { "path": $("#updatePath").val() } } };

	const sepa = Sepajs.client;
	start = Date.now();
	sepa.update(updateText, config).then((data) => {
		stop = Date.now();
		$("#updateResultLabel").html("[" + getTimestamp() + "] Update done in " + (stop - start) + " ms")
	}).catch((err) => {
		stop = Date.now();
		$("#updateResultLabel").html("[" + getTimestamp() + "] " + err + " *** Update FAILED in " + (stop - start) + " ms ***")
	});
};

function generateIdBySuggestion(suggestion) {
	if (!suggestion || openSubscriptions.get(suggestion)) {
		return suggestion + ++ids
	} else {
		return suggestion
	}
}

function subscribe() {
	let subscribeText = subEditor.getValue();
	let bench = new Sepajs.bench()
	subscribeText = bench.sparql(subscribeText, getForcedBindings("S"))

	ws = $("#sparql11seprotocol").val();
	config = { host: $("#host").val(), sparql11seprotocol: { protocol: ws, availableProtocols: { [ws]: { port: $("#sparql11seport").val(), path: $("#subscribePath").val() } } } };

	const sepa = Sepajs.client;
	let id = generateIdBySuggestion($('#subscriptionAlias').val())
	let subscription = sepa.subscribe(subscribeText, config, id)
	let tab = undefined
	subscription.on("subscribed", (data) => {
		spuid = data.spuid
		let alias = data.alias

		$("#subscribeInfoLabel").html("[" + getTimestamp() + "] New subscription " + spuid);
		$("#notificationsInfoLabel").html("[" + getTimestamp() + "] New subscription " + spuid);

		tabIndex = tabIndex + 1;
		tab = tabIndex

		$("#pills-tab-subscriptions").append(
			"<li class=\"nav-item\">" +
			"	<a class=\"nav-link\" " +
			"id=\"pills-" + tabIndex + "-tab\" " +
			"data-bs-toggle=\"pill\" " +
			"href=\"#pills-" + tabIndex + "\" " +
			"role=\"tab\" " +
			"aria-controls=\"pills-" + tabIndex + "\" " +
			"aria-selected=\"false\">" + alias + "</a></li>");

		$("#pills-tabContent-subscriptions").append(
			"<div class=\"tab-pane mt-3 fade\" " +
			"id=\"pills-" + tabIndex + "\" " +
			"role=\"tabpanel\" " +
			"aria-labelledby=\"pills-" + tabIndex + "-tab\">" +
			"<button action='button' class='btn btn-outline-danger btn-sm mb-3 float-end' " +
			"onclick='javascript:unsubscribe(\"" + alias + "\")'>" +
			"<small><i class='fas fa-trash-alt'></i>&nbsp;Unsubscribe</small>" +
			"</button>" +
			"<div class=\"table-responsive\">" +
			"<div class=\"table-wrapper\">" +
			"<table class=\"table table-bordered table-hover table-sm\" " +
			"id=\"table-" + tabIndex + "\"></table>" +
			"</div>" +
			"</div>" +
			"</div>");

		showTabById('pills-' + tabIndex + "-tab");
	})

	subscription.on("notification", (data) => {
		if (data) {
			spuid = data.spuid

			$("#notificationsInfoLabel").html("[" + getTimestamp() + "] Last notification: " + spuid + " (" + data.sequence + ")");

			for (v in data["removedResults"]["head"]["vars"]) {
				name = data["removedResults"]["head"]["vars"][v];

				if (headers[spuid] == null) {
					headers[spuid] = [];
					headers[spuid].push(name);

					$("#activeSubscriptions #table-" + tab).append("<thead class=\"table-light\"><tr><th scope=\"col\">#</th></tr></thead>");
					$("#activeSubscriptions #table-" + tab + " thead tr").append("<th scope=\"col\">" + name + "</th>");

					$("#activeSubscriptions #table-" + tab).append("<tbody id=\"tbody-" + tab + "\"></tbody>");
				}
				else if (!headers[spuid].includes(name)) {
					headers[spuid].push(name);
					$("#activeSubscriptions #table-" + tab + " thead tr").append("<th scope=\"col\">" + name + "</th");
				}
			}

			for (v in data["addedResults"]["head"]["vars"]) {
				name = data["addedResults"]["head"]["vars"][v];

				if (headers[spuid] == null) {
					headers[spuid] = [];
					headers[spuid].push(name);

					$("#activeSubscriptions #table-" + tab).append("<thead class=\"table-light\"><tr><th scope=\"col\">#</th></tr></thead>");
					$("#activeSubscriptions #table-" + tab + " thead tr").append("<th scope=\"col\">" + name + "</th>");

					$("#activeSubscriptions #table-" + tab).append("<tbody id=\"tbody-" + tab + "\"></tbody>");
				}
				else if (!headers[spuid].includes(name)) {
					headers[spuid].push(name);
					$("#activeSubscriptions #table-" + tab + " thead tr").append("<th scope=\"col\">" + name + "</th");
				}
			}

			for (index in data["removedResults"]["results"]["bindings"]) {
				bindings = data["removedResults"]["results"]["bindings"][index];

				$("#activeSubscriptions #tbody-" + tab).prepend("<tr class=\"table-danger\"></tr>");

				tr = $("#activeSubscriptions #tbody-" + tab + " tr:first");
				tr.append("<td>" + data["sequence"] + "</td>");

				for (name of headers[spuid]) {
					if (bindings[name] != null) value = bindings[name]["value"];
					else value = "";

					tr.append("<td>" + value + "</td>");
				}
			}

			for (index in data["addedResults"]["results"]["bindings"]) {
				bindings = data["addedResults"]["results"]["bindings"][index];

				$("#activeSubscriptions #tbody-" + tab).prepend("<tr class=\"table-success\"></tr>");

				tr = $("#activeSubscriptions #tbody-" + tab + " tr:first");
				tr.append("<td>" + data["sequence"] + "</td>");

				for (name of headers[spuid]) {
					if (bindings[name] != null) value = bindings[name]["value"];
					else value = "";

					tr.append("<td>" + value + "</td>");
				}
			}
		}
		else {
			console.log(msg);
			$("#subscribeInfoLabel").html("[" + getTimestamp() + "] Subscribe FAILED @ " + msg);
		}
	})
	subscription.on("error", (err) => {
		$("#subscribeInfoLabel").html("[" + getTimestamp() + "] *** Subscribe FAILED @ " + $("#host").val() + " ***")
	})
	subscription.on("connection-error", (err) => {
		$("#subscribeInfoLabel").html("[" + getTimestamp() + "] *** Subscribe FAILED @ " + $("#host").val() + " ***")
	})
	subscription.on("unsubscribed", (not) => {
		if (tab) closeSpuidTab(tab);
		$("#subscribeInfoLabel").html("[" + getTimestamp() + "] Unsubscribed ");
	})

	openSubscriptions.set(id, subscription)
}

function closeSpuidTab(tab) {
	$("#pills-tab-subscriptions #pills-" + tab + "-tab").remove();
	$("#pills-tabContent-subscriptions #pills-" + tab).remove();
	showLastSubscriptionTab();
}

function unsubscribe(alias) {
	openSubscriptions.get(alias).unsubscribe()
}

function getQueryVariable(variable) {
	var query = window.location.search.substring(1);
	var vars = query.split('&');
	for (var i = 0; i < vars.length; i++) {
		var pair = vars[i].split('=');
		if (decodeURIComponent(pair[0]) == variable) {
			return decodeURIComponent(pair[1]);
		}
	}
	console.log('Query variable %s not found', variable);
}

function getEnvVariables() {
	let env = {}
	if (
		___SEPA_DASHBOARD_INLINE_JSON_CONFIG___ != null &&
		___SEPA_DASHBOARD_INLINE_JSON_CONFIG___ != undefined
	) {
		env = ___SEPA_DASHBOARD_INLINE_JSON_CONFIG___;
	}
	console.log("Env:", env);
	return env;
}
