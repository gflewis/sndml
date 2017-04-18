package servicenow.common.soap;

import org.jdom2.*;

public class PartialRecord extends Record {

	final protected FieldNames fields;
	
	protected PartialRecord(Table table, Element element, FieldNames fields) 
			throws SoapResponseException {
		super(table, element);
		this.fields = fields;
	}
}
