<%@page import="no.bbs.trust.ts2.idp.common.vo.SignerStatusTable"%>
<%@page import="no.bbs.trust.ts2.idp.common.vo.StatusTable"%>
<%@page import="no.bbs.trust.ts2.idp.common.statustable.StatusTableFormatter"%>
<%@page import="java.util.ResourceBundle"%>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>
<%@ page import="no.bbs.trust.common.config.Config" %>
<%@ page import="no.bbs.trust.common.i18n.LangSupport" %>
<%@ page import="org.bouncycastle.util.encoders.Base64" %>
<%
	String locale = "" + (String) request.getAttribute("locale");
	SimpleDateFormat formatter = null;
	formatter = new SimpleDateFormat(LangSupport.getUserText("format.mediumdate", locale));
	StatusTable st = (StatusTable) request.getAttribute("statustable");
	SignerStatusTable[] ssts = st.getSignerStatusTable();
%>
<div class="signinfo">
	<div class="signdeadline"><%= LangSupport.getUserText("signstatus.deadline", locale)%>: <%= (String) request.getAttribute("signstatus.deadline")%></div>

	<% if(null != st.getXofYCount()) { %>
		<div class="signxofy"><%= LangSupport.getUserText("signstatus.xofytext", locale)%>: <%= st.getXofYCount()%></div>
	<% } %>

	<% if ((null != ssts) && (ssts.length > 0)) { %>
			<div class="signstatus">
				<%
					ResourceBundle userTextBundle = LangSupport.getUserTextBundle(locale);
					Integer tzo = 0;
					// Time zone offset
					if (null != request.getAttribute("tzo")) {
						try
						{
							tzo = Integer.parseInt((String) request.getAttribute("tzo"));
						} catch (Throwable t) {
							//do nothing
						}
					}
				%>
				<%=StatusTableFormatter.htmlFormat(ssts, userTextBundle, tzo) %>
			</div>
	<% } %>
</div>
