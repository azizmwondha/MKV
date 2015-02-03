/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package no.bbs.trust.ts.idp.nemid.startup;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import no.bbs.trust.common.basics.constants.Constants;
import no.bbs.trust.common.basics.exceptions.StatusCodeException;
import no.bbs.trust.common.basics.utils.EventLogger;
import no.bbs.trust.common.config.Config;
import no.bbs.trust.common.webapp.startup.InitState;
import no.bbs.trust.common.webapp.startup.StartupCheck;
import no.bbs.trust.common.webapp.utils.StackLogger;
import no.bbs.trust.ts.idp.nemid.contants.ConfigKeys;
import no.bbs.trust.ts.idp.nemid.event.NemIDActionEvent;
import no.bbs.trust.ts.idp.nemid.utils.NemIDUtils;
import no.bbs.trust.ts.idp.nemid.war.version.ArtifactVersion;
import no.bbs.trust.ts2.idp.common.context.idprovider.IDPConfigCache;
import no.bbs.trust.ts2.idp.common.context.merchant.MerchantContextCache;
import no.bbs.tt.bc.cryptlib.util.BCCryptoLoader;
import no.bbs.tt.trustsign.trustsignDAL.constant.PKIIDMap;

import org.apache.log4j.Logger;
import org.openoces.ooapi.environment.Environments;
import org.openoces.ooapi.environment.Environments.Environment;
import org.openoces.serviceprovider.ServiceProviderSetup;

public class InitDKNEMID extends HttpServlet {

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
			BCCryptoLoader.registerBCProvider();
			initNemIDEnv();
			initMerchantCache();
			initIDPCache();
			NemIDUtils.initDKNEMID();
			EventLogger.appendEvent(NemIDActionEvent.ACTION_IDP_DK_NEMID_LIFECYCLE);
			logger.info("Starting " + ArtifactVersion.ARTIFACT_NAME + " " + ArtifactVersion.ARTIFACT_VERSION);
			InitState.assertInitCompletedWithoutErrors();
		} catch (StatusCodeException sce) {
			StackLogger.logStatusCode(sce);
		}
	}

	private void initMerchantCache() throws StatusCodeException {
		MerchantContextCache.loadMerchantContexts(PKIIDMap.DKNEMIDJS_ID);
		EventLogger.appendEvent(NemIDActionEvent.ACTION_LOAD_MERCHANT_CONTEXTS);
		logger.info("Merchant config for IDP[" + PKIIDMap.DKNEMID_NAME + "] loaded");
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

	private void initNemIDEnv() {
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

		EventLogger.appendEvent(NemIDActionEvent.ACTION_LOAD_OCES_CONFIG);
		logger.info("OCES config for IDP[" + PKIIDMap.DKNEMID_NAME + "] completed");
	}

}
