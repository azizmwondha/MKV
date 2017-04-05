package no.bbs.trust.ts.idp.nemid.db;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import no.bbs.tt.trustsign.trustsignDAL.config.IDBConnectionFactory;
import oracle.jdbc.pool.OracleDataSource;

/**
 * @author Lars Anders Fjeldstad
 * Created: 20. sep. 2007
 */
public class OracleConnectionFactory implements IDBConnectionFactory {

	private static OracleDataSource ds = null;
	private static final String name = "OracleConnectionFactory";
	private static final String DRIVER_TYPE = "thin";
	private static final String SERVER_NAME = "172.21.71.124";
	private static final int PORT_NUMBER = 1521;
	private static final String DATABASE_NAME = "trust";
	private static final String USER = "trustsign";
	private static final String PASSWORD = "BankID1!";
	private static final String DETAILS = "jdbc:oracle:" + DRIVER_TYPE + ":@" + SERVER_NAME + ":" + PORT_NUMBER + ":" + DATABASE_NAME + ", " + USER + ", "
			+ PASSWORD;

	public OracleConnectionFactory() throws SQLException {
		ds = new OracleDataSource();
		ds.setDriverType(DRIVER_TYPE);
		ds.setServerName(SERVER_NAME);
		ds.setPortNumber(PORT_NUMBER);
		ds.setServiceName(DATABASE_NAME);
		ds.setUser(USER);
		ds.setPassword(PASSWORD);
	}

	/**
	 * @see no.bbs.tt.trustsign.trustsignDAL.config.IDBConnectionFactory#getConnection()
	 */
	public Connection getConnection() throws SQLException {
		return ds.getConnection();
	}

	/**
	 * @see no.bbs.tt.trustsign.trustsignDAL.config.IDBConnectionFactory#getDetails()
	 */
	public String getDetails() {
		return DETAILS;
	}

	/**
	 * @see no.bbs.tt.trustsign.trustsignDAL.config.IDBConnectionFactory#getName()
	 */
	public String getName() {
		return name;
	}

	protected static DataSource getDataSource() {
		return ds;
	}

	@Override
	public void close(Connection connection) {
		try {
			connection.close();
		} catch (SQLException e) { /* Ignore */ }
	}

}
