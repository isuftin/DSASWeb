
<%@page import="java.util.Arrays"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page import="gov.usgs.cida.dsas.dao.pdb.Pdb"%>
<%@page import="gov.usgs.cida.dsas.rest.service.ServiceURI"%>
<%@page import="gov.usgs.cida.dsas.utilities.properties.Property"%>
<%@page import="gov.usgs.cida.dsas.utilities.properties.PropertyUtil"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>

<%!
	boolean development = Boolean.parseBoolean(PropertyUtil.getProperty(Property.DEVELOPMENT));
	String applicationVersion = PropertyUtil.getProperty("application.version");
%>

<%
	String baseUrl = PropertyUtil.getProperty("dsas.base.url", request.getContextPath());
	int contextSpacing = StringUtils.countMatches(baseUrl, "/");
	String[] backTicksArray = new String[contextSpacing];
	String backticks = "";
	if (backTicksArray.length > 0) {
		Arrays.fill(backTicksArray, "../");
		backTicksArray[backTicksArray.length - 1] = backTicksArray[backTicksArray.length - 1].substring(0, 2);
		backticks = String.join("", backTicksArray);
	}
%>

<html lang="en">

    <head>
		<title>DSASweb Management</title>
		<link type="text/css" rel="stylesheet" href="<%=baseUrl%>/webjars/jquery-ui/<%= PropertyUtil.getProperty("version.jquery.ui") %>/jquery-ui.min.css" />
		<link type="text/css" rel="stylesheet" href="<%=baseUrl%>/webjars/bootstrap/<%= PropertyUtil.getProperty("version.bootstrap")%>/css/bootstrap<%= development ? "" : ".min"%>.css" />
		<link type="text/css" rel="stylesheet" href="<%=baseUrl%>/webjars/font-awesome/<%=PropertyUtil.getProperty("version.fontawesome")%>/css/font-awesome<%= development ? "" : ".min"%>.css" />
		<link type="text/css" rel="stylesheet" href="<%=baseUrl%>/css/management/management.css" />
	</head>
	<body>
		<div id="page-content-container" class="container-fluid" role="application"></div>
		<script type="text/javascript">
			var require = {
				config: {
					'init': {
						'contextPath': "<%=baseUrl%>"
					},
					'views/ManagementView': {
						'paths': {
							'staging': '<%= backticks %><%= ServiceURI.SHAPEFILE_SERVICE_ENDPOINT%>'
						}
					},
					'utils/PdbUtil' : {
						'pdbRequiredColumns' : ['<%= StringUtils.join(Pdb.REQUIRED_FIELD_NAMES, "','") %>']
					}
				},
				baseUrl: "<%=baseUrl%>/js/",
				paths: {
					"bootstrap": ["<%=baseUrl%>/webjars/bootstrap/<%= PropertyUtil.getProperty("version.bootstrap")%>/js/bootstrap<%= development ? "" : ".min"%>"],
					"jquery": ["<%=baseUrl%>/webjars/jquery/<%=  PropertyUtil.getProperty("version.jquery")%>/jquery<%= development ? "" : ".min"%>"],
					"jqueryui": ['<%=baseUrl%>/webjars/jquery-ui/<%= PropertyUtil.getProperty("version.jquery.ui")%>/jquery-ui<%= development ? ".min" : ""%>'],
					"backbone": ['<%=baseUrl%>/webjars/backbonejs/<%=  PropertyUtil.getProperty("version.backbone")%>/backbone<%= development ? "" : "-min"%>'],
					"underscore": ['<%=baseUrl%>/webjars/underscorejs/<%=  PropertyUtil.getProperty("version.underscore")%>/underscore<%= development ? "" : "-min"%>'],
					"handlebars": ['<%=baseUrl%>/webjars/handlebars/<%=  PropertyUtil.getProperty("version.handlebars")%>/handlebars<%= development ? "" : ".min"%>'],
					"text": ['<%=baseUrl%>/webjars/requirejs-text/<%=  PropertyUtil.getProperty("version.require.text")%>/text'],
					"loglevel": ['<%=baseUrl%>/webjars/loglevel/<%=  PropertyUtil.getProperty("version.loglevel")%>/loglevel<%= development ? "" : ".min"%>'],
					"openlayers": ['<%=baseUrl%>/webjars/openlayers/<%= PropertyUtil.getProperty("version.openlayers")%>/OpenLayers<%= development ? ".debug" : ""%>']
				},
				shim: {
					openlayers: {
						exports: "OpenLayers"
					},
					"bootstrap": { 
						"deps" :['jquery'] 
					}
				}
			};
		</script>
		<script data-main="init"  src="<%=baseUrl%>/webjars/requirejs/<%= PropertyUtil.getProperty("version.require")%>/require<%= development ? "" : ".min"%>.js"></script>
	</body>
</html>
