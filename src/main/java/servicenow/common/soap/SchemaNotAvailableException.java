package servicenow.common.soap;

@SuppressWarnings("serial")
public class SchemaNotAvailableException extends IllegalStateException {

	public SchemaNotAvailableException(Exception e) {
		super(e);
	}

}
