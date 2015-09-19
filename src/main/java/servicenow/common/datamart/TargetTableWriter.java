package servicenow.common.datamart;

import java.sql.SQLException;

import servicenow.common.datamart.DatabaseWriter;
import servicenow.common.datamart.LoadMethod;
import servicenow.common.datamart.Metrics;
// import servicenow.common.datamart.QueueWriter;
import servicenow.common.datamart.SuiteExecException;
import servicenow.common.datamart.TargetWriter;

import servicenow.common.soap.KeyList;
import servicenow.common.soap.RecordList;
import servicenow.common.soap.Table;

/**
 * This is an abstract class which was introduced in order to support writing
 * to queues rather than databases. 
 * However the {@link QueueWriter} implementation was never completed.
 * 
 * This class has only two implementations
 * <ul>
 *   <li>{@link DatabaseWriter} - active</li>
 *   <li>{@link QueueWriter} - not active</li>
 * </ul>
 *
 */
abstract class TargetTableWriter {

	protected final TargetWriter target;
	protected final Table table;
	protected String sqlTableName;
	
	TargetTableWriter(
			TargetWriter target, 
			Table table, 
			String sqlTableName) {
		this.target = target;
		this.table = table;
		this.sqlTableName = sqlTableName;
	}
	
	TargetWriter getTargetWriter() { return this.target; }
		
	abstract void truncateTable() throws SuiteExecException;
	
	abstract int processRecordSet(RecordList data, LoadMethod method, Metrics metrics) 
			throws SuiteExecException;

	abstract int deleteRecords(KeyList keys, Metrics metrics) 
			throws SuiteExecException;

	@Deprecated
	void executeSQL(String command) throws SQLException {
		target.executeSQL(command);
	}

	@Deprecated
	void rollback() { 
		target.rollback();
	}
			
}
