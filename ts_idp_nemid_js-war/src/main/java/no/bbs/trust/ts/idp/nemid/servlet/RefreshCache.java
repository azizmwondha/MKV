package no.bbs.trust.ts.idp.nemid.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import no.bbs.trust.common.basics.constants.Constants;
import no.bbs.trust.common.basics.constants.Parameters;
import no.bbs.trust.common.basics.exceptions.StatusCodeException;
import no.bbs.trust.common.basics.utils.EventLogger;
import no.bbs.trust.common.basics.utils.StringUtils;
import no.bbs.trust.common.webapp.utils.SessionUtils;
import no.bbs.trust.common.webapp.utils.StackLogger;
import no.bbs.trust.ts.idp.nemid.event.NemIDActionEvent;
import no.bbs.trust.ts.idp.nemid.utils.NemIDUtils;
import no.bbs.trust.ts2.idp.common.context.merchant.MerchantContextCache;
import no.bbs.tt.trustsign.trustsignDAL.constant.PKIIDMap;
import org.apache.log4j.Logger;

/**
 * Refreshes Merchant context cache.
 */
public class RefreshCache extends HttpServlet {
	private final Logger logger = Logger.getLogger(Constants.MAIN_LOGGER);

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String midf = SessionUtils.getParameter(request, Parameters.MID);

		try {
			if (StringUtils.isNullorEmpty(midf)) {
				logger.info("Reloading all merchant contexts (no mid sent)");
				MerchantContextCache.loadMerchantContexts(PKIIDMap.DKNEMIDJS_ID);
				EventLogger.appendEvent(NemIDActionEvent.ACTION_LOAD_MERCHANT_CONTEXTS);
				logger.info("Merchant config for IDP[" + PKIIDMap.DKNEMID_NAME + "] loaded");
			} else {
				int mid = (int) StringUtils.toLong(midf, 0);
				logger.info("Reloading merchant context for mid=" + mid);

				MerchantContextCache.loadMerchantContextByMID(PKIIDMap.DKNEMIDJS_ID, mid);
				EventLogger.appendEvent(NemIDActionEvent.ACTION_LOAD_MERCHANT_CONTEXTS);
				logger.info("Merchant config for MID[" + mid + "] IDP[" + PKIIDMap.DKNEMID_NAME + "] loaded");

				NemIDUtils.initDKNEMIDForMID(midf);
			}
		} catch (StatusCodeException sce) {
			response.getOutputStream().println("Error loading merchant context [mid=" + midf + "]");
			StackLogger.logStatusCode(sce);
		}
	}
}
