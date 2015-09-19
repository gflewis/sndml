package servicenow.common.soap;

import java.io.IOException;
import java.util.ArrayList;

import servicenow.common.soap.FieldValues;
import servicenow.common.soap.Table;

/**
 * Used as an input argument for 
 * {@link Table#insertMultiple(java.util.List) Table#insertMultiple}.
 */
@SuppressWarnings("serial")
public class FieldValuesList extends ArrayList<FieldValues> {

	/**
	 * Clone the values and add to the list.
	 */
	public boolean add(FieldValues values) {
		return super.add((FieldValues) values.clone());
	}
	
	public void insert(Table table) throws IOException {
		table.insertMultiple(this);
	}
	
}
