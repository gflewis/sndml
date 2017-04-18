package servicenow.common.datamart;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.io.*;

import org.slf4j.Logger;

import servicenow.common.datamart.CommandScript;
import servicenow.common.datamart.DatamartConfiguration;
import servicenow.common.datamart.EphemeralSuite;
import servicenow.common.datamart.Loader;
import servicenow.common.datamart.LoggerFactory;
import servicenow.common.datamart.PersistentSuite;
import servicenow.common.datamart.ResourceManager;
import servicenow.common.datamart.ShutdownHook;
import servicenow.common.datamart.Status;
import servicenow.common.datamart.SuiteException;
import servicenow.common.datamart.SuiteModel;
import servicenow.common.soap.Record;
import servicenow.common.soap.Session;

import org.apache.commons.cli.Options;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.jdom2.JDOMException;

/**
 * Main class and entry point for the Datamart Loader application.
 * This application uses the "soap" package to read/update the ServiceNow instance
 * and JDBC to write to the SQL database.
 * 
 * @author Giles Lewis
 */
public class Loader {

	private final Session session;
	private SuiteModel suite;
	
	static final Logger logger = LoggerFactory.getLogger(Loader.class);
	
	public Loader(Properties props)
			throws IOException, SuiteException, SQLException {
		if (props != null) DatamartConfiguration.setProperties(props);
		this.session = ResourceManager.getMainSession();
		assert this.session != null;
		ShutdownHook hook = new ShutdownHook();
		Runtime.getRuntime().addShutdownHook(hook);
	}

	public Loader() 
			throws IOException, SuiteException, SQLException {
		this(null);
	}
	
	public Loader(Properties props, String text) 
			throws IOException, SuiteException, 
			SQLException, ParseException {
		this(props);
		assert this.session != null;
		loadScriptText(text);
	}
	
    public Session getSession() { return this.session; } 
    	
	public static void main(String[] args) throws Exception {
		
		Properties options = parseOptions(args);

		String operation = options.getProperty("operation");
		String propfilename = options.getProperty("propfilename");
		Boolean resume = new Boolean(options.getProperty("resume"));
		
		logger.info("propfile=" + propfilename);
		assert propfilename != null;
		assert propfilename.length() > 0;
		DatamartConfiguration.setPropFile(new File(propfilename));		
		
		assert operation != null;
		if (operation.equals("help")) {
			showHelp();
			return;
		}

		Loader loader = new Loader();
		
		if (operation.equals("script")) {
			loader.loadScriptFile(
				new File(options.getProperty("scriptfilename")));
			loader.runToComplete();
		}
		else if (operation.equals("command")) {
			String allLines = options.getProperty("command");
			CommandScript script = new CommandScript(allLines);
			loader.loadBuffer(script);
			loader.runToComplete();
		}
		else if (operation.equals("suite")) {
			loader.loadSuite(options.getProperty("suite"));
			SuiteModel suite = loader.getSuite();
			if (resume)	
				suite.setStatus(Status.RESUME);
			else
				suite.setStatus(Status.READY);
			loader.runToComplete();
		}		
		logger.info("Done");
	}
			
	/**
	 * Figure out what we are supposed to do.
	 * @throws org.apache.commons.cli.ParseException 
	 */
	static Properties parseOptions(String[] args) 
			throws org.apache.commons.cli.ParseException {	
		
		Options options = new Options();
		options.addOption("p", "prop", true, "Property file (required)");
		options.addOption("f", "script", true, "Specify script file");
		options.addOption("e", "command", true, "Execute a command");
		options.addOption("js", "suite", true, "Specify name of job suite");
		options.addOption("resume", false, "Resume a cancelled or aborted suite");
		options.addOption("debug", false, "Enable log4j debugging");
		// options.addOption("cancel", false, "Cancel currently running suite");		
		org.apache.commons.cli.CommandLineParser cparser = 
				new org.apache.commons.cli.BasicParser();
		org.apache.commons.cli.CommandLine line = cparser.parse(options, args);
		@SuppressWarnings("unchecked")
		LinkedList<String> extras = new LinkedList<String>(line.getArgList());

		if (line.hasOption("debug"))
			LogManager.getRootLogger().setLevel(Level.DEBUG);
		String operation = null;
		String propfilename = line.getOptionValue("p");
		if (propfilename == null) optionsError("Missing propfilename");
		DatamartConfiguration.setPropFile(new File(propfilename));
		Properties results = new Properties();
		results.setProperty("propfilename", propfilename);
		results.setProperty("resume",  "false");
		// results.setProperty("cancel", "false");
		
		if (line.hasOption("help")) {
			operation = "help";
		}
		if (line.hasOption("cancel")) {
			if (operation != null) optionsError("Multiple options specified");
			results.setProperty("cancel", "true");			
			operation = "cancel";
		}
		if (line.hasOption("script")) {
			if (operation != null) optionsError("Multiple options specified");
			operation = "script";
			String scriptfilename = line.getOptionValue("script");
			logger.debug("scriptfile=" + scriptfilename);
			results.setProperty("scriptfilename", scriptfilename);			
		}
		if (line.hasOption("command")) {
			if (operation != null) optionsError("Multiple options specified");
			operation = "command";
			String command = line.getOptionValue("command");
			while (extras.size() > 0) {
				command = command + " " + extras.removeFirst();
			}
			logger.debug("command=\"" + command + "\"");
			results.setProperty("command", command);			
		}
		if (line.hasOption("suite")) {
			if (operation != null) optionsError("Multiple options specified");
			operation = "suite";
			results.setProperty("suite", line.getOptionValue("suite"));			
		}
		if (line.hasOption("resume")) {
			if (!"suite".equals(operation)) optionsError("Suite name is required");
			results.setProperty("resume", "true");
		}
		if (operation == null) optionsError("Missing operation");
		if (extras.size() > 0) optionsError("Unrecognized argument: " + extras.getFirst());
		results.setProperty("operation",  operation);
		return results;		

	}

	private static void optionsError(String message) {
		logger.error(message);
		showHelp();
		throw new IllegalArgumentException();		
	}
	
	static public void showHelp() {
		String text =
			"-p <filename>          Property file (required)\n" +
			"-f <filename>          Specify script file\n" +
			"-e \"<command>\"         Execute a command\n" +
			"-js <suite>            Specify name of suite\n" +
			"-js <suite> -resume    Resume a cancelled or aborted suite\n" +
			"-cancel                Cancel currently running suites\n";
		System.err.println(text);
    }
            
    protected SuiteModel getSuite() { return this.suite; }
		
	public void loadSuite(String suiteName) throws Exception {
		logger.info("suite=" + suiteName);
		Record suiteRec = PersistentSuite.getSuiteRecord(session, suiteName);
		if (suiteRec == null) throw new NullPointerException();
		this.suite = new PersistentSuite(session, suiteRec);
	}	
	
	public void loadScriptFile(File file) 
			throws IOException, SuiteException, SQLException,	ParseException {
		logger.info("scriptfile=" + file.getAbsolutePath());
		loadBuffer(new CommandScript(file));
	}
	
	public void loadScriptText(String text) 
			throws SuiteException, IOException, SQLException, ParseException {
		String[] array = text.split(",\\s*");
		loadBuffer(new CommandScript(array));		
	}
	
	protected void loadBuffer(CommandScript buffer) 
			throws SuiteException, IOException, SQLException, ParseException {
		this.suite = new EphemeralSuite(session, buffer);
	}
	
	public void runToComplete() throws Exception {
		suite.getController().runOnce();
		if (suite.isPolling()) runForever();
	}

	private void runForever() 
			throws IOException, ParseException, SQLException, 
			InterruptedException, JDOMException, SuiteException {
		// can only run forever if there is a frequency
		assert suite.isPolling();
		// DateTime last = DateTime.now().subtractSeconds(60);
		DatamartConfiguration config = DatamartConfiguration.getDatamartConfiguration();
		int intervalSec = config.getInt("interval_seconds", 20);
		if (intervalSec < 2)
			throw new AssertionError("Too small interval between runs");
		while (true) {
			// NextRunStart.reset();
			// NextRunStart.setNext(suite.getNextRunStart());
			// NextRunStart.sleepUntilNext();
			Thread.sleep(1000 * intervalSec);
			// if (DateTime.now().compareTo(last) < 2)
			// 	throw new AssertionError("Too small interval between runs");
			suite.getController().runOnce();
			// last = DateTime.now();
			System.gc();
		}
	}
	
}

