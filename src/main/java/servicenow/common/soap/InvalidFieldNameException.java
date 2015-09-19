package servicenow.common.soap;

@SuppressWarnings("serial")
public class InvalidFieldNameException extends IllegalArgumentException {

	InvalidFieldNameException(String name) {
		super("InvalidFieldName: " + name);
	}

}
