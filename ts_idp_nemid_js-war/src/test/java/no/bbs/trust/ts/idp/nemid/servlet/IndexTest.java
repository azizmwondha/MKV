package no.bbs.trust.ts.idp.nemid.servlet;

import java.net.URL;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import no.bbs.trust.common.basics.constants.Constants;
import no.bbs.trust.common.basics.exceptions.StatusCodeException;
import no.bbs.trust.common.basics.utils.StringUtils;
import no.bbs.trust.common.config.Config;
import no.bbs.trust.common.config.ConfigStarter;
import no.bbs.trust.common.webapp.servlets.InitConfig;
import no.bbs.trust.ts.idp.nemid.contants.ConfigKeys;
import no.bbs.trust.ts.idp.nemid.db.OracleConnectionFactory;
import no.bbs.trust.ts.idp.nemid.tag.ChallengeGenerator;
import no.bbs.trust.ts.idp.nemid.tag.OcesJsonParameterGenerator;
import no.bbs.trust.ts.idp.nemid.utils.DAOUtil;
import no.bbs.trust.ts2.idp.common.context.merchant.MerchantContext;
import no.bbs.trust.ts2.idp.common.context.merchant.MerchantContextCache;
import no.bbs.tt.trustsign.trustsignDAL.config.ConnectionFactories;
import no.bbs.tt.trustsign.trustsignDAL.constant.PKIConfigKeys;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SigningProcess;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IndexTest {

	protected static final Logger logger = Logger.getLogger(Constants.MAIN_LOGGER);

	private static final String SREF = "A92ED314B462D52159965681E5FD4F3AA3AF7D28";
	private static final Map<String, String> SESSION_DATAS = new HashMap<String, String>();

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		InitConfig config = new InitConfig();
		ConfigStarter.initConfig(config.getConfigPropertySources("../ts_idp_nemid_js-config/env/common"), config.getConfigPropertySettings());
		if (ConnectionFactories.getInstance().getNumberOfConnectionFactories() == 0) {
			ConnectionFactories.getInstance().registerDBConnectionFactory(new OracleConnectionFactory());
		}
		Security.addProvider(new BouncyCastleProvider());
	}

	@SuppressWarnings("static-method")
	@Test
	public void testGetClientModeNull() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter(ConfigKeys.PARAM_NEMID_CLIENTMODE, (String) null);
		String clientMode = Index.getClientMode(request, SESSION_DATAS);
		assertNotNull(clientMode);
		assertEquals(Config.INSTANCE.getProperty(ConfigKeys.CONFIG_NEMID_CLIENTMODE_STANDARD), clientMode);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testGetClientModeEmpty() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter(ConfigKeys.PARAM_NEMID_CLIENTMODE, "");
		String clientMode = Index.getClientMode(request, SESSION_DATAS);
		assertNotNull(clientMode);
		assertEquals(Config.INSTANCE.getProperty(ConfigKeys.CONFIG_NEMID_CLIENTMODE_STANDARD), clientMode);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testGetClientModeStandard() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter(ConfigKeys.PARAM_NEMID_CLIENTMODE, "standard");
		String clientMode = Index.getClientMode(request, SESSION_DATAS);
		assertNotNull(clientMode);
		assertEquals(Config.INSTANCE.getProperty(ConfigKeys.CONFIG_NEMID_CLIENTMODE_STANDARD), clientMode);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testGetClientModeLimited() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter(ConfigKeys.PARAM_NEMID_CLIENTMODE, "limited");
		String clientMode = Index.getClientMode(request, SESSION_DATAS);
		assertNotNull(clientMode);
		assertEquals(Config.INSTANCE.getProperty(ConfigKeys.CONFIG_NEMID_CLIENTMODE_LIMITED), clientMode);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testGenerateJsonParameters() throws StatusCodeException, ServletException {
		MerchantContext context = new MerchantContext();
		HashMap<String, String> idpConfig = new HashMap<String, String>();
		context.setIdpConfig(idpConfig);
		MerchantContextCache.addMerchantContext("1001", context);

		String certFilename = findCert("VOCES_gyldig.p12");
		idpConfig.put(PKIConfigKeys.KEYSTORE, certFilename);
		idpConfig.put(PKIConfigKeys.KEYSTORE_PASSWORD, "Test1234");
		idpConfig.put(PKIConfigKeys.MERCHANT_ALIAS, "nets danid a/s - tu voces gyldig");
		idpConfig.put(PKIConfigKeys.MERCHANT_ALIAS_PASSWORD, "Test1234");

		final String sref = SREF;
		Index index = new Index();
		index.init(null);
		String mid = DAOUtil.getSessionDataByKey(sref, ConfigKeys.SESSIONKEY_MID);
		OcesJsonParameterGenerator clientGenerator = Index.createClientGenerator(mid);

		int spid = (int) StringUtils.toLong(DAOUtil.getSessionDataByKey(sref, ConfigKeys.SESSIONKEY_SPID), 0);
		SigningProcess signingProcess = DAOUtil.getSigningProcess(spid);
		Index.setSigningDocument(clientGenerator, signingProcess);
		String challenge = ChallengeGenerator.generateChallenge();
		String clientTag = clientGenerator.generateClientTag("standard", "en", challenge, sref);

		assertClientTag(clientTag);
	}

	private String findCert(String certFilename) {
		URL certificate = getClass().getClassLoader().getResource(certFilename);
		return certificate.getFile();
	}

	private static void assertClientTag(String clientTag) {
		assertNotNull(clientTag);
		assertTrue(clientTag.contains("<iframe id=\"nemid_iframe\""));
		assertTrue(clientTag.contains("<script>function onNemIDMessage(e)"));
		assertTrue(clientTag.contains("<form name=\"postBackForm\""));
		assertTrue(clientTag.contains("\"SIGNTEXT_FORMAT\":\"text\""));
		assertTrue(clientTag.contains("\"CLIENTMODE\":\"standard\""));
		assertTrue(clientTag.contains("\"CLIENTFLOW\":\"ocessign2\""));
		assertTrue(clientTag.contains("\"SIGN_PROPERTIES\":\"challenge="));
	}

}
