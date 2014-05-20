package org.openoces.ooapi.web;

import java.security.Security;
import java.sql.SQLException;

import no.bbs.trust.common.basics.exceptions.StatusCodeException;
import no.bbs.trust.common.config.ConfigStarter;
import no.bbs.trust.common.webapp.servlets.InitConfig;
import no.bbs.trust.ts.idp.nemid.contants.ConfigKeys;
import no.bbs.trust.ts.idp.nemid.db.OracleConnectionFactory;
import no.bbs.trust.ts.idp.nemid.event.NemIDActionEvent;
import no.bbs.trust.ts.idp.nemid.tag.OcesJSONParameterGenerator;
import no.bbs.trust.ts.idp.nemid.tag.Signer;
import no.bbs.trust.ts.idp.nemid.utils.DAOUtil;
import no.bbs.tt.trustsign.trustsignDAL.config.ConnectionFactories;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openoces.ooapi.utils.Base64Handler;

import eu.nets.no.vas.esign.sdosigner.types.KeyCredentials;

public class JsonParameterGeneratorTest {

	private static final String SREF = "A92ED314B462D52159965681E5FD4F3AA3AF7D28";
	private static final String SAML_REQUEST = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
			+ "<sp:AuthnRequest xmlns:sp=\"urn:oasis:names:tc:SAML:2.0:protocol\" ID=\"Request-12345\" IssueInstant=\"2014-05-08T14:10:28.282Z\" Version=\"2.0\">"
			+ "<s:Issuer xmlns:s=\"urn:oasis:names:tc:SAML:2.0:assertion\">49</s:Issuer><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">"
			+ "<ds:SignedInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">"
			+ "<ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></ds:CanonicalizationMethod>"
			+ "<ds:SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\"></ds:SignatureMethod>"
			+ "<ds:Reference URI=\"#Request-12345\">"
			+ "<ds:Transforms>"
			+ "<ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"></ds:Transform><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"><ec:InclusiveNamespaces xmlns:ec=\"http://www.w3.org/2001/10/xml-exc-c14n#\" PrefixList=\"ds s sp\"></ec:InclusiveNamespaces></ds:Transform>"
			+ "</ds:Transforms>"
			+ "<ds:DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"></ds:DigestMethod><ds:DigestValue>XYYudVHWRTdGMf4LJCBaauQwHl7VxfUXqTgup7JIMDg=</ds:DigestValue>"
			+ "</ds:Reference>"
			+ "</ds:SignedInfo><ds:SignatureValue>"
			+ "DU9aJ46Y4XBTjQ6220yNeEKgwILI8OFfh2pBkZNll+PCQhQHYRK5qkBFfeDHYdxVA8axI7FOs6t5X7RWBLWRNXeSJOUbSr2m8Dq9ovZSAjswBgY4VTD9uNB5/a70ddMySqe0JPZHHA0xQxM4sVUYEUkLrDsSWaq11Alco2JpZR5BL+jTQ63wAt1fIKXu16gBMW8TlvVjge/MD1Yh/OT1GOGZ5ye9pEzC3Ja3UxR/dHvzrjGN4v3K0hfauxQkKgSLc6gUDZxTggw1JWWwQZR45JXMJUe/55KUEy5QKbKc9HxA6CK7HfVv+hQFDV9PKy7WjlPaiBcgs+s89XcDWQdEGQ==</ds:SignatureValue>"
			+ "</ds:Signature></sp:AuthnRequest>";

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
	public void testGenerateJsonParameters() throws StatusCodeException {
		String mid = DAOUtil.getSessionDataByKey(SREF, ConfigKeys.SESSIONKEY_MID);
		String jsonParameters = generateJsonParameters(mid);
		System.out.println(jsonParameters);
	}

	private static String generateJsonParameters(String mid) throws StatusCodeException {
		KeyCredentials credentials = null;
		try {
			credentials = DAOUtil.getMerchantCredentials(mid);
		} catch (SQLException ex) {
			throw new StatusCodeException(NemIDActionEvent.STATUS_DAL_SQL_ERROR, "Unable to obtain key credentials for Merchant [" + mid + "] "
					+ ex.getMessage());
		}

		Signer signer2 = new Signer(credentials.getKeystorepath(), credentials.getKeystorepass(), credentials.getKeyalias(), credentials.getKeyaliaspass());
		OcesJSONParameterGenerator generator = new OcesJSONParameterGenerator(signer2);

		// Mandatory parameters
		generator.addParameter("ORIGIN", "https://appletk.danid.dk");
		generator.addParameter("SAML_REQUEST", Base64Handler.encode(SAML_REQUEST));
		generator.addParameter("CLIENTFLOW", "ocessign2");
		generator.addParameter("TIMESTAMP", Base64Handler.encode(String.valueOf(System.currentTimeMillis())));

		// Optional parameters
		generator.addParameter("CLIENTMODE", "standard");
		generator.addParameter("LANGUAGE", "en");

		String jsonParameters = generator.getParametersAsJSON();
		return jsonParameters;
	}

}
