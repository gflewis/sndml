package servicenow.common.datamart;

import java.io.IOException;
import java.sql.SQLException;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;

import org.jdom2.JDOMException;
import org.slf4j.Logger;

import servicenow.common.datamart.DatamartConfiguration;
import servicenow.common.datamart.LoggerFactory;
import servicenow.common.datamart.PersistentSuite;
import servicenow.common.datamart.ResourceException;
import servicenow.common.datamart.Scanner;
import servicenow.common.datamart.Status;
import servicenow.common.datamart.SuiteController;
import servicenow.common.datamart.SuiteException;
import servicenow.common.datamart.SuiteModel;
import servicenow.common.soap.DateTime;
import servicenow.common.soap.InsufficientRightsException;
import servicenow.common.soap.QueryFilter;
import servicenow.common.soap.Record;
import servicenow.common.soap.RecordList;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;
/**
 * A task that is scheduled to run periodically, to check for suites that need to run.
 * If a suite is ready to run, then its status is changed to Queued 
 * and it is submitted to the worker pool for execution.
 *
 */
public class Scanner extends TimerTask {

	static final Logger logger = LoggerFactory.getLogger(Scanner.class);
	
	final DatamartConfiguration config;
	final ExecutorService workerpool;
	
	private Session session = null;
	private String target;
	private int lagSeconds;
		
	Scanner(DatamartConfiguration config, ExecutorService pool) 
			throws InsufficientRightsException, ResourceException, 
			IOException, JDOMException {
		this.config = config;	
		this.workerpool = pool;
		init();		
	}
	
	/**
	 * Open a new ServiceNow session if necessary
	 */
	private void init() 
			throws InsufficientRightsException, ResourceException, 
			IOException, JDOMException {
		if (session == null) { 
			this.target = config.getRequiredString("target");
			this.lagSeconds = config.getInt("lag_seconds", 0);
			int threadCount = config.getInt("threads", 0);
			int intervalSeconds = config.getInt("interval_seconds", 20);
			logger.info("target=" + target + 
					(" interval=" + intervalSeconds + "s") +		
					(threadCount != 0 ? " threads=" + threadCount : "") + 
					(lagSeconds != 0 ? " lag=" + lagSeconds  + "s" : "")); 
			this.session = new Session(DatamartConfiguration.getSessionConfiguration());
		}		
	}
	
	/**
	 * Find all suites with status READY and run them.
	 */
	@Override
	public synchronized void run() {
				
		try {
			init();
			DateTime runStart = getLocalTime();
			logger.info("run: scanning target=" + target + " runStart=" + runStart);
			// NextRunStart.reset();
			QueryFilter filter = new QueryFilter();
			filter.addFilter("u_status", QueryFilter.IN, "ready,resume");
			if (target != null) 
				filter.addFilter("u_target", QueryFilter.EQUALS, target);
			Table suiteTable = session.table("u_datapump_jobset");
			RecordList recs = suiteTable.petitReader(filter).getAllRecords();
			logger.debug("run: processing " + recs.size() + " suites");
			for (Record rec : recs) {
				SuiteModel suite = new PersistentSuite(session, rec);
				runSuite(suite.getController(), runStart);
			}
		}
		catch (InterruptedException e) {
			logger.warn(
				getClass().getSimpleName() + " has been interrupted", e);
		}
		catch (SuiteException e) { forceReinitization(e); }
		catch (IOException e)    { forceReinitization(e); }
		catch (SQLException e)   { forceReinitization(e); }
		catch (JDOMException e)  { abortProcess(e); }
		catch (Throwable e)      { abortProcess(e); }
	}

	private void forceReinitization(Exception e) {
		// force re-initialization next time we are called
		session = null;
		target = null;
		logger.error(e.getMessage(), e);
		logger.warn("Forcing reinitialization");		
	}
	
	/**
	 * If an exception occurs, then close the ServiceNow session
	 * so that we can re-open it next time we are called.
	 */
	private void abortProcess(Throwable e) {
		logger.error(e.getMessage(), e);
		logger.error("Aborting to unexpected exception:" + e.getClass().getSimpleName());
		System.exit(-1);
	}
	
	/**
	 * Run a suite if it is ready.
	 * Set the status of the suite to QUEUED and run all jobs in the suite.
	 * @throws LoaderException 
	 * @throws JDOMException 
	 * @throws InterruptedException 
	 * @throws SQLException 
	 */
	private void runSuite(SuiteController suite, DateTime runStart) 
		throws IOException, SuiteException, SQLException, 
			InterruptedException, JDOMException {
		
		assert suite.getStatus().equals(Status.READY) ||
				suite.getStatus().equals(Status.RESUME);
		String suiteName = suite.getName();
		// First check the nextRunStart to see if it is really ready to run
		if (suite.isPolling()) {
			DateTime suiteNextRun = suite.getNextRunStart();
			if (suiteNextRun == null) {
				logger.warn(suiteName + " nextRunStart is null");;
				return;
			}
			// Only run it if the next run is in the past
			if (suiteNextRun.compareTo(runStart) >= 0) {
				logger.info(suiteName + " will be ready at " + suiteNextRun);
				// If this suite needs to run before the next server run
				// then make the next server run earlier
				// NextRunStart.setNext(suiteNextRun);
				return;
			}
		}
		
		if (suite.getStatus().equals(Status.READY))
			suite.setStatus(Status.QUEUED);
		
		if (this.workerpool == null) {
			// No threads. 
			// Just run it.
			logger.info("Running " + suiteName);
			suite.runOnce();
			// If we are polling, then check to see if we need to make the
			// next run sooner
			if (suite.isPolling()) {
				DateTime suiteNextRun = suite.getNextRunStart();
				logger.info(suiteName + " next run set for " + suiteNextRun);
				// NextRunStart.setNext(suiteNextRun);
			}
			
			suite.close();			
		}
		else {
			// Multi-threaded.
			// Submit this suite to the pool for execution
			logger.info("Starting " + suiteName);
			workerpool.submit(suite);
			
		}				
	}

	private DateTime getLocalTime() {
		DateTime now = DateTime.now();
		if (lagSeconds != 0) now = now.subtractSeconds(lagSeconds);
		return now;
	}

	
}
