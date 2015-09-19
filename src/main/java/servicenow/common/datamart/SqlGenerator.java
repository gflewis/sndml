package servicenow.common.datamart;

import java.io.*;
import java.util.*;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import servicenow.common.datamart.DatabaseWriter;
import servicenow.common.datamart.DatamartConfiguration;
import servicenow.common.datamart.LoggerFactory;
import servicenow.common.datamart.NameMap;
import servicenow.common.datamart.ResourceException;
import servicenow.common.datamart.ResourceManager;
import servicenow.common.datamart.SqlGenerator;
import servicenow.common.datamart.SuiteInitException;

import servicenow.common.soap.FieldDefinition;
import servicenow.common.soap.Table;
import servicenow.common.soap.TableSchema;
import servicenow.common.soap.TableWSDL;

/**
 * Generates SQL statements using templates from the <b>sqltemplates.xml</b> file.
 * @author Giles Lewis
 *
 */
public class SqlGenerator {
	
	static Logger logger = LoggerFactory.getLogger(SqlGenerator.class);
	
	private Document xmldocument;
	private String dialect;
	private Element tree;
	private final String namecase;
	private final String namequotes;
	private final String user;
	private final String schema;
	NameMap namemap = new NameMap();
		
	public SqlGenerator(DatamartConfiguration config, DatabaseWriter writer) 
			throws ResourceException, SQLException {
		this(config, writer.getConnection());
	}
	
	public SqlGenerator(DatamartConfiguration config, Connection connection)  
			throws ResourceException, SQLException {
		InputStream sqlConfigStream;		
		sqlConfigStream = 
			ClassLoader.getSystemResourceAsStream(
				config.getString("templates",  "sqltemplates.xml"));
		if (sqlConfigStream == null)
			throw new ResourceException("Unable to locate resource: sqltemplates.xml");
		SAXBuilder xmlbuilder = new SAXBuilder();
		try {
			xmldocument = xmlbuilder.build(sqlConfigStream);
		} catch (IOException e) {
			throw new ResourceException(e);
		} catch (JDOMException e) {
			throw new ResourceException(e);
		}
		setDialect(config.getString("dialect", "default"));
		user = config.getRequiredString("username");
		schema = config.getString("schema", "");
		namemap.loadFromXML(tree.getChild("fieldnames"));
		Element dialogProps = tree.getChild("properties");
		DatabaseMetaData meta = connection.getMetaData();
		
		String namecaseprop = dialogProps.getChildText("namecase");		
		if ("upper".equals(namecaseprop)) {
			namecase = "upper";
		}
		else if ("lower".equals(namecaseprop)) {
			namecase = "lower";
		}
		else if ("auto".equals(namecaseprop)) {
			if (meta.storesUpperCaseIdentifiers()) 
				namecase = "upper";
			else if (meta.storesLowerCaseIdentifiers()) 
				namecase = "lower";
			else 
				throw new ResourceException("could not determine namecase");				
		}
		else
			throw new ResourceException("namecase=" + namecaseprop);
		
		String namequotesprop = dialogProps.getChildText("namequotes");
		if ("none".equals(namequotesprop)) 
			namequotes = "none";
		else if ("double".equals(namequotesprop)) 
			namequotes = "double";
		else if ("square".equals(namequotesprop)) 
			namequotes = "square";
		else
			throw new ResourceException("namequotes=" + namequotesprop);
		logger.info("namecase=" + namecase + " namequotes=" + namequotes);
	}
	
	private void setDialect(String dialect) throws ResourceException {
		tree = null;
		ListIterator<Element> children = 
			xmldocument.getRootElement().getChildren().listIterator();
		while (tree == null && children.hasNext()) {
			Element child = children.next();
			if (child.getName().equals("sql")) {
				if (child.getAttributeValue("dialect").equals(dialect))
					tree = child;
			}
		}
		if (tree == null)
			throw new ResourceException(
				"No sql found for dialect='" + dialect +"'");
		this.dialect = dialect;
	}

	String getDialect() { return this.dialect; }
	
	List<String> getInitializations() {
		Map<String,String> myvars = new HashMap<String,String>();
		myvars.put("user", this.user);
		myvars.put("schema", this.schema);
		List<String> result = new ArrayList<String>();
		Element initialize = tree.getChild("initialize");
		ListIterator<Element> iter = initialize.getChildren("statement").listIterator();
		while (iter.hasNext()) {
			String stmt = iter.next().getTextTrim();
			stmt = replaceVars(stmt, myvars);
			result.add(stmt);
		}
		return result;
	}
	
	String getUser() { 
		return this.user; 
	}

	/**
	 * Return schema name as specified by property setting
	 * or "" if not specified.
	 */
	String getSchema() {
		return sqlCase(this.schema);
	}
	
	String sqlCase(String name) {
		if (namecase.equals("lower"))
			return name.toLowerCase();
		else
			return name.toUpperCase();
	}
	
	String sqlQuote(String name) {
		if (namequotes.equals("double")) 
			return "\"" + name + "\"";
		else if (namequotes.equals("square")) 
			return "[" + name + "]";
		else
			return name;
	}
	

	String sqlName(String name) {
		String lookup = namemap.getSqlName(name);
		return (lookup == null) ? sqlQuote(sqlCase(name)) : lookup;
	}
	
	/**
	 * Return a qualfied table name as schema.table
	 */
	String sqlTableName(String name) {
		String result = sqlCase(name);
		if (schema != null && schema.length() > 0) 
			result = schema + "." + result;
		return result;
	}
	
	String glideName(String name) {
		String result = namemap.getGlideName(name);
		return (result == null) ? name.toLowerCase() : result;
	}

	/**
	 * Return the SQL Type corresponding to a Glide Type
	 */
	String sqlType(String glidetype, int size) {
		String sqltype = null;
		ListIterator<Element> alltypes = tree.getChild("datatypes").
			getChildren("typemap").listIterator();
		while (sqltype == null && alltypes.hasNext()) {
			Element ele = alltypes.next();
			String attr = ele.getAttributeValue("glidetype");
			if (attr.equals(glidetype) || attr.equals("*")) {
				String minsize = ele.getAttributeValue("minsize");
				String maxsize = ele.getAttributeValue("maxsize");
				if (minsize != null && size < Integer.parseInt(minsize)) continue;
				if (maxsize != null && size > Integer.parseInt(maxsize)) continue;
				sqltype = ele.getTextTrim();
				if (sqltype.indexOf("#") > -1)
					sqltype = sqltype.replaceAll("#", Integer.toString(size));
			}
		}
		return sqltype;
	}
	
	String getTemplate(String templateName, String tableName) {
		return getTemplate(templateName, tableName, null);
	}
	
	String getTemplate(
			String templateName, 
			String tableName,
			Map<String,String> vars) {
		String sql = tree.getChild("templates").getChildText(templateName);
		Map<String,String> myvars = new HashMap<String,String>();
		myvars.put("user", this.user);
		myvars.put("schema", getSchema());
		myvars.put("table", sqlCase(tableName));
		myvars.put("keyvalue", "?");
		if (vars != null) myvars.putAll(vars);
		return replaceVars(sql, myvars);
	}

	
	String getCreateTable(
				Table table,
				String sqlTableName,
				boolean displayValues) throws SuiteInitException {
		// We may be pulling the schema from a different ServiceNow instance
		TableSchema tableSchema = ResourceManager.getTableSchema(table.getName());
		TableWSDL tableWSDL = table.getWSDL();
		final String fieldSeparator = ",\n";
		StringBuilder fieldlist = new StringBuilder();
		// Make sys_id the first field.
		// All tables have sys_id.
		fieldlist.append(sqlFieldDefinition(tableSchema.getFieldDefinition("sys_id")));
		Iterator<FieldDefinition> iter = tableSchema.iterator();
		while (iter.hasNext()) {
			FieldDefinition fd = iter.next();
			if (fd.getName().equals("sys_id")) {
				// sys_id already added above
				continue;
			}
			if (!tableWSDL.canReadField(fd.getName())) {
				// it is in the dictionary, but not the WSDL
				// it could be blocked by an access control
				// skip it
				continue;
			}
			fieldlist.append(fieldSeparator);
			fieldlist.append(sqlFieldDefinition(fd));
			if (displayValues && fd.isReference()) {
				FieldDefinition fddv = fd.displayValueDefinition();
				fieldlist.append(fieldSeparator);
				fieldlist.append(sqlFieldDefinition(fddv));
			}
		}
		HashMap<String,String> map = new HashMap<String,String>();
		map.put("fielddefinitions", fieldlist.toString());		
		String result = getTemplate("create", sqlTableName, map);
		return result;
	}

	String sqlFieldDefinition(FieldDefinition fd) {
		String fieldname = fd.getName();
		String fieldtype = fd.getType();
		assert fieldname != null;
		assert fieldtype != null;
		int size = fd.getLength();
		String sqlname = sqlName(fieldname);
		String sqltype = sqlType(fieldtype, size);		
		logger.trace(fieldname + " " + fieldtype + "->" + sqltype);
		if (sqltype == null)  {
			String descr = sqlname + " " + fieldtype + "(" + size + ")";
			logger.warn("Field type not mapped " + descr);
			return "-- " + descr;
		}
		String result = sqlname + " " + sqltype;
		return result;
	}
		
	private String replaceVars(
		String sql, Map<String,String> vars) {
		Iterator<Map.Entry<String,String>> iterator = vars.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String,String> entry = iterator.next();
			String name = entry.getKey();
			String value = entry.getValue();
			if (value == null || value.length() == 0) {				
				// if the variable (e.g. $schema) is null or zero length
				// then also consume a period following the variable
				// i.e. "insert into $schema.$table" becomes "insert into $table"
				sql = sql.replaceAll("\\$" + name + "\\.", "");
				sql = sql.replaceAll("\\$" + name + "\\b", "");
				sql = sql.replaceAll("\\$\\{" + name + "\\}\\.?", "");
			}
			else {
				sql = sql.replaceAll("\\$" + name + "\\b", value);
				sql = sql.replaceAll("\\$\\{" + name + "\\}", value);
			}
		}
		return sql;		
	}

}
