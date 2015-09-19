package servicenow.common.soap;

import java.io.*;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.jdom2.JDOMException;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import servicenow.common.soap.ServiceNowException;
import servicenow.common.soap.Session;

@RunWith(Suite.class)
@SuiteClasses({
	TableConfigurationTest.class,
	DateTimeTest.class,
	SessionTest.class,
	UpdateDurationTest.class,
	AggregateTest.class,
	AttachmentTest.class,
	TableReaderTest.class,
	DisplayValueTest.class,
	TableTest.class,
	ParametersTest.class,
	PropertiesTest.class,
	SessionCookieTest.class,
	ValidateTest.class,
	InsertMultipleTest.class,
	KeyListTest.class,
	CompressionTest.class,
	ThreadTest.class
})

public class AllTests {
	
	static Session session = null;
	static Properties properties = null;
	static String propFileName = "mysql.junit.properties";
	static Logger logger = junitLogger(AllTests.class);
	
	public static Logger junitLogger(@SuppressWarnings("rawtypes") Class c) {
		return LoggerFactory.getLogger("junit." + c.getSimpleName());
	}

	public static Properties getProperties() throws IOException {
		if (properties != null) return properties;
		logger.info("getProperties loading " + propFileName);
		properties = new Properties();
		InputStream stream = ClassLoader.getSystemResourceAsStream(propFileName);
		if (stream == null)
			throw new IOException("Unable to load " + propFileName);
		properties.load(stream);
		return properties;
	}

	public static void printFile(String filename) throws IOException {
		logger.info("printFile: " + filename);
		InputStream stream = ClassLoader.getSystemResourceAsStream(propFileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		while (reader.ready()) {
			String line = reader.readLine();
			logger.info(line);
		}		
	}
	
	public static Session getSession() 
			throws FileNotFoundException, IOException, 
			ServiceNowException, JDOMException {
		if (session != null) return session;		
		session = new Session(getProperties());
		return session;
	}
	
	public static String getProperty(String name) {
		String propname = "junit." + name;
		String value = properties.getProperty(propname);
		if (value == null) throw new IllegalArgumentException(propname);
		return value;
	}

	public static String someIncidentNumber() {
		return getProperty("some_incident_number");
	}
	
	public static String someGroup() {
		return getProperty("some_group_name");
	}
	
	public static String someGroupManger() {
		return getProperty("some_group_manager");
	}
	
}
