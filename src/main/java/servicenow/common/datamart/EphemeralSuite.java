package servicenow.common.datamart;

import java.io.File;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;

import servicenow.common.datamart.CommandBuffer;
import servicenow.common.datamart.CommandScript;
import servicenow.common.datamart.EphemeralJob;
import servicenow.common.datamart.EphemeralSuite;
import servicenow.common.datamart.JobModel;
import servicenow.common.datamart.LoggerFactory;
import servicenow.common.datamart.Status;
import servicenow.common.datamart.SuiteController;
import servicenow.common.datamart.SuiteInitException;
import servicenow.common.datamart.SuiteModel;
import servicenow.common.datamart.SuiteModelException;

import servicenow.common.soap.DateTime;
import servicenow.common.soap.Session;

class EphemeralSuite extends SuiteModel {

	EphemeralSuite(Session session, File file) 
			throws SuiteInitException, IOException, SQLException {
		this(session, new CommandScript(file));
	}
	
	EphemeralSuite(Session session, CommandScript input) 
			throws SuiteInitException, IOException, SQLException {
		
		super(session );
		this.status = Status.QUEUED;
		this.runStart = null;
		this.nextRunStart = DateTime.now();
    	this.logger = LoggerFactory.getLogger(EphemeralSuite.class);
    	jobs = new LinkedList<JobModel>();

    	logger.debug("input count=" + input.lines.size());
    	Iterator<CommandBuffer> lines = input.iterator();
    	CommandBuffer commandLine = lines.next();
    	if (commandLine == null) throw new BufferUnderflowException();
    	if (commandLine.match("every")) {
    		this.frequency = commandLine.getInterval();
    		logger.debug("frequency=" + frequency);
    		commandLine.verifyAtEnd();
    	}
    	else {
    		frequency = null;
    		logger.debug(commandLine.toString());
    		jobs.add(new EphemeralJob(this, commandLine));
    	}
		
    	while (lines.hasNext()) {
    		commandLine = lines.next();
    		logger.debug(commandLine.toString());
    		jobs.add(new EphemeralJob(this, commandLine));
    	}
    	
		if (jobs.size() == 0)
			throw new SuiteInitException("no jobs loaded");

		controller = new SuiteController(this);		
	}
	
	String getName() { return null; }
	
	DateTime startRunning() 
			throws InterruptedException, SuiteInitException, SuiteModelException {
		runStart = DateTime.now();
		nextRunStart = isPolling() ? runStart.addSeconds(getFrequency()) : null;
		logger.info("startRunning " + runStart.toString());
		return runStart;
	}
	

	void setStatus(Status newstatus, String runlog)	
			throws SuiteModelException {
		logger.debug("setStatus " + newstatus);
		this.status = newstatus;
	}

	
}
