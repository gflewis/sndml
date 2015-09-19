package servicenow.common.datamart;

/**
 * Exception while trying to initialize or run a suite.
 */
@SuppressWarnings("serial")
public class SuiteException extends Exception {

	public SuiteException(String message) {
		super(message);
	}

	public SuiteException(Throwable cause) {
		super(cause);
	}

	public SuiteException(String message, Throwable cause) {
		super(message, cause);
	}

}
