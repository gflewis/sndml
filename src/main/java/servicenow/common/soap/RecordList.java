package servicenow.common.soap;

import java.util.*;

import org.jdom2.Element;

import servicenow.common.soap.FieldDefinition;
import servicenow.common.soap.InvalidFieldNameException;
import servicenow.common.soap.Key;
import servicenow.common.soap.KeyList;
import servicenow.common.soap.Record;
import servicenow.common.soap.RecordIterator;
import servicenow.common.soap.Table;

/**
 * An array of Records.
 *
 */
public class RecordList extends ArrayList<Record> {
	
	private static final long serialVersionUID = 1L;

	RecordList() {
		super();
	}

	RecordList(int size) {
		super.ensureCapacity(size);
	}
	
	public RecordIterator iterator() {
		return new RecordIterator(this);
	}

	/**
	 * Extract all the values of a reference field from a list of records.
	 * Null keys are not included in the list.
	 * @param fieldname Name of a reference field
	 * @return A list keys
	 */
	public KeyList extractKeys(String fieldname) {
		KeyList result = new KeyList(this.size());
		if (this.size() == 0) return result;
		Table table = this.get(0).getTable();
		String tablename = table.getName();
		if (table.validate) {
			FieldDefinition defn = table.getSchema().getFieldDefinition(fieldname);
			if (defn == null) throw new InvalidFieldNameException(fieldname);
			if (!defn.isReference()) throw new InvalidFieldNameException(fieldname);
		}
		for (Record rec : this) {
			assert tablename.equals(rec.table.getName());
			String value = rec.getField(fieldname, false);
			if (value != null) result.add(new Key(value));
		}		
		return result;
	}

	/**
	 * Extract the primary keys (sys_ids) from this list.
	 */
	public KeyList extractKeys() {
		return extractKeys("sys_id");
	}
	
	/**
	 * Returns this list of records as a JDOM Element.
	 * @param name Name to be assigned to the root element
	 * @return A JDOM Element constructed from this list of records
	 */
	public org.jdom2.Element getElement(String name) {
		Element result = new Element(name);
		for (Record rec : this) {
			result.addContent(rec.getElement());
		}
		return result;
	}
}
