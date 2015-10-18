
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
%>
<%
	String baseUrl = PropertyUtil.getProperty("dsas.base.url", request.getContextPath());
%>

<html lang="en">

    <head>
		<title>DSASweb</title>
		<link type="text/css" rel="stylesheet" href="<%=baseUrl%>/<%= org.webjars.AssetLocator.getWebJarPath("css/bootstrap" + (development ? "" : ".min") + ".css")%>" />
		<link type="text/css" rel="stylesheet" href="webjars/font-awesome/<%=versionFontAwesome%>/css/font-awesome<%= development ? "" : ".min"%>.css" />
    </head>

    <body>

		<div id="page-content-container" class="container-fluid" role="application"></div>

		<script type="text/javascript">
			var require = {
				config: {
					'init': {
						'contextPath': "<%=baseUrl%>/"
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
					"openlayers": ['<%=baseUrl%>/webjars/openlayers/<%= versionOpenLayers%>/OpenLayers<%= development ? ".debug" : ""%>']
				},
				shim: {
					openlayers: {
						exports: "OpenLayers"
					},
					"bootstrap": ["jquery"]
				}
			};
		</script>
		<script data-main="init" src="<%=baseUrl%>/<%= org.webjars.AssetLocator.getWebJarPath("require" + (development ? "" : ".min") + ".js")%>"></script>

	</body>
</html>
