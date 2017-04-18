package servicenow.common.soap;

import java.io.IOException;
import java.util.*;

import org.jdom2.*;

import servicenow.common.soap.DateTime;
import servicenow.common.soap.FieldDefinition;
import servicenow.common.soap.InvalidDateTimeException;
import servicenow.common.soap.InvalidFieldNameException;
import servicenow.common.soap.Key;
import servicenow.common.soap.QueryFilter;
import servicenow.common.soap.Record;
import servicenow.common.soap.SoapResponseException;
import servicenow.common.soap.Table;
import servicenow.common.soap.XMLFormatter;

/**
 * Contains an XML document (in the form of a JDOM Element) which 
 * has been retrieved from ServiceNow.
 * The only way to obtain one of these is to use 
 * {@link Table#get(Key) Table.get()} or
 * {@link Table#getRecords(QueryFilter) Table.getRecords()}. 
 * 
 * @author Giles Lewis
 */
public abstract class Record {
	
	final protected Table table;
	final protected Key key;
	final protected Element element;
	final protected Namespace ns;
	protected DateTime updatedTimestamp;
	protected DateTime createdTimestamp;
	
	protected Record(Table table, Element element) 
			throws SoapResponseException {
		this.table = table;
		this.element = element;
		this.ns = element.getNamespace();
		String sysid = element.getChildText("sys_id", ns);
		this.key = (sysid == null || sysid.length() == 0) ? null : new Key(sysid);
		String updated_on = element.getChildText("sys_updated_on", ns);
		String created_on = element.getChildText("sys_created_on", ns);
		this.createdTimestamp =
			created_on == null ? null : new DateTime(created_on);
		this.updatedTimestamp =
			updated_on == null ? null : new DateTime(updated_on);
	}

	/**
	 * Return a clone of the JDOM Element underlying this object.
	 * The name of the returned element is the table name.
	 */
	Element getElement() {
		Element result = element.clone();
		result.setName(table.getName());
		return result;
	}
	
	/**
	 * The table from which this record was retrieved.
	 */
	public Table getTable() { return this.table; }
	
	/**
	 * The number of XML elements in this record,
	 * which may be fewer than getTable().getSchema().numFields().
	 */
	public int numFields() { 
		return element.getContentSize(); 
	}

	/**
	 * Returns the underlying JDOM Element as a formatted XML string.  
	 * Use for debugging and diagnostics.
	 */
	public String getXML() {
		return getXML(false);
	}

	/**
	 * Returns the underlying JDOM Element as a formatted XML string.  
	 * Use for debugging and diagnostics.
	 */
	public String getXML(boolean pretty) {
		return XMLFormatter.format(element, pretty);
	}
	
	public FieldNames getFieldNames() {
		FieldNames result = new FieldNames();
		for (Element field : element.getChildren()) {
			result.add(field.getName());
		}
		return result;
	}
	
	public LinkedHashMap<String,String> getAllFields() {
		LinkedHashMap<String,String> result = new LinkedHashMap<String,String>();
		for (Element field : element.getChildren()) {
			String name = field.getName();
			String value = field.getText();
			result.put(name,  value);
		}
		return result;
	}
	
	/**
	 * Get the value of a field from a Record.
	 * If the table has no field by this name then an exception will be thrown.
	 * If the field is empty (zero length) or missing then return null.
	 * 
	 * @param fieldname Name of Record field
	 * @return Null if the field is missing or has zero length, 
	 * otherwise the field value as a string
	 * @throws InvalidFieldNameException 
	 */
	public String getField(String fieldname) throws InvalidFieldNameException {
		return getField(fieldname, table.validate);
	}

	/**
	 * Get the value of a field from a record
	 * with the option of performing or suppressing validation
	 * @param fieldname Name of a Record field
	 * @param validate Indicates whether to perform name checking 
	 * @return Null if the field is missing or has zero length, 
	 * otherwise the field value as a string
	 * @throws InvalidFieldNameException
	 */
	public String getField(String fieldname, boolean validate) 
			throws InvalidFieldNameException {
		String result = element.getChildText(fieldname, ns);
		if (result == null) {
			if (validate) {
				if (!table.getWSDL().canReadField(fieldname))
					throw new InvalidFieldNameException(fieldname);
			}
			return null;
		}
		if (result.length() == 0) return null;
		return result;		
	}
	
	/**
	 * Get the sys_id from a Record object.  
	 * 
	 * @return sys_id
	 */
	public Key getKey() {
		return key;
	}

	/**
	 * Returns the Display Value of a reference field.
	 * You must use {@link Table#setDisplayValues(boolean)}
	 * to enable Display Values.  
	 * 
	 * @param fieldname
	 * @throws InvalidFieldNameException
	 * Specified field is not a reference field.
	 * @throws IllegalStateException
	 * Display values not enabled using
	 * {@link Table#setDisplayValues(boolean)}.
	 */
	public String getDisplayValue(String fieldname)
			throws InvalidFieldNameException, IllegalStateException {
		if (!table.displayvalues)
			throw new IllegalStateException(
				"DisplayValues not enabled; use setDisplayValue(true)");
		String dvname = "dv_" + fieldname;
		String result = element.getChildText(dvname, ns);
		if (result == null) {
			FieldDefinition fd = table.getSchema().getFieldDefinition(fieldname);
			if (fd.isReference()) 
				return null;
			else
				throw new InvalidFieldNameException(
					"Not a reference field: " + fieldname);
		}
		if (result.length() == 0) return null;
		return result;
	}
	
	public Key getReference(String fieldname) throws InvalidFieldNameException {		
		String value = element.getChildText(fieldname, ns);
		if (value == null || value.length() == 0) return null;
		if (table.validate) {
			FieldDefinition defn = table.getSchema().getFieldDefinition(fieldname);
			if (defn == null) throw new InvalidFieldNameException(fieldname);
			if (!defn.isReference()) throw new InvalidFieldNameException(fieldname);
		}
		return new Key(value);
	}
	
	/**
	 * Get sys_updated_on from a Record object.
	 */
	public DateTime getUpdatedTimestamp() {
		return this.updatedTimestamp;
	}
	
	/**
	 * Get sys_created_on from a Record object.
	 */
	public DateTime getCreatedTimestamp() {
		return this.createdTimestamp;
	}
	
	/**
	 * If a reference field in a Record contains a valid sys_id, then
	 * return the Record to which the reference field points.
	 * If the reference field is null then return null.
	 * This function requires a Table object as the second parameter
	 * so that the function will know which table to query.
	 *  
	 * @param fieldname Name of the reference field.
	 * @param table Table to which the reference field points, 
	 * or null if the value of the reference field is null.
	 * @return Record to which the reference field points.
	 * @throws IOException
	 */
	public Record getRecord(String fieldname, Table table) 
			throws IOException, SoapResponseException  {
		String fieldvalue = getField(fieldname);
		if (fieldvalue == null) return null;
		Key sysid = new Key(fieldvalue);
		Record result = table.get(sysid);
		return result;
	}

	/**
	 * If a reference field in a Record contains a valid sys_id, then
	 * return the Record to which the reference field points.
	 * If the reference field is null then return null.
	 * <p/>
	 * This function can be used to simulate the "dot walking" behavior
	 * of JavaScript.  The following example will set <code>name</code>
	 * to the manager of the "Network Support" group.
	 * The <code>get</code> function returns a Record
	 * for the sys_user_group table and the the
	 * <code>getRecord</code> function returns a Record
	 * for the sys_user table.  
	 * However, if the
	 * group has no manager this code will throw a NullPointerException.
	 * <pre>
	 * String name = session.table("sys_user_group").get("name", "Network Support").
	 *   getRecord("manager").getField("name");
	 * </pre>
	 * 
	 * @param fieldname Name of the reference field.
	 * @return A retrieved copy of the Record to which the reference field points, 
	 * or null if the value of the reference field is null.
	 * @throws IOException
	 */
	public Record getRecord(String fieldname) 
			throws IOException, JDOMException, SoapResponseException  {
		FieldDefinition fielddef = 
			table.getSchema().getFieldDefinition(fieldname);
		if (fielddef == null)
			throw new InvalidFieldNameException(fieldname);
		String tablename = fielddef.getReference();
		return getRecord(fieldname, table.session.table(tablename));
	}

	/**
	 * Get the value of an integer field.
	 */
	public int getInt(String fieldname) {
		String value = getField(fieldname);
		if (value == null) return 0;
		return Integer.parseInt(value);
	}
	   	
	/**
	 * Get the value of a DateTime field.
	 * For a Java date use getDateTime(fieldname).toDate().
	 * @throws InvalidDateTimeException 
	 */
	public DateTime getDateTime(String fieldname) throws InvalidDateTimeException {
		String value = getField(fieldname);
		if (value == null) return null;
		DateTime result = new DateTime(value, DateTime.DATE_TIME);
		return result;
	}
	
	/**
	 * Get the value of a Date field.
	 * For a Java date use getDate(fieldname).toDate().
	 * @throws InvalidDateTimeException 
	 */
	public DateTime getDate(String fieldname) throws InvalidDateTimeException {
		String value = getField(fieldname);
		if (value == null) return null;
		DateTime result = new DateTime(value, DateTime.DATE_ONLY);
		return result;
	}

	/**
	 * Get the value of a boolean field.
	 */
	public boolean getBoolean(String fieldname) throws SoapResponseException {
		String value = getField(fieldname);
		if (value == null) return false;
		if (value.equals("0") || value.equals("false")) return false;
		if (value.equals("1") || value.equals("true")) return true;
		throw new SoapResponseException(
			table, "getBoolean " + fieldname + "=" + value);
	}
	
	/**
	 * Get the value of a duration field converted to seconds.
	 */
	public Integer getDuration(String fieldname) throws InvalidDateTimeException {
		String value = getField(fieldname);
		if (value == null) return null;
		// value will be stored as yyyy-mm-dd hh:mm:ss
		DateTime dt;
		try {
			dt = new DateTime(value, DateTime.DATE_TIME);
		}
		catch (InvalidDateTimeException e) {
			throw new InvalidDateTimeException(table, fieldname, value);
		}
		long millis = dt.getMillisec();
		return new Integer((int) (millis / 1000));
	}

	/**
	 * Change the value of a field in this record.
	 */
	@Deprecated
	public void setField(String fieldname, String fieldvalue) {
		Element child = element.getChild(fieldname);
		child.setText(fieldvalue);
	}

	/**
	 * Change the value of an integer field.
	 */
	@Deprecated
	public void setInt(String fieldname, int value) {
		setField(fieldname, Integer.toString(value));
	}
	
	/**
	 * Change the value of a boolean field.
	 */
	@Deprecated
	public void setBoolean(String fieldname, boolean value) {
		setField(fieldname, (value ? "1" : "0"));
	}

}
