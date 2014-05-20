<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>
<%@ page import="no.bbs.trust.common.config.Config" %>
<%@ page import="no.bbs.trust.common.i18n.LangSupport" %>
<%@ page import="no.bbs.tt.trustsign.te.xml.messages.SignerStatusTable" %>
<%@ page import="org.bouncycastle.util.encoders.Base64" %>
<%
	String locale = "" + (String) request.getAttribute("locale");
	SimpleDateFormat formatter = null;
	formatter = new SimpleDateFormat(LangSupport.getUserText("format.mediumdate", locale));

	SignerStatusTable[] ssts = (SignerStatusTable[]) request.getAttribute("statustable");
%><div class="signinfo">
	<div class="signdeadline"><%= LangSupport.getUserText("signstatus.deadline", locale)%>: <%= (String) request.getAttribute("signstatus.deadline")%></div>
	<div class="signstatus"><%
	if ((null != ssts) && (ssts.length > 0))
	{
		// The first row determines which columns are visible.
		boolean hasStatus = (ssts[0].getStatusId() > 0);

		StringBuffer html = new StringBuffer();
		html.append("<table width=\"390\" cellspacing=\"0\" cellpadding=\"0\">");
		html.append("<tr>");

		if (hasStatus)
		{
			html.append("<th width=\"57%\">").append(LangSupport.getUserText("signstatus.name", locale)).append("</th>");
			html.append("<th>").append(LangSupport.getUserText("signstatus.signed", locale)).append("</th>");
		}
		else
		{
			html.append("<th>").append(LangSupport.getUserText("signstatus.name", locale)).append("</th>");
		}
		html.append("</tr>");

		for (int i = 0; i < ssts.length; i++)
		{
			html.append("<tr>");

			try
			{
				html.append("<td>");
				// We break this line here so we don't get double <td> tags 
				// if an exception is thrown while decoding the b64 signer name.
				html.append(new String(Base64.decode(ssts[i].getName()), "UTF-8")).append("</td>");
			}
			catch (Throwable t)
			{
				// The <td> has already been appended in the try block.
				html.append("(").append(i).append(")</td>");
			}

			if (hasStatus)
			{
				switch (ssts[i].getStatusId())
				{
					case 10: // Completed
						html.append("<td>");

						if (null != ssts[i].getStatusTime())
						{
							long time = ssts[i].getStatusTime().getTime();
							long tzo = 0L;

							// Time zone offset
							if (null != request.getAttribute("tzo"))
							{
								try
								{
									tzo = Long.parseLong((String) request.getAttribute("tzo"));
								}
								catch (Throwable t)
								{
									tzo = 0L;
								}
							}
							html.append(formatter.format(new Date(time + tzo)));
						}
						else
						{
							html.append(LangSupport.getUserText("signstatus.signed", locale));
						}
						html.append("</td>");
						break;

					case 13: // Rejected
						html.append("<td>");
						html.append(LangSupport.getUserText("signstatus.rejected", locale));
						html.append("</td>");
						break;

					default:
						html.append("<td>");
						html.append(LangSupport.getUserText("signstatus.notsigned", locale));
						html.append("</td>");
						break;
				}
			}

			html.append("</tr>");
		}
		html.append("</table>");
		%><%=html.toString()%>			
<%
	}
%>
</div>
</div>

