package servicenow.common.datamart;

import servicenow.common.soap.*;
import java.io.*;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.jdom2.JDOMException;

/**
 * Contains methods to process DataPump jobs.
 * @author Giles Lewis
 *
 */
public class JobController {

	final private SuiteController suite;
	final private DatamartConfiguration baseConfig;
	final private JobModel model;
	final private JobOperation operation;
	final private String tableName;
	final private String sqlTableName;
	final private Table table;
	private TableConfiguration tableConfig;
	private DatabaseWriter database;
	final private Logger logger;
	final private String jobName;
    final private Metrics metrics;
	final int loadLimit;
	private boolean isResume;

	public JobController(SuiteController suite, JobModel model) 
			throws SuiteInitException {
		assert suite != null;
		assert model != null;
		this.suite = suite;
		this.model = model;
		this.operation = model.getOperation();
		this.jobName = model.getName();
		assert operation != null;
		this.baseConfig = DatamartConfiguration.getDatamartConfiguration();
		this.logger = LoggerFactory.getLogger(this.getClass(), this.jobName);
		logger.debug("constructing " + jobName + " operation=" + operation);		
		if (this.operation.equals(JobOperation.SQL)) {
			this.tableName = null;
			this.table = null;
			this.metrics = null;
		}
		else {
			this.tableName = model.getTable().getName();
			this.tableConfig = new TableConfiguration(baseConfig, tableName);
			try {
				this.table = suite.getSession().table(tableName);
				Boolean checkReadable = tableConfig.getBoolean("check_readable",  true);
				if (checkReadable)
					if (!this.table.isReadable())
						throw new SuiteInitException(
							"Unable to read table: " + table.getName());
			} catch (InvalidTableNameException e) {
				throw new SuiteInitException(e);
			} catch (JDOMException e) {
				throw new SuiteInitException(e);
			} catch (IOException e) {
				throw new SuiteInitException(e);
			}
			this.metrics = new Metrics();
		}
		logger.info(model.getDescription());
		if (this.operation.equals(JobOperation.SQL)) {
			this.sqlTableName = "SQL";
			this.loadLimit = 0;
		}
		else if (this.operation.equals(JobOperation.GENERATE) ||
				 this.operation.equals(JobOperation.CREATETABLE) ||
				 this.operation.equals(JobOperation.TRUNCATE)) {
			this.sqlTableName = model.getSqlTableName();
			this.loadLimit = 0;
		}
		else {
			sqlTableName = model.getSqlTableName();
			// For testing and debugging
			// Limits the maximum number of records that can be loaded
			loadLimit = tableConfig.getInt("load_limit", 0);
		}
		logger.debug("constructed " + jobName);
	}
	
	String getName() { return this.jobName; }
	Session getSession() { return this.suite.getSession(); }
	JobOperation operation() { return this.operation; }
	boolean isPending() { return model.getStatus().equals(Status.PENDING); }
	boolean isPersistent() { return (model instanceof PersistentJob); }
	JobModel getModel() { return this.model; }
	Metrics getMetrics() { return this.metrics; }
	
	SuiteController getSuiteController() {
		return model.getSuite().getController();
	}
	
	Status getStatus() {
		Status result = model.getStatus();
		return result;
	}

	void setFailedStatus(Status newstatus, String message) throws IOException {
		model.setFailedStatus(newstatus, message);
	}
	
	DateTime getStartTime() { return model.getIntervalStart(); }
	DateTime getEndTime() { return model.getIntervalEnd(); }
	
	public DateTime resetInterval() throws SuiteModelException {
		if (model instanceof PersistentJob)
			return ((PersistentJob) model).resetInterval();
		else
			return null;
	}
	
	public void runJob(DateTime runStart, Status newStatus) 
			throws InterruptedException, SuiteInitException, 
				SuiteExecException, SuiteModelException {
		MDC.put("job", jobName);
		MDC.put("sqltable", sqlTableName);
		MDC.put("operation", operation.name().toLowerCase());
		MDC.put("table", table == null ? "" : table.getName());
		MDC.put("start", runStart.toString());
		database = getSuiteController().getDatabaseWriter();
		logger.debug("runJob start=" + runStart);
		isResume = getStatus().equals(Status.RESUME);
		if (isResume && isPersistent()) {
			logger.info("Resume job - loading metrics");
			PersistentJob pjob = (PersistentJob) model;
			metrics.loadValues(pjob.readRecord());
			metrics.logInfo(logger);
		}
		model.startRunning(runStart);
		try {
			switch (operation) {
			case LOAD :
				database.createMissingTargetTable(table, sqlTableName, model.isDV());
				runLoad();
				break;
			case REFRESH :
				database.createMissingTargetTable(table, sqlTableName, model.isDV());
				runRefresh(runStart);
				break;
			case PRUNE :
				runPrune(runStart);
				break;
			case SQL :
				runSql();
				break;
			case GENERATE : 
				runGenerate();
				break;
			default :
				throw new AssertionError();
			}
		}
		catch (InterruptedException e) {
			database.rollback();
			model.setFailedStatus(Status.CANCELLED, e.getMessage());
			throw e;
		}
		catch (SuiteInitException e) {
			database.rollback();
			model.setFailedStatus(Status.FAILED, e.getMessage());
			throw e;
		}
		catch (SuiteExecException e) {
			database.rollback();
			model.setFailedStatus(Status.FAILED, e.getMessage());
			throw e;
		}
		catch (SQLException e) {
			database.rollback();
			model.setFailedStatus(Status.FAILED, e.toString());
			throw new SuiteExecException(e);		
		}
		catch (SoapResponseException e) {
			database.rollback();
			model.setFailedStatus(Status.FAILED, e.toString());
			throw new SuiteExecException(e);
		}
		catch (IOException e) {
			database.rollback();
			model.setFailedStatus(Status.FAILED, e.toString());
			throw new SuiteExecException(e);
		}
		catch (AssertionError e) {
			database.rollback();
			model.setFailedStatus(Status.FAILED, e.toString());
			throw e;
		}
		model.setCompletedStatus(newStatus);
		logger.debug("runJob finished");
	}
	
	private void runLoad() 
			throws IOException, InterruptedException, SQLException,
				SuiteInitException, SuiteExecException, SuiteModelException {
		int published = 0;
		if (isResume) {
			published = metrics.recordsPublished();
		}
		else {
			metrics.clear();
		}
		assert published == metrics.recordsPublished();
		DatabaseTableWriter writer = 
			database.getTableWriter(table, sqlTableName, model.isDV());
		if (model.isTruncate() && published == 0) 
			writer.truncateTable();
		BasicTableReader reader = table.reader();
		QueryFilter filter = new QueryFilter()
			.addFilter(model.getBaseFilter())
			.addFilter(model.getPartitionFilter())
			.addFilter(getIntervalFilter());
		reader.setFilter(filter);
		reader.sortAscending(model.getSortField());
		logger.debug("runLoad filter=" + filter.toString() +
				" sort=" + model.getSortField());
		metrics.setExpected(reader.getKeys().size());
		if (published > 0) reader.setFirstRowIndex(published);
		loadRecordSets(reader);
	}
	
	private void runRefresh(DateTime runStart) 
			throws IOException, InterruptedException, SQLException,
				SuiteInitException, SuiteExecException, SuiteModelException{
		DateTime intervalStart = model.getIntervalStart();
		DateTime intervalEnd = model.getIntervalEnd();
		logger.debug("runRefresh runStart=" + runStart + 
			" intervalStart=" + intervalStart + " intervalEnd=" + intervalEnd);
		assert runStart != null;		
		if (intervalStart == null) {
			logger.info("runRefresh skipped");
			return;
		}			
		if (intervalStart.compareTo(intervalEnd) > 0) {
			logger.error("runRefresh intervalStart=" + intervalStart + 
				" precedes intervalEnd=" + intervalEnd);
			throw new AssertionError("runStart preceeds lastRunStart");
		}
		metrics.clear();
		BasicTableReader reader = table.reader();
		QueryFilter filter = new QueryFilter();
		filter.addFilter(model.getBaseFilter());
		filter.addFilter(model.getPartitionFilter());
		filter.addUpdatedFilter(intervalStart, intervalEnd);
		logger.debug("runRefresh filter=" + filter);
		assert intervalStart != null;
		assert intervalEnd != null;
		assert intervalEnd.compareTo(intervalStart) >= 0;
		reader.setFilter(filter);
		metrics.setExpected(reader.getKeys().size());
		logger.debug("runRefresh begin while");
		loadRecordSets(reader);
	}

	private int loadRecordSets(BasicTableReader reader)
			throws IOException, InterruptedException, SQLException,
				SuiteInitException, SuiteExecException, SuiteModelException {
		int published = metrics.recordsPublished();
		DatabaseTableWriter writer = 
				database.getTableWriter(table, sqlTableName, model.isDV());
		while (reader.hasNext()) {
			if (Thread.interrupted()) throw new InterruptedException();
			RecordList data = reader.nextChunk();
			if (Thread.interrupted()) throw new InterruptedException();
			int oldtotal = published;
			int processed = writer.processRecordSet(data, model.getMethod(), metrics);
			published += processed;
			assert processed == data.size();
			assert published == oldtotal + processed;
			model.postMetrics(metrics);
			logger.info(published + " of " + reader.numKeys() +	" records processed");
			if (loadLimit > 0 && metrics.recordsPublished() > loadLimit)
				throw new LoadLimitExceededException(sqlTableName, loadLimit);
		}		
		return published;
	}
	
	// TODO
	/*
	private int parallelLoadRecordSets(ParallelTableReader reader, ExecutorService executor) {
		DatabaseTableWriter writer = 
				database.getTableWriter(table, sqlTableName, model.isDV());
		while (reader.hasNextTask()) {
			Runnable task = reader.getNextTask();
			Future<RecordList> data =
		}
		
	}
	*/
	
	private void runPrune(DateTime runStart) 
			throws IOException, InterruptedException, SQLException, 
				SuiteInitException, SuiteExecException, SuiteModelException {
		Table auditDelete;
		try {
			auditDelete = getSession().table("sys_audit_delete");
		} catch (InvalidTableNameException e) {
			throw new SuiteExecException(e);
		} catch (JDOMException e) {
			throw new SuiteExecException(e);
		}		
		DateTime intervalStart = model.getIntervalStart();
		DateTime intervalEnd = model.getIntervalEnd();
		logger.debug("runPrune runStart=" + runStart + 
			" intervalStart=" + intervalStart + " intervalEnd=" + intervalEnd);
		assert runStart != null;		
		if (intervalStart == null) {
			logger.info("runRefresh skipped");
			return;
		}			
		if (intervalStart.compareTo(intervalEnd) > 0) {
			logger.error("runPrune intervalStart=" + intervalStart + 
				" precedes intervalEnd=" + intervalEnd);
			throw new AssertionError("runStart preceeds lastRunStart");
		}
		metrics.clear();
		int count = 0, deleted = 0;
		QueryFilter filter = new QueryFilter();
		filter.addFilter("tablename", QueryFilter.EQUALS, table.getName());
		filter.addCreatedFilter(intervalStart, intervalEnd);
		TableReader reader = auditDelete.reader(filter);
		DatabaseTableWriter writer = 
			database.getTableWriter(table, sqlTableName, model.isDV());
		while (reader.hasNext()) {
			if (Thread.interrupted()) throw new InterruptedException();
			RecordList deletes = reader.nextChunk();
			if (Thread.interrupted()) throw new InterruptedException();
			count = deletes.size();
			if (loadLimit > 0 && count > loadLimit)
				throw new LoadLimitExceededException(sqlTableName, loadLimit);
			KeyList keys = new KeyList();
			for (Record delete : deletes) {
				keys.add(new Key(delete.getField("documentkey")));
			}
			deleted += writer.deleteRecords(keys, metrics);		
			model.postMetrics(metrics);
			logger.info(deleted + " records deleted / " + count + " audit records");
		}
	}
	
	private void runSql() throws SQLException {
		database.executeSQL(model.getSqlCommand());
	}
	
	private void runGenerate() throws IOException, SQLException, SuiteInitException {
		EphemeralJob job = (EphemeralJob) this.model;
		File outfile = job.getOutputFile();
		PrintStream out = (outfile == null ? System.out : new PrintStream(outfile));
		SqlGenerator generator = new SqlGenerator(baseConfig, (DatabaseWriter) database);
		String sql = generator.getCreateTable(table, sqlTableName, model.isDV());
		out.println(sql);
		out.close();
	}
		
    public QueryFilter getIntervalFilter() {
		DateTime intervalStart = model.getIntervalStart();
		DateTime intervalEnd = model.getIntervalEnd();
		// logger.debug("getIntervalFilter start=" + intervalStart + " end=" + intervalEnd);
    	if (intervalStart == null && intervalEnd == null) {
    		return null;    	
    	}
    	else {
			QueryFilter result = new QueryFilter();
			if (model.getUseCreatedDate() == true) 
				result.addCreatedFilter(intervalStart, intervalEnd);
			else
				result.addUpdatedFilter(intervalStart, intervalEnd);
			return result;
    	}
    }
    	
}
