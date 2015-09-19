package servicenow.common.datamart;

import servicenow.common.datamart.SuiteException;

/**
 * Exception encountered while trying to initialize a suite.
 */
@SuppressWarnings("serial")
public class SuiteInitException extends SuiteException {

	public SuiteInitException(String message) {
		super(message);
	}

	public SuiteInitException(Exception e) {
		super(e);
	}
}
