package no.bbs.trust.ts.idp.nemid.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import no.bbs.trust.common.basics.exceptions.StatusCodeException;
import no.bbs.trust.common.basics.types.ReturnCode;
import no.bbs.trust.common.basics.utils.EventLogger;
import no.bbs.trust.common.config.Config;
import no.bbs.trust.common.webapp.servlets.BaseSignServlet;
import no.bbs.trust.ts.idp.nemid.event.NemIDActionEvent;
import no.bbs.trust.ts.idp.nemid.event.NemIDPerformanceEvent;

import org.apache.log4j.MDC;

public abstract class BaseServlet extends BaseSignServlet {

	@Override
	protected ReturnCode handleRequest(HttpServletRequest request, HttpServletResponse response) throws StatusCodeException {
		long start = System.currentTimeMillis();
		dumpRequest(request);

		logger.info("[AE=" + NemIDActionEvent.REQUEST_RECEIVED.getCode() + "]");

		try {
			return serviceRequest(request, response);
		} finally {
			EventLogger.appendEvent(NemIDActionEvent.RESPONSE_SENT);
			EventLogger.appendEvent(NemIDPerformanceEvent.DK_NEMID_HANDLE_REQUEST, start);
			EventLogger.flush();
		}
	}

	protected static String getConfigProperty(String pname) {
		return Config.INSTANCE.getProperty(pname);
	}

	protected abstract ReturnCode serviceRequest(HttpServletRequest request, HttpServletResponse response) throws StatusCodeException;

	@Override
	protected String buildMDC(HttpServletRequest request, String tidParam) {
		String sref = null;
		try {
			sref = request.getParameter(tidParam);
		} catch (IllegalStateException e) {
			String queryString = request.getQueryString();
			if (queryString.startsWith("sref=")) {
				sref = queryString.substring(5);
			}

		}

		if (null != sref) {
			updateMDCTID(sref);
		} else {
			sref = "no-tid-" + System.currentTimeMillis() + "-" + Math.random();
			updateMDCTID(sref);
			request.setAttribute("no-tid-sref", sref);
		}
		try {
			MDC.remove("IP");
			MDC.put("IP", "" + getClientIP(request));
		} catch (StatusCodeException sce) {
			EventLogger.dumpStack(sce, "DEBUG");
		}
		return sref;
	}

}
