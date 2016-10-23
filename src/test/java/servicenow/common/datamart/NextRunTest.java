package servicenow.common.datamart;

import static org.junit.Assert.*;

import java.sql.SQLException;

import org.slf4j.Logger;

import servicenow.common.datamart.LoggerFactory;
import servicenow.common.datamart.SuiteModel;
import servicenow.common.soap.DateTime;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NextRunTest {

	static SuiteModel suite;
	static Logger logger = LoggerFactory.getLogger(NextRunTest.class);
	
	@Before
	public void setUpBefore() throws Exception {
		AllTests.initialize();
	}
	
	@Test
	public void testNextRun() throws Exception {
		AllTests.banner(logger, "Begin testNextRun");
		String[] script = {
				"every 60 seconds", "refresh cmn_location"};
		suite = AllTests.newSuite(script);
		Long nowMS = DateTime.now().toDate().getTime();
		DateTime lastrun = suite.getRunStart();
		DateTime nextrun = suite.getNextRunStart();
		Long nextRunMS = nextrun.toDate().getTime();
		assertNull(lastrun);
		assertTrue(Math.abs(nextRunMS - nowMS) < 2000);
		int numJobs = suite.numJobs();
		assertEquals(1, numJobs);
	}

	@After
	public void tearDownAfter() throws SQLException {
		DB.rollback();
	}
	
}
