package no.bbs.trust.ts.idp.nemid.startup;

import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;

import no.bbs.trust.common.basics.constants.Constants;
import no.bbs.trust.common.basics.utils.EventLogger;
import no.bbs.trust.common.webapp.startup.InitState;
import no.bbs.trust.ts.idp.nemid.event.NemIDActionEvent;
import no.bbs.trust.ts.idp.nemid.utils.NemIDJndiConnectionFactory;
import no.bbs.tt.trustsign.trustsignDAL.config.ConnectionFactories;
import no.bbs.tt.trustsign.trustsignDAL.dao.table.MerchantDAO;

import org.apache.log4j.Logger;

public class InitDB extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private String TRUSTSIGN_DB_JNDI = "java:comp/env/jdbc/TrustSignDB";
	private static final Logger logger = Logger.getLogger(Constants.MAIN_LOGGER);
	private transient MerchantDAO merchantDAO = new MerchantDAO();

	@Override
	public void init() throws ServletException
	{
		logger.info("Loading JNDI Configuration Started [" + TRUSTSIGN_DB_JNDI + "]");

		try
		{
			System.setProperty("jndi.config.dir", TRUSTSIGN_DB_JNDI);
			logger.info("Register NemIDConnection factory");
			ConnectionFactories.getInstance().registerDBConnectionFactory(new NemIDJndiConnectionFactory(TRUSTSIGN_DB_JNDI));
			logger.info("NemIDConnection factory registration OK");
		}
		catch (Throwable t)
		{
			EventLogger.dumpStack(t);
			EventLogger.appendEvent(NemIDActionEvent.STATUS_NEMID_SERVICE_LOAD_FAILED);
			logger.info("Unable to init JNDI: " + t.getMessage());
			InitState.initFailed(this, "Unable to init JNDI[" + TRUSTSIGN_DB_JNDI + "]: " + t.getMessage());
			throw new UnavailableException("Unable to init JNDI[" + TRUSTSIGN_DB_JNDI + "] " + t.getMessage());
		}
		logger.info("Load JNDI Configuration complete");

		try
		{
			executeTestQuery();
		}
		catch (Throwable t)
		{
			EventLogger.dumpStack(t);
			EventLogger.appendEvent(NemIDActionEvent.STATUS_NEMID_SERVICE_LOAD_FAILED);
			logger.info("DB connection test failed: " + t.getMessage());
			InitState.initFailed(this, "DB connection test failed: " + t.getMessage());
			throw new UnavailableException("DB connection test failed " + t.getMessage());
		}
		finally
		{
		}
	}

	private void executeTestQuery() throws SQLException
	{
		logger.info("Run test query agains DB [MerchantDAO.getAll()]");
		ArrayList merchants = merchantDAO.getAll(null);
		logger.info("Record count for test query: " + merchants.size());
	}
}
