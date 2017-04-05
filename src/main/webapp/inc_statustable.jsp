<%@page import="java.util.Date" %>
<%@page import="java.util.ResourceBundle" %>
<%@page import="no.bbs.trust.common.i18n.LangSupport" %>
<%@page import="no.bbs.trust.ts2.idp.common.statustable.StatusTableFormatter" %>
<%@page import="no.bbs.trust.ts2.idp.common.vo.StatusTable" %>
<%
    String locale = (String) request.getAttribute("locale");
    ResourceBundle bundle = LangSupport.getUserTextBundle(locale);
    StatusTable statusTable = (StatusTable) request.getAttribute("statustable");
    Date deadline = (Date) request.getAttribute("deadline");
    String tzo = (String) request.getAttribute("tzo");

    StatusTableFormatter statusTableFormatter = new StatusTableFormatter();
%>
<%= statusTableFormatter.htmlFormat(statusTable, bundle, deadline, tzo) %>

