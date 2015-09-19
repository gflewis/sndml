package servicenow.common.datamart;

import servicenow.common.datamart.DatabaseWriter;
import servicenow.common.datamart.DatamartConfiguration;
import servicenow.common.datamart.LoadMethod;
import servicenow.common.datamart.LoggerFactory;
import servicenow.common.datamart.Metrics;
import servicenow.common.datamart.SqlFieldDefinition;
import servicenow.common.datamart.SqlGenerator;
import servicenow.common.datamart.SuiteExecException;
import servicenow.common.datamart.SuiteInitException;
import servicenow.common.datamart.TargetTableWriter;
import servicenow.common.soap.DateTime;
import servicenow.common.soap.InvalidDateTimeException;
import servicenow.common.soap.InvalidFieldNameException;
import servicenow.common.soap.Key;
import servicenow.common.soap.KeyList;
import servicenow.common.soap.Record;
import servicenow.common.soap.RecordList;
import servicenow.common.soap.Table;
import servicenow.common.soap.TableWSDL;

import java.util.*;
import java.util.regex.*;

import org.slf4j.Logger;

import java.io.IOException;
import java.sql.BatchUpdateException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;

import org.jdom2.JDOMException;

/**
 * Writes to a specific table in an SQL database.  
 * The SQL table corresponds to a table in a ServiceNow instance.
 * @author Giles Lewis
 *
 */
public class DatabaseTableWriter extends TargetTableWriter {
	
	private final DatabaseWriter database;
	private final java.sql.Connection dbc;
	private final java.sql.DatabaseMetaData meta;
	private final SqlGenerator generator;
	private final String sqlSchemaName;
	private final TableWSDL tableWSDL;
	final ArrayList<SqlFieldDefinition> columns;
	private boolean warnOnTruncate;
	
	private final java.sql.PreparedStatement stmtInsert;
	private final java.sql.PreparedStatement stmtUpdate;	
	private final java.sql.PreparedStatement stmtReadTimestamp;
	private final java.sql.PreparedStatement stmtDeleteRecord;
    private final String sqlInsert;
    private final String sqlUpdate;
    private final String sqlReadTimestamp;
    private final String sqlDeleteRecord;
    final Logger logger;

    private final Pattern dateTimePattern = 
    	Pattern.compile("\\d\\d\\d\\d-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d");
    private final Calendar gmtCalendar = 
    	Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    
	DatabaseTableWriter(
			DatabaseWriter database, 
			Table table, 
			String sqlTableName,
			boolean displayValues)
			throws SQLException, SuiteInitException, IOException {
		super(database, table, sqlTableName);
		assert database != null;
		assert table != null;
		assert sqlTableName != null && sqlTableName.length() > 0;
		this.database = database;
		this.dbc = database.getConnection();
		this.meta = dbc.getMetaData();
		this.generator = database.sqlGenerator();
		this.sqlSchemaName = database.getSchema();
		assert sqlSchemaName != null;
		if (sqlTableName == null || sqlTableName.length() == 0)
			throw new AssertionError("tableName is null");		
		this.sqlTableName = 
			meta.storesUpperCaseIdentifiers() ? sqlTableName.toUpperCase() :
			meta.storesLowerCaseIdentifiers() ? sqlTableName.toLowerCase() :
			sqlTableName;
		// this.displayValues = displayValues;
		this.logger = LoggerFactory.getLogger(this.getClass(), sqlTableName);
		// this.metrics = metrics;
		this.tableWSDL = table.getWSDL();
		assert generator != null;
		assert dbc != null;
		database.createMissingTargetTable(table, sqlTableName, displayValues);
		logger.debug("begin fetchMetaData");
		columns = fetchMetaData();
		if (columns == null)
			throw new AssertionError(
				"Unable to retrieve metadata for " + generator.sqlTableName(sqlTableName));
		sqlInsert = generateInsert();
		sqlUpdate = generateUpdate();
		sqlReadTimestamp = generator.getTemplate("timestamp", sqlTableName);
		sqlDeleteRecord = generator.getTemplate("delete", sqlTableName);
		logger.debug("Prepare " + sqlInsert);
		stmtInsert = dbc.prepareStatement(sqlInsert);
		logger.debug("Prepare " + sqlUpdate);
		stmtUpdate = dbc.prepareStatement(sqlUpdate);
		logger.debug("Prepare " + sqlDeleteRecord);
		stmtDeleteRecord = dbc.prepareStatement(sqlDeleteRecord);
		logger.debug("Prepare " + sqlReadTimestamp);
		stmtReadTimestamp = dbc.prepareStatement(sqlReadTimestamp);
		DatamartConfiguration config = 
			DatamartConfiguration.getDatamartConfiguration();
		warnOnTruncate = config.getBoolean("warn_on_truncate",  true);
	}

	void close() {
		try {
			stmtInsert.close();
			stmtUpdate.close();
			stmtReadTimestamp.close();
			stmtDeleteRecord.close();			
		}
		catch (SQLException e) {
			logger.warn("failed to close database statement", e);			
		}		
	}
	
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}
	
	// public Metrics getMetrics() { return this.metrics; }
	public SqlGenerator sqlGenerator() { return database.sqlGenerator(); }
		
	/**
	 * Read the schema for a SQL table from the database.
	 * 
	 * @return A list of all the columns in the table.  
	 * Returns null if the table does not exist in the database.
	 * @throws SQLException
	 * @throws LoaderException 
	 */
	synchronized ArrayList<SqlFieldDefinition> fetchMetaData() 
			throws SQLException, SuiteInitException {
		if (!tableExists()) return null;
		logger.debug("Begin Load Schema");		
		assert sqlTableName != null && sqlTableName.length() > 0;
		assert sqlSchemaName != null;
		ArrayList<SqlFieldDefinition> list = new ArrayList<SqlFieldDefinition>();
		ResultSet columns = meta.getColumns(null, sqlSchemaName, sqlTableName, null);
		while (columns.next()) {
			String name = columns.getString(4);
			int type = columns.getInt(5);
			int size = columns.getInt(7);
			String glidename = sqlGenerator().glideName(name);
			if (tableWSDL.canReadField(glidename)) {
				SqlFieldDefinition defn =
					new SqlFieldDefinition(name, type, size, glidename);
				list.add(defn);
				logger.debug(name + " type=" + type + " size=" + size);				
			}
			else {
				logger.warn(name + " type=" + type + " size=" + size + " (not mapped)");				
			}				
			/*
			FieldDefinition glideDef = tableSchema.getFieldDefinition(glidename);
			if (glideDef == null) {
				String lcname = name.toLowerCase();
				if (lcname.startsWith("dv_")) {
					String refName = lcname.substring(3);
					FieldDefinition refDef = tableSchema.getFieldDefinition(refName);
					if (refDef != null && refDef.isReference()) {
						glideDef = refDef.displayValueDefinition();
						type = java.sql.Types.VARCHAR;
						size = 255;
						this.displayValues = true;
					}
				}
			}
			logger.debug(name +
				" glidename=" + glidename +
				(glideDef == null ? " (not mapped)" : "") +
				" type=" + type + " size=" + size);
			if (glideDef != null) {
				SqlFieldDefinition defn = new SqlFieldDefinition(name, type, size, glideDef);
				list.add(defn);
			}
			*/
		}
		if (list.size() < 1)
			throw new SuiteInitException(
				"SQL table not found: " + sqlSchemaName + "." + sqlTableName);
		if (!list.get(0).getName().toUpperCase().equals("SYS_ID"))
			throw new RuntimeException(
				"expected SYS_ID, found " + list.get(0).getName() + 
				" in first column of table \"" + sqlTableName + "\"");
		logger.debug(list.size() + " columns");
		columns.close();
		return list;
	}

	void truncateTable() throws SuiteExecException {
		String sql = sqlGenerator().getTemplate("truncate", sqlTableName);
		logger.info(sql);
		try {
			Statement stmt = database.getConnection().createStatement();
			stmt.execute(sql);
			stmt.close();
			database.commit();
		}
		catch (SQLException e) {
			throw new SuiteExecException(e);
		}
	}

	/**
	 * Get a value from a Glide Record and bind it to a variable in prepared statement.
	 * 
	 * @param stmt Prepared Statement to which variables are bound.
	 * @param bindCol Index (starting with 1) of the variable within the statement.
	 * @param rec Glide Record which serves as a source of the data.
	 * @param glideCol Index (starting with 0) of the variable in the columns array.
	 * @throws SQLException
	 * @throws IOException
	 * @throws JDOMException
	 */
	private void bindField(PreparedStatement stmt, int bindCol, Record rec, int glideCol) 
			throws SQLException, IOException, JDOMException {
		SqlFieldDefinition d = columns.get(glideCol);
		String glidename = d.getGlideName();
		String value = rec.getField(glidename, false);
		int sqltype = d.sqltype;
		// If value is null then bind to null and exit
		if (value == null) {
			stmt.setNull(bindCol, sqltype);
			return;
		}		
		// If the value appears to be a date (dddd-dd-dd dd:dd:dd)
		// and the target data type is numeric
		// then it must be a duration
		// so try to convert it to a number of seconds
		if ((sqltype == Types.NUMERIC || sqltype == Types.DECIMAL || 
				sqltype == Types.INTEGER || sqltype == Types.DOUBLE) && 
				value.length() == 19) {
			if (dateTimePattern.matcher(value).matches()) {
				try {
					DateTime timestamp = new DateTime(value, DateTime.DATE_TIME);
					long seconds = timestamp.toDate().getTime() / 1000L;
					if (logger.isTraceEnabled())
						logger.trace(glidename + " " + value + "=" + seconds);
					if (seconds < 0L) {
						logger.warn(rec.getKey() + " duration underflow: " +
							glidename + "=" + value);
						value = null;
					}
					else if (seconds > 999999999L) {
						logger.warn(rec.getKey() + " duration overflow: " +
							glidename + "=" + value);
						value = null;
					}
					else {
						value = Long.toString(seconds);
					}
				} catch (InvalidDateTimeException e) {
					logger.warn(rec.getKey() + " duration error: " +
							glidename + "=" + value);
					value = null;
				}
				if (value == null) {
					stmt.setNull(bindCol, sqltype);
					return;					
				}
			}
		}
		assert value != null;
		// If the SQL type is VARCHAR, then check for an over-size value
		// and truncate if necessary
		if (sqltype == Types.VARCHAR || sqltype == Types.CHAR) {
			int oldSize = value.length();
			int maxSize = d.getSize();
			if (value.length() > maxSize) {
				value = value.substring(0,  maxSize);
			}
			if (generator.getDialect().equals("oracle2")) {
				// This is a workaround for an apparent bug in the Oracle JDBC 
				// driver which occasionally generates an ORA-01461 error when 
				// inserting from a text field containing multi-byte characters
				// into a VARCHAR2 column.
				// Keep chopping more characters off the end of the string until
				// the number of BYTES is less than the field size.
				while (value.getBytes("UTF8").length > maxSize)
					value = value.substring(0, value.length() - 1);					
			}
			if (value.length() != oldSize) {
				String message = rec.getKey() + " truncated: " + glidename +
					" from " + oldSize + " to " + value.length();
				if (warnOnTruncate)
					logger.warn(message);
				else
					logger.debug(message);
			}
		}
		if (logger.isTraceEnabled()) {
			int len = (value == null ? 0 : value.length());
			logger.trace("bind (" + bindCol + "/" + sqltype + ") " + 
			  glidename + "=" + value + " [" + len + "]");
		}
		assert value != null;
		switch (sqltype) {
		case Types.DATE :
			DateTime dt;
			try {
				dt = new DateTime(value);
				java.sql.Date sqldate = new java.sql.Date(dt.getMillisec());
				stmt.setDate(bindCol, sqldate, gmtCalendar);
			}
			catch (InvalidDateTimeException e) {
				logger.warn(rec.getKey() + " date error: " +
						glidename + "=" + value);
				stmt.setDate(bindCol,  null);			
			}
			break;
		case Types.TIMESTAMP :
			// If the SQL type is TIMESTAMP, then try to bind the field to a java.sql.Timesetamp.
			// Note that in Oracle the DATE fields have a java.sql type of TIMESTAMP.
			DateTime ts;
			try { 
				ts = new DateTime(value);
				java.sql.Timestamp sqlts = new java.sql.Timestamp(ts.getMillisec());
				stmt.setTimestamp(bindCol, sqlts, gmtCalendar);
			}
			catch (InvalidDateTimeException e) {
				logger.warn(rec.getKey() + " timestamp error: " +
						glidename + "=" + value);
				stmt.setTimestamp(bindCol, null);				
			}
			break;
		case Types.BOOLEAN :
		case Types.BIT :
			if (value.equals("1") || value.equalsIgnoreCase("true"))
				stmt.setBoolean(bindCol, true);
			else if (value.equals("0") || value.equalsIgnoreCase("false"))
				stmt.setBoolean(bindCol,  false);
			else {
				logger.warn(rec.getKey() + "boolean error: " +
						glidename + "=" + value);
				stmt.setNull(bindCol, sqltype);
			}
			break;
		case Types.TINYINT :
			stmt.setByte(bindCol, Byte.parseByte(value));
			break;
		case Types.SMALLINT :
			stmt.setShort(bindCol, Short.parseShort(value));
			break;
		case Types.INTEGER :
			// This is a workaround for the fact that ServiceNow includes decimal portions
			// in integer fields, which can cause JDBC to choke.
			int p = value.indexOf('.');
			if (p > -1) {
				String message = rec.getKey() + " decimal truncated: " +
						glidename + "=" + value;
				if (warnOnTruncate)
					logger.warn(message);
				else
					logger.debug(message);
				value = value.substring(0,  p);
			}
			if (value == "") value = "0";
			stmt.setInt(bindCol, Integer.parseInt(value));
			break;
		case Types.DOUBLE :
		case Types.FLOAT :
		case Types.NUMERIC :
		case Types.DECIMAL :
			stmt.setDouble(bindCol, Double.parseDouble(value));
			break;
		default :
			stmt.setString(bindCol, value);
		}
	}

	/**
	 * This procedure is called only if an exception is caught.
	 * It displays the contents of the record.
	 */
	private void logFields(Record rec, String method) {
		Iterator<SqlFieldDefinition> iter = columns.iterator();
		while (iter.hasNext()) {
			SqlFieldDefinition d = iter.next();
			String line = method + " " + d.name + 
				" type=" + d.getType() + " size=" + d.getSize() + " value=";
			try {
				line += rec.getField(d.getGlideName());
			} catch (InvalidFieldNameException e) {
				line += e.getClass().getSimpleName();
			}
			logger.debug(line);
		}
	}
	
	String generateInsert() throws SQLException {
		final String fieldSeparator = ",\n";
		StringBuilder fieldnames = new StringBuilder();
		StringBuilder fieldvalues = new StringBuilder();
		for (int i = 0; i < columns.size(); ++i) {
			if (i > 0) {
				fieldnames.append(fieldSeparator);
				fieldvalues.append(",");
			}
			fieldnames.append(sqlGenerator().sqlQuote(columns.get(i).getName()));
			fieldvalues.append("?");
		}
		HashMap<String,String> map = new HashMap<String,String>();
		map.put("fieldnames", fieldnames.toString());
		map.put("fieldvalues", fieldvalues.toString());
		return sqlGenerator().getTemplate("insert", sqlTableName, map);
	}
	
	String generateUpdate() throws SQLException {
		final String fieldSeparator = ",\n";
		StringBuilder fieldmap = new StringBuilder();
		// skip sys_id and start with 2nd column
		for (int i = 1; i < columns.size(); ++i) {
			if (i > 1) fieldmap.append(fieldSeparator);
			fieldmap.append(sqlGenerator().sqlQuote(columns.get(i).getName()));
			fieldmap.append("=?");			
		}
		HashMap<String,String> map = new HashMap<String,String>();
		map.put("fieldmap", fieldmap.toString());
		map.put("keyvalue", "?");
		return sqlGenerator().getTemplate("update", sqlTableName, map);
	}
	
	/**
	 * Insert a record into the SQL database using a Glide Record as the source.
	 * @param rec Data to be inserted.
	 */
	void insert(Record rec) 
			throws SQLException, IOException, JDOMException {
		String sys_id = rec.getKey().toString();
		logger.debug("INSERT " + sys_id);
		for (int i = 0; i < columns.size(); ++i) {
			bindField(stmtInsert, i + 1, rec, i);
		}
		try {
			stmtInsert.executeUpdate();
		} catch (SQLException e) {
			logFields(rec, "INSERT");
			logger.error(rec.getXML(true) + "\n" + sqlInsert, e);
			throw e;
		}
	}

	int insertBatch(RecordList recs) 
			throws SQLException, IOException, JDOMException {
		stmtInsert.clearBatch();
		for (Record rec : recs) {
			String sys_id = rec.getKey().toString();
			logger.debug("INSERT " + sys_id);
			for (int i = 0; i < columns.size(); ++i) {
				bindField(stmtInsert, i + 1, rec, i);
			}
			try {
				stmtInsert.addBatch();
			} catch (SQLException e) {
				logFields(rec, "INSERT");
				logger.error(rec.getXML(true) + "\n" + sqlInsert, e);
				throw e;
			}
		}
		int [] results;
		try {
			 results = stmtInsert.executeBatch();
		} catch (BatchUpdateException e) {
			results = e.getUpdateCounts();
			StringBuffer msg = new StringBuffer("BatchUpdateException\n");
			// Display the sys_id of all records (successful and unsuccessful)
			for (int i = 0; i < recs.size(); ++i) {
				msg.append(i + ": " + recs.get(i).getKey());
				if (i < results.length)
					msg.append(" result=" + results[i]);
				msg.append("\n");
			}
			// Display the XML of the record that failed
			// find the index of the record that failed
			int badi = -1;
			for (int b = 0; badi < 0 && b < results.length; ++b)
				if (results[b] != 1) badi = b;
			if (badi >= 0 && badi < recs.size()) {
				Record badrec = recs.get(badi);
				msg.append("Contents of record #" + badi + ":\n");
				msg.append(badrec.getXML(true) + "\n");
			}
			logger.error(msg.toString(), e);
			throw e;
		}
		return results.length;
	}
	
	
	/**
	 * Update a SQL table using a Record as input.
	 * Return true SQL record exists and is updated. 
	 * Return false if the SQL record does not exist.
	 * @param rec Input record which is the source of all fields.
	 * @return true if SQL record exists and is updated.
	 */
	boolean update(Record rec) 
			throws SQLException, IOException, JDOMException {
		Key key = rec.getKey();
		int count = 0;
		int n = columns.size();
		try {
			// Skip column 0 which is the sys_id
			for (int i = 1; i < n; ++i) {
				bindField(stmtUpdate, i, rec, i);
			}
			// Bind sys_id to the last position
			stmtUpdate.setString(n, key.toString());
			count = stmtUpdate.executeUpdate();
		}
		catch (SQLException e) {
			logFields(rec, "UPDATE");
			logger.error(rec.getXML(true) + "\n" + sqlUpdate, e);
			throw e;
		}
		logger.debug("UPDATE " + key + " rows=" + count);		
		if (count > 1) throw new AssertionError("update count=" + count);
		return (count > 0);
	}
	
	boolean deleteRecord(Key key) 
			throws SQLException {
		int count = 0;
		stmtDeleteRecord.setString(1, key.toString());
		count = stmtDeleteRecord.executeUpdate();
		logger.debug("DELETE " + key + " rows=" + count);
		if (count > 1) throw new AssertionError("delete count=" + count);
		return (count > 0);
	}
	
	/**
	 * Get sys_updated_on from the SQL table so that we can compare it
	 * with the Glide Record to determine if the SQL table needs to be updated.
	 * 
	 * @param key sys_id of record to be retrieved from SQL table
	 * @return sys_updated_on from the SQL table
	 * @throws SQLException
	 * @throws ParseException
	 */
	DateTime getTimestamp(Key key) throws SQLException {
		DateTime result = null;
		stmtReadTimestamp.setString(1, key.toString());
		ResultSet rset = stmtReadTimestamp.executeQuery();
		if (rset.next()) {
			java.util.Date ts = rset.getDate(1);
			result = new DateTime(ts);
		}
		rset.close();
		return result;
	}
	
	/*  This procedure is not used
	 * 
	@Deprecated
	Map<RecordKey,DateTime> getTimestamps(RecordKeyList keys) throws SQLException {
		StringBuilder keylist = new StringBuilder();
		for (int i = 0; i < keys.size(); ++i) {
			if (i > 0) keylist.append(",");
			keylist.append("'" + keys.get(i).toString() + "'");
		}
		Map<String,String> myvars = new HashMap<String,String>();
		myvars.put("keylist", keylist.toString());
		String sql = generator.getTemplate("timestamps", sqlTableName, myvars);
		Statement stmt = connection.createStatement();
		ResultSet rset = stmt.executeQuery(sql);
		HashMap<RecordKey, DateTime> result = new HashMap<RecordKey, DateTime>();
		while (rset.next()) {
			RecordKey key = new RecordKey(rset.getString(1));
			DateTime updated = new DateTime(rset.getTimestamp(2));
			result.put(key, updated);
		}
		return result;
	}
	*/
	
	private void processRecord(Record rec, LoadMethod method, Metrics metrics) 
			throws IOException, JDOMException, SQLException {
		DateTime tsRec = rec.getUpdatedTimestamp();
		Key key = rec.getKey();
		String action = null;
		switch (method) {
		case INSERT_ONLY :
			this.insert(rec);
			metrics.incrementInserts();
			action = "insert";
			break;
		case UPDATE_INSERT :
			if (this.update(rec)) {
				metrics.incrementUpdates();
				action = "update";
			}
			else {
				this.insert(rec);
				metrics.incrementInserts();
				action = "insert";
			}
			break;
		case COMPARE_TIMESTAMPS : 
			DateTime tsDB = getTimestamp(key);
			if (tsDB == null) {
				this.insert(rec);
				metrics.incrementInserts();
				action = "insert";
			}
			else { 
				int compare = tsRec.compareTo(tsDB);
				if (compare > 0) {
					this.update(rec);
					metrics.incrementUpdates();
					action = "update";
				}
				else {
					metrics.incrementUnchanged();
					action = "unchanged";
				}						
			}
			break;
		}
		if (logger.isTraceEnabled())
			logger.trace(key + " " + tsRec + " " + action);		
	}
	
	int processRecordSet(RecordList data, LoadMethod method, Metrics metrics) 
			throws SuiteExecException {
		int count = 0;
		try {
			if (method.equals(LoadMethod.INSERT_ONLY) && database.batchInserts()) {
				count = insertBatch(data);
				metrics.incrementInserts(count);
			}
			else {
				for (Record rec : data) {
					processRecord(rec, method, metrics);
					++count;
				}
			}
			database.commit();
		}
		catch (SQLException e) {
			throw new SuiteExecException(e);
		}
		catch (IOException e) { 
			throw new SuiteExecException(e); 
		}					
		catch (JDOMException e) { 
			throw new SuiteExecException(e); 
		}
		metrics.incrementPublished(count);
		return count;
	}
	
	int deleteRecords(KeyList keys, Metrics metrics) throws SuiteExecException {
		int count = 0;
		try {
			for (Key key : keys) {
				if (deleteRecord(key)) {
					metrics.incrementDeletes();
					count += 1;
				}
			}
			database.commit();
		}
		catch (SQLException e) {
			throw new SuiteExecException(e);
		}
		metrics.incrementPublished(count);
		return count;
	}
	
	void commit() throws SQLException {
		database.commit();
	}
	
	/**
	 * Determine if a table already exists in the target database.
	 * 
	 * @return true if table exists; otherwise false
	 * @throws SQLException
	 */
	@Deprecated
	boolean tableExists() throws SQLException {
		return database.tableExists(sqlTableName);
	}
	
	@Deprecated
	void createIfNotExists(boolean displayValues) 
			throws SQLException, SuiteInitException {
		if (database.autoCreate() && !tableExists()) 
			createTable(displayValues);		
	}
	
	/**
	 * Create a table in the target database.
	 */
	@Deprecated
	void createTable(boolean displayValues) 
			throws SQLException, SuiteInitException {
		Statement stmt = database.getConnection().createStatement();
		String createSql = generator.getCreateTable(
			this.table, sqlTableName, displayValues);
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
		database.commit();
	}

	@Deprecated
	void executeSQL(String command) throws SQLException {
		database.executeStatement(command);
		database.commit();		
	}
	
}
