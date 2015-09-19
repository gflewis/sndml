package servicenow.common.datamart;

import java.util.*;
import java.io.*;
import java.sql.*;

import org.slf4j.Logger;

import servicenow.common.datamart.DatabaseTableWriter;
import servicenow.common.datamart.DatabaseWriter;
import servicenow.common.datamart.DatamartConfiguration;
import servicenow.common.datamart.LoggerFactory;
import servicenow.common.datamart.ResourceException;
import servicenow.common.datamart.ResourceManager;
import servicenow.common.datamart.SqlGenerator;
import servicenow.common.datamart.SuiteInitException;
import servicenow.common.datamart.TargetTableWriter;
import servicenow.common.datamart.TargetWriter;
import servicenow.common.soap.Table;

/**
 * Contains the JDBC connection for an SQL database. 
 *
 * Initialization statements from sqltemplates.xml are 
 * executed immediately upon opening the connection.
 */
public class DatabaseWriter extends TargetWriter {

	private final DatamartConfiguration config;
	private SqlGenerator generator;
	private Connection dbc;
	DatabaseMetaData meta;
	private String schema;
	private boolean autoCreate;
	private boolean batchInserts;

	static final Logger logger = LoggerFactory.getLogger(DatabaseWriter.class);
		
	DatabaseWriter(DatamartConfiguration config) throws ResourceException {
		assert config != null;
		this.config = config;
		open();
	}
	
	@Override
	void open() throws ResourceException {
		if (dbc != null) return;
		try {
			dbc = ResourceManager.getNewConnection(config);
			meta = dbc.getMetaData();
			assert dbc != null;
			autoCreate = config.getBoolean("autocreate",  true);
			batchInserts = config.getBoolean("batch_inserts",  false);
			logger.info("autocreate=" + autoCreate + " batch_inserts=" + batchInserts);
			generator = new SqlGenerator(config, dbc);
			initialize();
		}
		catch (SQLException e) {
			throw new ResourceException(e);
		}
	}
	
	protected void finalize() throws Throwable {
		close();
		super.finalize();		
	}
	
	@Override
	void close() {
		if (dbc == null) return;
		logger.info("close database connection");
		try {
			dbc.close();
		} catch (SQLException e) {
			logger.warn("failed to close database connection", e);
		}
		dbc = null;
	}
	
	
	/**
	 * Initialize the database connection.
	 * Set the timezone to GMT.
	 * Set the date format to YYYY-MM-DD
	 */
	private void initialize() throws SQLException {
		String dbProductName = meta.getDatabaseProductName();
		String dbProductVersion = meta.getDatabaseProductVersion();
		String username = config.getRequiredString("username");
		schema = config.getString("schema");
		if (schema == null) 
			schema = "";
		if (schema.length() == 0 && dbProductName.equalsIgnoreCase("Oracle")) 
			schema = username;
		schema = sqlCase(schema);
		assert schema != null;
		logger.debug("schema=" + schema);
		logger.info("JDBC driver" + 
		    " name=\"" + meta.getDriverName() + "\"" + 
			" version=" + meta.getDriverVersion());
		logger.info("JDBC database product" + 
		    " name=\"" + dbProductName + "\"" + 
			" version=" + dbProductVersion);
		
		dbc.setAutoCommit(false);
		Statement stmt = dbc.createStatement();
		Iterator<String> iter = generator.getInitializations().listIterator();
		while (iter.hasNext()) {
			String sql = iter.next();
			logger.info(sql);
			stmt.execute(sql);
		}
		stmt.close();
		dbc.commit();
		if (batchInserts) {
			if (!meta.supportsBatchUpdates()) {
				logger.warn("batch inserts not supported");
				batchInserts = false;
			}
		}
		logger.debug("batch_inserts=" + batchInserts);
	}
	
	Connection getConnection() {
		if (dbc == null) throw new IllegalStateException();
		return dbc;
	}
	
	TargetTableWriter getTableWriter(
			Table table, 
			String sqlTableName, 
			boolean displayValues) 
			throws SuiteInitException {
		if (sqlTableName == null || sqlTableName.length() == 0) 
			sqlTableName = table.getName();
		sqlTableName = generator.sqlCase(sqlTableName);
		DatabaseTableWriter writer;
		try {
			writer = new DatabaseTableWriter(
				this, table, sqlTableName, displayValues);
		} catch (SQLException e) {
			throw new SuiteInitException(e);
		} catch (IOException e) {
			throw new SuiteInitException(e);
		}
		return writer;
	}
	
	void commit() throws SQLException {
		logger.debug("commit");
		dbc.commit();
	}
	
	@Override
	void rollback() {
		try {
			dbc.rollback();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	SqlGenerator sqlGenerator() { return this.generator; }
	String getSchema() { return this.schema; }
	boolean autoCreate() { return this.autoCreate; }
	boolean batchInserts() { return this.batchInserts; }

	String sqlCase(String name) {
		String result = name;
		try {
			if (meta.storesLowerCaseIdentifiers()) 
				result = name.toLowerCase();
			if (meta.storesUpperCaseIdentifiers()) 
				result = name.toUpperCase();
		}
		catch (SQLException e) {
			logger.error("sqlCase", e);
		}		
		logger.trace("sqlCase " + name + "=" + result);
		return result;
	}
	
	void executeStatement(String sqlCommand) throws SQLException {
		if (dbc == null) throw new IllegalStateException();
		Statement stmt = dbc.createStatement();
		logger.info(sqlCommand);
		stmt.execute(sqlCommand);
		stmt.close();
	}

	void executeSQL(String command) throws SQLException {
		executeStatement(command);
		commit();		
	}
	
	/**
	 * Determine if a table already exists in the target database.
	 * 
	 * @return true if table exists; otherwise false
	 * @throws SQLException
	 */
	boolean tableExists(String sqlTableName) 
			throws SQLException {
		DatabaseMetaData meta = getConnection().getMetaData();
		ResultSet rs = meta.getTables(null, getSchema(), sqlCase(sqlTableName), null);
		boolean result = (rs.next() ? true : false);
		rs.close();
		logger.debug(sqlTableName + " " + (result ? "Exists" : "Does Not Exist"));
		return result;
	}
	
	/**
	 * Create a table in the target database if it does not already exist.
	 */
	@Override
	void createMissingTargetTable(Table table, String sqlTableName,	boolean displayValues) 
			throws SQLException, SuiteInitException {
		if (!autoCreate) return;
		if (tableExists(sqlTableName)) return;
		Statement stmt = dbc.createStatement();
		String createSql = generator.getCreateTable(
			table, sqlTableName, displayValues);
		logger.info(createSql);
		try {
			stmt.execute(createSql);
		} catch (SQLException e) {
			logger.error(createSql, e);
			throw e;
		}
		String grantSql = sqlGenerator().getTemplate("grant", sqlTableName);
		if (grantSql.length() > 0) {
			logger.info(grantSql);
			try { 
				stmt.execute(grantSql);		
			} catch (SQLException e) {
				logger.error(grantSql, e);
				throw e;
			}
		}
		stmt.close();
		commit();
	}
}
