package servicenow.common.datamart;

import java.io.IOException;

import org.jdom2.JDOMException;

import servicenow.common.datamart.JobModel;
import servicenow.common.datamart.JobOperation;
import servicenow.common.datamart.LoadMethod;
import servicenow.common.datamart.LoggerFactory;
import servicenow.common.datamart.Metrics;
import servicenow.common.datamart.PersistentJob;
import servicenow.common.datamart.Status;
import servicenow.common.datamart.SuiteInitException;
import servicenow.common.datamart.SuiteModel;
import servicenow.common.datamart.SuiteModelException;
import servicenow.common.datamart.SuiteParseException;

import servicenow.common.soap.DateTime;
import servicenow.common.soap.FieldValues;
import servicenow.common.soap.InvalidTableNameException;
import servicenow.common.soap.Key;
import servicenow.common.soap.QueryFilter;
import servicenow.common.soap.Record;
import servicenow.common.soap.Session;
import servicenow.common.soap.Table;

public class PersistentJob extends JobModel {

	protected final Key jobkey; 
	protected final Table jobtable; 
		
    PersistentJob(SuiteModel suite, Record jobrec) 
    		throws IOException,	InvalidTableNameException, SuiteInitException {
    	super(suite);
    	assert suite != null;
    	assert jobrec != null;
		assert jobrec.getTable().getName().equals("u_datapump_job");
    	this.jobkey = jobrec.getKey();
    	this.jobtable = jobrec.getTable();
		jobName = jobrec.getField("u_name");
    	logger = LoggerFactory.getLogger(PersistentJob.class, jobName);
		String tableName = jobrec.getField("u_table");
		String operationName = jobrec.getField("u_operation");
		if (operationName.equals("sql")) {
			this.operation = JobOperation.SQL;
			this.method = null;
    		this.sqlCommand = jobrec.getField("u_sql_statement");
			this.glidetable = null;
		}
		else {
	    	try {
				Session session = suite.getSession();
				this.glidetable = session.table(tableName);
			} catch (JDOMException e) {
				throw new RuntimeException(e);
			}
	    	if (operationName.equals("load")) {
	    		this.operation = JobOperation.LOAD;
	        	this.truncate = jobrec.getBoolean("u_truncate");
	        	this.partitionField = jobrec.getField("u_partition_field");
	        	this.partitionValue = jobrec.getField("u_partition_value");
	    		String intervalField = jobrec.getField("u_interval_field");
	    		if (intervalField == null) {
	        		this.sortField = "sys_created_on";
	    		}
	    		else {
	    			if (intervalField.equals("created") ||
	    					intervalField.equals("sys_created_on")) {
	    				this.useCreatedDate = true;
	    				this.sortField = "sys_created_on";    				
	    			}
	    			else if (intervalField.equals("updated") ||
	    					intervalField.equals("sys_updated_on")) {
	    				this.useCreatedDate = false;
	    				this.sortField = "sys_updated_on";    				
	    			}
	    			else {
	    				// should be either name or sys_id
	    				this.sortField = intervalField;
	    			}
	    			this.intervalStart = jobrec.getDateTime("u_interval_start");
	    			this.intervalEnd = jobrec.getDateTime("u_interval_end");
	    		}
	    	}
	    	else if (operationName.equals("refresh")) {
	    		this.operation = JobOperation.REFRESH;
	        	this.partitionField = jobrec.getField("u_partition_field");
	        	this.partitionValue = jobrec.getField("u_partition_value");
	    		this.sortField = "sys_updated_on";
	    	}
	    	else if (operationName.equals("prune")) {
	    		this.operation = JobOperation.PRUNE;
	    		this.sortField = null;
	    	}
			else 
				throw new SuiteParseException("Invalid operation: " + operationName);
	    	String method = jobrec.getField("u_load_method");
	    	if (method.equals("I")) this.method = LoadMethod.INSERT_ONLY;
	    	else if (method.equals("UI")) this.method = LoadMethod.UPDATE_INSERT;
	    	else if (method.equals("T")) this.method = LoadMethod.COMPARE_TIMESTAMPS;
	    	else throw new SuiteParseException("load_method=" + method);
	    	this.sqlTableName = jobrec.getField("u_sql_table_name");
	    	this.basefilter = new QueryFilter(jobrec.getField("u_conditions"));
		}
    	assert operation != null;
    	this.status = Status.valueOf(jobrec.getField("u_status").toUpperCase());
		// this.controller = new JobController(this);
    	logger.debug(getDescription());
    }	

	Key getKey() { return this.jobkey; }

    /**
     * Set the status to Running, and advance the interval.
     */
    synchronized void startRunning(DateTime runStart) throws SuiteModelException {
    	logger.debug("startRunning " + runStart);
		this.status = Status.RUNNING;
		FieldValues values = new FieldValues();
		values.set("u_status", "running");
		// for persistent jobs: ignore runStart, let business rule calculate it
		// if (runStart != null && operation != JobOperation.LOAD) 
		// 	values.set("u_interval_end", runStart);
		updateJobTableValues(values);
		Record rec = readRecord();
		if (operation != JobOperation.LOAD) {
	    	intervalStart = rec.getDateTime("u_interval_start");
	    	intervalEnd = rec.getDateTime("u_interval_end");
		}
		logger.debug("startRunning runStart=" + runStart + " start=" + intervalStart + " end=" + intervalEnd);
		// return intervalEnd;
    }

    synchronized void setFailedStatus(Status newstatus, String message) {
    	assert newstatus.equals(Status.CANCELLED) || newstatus.equals(Status.FAILED);
		this.status = newstatus;
		logger.debug("setFailedStatus " + newstatus);
		if (!isPersistent()) return;
		FieldValues values = new FieldValues();
		values.set("u_status", newstatus.toString().toLowerCase());
		values.set("u_error_message", message);
		try {
			updateJobTableValues(values);
		} catch (SuiteModelException e) {
			logger.error("setFailedStatus: unable to set status to " + newstatus, e);
		}
	}
		
    synchronized void setCompletedStatus(Status newstatus) throws SuiteModelException {
    	assert newstatus.equals(Status.COMPLETE) || newstatus.equals(Status.READY);
		this.status = newstatus;
		logger.debug("setFinishedStatus " + newstatus);
		if (!isPersistent()) return;
		FieldValues values = new FieldValues();
		values.set("u_status", newstatus.toString().toLowerCase());
    	updateJobTableValues(values);
    }
    
    synchronized void postMetrics(Metrics metrics) throws SuiteModelException {
		int inserts = metrics.recordsInserted();
		int updates = metrics.recordsUpdated();
		int deletes = metrics.recordsDeleted();
		int processed = metrics.recordsPublished();
		logger.debug("postMetrics inserts=" + inserts + 
				" updates=" + updates +
				" deletes=" + deletes + 
				" processed=" + processed);	
		assert (inserts + updates + deletes == processed) : (
			"inserts=" + inserts + " updates=" + updates +
			" deletes=" + deletes + " processed=" + processed); 
		FieldValues values = metrics.fieldValues();
    	updateJobTableValues(values);
	}

    synchronized DateTime resetInterval() throws SuiteModelException {
    	logger.debug("resetInterval");
    	if (!isPersistent()) throw new IllegalStateException();
    	if (operation.equals(JobOperation.LOAD)) throw new IllegalStateException();
		Metrics emptyMetrics = new Metrics();
		DateTime future = new DateTime("2199-12-31 24:59:59");
    	FieldValues values = new FieldValues();
    	values.set("u_status", Status.PENDING.toString().toLowerCase());
    	values.set("u_interval_start", future);
    	values.putAll(emptyMetrics.fieldValues());
    	updateJobTableValues(values);
		Record rec = readRecord();
		status = Status.valueOf(rec.getField("u_status").toUpperCase());
		intervalStart = rec.getDateTime("u_interval_start");
		intervalEnd = rec.getDateTime("u_interval_end");
		return intervalStart;
	}

    /*
    private synchronized DateTime getTimestamp() throws IOException {
		Record rec = jobtable.get(jobkey);
		return rec.getUpdatedTimestamp();
	}
	*/
		
    /**
     * Update the status of this job in case it was changed
     * by a business rule.
     */
    synchronized void refreshStatus(Record jobrec) {
    	if (logger.isDebugEnabled()) {
    		String jobname = jobrec.getField("u_name");
    		String newstatus = jobrec.getField("u_status");
    		logger.debug("refreshStatus " + jobname + " " + newstatus);
    	}
    	assert jobkey != null;
    	assert jobkey.equals(jobrec.getKey());
    	this.status = Status.valueOf(jobrec.getField("u_status").toUpperCase());
    }

    /**
     * Update the job model table with a set of values.
     */
    private void updateJobTableValues(FieldValues values) throws SuiteModelException {
    	try {
    		jobtable.update(jobkey, values);
    	}
    	catch (IOException e) {
    		throw new SuiteModelException(e);
    	}    	
    }
        
	/**
	 * Get the record from ServiceNow.
	 */
	synchronized Record readRecord() throws SuiteModelException {
		try { 
			return jobtable.get(jobkey);
		}
		catch (IOException e) {
			throw new SuiteModelException(e);
		}
	}	
	    
}
