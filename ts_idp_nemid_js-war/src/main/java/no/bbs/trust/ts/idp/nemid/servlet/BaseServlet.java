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


}
