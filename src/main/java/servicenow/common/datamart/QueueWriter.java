package servicenow.common.datamart;

import java.sql.SQLException;

import servicenow.common.datamart.DatamartConfiguration;
import servicenow.common.datamart.ResourceException;
import servicenow.common.datamart.SuiteInitException;
import servicenow.common.datamart.TargetTableWriter;
import servicenow.common.datamart.TargetWriter;

import servicenow.common.soap.Table;

// TODO Implement this class
/**
 * Implementation of this class was never completed.
 */
@Deprecated
public class QueueWriter extends TargetWriter {

	public QueueWriter(DatamartConfiguration config) {
		throw new UnsupportedOperationException();
	}

	@Override
	void open() throws ResourceException {		
	}

	@Override
	void close() {		
	}

	@Override
	TargetTableWriter getTableWriter(
			Table table, 
			String sqlTableName,
			boolean displayValues)
			throws SuiteInitException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	void createMissingTargetTable(Table table, String sqlTableName,
			boolean displayValues) throws SQLException, SuiteInitException {
		// TODO Auto-generated method stub		
	}

	@Override
	void executeSQL(String command) throws SQLException {
		// TODO Auto-generated method stub				
	}
	
	@Override
	void rollback() {
		// TODO Auto-generated method stub		
	}

}
