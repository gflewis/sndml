package servicenow.common.datamart;

import servicenow.common.datamart.SuiteException;

/**
 * Exception while trying to run a suite
 */
@SuppressWarnings("serial")
public class SuiteExecException extends SuiteException {

	public SuiteExecException(String message) {
		super(message);
	}
	
	public SuiteExecException(Throwable cause) {
		super(cause);
	}
	
	public SuiteExecException(String message, Throwable cause) {
		super(message, cause);
	}

}
