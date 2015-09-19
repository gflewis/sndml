package servicenow.common.datamart;

import servicenow.common.datamart.SuiteException;

/**
 * Exception occurred while trying to read the contents of a suite
 * or update the status of a suite.
 */
@SuppressWarnings("serial")
public class SuiteModelException extends SuiteException {

	public SuiteModelException(String message) {
		super(message);
	}

	public SuiteModelException(Throwable cause) {
		super(cause);
	}

	public SuiteModelException(String message, Throwable cause) {
		super(message, cause);
	}

}
