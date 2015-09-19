package servicenow.common.datamart;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.jdom2.JDOMException;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import servicenow.common.datamart.CommandScript;
import servicenow.common.datamart.DatabaseWriter;
import servicenow.common.datamart.DatamartConfiguration;
import servicenow.common.datamart.EphemeralSuite;
import servicenow.common.datamart.Loader;
import servicenow.common.datamart.PersistentSuite;
import servicenow.common.datamart.SuiteModel;

import servicenow.common.soap.DateTime;
import servicenow.common.soap.Session;

@RunWith(Suite.class)
@SuiteClasses({
	ParserTest.class,
	JunitDetermineLag.class,
	SessionTest.class,
	JobOrderTest.class,
	GetJobByNameTest.class,
	DB.class,
	SimpleLoadTest.class,
	JunitStartSuite.class,
	JunitSuiteStatusTest.class,
	BatchInsertTest.class,
	ReservedWordsTest.class,
	PrimaryKeyTest.class,
	JunitStartRunning.class,
	LoaderOptionsTest.class,
	JobModelTest.class,
	JobScriptsTest.class,
	JunitSuiteLoad.class,
	NextRunTest.class,	
	SqlGeneratorTest.class,
	PruneTest.class,
	PollingTestBack.class,
	PollingTestNow.class,
	JunitPollingRunBack.class,
	JunitPollingRunNow.class,
})

public class AllTests {

	static String propFileName = "mysql.junit.properties";
	
	private static Properties properties = null;
	private static Session session = null;
	private static DatabaseWriter dbwriter = null;
	private static Logger logger = getLogger(AllTests.class);
	
	public static void initialize() throws IOException {
		getConfiguration();
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
		
	public static DatamartConfiguration getConfiguration() throws IOException {
		Properties props = getProperties();
		DatamartConfiguration.setProperties(props);
		return DatamartConfiguration.getDatamartConfiguration();
	}
		
	public static Logger getLogger(@SuppressWarnings("rawtypes") Class c) {
		return LoggerFactory.getLogger("junit." + c.getSimpleName());
	}
	
	/*
	public static SignalMonitor getMonitor() throws IOException {
		return new SignalMonitor(getConfiguration());
	}
	*/
	
	public static Session getSession() throws IOException, JDOMException {
		initialize();
		if (session != null) return session;
		session = SN.getSession();
		return session;
	}
	
	public static DatabaseWriter getDBWriter() throws Exception {
		if (dbwriter == null) {
			dbwriter = new DatabaseWriter(getConfiguration());
		}
		return dbwriter;
	}
	
	public static Connection getConnection() throws Exception {
		return getDBWriter().getConnection();
	}
	
	public static String getProperty(String propname) throws IOException {
		return getProperties().getProperty(propname);
	}
	
	public static String junitProperty(String name) throws IOException {
		String value = getProperties().getProperty("junit." + name);
		if (value == null) throw new IllegalArgumentException("name");
		return value;
	}
	
	public static Loader newLoader(String command) throws Exception {
		initialize();
		// SignalMonitor.clearSignal();
		Loader loader = new Loader(getProperties());
		loader.loadScriptText(command);
		return loader;
	}
	
	public static Loader newLoader() throws Exception {
		initialize();
		// SignalMonitor.clearSignal();
		return new Loader(getProperties());
	}
	
	public static SuiteModel newSuite(String command) throws Exception {
		String[] array = { command };
		return newSuite(array);
	}

	public static EphemeralSuite newSuite(String[] commands) throws Exception {
		CommandScript buffer = new CommandScript(commands);
		EphemeralSuite suite = new EphemeralSuite(getSession(), buffer);
		return suite;
	}
	
	public static PersistentSuite loadJunitSuite(String suiteName) throws Exception {
		// SignalMonitor.clearSignal();
		Loader loader = newLoader();
		loader.loadSuite(suiteName);
		return (PersistentSuite) loader.getSuite();
	}
		
	static File tempDir() throws IOException {
		return new File(junitProperty("temp"));
	}
	
	static File tempFile(String name) throws IOException {
		return new File(tempDir(), name);
	}
	
	static String getSchema() throws IOException {
		return getConfiguration().getString("schema", "");
	}
	
	static void banner(Logger logger, String msg) {
		int len = msg.length();
		String bar = "\n" + StringUtils.repeat("*", len + 4);
		logger.info(DateTime.now() + " " + msg + bar + "\n* " + msg + " *" + bar);
	}

	static void banner(Logger logger, String classname, String msg) {
		banner(logger, classname + " - " + msg);
	}
	
	static void banner(Logger logger, @SuppressWarnings("rawtypes") Class klass, String msg) {
		banner(logger, klass.getSimpleName(), msg);
	}
	
	static void sleep(int sleepSeconds) throws InterruptedException {
		banner(logger, "Sleeping " + sleepSeconds + " sec...");
		Thread.sleep(1000 * sleepSeconds);
	}
	
}
