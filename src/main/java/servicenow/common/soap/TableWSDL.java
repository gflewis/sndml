package servicenow.common.soap;

import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;

import servicenow.common.soap.HttpUtil;
import servicenow.common.soap.InsufficientRightsException;
import servicenow.common.soap.InvalidTableNameException;
import servicenow.common.soap.Session;
import servicenow.common.soap.TableWSDL;
import servicenow.common.soap.WSDLException;

/**
 * Obtain and parse the WSDL for a table.
 */
public class TableWSDL {
	
	final private Document doc;
	final List<String> readColumnNames;
	final List<String> writeColumnNames;
	final Map<String,String> readColumnTypes;
	final Map<String,String> writeColumnTypes;
	
	final static Logger logger = Session.getLogger(TableWSDL.class);
	
	static Namespace nsWSDL = Namespace.getNamespace(
		"wsdl", "http://schemas.xmlsoap.org/wsdl/");
	static Namespace nsXSD = Namespace.getNamespace(
		"xsd", "http://www.w3.org/2001/XMLSchema");
	
	public TableWSDL(Session session, String tablename) 
			throws IOException, InvalidTableNameException {
		this(session, tablename, false);
	}
	
	public TableWSDL(Session session, String tablename, boolean displayValues) 
			throws IOException, InvalidTableNameException {
		logger.debug("table=" + tablename);
    	ArrayList<String> params = new ArrayList<String>(2);
    	params.add("WSDL");
    	if (displayValues) params.add("displayvalue=all");
		HttpURLConnection connection = session.openConnection(tablename,  params);
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Accept-Encoding", "gzip");
		connection.setDoInput(true);
		connection.setDoOutput(false);
		connection.connect();
		if (logger.isDebugEnabled()) {
			logger.debug(HttpUtil.getAllHeaders(connection));
		}				
		int responseCode = connection.getResponseCode();
		String contentType = connection.getContentType();
		if (responseCode != HttpURLConnection.HTTP_OK)  {
			if (responseCode == HttpURLConnection.HTTP_FORBIDDEN ||
					responseCode == HttpURLConnection.HTTP_UNAUTHORIZED)
				throw new InsufficientRightsException();
			else
				throw new IOException();
		}
		String responseBuffer = HttpUtil.readFully(connection);
		if (!"text/xml".equals(contentType)) {
			logger.debug(HttpUtil.getAllHeaders(connection));
			logger.debug("responseBuffer=" + responseBuffer);
			if (responseBuffer.contains("WSDLGeneratorException") || 
					responseBuffer.contains("Cannot generate WSDL"))
				throw new InvalidTableNameException(tablename);
			else
				throw new AssertionError("Invalid content type: " + contentType);
		}
		SAXBuilder parser = new SAXBuilder();
		try {
			doc = parser.build(new StringReader(responseBuffer));
		} catch (JDOMException e) {
			throw new WSDLException(e);
		}		
		readColumnNames = getColumnNames("getResponse");
		readColumnTypes = getColumnTypes("getResponse");
		writeColumnNames = getColumnNames("update");
		writeColumnTypes = getColumnTypes("update");		
	}

	private Element getTypeDefinition(String typeName) throws WSDLException {
		List<Element> eleElements = doc.getRootElement()
			.getChild("types", nsWSDL)
			.getChild("schema", nsXSD)
			.getChildren("element", nsXSD);
		Element getResponse = null;
		for (Element ele : eleElements) {
			String name = ele.getAttributeValue("name");
			if (name.equals(typeName)) getResponse = ele; 
		}
		if (getResponse == null)
			throw new WSDLException("Could not find \"" + typeName + "\" in WSDL");
		Element complexType = getResponse.getChild("complexType", nsXSD);
		Element sequence = complexType.getChild("sequence", nsXSD);
		return sequence;
	}

	public List<String> getColumnNames(String typeName) throws WSDLException {
		Element schema = getTypeDefinition(typeName);
		List<Element> children = schema.getChildren("element", nsXSD);
		List<String> list = new ArrayList<String>(children.size());
		for (Element child : children) {
			list.add(child.getAttributeValue("name"));
		}
		return list;		
	}
	
	public Map<String,String> getColumnTypes(String typeName) throws WSDLException {
		Element schema = getTypeDefinition(typeName);
		List<Element> children = schema.getChildren("element", nsXSD);
		HashMap<String,String> map = new HashMap<String,String>(children.size());
		for (Element child : children) {
			String name = child.getAttributeValue("name");
			String type = child.getAttributeValue("type");
			type.replaceFirst("xsd:", "");
			map.put(name,  type);
		}
		return map;		
	}
	
	/**
	 * Return a list of all the columns in the table.
	 * @throws WSDLException 
	 */
	public List<String> getReadColumnNames() {
		return readColumnNames;
	}
	
	public List<String> getWriteColumnNames() {
		return writeColumnNames;
	}
		
	
	public boolean canReadField(String fieldname) {
		return readColumnTypes.get(fieldname) != null;
	}
	
	public boolean canWriteField(String fieldname) {
		return writeColumnTypes.get(fieldname) != null;
	}
	
	
	/*
	 * Return true if the table has a field with this name; otherwise false.
	 * @param fieldname Column name.
	 * @param displayValues True if DisplayValues are enabled for this table.
	 * @return
	public boolean hasField(String fieldname, boolean displayValues) {
		Map<String,String> fields = getColumnTypes();
		if (fields.get(fieldname) != null) return true;
		if (displayValues && fieldname.startsWith("dv_")) {
			String refname = fieldname.substring(3);
			return fields.get(refname) != null;
		}
		return false;
	}
	 */

}
