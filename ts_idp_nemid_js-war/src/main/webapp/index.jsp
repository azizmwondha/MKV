<html xmlns="http://www.w3.org/1999/xhtml">
	<%@ page import="no.bbs.trust.common.basics.constants.Parameters" %>
	<%@ page import="no.bbs.trust.ts.idp.nemid.contants.ConfigKeys" %>
	<%@ page import="no.bbs.trust.common.config.Config" %>
	<%@ page import="no.bbs.trust.common.i18n.LangSupport" %>
	<%
		String locale = "" + (String) request.getAttribute("locale");
	%>
	<head>
		<meta http-equiv="content-Type" content="text/html; charset=<%= Config.INSTANCE.getProperty(ConfigKeys.CONFIG_HTML_CHARSET)%>" />
		<title><%= LangSupport.getUserText("digitalsignature", locale) %></title>
		<link href="<%= (String) request.getAttribute("styleurl") %>" rel="stylesheet" type="text/css" />
		</head>
		<body class="signiframe">
			<div class="signipage">
				<!-- Client tag -->
				<%= (String) request.getAttribute("clienttag") %>
				<!-- /Client tag -->

			<!-- Signer status table -->
			<jsp:include page="inc_statustable.jsp"></jsp:include>
			<!-- /Signer status table -->
		</div>
	</body>
</html>
