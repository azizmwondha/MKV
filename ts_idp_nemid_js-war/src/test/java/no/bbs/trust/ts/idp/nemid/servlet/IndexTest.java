package no.bbs.trust.ts.idp.nemid.servlet;

import static org.junit.Assert.*;

import java.security.Security;

import javax.servlet.ServletException;

import no.bbs.trust.common.basics.exceptions.StatusCodeException;
import no.bbs.trust.common.basics.types.ReturnCode;
import no.bbs.trust.common.config.ConfigStarter;
import no.bbs.trust.common.webapp.servlets.InitConfig;
import no.bbs.trust.ts.idp.nemid.contants.ConfigKeys;
import no.bbs.trust.ts.idp.nemid.db.OracleConnectionFactory;
import no.bbs.tt.trustsign.trustsignDAL.config.ConnectionFactories;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class IndexTest {

	public static final String SREF = "AE910DF8E2D812F5E53831032437F078C5F76260";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		InitConfig config = new InitConfig();
		ConfigStarter.initConfig(config.getConfigPropertySources("../ts_idp_nemid-config/env/common"), config.getConfigPropertySettings());
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
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@SuppressWarnings("static-method")
	@Test
	public void testDoPost() throws ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter(ConfigKeys.PARAM_SREF, SREF);
		MockHttpServletResponse response = new MockHttpServletResponse();
		Index index = new Index();
		index.init(null);

		try {
			index.doGet(request, response);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

}
