<%@page import="gov.usgs.cida.dsas.rest.service.ServiceURI"%>
<%@page import="gov.usgs.cida.dsas.service.util.PropertyUtil"%>
<%@page import="gov.usgs.cida.dsas.service.util.Property"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>

<%!
	boolean development = Boolean.parseBoolean(PropertyUtil.getProperty(Property.DEVELOPMENT));
	String applicationVersion = PropertyUtil.getProperty("application.version");
%>

<%
	String baseUrl = PropertyUtil.getProperty("dsas.base.url", request.getContextPath());
%>

<html lang="en">

    <head>
		<title>DSASweb</title>
		<link type="text/css" rel="stylesheet" href="<%=baseUrl%>/webjars/bootstrap/<%= PropertyUtil.getProperty("version.bootstrap")%>/css/bootstrap<%= development ? "" : ".min"%>.css" />
		<link type="text/css" rel="stylesheet" href="<%=baseUrl%>/webjars/font-awesome/<%=PropertyUtil.getProperty("version.fontawesome")%>/css/font-awesome<%= development ? "" : ".min"%>.css" />
		<link type="text/css" rel="stylesheet" href="<%=baseUrl%>/css/pdb-management/pdb-management.css" />
	</head>
	<body>
		<div id="page-content-container" class="container-fluid" role="application"></div>
		<script type="text/javascript">
			var require = {
				config: {
					'init': {
						'contextPath': "<%=baseUrl%>"
					},
					'views/ManagementView' : {
						'paths' : {
							'staging' : '<%= ServiceURI.SHAPEFILE_SERVICE_ENDPOINT %>'
						}
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