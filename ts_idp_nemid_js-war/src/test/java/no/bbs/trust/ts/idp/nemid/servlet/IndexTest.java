package no.bbs.trust.ts.idp.nemid.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import no.bbs.tt.trustsign.trustsignDAL.config.ConnectionFactories;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SigningProcess;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

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
		final String sref = SREF;
		Index index = new Index();
		index.init(null);
		String mid = DAOUtil.getSessionDataByKey(sref, ConfigKeys.SESSIONKEY_MID);
		OcesJsonParameterGenerator clientGenerator = Index.createClientGenerator(mid);

		int spid = (int) StringUtils.toLong(DAOUtil.getSessionDataByKey(sref, ConfigKeys.SESSIONKEY_SPID), 0);
		SigningProcess signingProcess = DAOUtil.getSigningProcess(spid);
		Index.setSigningDocument(clientGenerator, signingProcess, sref);
		String challenge = ChallengeGenerator.generateChallenge();
		String clientTag = clientGenerator.generateClientTag("standard", "en", challenge, sref);

		assertClientTag(clientTag);
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
