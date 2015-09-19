package servicenow.common.soap;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.apache.commons.codec.binary.Base64;
import org.jdom2.JDOMException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import servicenow.common.soap.CookieJar;
import servicenow.common.soap.InsufficientRightsException;
import servicenow.common.soap.InvalidTableNameException;
import servicenow.common.soap.Key;
import servicenow.common.soap.Parameters;
import servicenow.common.soap.Record;
import servicenow.common.soap.RecordList;
import servicenow.common.soap.Session;
import servicenow.common.soap.SessionConfiguration;
import servicenow.common.soap.SessionMetrics;
import servicenow.common.soap.Table;
import servicenow.common.soap.TableSchema;

/**
 * This is the main class in the package.  It holds three things:
 * <ul>
 * <li>credentials (url, username, password) used to connect to the instance</li>
 * <li>session cookie (JSESSIONID)</li>
 * <li>cache of schema information for accessed tables
 * (list of {@link TableSchema} objects)</li>
 * </ul>
 * 
 * This class requires read access to <b>sys_dictionary</b> and <b>sys_db_object</b>
 * in order to retrieve schema information.
 * These two tables are read by the class constructor and by the 
 * {@link Session#table(String)} method.
 * An exception will be thrown if either of these tables cannot be read.
 * 
 * @author Giles Lewis
 *
 */
public class Session {
	
	private final String baseurl;
	private final String username;
	private final String authorization;
	private final SessionConfiguration config;
	private final CookieJar cookiejar;
	private final Record userProfile;
	final boolean validate;
	// final Table hierarchy; // sys_db_object
	private Table sys_dictionary;
	private Table sys_properties;
	private Table ecc_queue;
	// boolean initialized = false;
	
	Hashtable<String,Table> tables = new Hashtable<String,Table>();
	SessionMetrics metrics = new SessionMetrics();

    final static String loggerPrefix = "ServiceNow.";
    // final static String propertyPrefix = "servicenow.";
        
    final static Logger logger = Session.getLogger(Session.class);

    /**
     * Factory method used by other classes in this package
     * to obtain a Log4J Logger.
     */
    protected static Logger getLogger(String category) {
    	return LoggerFactory.getLogger(loggerPrefix + category);
    }
    
    /**
     * Factory method used by other classes in this package
     * to obtain a Log4J Logger.
     */
    protected static Logger getLogger(@SuppressWarnings("rawtypes") Class c) {
    	return getLogger(c.getSimpleName());
    }
            
    /**
     * Establish a connection to a ServiceNow instance
     * using a URL, a username and a password.
     * A Properties object may be used to specify additional values.
     * 
     * @param url - E.g. //https://demo008.service-now.com/
     * @param username - ServiceNow user name
     * @param password - ServiceNow password
     * @param validate - Set to false to disable field name validations
     * @param config - Allows specification of additional properties
     * @throws InsufficientRightsException Thrown if password is bad
     * or unable to read sys_user table
     */
	public Session(String url, String username, String password, 
				Boolean validate, SessionConfiguration config) 
			throws InsufficientRightsException, IOException, JDOMException {
		this.config = config;
		if (logger.isTraceEnabled()) config.logTrace(logger);
		if (url == null) url = config.getString("url", null);
		if (username == null) username = config.getString("username", null);
		if (password == null) password = config.getString("password", null);
		if (validate == null) validate = config.getBoolean("validate", true);
		if (url == null) 
			throw new IllegalArgumentException("URL not specified");
		if (username == null) 
			throw new IllegalArgumentException("username not specified");
		this.cookiejar = new CookieJar();
		this.baseurl = (url.endsWith("/") ? url : url + "/");
		this.username = username;
		String userpassword = username + ":" + password;
		authorization = Base64.encodeBase64String(userpassword.getBytes());		
		this.validate = validate;
		logger.info("url=" + baseurl + " username=" + username);
		if (!validate) logger.info("schema will not be loaded since validation disabled");
		// The following statement will throw InsufficientRightsException
		// if password is bad or if unable to read sys_user
		userProfile = table("sys_user").get("user_name", username);
		assert userProfile != null;
		logger.info(username + " timezone=" + getTimeZone() + " sessionid=" + getSessionID());
		if (!"GMT".equals(getTimeZone())) {
			logger.warn("user \" + username + \" time zone is not GMT");
		}
	}

	public Session(String url, String username, String password, 
			Boolean validate, Properties props) 
		throws InsufficientRightsException, IOException, JDOMException {
			this(url, username, password, validate, new SessionConfiguration(props));
	}
	
	public Session(String url, String username, String password, Properties props) 
			throws InsufficientRightsException, IOException, JDOMException {
		this(url, username, password, null, new SessionConfiguration(props));
	}

	public Session(String url, String username, String password, boolean validate) 
			throws InsufficientRightsException, IOException, JDOMException {
		this(url, username, password, validate, new SessionConfiguration());		
	}
	
    /**
     * Establish a connection to a ServiceNow instance
     * using a URL, a username and a password.
     * 
     * @param url - E.g. //https://demo008.service-now.com/
     * @param username - ServiceNow user name
     * @param password - ServiceNow password
     * @throws InsufficientRightsException Thrown if password is bad
     * or unable to read sys_user table
     */
	public Session(String url, String username, String password)
			throws InsufficientRightsException,	IOException, JDOMException {
		this(url, username, password, null, new SessionConfiguration());
	}
	
	/**
	 * Connect to a ServiceNow instance using values from a 
	 * Properties object.  The following properties must be defined:
	 * <ul>
	 * <li>servicenow.url</li>
	 * <li>servicenow.username</li>
	 * <li>servicenow.password</li>
	 * </ul>
	 * @param props
     * @throws InsufficientRightsException Thrown if password is bad
     * or unable to read sys_user table
	 */
	public Session(Properties props) 
			throws InsufficientRightsException, IOException, JDOMException {
		this(null, null, null, null, new SessionConfiguration(props));
	}
	
	public Session(SessionConfiguration config) 
			throws InsufficientRightsException, IOException, JDOMException {
		this(null, null, null, null, config);
	}

	/**
	 * Return the URL of a table
	 */
	public URL getURL(String tablename, String params) throws MalformedURLException {
		return new URL(baseurl + tablename + ".do?" + params);
	}
	
	public URL getURL(String tablename, String protocol, String param)
		throws MalformedURLException {
    	ArrayList<String> params = new ArrayList<String>(2);
    	params.add(protocol);
    	if (param != null) params.add(param);
    	return getURL(tablename, params);		
	}
	
	/**
	 * Return the URL of a table
	 */
	public URL getURL(String tablename, List<String> params) throws MalformedURLException {
		StringBuilder url = new StringBuilder(baseurl + tablename + ".do");
		if (params != null) {
			for (int i = 0; i < params.size(); ++i) {
				url.append(i == 0 ? "?" : "&");
				url.append(params.get(i));
			}
		}
		return new URL(url.toString());
	}
	
	/**
	 * Add username and password to an HttpURLConnection.
	 */
	void authorize(HttpURLConnection connection) {
		connection.setRequestProperty("Authorization", "Basic " + authorization);		
	}

	/**
	 * Create a new HttpURLConnection with authentication and JSESSIONID
	 */
	HttpURLConnection openConnection(URL url) 
			throws IOException {
		logger.debug("openConnection " + url.toString());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestProperty("Content-Type", "text/xml");
		authorize(connection);
		cookiejar.setRequestCookies(connection);
		return connection;
	}
	
	/**
	 * Create a new HttpURLConnection with authentication and JSESSIONID
	 */
	HttpURLConnection openConnection(String tablename, String params) 
			throws IOException {
		return openConnection(getURL(tablename, params));
	}

	/**
	 * Create a new HttpURLConnection with authentication and JSESSIONID
	 */
	HttpURLConnection openConnection(String tablename, List<String> params) 
			throws IOException {
		return openConnection(getURL(tablename, params));
	}

	/**
	 * Update the JSESSIONID cookie
	 */
	synchronized void updateSessionInfo(HttpURLConnection connection) {
		cookiejar.addResponseCookies(connection, false);	
	}
	
	synchronized String getSessionID() {
		return cookiejar.getCookie("JSESSIONID");
	}
	
	public String getUserName() { return username; }
	SessionConfiguration getConfiguration() { return this.config; }
	
	synchronized Record getUserProfile() { 
		return this.userProfile; 
	}
	
	synchronized String getSystemProperty(String name) 
			throws IOException, JDOMException {
		if (sys_properties == null)
			sys_properties = table("sys_properties");
		Record rec = sys_properties.get("name", name);
		return (rec == null) ? null : rec.getField("value");
	}
	
	public synchronized String getTimeZone() 
			throws IOException, JDOMException {
		String result;
		Record userProfile = getUserProfile();
		result = userProfile.getField("time_zone");
		if (result == null) {
			result = getSystemProperty("glide.sys.default.tz");
		}
		return result;
	}
	
	/**
	 * Returns a {@link Table} object which can be used for 
	 * get, insert, update and delete operations.
	 * <p/>
	 * This object is locally cached.
	 * If you make a subsequent call to this method
	 * with the same table name you will receive a receive a reference 
	 * to the previously created object.
	 * Since v1.7.9 this method retrieves and stores the table WSDL
	 * for use in validations.
	 * The full schema will be retrieved from sys_dictionary 
	 * only if you call {@link Table#getSchema()}.
	 * 	 
	 * @param tablename 
	 * The internal name of the table (not the label that
	 * appears on the forms).  The table name is in all lower case letters
	 * and may contain underscores.
	 * @return 
	 * {@link Table} object
	 * @throws InvalidTableNameException 
	 * No table with this name was found in sys_dictionary. 
	 */
	public synchronized Table table(String tablename) 
			throws IOException, JDOMException, InvalidTableNameException {
		assert tablename != null && tablename.length() > 0;
		Table result = tables.get(tablename);
		if (result == null) {
			result = new Table(this, tablename);
			tables.put(tablename, result);
		}
		result.requestlog.debug(
			"url=" + result.proxy.url.toString() +
			" validate=" + result.validate);	    
		return result;
	}

	/** 
	 * Returns true if there is a table with this name; otherwise false.
	 */
	public boolean tableExists(String tablename) 
			throws IOException, JDOMException, InsufficientRightsException {
		if (sys_dictionary == null) 
			sys_dictionary = table("sys_dictionary");
		if (!sys_dictionary.isReadable())
			throw new InsufficientRightsException(
				sys_dictionary, "getRecords", "tableExists method failed");
		Parameters params = new Parameters();
		params.add("name", tablename);
		params.add("element", "");
		RecordList dictRecs = sys_dictionary.getRecords(params);
		return (dictRecs.size() == 1);
	}
	/**
	 * Returns a reference to the ecc_queue table.
	 * This method is used by 
	 * {@link Table#attachFile(Key, java.io.File)}.
	 */
	public synchronized Table eccQueue() throws IOException, JDOMException {
		if (this.ecc_queue == null) {
			this.ecc_queue = this.table("ecc_queue");
		}
		return this.ecc_queue;
	}
}
