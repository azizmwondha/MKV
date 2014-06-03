package no.bbs.trust.ts.idp.nemid.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.security.Security;

import javax.servlet.ServletException;

import no.bbs.trust.common.basics.types.Dispatch;
import no.bbs.trust.common.basics.types.ReturnCode;
import no.bbs.trust.common.config.Config;
import no.bbs.trust.common.config.ConfigStarter;
import no.bbs.trust.common.webapp.servlets.InitConfig;
import no.bbs.trust.ts.idp.nemid.contants.ConfigKeys;
import no.bbs.trust.ts.idp.nemid.db.OracleConnectionFactory;
import no.bbs.tt.trustsign.trustsignDAL.config.ConnectionFactories;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class IndexTest {

	private static final String SREF = "AE910DF8E2D812F5E53831032437F078C5F76260";
	private static final String TARGET = "/index.jsp";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		InitConfig config = new InitConfig();
		ConfigStarter.initConfig(config.getConfigPropertySources("../ts_idp_nemid_js-config/env/common"), config.getConfigPropertySettings());
		ConnectionFactories.getInstance().registerDBConnectionFactory(new OracleConnectionFactory());
		Security.addProvider(new BouncyCastleProvider());
	}

	@SuppressWarnings("static-method")
	@Test
	public void testServiceRequest() throws ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter(ConfigKeys.PARAM_SREF, SREF);
		MockHttpServletResponse response = new MockHttpServletResponse();
		Index index = new Index();
		index.init(null);

		try {
			ReturnCode returnCode = index.serviceRequest(request, response);
			assertNotNull(returnCode);
			assertEquals(Dispatch.INCLUDE, returnCode.getDispatch());
			assertEquals(TARGET, returnCode.getTarget());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@SuppressWarnings("static-method")
	@Test
	public void testHandleRequest() throws ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter(ConfigKeys.PARAM_SREF, SREF);
		MockHttpServletResponse response = new MockHttpServletResponse();
		Index index = new Index();
		index.init(null);

		try {
			ReturnCode returnCode = index.handleRequest(request, response);
			assertNotNull(returnCode);
			assertEquals(Dispatch.INCLUDE, returnCode.getDispatch());
			assertEquals(TARGET, returnCode.getTarget());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@SuppressWarnings("static-method")
	@Test
	public void testGetClientModeNull() {
		String clientMode = Index.getClientMode(null);
		assertNotNull(clientMode);
		assertEquals(Config.INSTANCE.getProperty(ConfigKeys.CONFIG_NEMID_CLIENTMODE_STANDARD), clientMode);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testGetClientModeEmpty() {
		String clientMode = Index.getClientMode("");
		assertNotNull(clientMode);
		assertEquals(Config.INSTANCE.getProperty(ConfigKeys.CONFIG_NEMID_CLIENTMODE_STANDARD), clientMode);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testGetClientModeStandard() {
		String clientMode = Index.getClientMode("standard");
		assertNotNull(clientMode);
		assertEquals(Config.INSTANCE.getProperty(ConfigKeys.CONFIG_NEMID_CLIENTMODE_STANDARD), clientMode);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testGetClientModeLimited() {
		String clientMode = Index.getClientMode("limited");
		assertNotNull(clientMode);
		assertEquals(Config.INSTANCE.getProperty(ConfigKeys.CONFIG_NEMID_CLIENTMODE_LIMITED), clientMode);
	}

}
