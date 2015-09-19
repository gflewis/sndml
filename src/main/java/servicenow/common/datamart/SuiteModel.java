package servicenow.common.datamart;

import java.util.List;

import org.slf4j.Logger;

import servicenow.common.datamart.JobModel;
import servicenow.common.datamart.PersistentSuite;
import servicenow.common.datamart.Status;
import servicenow.common.datamart.SuiteController;
import servicenow.common.datamart.SuiteInitException;
import servicenow.common.datamart.SuiteModelException;
import servicenow.common.soap.DateTime;
import servicenow.common.soap.Session;

abstract class SuiteModel {
		
	protected Session session;
	protected SuiteController controller;
	protected Integer frequency; // null if not polling
	protected Status status;
	protected DateTime runStart;
	protected DateTime nextRunStart;
		
	protected List<JobModel> jobs;
	
	protected Logger logger;

	SuiteModel(Session session) {
		assert session != null;
		this.session = session;
	}
	
	List<JobModel> getJobs() {
		if (jobs == null) throw new IllegalStateException("Jobs not yet loaded");
		return this.jobs; 
	}
	
	SuiteController getController() { 
		return this.controller; 
	}
	
	Session getSession() { 
		return this.session; 
	}

	/**
	 * Use this procedure to assign a new session if multi-threaded
	 */
	void setSession(Session newsession) {
		this.session = newsession;
	}

	int numJobs() { return this.jobs.size(); }
	Status getStatus() { return this.status; }

	boolean isPersistent() { return (this instanceof PersistentSuite); }
	abstract String getName();

	boolean isPolling() { 
		return (frequency != null && frequency > 0); 
	}
	
	int getFrequency() { 
		if (!isPolling()) throw new UnsupportedOperationException();
		Integer freq = this.frequency;
		assert freq > 0;
		return freq;
	}
	
	DateTime getRunStart() {
		DateTime result = this.runStart; 
		return result;
	}
	
	DateTime getNextRunStart() {
		DateTime result = this.nextRunStart;
		return result;
	}

	/**
	 * Change the status of the Suite to Running
	 * and return the start time.
	 * @throws SuiteInitException 
	 */
	abstract DateTime startRunning() 
			throws InterruptedException, SuiteInitException, SuiteModelException;
	
	void setStatus(Status newstatus) throws SuiteModelException {
		setStatus(newstatus, null);
	}

	abstract void setStatus(Status newstatus, String runlog)	
			throws SuiteModelException;

	public String getDescription() {
		StringBuilder result = new StringBuilder();
		String name = getName();
		if (name != null) result.append(name + ": ");
		if (isPolling()) result.append("every " + getFrequency() + " seconds\n");
		for (JobModel job : getJobs()) {
			result.append(job.getDescription());
			result.append("\n");
		}
		return result.toString();
	}
		
}
