package no.bbs.trust.ts.idp.nemid.tag;

import java.security.Security;
import java.sql.SQLException;

import no.bbs.trust.common.basics.exceptions.StatusCodeException;
import no.bbs.trust.common.config.Config;
import no.bbs.trust.common.config.ConfigStarter;
import no.bbs.trust.common.webapp.servlets.InitConfig;
import no.bbs.trust.ts.idp.nemid.contants.ConfigKeys;
import no.bbs.trust.ts.idp.nemid.contants.Constants;
import no.bbs.trust.ts.idp.nemid.db.OracleConnectionFactory;
import no.bbs.trust.ts.idp.nemid.event.NemIDActionEvent;
import no.bbs.trust.ts.idp.nemid.utils.DAOUtil;
import no.bbs.tt.trustsign.trustsignDAL.config.ConnectionFactories;

import org.apache.log4j.MDC;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.nets.no.vas.esign.sdosigner.types.KeyCredentials;

public class AppletElementGeneratorTest {

	private static final String SREF = "A92ED314B462D52159965681E5FD4F3AA3AF7D28";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		InitConfig config = new InitConfig();
		ConfigStarter.initConfig(config.getConfigPropertySources("C:/Users/shboh/Projects/SIS/ts_idp_nemid/ts_idp_nemid-config/env/common"), config.getConfigPropertySettings());
		ConnectionFactories.getInstance().registerDBConnectionFactory(new OracleConnectionFactory());
		Security.addProvider(new BouncyCastleProvider());
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGenerateSignAppletElement() throws StatusCodeException {
		String midf = DAOUtil.getSessionDataByKey(SREF, ConfigKeys.SESSIONKEY_MID);

		MDC.put("MID", midf);
		AppletElementGenerator appletGenerator = createGenerator(midf);
//		appletGenerator.generateSignAppletElement(SREF, Constants.SUBJECT_DN_FILTER_PID, Config.INSTANCE.getProperty(ConfigKeys.CONFIG_NEMID_TAG_VERIFYURL));
		appletGenerator.generateSignAppletElement(SREF, Constants.SUBJECT_DN_FILTER_PID, "verifyurl");

	}

	private AppletElementGenerator createGenerator(String midf) throws StatusCodeException {
		try {
			KeyCredentials credentials = DAOUtil.getMerchantCredentials(midf);

			String keystore = credentials.getKeystorepath();
			String keystorePasswd = credentials.getKeystorepass();
			String alias = credentials.getKeyalias();
			String aliasPasswd = credentials.getKeyaliaspass();

			Signer appletSigner = null;
			try {
				appletSigner = new Signer(keystore, keystorePasswd, alias, aliasPasswd);
			} catch (Throwable t) {
				throw new StatusCodeException(NemIDActionEvent.STATUS_IDP_OPERATION_FAILED, "Cannot create applet signer for Merchant [" + midf + "] "
						+ t.getMessage());
			}
			String challenge = ChallengeGenerator.generateChallenge();

			AppletElementGenerator generator = new AppletElementGenerator(appletSigner);
			generator.setServerUrlPrefix(Config.INSTANCE.getProperty(ConfigKeys.APPLET_URL_PREFIX));
			generator.setChallenge(challenge);
			generator.setLogLevel("debug"); // INFO/DEBUG/ERROR
			generator.addSignedParameter("always_embedded", "true");

			return generator;
		} catch (SQLException ex) {
			throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, "Unable to obtain key credentials for Merchant [" + midf + "] "
					+ ex.getMessage());
		}
	}

}
