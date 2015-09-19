package servicenow.common.soap;

import org.jdom2.*;

import servicenow.common.soap.FieldDefinition;
import servicenow.common.soap.Key;
import servicenow.common.soap.Record;
import servicenow.common.soap.Table;

/**
 * Contains data type information for a single field in a ServiceNow table.
 * @author Giles Lewis
 *
 */
public class FieldDefinition {

	private final String name;
	private final String type;
	private final int max_length;
	private final String ref_table;
	private final boolean displayValue;
	
	/**
	 * Construct a FieldDefinition from sys_dictionary record.
	 * 
	 * @param table - The table in which this field appears.
	 * @param dictionary - The sys_dictionary record that describes this field.
	 */
	protected FieldDefinition(Table table, Record dictionary) {
		Element element = dictionary.element;
		name = element.getChildText("element");
		type = element.getChildText("internal_type");
		assert name != null : "Field has no name";
		assert type != null : "Field " + name + " has no type";
		max_length = Integer.parseInt(element.getChildText("max_length"));
		ref_table = element.getChildText("reference");
		displayValue = false;
	}

	/**
	 * Construct a FieldDefinition for a DisplayValue field.
	 * 
	 * @param name Name of the reference field.
	 * @param dv Must be true; otherwise an exception will be thrown.
	 */
	protected FieldDefinition(String name, boolean dv) {
		if (!dv) 
			throw new IllegalArgumentException("expected true");
		if (!(name.length() > 3 && name.substring(0, 3).equals("dv_")))
			throw new IllegalArgumentException("bad field name");
		this.name = name;
		this.type = "string";
		this.max_length = 255;
		this.ref_table = "";
		this.displayValue = dv;
	}

	/**
	 * For internal use.
	 * </p>
	 * Converts a reference field definition into a display value field definition. 
	 */
	public FieldDefinition displayValueDefinition() {
		if (this.isReference()) {
			FieldDefinition result = new FieldDefinition("dv_" + this.name, true);
			return result;			
		}
		else {
			return null;
		}
	}

	/**
	 * Return the name of this field.
	 */
	public String getName() { return name; }
	
	/**
	 * Return the type of this field.
	 */
	public String getType() { return type; }
	
	/**
	 * Return the length of this field.
	 */
	public int getLength() { return max_length; }
	
	/**
	 * If this is a reference field then return the name of the
	 * referenced table.  Otherwise return null.
	 */
	public String getReference() { return ref_table; }
	
	/**
	 * Return true if the field is a reference field.
	 * The value of a reference field is always a {@link Key} (sys_id).
	 */
	public boolean isReference() { return (ref_table != null && ref_table.length() > 0); }
	
	/**
	 * Return true if the field is a DisplayValue pseudo-value.
	 * These fields correspond to reference fields.  They cannot be updated.
	 * The name of a DisplayValue field is always "dv_name" 
	 * where "name" is the name of the corresponding reference field.
	 * 
	 */
	public boolean isDisplayValue() { return displayValue; }
	
}
