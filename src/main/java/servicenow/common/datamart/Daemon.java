package servicenow.common.datamart;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.slf4j.Logger;

import servicenow.common.datamart.Daemon;
import servicenow.common.datamart.DatamartConfiguration;
import servicenow.common.datamart.LoggerFactory;
import servicenow.common.datamart.Scanner;
import servicenow.common.datamart.ShutdownHook;
import servicenow.common.datamart.SuiteModel;

public class Daemon implements org.apache.commons.daemon.Daemon {
	
	private static final Logger logger = LoggerFactory.getLogger(Daemon.class);
	private static Daemon service;

	private Options options;			
	private String propfilename;
	private DatamartConfiguration config;
	private Timer timer;
	private Scanner scanner;
	
	private int intervalSeconds;
	private int threadCount;

	HashMap<String,SuiteModel> suites = new HashMap<String,SuiteModel>();
	
	private static ExecutorService workerPool = null;
	DaemonContext daemonContext = null;

	public static void main(String[] args) throws Exception {
		logger.info("begin main");
		service = new Daemon();
		service.init(args);
		service.start();
		while (!workerPool.isTerminated()) {
			logger.info("main awaiting threadpool termination");
			workerPool.awaitTermination(120, TimeUnit.SECONDS);
		}
		logger.info("end main");
	}
			
	/**
	 * Return true if this server is multi-threaded
	 */
	@Deprecated
	static boolean isMultiThreaded() { 
		return workerPool != null; 
	}
	
	public void init(String[] args) throws DaemonInitException {
		
		logger.info("begin init args=" + Arrays.toString(args));
		options = new Options();
		Option optProp = new Option("p", "prop", true, "Property file (required)");
		optProp.setRequired(true);
		options.addOption(optProp);
		try {
			CommandLineParser cparser =	new BasicParser();
			CommandLine line = cparser.parse(options, args);
			propfilename = line.getOptionValue("p");
			logger.debug("propfilename=" + propfilename);
		} catch (ParseException e) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(Daemon.class.getName(), options );
			logger.error(e.getMessage(), e);
			throw new DaemonInitException(e.getMessage(), e);
		}
		
		if (propfilename == null) {
			logger.error("Missing propfilename");
			throw new DaemonInitException("Missing propfilename");
		}
		logger.info("end init");
		
	}
	
	@Override
	public void init(DaemonContext context) 
			throws DaemonInitException, Exception {
		this.daemonContext = context;
		this.init(context.getArguments());
	}

	@Override
	public void start() throws Exception {
		logger.info("begin start");
		DatamartConfiguration.setPropFile(new File(propfilename));		
		config = DatamartConfiguration.getDatamartConfiguration();
		
		threadCount = config.getInt("threads", 1);
		intervalSeconds = config.getInt("interval_seconds", 20);
		assert threadCount > 0;
		assert intervalSeconds > 0;
				
		workerPool = Executors.newFixedThreadPool(threadCount);
        this.timer = new Timer("scanner", true);
        scanner = new Scanner(config, workerPool);
        
		ShutdownHook shutdownHook = new ShutdownHook(scanner, workerPool);
		Runtime.getRuntime().addShutdownHook(shutdownHook);
		
        timer.schedule(scanner, 0, 1000 * intervalSeconds);
		logger.info("end start");
	}

	@Deprecated
	public static void shutdown() {
		service.stop();
		System.exit(-1);
	}
	
	@Override
	public void stop() {
		logger.info("begin stop");
		config = DatamartConfiguration.getDatamartConfiguration();
		boolean terminated = false;
		// shutdownNow will send an interrupt to all threads
		workerPool.shutdownNow();
		int waitSec = config.getInt("shutdown_seconds", 30);
		try {
			terminated = 
				workerPool.awaitTermination(waitSec, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
			terminated = false;
		}
		if (terminated) {
			logger.info("Shutdown Successful");
		}
		else {
			logger.warn("Some threads failed to terminate");
		}
		logger.info("end stop");		
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub		
	}
	
}
