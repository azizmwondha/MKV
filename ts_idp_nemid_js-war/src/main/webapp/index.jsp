<!doctype html>
<%@ page import="no.bbs.trust.common.config.Config" %>
<%@ page import="no.bbs.trust.common.i18n.LangSupport" %>
<%@ page import="no.bbs.trust.ts.idp.nemid.contants.ConfigKeys" %>
<%
    String locale = (String) request.getAttribute("locale");
%>
<html>
<head>
    <meta http-equiv="content-Type" content="text/html; charset=<%= Config.INSTANCE.getProperty(ConfigKeys.CONFIG_HTML_CHARSET)%>">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes">
    <meta http-equiv="X-UA-Compatible" content="IE=Edge">
    <title><%= LangSupport.getUserText("digitalsignature", locale) %></title>
    <link href="<%= (String) request.getAttribute("styleurl") %>" rel="stylesheet" type="text/css">
</head>
<body class="iframe">
	<div class="ipage">
		<div class="main">
			<div class="nemid_client">
				<!-- Client tag -->
				<%= (String) request.getAttribute("clienttag") %>
				<!-- /Client tag -->
			</div>
			
			<!-- Signer status table -->
			<jsp:include page="inc_statustable.jsp"></jsp:include>
			<!-- /Signer status table -->
		</div>
	</div>
</body>
</html>
