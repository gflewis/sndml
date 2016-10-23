package servicenow.common.datamart;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.MDC;

import servicenow.common.datamart.DatamartConfiguration;
import servicenow.common.datamart.JobController;
import servicenow.common.datamart.JobModel;
import servicenow.common.datamart.LoggerFactory;
import servicenow.common.datamart.PersistentSuite;
import servicenow.common.datamart.ResourceManager;
import servicenow.common.datamart.Status;
import servicenow.common.datamart.SuiteController;
import servicenow.common.datamart.SuiteException;
import servicenow.common.datamart.SuiteExecException;
import servicenow.common.datamart.SuiteInitException;
import servicenow.common.datamart.SuiteModel;
import servicenow.common.datamart.SuiteModelException;
import servicenow.common.soap.DateTime;
import servicenow.common.soap.Session;

import org.jdom2.JDOMException;

/**
 * Contains a collection of jobs as loaded from a script file
 * or from a <b>u_datapump_jobset</b> in the ServiceNow instance.
 * @author Giles Lewis
 *
 */
public class SuiteController implements Runnable {

	// private final SignalMonitor monitor;
	private final DatamartConfiguration config;
	private DatabaseWriter writer = null;
	
	DateTime lastRunStart;
	DateTime runStart;
	
	final protected SuiteModel model;	
	
	static Logger logger = LoggerFactory.getLogger(SuiteController.class);	

	SuiteController(SuiteModel model) {		
		this.config = DatamartConfiguration.getDatamartConfiguration();
		assert config != null;
		assert model != null;		
		this.model = model;
		// this.monitor = new SignalMonitor();
		// assert this.monitor != null;
		this.lastRunStart = null;
		if (model.isPersistent()) {
			this.runStart = model.getRunStart();
		}
		else {
			this.runStart = null;			
		}
	}
	
	void close() {
		if (writer != null) {
			writer.close();
			writer = null;
		}
	}
	
	// SignalMonitor getMonitor() { return this.monitor; }
	SuiteModel getModel() { return this.model; }
	String getName() { return model.getName(); }
	int numJobs() {	return model.numJobs();	}
	List<JobModel> getJobs() { return model.getJobs(); }
	
	Session getSession() { 
		return model.getSession(); 
	}
	
	/**
	 * Used to assign a new session if multi-threaded
	 */
	void setSession(Session newsession) { 
		model.setSession(newsession); 
	}
	
	/**
	 * Defer opening of a target connection (database) until it is needed.
	 * The suite controller could have been constructed in a different thread.
	 */
	DatabaseWriter getDatabaseWriter() {
		// if (writer == null) writer = TargetWriter.newTargetWriter(config);
		if (writer == null) writer = new DatabaseWriter(config);
		return writer;
	}
	
	/**
	 * A polling suite runs at some pre-defined frequency.
	 * After all jobs are complete status is set to READY.
	 * and the nextRunStart is calculated.
	 * The status of a non-polling suite is set to COMPLETE
	 * after all jbos are complete.
	 */
	boolean isPolling() { return model.isPolling();	}
	
	/**
	 * Return true if suite and job definitions are read from ServiceNow.
	 * Return false if job definitions are read from a text file.
	 */
	boolean isPersistent() { return model.isPersistent(); }

	/**
	 * If this is a polling job then return the frequency in seconds.
	 * If this is a non-polling job then return null.
	 */
	Integer getFrequency() { return model.getFrequency(); }
	
	
	synchronized DateTime getRunStart() { 
		return model.getRunStart(); 
	}
	
	synchronized DateTime getNextRunStart() { 
		return model.getNextRunStart(); 
	}	
	
	synchronized void setStatus(Status newstatus) throws SuiteModelException {
		model.setStatus(newstatus);
	}
	
	synchronized Status getStatus() {
		return model.getStatus();
	}
			
	/**
	 * This procedure will catch and handle any exceptions 
	 * which are specific to the suite such as IOException or SQLException.
	 * If such an exception is encountered then the status of the suite
	 * will be set to FAILED and the run terminated. 
	 */
	@Override
	public void run() {
		String name = getName();
		logger.info("run " + name);
		// If we are multi-threaded, then each suite gets a 
		// private ServiceNow Session and a private Database connection (TargetWriter)
		
		// Here is the ServiceNow Session
		setSession(ResourceManager.getNewSession());
		
		// The Database connection (TargetWriter) will be opened on first access

		// DatamartConfiguration config = DatamartConfiguration.getDatamartConfiguration();
		// model.setSession(config.getString("servicenow_session", "thread"));
		// model.setDatabaseWriter(config.getString("database_connection", "thread"));
		assert getStatus().equals(Status.QUEUED) ||
				getStatus().equals(Status.RESUME);		
		try {
			runOnce();
		}
		catch (SuiteException e) {
			logger.warn(e.getMessage(), e);
		}
		catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
			logger.error(name + " has been cancelled");
		}
		catch (Throwable e) {
			logger.error(e.getMessage(), e);
			logger.error(
				"Aborting main thread due to unexpected exception " + 
				e.getClass().getSimpleName());
			System.exit(-1);
		}
		logger.info("end " + name);
		close();
	}
	
	/**
	 * Place suite in RUNNING state and all jobs in a QUEUED state.
	 * 
	 * @return The datetime that we started running.
	 * @throws SuiteInitException 
	 * @throws JDOMException 
	 */
	synchronized DateTime startRunning() 
			throws InterruptedException, SuiteInitException, SuiteModelException {
		logger.debug("startRunning");;
		DateTime result = model.startRunning();
		// If we are persistent, then we have deferred the loading of jobs
		// if (isPersistent() && jobs == null)	loadJobs();		
		assert model.getJobs() != null;
		return result;
	}
	
	/**
	 * Run each job once, if it is ready.
	 * @throws LoaderException 
	 */
	public synchronized DateTime runOnce() 
			throws InterruptedException, SuiteInitException, 
				SuiteExecException, SuiteModelException {
		assert (getStatus().equals(Status.READY) ||
				getStatus().equals(Status.QUEUED) ||
				getStatus().equals(Status.RESUME));
		if (model.getName() != null) MDC.put("suite", model.getName());
		lastRunStart = getRunStart();
		runStart = startRunning();
		if (model instanceof PersistentSuite) {
			int jobcount = ((PersistentSuite) model).countJobs();
			logger.debug("jobcount=" + jobcount);
			assert getJobs().size() == jobcount :
				"Jobcount mismatch; database=" + jobcount + " model=" + getJobs().size();
		}
		MDC.put("start", runStart.toString());
		logger.debug("runOnce lastStart=" + lastRunStart + " start=" + runStart);
		Status newStatus = 
			isPolling() ? Status.READY : Status.COMPLETE;
		for (JobModel job : getJobs()) {
			String jobname = job.getName();
			// Only run jobs with a status of QUEUED, READY, RUNNING or RESUME
			Status jobstatus = job.getStatus();
			logger.debug(jobname + " status=" + jobstatus);
			if (!(jobstatus.equals(Status.QUEUED) ||
					jobstatus.equals(Status.READY) || 
					jobstatus.equals(Status.RUNNING) || 
					jobstatus.equals(Status.RESUME))) {
				continue;
			}
			JobController jobController = new JobController(this, job);
			jobController.runJob(runStart, newStatus);
			
		}
		setStatus(newStatus);
		return model.getNextRunStart();
	}

}
