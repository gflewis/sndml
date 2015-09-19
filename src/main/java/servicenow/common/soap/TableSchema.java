package servicenow.common.soap;

import java.io.*;
import java.util.*;

import org.jdom2.JDOMException;
import org.slf4j.Logger;

import servicenow.common.soap.FieldDefinition;
import servicenow.common.soap.InsufficientRightsException;
import servicenow.common.soap.InvalidTableNameException;
import servicenow.common.soap.QueryFilter;
import servicenow.common.soap.Record;
import servicenow.common.soap.RecordList;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;
import servicenow.common.soap.TableSchema;

/**
 * This class holds the the schema or definition for a ServiceNow table.
 * The definition is read from <b>sys_dictionary</b> by the {@link Table} constructor.
 * 
 * @author Giles Lewis
 *
 */
public class TableSchema {

	private final Table table;
	private final Session session;
	private final String tablename;
	private final String parentname;
	private TreeMap<String,FieldDefinition> fields;
	static final Logger logger = Session.getLogger(TableSchema.class);

	protected TableSchema(Table table) 
			throws IOException, JDOMException, InterruptedException,
				InvalidTableNameException {
		this.table = table;
		this.session = table.session;
		this.tablename = table.getName();
		logger.debug("get definition for table " + tablename);
		fields = new TreeMap<String,FieldDefinition>();
		Table dictionary = session.table("sys_dictionary");
		parentname = determineParentName();
		logger.debug(tablename + " parent is " + parentname);
		if (parentname != null) {
			// recursive call for parent definition
			TableSchema parentDefinition = session.table(parentname).getSchema();
			Iterator<FieldDefinition> parentIter = parentDefinition.iterator();
			while (parentIter.hasNext()) {
				FieldDefinition parentField = parentIter.next();
				String fieldname = parentField.getName();
				fields.put(fieldname, parentField);
			}
		}
		QueryFilter filter = new QueryFilter().
			addFilter("name",  tablename).
			addFilter("active", "true");
		RecordList list = dictionary.petitReader(filter).getAllRecords();
		boolean empty = true;
		ListIterator<Record> iter = list.listIterator(); 
		while (iter.hasNext()) {
			Record rec = iter.next();
			String fieldname = rec.getField("element");
			if (fieldname != null) {
				FieldDefinition fieldDef = new FieldDefinition(table, rec);
				fields.put(fieldname, fieldDef);
				logger.debug(tablename + "." + fieldname);
				empty = false;
			}
		}
		if (empty) {
			logger.error("Unable to read schema for: " + tablename);
			if (tablename.equals("sys_db_object") || tablename.equals("sys_dictionary"))
				throw new InsufficientRightsException(table, "getRecords",
					"Unable to read " + tablename);
			else
				throw new InvalidTableNameException(tablename);
		}
	}

	private String determineParentName() 	
			throws IOException, JDOMException {
		if (this.tablename.startsWith("sys_")) return null;
		Table hierarchy = session.table("sys_db_object");
		Record h = hierarchy.get("name", this.tablename);
		if (h == null) return null;
		Record parent = h.getRecord("super_class", hierarchy);
		if (parent == null) return null;
		return parent.getField("name");		
	}
	
	/**
	 * Return the name of the parent table or null if this table has no parent.
	 */
	public String getParentName() {
		return parentname;
	}
	
	/**
	 * Return a list of all the fields in a table.
	 */
	public String[] getFieldNames() {
		return fields.keySet().toArray(new String[0]);
	}

	/**
	 * Return the type definition for a field.
	 */
	public FieldDefinition getFieldDefinition(String fieldname) {
		return fields.get(fieldname);
	}

	/**
	 * Return the type definition for the sys_id.
	 */
	public FieldDefinition getKeyFieldDefinition() {
		return fields.get("sys_id");
	}

	/**
	 * Return true if the table has a field with this name; otherwise false.
	 */
	@Deprecated
	public boolean isDefined(String fieldname) {
		if (fields.get(fieldname) != null) return true;
		if (fieldname.startsWith("dv_") && table.displayvalues) {
			String refname = fieldname.substring(3);
			return isReference(refname);
		}
		return false;
	}
	
	/**
	 * Return true if the table has a reference field with this name; otherwise false.
	 */
	public boolean isReference(String fieldname) {
		FieldDefinition fd = fields.get(fieldname);
		if (fd == null) return false;
		return fd.isReference();
	}

	/**
	 * Return a collection of all the field type definitions.
	 */
	public Collection<FieldDefinition> getFieldDefinitions() {
		return fields.values();
	}
	
	/**
	 * Return an iterator which loops through all the field definitions.
	 */
	public Iterator<FieldDefinition> iterator() {
		return getFieldDefinitions().iterator();
	}

	/**
	 * Return the number of fields in this table.
	 */
	public int numFields() {
		return fields.size();
	}

	/*
	public void report() {
		String tablename = table.getName();
		System.out.println("Table Definition for " + tablename);
		int row = 0;
		Iterator<FieldDefinition> iter = iterator();
		while (iter.hasNext()) {
			FieldDefinition fd = iter.next();
			String fieldname = fd.getName();
			String fieldtype = fd.getType();
			int fieldlen = fd.getLength();
			System.out.println(++row + " " + fieldname + " " + fieldtype + " " + fieldlen);			
		}
		System.out.println("End of Report");
	}
	*/
}
