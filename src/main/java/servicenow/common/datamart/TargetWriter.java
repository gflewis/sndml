package servicenow.common.datamart;

import java.sql.SQLException;

import servicenow.common.datamart.DatabaseTableWriter;
import servicenow.common.datamart.DatabaseWriter;
import servicenow.common.datamart.DatamartConfiguration;
// import servicenow.common.datamart.QueueTableWriter;
// import servicenow.common.datamart.QueueWriter;
import servicenow.common.datamart.ResourceException;
import servicenow.common.datamart.SuiteInitException;
import servicenow.common.datamart.TargetTableWriter;
import servicenow.common.datamart.TargetWriter;

import servicenow.common.soap.Table;

/**
 * This is an abstract class which was introduced in order to support writing
 * to queues rather than databases. 
 * However the {@link QueueTableWriter} implementation was never completed.
 * 
 * This class has only two implementations
 * <ul>
 *   <li>{@link DatabaseTableWriter} - active</li>
 *   <li>{@link QueueTableWriter} - not active</li>
 * </ul>
 *
 */
public abstract class TargetWriter {

	@SuppressWarnings("deprecation")
	static TargetWriter newTargetWriter(DatamartConfiguration config) {
		TargetWriter target;
		String datamartUrl = config.getRequiredString("url");
		if (datamartUrl.startsWith("amqp")) {
			target = new QueueWriter(config);
		}
		else {
			target = new DatabaseWriter(config);
		}
		return target;
	}
	
	abstract TargetTableWriter getTableWriter(
			Table table, 
			String sqlTableName,
			boolean displayValues)
			throws SuiteInitException;
	
	abstract void open() throws ResourceException;
	
	abstract void executeSQL(String command) throws SQLException;
	
	abstract void rollback();
	
	abstract void close();
	
	abstract void createMissingTargetTable(Table table, String sqlTableName, boolean displayValues)
			throws SQLException, SuiteInitException;
	
}
