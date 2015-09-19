package servicenow.common.soap;

@SuppressWarnings("serial")
public class InvalidTableNameException extends IllegalArgumentException {
	
	InvalidTableNameException(String name) {
		super("InvalidTableName: " + name);
	}

}
