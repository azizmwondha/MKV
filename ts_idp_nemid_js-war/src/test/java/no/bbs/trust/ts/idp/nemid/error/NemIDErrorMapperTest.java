package no.bbs.trust.ts.idp.nemid.error;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import no.bbs.trust.common.config.ConfigStarter;
import no.bbs.trust.common.webapp.servlets.InitConfig;
import no.bbs.trust.ts.idp.nemid.error.ErrorCodes;
import no.bbs.trust.ts.idp.nemid.error.NemIDErrorMapper;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openoces.ooapi.validation.ErrorCodeChecker;

public class NemIDErrorMapperTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		InitConfig config = new InitConfig();
		ConfigStarter.initConfig(config.getConfigPropertySources("../ts_idp_nemid_js-config/env/common"), config.getConfigPropertySettings());
	}

	@SuppressWarnings("static-method")
	@Test
	public void testNemIDErrorMapper1() {
		final String nemiIdCode = ErrorCodeChecker.ErrorCodes.CAN002.name();
		ErrorCodes errorCode = NemIDErrorMapper.getErrorCodeFromNemIDCode(nemiIdCode);

		assertNotNull(errorCode);
		assertEquals(ErrorCodes.USERCANCEL, errorCode);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testNemIDErrorMapper2() {
		final String nemiIdCode = ErrorCodeChecker.ErrorCodes.APP001.name();
		ErrorCodes errorCode = NemIDErrorMapper.getErrorCodeFromNemIDCode(nemiIdCode);

		assertNotNull(errorCode);
		assertEquals(ErrorCodes.BADPROTOCOL, errorCode);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testNemIDErrorMapper3() {
		final String nemiIdCode = ErrorCodeChecker.ErrorCodes.LOCK003.name();
		ErrorCodes errorCode = NemIDErrorMapper.getErrorCodeFromNemIDCode(nemiIdCode);

		assertNotNull(errorCode);
		assertEquals(ErrorCodes.UIDREVOKED, errorCode);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testNemIDErrorMapper4() {
		final String nemiIdCode = ErrorCodeChecker.ErrorCodes.OCES003.name();
		ErrorCodes errorCode = NemIDErrorMapper.getErrorCodeFromNemIDCode(nemiIdCode);

		assertNotNull(errorCode);
		assertEquals(ErrorCodes.UIDINVALID, errorCode);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testNemIDErrorMapper5() {
		final String nemiIdCode = ErrorCodeChecker.ErrorCodes.AUTH012.name();
		ErrorCodes errorCode = NemIDErrorMapper.getErrorCodeFromNemIDCode(nemiIdCode);

		assertNotNull(errorCode);
		assertEquals(ErrorCodes.AUTHFAILED, errorCode);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testNemIDErrorMapper6() {
		final String nemiIdCode = ErrorCodeChecker.ErrorCodes.APP002.name();
		ErrorCodes errorCode = NemIDErrorMapper.getErrorCodeFromNemIDCode(nemiIdCode);

		assertNotNull(errorCode);
		assertEquals(ErrorCodes.DOCINVALID, errorCode);
	}

}
