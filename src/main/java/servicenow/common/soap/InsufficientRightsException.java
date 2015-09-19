package servicenow.common.soap;

import servicenow.common.soap.Table;

@SuppressWarnings("serial")
public class InsufficientRightsException extends java.io.IOException {

	InsufficientRightsException(
			Table table, String method, String message) {
		super(message + "; table=" + table.getName());
	}

	InsufficientRightsException() {
		super();
	}
}
