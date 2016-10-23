package servicenow.common.datamart;

import static org.junit.Assert.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;

import servicenow.common.datamart.DatabaseWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DB {

	static private boolean initialized = false;
	static private Connection connection;
	static private Logger logger = AllTests.getLogger(DB.class);

	@Before
	public void setUp() throws Exception {
		AllTests.initialize();
	}
	
	@After
	public void tearDown() throws Exception {
		
	}
	
	static void initialize() {
		if (initialized) return;
		try {
			connection = AllTests.getConnection();
		} catch (Exception e) {
			e.printStackTrace();
			throw new AssertionError();
		}
		assertNotNull(connection);
		// log.info("set AutoCommit=false");
		// connection.setAutoCommit(false);
		initialized = true;
	}

	static java.sql.Connection getConnection() throws IOException, SQLException {
		initialize();
		return connection;
	}
	
	static void rollback() throws SQLException {
		if (!initialized) return;
		connection.rollback();		
	}
	
	static void commit() throws SQLException {
		if (!initialized) return;
		connection.commit();
	}
	
	static void disconnect() throws SQLException {
		connection.close();
		connection = null;
		initialized = false;
	}
	
	@Test
	public void testInitialize() throws Exception {
		initialize();
		assertNotNull(connection);
	}

	@Test
	public void testCreateTable() throws Exception {
		// Properties props = AllTests.getProperties();
		DatabaseWriter dbwriter = AllTests.getDBWriter();
		assertFalse(dbwriter.tableExists("blahblahblah"));
		String schema = dbwriter.getSchema();
		logger.debug("schema=" + schema);
		assertNotNull(schema);
		assertTrue(schema.length() > 0);
		String shortname = "abc";
		String fullname = schema + ".ABC";
		String createtable = "create table " + fullname + "(foo varchar(20))";
		String droptable = "drop table " + fullname;
		if (dbwriter.tableExists(shortname)) sqlUpdate(droptable);
		sqlUpdate(createtable);
		connection.commit();
		assertTrue(dbwriter.tableExists(shortname));
		sqlUpdate(droptable);
		connection.commit();
		assertFalse(dbwriter.tableExists(shortname));
	}
	
	static String tableName(String name) throws IOException {
		if (name.indexOf(".") > -1) return name;
		String schema = AllTests.getSchema();
		String prefix = schema.length() > 0 ? schema + "." : schema;
		return prefix + name;
	}
				
	static String replace(String sql) throws IOException {
		String schema = AllTests.getSchema();
		String prefix = schema.length() > 0 ? schema + "." : schema;
		return sql.replaceAll("\\$", prefix);
	}
	

	static void dropTable(String tablename) throws Exception {
		tablename = tableName(tablename);
		logger.debug("dropTable " + tablename);
		try {
			sqlUpdate("drop table " + tablename);
			connection.commit();
			logger.debug("table " + tablename + " has been dropped");
		}
		catch (SQLException e) {}	
	}
		
	static int sqlUpdate(String sql) throws SQLException, IOException {
		initialize();
		assertNotNull(connection);
		logger.debug("sqlUpdate \"" + sql + "\"");
		Statement stmt = connection.createStatement();
		int count = 0;
		try {
			count = stmt.executeUpdate(sql);
			connection.commit();
		} catch (SQLException e) {
			logger.error(sql, e);
			throw e;
		}
		logger.info(sql + " (" + count + ")");
		return count;
	}

	static int numRows(String tablename) throws SQLException, IOException {
		String sql = "select count(*) from " + tableName(tablename);
		return sqlCount(sql);
	}
	
	static int sqlCount(String query) throws SQLException, IOException {
		initialize();
		connection.commit();
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery(query);
		rs.next();
		int count = rs.getInt(1);
		rs.close();
		connection.commit();
		logger.info(query + " (" + count + ")");
		return count;
	}

	static void sqlDeleteJupiter() throws IOException, SQLException {
		String tablename = tableName("cmn_location");
		sqlUpdate("delete from " + tablename +" where name='Jupiter'");
		connection.commit();
	}
	
	static int sqlCountJupiter() throws IOException, SQLException {
		return sqlCountTable("cmn_location", "where name='Jupiter'");
	}
	
	static int sqlCountTable(String name, String qualifier) throws IOException, SQLException {
		String tablename = tableName(name);
		String sql = "select count(*) from " + tablename;
		if (qualifier != null) sql = sql + " " + qualifier;
		return sqlCount(sql);
	}
	
	static int sqlCountTable(String name) throws IOException, SQLException {
		return sqlCountTable(name, null);
	}
	
	static void printReport(String query) throws SQLException {
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery(query);
		int colCount = rs.getMetaData().getColumnCount();
		while (rs.next()) {
			StringBuilder line = new StringBuilder();
			for (int col = 1; col <= colCount; ++col) {
				String value = rs.getString(col);
				line.append(value);
				line.append(" ");
			}
			logger.info(line.toString());
		}
		
	}
}
