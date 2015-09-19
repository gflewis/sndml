package servicenow.common.soap;

import servicenow.common.soap.Table;

@SuppressWarnings("serial")
public class InvalidDateTimeException extends IllegalArgumentException {

	InvalidDateTimeException(String value) {
		super(value);
	}
	
	InvalidDateTimeException(Table table, String fieldname, String value) {
		super(table.getName() + "." + fieldname + "=" + value);
	}
	
}
