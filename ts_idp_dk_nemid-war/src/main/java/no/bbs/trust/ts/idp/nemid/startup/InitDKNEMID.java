/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package no.bbs.trust.ts.idp.nemid.startup;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;
import org.openoces.ooapi.environment.Environments;
import org.openoces.ooapi.environment.Environments.Environment;
import org.openoces.serviceprovider.ServiceProviderSetup;

import eu.nets.sis.common.cache.loader.CacheLoader;
import eu.nets.sis.common.cache.util.CacheConstants;
import no.bbs.trust.amqcapi.types.AMQAPIException;
import no.bbs.trust.amqcapi.utils.AMQConnector;
import no.bbs.trust.common.basics.constants.Constants;
import no.bbs.trust.common.basics.exceptions.StatusCodeException;
import no.bbs.trust.common.basics.utils.EventLogger;
import no.bbs.trust.common.config.Config;
import no.bbs.trust.common.webapp.startup.InitState;
import no.bbs.trust.common.webapp.startup.StartupCheck;
import no.bbs.trust.ts.idp.nemid.contants.ConfigKeys;
import no.bbs.trust.ts.idp.nemid.event.NemIDActionEvent;
import no.bbs.tt.bc.cryptlib.util.BCCryptoLoader;
import no.bbs.tt.trustsign.trustsignDAL.constant.PKIIDMap;

public class InitDKNEMID extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private final Logger logger = Logger.getLogger(Constants.MAIN_LOGGER);

	@Override
	public void init(ServletConfig config) throws ServletException {
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
		initCache();
		initActiveMQConnection();
		EventLogger.appendEvent(NemIDActionEvent.ACTION_IDP_DK_NEMID_LIFECYCLE);

		InitState.assertInitCompletedWithoutErrors();
	}

	private void initActiveMQConnection() throws ServletException {
		String amqUrl = Config.INSTANCE.getProperty(no.bbs.trust.ts.idp.nemid.contants.Constants.ACTIVEMQ_URL);
		try {
			AMQConnector.init(amqUrl, null, AMQConnector.PoolSize.XSMALL_16);
		} catch (AMQAPIException e) {
			throw new ServletException("Unable to initialize ActiveMQ connection", e);
		}
	}

	private void initCache() {
		try {
			CacheLoader.loadCache(CacheConstants.SOURCE_ESIGN,PKIIDMap.DKNEMIDJS_ID); 
		} catch(StatusCodeException sce) {
			InitState.initFailed(this, "Cache initialization failed.  Check startup logs");
		}
	}

	

	@Override
	public void destroy() {
		EventLogger.appendEvent(NemIDActionEvent.ACTION_IDP_DK_NEMID_LIFECYCLE);
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

		System.setProperty("http.proxyHost", Config.INSTANCE.getProperty(ConfigKeys.CPR_LOOKUP_PROXYHOST));
		System.setProperty("http.proxyPort", Config.INSTANCE.getProperty(ConfigKeys.CPR_LOOKUP_PROXYPORT));
		System.setProperty("https.proxyHost", Config.INSTANCE.getProperty(ConfigKeys.CPR_LOOKUP_PROXYHOST));
		System.setProperty("https.proxyPort", Config.INSTANCE.getProperty(ConfigKeys.CPR_LOOKUP_PROXYPORT));

		EventLogger.appendEvent(NemIDActionEvent.ACTION_LOAD_OCES_CONFIG);
		logger.info("OCES config for IDP[" + PKIIDMap.DKNEMID_NAME + "] completed");
	}

}
