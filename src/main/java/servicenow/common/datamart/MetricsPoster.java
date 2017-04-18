package servicenow.common.datamart;

import org.slf4j.Logger;

public class MetricsPoster {

    Logger logger;

	public MetricsPoster(Logger logger) {
		this.logger = logger;
	}

    void postMetrics(Metrics metrics) throws SuiteModelException {
		int inserts = metrics.recordsInserted();
		int updates = metrics.recordsUpdated();
		int deletes = metrics.recordsDeleted();
		int processed = metrics.recordsPublished();
		int total = metrics.recordsExpected();
		logger.info(processed + " of " + total + " records processed");
		logger.debug("postMetrics inserts=" + inserts + 
				" updates=" + updates +
				" deletes=" + deletes + 
				" processed=" + processed);	
		assert (inserts + updates + deletes == processed);
	}

}
