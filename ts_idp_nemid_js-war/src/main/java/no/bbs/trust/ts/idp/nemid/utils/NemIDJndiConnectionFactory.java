package no.bbs.trust.ts.idp.nemid.utils;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import no.bbs.tt.trustsign.trustsignDAL.config.IDBConnectionFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.lookup.DataSourceLookupFailureException;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;

public class NemIDJndiConnectionFactory implements IDBConnectionFactory
{
	private static JdbcTemplate dbtemplate = null;
	private String dataSourceName;

	public NemIDJndiConnectionFactory(String name) throws InstantiationException
	{
		this.dataSourceName = name;
		try
		{
			JndiDataSourceLookup jndiDataSourceLookup = new JndiDataSourceLookup();
			DataSource dataSource = jndiDataSourceLookup.getDataSource(dataSourceName);
			dbtemplate = new JdbcTemplate(dataSource);
		}
		catch (DataSourceLookupFailureException e)
		{
			throw new InstantiationException("Failed to Lookup ConnectionPool [" + dataSourceName + "]. Reason: " + e.getMessage());
		}
	}

	public void close(Connection conn)
	{
		DataSourceUtils.releaseConnection(conn, dbtemplate.getDataSource());
	}

	@Override
	public Connection getConnection() throws SQLException
	{
		return DataSourceUtils.getConnection(dbtemplate.getDataSource());
	}

	@Override
	public String getDetails()
	{
		return dataSourceName;
	}

	@Override
	public String getName()
	{
		return "NemIDJndiConnectionFactory";
	}
}
