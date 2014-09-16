/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package no.bbs.trust.ts.idp.nemid.startup;

import java.security.Provider;
import java.security.Security;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import no.bbs.trust.common.basics.constants.Constants;
import no.bbs.trust.common.basics.constants.Parameters;
import no.bbs.trust.common.basics.exceptions.StatusCodeException;
import no.bbs.trust.common.basics.utils.EventLogger;
import no.bbs.trust.common.basics.utils.StringUtils;
import no.bbs.trust.common.config.Config;
import no.bbs.trust.common.webapp.startup.InitState;
import no.bbs.trust.common.webapp.startup.StartupCheck;
import no.bbs.trust.common.webapp.utils.SessionUtils;
import no.bbs.trust.common.webapp.utils.StackLogger;
import no.bbs.trust.ts.idp.nemid.contants.ConfigKeys;
import no.bbs.trust.ts.idp.nemid.event.NemIDActionEvent;
import no.bbs.trust.ts.idp.nemid.war.version.ArtifactVersion;
import no.bbs.trust.ts2.idp.common.context.idprovider.IDPConfigCache;
import no.bbs.trust.ts2.idp.common.context.merchant.MerchantContext;
import no.bbs.trust.ts2.idp.common.context.merchant.MerchantContextCache;
import no.bbs.tt.trustsign.trustsignDAL.constant.PKIConfigKeys;
import no.bbs.tt.trustsign.trustsignDAL.constant.PKIIDMap;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.openoces.ooapi.environment.Environments;
import org.openoces.ooapi.environment.Environments.Environment;
import org.openoces.serviceprovider.ServiceProviderSetup;

public class InitDKNEMID extends HttpServlet {

	private static boolean IS_CACHE_REFRESH = false;
	private final Logger logger = Logger.getLogger(Constants.MAIN_LOGGER);

	@Override
	public void init(ServletConfig config) throws ServletException {
		try {
			try {
				StartupCheck.assertActionEvents(NemIDActionEvent.values());
			} catch (Exception e) {
				InitState.initFailed(this, "Some event codes have not been defined in the errormap.xml. Check the startup logs.");
				EventLogger.appendEvent(NemIDActionEvent.ACTION_IDP_DK_NEMID_LIFECYCLE);
				logger.info("Startup failed. Some event codes have not been defined in the errormap.xml. Check the startup logs.");
			}

			InitState.assertInitCompletedWithoutErrors();
			initSecurityProvider();
			initNemIDEnv();
			initMerchantCache(0);
			initIDPCache();
			initDKNEMID();
			EventLogger.appendEvent(NemIDActionEvent.ACTION_IDP_DK_NEMID_LIFECYCLE);
			logger.info("Starting " + ArtifactVersion.ARTIFACT_NAME + " " + ArtifactVersion.ARTIFACT_VERSION);
			InitState.assertInitCompletedWithoutErrors();
		} catch (StatusCodeException sce) {
			StackLogger.logStatusCode(sce);
		}
	}

	@Override
	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		String midf = SessionUtils.getParameter(request, Parameters.MID);

		if (StringUtils.isNullorEmpty(midf)) {
			// Reload cache
			ServletConfig sc = null;
			init(sc);
		} else {
			try {
				int mid = (int) StringUtils.toLong(midf, 0);

				initMerchantCache(mid);
				initDKNEMIDForMID(midf);
			} catch (StatusCodeException sce) {
				StackLogger.logStatusCode(sce);
			}
		}
	}

	private void initMerchantCache(int mid) throws StatusCodeException {
		if (0 == mid) {
			MerchantContextCache.loadMerchantContexts(PKIIDMap.DKNEMIDJS_ID);
			EventLogger.appendEvent(NemIDActionEvent.ACTION_LOAD_MERCHANT_CONTEXTS);
			logger.info("Merchant config for IDP[" + PKIIDMap.DKNEMID_NAME + "] loaded");
		} else {
			MerchantContextCache.loadMerchantContextByMID(PKIIDMap.DKNEMIDJS_ID, mid);
			EventLogger.appendEvent(NemIDActionEvent.ACTION_LOAD_MERCHANT_CONTEXTS);
			logger.info("Merchant config for MID[" + mid + "] IDP[" + PKIIDMap.DKNEMID_NAME + "] loaded");
		}
	}

	private void initIDPCache() throws StatusCodeException {
		IDPConfigCache.loadIDPConfig(PKIIDMap.DKNEMIDJS_ID);
		EventLogger.appendEvent(NemIDActionEvent.ACTION_LOAD_IDP_CONFIG);
		logger.info("IDP config for IDP[" + PKIIDMap.DKNEMID_NAME + "] loaded");
	}

	@Override
	public void destroy() {
		EventLogger.appendEvent(NemIDActionEvent.ACTION_IDP_DK_NEMID_LIFECYCLE);
		logger.info("Stopping " + ArtifactVersion.ARTIFACT_NAME + " " + ArtifactVersion.ARTIFACT_VERSION);
		super.destroy();
	}

	private static void initDKNEMID() {
		Set<String> merchantIDs = MerchantContextCache.getMerchantIdentifiers();

		for (String mid : merchantIDs) {
			initDKNEMIDForMID(mid);
		}
	}

	private static void initDKNEMIDForMID(String mid) {
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

	private void initNemIDEnv() {
		if (!IS_CACHE_REFRESH) {
			String danidEnvironment = Config.INSTANCE.getProperty(ConfigKeys.DANID_ENVIRONMENT);

			if ("OCESII_DANID_ENV_PREPROD".equals(danidEnvironment)) {
				Environments.setEnvironments(Environment.OCESII_DANID_ENV_PREPROD);
			} else {
				Environments.setEnvironments(Environment.OCESII_DANID_ENV_PROD);
			}
			EventLogger.appendEvent(NemIDActionEvent.ACTION_LOAD_IDP_CONFIG);
			logger.info("NemID environment set to: " + danidEnvironment);

			ServiceProviderSetup.setOcspRevocationChecker();

			System.setProperty("proxyHost", Config.INSTANCE.getProperty(ConfigKeys.CPR_LOOKUP_PROXYHOST));
			System.setProperty("proxyPort", Config.INSTANCE.getProperty(ConfigKeys.CPR_LOOKUP_PROXYPORT));

			IS_CACHE_REFRESH = true;
			EventLogger.appendEvent(NemIDActionEvent.ACTION_LOAD_OCES_CONFIG);
			logger.info("OCES config for IDP[" + PKIIDMap.DKNEMID_NAME + "] completed");
		} else {
			EventLogger.appendEvent(NemIDActionEvent.ACTION_LOAD_OCES_CONFIG);
			logger.info("OCES config already set for IDP[" + PKIIDMap.DKNEMID_NAME + "]");
		}
	}

	public final void initSecurityProvider() {
		if (!IS_CACHE_REFRESH) {
			Provider[] providers = Security.getProviders();

			for (Provider provider : providers) {
				logger.debug("Security provider: " + provider.getName() + "[" + provider.getInfo() + "]");
			}

			Provider bc = Security.getProvider("BC");

			if (null != bc) {
				Security.removeProvider("BC");
			}

			BouncyCastleProvider bcp = new BouncyCastleProvider();
			Security.addProvider(bcp);

			EventLogger.appendEvent(NemIDActionEvent.ACTION_LOAD_IDP_CONFIG);
			logger.info("Registered security provider: " + bcp.getName() + "[" + bcp.getInfo() + "]");
		} else {
			EventLogger.appendEvent(NemIDActionEvent.ACTION_LOAD_IDP_CONFIG);
			logger.info("Security providers already set for IDP[" + PKIIDMap.DKNEMID_NAME + "]");
		}
	}
	
}
