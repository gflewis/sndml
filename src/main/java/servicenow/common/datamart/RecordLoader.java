package servicenow.common.datamart;

import servicenow.common.soap.*;

public class RecordLoader implements RecordListProcessor {

	final DatabaseTableWriter writer;
	final LoadMethod method;
	final Metrics metrics;
	
	public RecordLoader(DatabaseTableWriter writer, LoadMethod method, Metrics metrics) {
		this.writer = writer;
		this.method = method;
		this.metrics = metrics;
	}

	@Override
	public void processList(RecordList data) 
			throws InterruptedException, SuiteExecException {
		// TODO Auto-generated method stub
		if (Thread.interrupted()) throw new InterruptedException();
		// int oldtotal = published;
		
		int processed = writer.processRecordSet(data, method, metrics);
		// published += processed;
		assert processed == data.size();
		// assert published == oldtotal + processed;
		//
		// TODO fix finish this
		/*
		model.postMetrics(metrics);
		logger.info(published + " of " + reader.numKeys() +	" records processed");
		if (loadLimit > 0 && metrics.recordsPublished() > loadLimit)
			throw new LoadLimitExceededException(sqlTableName, loadLimit);
		*/

	}

}
