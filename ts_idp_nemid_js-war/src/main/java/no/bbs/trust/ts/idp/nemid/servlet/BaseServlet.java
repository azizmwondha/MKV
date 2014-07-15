package no.bbs.trust.ts.idp.nemid.servlet;

import java.io.File;
import java.io.UnsupportedEncodingException;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import no.bbs.trust.common.basics.exceptions.StatusCodeException;
import no.bbs.trust.common.basics.types.Dispatch;
import no.bbs.trust.common.basics.types.ReturnCode;

import no.bbs.trust.common.basics.utils.EventLogger;
import no.bbs.trust.common.config.Config;
import no.bbs.trust.common.errors.Errors;
import no.bbs.trust.common.webapp.servlets.BaseSignServlet;
import no.bbs.trust.ts.idp.nemid.contants.ConfigKeys;
import no.bbs.trust.ts.idp.nemid.event.NemIDActionEvent;
import no.bbs.trust.ts.idp.nemid.event.NemIDPerformanceEvent;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.PropertyConfigurator;
import org.bouncycastle.util.encoders.Base64;

public abstract class BaseServlet extends BaseSignServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected ReturnCode handleRequest(HttpServletRequest request, HttpServletResponse response) throws StatusCodeException {
		long start = System.currentTimeMillis();
		buildMDC(request);
		dumpRequest(request);

		logger.info("[AE=" + NemIDActionEvent.REQUEST_RECEIVED.getCode() + "]");

		try {
			logger.debug("NemID incoming request");
			return serviceRequest(request, response);
		} catch (StatusCodeException sce) {
			EventLogger.dumpStack(sce, Level.INFO);
			no.bbs.trust.common.errors.xml.Error e = Errors.getInstance().getError(sce.getActionEvent().getCode());
			logger.info("[StatusCode=" + sce.getActionEvent().getCode() + "][EventCode=" + sce.getActionEvent().toString() + "][StatusLabel=" + e.getLabel()
					+ "]");
			logger.info("[StatusCodeMessage=" + sce.getMessage() + "]");

			String sref = request.getParameter(ConfigKeys.PARAM_SREF);
			return new ReturnCode(Dispatch.REDIRECT, getConfigProperty(ConfigKeys.CONFIG_NEMID_STATUSURL) + "?status=" + e.getLabel() + "&sref=" + sref);
		} catch (Throwable t) {
			EventLogger.dumpStack(t, "INFO");
			String sref = request.getParameter(ConfigKeys.PARAM_SREF);
			return new ReturnCode(Dispatch.REDIRECT, getConfigProperty(ConfigKeys.CONFIG_NEMID_STATUSURL) + "?status=generalerror&sref=" + sref);
		} finally {
			EventLogger.appendEvent(NemIDActionEvent.RESPONSE_SENT);
			EventLogger.appendEvent(NemIDPerformanceEvent.DK_NEMID_HANDLE_REQUEST, start);
			EventLogger.flush();
		}
	}

	protected static String getConfigProperty(String pname) {
		return Config.INSTANCE.getProperty(pname);
	}

	protected static byte[] decodeUTF8b64To88591Bytes(String b64in) throws StatusCodeException {
		try {
			byte[] utfbytes = Base64.decode(b64in);
			b64in = null;
			return new String(utfbytes, "UTF-8").getBytes("ISO-8859-1");
		} catch (UnsupportedEncodingException uex) {
			EventLogger.dumpStack(uex, logger);
			throw new StatusCodeException(NemIDActionEvent.STATUS_UNEXPECTED_INTERNAL_ERROR, uex.getMessage());
		}
	}

	/**
	 * Initialize servlet.
	 */
	public final void init() throws ServletException {
		if (null == logger) {
			String log4jPropFile = null;
			String configDir = getInitParameter("configdir");

			if (null != configDir) {
				File conf = new File(configDir);
				if ((!conf.exists()) || (!conf.isDirectory())) {
					throw new UnavailableException("ConfigDir  (" + configDir + ") is not a valid Directory");
				}

				log4jPropFile = configDir + "/log4j.properties";

				File logfile = new File(log4jPropFile);
				if (!logfile.exists()) {
					throw new UnavailableException("Could not find log4j file: [" + log4jPropFile + "]");
				}

				PropertyConfigurator.configure(log4jPropFile);
			}
			logger = Logger.getLogger(no.bbs.trust.common.basics.constants.Constants.MAIN_LOGGER);
			logger.info("Initialized log4j to use " + no.bbs.trust.common.basics.constants.Constants.MAIN_LOGGER);
		}

		try {
			doInit();
		} catch (Throwable t) {
			EventLogger.dumpStack(t);
			throw new UnavailableException("Servlet Startup failed. " + t.getMessage());
		}
	}

	/**
	 * Clean up and terminate.
	 */
	public void destroy() {
		super.destroy();
		logger.removeAllAppenders();
	}

	public abstract void doInit() throws ServletException;

	protected abstract ReturnCode serviceRequest(HttpServletRequest request, HttpServletResponse response) throws StatusCodeException;

	private void buildMDC(HttpServletRequest request) {
		String sref = request.getParameter(ConfigKeys.PARAM_SREF);
		if (null != sref) {
			MDC.put("TID", sref);
		}
		try {
			MDC.put("IP", "" + getClientIP(request));
		} catch (StatusCodeException sce) {
			EventLogger.dumpStack(sce, "DEBUG");
		}
	}

}
