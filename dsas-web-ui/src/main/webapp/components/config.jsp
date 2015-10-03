<%@page import="gov.usgs.cida.utilities.features.Constants"%>
<%@page import="gov.usgs.cida.coastalhazards.shoreline.file.ShorelineFile"%>
<%@page import="gov.usgs.cida.coastalhazards.service.util.Property"%>
<%@page import="gov.usgs.cida.coastalhazards.service.util.PropertyUtil"%>
<%!
	boolean development = Boolean.parseBoolean(PropertyUtil.getProperty(Property.DEVELOPMENT));
	String geoserverEndpoint = PropertyUtil.getProperty(Property.GEOSERVER_ENDPOINT);
	String n52Endpoint = PropertyUtil.getProperty(Property.N52_ENDPOINT);
%>
<script type="text/javascript">
	splashUpdate("Setting configuration...");
	var CONFIG = Object.extended();
	CONFIG.window = {};
	CONFIG.window.login = null;
	// Tells the application whether its in development mode or not.
	// Development mode tends to have more verbose logging
	CONFIG.development = <%= development%>;
	CONFIG.geoServerEndpoint = '<%=geoserverEndpoint%>';
	CONFIG.n52Endpoint = '<%=n52Endpoint%>';
	CONFIG.popupHoverDelay = 1500;
	CONFIG.namespace = Object.extended();
	CONFIG.namespace.proxydatumbias = 'gov.usgs.cida.ch.bias';
	CONFIG.namespace.published = 'gov.usgs.cida.ch.published';
	CONFIG.namespace.input = 'gov.usgs.cida.ch.input';
	CONFIG.namespace.output = 'gov.usgs.cida.ch.output';
	CONFIG.name = {};
	CONFIG.name.published = 'published';
	CONFIG.name.proxydatumbias = 'proxydatumbias';
	// TODO- Very obviously a bad way of doing security :D Remove me when we
	// implement something that makes sense.
	CONFIG.isAdmin = window.location.search.toLowerCase().indexOf('u=admin') !== -1;
	CONFIG.strings = {
		epsg4326: 'EPSG:4326',
		epsg900913: 'EPSG:900913',
		epsg3857: 'EPSG:3857',
		columnAttrNames: {
			'LRR': '<%= Constants.LRR_ATTR%>',
			'WLR': '<%= Constants.WLR_ATTR%>',
			'SCE': '<%= Constants.SCE_ATTR%>',
			'NSM': '<%= Constants.NSM_ATTR%>',
			'EPR': '<%= Constants.EPR_ATTR%>',
			'LCI': '<%= Constants.LCI_ATTR%>',
			'WCI': '<%= Constants.WCI_ATTR%>',
			'ECI': '<%= Constants.ECI_ATTR%>',
			'MHW': '<%= Constants.MHW_ATTR%>',
			'SurveyID': '<%= Constants.SURVEY_ID_ATTR%>',
			'distance' : '<%= Constants.DISTANCE_ATTR%>',
			'defaultDirection' : '<%= Constants.DEFAULT_D_ATTR%>',
			'name' : '<%= Constants.NAME_ATTR%>',
			'source' : '<%= Constants.SOURCE_ATTR%>',
			'biasUncertainty' : '<%= Constants.BIAS_UNCY_ATTR%>'
		}
	};
	CONFIG.dateFormat = {
		padded: '{yyyy}-{MM}-{dd}',
		nonPadded: '{yyyy}-{M}-{d}'
	};
	CONFIG.alertQueue = {
		application: [],
		shorelines: [],
		baseline: [],
		transects: [],
		bias: [],
		calculation: [],
		results: []
	};
	CONFIG.colorGroups = Object.extended();
	CONFIG.ajaxTimeout = 300000;
	CONFIG.graph = Object.extended();
	CONFIG.graph.enabled = CONFIG.strings.columnAttrNames.LRR;
	CONFIG.graph.displayMap = {
		'<%= Constants.LRR_ATTR%>': {
			longName: 'Linear regression rate +/- LCI',
			units: 'm yr^-1',
			uncertainty: CONFIG.strings.columnAttrNames.LCI,
			invert: true
		},
		'<%= Constants.WLR_ATTR%>': {
			longName: 'Weighted linear regression rate +/i WCI',
			units: 'm yr^-1',
			uncertainty: CONFIG.strings.columnAttrNames.WCI,
			invert: true
		},
		'<%= Constants.SCE_ATTR%>': {
			longName: 'Shoreline change envelope',
			units: 'm',
			invert: false
		},
		'<%= Constants.NSM_ATTR%>': {
			longName: 'Net shoreline movement',
			units: 'm',
			invert: false
		},
		'<%= Constants.EPR_ATTR%>': {
			longName: 'End point rate',
			units: 'm yr^-1',
			uncertainty: CONFIG.strings.columnAttrNames.ECI,
			invert: false
		}
	};
	JSON.stringify = JSON.stringify || function (obj) {
		var t = typeof (obj);
		if (t !== "object" || obj === null) {
			// simple data type
			if (t === "string")
				obj = '"' + obj + '"';
			return String(obj);
		}
		else {
			// recurse array or object
			var n, v, json = [], arr = (obj && obj.constructor === Array);
			for (n in obj) {
				v = obj[n];
				t = typeof (v);
				if (t === "string")
					v = '"' + v + '"';
				else if (t === "object" && v !== null)
					v = JSON.stringify(v);
				json.push((arr ? "" : '"' + n + '":') + String(v));
			}
			return (arr ? "[" : "{") + String(json) + (arr ? "]" : "}");
		}
	};


</script>