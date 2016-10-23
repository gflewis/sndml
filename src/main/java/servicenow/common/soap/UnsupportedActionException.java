package servicenow.common.soap;

@SuppressWarnings("serial")
public class UnsupportedActionException extends SoapResponseException {

	UnsupportedActionException(Table table, String methodname, String message) {
		super(table, message);
	}

}
