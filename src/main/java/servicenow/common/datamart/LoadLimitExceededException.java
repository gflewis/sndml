package servicenow.common.datamart;

import servicenow.common.datamart.SuiteExecException;

@SuppressWarnings("serial")
public class LoadLimitExceededException extends SuiteExecException {
	
	public LoadLimitExceededException(String tableName, int limit) {
		super("table=" + tableName + " loadLimit=" + limit);
	}
	
}
