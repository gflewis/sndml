package servicenow.common.datamart;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.JDOMException;
import org.slf4j.Logger;

import servicenow.common.datamart.JobModel;
import servicenow.common.datamart.LoggerFactory;
import servicenow.common.datamart.PersistentJob;
import servicenow.common.datamart.PersistentSuite;
import servicenow.common.datamart.Status;
import servicenow.common.datamart.SuiteController;
import servicenow.common.datamart.SuiteInitException;
import servicenow.common.datamart.SuiteModel;
import servicenow.common.datamart.SuiteModelException;
import servicenow.common.soap.DateTime;
import servicenow.common.soap.FieldValues;
import servicenow.common.soap.InvalidDateTimeException;
import servicenow.common.soap.Key;
import servicenow.common.soap.QueryFilter;
import servicenow.common.soap.Record;
import servicenow.common.soap.RecordList;
import servicenow.common.soap.Session;
import servicenow.common.soap.SoapResponseException;
import servicenow.common.soap.Table;
import servicenow.common.soap.TableReader;

public class PersistentSuite extends SuiteModel {

	protected Key key;
	protected Record rec;

	protected Table suiteTable;
	protected Table jobTable;

	static PersistentConfigMap names = new PersistentConfigMap();
	
	PersistentSuite(Session session, Record rec) 
    		throws IOException, JDOMException, SQLException {
		
		super(session);
		assert rec != null;
		
		this.rec = rec;
		this.key = rec.getKey();		
		this.status = Status.valueOf(
			rec.getField(fieldName("status")).toUpperCase());

		suiteTable = session.table(myTableName());
		jobTable   = session.table(PersistentJob.myTableName());
		assert rec.getTable().getName().equals(("suite.table"));
		
		String suiteName = rec.getField(fieldName("name"));
    	logger = LoggerFactory.getLogger(PersistentSuite.class, suiteName);

    	updateFromRec();
		logger.info("suite=" + suiteName + " frequency=" + frequency);
		
		controller = new SuiteController(this);		
	}	

	static String myTableName() {
		return names.getName("suite.table");
	}
	
	static String fieldName(String name) {
		return names.getName("suite.field." + name);
	}
	
	/**
	 * Load the list of jobs from ServiceNow.
	 * This function is only valid if the suite is persistent.
	 * For non-persistent suite the jobs are loaded by the constructor.
	 * For persistent suite the jobs are loaded by startRunning.
	 */
	void loadJobs() 
			throws IOException, InterruptedException, 
				SuiteInitException, SuiteModelException {
		if (jobs != null) return;
		RecordList jobRecList = readAllJobs();
    	List<JobModel> joblist = new ArrayList<JobModel>(jobRecList.size());
		for (Record jobRec : jobRecList) {
			String jobName = jobRec.getField("u_name");
			Key jobKey = jobRec.getKey();
			logger.debug("addJob " + jobName + " " + jobKey);
			JobModel job = new PersistentJob(this, jobRec);
			joblist.add(job);
		}
		this.jobs = joblist;
	}
	
	/**
	 * Read all jobs from the database
	 */
	private RecordList readAllJobs() 
			throws SuiteModelException, InterruptedException {
		RecordList jobRecList;
		try {
			QueryFilter filter = new QueryFilter();
			filter.addFilter(PersistentJob.fieldName("suite"), QueryFilter.EQUALS, key.toString());
			filter.addFilter(PersistentJob.fieldName("active"), QueryFilter.EQUALS, "true");
			TableReader jobReader = jobTable.petitReader(filter);
			jobReader.sortAscending(PersistentJob.fieldName("order"));		
			jobRecList = jobReader.getAllRecords();
		}
		catch (IOException e) {
			throw new SuiteModelException(e);
		}
		return jobRecList;
	}

	/**
	 * Update the status of the job model to match the status in ServiceNow
	 * @throws IOException 
	 */
	private void refreshJobs() throws SuiteModelException {
		// No need to refresh jobs if they are not yet loaded
		if (jobs == null) return;
		try {
			RecordList jobRecList = readAllJobs();
			for (Record jobRec : jobRecList) {
				String jobName = jobRec.getField("u_name");
				logger.debug("refresh " + jobName);
				PersistentJob job = findJob(jobRec);
				assert job != null;
				job.refreshStatus(jobRec);
			}
		}
		catch (InterruptedException e) {
			logger.warn("refreshJobs interrupted", e);
			Thread.currentThread().interrupt();
		}
	}
	
	/**
	 * Counts the number of jobs in the database. Used for debugging.
	 */
	int countJobs() throws SuiteModelException {
		QueryFilter filter = new QueryFilter();
		filter.addFilter("u_jobset", QueryFilter.EQUALS, key.toString());
		filter.addFilter("u_inactive", QueryFilter.EQUALS, "false");
		try {
			return jobTable.getCount(filter);
		} catch (SoapResponseException e) {
			throw new SuiteModelException(e);
		} catch (IOException e) {
			throw new SuiteModelException(e);
		}
	}
	
	PersistentJob findJob(Record jobRec) {
		assert jobRec.getTable().getName().equals(jobTable.getName());
		Key key = jobRec.getKey();
		assert key != null;		
		assert jobs.size() > 0;
		for (JobModel job : jobs) {
			PersistentJob pjob = (PersistentJob) job;
			assert pjob.getKey() != null;
			if (pjob.getKey().equals(key)) return pjob;
		}
		return null;		
	}	

	String getName() {
		String name = rec.getField(fieldName("name"));
		assert name != null && name.length() > 0;
		return name;
	}
	
	DateTime startRunning() 
			throws InterruptedException, SuiteInitException, SuiteModelException {
		DateTime oldRunStart = runStart;
		this.status = Status.RUNNING;
		try {
			loadJobs();
			FieldValues values = new FieldValues();
			values.set(fieldName("status"), "running");
			suiteTable.update(key, values);
			rec = suiteTable.get(key);
			// runStart and nextRunStart will be calculated by business rule			
			updateFromRec();
		}
		catch (IOException e) {
			throw new SuiteModelException(e);
		}
		assert (oldRunStart == null || runStart.compareTo(oldRunStart) >= 0) :
			("runStart=" + runStart + " oldRunStart=" + oldRunStart);
		logger.info("startRunning " + runStart.toString());
		return runStart;
	}

	void setStatus(Status newstatus, String runlog)	
			throws SuiteModelException {
		logger.debug("setStatus " + newstatus);
		this.status = newstatus;
		FieldValues values = new FieldValues();
		values.set(fieldName("status"), newstatus.toString().toLowerCase());
		try {
			suiteTable.update(key, values);
		}
		catch (IOException e) {
			throw new SuiteModelException(e);
		}
		refreshJobs();
	}
	
	void updateFromRec() throws InvalidDateTimeException, SoapResponseException {
		status = Status.valueOf(rec.getField(fieldName("status")).toUpperCase());
		runStart = rec.getDateTime(fieldName("run_start"));
		nextRunStart = rec.getDateTime(fieldName("next_run_start"));
		boolean polling = rec.getBoolean(fieldName("polling"));
		if (polling)
			frequency = rec.getDuration(fieldName("frequency"));
		else
			frequency = null;
	}
	
	/**
	 * Get a job by name. Used for unit tests.
	 */
	PersistentJob getJobByName(String name) 
			throws IOException, JDOMException, InterruptedException, 
				SQLException, SuiteInitException, SuiteModelException {
		if (jobs == null) loadJobs();
		for (JobModel job : jobs) {
			if (job.getName().equals(name)) return (PersistentJob) job;
		}
		return null;
	}
	
	static Record getSuiteRecord(Session session, String name) 
			throws IOException, JDOMException {
		Table suiteTable = session.table(myTableName());
		assert suiteTable != null;
		Record rec = suiteTable.get(fieldName("name"), name);
		Logger logger = LoggerFactory.getLogger(PersistentSuite.class); 
		if (rec == null)
			logger.debug("getSuiteRecord " + name + " is null");
		else
			logger.debug("");
		return rec;
	}
		
}
