package servicenow.common.datamart;

import servicenow.common.datamart.SuiteInitException;

@SuppressWarnings("serial")
public class SuiteParseException extends SuiteInitException {

	public SuiteParseException(String message) {
		super(message);
	}

	public SuiteParseException(
			String message, String buffer, int lineptr, int charptr) {
		super(message + 
			"\nat line " + (lineptr + 1) + " char " + (charptr + 1) +
			"\nof \"" + buffer + "\"");
	}

	public SuiteParseException(Exception e) {
		super(e);
	}
}
