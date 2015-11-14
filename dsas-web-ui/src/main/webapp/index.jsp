
<%@page import="gov.usgs.cida.utilities.features.Constants"%>
<%@page import="gov.usgs.cida.dsas.service.util.Property"%>
<%@page import="gov.usgs.cida.dsas.service.util.PropertyUtil"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>

<%!
	boolean development = Boolean.parseBoolean(PropertyUtil.getProperty(Property.DEVELOPMENT));
	String applicationVersion = PropertyUtil.getProperty("application.version");
	String versionJquery = PropertyUtil.getProperty("version.jquery");
	String versionJqueryUi = PropertyUtil.getProperty("version.jquery.ui");
	String versionBootstrap = PropertyUtil.getProperty("version.bootstrap");
	String versionFontAwesome = PropertyUtil.getProperty("version.fontawesome");
	String versionOpenLayers = PropertyUtil.getProperty("version.openlayers");
	String versionSugar = PropertyUtil.getProperty("version.sugarjs");
	String versionBootstrapSwitch = PropertyUtil.getProperty("version.bootstrap.switch");
	String versionHandlebars = PropertyUtil.getProperty("version.handlebars");
	String versionBootstrapToggle = PropertyUtil.getProperty("version.bootstrap.toggle");
	String versionTableSorter = PropertyUtil.getProperty("version.tablesorter");
%>
<%
	String baseUrl = PropertyUtil.getProperty("dsas.base.url", request.getContextPath());
%>
<html lang="en">
    <head>
		<title>DSASweb</title>
		<link type="text/css" rel="stylesheet" href="<%=baseUrl%>/<%= org.webjars.AssetLocator.getWebJarPath("css/bootstrap" + (development ? "" : ".min") + ".css")%>" />
		<link type="text/css" rel="stylesheet" href="<%=baseUrl%>/webjars/font-awesome/<%=versionFontAwesome%>/css/font-awesome<%= development ? "" : ".min"%>.css" />
		<link type="text/css" rel="stylesheet" href="<%=baseUrl%>/webjars/bootstrap-toggle/<%=versionBootstrapToggle%>/css/bootstrap-toggle<%= development ? "" : ".min"%>.css" />
		<link type="text/css" rel="stylesheet" href="<%=baseUrl%>/webjars/tablesorter/<%=versionTableSorter%>/css/theme.default.css" />
		<link type="text/css" rel="stylesheet" href="<%=baseUrl%>/webjars/jquery-ui/<%= versionJqueryUi%>/jquery-ui.min.css" />
		<link type="text/css" rel="stylesheet" href="css/home/home.css" />
		<link type="text/css" rel="stylesheet" href="css/shorelines/shorelines.css" />
		<link type="text/css" rel="stylesheet" href="css/map/map.css" />
    </head>

    <body>

		<div id="page-content-container" class="container-fluid" role="application"></div>

		<script type="text/javascript">
			var require = {
				config: {
					'init': {
						'contextPath': "<%=baseUrl%>/"
					},
					'utils/OwsUtil': {
						'geoserverProxyEndpoint' : 'geoserver/'
					},
					'utils/ShorelineUtil': {
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
						},
						'geoserverProxyEndpoint' : 'geoserver/'
					}
				},
				baseUrl: "<%=baseUrl%>/js/",
				paths: {
					"bootstrap": ["<%=baseUrl%>/webjars/bootstrap/<%= PropertyUtil.getProperty("version.bootstrap")%>/js/bootstrap<%= development ? "" : ".min"%>"],
					"jquery": ["<%=baseUrl%>/webjars/jquery/<%=  PropertyUtil.getProperty("version.jquery")%>/jquery<%= development ? "" : ".min"%>"],
					"backbone": ['<%=baseUrl%>/webjars/backbonejs/<%=  PropertyUtil.getProperty("version.backbone")%>/backbone<%= development ? "" : "-min"%>'],
					"underscore": ['<%=baseUrl%>/webjars/underscorejs/<%=  PropertyUtil.getProperty("version.underscore")%>/underscore<%= development ? "" : "-min"%>'],
					"handlebars": ['<%=baseUrl%>/webjars/handlebars/<%=  PropertyUtil.getProperty("version.handlebars")%>/handlebars<%= development ? "" : ".min"%>'],
					"text": ['<%=baseUrl%>/webjars/requirejs-text/<%=  PropertyUtil.getProperty("version.require.text")%>/text'],
					"loglevel": ['<%=baseUrl%>/webjars/loglevel/<%=  PropertyUtil.getProperty("version.loglevel")%>/loglevel<%= development ? "" : ".min"%>'],
					"openlayers": ['<%=baseUrl%>/webjars/openlayers/<%= versionOpenLayers%>/OpenLayers<%= development ? ".debug" : ""%>'],
					"jqueryui": ['<%=baseUrl%>/webjars/jquery-ui/<%= PropertyUtil.getProperty("version.jquery.ui")%>/jquery-ui<%= development ? ".min" : ""%>'],
					"localstorage": ['<%=baseUrl%>/webjars/backbone-localstorage/<%=  PropertyUtil.getProperty("version.backbone-localstorage")%>/backbone.localStorage<%= development ? "" : "-min"%>'],
					"bootstrapToggle": ['<%=baseUrl%>/webjars/bootstrap-toggle/<%=versionBootstrapToggle%>/js/bootstrap-toggle<%= development ? "" : ".min"%>'],
					"tablesorter": ['<%=baseUrl%>/webjars/tablesorter/<%=versionTableSorter%>/js/jquery.tablesorter<%= development ? "" : ".min"%>']
				},
				shim: {
					openlayers: {
						exports: "OpenLayers"
					},
					bootstrapToggle : {
						"deps" :['jquery'],
						exports: "Toggle"
					},
					tablesorter : {
						"deps" :['jquery']
					},
					"bootstrap": { 
						"deps" :['jquery'] 
					}
				}
			};
		</script>
		<script data-main="init" src="<%=baseUrl%>/<%= org.webjars.AssetLocator.getWebJarPath("require" + (development ? "" : ".min") + ".js")%>"></script>

	</body>
</html>
