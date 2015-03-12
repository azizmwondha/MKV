package no.bbs.trust.ts.idp.nemid.utils;

import java.util.Set;

import no.bbs.trust.common.basics.exceptions.StatusCodeException;
import no.bbs.trust.common.webapp.utils.StackLogger;
import no.bbs.trust.ts.idp.nemid.event.NemIDActionEvent;
import no.bbs.trust.ts2.idp.common.context.merchant.MerchantContext;
import no.bbs.trust.ts2.idp.common.context.merchant.MerchantContextCache;
import no.bbs.tt.trustsign.trustsignDAL.constant.PKIConfigKeys;

/**
 *
 */
public class NemIDUtils {
	public static void initDKNEMIDForMID(String mid) {
		// Verify Merchant config
		MerchantContext mc = MerchantContextCache.getMerchantContext(mid);

		if (null == mc) {
			StatusCodeException sce = new StatusCodeException(NemIDActionEvent.STATUS_IDP_CACHE_ERROR, "Unable to retrieve merchant context from cache for Merchant[" + mid + "]");
			StackLogger.logStatusCode(sce);
		} else {
			java.util.Map<String, String> idpc = mc.getIdpConfig();

			String keystorepath = idpc.get(PKIConfigKeys.KEYSTORE);
			java.io.File file = new java.io.File(keystorepath);
			if (!file.isFile() || !file.canRead()) {
				StatusCodeException sce = new StatusCodeException(NemIDActionEvent.ACTION_LOAD_MERCHANT_CONTEXTS, "Merchant[" + mid + "] keystore[" + keystorepath
						+ "] missing, or unreadable");
				StackLogger.logStatusCode(sce);
				return;
			}
		}
	}

	public static void initDKNEMID() {
		Set<String> merchantIDs = MerchantContextCache.getMerchantIdentifiers();

		for (String mid : merchantIDs) {
			initDKNEMIDForMID(mid);
		}
	}
}
