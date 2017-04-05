package no.bbs.trust.ts.idp.nemid.error;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import no.bbs.trust.common.basics.events.ActionEvent;
import no.bbs.trust.common.config.ConfigStarter;
import no.bbs.trust.common.webapp.servlets.InitConfig;
import no.bbs.trust.ts.idp.nemid.event.NemIDActionEvent;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openoces.ooapi.validation.ErrorCodeChecker;

public class NemIDErrorMapperTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		InitConfig config = new InitConfig();
		ConfigStarter.initConfig(config.getConfigPropertySources("env/common"), config.getConfigPropertySettings());
	}

	@SuppressWarnings("static-method")
	@Test
	public void testNemIDErrorMapper1() {
		final String ERROR_CODE = ErrorCodeChecker.ErrorCodes.CAN002.name();
		ActionEvent actionEvent = NemIDErrorMapper.getActionEvent(ERROR_CODE);
		String errorDescription = NemIDErrorMapper.getErrorCodeDescription(ERROR_CODE);

		assertNotNull(actionEvent);
		assertEquals(NemIDActionEvent.STATUS_USER_CANCEL, actionEvent);
		assertNotNull(errorDescription);
		assertEquals(NemIDErrorMapper.USER_CANCEL_DESCRIPTION + " [ErrorCode=" + ERROR_CODE + "]", errorDescription);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testNemIDErrorMapper2() {
		final String ERROR_CODE = ErrorCodeChecker.ErrorCodes.APP001.name();
		ActionEvent actionEvent = NemIDErrorMapper.getActionEvent(ERROR_CODE);
		String errorDescription = NemIDErrorMapper.getErrorCodeDescription(ERROR_CODE);

		assertNotNull(actionEvent);
		assertEquals(NemIDActionEvent.STATUS_BAD_PROTOCOL, actionEvent);
		assertNotNull(errorDescription);
		assertEquals(NemIDErrorMapper.BAD_PROTOCOL_DESCRIPTION + " [ErrorCode=" + ERROR_CODE + "]", errorDescription);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testNemIDErrorMapper3() {
		final String ERROR_CODE = ErrorCodeChecker.ErrorCodes.LOCK003.name();
		ActionEvent actionEvent = NemIDErrorMapper.getActionEvent(ERROR_CODE);
		String errorDescription = NemIDErrorMapper.getErrorCodeDescription(ERROR_CODE);

		assertNotNull(actionEvent);
		assertEquals(NemIDActionEvent.STATUS_UID_REVOKED, actionEvent);
		assertNotNull(errorDescription);
		assertEquals(NemIDErrorMapper.UID_REVOKED_DESCRIPTION + " [ErrorCode=" + ERROR_CODE + "]", errorDescription);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testNemIDErrorMapper4() {
		final String ERROR_CODE = ErrorCodeChecker.ErrorCodes.OCES006.name();
		ActionEvent actionEvent = NemIDErrorMapper.getActionEvent(ERROR_CODE);
		String errorDescription = NemIDErrorMapper.getErrorCodeDescription(ERROR_CODE);

		assertNotNull(actionEvent);
		assertEquals(NemIDActionEvent.STATUS_UID_INVALID, actionEvent);
		assertNotNull(errorDescription);
		assertEquals(NemIDErrorMapper.UID_INVALID_DESCRIPTION + " [ErrorCode=" + ERROR_CODE + "]", errorDescription);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testNemIDErrorMapper5() {
		final String ERROR_CODE = ErrorCodeChecker.ErrorCodes.AUTH012.name();
		ActionEvent actionEvent = NemIDErrorMapper.getActionEvent(ERROR_CODE);
		String errorDescription = NemIDErrorMapper.getErrorCodeDescription(ERROR_CODE);

		assertNotNull(actionEvent);
		assertEquals(NemIDActionEvent.STATUS_AUTH_FAILED, actionEvent);
		assertNotNull(errorDescription);
		assertEquals(NemIDErrorMapper.AUTH_FAILED_DESCRIPTION+ " [ErrorCode=" + ERROR_CODE + "]", errorDescription);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testNemIDErrorMapper6() {
		final String ERROR_CODE = ErrorCodeChecker.ErrorCodes.APP002.name();
		ActionEvent actionEvent = NemIDErrorMapper.getActionEvent(ERROR_CODE);
		String errorDescription = NemIDErrorMapper.getErrorCodeDescription(ERROR_CODE);

		assertNotNull(actionEvent);
		assertEquals(NemIDActionEvent.STATUS_DOC_INVALID, actionEvent);
		assertNotNull(errorDescription);
		assertEquals(NemIDErrorMapper.DOC_INVALID_DESCRIPTION+ " [ErrorCode=" + ERROR_CODE + "]", errorDescription);
	}

}
