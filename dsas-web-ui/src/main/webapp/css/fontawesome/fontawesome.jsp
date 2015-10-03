
<%
    String debug = Boolean.parseBoolean(request.getParameter("debug-qualifier")) ? "" : ".min";
%>

<link type="text/css" rel="stylesheet" href="css/fontawesome/css/font-awesome<%= debug %>.css" />
<!--[if IE 7]>
    <link rel="stylesheet" href="assets/css/font-awesome-ie7.min.css">
<![endif]-->