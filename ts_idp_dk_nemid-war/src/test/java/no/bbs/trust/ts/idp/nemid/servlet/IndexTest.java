package no.bbs.trust.ts.idp.nemid.servlet;

import eu.nets.sis.common.cache.loader.MerchantCache;
import eu.nets.sis.common.cache.loader.SignCacheLoader;
import eu.nets.sis.common.cache.types.MerchantProviderConfig;
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
import no.bbs.tt.trustsign.trustsignDAL.constant.PKIConfigKeys;
import no.bbs.tt.trustsign.trustsignDAL.constant.PKIIDMap;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SignerId;
import no.bbs.tt.trustsign.trustsignDAL.vos.table.SigningProcess;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.ServletException;
import java.net.URL;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class IndexTest {

	protected static final Logger logger = Logger.getLogger(Constants.MAIN_LOGGER);

	private static final String SREF = "5AB9A24E14737AB9F614BC2F96CC02B2B62729F5";
	private static final Map<String, String> SESSION_DATAS = new HashMap<String, String>();

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		
		InitConfig config = new InitConfig();
		ConfigStarter.initConfig(config.getConfigPropertySources("../ts_idp_dk_nemid-config/env/common"), config
				.getConfigPropertySettings());
		ConnectionFactories.getInstance().registerDBConnectionFactory(new OracleConnectionFactory());
		SignCacheLoader.loadCache(PKIIDMap.DKNEMIDJS_ID);
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
	public void testGenerateJsonParameters() throws ServletException {
     
		MerchantProviderConfig credentials = new MerchantProviderConfig();
		
		String certFilename = findCert("VOCES_gyldig.p12");
		credentials.putConfig(PKIConfigKeys.KEYSTORE, certFilename);
		credentials.putConfig(PKIConfigKeys.KEYSTORE_PASSWORD, "Test1234");
		credentials.putConfig(PKIConfigKeys.MERCHANT_ALIAS, "nets danid a/s - tu voces gyldig");
		credentials.putConfig(PKIConfigKeys.MERCHANT_ALIAS_PASSWORD, "Test1234");
		MerchantCache.putConfig(1001, PKIIDMap.DKNEMIDJS_ID, credentials);
		
		final String sref = SREF;
		Index index = new Index();
		index.init(null);
		OcesJsonParameterGenerator clientGenerator=null;
		SigningProcess signingProcess=null;
		try{
		String mid = DAOUtil.getSessionDataByKey(sref, ConfigKeys.SESSIONKEY_MID);
		clientGenerator = Index.createClientGenerator(mid);
		
		int spid = (int) StringUtils.toLong(DAOUtil.getSessionDataByKey(sref, ConfigKeys.SESSIONKEY_SPID), 0);
		signingProcess = DAOUtil.getSigningProcess(spid);

		Index.setSigningDocument(clientGenerator, signingProcess);
		String challenge = ChallengeGenerator.generateChallenge();
		SignerId signerID = DAOUtil.getSignerID(signingProcess.getSignerId());
		String signerIDValue = null == signerID ? "" : signerID.getIdValue();
		String clientTag = clientGenerator.generateClientTag("standard", "en", challenge, sref, signerIDValue);
		assertClientTag(clientTag);
		}catch(StatusCodeException se){
			se.printStackTrace();
		}
		
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
