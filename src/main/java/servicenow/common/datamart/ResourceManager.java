package servicenow.common.datamart;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.jdom2.JDOMException;
import org.slf4j.Logger;

import servicenow.common.datamart.DatamartConfiguration;
import servicenow.common.datamart.LoggerFactory;
import servicenow.common.datamart.ResourceException;
import servicenow.common.datamart.ResourceManager;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;
import servicenow.common.soap.TableSchema;

/**
 * Each thread has a private connection to ServiceNow (Session) and to the
 * JDBC Database (Connection). This class manages those connection objects.
 *
 */
public class ResourceManager {

	static Thread mainThread = Thread.currentThread();	
	static final Logger logger = LoggerFactory.getLogger(ResourceManager.class);

	static private DatamartConfiguration globalConfig;
	static private Session globalSession;
	static private Session schemaSession;
	static private DatabaseWriter globalWriter;
			
	private static DatamartConfiguration getConfiguration() {
		if (globalConfig == null) {
			globalConfig = DatamartConfiguration.getDatamartConfiguration();
		}
		return globalConfig;
	}
	
	/**
	 * Return ServiceNow session associated with the main thread
	 */
	static Session getMainSession() {
		if (globalSession == null) {
			assert Thread.currentThread().equals(mainThread);
			globalSession = getNewSession();
		}
		return globalSession;
	}

	/**
	 * Establish a new ServiceNow session
	 */
	static Session getNewSession() throws ResourceException {
		logger.debug("getNewSession");
		try {
			return new Session(DatamartConfiguration.getSessionConfiguration());
		} catch (IOException e) {
			throw new ResourceException(e);
		} catch (JDOMException e) {
			throw new ResourceException(e);
		}
	}
	
	/**
	 * Return ServiceNow 
	 * @throws ResourceException
	 */
	static Session getSchemaSession() throws ResourceException {
		if (schemaSession == null) {
			DatamartConfiguration config = getConfiguration();
			String url = config.getString("schema.servicenow.url");
			String username = config.getString("schema.servicenow.username");
			String password = config.getString("schema.servicenow.password");
			if (url == null || url.length() == 0) {
				schemaSession = getMainSession();			
			}
			else {
				logger.info("getSchemaSession url=" + url);
				try {
					schemaSession = new Session(url, username, password, null,
						DatamartConfiguration.getSessionConfiguration());
				} catch (Exception e) {
					throw new ResourceException(e);
				}
			}
		}
		return schemaSession;		
	}
	
	static TableSchema getTableSchema(String tablename) throws ResourceException {
		Table schemaTable;
		try {
			schemaTable = getSchemaSession().table(tablename);
		} catch (Exception e) {
			throw new ResourceException(e);
		}
		TableSchema tableSchema = schemaTable.getSchema();
		return tableSchema;
	}

	@Deprecated
	public static DatabaseWriter getMainTargetWriter() {
		if (globalWriter == null) {
			assert Thread.currentThread().equals(mainThread);
			DatamartConfiguration config = getConfiguration();
			globalWriter = new DatabaseWriter(config);
		}
		return globalWriter;	
	}

	@Deprecated
	public static DatabaseWriter getNewTargetWriter() {
		DatamartConfiguration config = getConfiguration();
		return new DatabaseWriter(config);
	}
	

	static Connection getNewConnection() {
		return getNewConnection(getConfiguration());
	}
	
	static Connection getNewConnection(DatamartConfiguration config) {
		try {
			String dburl  = config.getRequiredString("url");
			String dbuser = config.getRequiredString("username");
			String schema = config.getString("schema");
			String dbpass = config.getString("password", "");
			logger.info("database=" + dburl + 
				" user=" + dbuser + " schema=" + schema);
			return DriverManager.getConnection(dburl, dbuser, dbpass);
		}
		catch (SQLException e) {
			throw new ResourceException(e);
		}
	}
	
}
