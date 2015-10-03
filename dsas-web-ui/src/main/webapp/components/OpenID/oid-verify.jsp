
<?xml version="1.0" encoding="UTF-8"?>
<%@page contentType="text/html; charset=UTF-8" import="java.util.Map" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
	<head>
		<title>Verified</title>
		<meta http-equiv="Content-Type" content="application/xhtml+xml; charset=UTF-8" />
	</head>
	<body>
		<script>
				var returnAttributes = {};
				<c:forEach items="${attributes}" var="attribute">
				returnAttributes['${attribute.key}'] = '${attribute.value}';
				</c:forEach>
				window.opener.CONFIG.ui.createLoginMenuItem();
		</script>
	</body>
</html>