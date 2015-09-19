package servicenow.common.datamart;

import org.slf4j.Logger;

import servicenow.common.datamart.JobOperation;
import servicenow.common.datamart.LoadMethod;
import servicenow.common.datamart.Metrics;
import servicenow.common.datamart.PersistentJob;
import servicenow.common.datamart.Status;
import servicenow.common.datamart.SuiteModel;
import servicenow.common.datamart.SuiteModelException;
import servicenow.common.soap.DateTime;
import servicenow.common.soap.QueryFilter;
import servicenow.common.soap.Table;

/**
 * Contains the representation of a single job.
 * The job may be instantiated from string/command
 * or from a u_datapump_job Record.
 * If the job is a u_datapump_job in the ServiceNow instance,
 * then this class will also update the status of the job.
 * @author Giles Lewis
 *
 */
abstract class JobModel {
	
	protected Table glidetable;
	protected JobOperation operation;
	protected String jobName;
	protected LoadMethod method;
	protected SuiteModel suite;
	// protected JobController controller;
	
	protected boolean displayValues = false;
	protected String sortField = null;
	protected String partitionField = null;
	protected String partitionValue = null;
	protected DateTime intervalStart = null;
	protected DateTime intervalEnd = null;	
	protected QueryFilter basefilter = null;
	protected String sqlTableName = null;	
	protected String sqlCommand = null;

	protected Status status = null;
	
	// For Load jobs
	protected boolean truncate = false;
	protected boolean useCreatedDate = false;

    Logger logger;        

    JobModel(SuiteModel suite) {
    	this.suite = suite;
    }
    
    // JobController getController() { return this.controller; }
    boolean isPersistent() { return (this instanceof PersistentJob); }
    boolean isTruncate() { return this.truncate; }
	boolean getUseCreatedDate() { return this.useCreatedDate; }	
	JobOperation getOperation() { return this.operation; }
    
    /**
     * Return true if the dv (display values) option was specified.
     */
    boolean isDV() { return this.displayValues; }
	
    Table getTable() { return this.glidetable; }
    String getName() { return this.jobName; }
    LoadMethod getMethod() { return this.method; }
    String getSortField() { return this.sortField; }
    
    SuiteModel getSuite() { return this.suite; }

    synchronized Status getStatus() { return this.status; }    
    
    String getSqlCommand() {
    	if (operation != JobOperation.SQL) 
    		throw new UnsupportedOperationException();
    	return this.sqlCommand;
    }
       
    QueryFilter getBaseFilter() { return basefilter; }
    
    QueryFilter getPartitionFilter() {
    	if (partitionField == null) return null;
    	assert partitionField.length() > 0;
    	QueryFilter filter = new QueryFilter(partitionField, partitionValue);
    	return filter;
    }
    
    synchronized DateTime getIntervalStart() { return intervalStart;  }
    synchronized DateTime getIntervalEnd() { return intervalEnd; }
    
    /**
     * Set the status to Running, and advance the interval.
     */
    abstract void startRunning(DateTime runStart) throws SuiteModelException;
    abstract void postMetrics(Metrics metrics) throws SuiteModelException;

    // override for persistent jobs
    void setFailedStatus(Status newstatus, String message) { };
    void setCompletedStatus(Status newstatus) throws SuiteModelException { };
    
	String getSqlTableName() {
		return sqlTableName == null ? glidetable.getName() : sqlTableName;
	}
	
	public String getDescription() {
		StringBuilder result = new StringBuilder();
		result.append(operation.name().toLowerCase());
		if (operation.equals(JobOperation.SQL)) {
			String sql = getSqlCommand();
			result.append(" {" + sql + "}");
		}
		else {
			result.append(" ");
			result.append(glidetable.getName());
			result.append(" into " + getSqlTableName());
			if (isTruncate()) result.append(" truncate");
			if (operation.equals(JobOperation.LOAD)) {
				switch (getMethod()) {
					case UPDATE_INSERT : result.append(" update-insert"); break; 
					case INSERT_ONLY : result.append(" insert-only"); break;
					case COMPARE_TIMESTAMPS : result.append(" compare-timestamps"); break;
					default : throw new IllegalStateException();
				}
				if (getIntervalStart() != null) 
					result.append(" from " + getIntervalStart().toString());				
				if (getIntervalEnd() != null) 
					result.append(" to " + getIntervalEnd().toString());
			}
			if (operation.equals(JobOperation.REFRESH) || 
					operation.equals(JobOperation.PRUNE)) {
				if (getIntervalEnd() != null)
					result.append(" since " + getIntervalEnd().toString());								
			}
			if (partitionField != null) {
				result.append(" partition " + partitionField);
				result.append(" {" + partitionValue + "}");
			}
			QueryFilter filter = getBaseFilter();
			if (filter != null && !filter.isEmpty())
				result.append(" where {" + filter + "}");
		}
		return result.toString();
	}	
	
}

