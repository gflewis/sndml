package servicenow.common.datamart;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.jdom2.JDOMException;

import servicenow.common.datamart.CommandBuffer;
import servicenow.common.datamart.EphemeralJob;
import servicenow.common.datamart.JobModel;
import servicenow.common.datamart.JobOperation;
import servicenow.common.datamart.LoadMethod;
import servicenow.common.datamart.LoggerFactory;
import servicenow.common.datamart.Metrics;
import servicenow.common.datamart.Status;
import servicenow.common.datamart.SuiteInitException;
import servicenow.common.datamart.SuiteModel;
import servicenow.common.datamart.SuiteModelException;
import servicenow.common.datamart.SuiteParseException;
import servicenow.common.soap.DateTime;
import servicenow.common.soap.InvalidTableNameException;
import servicenow.common.soap.QueryFilter;
import servicenow.common.soap.Session;

public class EphemeralJob extends JobModel {

	File outfile;
	
	EphemeralJob(SuiteModel suite, CommandBuffer buffer)
    		throws IOException, SQLException, 
    			InvalidTableNameException, SuiteInitException {
		super(suite);
    	assert suite != null;
    	DateTime now = DateTime.now();
    	String[] validCommands = {"load", "refresh", "prune", "sql", "generate"};
		String command = buffer.getOneOf(validCommands);
		if (command.equals("sql")) {
			this.jobName = "SQL";
			this.glidetable = null;
			// this.metrics = null;
		}
		else {
			String tableName = buffer.getToken();
			this.jobName = tableName;
			try {
				Session session = suite.getSession();
				this.glidetable = session.table(tableName);
			}
			catch (JDOMException e) {
				throw new RuntimeException(e);
			}
			// this.metrics = new Metrics();
		}
    	this.logger = LoggerFactory.getLogger(EphemeralJob.class, this.jobName);
		logger.debug(buffer.getBuffer());
		if (command.equals("sql")) {
			this.operation = JobOperation.SQL;
			this.method = null;
			this.sqlCommand = buffer.getToken();		
		}
		else if (command.equals("generate")) {
			this.operation = JobOperation.GENERATE;
			if (buffer.match("into")) 
				sqlTableName = buffer.getToken();
			if (buffer.match("file"))
				outfile = new File(buffer.getToken());
		}
		else if (command.equals("load")) {
			this.operation = JobOperation.LOAD;
			this.useCreatedDate = true;
			this.sortField = "sys_created_on";
	    	this.basefilter = new QueryFilter();
			if (buffer.match("into")) 
				sqlTableName = buffer.getToken();
			if (buffer.match("dv") || buffer.match("display-values"))
				displayValues = true;
    		if (buffer.match("truncate")) {
    			truncate = true;
    		}
    		if (buffer.match("update-insert"))
    			method = LoadMethod.UPDATE_INSERT;
    		else if (buffer.match("insert-only"))
    			method = LoadMethod.INSERT_ONLY;
    		else if (buffer.match("compare-timestamps"))
    			this.method = LoadMethod.COMPARE_TIMESTAMPS;
    		else {
    			if (truncate) 
    				this.method = LoadMethod.INSERT_ONLY;
    			else 
    				this.method = LoadMethod.UPDATE_INSERT;
    		}
    		if (buffer.match("created")) {
    			useCreatedDate = true;
    			sortField = "sys_created_on";
    		}
    		else if (buffer.match("updated")) {
    			useCreatedDate = false;
    			sortField = "sys_updated_on";
    		}
    		if (buffer.match("from")) 
    			intervalStart = buffer.getDatePlus();
    		if (buffer.match("to")) 
    			intervalEnd = buffer.getDatePlus();
    		if (buffer.match("partition")) {
    			this.partitionField = buffer.getToken();
    			buffer.consume("value");
    			this.partitionValue = buffer.getToken();
    		}
    		if (buffer.match("where"))
    			this.basefilter = new QueryFilter(buffer.getToken());
    		if (buffer.match("order-by")) {
    			sortField = buffer.getToken();
    		}
		}
		else if (command.equals("refresh")) {
			// DateTime refreshSince = null;
			this.operation = JobOperation.REFRESH;
			this.method = LoadMethod.UPDATE_INSERT;			
			this.sortField = "sys_updated_on";
			// this.useKeys = true;
	    	this.basefilter = new QueryFilter();
			if (buffer.match("into")) 
				sqlTableName = buffer.getToken();
			if (buffer.match("dv") || buffer.match("display-values"))
				displayValues = true;
			if (buffer.match("since")) {
				DateTime refreshSince = buffer.getDatePlus();
				if (now.compareTo(refreshSince) < 0)
					throw new SuiteParseException("Invalid: since=" + refreshSince);
				intervalStart = null;
				intervalEnd = refreshSince;
			}
			else {
				intervalStart = null;
				intervalEnd = now;
			}
			if (buffer.match("where"))
				this.basefilter = new QueryFilter(buffer.getToken());
		}
		else if (command.equals("prune")) {
			DateTime pruneSince = null;
			this.operation = JobOperation.PRUNE;
			this.method = null;
			if (buffer.match("into"))
				this.sqlTableName = buffer.getToken();
			if (buffer.match("since")) {
				pruneSince = buffer.getDate();
				intervalStart = null;
				intervalEnd = pruneSince;
			}
			else {
				intervalStart = null;
				intervalEnd = now;
			}
		}
		else 
			throw new SuiteParseException("Invalid operation: " + command);
		buffer.verifyAtEnd();
		this.status = Status.QUEUED;
    	// this.controller = new JobController(this);
    	assert operation != null;
    }	

    /**
     * Set the status to Running, and advance the interval.
     */
    synchronized void startRunning(DateTime runStart) throws SuiteModelException {
    	logger.debug("startRunning " + runStart);
		this.status = Status.RUNNING;
		if (operation != JobOperation.LOAD) {
			intervalStart = intervalEnd;
			intervalEnd = runStart;
		}
		logger.debug("startRunning runStart=" + runStart + " start=" + intervalStart + " end=" + intervalEnd);
    }

    File getOutputFile() {
    	return outfile;
    }
    
	/**
	 * This procedure really does nothing for ephemeral jobs.
	 */
    void postMetrics(Metrics metrics) throws SuiteModelException {
		int inserts = metrics.recordsInserted();
		int updates = metrics.recordsUpdated();
		int deletes = metrics.recordsDeleted();
		int published = metrics.recordsPublished();
		logger.debug("postMetrics inserts=" + inserts + 
				" updates=" + updates +
				" deletes=" + deletes + 
				" published=" + published);	
		assert (inserts + updates + deletes == published);
	}
	
}
