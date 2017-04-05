package no.bbs.trust.ts.idp.nemid.utils;

import java.util.Map;

import no.bbs.trust.common.basics.exceptions.StatusCodeException;
import no.bbs.trust.common.config.ConfigStarter;
import no.bbs.trust.common.webapp.servlets.InitConfig;
import no.bbs.trust.ts.idp.nemid.contants.ConfigKeys;
import no.bbs.trust.ts.idp.nemid.db.OracleConnectionFactory;
import no.bbs.tt.trustsign.trustsignDAL.config.ConnectionFactories;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DaoUtilTest {

	private static final String SREF = "5AB9A24E14737AB9F614BC2F96CC02B2B62729F5";
	private static final String[] SESSION_DATA_KEYS = new String[] { ConfigKeys.SESSIONKEY_SPID, ConfigKeys.SESSIONKEY_MID, ConfigKeys.SESSIONKEY_LOCALE,
			ConfigKeys.SESSIONKEY_TZO, ConfigKeys.SESSIONKEY_NEMID_CLIENTMODE };
	private static final String[] SESSION_DATA_VALUES = new String[] { "446693", "1001", "no", "0", "" };

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		InitConfig config = new InitConfig();
		ConfigStarter.initConfig(config.getConfigPropertySources("env/common"), config.getConfigPropertySettings());
		ConnectionFactories.getInstance().registerDBConnectionFactory(new OracleConnectionFactory());
	}

	@SuppressWarnings("static-method")
	@Test
	public void testGetSessionDataByKey() throws StatusCodeException {
		String spid = DAOUtil.getSessionDataByKey(SREF, SESSION_DATA_KEYS[0]);
		String mid = DAOUtil.getSessionDataByKey(SREF, SESSION_DATA_KEYS[1]);
		String locale = DAOUtil.getSessionDataByKey(SREF, SESSION_DATA_KEYS[2]);
		String tzos = DAOUtil.getSessionDataByKey(SREF, SESSION_DATA_KEYS[3]);

		assertNotNull(spid);
		assertNotNull(mid);
		assertNotNull(locale);
		assertNotNull(tzos);
		assertEquals(SESSION_DATA_VALUES[0], spid);
		assertEquals(SESSION_DATA_VALUES[1], mid);
		assertEquals(SESSION_DATA_VALUES[2], locale);
		assertEquals(SESSION_DATA_VALUES[3], tzos);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testGetSessionDataKeysAndValues() throws StatusCodeException {
		Map<String, String> sessionDataKeysAndValues = DAOUtil.getSessionDataKeysAndValues(SREF, SESSION_DATA_KEYS);

		assertNotNull(sessionDataKeysAndValues);
		assertEquals(SESSION_DATA_VALUES[0], sessionDataKeysAndValues.get(SESSION_DATA_KEYS[0]));
		assertEquals(SESSION_DATA_VALUES[1], sessionDataKeysAndValues.get(SESSION_DATA_KEYS[1]));
		assertEquals(SESSION_DATA_VALUES[2], sessionDataKeysAndValues.get(SESSION_DATA_KEYS[2]));
		assertEquals(SESSION_DATA_VALUES[3], sessionDataKeysAndValues.get(SESSION_DATA_KEYS[3]));
	}

}
