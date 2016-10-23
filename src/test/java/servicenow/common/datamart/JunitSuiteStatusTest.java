package servicenow.common.datamart;

import static org.junit.Assert.*;

import org.junit.*;
import org.slf4j.Logger;

import servicenow.common.datamart.JobModel;
import servicenow.common.datamart.PersistentJob;
import servicenow.common.datamart.PersistentSuite;
import servicenow.common.datamart.Status;

public class JunitSuiteStatusTest {

	static Logger log = AllTests.getLogger(JunitSuiteStatusTest.class);

	@Before
	public void setUpBefore() throws Exception {
		AllTests.initialize();
	}
	
	/**
	 * If status is changed to PENDING then READY, 
	 * failed job will reset to READY
	 * @throws Exception
	 */
	@Test
	public void testPendingReady() throws Exception {
		PersistentSuite suite = AllTests.loadJunitSuite("junit-location-refresh");
		Status status = suite.getStatus();
		assertNotNull(status);
		PersistentJob refreshjob = suite.getJobByName("junit-location-refresh");
		assertNotNull(refreshjob);
		refreshjob.setFailedStatus(Status.FAILED, "Testing");
		log.info(refreshjob.getName() + " set to " + Status.FAILED);
		suite.setStatus(Status.PENDING);
		assertEquals(Status.PENDING, suite.getStatus());
		suite.setStatus(Status.READY);
		assertEquals(Status.READY, suite.getStatus());
		for (JobModel job : suite.getJobs()) {
			log.info(job.getName() + ": " + job.getStatus());
			assertEquals(Status.READY, job.getStatus());
		}			
	}

	@Test
	public void testCancelled() throws Exception {
		PersistentSuite suite = AllTests.loadJunitSuite("junit-location-refresh");
		suite.setStatus(Status.READY);
		suite.loadJobs();
		JobModel refreshjob = suite.getJobByName("junit-location-refresh");
		refreshjob.setFailedStatus(Status.FAILED, "Testing");
		suite.setStatus(Status.RESUME);
		int ready = 0;
		int failed = 0;
		int resume = 0;
		for (JobModel job : suite.getJobs()) {
			switch (job.getStatus()) {
			case READY : 
				++ready;
				break;
			case FAILED :
				++failed;
				break;
			case RESUME : 
				++resume;
				break;
			default :
				fail("Status=" + job.getStatus());
			}
		}
		assertTrue(ready > 0);
		assertTrue(failed == 0);
		assertTrue(resume == 1);
	}
}
