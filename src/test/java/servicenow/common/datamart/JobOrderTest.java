package servicenow.common.datamart;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import servicenow.common.datamart.JobModel;
import servicenow.common.datamart.PersistentSuite;

public class JobOrderTest {

	Logger logger = AllTests.getLogger(JobOrderTest.class);
	
	@Before
	public void setUp() throws Exception {
		AllTests.initialize();
	}

	@Test
	public void test() throws Exception {
		PersistentSuite suite = AllTests.loadJunitSuite("junit-location-refresh");
		suite.loadJobs();
		for (JobModel job : suite.getJobs()) {
			logger.info(job.getName());
		}
		assertEquals("junit-location-refresh", suite.getJobs().get(0).getName());
		assertEquals("junit-location-prune", suite.getJobs().get(1).getName());
	}

}
