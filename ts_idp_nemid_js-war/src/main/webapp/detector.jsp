<%
	String serverurl = (String) request.getAttribute("URI_SIGN");
	String useragent = (String) request.getHeader("User-Agent");
	String javatiming = (String) request.getAttribute("javatiming");
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
 <head>
 <link href="/sign/signng.css" rel="stylesheet" type="text/css" />
 <script type="text/javascript">
	 function redir() {
		 window.top.location.replace("<%=serverurl%>?javatiming=<%=javatiming%>&useragent=<%=useragent%>");
	 }
 </script>
 </head>

<body onLoad="setTimeout('redir()', 5000)">
<embed id="UAInfoApplet"
       type="application/x-java-applet;version=1.5"
       width="0" height="0"
       codebase="/sign/content/"
       code="no.bbs.tt.trustsign.signweb.misc.Detector"
       pluginspage="http://java.com/download/"
       serverurl="/dknemid/detector.html"
       useragent="<%=useragent%>"
       javatiming="<%=javatiming%>"><div class="infotext">Detecting if Java is installed on your pc and browser. This may take a while.</div>
</embed>
</body>
</html>