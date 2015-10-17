package servicenow.common.soap;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.jdom2.*;

import java.lang.reflect.UndeclaredThrowableException;
import java.net.FileNameMap;
import java.net.URLConnection;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;

import servicenow.common.soap.BasicTableReader;
import servicenow.common.soap.FieldValues;
import servicenow.common.soap.InsertMultipleResponse;
import servicenow.common.soap.InsertResponse;
import servicenow.common.soap.InvalidFieldNameException;
import servicenow.common.soap.InvalidTableNameException;
import servicenow.common.soap.Key;
import servicenow.common.soap.KeyList;
import servicenow.common.soap.KeyReader;
import servicenow.common.soap.Parameters;
import servicenow.common.soap.PetitTableReader;
import servicenow.common.soap.QueryFilter;
import servicenow.common.soap.Record;
import servicenow.common.soap.RecordList;
import servicenow.common.soap.SchemaNotAvailableException;
import servicenow.common.soap.Session;
import servicenow.common.soap.SoapResponseException;
import servicenow.common.soap.Table;
import servicenow.common.soap.TableConfiguration;
import servicenow.common.soap.TableProxy;
import servicenow.common.soap.TableReader;
import servicenow.common.soap.TableSchema;
import servicenow.common.soap.TableWSDL;
import servicenow.common.soap.XMLFormatter;

/**
 * Used to access a ServiceNow table.
 * The recommended way to obtain a Table object is the
 * {@link Session#table(String)} method.
 * 
 * The table schema is read from sys_dictionary.
 * 
 * @author Giles Lewis
 *
 */
public class Table {
	
	final String tablename;
	final Session session;
	final TableConfiguration config;
	final Namespace nsTNS;
	final TableProxy proxy;
	final TableWSDL wsdl;
	TableWSDL dvWsdl;
	
	// these two loggers are visible to other classes in this package
	final Logger requestlog, responselog;

	static public final int DEFAULT_CHUNK_SIZE = 200;
	
	TableSchema schema = null;
	
	// Number of records to be retrieved in a single getRecords call
	int chunkSize;
	
	/*
	 * The validate property disables schema validations.
	 * This property is false for system tables 
	 * (sys_db_object and sys_dictionary)
	 * because they are read before their schemas are loaded.
	 * However, validate should be set to true in all other cases. 
	 */
	protected boolean validate = true;		
	protected boolean displayvalues = false;
	protected Boolean readable = null;
	
	/**
	 * The recommended way to obtain a Table
	 * object is the
	 * {@link Session#table(String)} method. 
	 */
	Table(Session session, String tablename) 
			throws IOException, JDOMException, InvalidTableNameException {
		this.tablename = tablename;
		this.session = session;
		this.config = new TableConfiguration(session.getConfiguration(), tablename);
		requestlog = Session.getLogger("Request." + tablename);
		responselog = Session.getLogger("Response." + tablename);
		nsTNS = Namespace.getNamespace("tns", "http://www.service-now.com/" + tablename);
		proxy = new TableProxy(this);
		wsdl = new TableWSDL(session, tablename);
		// this parameter can be named either "chunk" or "limit"
		this.chunkSize = config.getInt("chunk",
				config.getInt("limit", DEFAULT_CHUNK_SIZE));
		this.validate = session.validate;
	}
	
	public String getName() { return this.tablename; }	
	// String getParentName() { return this.parentname; }
	Namespace getNamespace() { return this.nsTNS; }
	TableConfiguration getConfiguration() { return this.config; }

	public TableWSDL getWSDL(boolean dv) throws IOException { 
		if (dv) {
			if (dvWsdl == null)
				try {
					dvWsdl = new TableWSDL(session, tablename, dv);
				} catch (InvalidTableNameException e) {
					throw new IllegalStateException(e);
				}
			return dvWsdl;
		}
		else {
			return wsdl;
		}
	}
	
	public TableWSDL getWSDL() {
		return (displayvalues ? dvWsdl : wsdl);
	}
	
	/*
	 * Load the table schema from sys_dictionary and sys_db_object.
	 *
	protected synchronized Table loadSchema() throws IOException, JDOMException {
		schema = new TableSchema(this);
		return this;
	}
	*/
	
	/**
	 * Returns the table schema as obtained from sys_dictionary
	 * when the class was instantiated.
	 * @throws IOException 
	 */
	public synchronized TableSchema getSchema() 
			throws SchemaNotAvailableException {
		if (schema != null) return schema;
		try {
			schema = new TableSchema(this);
		}
		  catch (InvalidTableNameException e) {
		    throw new SchemaNotAvailableException(e);
		} catch (IOException e) {
			throw new SchemaNotAvailableException(e);
		} catch (JDOMException e) {
			throw new SchemaNotAvailableException(e);
		} catch (InterruptedException e) {
			throw new UndeclaredThrowableException(e);
		}
		return schema;
	}

	/**
	 * Sets the default chunk size for any {@link TableReader}
	 * objects created from this {@link Table}.
	 * Affects the number of records to be retrieved in any
	 * single getRecords Web Service call.
	 * @see TableReader#setChunkSize(int)
	 */
	public synchronized Table setChunkSize(int size) {
		requestlog.info("setLimit " + size);
		if (size <= 0) 
			throw new IllegalArgumentException("invalid limit: " + size);
		this.chunkSize = size;
		return this;
	}
	
	/**
	 * This package performs run-time validation of field names.  
	 * If you misspell a field name then {@link InvalidFieldNameException}
	 * will be thrown. 
	 * While overhead this checking is very small,
	 * it is possible to disable the checks by setting validate to false.
	 * 
	 * @return The modified {@link Table} object.
	 */
	@Deprecated
	public synchronized Table setValidate(boolean validate) {
		requestlog.debug("setValidate " + validate);
		this.validate = validate;
		return this;
	}
	
	/**
	 * Causes Display Values to be made available for get and getRecords calls.
	 * <p/>
	 * The parameter "displayvalue=all" will be appended to the URL.
	 * For each reference field "name" there will be an additional
	 * field "dv_name" in the returned record which can be accessed
	 * using {@link Record#getField(String)}.
	 * </p>
	 * For additional info on this parameter refer to
	 * <a href="http://wiki.servicenow.com/index.php?title=Direct_Web_Services#Return_Display_Value_for_Reference_Variables"
	 * >http://wiki.servicenow.com/index.php?title=Direct_Web_Services</a>
	 * 
	 * @return The modified {@link Table} object.
	 */
	public synchronized Table setDisplayValues(boolean dv) throws IOException {
		requestlog.info("setDisplayValue " + dv);		
		this.displayvalues = dv;
		proxy.setURL(dv);
		if (dv && dvWsdl == null)
			dvWsdl = new TableWSDL(session, tablename, dv);
		return this;
	}
	
	private Element createXmlText(String name, String value) {
		Element ele = new Element(name);
		ele.setText(value);
		return ele;
	}

	/**
	 * Construct a JDOM Element to be included in an insert or update SOAP message.
	 * 
	 * @param method The name of the Element, which should be either "insert" or "update".
	 * @param sysid The sys_id to be updated.  This is applicable for updates only. 
	 * For an insert it should be null.
	 * @param rec A record from which all fields will be extracted.
	 */
	private Element createXmlElement(String method, Key sysid, Record rec) {
		Element element = new Element(method, getNamespace());
		if (sysid != null)
 			element.addContent(createXmlText("sys_id", sysid.toString()));
		Iterator<Element> fields = rec.element.getChildren().iterator();
		while (fields.hasNext()) {
			Element field = fields.next();
			String fieldname = field.getName();
			String fieldvalue = field.getText();
			// fields that start with "sys_" are discarded because
			// they cannot be inserted or updated
			if (fieldname.startsWith("sys_")) continue;
			element.addContent(createXmlText(fieldname, fieldvalue));
		}
		return element;
	}
	
	private Element createXmlElement(String method, Key sysid, FieldValues fields) {
		Element element = new Element(method, getNamespace());
		if (sysid != null)
			element.addContent(createXmlText("sys_id", sysid.toString()));
		Set<String> keys = fields.keySet();
		Iterator<String> iter = keys.iterator();
		while (iter.hasNext()) {
			String fieldname = iter.next();
			Object fieldvalue = fields.get(fieldname);
			// For an explanation of the next statement, refer to
			// https://wiki.servicenow.com/index.php?title=Setting_a_GlideRecord_Variable_to_Null
			if (fieldvalue == null) fieldvalue = "NULL";
			element.addContent(createXmlText(fieldname, fieldvalue.toString()));
		}
		return element;
	}

	private Element createXmlElement(String method, Parameters params) {
		Element element = new Element(method, getNamespace());
		Set<String> keys = params.keySet();
		Iterator<String> iter = keys.iterator();
		while (iter.hasNext()) {
			String name = iter.next();
			String value = params.get(name);
			element.addContent(createXmlText(name, value));
		}
		return element;
	}

	/**
	 * Validates a list of field values to ensure that 
	 * all the field names are spelled correctly.
	 * An exception is thrown if any field name
	 * is not found in the dictionary.
	 * @throws InvalidFieldNameException
	 * Thrown if any field name is not found in the dictionary.
	 * @see TableSchema
	 */
	@Deprecated
	public void validateFields(FieldValues values) 
			throws InvalidFieldNameException {
		Set<String> keys = values.keySet();
		Iterator<String> iter = keys.iterator();
		while (iter.hasNext()) {
			String fieldname = iter.next();
			requestlog.trace("validate field " + fieldname);
			if (!wsdl.canWriteField(fieldname))
				throw new InvalidFieldNameException(fieldname);
		}		
	}
	
	private InsertResponse _insert(Element method) 
			throws IOException, SoapResponseException {
		Element responseElement = proxy.callSoap(method, "insertResponse");
		return new InsertResponse(responseElement);
	}
	
	/**
	 * This insert method is only used when copying data.
	 * It requires as input a Record, and the only
	 * proper way to obtain a Record is to read it.
	 * Columns names beginning with "sys_" will not be inserted.
	 * <p/>
	 * The recommended and safer way to insert records is to use the
	 * {@link #insert(FieldValues) insert()} method. 
	 */
	public InsertResponse insert(Record rec) 
			throws IOException, SoapResponseException {
		Element method = createXmlElement("insert", null, rec);
		InsertResponse response = _insert(method);
		Key sysid = response.getSysId();
		responselog.info("insert sys_id=" + sysid);
		if (sysid == null || sysid.equals(""))
			throw new IllegalArgumentException("missing sys_id");
		return response;
	}

	/**
	 * This is the normal method used to insert a new record.
	 * It requires as input an initialized <b>FieldValues</b> object.
	 * <p/>
	 * Here are two equivalent techniques.
	 * <pre>
	 * table.insert(new FieldValues().
	 *   set("category", "network").
	 *   set("short_description", "a network error has occurred"));
	 * </pre>
	 * 
	 * @param fields FieldValues object containing the values to be inserted.
	 */
	public InsertResponse insert(FieldValues fields)
			throws IOException, SoapResponseException {		
		if (validate) validateFields(fields);
		Element method = createXmlElement("insert", null, fields);
		InsertResponse response = _insert(method);
		responselog.info("insert sys_id=" + response.getSysId());
		return response;
	}
	
	public InsertMultipleResponse insertMultiple(List<FieldValues> records) 
			throws IOException, SoapResponseException {
		Element insertMultiple = new Element("insertMultiple", getNamespace());
		for (FieldValues values : records) {
			Element record = new Element("record", getNamespace());
			for (String fieldname : values.keySet()) {
				Object fieldvalue = values.get(fieldname);
				// For an explanation of the next statement, refer to
				// https://wiki.servicenow.com/index.php?title=Setting_a_GlideRecord_Variable_to_Null
				if (fieldvalue == null) fieldvalue = "NULL";
				record.addContent(createXmlText(fieldname, fieldvalue.toString()));
			}
			insertMultiple.addContent(record);
		}
		Element responseElement = proxy.callSoap(insertMultiple, "insertMultipleResponse");
		InsertMultipleResponse responses = new InsertMultipleResponse();
		Iterator<Element> children = responseElement.getChildren("insertResponse").iterator();
		while (children.hasNext()) {
			Element child = children.next();
			responses.add(new InsertResponse(child));
		}
		return responses;
	}
	
	/**
	 * Delete a record from the table.
	 * 
	 * @param sysid sys_id of the record to be deleted.
	 * @return true if the record is deleted, false if the record is not found.
	 */
	public boolean deleteRecord(Key sysid) 
			throws IOException, SoapResponseException {		
		Element element = new Element("deleteRecord", getNamespace());
		element.addContent(new Element("sys_id").setText(sysid.toString()));
		Element deleteResponse = proxy.callSoap(element, "deleteRecordResponse");
		String count = deleteResponse.getChildText("count");
		responselog.info("deleteRecord count=" + count + " sys_id=" + sysid);
		boolean result = Integer.parseInt(count) > 0;
		return result;
	}

	private Key _update(Element method)
			throws IOException, SoapResponseException {
		Element insertResponse = proxy.callSoap(method, "updateResponse");
		String sysid = insertResponse.getChildText("sys_id");
		responselog.info("update sys_id=" + sysid);
		if (sysid == null || sysid.equals(""))
			throw new IllegalArgumentException("missing sys_id");
		return new Key(sysid);				
	}
	
	public Key update(Record rec) 
			throws IOException, SoapResponseException {
		Key key = rec.getKey();
		if (key == null) throw new NullPointerException("sys_id");
		Element method = createXmlElement("update", key, rec);
		return _update(method);
	}
	
	public Key update(Key sysid, FieldValues fields)
			throws IOException, SoapResponseException {
		if (sysid == null) throw new NullPointerException("sys_id");
		if (validate) validateFields(fields);
		Element method = createXmlElement("update", sysid, fields);
		return _update(method);
	}

	/**
	 * Retrieves a single record based on a unique field such as "name" or "number".  
	 * This method should be used in cases where the field value is known to be unique.
	 * If no qualifying records are found this function will return null.
	 * If one qualifying record is found it will be returned.
	 * If multiple qualifying records are found this method 
	 * will throw an IndexOutOfBoundsException.
	 * <pre>
	 * {@link Record} grouprec = session.table("sys_user_group").get("name", "Network Support");
	 * </pre>
	 * 
	 * @param fieldname Field name, e.g. "number" or "name"
	 * @param fieldvalue Field value
	 * @throws IndexOutOfBoundsException Thrown if more than one record matches
	 * the fieldvalue.
	 */
	public Record get(String fieldname, String fieldvalue)
			throws IOException, SoapResponseException {
		Parameters params = new Parameters(fieldname, fieldvalue);
		RecordList result = getRecords(params);
		int size = result.size();
		String msg = 
			"get " + fieldname + "=" + fieldvalue +	" returned " + size + " records";
		responselog.info(msg);
		if (size == 0) return null;
		if (size > 1) throw new IndexOutOfBoundsException(msg);
		return result.get(0);
	}

	/**
	 * Return a single record based on a sys_id.  If no qualifying record
	 * is found then null is returned. 
	 * <p/>
	 * <code>
	 * {@link Record} rec = session.table("sys_user_group").get(new RecordKey("287ebd7da9fe198100f92cc8d1d2154e"));
	 * </code>
	 * @param sysid Key to uniquely identify this record.
	 * @return Retrieved copy of the record.
	 */
	public Record get(Key sysid) 
			throws IOException, SoapResponseException {
		String value = sysid.toString();
		if (value == null || value.length() == 0) 
			throw new IllegalArgumentException("missing sys_id");
		Element field = new Element("sys_id").setText(sysid.toString());
		Element method = new Element("get", nsTNS).addContent(field);
		Element responseElement = proxy.callSoap(method, "getResponse");
		boolean success = (responseElement.getContentSize() > 0);
		if (responselog.isInfoEnabled())
			responselog.info(
				"get " + (success ? "OK" : "FAILED") + " " + sysid);
		return success ? new Record(this, responseElement) : null;		
	}

	/**
	 * Return true if records can be read from a table.
	 * The ServiceNow Web Services API does not provide a good method to
	 * determine if a table is readable.  There is no way to distinguish
	 * a table which has been blocked by Access Controls from a table which is empty.
	 * This method attempts to read a single record from the table.
	 * If no records are returned, then it assumes that the table is not readable.
	 * @return true if at least one record can be read from this table, otherwise false
	 */
	public boolean isReadable() throws IOException {
		if (this.readable == null) {
			Parameters params = new Parameters();
			params.add("__limit", "1");
			RecordList recs;
			try {
				recs = getRecords(params);
				if (responselog.isDebugEnabled()) 
					responselog.debug("Table.readable size=" + recs.size());
				this.readable = new Boolean(recs.size() > 0);
			} catch (SoapResponseException e) {
				this.readable = false;
				if (responselog.isDebugEnabled())
					responselog.debug("Table.readable exception", e);
			}
		}
		return this.readable;
	}

	/**
	 * This method is intended for internal use.
	 * It calls the SOAP getRecords method
	 * and allows specification of a list of
	 * parameters and extended query parameters.
	 * @see <a href="http://wiki.servicenow.com/index.php?title=Direct_Web_Service_API_Functions#getRecords">http://wiki.servicenow.com/index.php?title=Direct_Web_Service_API_Functions#getRecords</a>
	 */
	RecordList getRecords(Parameters params) 
			throws IOException, SoapResponseException {
		Element method = createXmlElement("getRecords", params);
		Element responseElement = proxy.callSoap(method, "getRecordsResponse");
		int size = responseElement.getContentSize();
		RecordList list = new RecordList(size);
		Iterator<Element> iter = responseElement.getChildren().iterator();
		while (iter.hasNext()) {
			Record rec = new Record(this, iter.next());
			list.add(rec);
		}
		return list;		
	}
	
	/**
	 * Retrieve a list of records matching an encoded query.
	 * <p/>
	 * This method should only be used if it is known that
	 * the number of records is small and there are no
	 * access controls which could block some of the
	 * records from being read.
	 * Otherwise use {@link Table#reader(QueryFilter)}.
	 */
	public RecordList getRecords(QueryFilter filter)
			throws IOException, InterruptedException {		
		PetitTableReader reader = new PetitTableReader(this, filter);
		RecordList result = reader.getAllRecords();
		if (responselog.isInfoEnabled())
			responselog.info("getRecords returned " + result.size() + " records");
		return result;
	}

	/**
	 * Returns a list of records matching a name/value pair.
	 * If no matching records are found then an empty {@link RecordList}
	 * is returned.
	 * 
	 * This method should only be used if it is known that  
	 * the result set will be fewer than 250 records.
	 * 
	 * For performance reasons ServiceNow limits the number of records
	 * that can be retrieved in a single SOAP query to 250
	 * and it does not provide a warning if the returned
	 * result set is incomplete.
	 */
	public RecordList getRecords(String fieldname, String fieldvalue)
			throws IOException, SoapResponseException {
		Parameters params = new Parameters(fieldname, fieldvalue);
		RecordList result = getRecords(params);
		if (responselog.isInfoEnabled())
			responselog.info(
				"getRecords " + fieldname + "=" + fieldvalue +
				" returned " + result.size() + " records");
		return result;
	}

	/**
	 * Returns a list of records matching a slice of a {@link KeyList}.
	 * 
	 * This method is called by {@link TableReader}.
	 */
	public RecordList getRecords(KeyList list, int fromIndex, int toIndex) 
			throws IOException, SoapResponseException {
		RecordList result = new RecordList(list.size());
		while (fromIndex < toIndex) {
			int tempIndex = fromIndex + this.chunkSize;
			if (tempIndex > toIndex) tempIndex = toIndex;
			QueryFilter filter = list.filter(fromIndex, tempIndex);
			String queryStr = filter.toString();
			requestlog.debug(queryStr);
			Parameters params = new Parameters("__encoded_query", queryStr);
			result.addAll(getRecords(params));
			fromIndex = tempIndex;
		}
		return result;		
	}

	/**
	 * Return a list of all the keys in the table
	 * with extended query parameters. 
	 * 
	 * This method is called by {@link KeyReader}.
	 */
	public KeyList getKeys(Parameters params) 
			throws IOException, SoapResponseException {
		Element method = createXmlElement("getKeys", params);
		Element responseElement = proxy.callSoap(method, "getKeysResponse");
		int size = Integer.parseInt(responseElement.getChildText("count"));
		responselog.trace("getKeys returned " + size + " keys");
		KeyList result = new KeyList();
		if (size > 0) {
			result.ensureCapacity(size);
			String listStr = responseElement.getChildText("sys_id");
			String list[] = listStr.split(",");
			if (list.length != size)
				throw new SoapResponseException(this, 
					"getKeys expected: " + size + ", found=" + list.length + "\n" +
					XMLFormatter.format(responseElement));
			for (int i = 0; i < list.length; ++i) {
				result.add(new Key(list[i]));
			}
		}
		return result;		
	}

	/**
	 * Return a complete list of all the keys in the table.
	 */
	public KeyList getKeys() throws IOException, InterruptedException {
		return reader().getKeys();
	}
	
	public LinkedHashMap<String,String> getAggregate(
			String aggName, String field, String groupBy, QueryFilter filter)
			throws SoapResponseException, IOException {
		LinkedHashMap<String,String> result = new LinkedHashMap<String,String>();
		assert aggName != null;
		assert aggName.length() > 0;		
		assert "COUNT".equals(aggName) || "MIN".equals(aggName) || "MAX".equals(aggName) ||
				"AVG".equals(aggName) || "SUM".equals(aggName);
		assert field != null;
		assert field.length() > 0;
		Parameters params = new Parameters(aggName, field);
		if (groupBy != null) params.add("GROUP_BY", groupBy);
		if (filter != null)	params.add(filter.asParameters());
		Element method = createXmlElement("aggregate", params);
		Element responseElement = proxy.callSoap(method, "aggregateResponse");
		List<Element> responseResult = responseElement.getChildren("aggregateResult");
		for (Element ele : responseResult) {
			String category = ele.getChildText(groupBy);
			String value = ele.getChildText(aggName);
			result.put(category, value);
		}
		return result;
	}
		
	public LinkedHashMap<String,Integer> getCount(String groupBy, QueryFilter filter) 
			throws SoapResponseException, IOException {
		LinkedHashMap<String,String> aggData = getAggregate("COUNT", "sys_id", groupBy, filter);
		LinkedHashMap<String,Integer> result = new LinkedHashMap<String,Integer>(aggData.size());
		Set<Entry<String, String>> rows = aggData.entrySet();
		for (Entry<String,String> row : rows) {
			Integer count = Integer.parseInt(row.getValue());
			result.put(row.getKey(), count);
		}
		return result;
	}

	public LinkedHashMap<String,Integer> getCount(String groupBy)
		throws SoapResponseException, IOException {
			QueryFilter noFilter = null;
			return getCount(groupBy, noFilter);
	}
	
	/**
	 * Returns the number of records matching a query filter.
	 * Requires the Aggregate Web Services plugin.
	 */
	public int getCount(QueryFilter filter) 
			throws SoapResponseException, IOException {
		Parameters params = new Parameters("COUNT", "sys_id");
		if (filter != null)	params.add(filter.asParameters());
		Element method = createXmlElement("aggregate", params);
		Element responseElement = proxy.callSoap(method, "aggregateResponse");
		Element responseResult = responseElement.getChild("aggregateResult");		
		String count = responseResult.getChildText("COUNT");
		if (count == null || count.length() == 0) 
			count = responseResult.getChildText("count");
		responselog.debug("COUNT=" + count);
		if (count == null || count.length() == 0)
			throw new SoapResponseException(this,
				"getCount: count=" + count + "\n" +
				XMLFormatter.format(responseElement));
		return Integer.parseInt(count);
	}

	/**
	 * Returns the total number of records in a table.
	 * Requires the Aggregate Web Services Plugin.
	 */
	public int getCount() 
			throws SoapResponseException, IOException {
		QueryFilter noFilter = null;
		return getCount(noFilter);
	}
	
	/**
	 * Returns a {@link TableReader} object which can be safely used to 
	 * read records. ServiceNow limits to 250 the number of records
	 * that can be returned in a single Web Service call.  
	 * The {@link TableReader} issues multiple Web Service calls
	 * to overcome this limitation.
	 * 
	 * @param query - a {@link QueryFilter} used to filter the {@link TableReader}
	 * @return a filtered {@link TableReader}
	 */
	public BasicTableReader reader(QueryFilter query) 
			throws IOException {
		return new BasicTableReader(this, query);
	}
	
	/**
	 * Returns a {@link TableReader} object which can be safely used to 
	 * read records.  ServiceNow limits to 250 the number of records
	 * that can be returned in a single Web Service call.  
	 * The {@link TableReader} issues multiple Web Service calls
	 * to overcome this limitation.
	 */
	public BasicTableReader reader() throws IOException {
		return new BasicTableReader(this);
	}

	/**
	 * Returns a {@link TableReader} which operates on a set of keys.
	 */
	public BasicTableReader reader(KeyList keys) throws IOException {
		return new BasicTableReader(this, keys);
	}

	/**
     * A {@link TableReader} which attempts to read a set of records
     * using a single Web Service call.
     * @see PetitTableReader
	 */	
	public PetitTableReader petitReader(QueryFilter query) throws IOException {
		return new PetitTableReader(this, query);
	}
	
	/**
     * A {@link TableReader} which attempts to read a set of records
     * using a single Web Service call.
     * @see PetitTableReader
	 */	
	public PetitTableReader petitReader() throws IOException {
		return new PetitTableReader(this);
	}
	
	/**
	 * Attach a file to a record in a table.
	 * 
	 * @param key The sys_id of the record to which the file is to be attached.
	 * @param file The file that is to be attached.
	 * @throws IOException
	 */
	public void attachFile(Key key, File file) 
			throws IOException, JDOMException, SoapResponseException {
		byte [] linebreak = { '\n' };
		Base64 base64 = new Base64(76, linebreak);		
		String filename = file.getName();
		FileNameMap fileNameMap = URLConnection.getFileNameMap();
		String filetype = fileNameMap.getContentTypeFor(filename);
		int filesize = (int) file.length();
		requestlog.info("attachFile" + 
			" sys_id=" + key.toString() +
			" name=" + filename + 
			" size=" + filesize + " type=" + filetype);
		byte [] binaryData = new byte[filesize];
		DataInputStream stream = new DataInputStream((new FileInputStream(file)));
		stream.readFully(binaryData);
		stream.close();
		String encodedData = base64.encodeAsString(binaryData);
		FieldValues message = new FieldValues();
		message.put("agent", session.getUserName());
		message.put("topic", "AttachmentCreator");
		message.put("name", filename + ":" + filetype);
		message.put("source", this.getName() + ":" + key.toString());
		message.put("payload", encodedData);
		session.eccQueue().insert(message);
	}
}

