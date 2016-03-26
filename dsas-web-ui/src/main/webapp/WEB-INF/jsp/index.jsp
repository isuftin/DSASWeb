
<%@page import="gov.usgs.cida.dsas.utilities.properties.Property"%>
<%@page import="gov.usgs.cida.dsas.utilities.properties.PropertyUtil"%>
<%@page import="java.io.File"%>
<%@page import="java.net.URL"%>
<%@page import="org.slf4j.Logger"%>
<%@page import="org.slf4j.LoggerFactory"%>
<%@page import="gov.usgs.cida.config.DynamicReadOnlyProperties"%>
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

<html lang="en">

    <head>
        <META HTTP-EQUIV="CONTENT-TYPE" CONTENT="text/html; charset=UTF-8" />
        <meta name="viewport" content="width=device-width">
        <link rel="shortcut icon" href="favicon.ico" type="image/x-icon" />
        <link rel="icon" href="favicon.ico" type="image/x-icon" />
        <!-- Le HTML5 shim, for IE6-8 support of HTML5 elements -->
        <!--[if lt IE 9]>
        <script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
        <![endif]-->
        <jsp:include page="/template/USGSHead.jsp">
            <jsp:param name="relPath" value="" />
            <jsp:param name="shortName" value="USGS DSAS" />
            <jsp:param name="title" value="USGS DSAS" />
            <jsp:param name="description" value="" />
            <jsp:param name="author" value="Ivan Suftin, Tom Kunicki, Jordan Walker, Jordan Read, Carl Schroedl" />
            <jsp:param name="keywords" value="" />
            <jsp:param name="publisher" value="" />
            <jsp:param name="revisedDate" value="" />
            <jsp:param name="nextReview" value="" />
            <jsp:param name="expires" value="never" />
            <jsp:param name="development" value="<%= development%>" />
        </jsp:include>
		<script type="text/javascript" src="webjars/jquery/<%= versionJquery%>/jquery<%= development ? "" : ".min"%>.js"></script>
		<script type="text/javascript">
			/* This application does not support <IE9 - Stop early if <IE9*/
			if (navigator.appName === 'Microsoft Internet Explorer') {
				var ua = navigator.userAgent;
				if (ua.toLowerCase().indexOf('msie 6') !== -1 || ua.toLowerCase().indexOf('msie 7') !== -1 || ua.toLowerCase().indexOf('msie 8') !== -1) {
					alert("We apologize, but this application does not support Internet Explorer versions lower than 9.0.\n\nOther supported browsers are Firefox, Chrome and Safari.");
					window.open('http://windows.microsoft.com/en-us/internet-explorer/downloads/ie-9/worldwide-languages');
				}
			}
		</script>
		<link type="text/css" rel="stylesheet" href="webjars/jquery-ui/<%= versionJqueryUi%>/jquery-ui.min.css" />
		<link type="text/css" rel="stylesheet" href="webjars/bootstrap/<%=versionBootstrap%>/css/bootstrap<%= development ? "" : ".min"%>.css" />
		<link type="text/css" rel="stylesheet" href="webjars/bootstrap/<%=versionBootstrap%>/css/bootstrap-responsive<%= development ? "" : ".min"%>.css" />
		<link type="text/css" rel="stylesheet" href="webjars/font-awesome/<%=versionFontAwesome%>/css/font-awesome<%= development ? "" : ".min"%>.css" />
		<link type="text/css" rel="stylesheet" href="webjars/bootstrap-switch/<%=versionBootstrapSwitch%>/stylesheets/bootstrap-switch.css" />
		<link type="text/css" rel="stylesheet" href="css/custom.css" />
    </head>

    <body>
        <%-- Loads during application startup, fades out when application is built --%>
        <jsp:include page="components/application-overlay.jsp"></jsp:include>

			<div class="container-fluid">
				<div class="row-fluid" id="header-row">
                <jsp:include page="/template/USGSHeader.jsp">
                    <jsp:param name="relPath" value="" />
                    <jsp:param name="header-class" value="" />
                    <jsp:param name="site-title" value="USGS DSAS" />
                </jsp:include>
				<jsp:include page="components/app-navbar.jsp"></jsp:include>
				</div>

				<div class="row-fluid" id="content-row">
					<!-- NAV -->
					<div class="span1" id='nav-list'>
						<ul id="stage-select-tablist" class="nav nav-pills nav-stacked">
                        <li><a href="#bias" data-toggle="tab"><img id="bias_img" src="images/workflow_figures/bias_future.png" title="Display Proxy-Datum Bias"/></a></li>
                        <li class="active"><a href="#shorelines" data-toggle="tab"><img id="shorelines_img" src="images/workflow_figures/shorelines.png" title="Display Shorelines"/></a></li>
                        <li><a href="#baseline" data-toggle="tab"><img id="baseline_img" src="images/workflow_figures/baseline_future.png" title="Display Baseline"/></a></li>
                        <li><a href="#transect_verification" data-toggle="tab"><img id="transect_verification_img" src="images/workflow_figures/transects_future.png" title="Verify Transects"/></a></li>
                        <li><a href="#transects" data-toggle="tab"><img id="transects_img" src="images/workflow_figures/transects_future.png" title="Calculate Transects"/></a></li>
                        <li><a href="#calculation" data-toggle="tab"><img id="calculation_img" src="images/workflow_figures/calculation_future.png" title="Show Calculation"/></a></li>
                        <li><a href="#results" data-toggle="tab"><img id="results_img" src="images/workflow_figures/results_future.png" title="Display Results"/></a></li>
                    </ul>
                    <div id="application-spinner"><img src="images/spinner/spinner3.gif" /></div>
                </div>

                <!-- Toolbox -->
                <div class="span4" id="toolbox-span">
                    <div id="toolbox-well" class="well well-small tab-content">
						<jsp:include page="stages/shorelines.jsp"></jsp:include>
						<jsp:include page="stages/baseline.jsp"></jsp:include>
						<jsp:include page="stages/transect_verification.jsp"></jsp:include>
						<jsp:include page="stages/transects.jsp"></jsp:include>
						<jsp:include page="stages/pdb.jsp"></jsp:include>
						<jsp:include page="stages/intersects.jsp"></jsp:include>
						<jsp:include page="stages/results.jsp"></jsp:include>
						</div>
					</div>

					<!-- MAP -->
					<div class="span7" id="map-span">
						<div id="map-well" class="well well-small tab-content">
							<div id="map"></div>
						</div>
					</div>

				</div>
				<div class="row-fluid" id="alert-row">
					<div id="application-alert-container" class="span11 offset1"></div>
				</div>

				<div class="row-fluid" id="footer-row">
                <jsp:include page="/template/USGSFooter.jsp">
                    <jsp:param name="relPath" value="" />
                    <jsp:param name="header-class" value="" />
                    <jsp:param name="site-url" value="<script type='text/javascript'>document.write(document.location.href);</script>" />
                    <jsp:param name="contact-info" value="<a href='mailto:DSAS_Help@usgs.gov?Subject=DSASWeb%20Feedback'>Site Administrator</a>" />
                </jsp:include>
                <p id="footer-page-version-info">Application Version: <%= applicationVersion%></p>
            </div>
        </div>

		<%-- Stuff that isn't shown in the application but is used by JS --%>
        <div id="modal-window" class="modal hide fade" tabindex="-1" role="dialog" aria-labelledby="modal-window-label" aria-hidden="true">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>
                <h3 id="modal-window-label"></h3>
            </div>
            <div class="modal-body">
                <div id="modal-body-content"></div>
            </div>
            <div class="modal-footer"></div>
        </div>
        <iframe id="download" class="hidden"></iframe>

		<script type="text/javascript">splashUpdate("Loading Graphing Utilities...");</script>
		<jsp:include page="/js/dygraphs/dygraphs.jsp">
			<jsp:param name="debug-qualifier" value="true" />
		</jsp:include>

		<script type="text/javascript">splashUpdate("Loading Logging...");</script>
		<jsp:include page="/js/log4javascript/log4javascript.jsp">
			<jsp:param name="relPath" value="" />
		</jsp:include>

		<script type="text/javascript">splashUpdate("Loading Sorting Tables...");</script>
		<jsp:include page="/js/jquery-tablesorter/package.jsp">
			<jsp:param name="relPath" value="" />
			<jsp:param name="debug-qualifier" value="<%= development%>" />
		</jsp:include>
		<script type="text/javascript" src="webjars/jquery-ui/<%= versionJqueryUi%>/jquery-ui.min.js"></script>
		<script type="text/javascript" src="webjars/sugar/<%= versionSugar%>/sugar-full<%= development ? ".development" : ".min"%>.js"></script>
		<script type="text/javascript" src="webjars/bootstrap/<%=versionBootstrap%>/js/bootstrap<%= development ? "" : ".min"%>.js"></script>
		<script type="text/javascript" src="webjars/openlayers/<%= versionOpenLayers%>/OpenLayers<%= development ? ".debug" : ""%>.js"></script>
		<script type="text/javascript" src="webjars/handlebars/<%= versionHandlebars%>/handlebars.min.js"></script>
		<jsp:include page="/js/fineuploader/fineuploader.jsp">
			<jsp:param name="debug-qualifier" value="true" />
		</jsp:include>
		<jsp:include page="components/config.jsp"></jsp:include>
		<script type="text/javascript">splashUpdate("Loading UI module...");</script>
		<script type="text/javascript" src="js/ui/ui.js"></script>
		<script type="text/javascript">splashUpdate("Loading Utilities module...");</script>
		<script type="text/javascript" src="js/util/util.js"></script>
		<script type="text/javascript">splashUpdate("Loading Mapping module...");</script>
		<script type="text/javascript" src="js/map/map.js"></script>
		<script type="text/javascript">splashUpdate("Loading Session Management module...");</script>
		<script type="text/javascript" src="js/session/session.js"></script>
		<script type="text/javascript">splashUpdate("Loading OWS module...");</script>
		<script type="text/javascript" src="js/ows/ows.js"></script>
		<script type="text/javascript">splashUpdate("Loading Shorelines module...");</script>
		<script type="text/javascript" src="js/stages/shorelines.js"></script>
		<script type="text/javascript">splashUpdate("Loading Baseline module...");</script>
		<script type="text/javascript" src="js/stages/baseline.js"></script>
		<script type="text/javascript">splashUpdate("Loading Transects module...");</script>
		<script type="text/javascript" src="js/stages/transects.js"></script>
		<script type="text/javascript">splashUpdate("Loading Proxy Datum Bias module...");</script>
		<script type="text/javascript" src="js/stages/bias.js"></script>
		<script type="text/javascript">splashUpdate("Loading Transect Verification module...");</script>
		<script type="text/javascript" src="js/stages/transect_verification.js"></script>
		<script type="text/javascript">splashUpdate("Loading Calculation module...");</script>
		<script type="text/javascript" src="js/stages/calculation.js"></script>
		<script type="text/javascript">splashUpdate("Loading Results module...");</script>
		<script type="text/javascript" src="js/stages/results.js"></script>
		<script type="text/javascript">splashUpdate("Loading Toggle plugin...");</script>
		<script type="text/javascript" src="webjars/bootstrap-switch/<%=versionBootstrapSwitch%>/js/bootstrap-switch.js"></script>
		<script type="text/javascript">splashUpdate("Loading Intro Module...");</script>
		<jsp:include page="/js/bootstro/bootstro.jsp">
			<jsp:param name="debug-qualifier" value="true" />
		</jsp:include>
		<script type="text/javascript">splashUpdate("Loading Main module...");</script>
		<script type="text/javascript" src="js/onReady.js"></script>
	</body>
</html>
