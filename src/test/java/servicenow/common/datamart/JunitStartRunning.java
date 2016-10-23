package servicenow.common.datamart;

import static org.junit.Assert.*;

import java.sql.SQLException;

import org.slf4j.Logger;

import servicenow.common.datamart.PersistentSuite;
import servicenow.common.datamart.Status;
import servicenow.common.datamart.SuiteController;
import servicenow.common.soap.DateTime;

import org.junit.*;

public class JunitStartRunning {

	static SuiteController suite;
	static Logger logger = AllTests.getLogger(JunitStartRunning.class);
	
	static final int adjustment = -10;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Test
	public void testStartRunning() throws Exception {
		PersistentSuite suite = AllTests.loadJunitSuite("junit-location-load");
		int dbcount = suite.countJobs();
		logger.info("dbcount = " + dbcount);
		assertTrue(dbcount > 0);
		suite.setStatus(Status.COMPLETE);
		DateTime current = DateTime.now();
		// adjust for the fact that local time may differ from server time
		DateTime adjusted = current.addSeconds(adjustment);
		DateTime start = suite.startRunning();
		logger.info("current =" + current);
		logger.info("runstart=" + start);
		assertTrue(start.compareTo(adjusted) >= 0);
		assertEquals(dbcount, suite.jobs.size());
	}

	@After
	public void tearDownAfter() throws SQLException {
		DB.rollback();
	}
}
