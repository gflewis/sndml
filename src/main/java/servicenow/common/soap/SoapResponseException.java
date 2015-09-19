package servicenow.common.soap;

import java.io.IOException;

import servicenow.common.soap.Table;

/**
 * Exception thrown when there is an undetermined problem with a SOAP response.
 */
@SuppressWarnings("serial")
public class SoapResponseException extends IOException {

	SoapResponseException(Table table, String message) {
		super("table=" + table.getName() + " " + message);
	}
	
	SoapResponseException(Table table, Exception cause, String response) {
		super(response + "\ntable=" + table.getName(), cause);
	}

	SoapResponseException(Table table, String message, String response) {
		super(response == null ? 
			message + "\ntable=" + table.getName() : 
			message + "\ntable=" + table.getName() + "\n" + response);
	}
}
